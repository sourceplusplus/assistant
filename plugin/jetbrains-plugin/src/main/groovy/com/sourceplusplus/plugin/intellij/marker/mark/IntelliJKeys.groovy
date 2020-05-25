package com.sourceplusplus.plugin.intellij.marker.mark

import com.sourceplusplus.marker.source.mark.api.key.SourceKey

import java.time.Instant

/**
 * Keys used by Source++ to attribute data to IntelliJ elements.
 *
 * @version 0.2.6
 * @since 0.2.5
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJKeys {
    //todo: just save artifact instead of artifact configs
    public static final SourceKey<Boolean> ArtifactSubscribed = new SourceKey<>("ArtifactSubscribed")
    public static final SourceKey<Boolean> ArtifactDataAvailable = new SourceKey<>("ArtifactDataAvailable")
    public static final SourceKey<Instant> ArtifactSubscribeTime = new SourceKey<>("ArtifactSubscribeTime")
    public static final SourceKey<Instant> ArtifactUnsubscribeTime = new SourceKey<>("ArtifactUnsubscribeTime")
    public static final SourceKey<String> PortalUUID = new SourceKey<>("PortalUUID")
}
