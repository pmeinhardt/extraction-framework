package org.dbpedia.extraction.timemachine

import org.apache.log4j.Logger
import org.dbpedia.extraction.live.storage.JDBCPoolConnection
import java.sql.{SQLException, Connection, Date}

/**
 * Uses the live cache db to retrieve modification data for a given resource.
 *
 * Note: In order to use this feature, you will need to install the change
 * tracking by loading 'revisions.sql' into your database system.
 *
 * @param pageID page id for the resource
 */
class RevisionHistory(var pageID: Long) {
  val logger: Logger = Logger.getLogger(classOf[RevisionHistory])

  def rewind(timestamp: Long): Iterator[Revision] = {
    var connection: Connection = null
    try {
      connection = JDBCPoolConnection.getCachePoolConnection

      val statement = connection.prepareStatement("SELECT * FROM DBPEDIALIVE_REVISIONS WHERE pageID = ? AND timestamp > ? ORDER BY timestamp DESC")
      statement.setLong(1, pageID)
      statement.setDate(2, new Date(timestamp))

      val result = statement.executeQuery()

      new Iterator[Revision] {
        def hasNext: Boolean = result.next
        def next(): Revision = {
          val additionsStr = result.getString("additions")
          val deletionsStr = result.getString("deletions")
          Revision.from(additionsStr, deletionsStr)
        }
      }
    } catch {
      case e: SQLException =>
        logger.warn(e.getMessage)
        null
    } finally {
      if (connection != null) connection.close()
    }
  }
}
