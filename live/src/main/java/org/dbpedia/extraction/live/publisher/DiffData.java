package org.dbpedia.extraction.live.publisher;

import java.util.Date;
import java.util.HashSet;

/**
 * Holds a diff for a new extraction (called from PublisherDiffDestination)
 */
public class DiffData {

    public long pageID = 0;
    public long timestamp = 0;
    public HashSet<String> toAdd = null;
    public HashSet<String> toDelete = null;

    public DiffData(long id, long timestamp, HashSet<String> add, HashSet<String> delete) {
        this.pageID = id;
        this.timestamp = timestamp;
        this.toAdd = new HashSet<String>(add);
        this.toDelete = new HashSet<String>(delete);
    }
}

