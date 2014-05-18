package org.dbpedia.extraction.timemachine

import java.sql.{Connection, Date, ResultSet, SQLException}
import org.apache.log4j.Logger
import org.dbpedia.extraction.live.storage.JDBCPoolConnection

/**
 * Uses the live cache db to retrieve modification data for a given resource.
 *
 * All retrieved information is listed in reverse order (most recent first)
 * as this is the most natural ordering for any of our use-cases so far.
 *
 * Note: In order to use this feature, you will need to install the change
 * tracking by loading 'revisions.sql' into your database system and enable
 * the 'RevisionDestination'.
 *
 * @param pageID page id for the resource
 */
class RevisionHistory(var pageID: Long) {
  val logger: Logger = Logger.getLogger(classOf[RevisionHistory])

  /**
   * Traces back the specified resource's revisions/changes to the given time.
   *
   * @param timestamp point in time to go back to
   * @return iterator for revisions of the resource in reverse order
   */
  def revisions(timestamp: Long): Iterator[Revision] = {
    val q = "SELECT * FROM DBPEDIALIVE_REVISIONS WHERE pageID = ? AND timestamp > ? ORDER BY timestamp DESC"
    load(q, timestamp, (result) => {
      new Iterator[Revision] {
        def hasNext: Boolean = result.next
        def next(): Revision = {
          val modifiedDate = result.getDate("timestamp")
          val additionsStr = result.getString("additions")
          val deletionsStr = result.getString("deletions")
          Revision.from(modifiedDate.getTime, additionsStr, deletionsStr)
        }
      }
    })
  }

  /**
   * Traces back the specified resource's modification timestamps.
   *
   * @param timestamp point in time to go back to
   * @return iterator for modification timestamps of the resource in reverse order
   */
  def timestamps(timestamp: Long): Iterator[Long] = {
    val q = "SELECT timestamp FROM DBPEDIALIVE_REVISIONS WHERE pageID = ? AND timestamp > ? ORDER BY timestamp DESC"
    load(q, timestamp, (result) => {
      new Iterator[Long] {
        def hasNext: Boolean = result.next
        def next(): Long = {
          val modificationDate = result.getDate("timestamp")
          modificationDate.getTime
        }
      }
    })
  }

  private def load[A](query: String, timestamp: Long, iterate: (ResultSet) => Iterator[A]): Iterator[A] = {
    var connection: Connection = null
    try {
      connection = JDBCPoolConnection.getCachePoolConnection

      val statement = connection.prepareStatement(query)
      statement.setLong(1, pageID)
      statement.setDate(2, new Date(timestamp))

      val results = statement.executeQuery()

      iterate(results)
    } catch {
      case e: SQLException =>
        logger.warn(e.getMessage)
        null
    } finally {
      if (connection != null) connection.close()
    }
  }
}
