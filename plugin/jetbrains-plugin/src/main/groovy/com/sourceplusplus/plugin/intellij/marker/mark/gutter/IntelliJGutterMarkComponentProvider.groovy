package com.sourceplusplus.plugin.intellij.marker.mark.gutter

import org.jetbrains.annotations.NotNull
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEvent
import com.sourceplusplus.marker.source.mark.api.event.SourceMarkEventListener
import com.sourceplusplus.marker.source.mark.gutter.component.jcef.GutterMarkJcefComponentProvider
import com.sourceplusplus.marker.source.mark.gutter.event.GutterMarkEventCode

import java.awt.Dimension

/**
 * todo: description
 *
 * @version 0.2.5
 * @since 0.2.5
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
class IntelliJGutterMarkComponentProvider extends GutterMarkJcefComponentProvider  implements SourceMarkEventListener{

    IntelliJGutterMarkComponentProvider() {
        defaultConfiguration.preloadJcefBrowser = false
        defaultConfiguration.setComponentSize(new Dimension(775, 250))
        //todo: measure size of editor and make short if it will extend past IDE
        //defaultConfiguration.setComponentSize(new Dimension(620, 250))
    }

    @Override
    void handleEvent(@NotNull SourceMarkEvent event) {
        if (event.eventCode == GutterMarkEventCode.GUTTER_MARK_VISIBLE) {
            (event.sourceMark as IntelliJGutterMark).registerPortal()
        }
    }
}
