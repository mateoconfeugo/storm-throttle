package Nami::Flow::AdUnit::UsageDetail;
use Moose;
use MooseX::Storage;

with Storage('format' => 'JSON', 'io' => 'File');

has feed_ids => (is=>'rw', isa=>'ArrayRef');
has level => (is=>'rw');
has level_id => (is=>'rw');
has query => (is=>'rw');

no Moose;
1;
