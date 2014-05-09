--
-- Create the update tracking table `DBPEDIALIVE_UPDATES`
--

SET SESSION innodb_file_per_table=1;
SET SESSION innodb_file_format=Barracuda;

DROP TABLE IF EXISTS `DBPEDIALIVE_UPDATES`;
CREATE TABLE IF NOT EXISTS `DBPEDIALIVE_UPDATES` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'The unique id for the update',
  `pageID` int(11) NOT NULL DEFAULT '0' COMMENT 'The wikipedia page ID',
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'timestamp of when the page was updated',

  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=4;

-- Add an index on the DBPEDIALIVE_UPDATES `pageID`

CREATE INDEX `updates_page_id_index` ON `DBPEDIALIVE_UPDATES` (`pageID`);

-- Track changes using a database trigger on the cache table

DELIMITER |

CREATE TRIGGER trackchanges AFTER UPDATE ON `DBPEDIALIVE_CACHE`
FOR EACH ROW BEGIN
  INSERT INTO `DBPEDIALIVE_UPDATES` (pageID, timestamp) VALUES (NEW.pageID, NEW.updated);
END;
|

DELIMITER ;
