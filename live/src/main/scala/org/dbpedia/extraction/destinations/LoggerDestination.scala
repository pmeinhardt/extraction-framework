package org.dbpedia.extraction.destinations

import java.util.Date
import scala.collection.Seq
import org.apache.log4j.Logger

/**
 * Just logs the extraction output to the screen
 */
class LoggerDestination(pageID: Long, pageTitle: String) extends LiveDestination {

  private val logger = Logger.getLogger(classOf[LoggerDestination].getName)

  private var addedTriples = 0
  private var deletedTriples = 0
  private var unmodifiedTriples = 0
  private var extractors = 0;
  private var now = System.currentTimeMillis
  private var date: Date = null

  /**
   * Opens this destination. This method should only be called once during the lifetime
   * of a destination, and it should not be called concurrently with other methods of this class.
   */
  def open() {}

  override def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad], timestamp: Date) {
    extractors += 1
    addedTriples += graphAdd.length
    deletedTriples += graphRemove.length
    unmodifiedTriples += graphUnmodified.length
    date = timestamp
  }

  override def close = {
    val total = addedTriples + unmodifiedTriples
    logger.info("Page with ID:" + pageID + " produced " + total +
      " Triples (A:" + addedTriples + "/D:" + deletedTriples + "/U:" + unmodifiedTriples +
      ") in " + (date.getTime - now) + "ms. (Title: " + pageTitle + ")")
  }
}