--
-- Table structure for table `DBPEDIALIVE_CACHE`
--

SET SESSION innodb_file_per_table=1;
SET SESSION innodb_file_format=Barracuda;

DROP TABLE IF EXISTS `DBPEDIALIVE_CACHE`;
CREATE TABLE IF NOT EXISTS `DBPEDIALIVE_CACHE` (
  `pageID` int(11) NOT NULL DEFAULT '0' COMMENT 'The wikipedia page ID',
  `title` varchar(512) COLLATE utf8_unicode_ci NOT NULL DEFAULT '' COMMENT 'The wikipedia page title',
  `updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'timestamp of when the page was updated',
  `timesUpdated` smallint(6) NOT NULL DEFAULT '0' COMMENT 'Total times the page was updated', -- Fot future use: complete update after e.g. 10 updates
  `json` longtext COLLATE utf8_unicode_ci NOT NULL DEFAULT '' COMMENT 'The latest extraction in JSON format',
  `subjects` text COLLATE utf8_unicode_ci NOT NULL DEFAULT '' COMMENT 'Distinct subjects extracted from the current page (might be more than one)',
  `diff` text COLLATE utf8_unicode_ci NOT NULL DEFAULT '' COMMENT 'Keeps the latest triple diff (not implemented yet)',
  `error` SMALLINT NOT NULL DEFAULT '0' COMMENT 'If there was an error the last time the page was updated',

  PRIMARY KEY (`pageID`),
  KEY `updated_index` (`updated`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci ENGINE = InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=4;

--
-- Create the update tracking table `DBPEDIALIVE_UPDATES`
--

DROP TABLE IF EXISTS `DBPEDIALIVE_UPDATES`;
CREATE TABLE IF NOT EXISTS `DBPEDIALIVE_UPDATES` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'The unique id for the update',
  `pageID` int(11) NOT NULL DEFAULT '0' COMMENT 'The wikipedia page ID',
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'timestamp of when the page was updated',

  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci ENGINE = InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=4;

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

-- We use innodb_file_per_table=1; innodb_file_format=Barracuda; ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=4
-- because in English Wikipedia the cache can reach up to 200GB!!!
-- This way we reduce I/O and space a lot. It makes the db a little slower but it is also easier to recover
-- when tables are stored in separate files.