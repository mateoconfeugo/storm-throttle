DROP TABLE IF EXISTS `feed_source`;
CREATE TABLE `feed_source` (
  `feed_id` int(10) unsigned NOT NULL default '0',
  `source_id`  int(10) unsigned NOT NULL default '0',
  `current_count`  int(10) unsigned NOT NULL default '0',
  `allocated`  int(10) unsigned NOT NULL default '0',
  PRIMARY KEY  (`feed_id`, `source_id`)
) Engine=InnoDB;

insert into feed_source (feed_id, source_id, current_count, allocated) values (8087, 1, 100, 30000); 
insert into feed_source (feed_id, source_id, current_count, allocated) values (8087, 2, 200, 30000); 
insert into feed_source (feed_id, source_id, current_count, allocated) values (8002, 1, 300, 30000); 
insert into feed_source (feed_id, source_id, current_count, allocated) values (8003, 1, 400, 30000); 
insert into feed_source (feed_id, source_id, current_count, allocated) values (8004, 1, 500, 30000); 
insert into feed_source (feed_id, source_id, current_count, allocated) values (8004, 2, 600, 30000); 
