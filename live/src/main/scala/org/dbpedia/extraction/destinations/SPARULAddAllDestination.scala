package org.dbpedia.extraction.destinations

import java.util.Date
import formatters.UriPolicy._
import scala.collection.Seq

/**
 * Writes all triples to the DB. Used to cleanup insert/delete errors
 */

class SPARULAddAllDestination(policies: Array[Policy] = null)
  extends SPARULDestination(true, policies) {

  override def write(extractor: String, hash: String, addGraph: Seq[Quad], deleteGraph: Seq[Quad], unmodifiedGraph: Seq[Quad], timestamp: Long) {
    tripleSize += addGraph.length + deleteGraph.length + unmodifiedGraph.length

    for (quad <- addGraph) {
      sparql.append(formatter.render(quad))
    }

    for (quad <- deleteGraph) {
      sparql.append(formatter.render(quad))
    }

    for (quad <- unmodifiedGraph) {
      sparql.append(formatter.render(quad))
    }

  }

}