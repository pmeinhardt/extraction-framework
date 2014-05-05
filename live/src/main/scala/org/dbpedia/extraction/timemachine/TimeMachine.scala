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
   * @return set of all quads for the resource at the given time and
   *         the actual creation time of the retrieved version state
   */
  def retrieve(pageID: Long, timestamp: Long): (Set[Quad], Long) = {
    var quads = new mutable.HashSet[Quad]
    var t: Long = System.currentTimeMillis

    // Fetch current json-cache item and resource triples
    quads ++= current(pageID)

    // Trace modifications
    val timeline = trace(pageID, timestamp)

    // Roll back any changes
    for (revision <- timeline) {
      quads --= revision.additions
      quads ++= revision.deletions
      t = revision.timestamp
    }

    (quads.toSet, t)
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
   * Traces back a resource's modifications back to the specified point in time.
   *
   * @param pageID page id for the resource to trace
   * @param timestamp point in time to go back to
   * @return iterator for revisions of the resource in reverse order
   */
  def trace(pageID: Long, timestamp: Long): Iterator[Revision] = {
    val history = new RevisionHistory(pageID)
    history.revisions(timestamp)
  }
}
