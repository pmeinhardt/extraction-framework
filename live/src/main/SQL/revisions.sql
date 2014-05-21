--
-- Create the update tracking table `DBPEDIALIVE_REVISIONS`
--

SET SESSION innodb_file_per_table=1;
SET SESSION innodb_file_format=Barracuda;

DROP TABLE IF EXISTS `DBPEDIALIVE_REVISIONS`;
CREATE TABLE IF NOT EXISTS `DBPEDIALIVE_REVISIONS` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'The unique revision id',
  `pageID` int(11) NOT NULL DEFAULT '0' COMMENT 'The wikipedia page ID',
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'A timestamp of when the page was updated',
  `additions` text COLLATE utf8_unicode_ci NOT NULL DEFAULT '' COMMENT 'Keeps triples added in this revision in N-Triples format',
  `deletions` text COLLATE utf8_unicode_ci NOT NULL DEFAULT '' COMMENT 'Keeps triples removed in this revision in N-Triples format',

  PRIMARY KEY (`id`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci ENGINE=InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=4;

-- Add an index on the DBPEDIALIVE_REVISIONS `pageID` column

CREATE INDEX `revisions_page_id_index` ON `DBPEDIALIVE_REVISIONS` (`pageID`);
