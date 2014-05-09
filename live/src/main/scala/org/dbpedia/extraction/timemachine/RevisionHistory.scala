package org.dbpedia.extraction.timemachine

import org.apache.log4j.Logger
import org.dbpedia.extraction.live.storage.JDBCPoolConnection
import java.sql.{SQLException, Connection, Date}

/**
 * Uses the live cache db to retrieve modification data for a given resource.
 *
 * Note: In order to use this feature, you will need to install the change
 * tracking by loading `changetracking.sql` into your database system.
 *
 * @param pageID page id for the resource
 */
class RevisionHistory(var pageID: Long) {
  val logger: Logger = Logger.getLogger(classOf[RevisionHistory])

  def rewind(timestamp: Long): Iterator[Long] = {
    var connection: Connection = null
    try {
      connection = JDBCPoolConnection.getCachePoolConnection

      val statement = connection.prepareStatement("SELECT * FROM DBPEDIALIVE_UPDATES WHERE pageID = ? AND timestamp > ? ORDER BY timestamp DESC")
      statement.setLong(1, pageID)
      statement.setDate(2, new Date(timestamp))

      val result = statement.executeQuery()

      new Iterator[Long] {
        def hasNext: Boolean = result.next
        def next(): Long = {
          val date = result.getDate("timestamp")
          date.getTime
        }
      }
    } catch {
      case e: SQLException => {
        logger.warn(e.getMessage)
        null
      }
    } finally {
      if (connection != null) connection.close()
    }
  }
}
