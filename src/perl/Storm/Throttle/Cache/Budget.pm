package Storm::Throttle::Cache::Budget;
use Moose;
use Redis;

has cache => (is=>'rw', lazy_build=>1);
has redis_path => (is=>'rw', required=>1);
has dbindex => (is=>'rw', required=>1);

sub BUILD {
    my $self = shift;
}

sub gen_budget_cache_key {
    my ($self, $args) = @_;
    my $feed_id = $self->feed_id;
    my $level = $self->level;
    my $level_id = $self->level_id;
    my $key = $feed_id . $level . $level_id;
    return $key;
}

sub _build_cache {
    my $self = shift;
    my $redis = Redis->new(server => $self->redis_path);
    $redis->select( $self->dbindex );
    return $redis;
}	
 
sub set  {
    my ($self, $k, $v) = @_;
    my $cache = $self->cache;
    $cache->set($k=>$v->freeze);
    return $self;
}

sub get {
   my ($self, $k, $v) = @_;
   my $cache = $self->cache;
   my $json = $cache->get($k);
   return Storm::Throttle::Budget->thaw($json);
}

sub DESTROY {
    my $self = shift;
    my $cache =  $self->cache;
    untie $cache;
}

no Moose;
1;
