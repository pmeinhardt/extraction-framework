package org.dbpedia.extraction.timemachine

import org.dbpedia.extraction.destinations.{ChangesetPathProvider, Quad}
import scala.collection.mutable
import scala.io.Source

class Changeset(val added: Set[Quad], val removed: Set[Quad]) {}

object Changeset {
  def load(pageID: Long, timestamp: Long): Changeset = {
    load(ChangesetPathProvider.path(pageID, timestamp))
  }

  def load(path: String): Changeset = {
    var added = new mutable.HashSet[Quad]
    var removed = new mutable.HashSet[Quad]

    // TODO Get explicit added/removed file paths (see RDFDiffWriter/TimestampedDiffDestination)

    for (line <- Source.fromFile(path + ".added.nt.gz").getLines()) {
      added ++= Quad.unapply(line)
    }

    for (line <- Source.fromFile(path + ".removed.nt.gz").getLines()) {
      removed ++= Quad.unapply(line)
    }

    new Changeset(added.toSet, removed.toSet)
  }
}
