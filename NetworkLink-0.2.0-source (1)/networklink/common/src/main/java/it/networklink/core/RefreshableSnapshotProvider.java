package it.networklink.core;

public interface RefreshableSnapshotProvider extends PlatformSnapshotProvider {
    void refresh();
}
