package Storm::Throttle::Budget;
use Moose;
use MooseX::Storage;

with Storage('format' => 'JSON', 'io' => 'File');
 
has feed_id => (is=>'rw', isa=>'Str');
has level => (is=>'rw', isa=>'Str');
has level_id => (is=>'rw', isa=>'Str');
has allotment => (is=>'rw', isa=>'Int');
has current_count => (is=>'rw', isa=>'Int');

no Moose;
1;
