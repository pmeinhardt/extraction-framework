package org.dbpedia.extraction.destinations

import java.io.File
import java.util.Calendar
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.live.publisher.RDFDiffWriter
import scala.collection.mutable

/**
 * Writes resource diffs into separate changeset files using the page modified timestamp.
 *
 * @param pageID page id of the resource from which quads were extracted
 * @param policies URI formatting policies
 */
class TimestampedDiffDestination(pageID: Long, policies: Array[Policy] = null) extends LiveDestination {
  var formatter = new TerseFormatter(false, true, policies)

  var added   = new mutable.HashSet[String] // use a Set to remove duplicates
  var removed = new mutable.HashSet[String] // use a Set to remove duplicates

  var path: String = null

  def open() {}

  def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad], timestamp: Long) {
    for (quad <- graphAdd)
      added.add(formatter.render(quad))

    for (quad <- graphRemove)
      removed.add(formatter.render(quad))

    path = ChangesetPathProvider.path(pageID, timestamp)
  }

  def close() {
    val parent = new File(path).getParentFile
    if (parent != null) parent.mkdirs()

    val addStr = new StringBuilder()
    for (s: String <- added) addStr.append(s)
    RDFDiffWriter.write(addStr.toString(), true, path, true)

    val delStr = new mutable.StringBuilder()
    for (s: String <- removed) delStr.append(s)
    RDFDiffWriter.write(delStr.toString(), false, path, true)
  }
}

object ChangesetPathProvider {
  val basedir: String = LiveOptions.options.get("publishDiffRepoPath")

  def path(pageID: Long, timestamp: Long): String = {
    val cal = Calendar.getInstance()

    val year  = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val day   = cal.get(Calendar.DAY_OF_MONTH)
    val hour  = cal.get(Calendar.HOUR_OF_DAY)

    String.format("%s/%04d/%02d/%02d/%02d/%020d-%20d", basedir, year, month, day, hour, timestamp, pageID)
  }
}
