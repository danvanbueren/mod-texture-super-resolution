package me.danvb10.mtsr.config;

/**
 * Interface implemented by configuration GUI screens that support live UI refreshing.
 */
public interface RefreshableScreen {
    /**
     * Requests that the screen rebuild its UI layout on the next render tick.
     */
    void requestRefresh();
}
