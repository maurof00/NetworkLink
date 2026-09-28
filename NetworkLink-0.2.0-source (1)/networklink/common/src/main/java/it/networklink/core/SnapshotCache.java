package it.networklink.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SnapshotCache {
    private volatile NodeSnapshot local;
    private volatile Map<String, NodeSnapshot> remote = Map.of();

    public void setLocal(NodeSnapshot snapshot) { this.local = snapshot; }
    public NodeSnapshot local() { return local; }
    public Map<String, NodeSnapshot> remote() { return remote; }

    public void replaceRemote(Map<String, NodeSnapshot> values) {
        this.remote = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
