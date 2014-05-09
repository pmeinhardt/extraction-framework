package org.dbpedia.extraction.timemachine

import org.dbpedia.extraction.live.storage.JSONCache
import org.dbpedia.extraction.destinations.Quad
import scala.collection.mutable

class TimeMachine {
  /**
   * Retrieves all quads for the specified resource and timestamp.
   *
   * @param pageID page id of the resource to fetch and trace back
   * @param timestamp roll back the resource to this point in time
   * @return set of all quads for the resource at the given time
   */
  def retrieve(pageID: Long, timestamp: Long): Set[Quad] = {
    var triples = new mutable.HashSet[Quad]

    // Fetch current json-cache item and resource triples
    triples ++= current(pageID)

    // Identify modification dates/times
    val timeline = trace(pageID, timestamp)

    // Roll back any relevant changesets
    for (ts <- timeline) {
      val changeset = Changeset.load(pageID, ts)
      triples --= changeset.added
      triples ++= changeset.removed
    }

    triples.toSet
  }

  /**
   * Fetches the current set of quads for the specified resource.
   *
   * @param pageID page id of the resource to load quads for
   * @return set of all quads for the resource in its current state
   */
  def current(pageID: Long): Set[Quad] = {
    val cached = new JSONCache(pageID, null)
    cached.getAllHashedTriples().toSet
  }

  /**
   * Traces back a resource's modification timestamps back to the specified
   * point in time.
   *
   * @param pageID page id for the resource to trace
   * @param timestamp point in time to go back to
   * @return iterator for timestamps of resource modification times in reverse order
   */
  def trace(pageID: Long, timestamp: Long): Iterator[Long] = {
    val history = new RevisionHistory(pageID)
    history.rewind(timestamp)
  }
}
