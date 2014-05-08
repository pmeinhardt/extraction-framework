package org.dbpedia.extraction.timemachine

import org.dbpedia.extraction.destinations.Quad
import scala.collection.mutable

class Changeset(val added: Set[Quad], val removed: Set[Quad]) {}

object Changeset {
  def load(timestamp: Long): Changeset = {
    var added = new mutable.HashSet[Quad]
    var removed = new mutable.HashSet[Quad]

    // TODO
    // Get publishing path for the given timestamp, search for changeset files
    // Read all matched changeset files, extract added and removed Quads

    new Changeset(added.toSet, removed.toSet)
  }
}
