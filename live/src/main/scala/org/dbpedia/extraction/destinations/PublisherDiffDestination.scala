package org.dbpedia.extraction.destinations

import java.util.Date
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import collection.mutable
import org.dbpedia.extraction.live.main.Main
import org.dbpedia.extraction.live.publisher.DiffData

/**
 * This class publishes the triples to files (added / deleted)
 */
class PublisherDiffDestination(pageID: Long, policies: Array[Policy] = null) extends LiveDestination {

  var formatter = new TerseFormatter(false, true, policies)

  var added = new java.util.HashSet[String]() // Set to remove duplicates
  var deleted = new java.util.HashSet[String]() // Set to remove duplicates

  var time: Long = 0


  def open() { }

  def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad], timestamp: Long) {
    for (quad <- graphAdd)
      added.add(formatter.render(quad))

    for (quad <- graphRemove)
      deleted.add(formatter.render(quad))

    time = timestamp
  }

  def close() {
    Main.publishingDataQueue.add(new DiffData(pageID, time, added, deleted))
  }

}
