package com.sourceplusplus.api.bridge;

import com.sourceplusplus.api.model.artifact.SourceArtifact;
import com.sourceplusplus.api.model.integration.IntegrationInfo;
import com.sourceplusplus.api.model.metric.ArtifactMetricResult;
import com.sourceplusplus.api.model.trace.ArtifactTraceResult;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketFrame;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

/**
 * Used to setup a bridge client to core which allows for pub/sub communication.
 *
 * @version 0.3.2
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
public class SourceBridgeClient {

    private final Vertx vertx;
    private final String apiHost;
    private final int apiPort;
    private final boolean ssl;
    private HttpClient client;
    private boolean active;
    private boolean reconnecting;

    public SourceBridgeClient(Vertx vertx, String apiHost, int apiPort, boolean ssl) {
        this.vertx = vertx;
        this.apiHost = apiHost;
        this.apiPort = apiPort;
        this.ssl = ssl;
    }

    public void setupSubscriptions() {
        active = true;
        client = vertx.createHttpClient(new HttpClientOptions().setSsl(ssl));
        client.webSocket(apiPort, apiHost, "/eventbus/websocket", conn -> {
            if (conn.succeeded()) {
                WebSocket ws = conn.result();
                reconnecting = false;
                JsonObject pingMsg = new JsonObject().put("type", "ping");
                ws.writeFrame(WebSocketFrame.textFrame(pingMsg.encode(), true));
                vertx.setPeriodic(5000, it -> {
                    if (active && !reconnecting) {
                        ws.writeFrame(WebSocketFrame.textFrame(pingMsg.encode(), true));
                    } else {
                        vertx.cancelTimer(it);
                    }
                });
                ws.closeHandler(it -> {
                    if (active) {
                        reconnect();
                    }
                });

                JsonObject msg = new JsonObject().put("type", "register")
                        .put("address", PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED.getAddress());
                ws.writeFrame(WebSocketFrame.textFrame(msg.encode(), true));
                msg = new JsonObject().put("type", "register")
                        .put("address", PluginBridgeEndpoints.ARTIFACT_STATUS_UPDATED.getAddress());
                ws.writeFrame(WebSocketFrame.textFrame(msg.encode(), true));
                msg = new JsonObject().put("type", "register")
                        .put("address", PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.getAddress());
                ws.writeFrame(WebSocketFrame.textFrame(msg.encode(), true));
                msg = new JsonObject().put("type", "register")
                        .put("address", PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.getAddress());
                ws.writeFrame(WebSocketFrame.textFrame(msg.encode(), true));
                ws.handler(it -> {
                    JsonObject ob = new JsonObject(it.toString());
                    if (PluginBridgeEndpoints.INTEGRATION_INFO_UPDATED.getAddress().equals(ob.getString("address"))) {
                        handleIntegrationInfoUpdated(ob);
                    } else if (PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED.getAddress().equals(ob.getString("address"))) {
                        handleArtifactConfigUpdated(ob);
                    } else if (PluginBridgeEndpoints.ARTIFACT_STATUS_UPDATED.getAddress().equals(ob.getString("address"))) {
                        handleArtifactStatusUpdated(ob);
                    } else if (PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.getAddress().equals(ob.getString("address"))) {
                        handleArtifactMetricUpdated(ob);
                    } else if (PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.getAddress().equals(ob.getString("address"))) {
                        handleArtifactTraceUpdated(ob);
                    } else {
                        throw new IllegalArgumentException("Unsupported bridge address: " + ob.getString("address"));
                    }
                });
            } else {
                if (reconnecting) {
                    reconnect();
                } else {
                    conn.cause().printStackTrace();
                }
            }
        });
    }

    public void close() {
        active = false;
        client.close();
    }

    private void reconnect() {
        reconnecting = true;
        vertx.setTimer(5000, it -> {
            close();
            setupSubscriptions();
        });
    }

    private void handleIntegrationInfoUpdated(JsonObject msg) {
        IntegrationInfo integration = Json.decodeValue(msg.getJsonObject("body").toString(), IntegrationInfo.class);
        vertx.eventBus().publish(PluginBridgeEndpoints.INTEGRATION_INFO_UPDATED.getAddress(), integration);
    }

    private void handleArtifactConfigUpdated(JsonObject msg) {
        SourceArtifact artifact = Json.decodeValue(msg.getJsonObject("body").toString(), SourceArtifact.class);
        vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_CONFIG_UPDATED.getAddress(), artifact);
    }

    private void handleArtifactStatusUpdated(JsonObject msg) {
        SourceArtifact artifact = Json.decodeValue(msg.getJsonObject("body").toString(), SourceArtifact.class);
        vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_STATUS_UPDATED.getAddress(), artifact);
    }

    private void handleArtifactMetricUpdated(JsonObject msg) {
        ArtifactMetricResult artifactMetricResult = Json.decodeValue(msg.getJsonObject("body").toString(), ArtifactMetricResult.class);
        vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_METRIC_UPDATED.getAddress(), artifactMetricResult);
    }

    private void handleArtifactTraceUpdated(JsonObject msg) {
        ArtifactTraceResult artifactTraceResult = Json.decodeValue(msg.getJsonObject("body").toString(), ArtifactTraceResult.class);
        vertx.eventBus().publish(PluginBridgeEndpoints.ARTIFACT_TRACE_UPDATED.getAddress(), artifactTraceResult);
    }
}
