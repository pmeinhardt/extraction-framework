package org.dbpedia.extraction.destinations

import java.sql.{SQLException, Date, Connection}
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.live.storage.JDBCPoolConnection
import scala.collection.mutable
import org.apache.log4j.Logger

/**
 * Writes resource revisions into separate database table.
 *
 * Note: In order to use this destination, be sure to load 'revisions.sql'
 * into your database system.
 *
 * @param pageID page id of the resource from which quads were extracted
 * @param policies URI formatting policies
 */
class RevisionDestination(pageID: Long, policies: Array[Policy] = null) extends LiveDestination {
  private val logger: Logger = Logger.getLogger(classOf[RevisionDestination])

  var formatter = new TerseFormatter(false, true, policies)

  var additions = new mutable.HashSet[String] // use a Set to remove duplicates
  var deletions = new mutable.HashSet[String] // use a Set to remove duplicates

  var time: Long = 0

  def open() {}

  def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad], timestamp: Long) {
    for (quad <- graphAdd)
      additions.add(formatter.render(quad))

    for (quad <- graphRemove)
      deletions.add(formatter.render(quad))

    time = timestamp
  }

  def close() {
    var connection: Connection = null
    try {
      connection = JDBCPoolConnection.getCachePoolConnection

      val addStr = new StringBuilder()
      for (s: String <- additions) addStr.append(s)

      val delStr = new mutable.StringBuilder()
      for (s: String <- deletions) delStr.append(s)

      val statement = connection.prepareStatement("INSERT INTO DBPEDIALIVE_REVISIONS (pageID, timestamp, additions, deletions) VALUES (?, ?, ?, ?)")

      statement.setLong(1, pageID)
      statement.setDate(2, new Date(time))
      statement.setString(3, addStr.toString())
      statement.setString(4, delStr.toString())

      statement.executeQuery()
    } catch {
      case e: SQLException => logger.warn(e.getMessage)
    } finally {
      if (connection != null) connection.close()
    }
  }
}
