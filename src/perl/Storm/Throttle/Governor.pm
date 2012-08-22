package Storm::Throttle::Governor;
use Moose;

use CHI;
use IO::Socket;
use Fcntl ':flock'; 
use JSON::XS;
use Net::Kestrel;
use Randomize;

use Storm::Throttle::UsageDetail;
use Storm::Throttle::Budget;
use Storm::Throttle::Cache::Budget;

has kestrel_client => (is=>'rw', lazy_build=>1);
has query => (is=>'rw');
has asp_id => (is=>'rw');
has throttle_type => (is=>'rw');
has publisher_id => (is=>'rw');
has source_id => (is=>'rw');
has subid_id => (is=>'rw');
has feed_config => (is=>'rw');
has feeds => (is=>'rw', required=>1);
has feed_ids => (is=>'rw', lazy_build=>1);
has level => (is=>'rw');
has level_id => (is=>'rw');
has source_custom => (is=>'rw');
has subid_custom => (is=>'rw');
has blackout_periods => (is=>'rw');
has usage_detail => (is=>'rw', isa=>'Storm::Throttle::UsageDetail', lazy_build=>1);
has budget_cache  => (is=>'rw', isa=>'Storm::Throttle::Cache::Budget', lazy_build=>1);
has budget_dbindex => (is=>'rw', isa=>'Int', required=>1);
has usage_server_host => (is=>'rw', isa=>'Str', default=>'10.0.147.102');
has usage_server_port => (is=>'rw', isa=>'Int', default=>2222);
has usage_detail_server => (is=>'rw', lazy_build=>1);
has budget_cache_host => (is=>'rw', isa=>'Str', default=>'10.0.147.102');
has budget_cache_port => (is=>'rw', default=>6379);
has budget_cache => (is=>'rw', lazy_build=>1);
has matrix => (is=>'rw', isa=>'HashRef', default=>sub { {} });

# has usage_cache  => (is=>'rw', isa=>'Storm::Throttle::Cache::UsageDetail', lazy_build=>1);

sub throttle {
    my $self = shift;
    $self->update({query=>$self->query});
    my %filter_map = map { $_ => $self->matrix->{$_}->[0] } keys %{$self->matrix};
    return \%filter_map;
}

sub update {
    my ($self, $args) = @_;
    my $query = $args->{query};
    my $usage = $args->{usage_detail} || $self->usage_detail;
    my $level = $args->{level} || 'source';
    my $level_id = $usage->level_id;
    for my $fid (keys %{$self->feeds}) {
	my $budget_key = "$fid-$level-$level_id";
	my $budget_item = eval { $self->budget_cache->get($budget_key) };
	if ($budget_item) { 
	    my $count = $budget_item->{current_count};
	    $count = $count + 1;
	    $budget_item->{current_count} = $count;
	    my $usage_count_record = {feed_id=>$fid, level_id=>$level_id, level=>$level, count=>$count};
	    eval { $self->send_usage({key=>$budget_key, usage=>$usage_count_record}) };
	    $self->budget_cache->set($budget_key=>$budget_item);
	    if( $budget_item->current_count < $budget_item->allotment ) {
		$self->matrix->{$fid} = [1, $budget_item->current_count];  
	    }
	    else {
		$self->matrix->{$fid} = [0, $budget_item->current_count];
	    }
	}
	else { 
	    $self->matrix->{$fid} = [1, 0];  
	}
#	$self->usage_cache->set($budget_key, $count);
    }
    return $self;
}

sub pick_usage_server {
    my ($self, $args) = @_;
    # TODO: get the node values from a configuration file
    my @rules =({Field  => 'node',  Values => ["host1.foo.com", "host2.foo.com"]});
    my $randomizer = Randomize->new(\@rules);
    my $random_hash = $randomizer->generate();
    return $random_hash->{node};
}
has usage_detail_queue => (is=>'rw', isa=>'Str', default=> 'throttle_queue');
sub send_usage {
    my ($self, $args) = @_;
    my $usage = $args->{usage};
    $self->kestrel_client->put($self->usage_detail_queue, encode_json($usage));
#    $usage->{node} = $self->node_id;
#    my $socket = $self->usage_detail_server;
#    print $socket encode_json($usage);
}

sub _build_kestrel_client {
    my $self = shift;
#    my $host = $self->pick_usage_server;
    my $host = "10.0.0.85";
    my $port = $self->usage_server_port;
    my $kes = Net::Kestrel->new(host => $host, port => $port);
    return $kes;
}

sub _build_usage_detail_server {
    my $self = shift;


#    my $dt = { "tcp" => sub { return  IO::Socket::INET->new(PeerAddr => $self->pick_useage_server,
#							    PeerPort => $self->usage_server_port,
#							    Proto => 'tcp',
#							    Type => SOCK_STREAM)},
#	       "unix" => sub { return IO::Socket::Unix->(PeerAddr=> $self->usage_server_host
#							 Type=>SOCK_DGRAM,
#							 Timeout=>1)}
#    };
#    my $server_type = $self->server_type;
#    $server_type eq 'tcp' ? $dt->{tcp}->() : $dt->{unix}->();
    my $path = $self->pick_usage_server . ":" . $self->usage_server_port;
    return IO::Socket::INET->new($path);
}

sub _build_usage_detail {
    my $self = shift;
    my @fids = keys %{$self->feeds};
    return Storm::Throttle::UsageDetail->new({feed_ids=> \@fids,
					    level => 'source',
					    level_id=>$self->source_id,
					    query=>$self->query});
}

sub _build_feed_ids {
    my $self = shift;
    my  @feeds =  map { $_->{feedID} } @{ $self->feeds };
    return \@feeds;
}

sub _build_budget_cache {
    my $self = shift;
    my $path = $self->budget_cache_host . ":" . $self->budget_cache_port;
    my $budget_cache = Storm::Throttle::Cache::Budget->new({name=>"throttle_budget", 
							       redis_path=> $path,
							       dbindex=>$self->budget_dbindex});
    return $budget_cache;
}

no Moose;
1;
