package org.dbpedia.extraction.timemachine

import org.dbpedia.extraction.destinations.Quad
import scala.collection.mutable

class Revision(val timestamp: Long, val additions: Set[Quad], val deletions: Set[Quad]) {}

object Revision {
  def from(timestamp: Long, additionsStr: String, deletionsStr: String): Revision = {
    new Revision(timestamp, parse(additionsStr), parse(deletionsStr))
  }

  private def parse(str: String): Set[Quad] = {
    val quads = new mutable.HashSet[Quad]

    for (line <- str.lines) {
      quads ++= Quad.unapply(line)
    }

    quads.toSet
  }
}
