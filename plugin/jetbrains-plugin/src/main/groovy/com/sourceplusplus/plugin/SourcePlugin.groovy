package com.sourceplusplus.plugin

import com.sourceplusplus.api.client.SourceCoreClient
import com.sourceplusplus.api.model.config.SourcePluginConfig
import com.sourceplusplus.api.model.config.SourcePortalConfig
import groovy.util.logging.Slf4j
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import org.jetbrains.annotations.NotNull

/**
 * todo: description
 *
 * @version 0.2.4
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class SourcePlugin {

    public static final String SOURCE_ENVIRONMENT_UPDATED = "SourceEnvironmentUpdated"

    private final Vertx vertx
    private PluginBootstrap pluginBootstrap

    SourcePlugin(SourceCoreClient coreClient) {
        vertx = Vertx.vertx()
        System.addShutdownHook {
            vertx.close()
        }
        updateEnvironment(Objects.requireNonNull(coreClient))
        vertx.deployVerticle(pluginBootstrap = new PluginBootstrap(this))

        //start plugin bridge for portal
        startPortalUIBridge({
            if (it.failed()) {
                log.error("Failed to start portal ui bridge", it.cause())
                throw new RuntimeException(it.cause())
            } else {
                log.info("PluginBootstrap started")
                SourcePortalConfig.current.pluginUIPort = it.result().actualPort()
                log.info("Using portal ui bridge port: " + SourcePortalConfig.current.pluginUIPort)
            }
        })
    }

    void updateEnvironment(SourceCoreClient coreClient) {
        SourcePluginConfig.current.activeEnvironment.coreClient = coreClient
        coreClient.attachBridge(vertx)
        if (SourcePluginConfig.current.activeEnvironment.appUuid) {
            SourcePortalConfig.current.addCoreClient(SourcePluginConfig.current.activeEnvironment.appUuid, coreClient)
        }
        vertx.eventBus().publish(SOURCE_ENVIRONMENT_UPDATED, SourcePluginConfig.current.activeEnvironment.environmentName)
    }

    private void startPortalUIBridge(Handler<AsyncResult<HttpServer>> listenHandler) {
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx)
        BridgeOptions portalBridgeOptions = new BridgeOptions()
                .addInboundPermitted(new PermittedOptions().setAddressRegex(".+"))
                .addOutboundPermitted(new PermittedOptions().setAddressRegex(".+"))
        sockJSHandler.bridge(portalBridgeOptions)

        Router router = Router.router(vertx)
        router.route("/eventbus/*").handler(sockJSHandler)
        vertx.createHttpServer().requestHandler(router).listen(0, listenHandler)
    }

    @NotNull
    Vertx getVertx() {
        return vertx
    }
}
