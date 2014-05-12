package org.dbpedia.extraction.timemachine

import org.dbpedia.extraction.destinations.Quad
import scala.collection.mutable

class Revision(val additions: Set[Quad], val deletions: Set[Quad]) {
  def this(additionsStr: String, deletionsStr: String) = this(parse(additionsStr), parse(deletionsStr))

  private def parse(str: String): Set[Quad] = {
    val quads = new mutable.HashSet[Quad]

    for (line <- str.lines) {
      quads ++= Quad.unapply(line)
    }

    quads.toSet
  }
}
