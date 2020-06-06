package com.sourceplusplus.core.storage.h2

import com.google.common.base.Charsets
import com.google.common.io.Resources
import com.sourceplusplus.api.model.application.SourceApplication
import com.sourceplusplus.api.model.application.SourceApplicationSubscription
import com.sourceplusplus.api.model.artifact.*
import com.sourceplusplus.api.model.config.SourceAgentConfig
import com.sourceplusplus.api.model.trace.Trace
import com.sourceplusplus.api.model.trace.TraceQuery
import com.sourceplusplus.api.model.trace.TraceSpan
import com.sourceplusplus.core.storage.CoreConfig
import com.sourceplusplus.core.storage.SourceStorage
import groovy.util.logging.Slf4j
import io.vertx.core.*
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException

import java.time.Instant
import java.util.stream.Collectors

/**
 * Represents a H2 storage for saving/fetching core data.
 *
 * @version 0.2.6
 * @since 0.2.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@Slf4j
class H2DAO extends SourceStorage {

    private static final String SOURCE_CORE_SCHEMA = Resources.toString(Resources.getResource(
            "storage/h2/schema/source_core.sql"), Charsets.UTF_8)
    private static final String GET_CORE_CONFIG = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_core_config.sql"), Charsets.UTF_8)
    private static final String UPDATE_CORE_CONFIG = Resources.toString(Resources.getResource(
            "storage/h2/queries/update_core_config.sql"), Charsets.UTF_8)
    private static final String CREATE_APPLICATION = Resources.toString(Resources.getResource(
            "storage/h2/queries/create_application.sql"), Charsets.UTF_8)
    private static final String UPDATE_APPLICATION = Resources.toString(Resources.getResource(
            "storage/h2/queries/update_application.sql"), Charsets.UTF_8)
    private static final String FIND_APPLICATION_BY_NAME = Resources.toString(Resources.getResource(
            "storage/h2/queries/find_application_by_name.sql"), Charsets.UTF_8)
    private static final String GET_APPLICATION = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_application.sql"), Charsets.UTF_8)
    private static final String GET_ALL_APPLICATIONS = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_all_applications.sql"), Charsets.UTF_8)
    private static final String CREATE_ARTIFACT = Resources.toString(Resources.getResource(
            "storage/h2/queries/create_artifact.sql"), Charsets.UTF_8)
    private static final String UPDATE_ARTIFACT = Resources.toString(Resources.getResource(
            "storage/h2/queries/update_artifact.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT_BY_ENDPOINT_NAME = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact_by_endpoint_name.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT_BY_ENDPOINT_ID = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact_by_endpoint_id.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT_BY_SUBSCRIBE_AUTOMATICALLY = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact_by_subscribe_automatically.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT_BY_ENDPOINT = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact_by_endpoint.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT_BY_FAILING = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact_by_failing.sql"), Charsets.UTF_8)
    private static final String GET_APPLICATION_ARTIFACTS = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_application_artifacts.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT_SUBSCRIPTIONS = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact_subscriptions.sql"), Charsets.UTF_8)
    private static final String GET_SUBSCRIBER_ARTIFACT_SUBSCRIPTIONS = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_subscriber_artifact_subscriptions.sql"), Charsets.UTF_8)
    private static final String GET_ALL_ARTIFACT_SUBSCRIPTIONS = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_all_artifact_subscriptions.sql"), Charsets.UTF_8)
    private static final String UPDATE_ARTIFACT_SUBSCRIPTION = Resources.toString(Resources.getResource(
            "storage/h2/queries/update_artifact_subscription.sql"), Charsets.UTF_8)
    private static final String GET_SUBSCRIBER_ARTIFACT_SUBSCRIPTION = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_subscriber_artifact_subscription.sql"), Charsets.UTF_8)
    private static final String GET_APPLICATION_SUBSCRIPTIONS = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_application_subscriptions.sql"), Charsets.UTF_8)
    private static final String DELETE_ARTIFACT_SUBSCRIPTION = Resources.toString(Resources.getResource(
            "storage/h2/queries/delete_artifact_subscription.sql"), Charsets.UTF_8)
    private static final String ADD_ARTIFACT_FAILURE = Resources.toString(Resources.getResource(
            "storage/h2/queries/add_artifact_failure.sql"), Charsets.UTF_8)
    private static final String GET_ARTIFACT_FAILURES = Resources.toString(Resources.getResource(
            "storage/h2/queries/get_artifact_failures.sql"), Charsets.UTF_8)
    private final JDBCClient client

    H2DAO(Vertx vertx, JsonObject config, Promise storageCompleter) {
        client = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", "jdbc:h2:mem:spp;DB_CLOSE_DELAY=-1")
                .put("driver_class", "org.h2.Driver"))

        client.update(SOURCE_CORE_SCHEMA, {
            if (it.succeeded()) {
                storageCompleter.complete()
            } else {
                log.error("Failed to create Source++ Core schema in H2", it.cause())
                storageCompleter.fail(it.cause())
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getCoreConfig(Handler<AsyncResult<Optional<CoreConfig>>> handler) {
        client.query(GET_CORE_CONFIG, {
            if (it.succeeded()) {
                def results = it.result().results
                if (!results.isEmpty()) {
                    handler.handle(Future.succeededFuture(Optional.of(CoreConfig.fromJson(results.get(0).getString(0)))))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void updateCoreConfig(CoreConfig coreConfig, Handler<AsyncResult<CoreConfig>> handler) {
        client.updateWithParams(UPDATE_CORE_CONFIG, new JsonArray().add(coreConfig.toString()), {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture(coreConfig))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void createApplication(SourceApplication application,
                           Handler<AsyncResult<SourceApplication>> handler) {
        def params = new JsonArray()
        params.add(application.appUuid())
        params.add(application.appName())
        params.add(application.createDate())
        if (application.agentConfig()) {
            params.add(Json.encode(application.agentConfig()))
        } else {
            params.addNull()
        }
        client.updateWithParams(CREATE_APPLICATION, params, {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture(application))
            } else if (it.cause() instanceof JdbcSQLIntegrityConstraintViolationException) {
                handler.handle(Future.failedFuture(new IllegalArgumentException("Application name is already in use")))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void updateApplication(SourceApplication application,
                           Handler<AsyncResult<SourceApplication>> handler) {
        def params = new JsonArray()
        params.add(application.appUuid())
        if (application.appName()) {
            params.add(application.appName())
        } else {
            params.addNull()
        }
        if (application.agentConfig()) {
            params.add(Json.encode(application.agentConfig()))
        } else {
            params.addNull()
        }
        client.updateWithParams(UPDATE_APPLICATION, params, {
            if (it.succeeded()) {
                getApplication(application.appUuid(), {
                    if (it.succeeded()) {
                        handler.handle(Future.succeededFuture(it.result().get()))
                    } else {
                        handler.handle(Future.failedFuture(it.cause()))
                    }
                })
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void findApplicationByName(String appName,
                               Handler<AsyncResult<Optional<SourceApplication>>> handler) {
        def params = new JsonArray()
        params.add(appName)
        client.queryWithParams(FIND_APPLICATION_BY_NAME, params, {
            if (it.succeeded()) {
                def results = it.result().results
                if (!results.isEmpty()) {
                    def existingApp = results.get(0)
                    def sourceApplication = SourceApplication.builder()
                            .appUuid(existingApp.getString(0))
                            .appName(existingApp.getString(1))
                            .createDate(Instant.parse(existingApp.getString(2)))
                    if (existingApp.getString(3)) {
                        sourceApplication.agentConfig(Json.decodeValue(
                                existingApp.getString(3), SourceAgentConfig.class))
                    }
                    handler.handle(Future.succeededFuture(Optional.of(sourceApplication.build())))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getApplication(String appUuid, Handler<AsyncResult<Optional<SourceApplication>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        client.queryWithParams(GET_APPLICATION, params, {
            if (it.succeeded()) {
                def results = it.result().results
                if (!results.isEmpty()) {
                    def existingApp = results.get(0)
                    def sourceApplication = SourceApplication.builder()
                            .appUuid(existingApp.getString(0))
                            .appName(existingApp.getString(1))
                            .createDate(Instant.parse(existingApp.getString(2)))
                    if (existingApp.getString(3)) {
                        sourceApplication.agentConfig(Json.decodeValue(
                                existingApp.getString(3), SourceAgentConfig.class))
                    }
                    handler.handle(Future.succeededFuture(Optional.of(sourceApplication.build())))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getAllApplications(Handler<AsyncResult<List<SourceApplication>>> handler) {
        client.query(GET_ALL_APPLICATIONS, {
            if (it.succeeded()) {
                def applications = new ArrayList<SourceApplication>()
                def results = it.result().results
                if (!results.isEmpty()) {
                    results.each {
                        def sourceApplication = SourceApplication.builder()
                                .appUuid(it.getString(0))
                                .appName(it.getString(1))
                                .createDate(Instant.parse(it.getString(2)))
                        if (it.getString(3)) {
                            sourceApplication.agentConfig(Json.decodeValue(
                                    it.getString(3), SourceAgentConfig.class))
                        }
                        applications.add(sourceApplication.build())
                    }
                }
                handler.handle(Future.succeededFuture(applications))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void createArtifact(SourceArtifact artifact, Handler<AsyncResult<SourceArtifact>> handler) {
        def params = new JsonArray()
        params.add(artifact.appUuid())
        params.add(artifact.artifactQualifiedName())
        params.add(artifact.createDate())
        params.add(artifact.config().endpoint())
        params.add(artifact.config().subscribeAutomatically())
        params.add(artifact.config().forceSubscribe())
        params.add(artifact.config().moduleName())
        params.add(artifact.config().component())
        params.add(artifact.config().endpointName())
        if (artifact.config().endpointIds() != null) {
            params.add("[" + artifact.config().endpointIds().stream().collect(Collectors.joining(",")) + "]")
        } else {
            params.addNull()
        }
        params.add(artifact.status().activelyFailing())
        params.add(artifact.status().latestFailedServiceInstance())

        client.updateWithParams(CREATE_ARTIFACT, params, {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture(artifact))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void updateArtifact(SourceArtifact artifact, Handler<AsyncResult<SourceArtifact>> handler) {
        def params = new JsonArray()
        params.add(artifact.appUuid())
        params.add(artifact.artifactQualifiedName())
        params.add(artifact.config().endpoint())
        params.add(artifact.config().subscribeAutomatically())
        params.add(artifact.config().forceSubscribe())
        params.add(artifact.config().moduleName())
        params.add(artifact.config().component())
        params.add(artifact.config().endpointName())
        if (artifact.config().endpointIds() != null) {
            params.add("[" + artifact.config().endpointIds().stream().collect(Collectors.joining(",")) + "]")
        } else {
            params.addNull()
        }
        params.add(artifact.status().activelyFailing())
        params.add(artifact.status().latestFailedServiceInstance())

        client.updateWithParams(UPDATE_ARTIFACT, params, {
            if (it.succeeded()) {
                getArtifact(artifact.appUuid(), artifact.artifactQualifiedName(), {
                    if (it.succeeded()) {
                        handler.handle(Future.succeededFuture(it.result().get()))
                    } else {
                        handler.handle(Future.failedFuture(it.cause()))
                    }
                })
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getArtifact(String appUuid, String artifactQualifiedName,
                     Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        params.add(artifactQualifiedName)
        client.queryWithParams(GET_ARTIFACT, params, {
            if (it.succeeded()) {
                def results = it.result().results
                if (!results.isEmpty()) {
                    def artifactDetails = results.get(0)
                    def artifact = SourceArtifact.builder()
                            .appUuid(artifactDetails.getString(0))
                            .artifactQualifiedName(artifactDetails.getString(1))
                            .createDate(Instant.parse(artifactDetails.getString(2)))
                            .lastUpdated(Instant.parse(artifactDetails.getString(3))).build()
                    def config = SourceArtifactConfig.builder()
                            .endpoint(artifactDetails.getBoolean(4))
                            .subscribeAutomatically(artifactDetails.getBoolean(5))
                            .forceSubscribe(artifactDetails.getBoolean(6))
                            .moduleName(artifactDetails.getString(7))
                            .component(artifactDetails.getString(8))
                            .endpointName(artifactDetails.getString(9))
                    if (artifactDetails.getString(10)) {
                        config.addEndpointIds(artifactDetails.getString(10)[1..-2].split(","))
                    }
                    def status = SourceArtifactStatus.builder()
                            .activelyFailing(artifactDetails.getBoolean(11))
                            .latestFailedServiceInstance(artifactDetails.getString(12))

                    handler.handle(Future.succeededFuture(Optional.of(
                            artifact.withConfig(config.build()).withStatus(status.build()))))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void findArtifactByEndpointName(String appUuid, String endpointName,
                                    Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        params.add(endpointName)
        client.queryWithParams(GET_ARTIFACT_BY_ENDPOINT_NAME, params, {
            if (it.succeeded()) {
                def results = it.result().results
                if (!results.isEmpty()) {
                    def artifactDetails = results.get(0)
                    def artifact = SourceArtifact.builder()
                            .appUuid(artifactDetails.getString(0))
                            .artifactQualifiedName(artifactDetails.getString(1))
                            .createDate(Instant.parse(artifactDetails.getString(2)))
                            .lastUpdated(Instant.parse(artifactDetails.getString(3))).build()
                    def config = SourceArtifactConfig.builder()
                            .endpoint(artifactDetails.getBoolean(4))
                            .subscribeAutomatically(artifactDetails.getBoolean(5))
                            .forceSubscribe(artifactDetails.getBoolean(6))
                            .moduleName(artifactDetails.getString(7))
                            .component(artifactDetails.getString(8))
                            .endpointName(artifactDetails.getString(9))
                    if (artifactDetails.getString(10)) {
                        config.addEndpointIds(artifactDetails.getString(10)[1..-2].split(","))
                    }
                    def status = SourceArtifactStatus.builder()
                            .activelyFailing(artifactDetails.getBoolean(11))
                            .latestFailedServiceInstance(artifactDetails.getString(12))

                    handler.handle(Future.succeededFuture(Optional.of(
                            artifact.withConfig(config.build()).withStatus(status.build()))))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void findArtifactByEndpointId(String appUuid, String endpointId,
                                  Handler<AsyncResult<Optional<SourceArtifact>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)

        //todo: search endpoint id without using LIKE
        params.add("[$endpointId]")
        params.add("[$endpointId,%")
        params.add("%,$endpointId,%")
        params.add("%,$endpointId]")

        client.queryWithParams(GET_ARTIFACT_BY_ENDPOINT_ID, params, {
            if (it.succeeded()) {
                def results = it.result().results
                if (!results.isEmpty()) {
                    def artifactDetails = results.get(0)
                    def artifact = SourceArtifact.builder()
                            .appUuid(artifactDetails.getString(0))
                            .artifactQualifiedName(artifactDetails.getString(1))
                            .createDate(Instant.parse(artifactDetails.getString(2)))
                            .lastUpdated(Instant.parse(artifactDetails.getString(3))).build()
                    def config = SourceArtifactConfig.builder()
                            .endpoint(artifactDetails.getBoolean(4))
                            .subscribeAutomatically(artifactDetails.getBoolean(5))
                            .forceSubscribe(artifactDetails.getBoolean(6))
                            .moduleName(artifactDetails.getString(7))
                            .component(artifactDetails.getString(8))
                            .endpointName(artifactDetails.getString(9))
                    if (artifactDetails.getString(10)) {
                        config.addEndpointIds(artifactDetails.getString(10)[1..-2].split(","))
                    }
                    def status = SourceArtifactStatus.builder()
                            .activelyFailing(artifactDetails.getBoolean(11))
                            .latestFailedServiceInstance(artifactDetails.getString(12))

                    handler.handle(Future.succeededFuture(Optional.of(
                            artifact.withConfig(config.build()).withStatus(status.build()))))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void findArtifactBySubscribeAutomatically(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        client.queryWithParams(GET_ARTIFACT_BY_SUBSCRIBE_AUTOMATICALLY, params, {
            if (it.succeeded()) {
                def artifacts = new ArrayList<SourceArtifact>()
                def results = it.result().results
                results.each {
                    def artifact = SourceArtifact.builder()
                            .appUuid(it.getString(0))
                            .artifactQualifiedName(it.getString(1))
                            .createDate(Instant.parse(it.getString(2)))
                            .lastUpdated(Instant.parse(it.getString(3))).build()
                    def config = SourceArtifactConfig.builder()
                            .endpoint(it.getBoolean(4))
                            .subscribeAutomatically(it.getBoolean(5))
                            .forceSubscribe(it.getBoolean(6))
                            .moduleName(it.getString(7))
                            .component(it.getString(8))
                            .endpointName(it.getString(9))
                    if (it.getString(10)) {
                        config.addEndpointIds(it.getString(10)[1..-2].split(","))
                    }
                    def status = SourceArtifactStatus.builder()
                            .activelyFailing(it.getBoolean(11))
                            .latestFailedServiceInstance(it.getString(12))

                    artifacts.add(artifact.withConfig(config.build()).withStatus(status.build()))
                }
                handler.handle(Future.succeededFuture(artifacts))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void findArtifactByEndpoint(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        client.queryWithParams(GET_ARTIFACT_BY_ENDPOINT, params, {
            if (it.succeeded()) {
                def artifacts = new ArrayList<SourceArtifact>()
                def results = it.result().results
                results.each {
                    def artifact = SourceArtifact.builder()
                            .appUuid(it.getString(0))
                            .artifactQualifiedName(it.getString(1))
                            .createDate(Instant.parse(it.getString(2)))
                            .lastUpdated(Instant.parse(it.getString(3))).build()
                    def config = SourceArtifactConfig.builder()
                            .endpoint(it.getBoolean(4))
                            .subscribeAutomatically(it.getBoolean(5))
                            .forceSubscribe(it.getBoolean(6))
                            .moduleName(it.getString(7))
                            .component(it.getString(8))
                            .endpointName(it.getString(9))
                    if (it.getString(10)) {
                        config.addEndpointIds(it.getString(10)[1..-2].split(","))
                    }
                    def status = SourceArtifactStatus.builder()
                            .activelyFailing(it.getBoolean(11))
                            .latestFailedServiceInstance(it.getString(12))

                    artifacts.add(artifact.withConfig(config.build()).withStatus(status.build()))
                }
                handler.handle(Future.succeededFuture(artifacts))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void findArtifactByFailing(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        client.queryWithParams(GET_ARTIFACT_BY_FAILING, params, {
            if (it.succeeded()) {
                def artifacts = new ArrayList<SourceArtifact>()
                def results = it.result().results
                results.each {
                    def artifact = SourceArtifact.builder()
                            .appUuid(it.getString(0))
                            .artifactQualifiedName(it.getString(1))
                            .createDate(Instant.parse(it.getString(2)))
                            .lastUpdated(Instant.parse(it.getString(3))).build()
                    def config = SourceArtifactConfig.builder()
                            .endpoint(it.getBoolean(4))
                            .subscribeAutomatically(it.getBoolean(5))
                            .forceSubscribe(it.getBoolean(6))
                            .moduleName(it.getString(7))
                            .component(it.getString(8))
                            .endpointName(it.getString(9))
                    if (it.getString(10)) {
                        config.addEndpointIds(it.getString(10)[1..-2].split(","))
                    }
                    def status = SourceArtifactStatus.builder()
                            .activelyFailing(it.getBoolean(11))
                            .latestFailedServiceInstance(it.getString(12))

                    artifacts.add(artifact.withConfig(config.build()).withStatus(status.build()))
                }
                handler.handle(Future.succeededFuture(artifacts))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getApplicationArtifacts(String appUuid, Handler<AsyncResult<List<SourceArtifact>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        client.queryWithParams(GET_APPLICATION_ARTIFACTS, params, {
            if (it.succeeded()) {
                def artifacts = new ArrayList<SourceArtifact>()
                def results = it.result().results
                results.each {
                    def artifact = SourceArtifact.builder()
                            .appUuid(it.getString(0))
                            .artifactQualifiedName(it.getString(1))
                            .createDate(Instant.parse(it.getString(2)))
                            .lastUpdated(Instant.parse(it.getString(3))).build()
                    def config = SourceArtifactConfig.builder()
                            .endpoint(it.getBoolean(4))
                            .subscribeAutomatically(it.getBoolean(5))
                            .forceSubscribe(it.getBoolean(6))
                            .moduleName(it.getString(7))
                            .component(it.getString(8))
                            .endpointName(it.getString(9))
                    if (it.getString(10)) {
                        config.addEndpointIds(it.getString(10)[1..-2].split(","))
                    }
                    def status = SourceArtifactStatus.builder()
                            .activelyFailing(it.getBoolean(11))
                            .latestFailedServiceInstance(it.getString(12))

                    artifacts.add(artifact.withConfig(config.build()).withStatus(status.build()))
                }
                handler.handle(Future.succeededFuture(artifacts))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getArtifactSubscriptions(String appUuid, String artifactQualifiedName,
                                  Handler<AsyncResult<List<SourceArtifactSubscription>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        params.add(artifactQualifiedName)
        client.queryWithParams(GET_ARTIFACT_SUBSCRIPTIONS, params, {
            if (it.succeeded()) {
                def subscriptions = new HashMap<String, SourceArtifactSubscription.Builder>()
                def results = it.result().results
                results.each {
                    def key = it.getString(0) + "-" + it.getString(1) + "-" + it.getString(2)
                    def subscription = SourceArtifactSubscription.builder()
                            .subscriberUuid(it.getString(0))
                            .appUuid(it.getString(1))
                            .artifactQualifiedName(it.getString(2))
                    subscriptions.putIfAbsent(key, subscription)
                    subscriptions.get(key).putSubscriptionLastAccessed(SourceArtifactSubscriptionType.valueOf(
                            it.getString(3)), it.getInstant(4))
                }
                handler.handle(Future.succeededFuture(subscriptions.values().collect { it.build() }))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getSubscriberArtifactSubscriptions(String subscriberUuid, String appUuid,
                                            Handler<AsyncResult<List<SourceArtifactSubscription>>> handler) {
        def params = new JsonArray()
        params.add(subscriberUuid)
        params.add(appUuid)
        client.queryWithParams(GET_SUBSCRIBER_ARTIFACT_SUBSCRIPTIONS, params, {
            if (it.succeeded()) {
                def subscriptions = new HashMap<String, SourceArtifactSubscription.Builder>()
                def results = it.result().results
                results.each {
                    def key = it.getString(0) + "-" + it.getString(1) + "-" + it.getString(2)
                    def subscription = SourceArtifactSubscription.builder()
                            .subscriberUuid(it.getString(0))
                            .appUuid(it.getString(1))
                            .artifactQualifiedName(it.getString(2))
                    subscriptions.putIfAbsent(key, subscription)
                    subscriptions.get(key).putSubscriptionLastAccessed(SourceArtifactSubscriptionType.valueOf(
                            it.getString(3)), it.getInstant(4))
                }
                handler.handle(Future.succeededFuture(subscriptions.values().collect { it.build() }))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getArtifactSubscriptions(Handler<AsyncResult<List<SourceArtifactSubscription>>> handler) {
        client.query(GET_ALL_ARTIFACT_SUBSCRIPTIONS, {
            if (it.succeeded()) {
                def subscriptions = new HashMap<String, SourceArtifactSubscription.Builder>()
                def results = it.result().results
                results.each {
                    def key = it.getString(0) + "-" + it.getString(1) + "-" + it.getString(2)
                    def subscription = SourceArtifactSubscription.builder()
                            .subscriberUuid(it.getString(0))
                            .appUuid(it.getString(1))
                            .artifactQualifiedName(it.getString(2))
                    subscriptions.putIfAbsent(key, subscription)
                    subscriptions.get(key).putSubscriptionLastAccessed(SourceArtifactSubscriptionType.valueOf(
                            it.getString(3)), it.getInstant(4))
                }
                handler.handle(Future.succeededFuture(subscriptions.values().collect { it.build() }))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void updateArtifactSubscription(SourceArtifactSubscription subscription,
                                    Handler<AsyncResult<SourceArtifactSubscription>> handler) {
        def futures = []
        subscription.subscriptionLastAccessed().each {
            def future = Promise.promise()
            futures.add(future)

            def params = new JsonArray()
            params.add(subscription.subscriberUuid())
            params.add(subscription.appUuid())
            params.add(subscription.artifactQualifiedName())
            params.add(it.key.toString())
            params.add(it.value)
            client.updateWithParams(UPDATE_ARTIFACT_SUBSCRIPTION, params, future)
        }
        CompositeFuture.all(futures).onComplete({
            if (it.succeeded()) {
                getArtifactSubscription(subscription.subscriberUuid(), subscription.appUuid(),
                        subscription.artifactQualifiedName(), handler)
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void deleteArtifactSubscription(SourceArtifactSubscription subscription,
                                    Handler<AsyncResult<Void>> handler) {
        def params = new JsonArray()
        params.add(subscription.subscriberUuid())
        params.add(subscription.appUuid())
        params.add(subscription.artifactQualifiedName())
        client.updateWithParams(DELETE_ARTIFACT_SUBSCRIPTION, params, {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture())
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void setArtifactSubscription(SourceArtifactSubscription subscription,
                                 Handler<AsyncResult<SourceArtifactSubscription>> handler) {
        deleteArtifactSubscription(subscription, {
            if (it.succeeded()) {
                updateArtifactSubscription(subscription, handler)
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getArtifactSubscription(String subscriberUuid, String appUuid,
                                 String artifactQualifiedName,
                                 Handler<AsyncResult<Optional<SourceArtifactSubscription>>> handler) {
        def params = new JsonArray()
        params.add(subscriberUuid)
        params.add(appUuid)
        params.add(artifactQualifiedName)
        client.queryWithParams(GET_SUBSCRIBER_ARTIFACT_SUBSCRIPTION, params, {
            if (it.succeeded()) {
                def subscriptions = new HashMap<String, SourceArtifactSubscription.Builder>()
                def results = it.result().results
                results.each {
                    def key = it.getString(0) + "-" + it.getString(1) + "-" + it.getString(2)
                    def subscription = SourceArtifactSubscription.builder()
                            .subscriberUuid(it.getString(0))
                            .appUuid(it.getString(1))
                            .artifactQualifiedName(it.getString(2))
                    subscriptions.putIfAbsent(key, subscription)
                    subscriptions.get(key).putSubscriptionLastAccessed(SourceArtifactSubscriptionType.valueOf(
                            it.getString(3)), it.getInstant(4))
                }

                def value = subscriptions.values().collect { it.build() }
                if (value) {
                    handler.handle(Future.succeededFuture(Optional.of(value.get(0))))
                } else {
                    handler.handle(Future.succeededFuture(Optional.empty()))
                }
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getApplicationSubscriptions(String appUuid,
                                     Handler<AsyncResult<List<SourceApplicationSubscription>>> handler) {
        def params = new JsonArray()
        params.add(appUuid)
        client.queryWithParams(GET_APPLICATION_SUBSCRIPTIONS, params, {
            if (it.succeeded()) {
                def results = it.result().results
                def subscriptionCounts = new HashMap<String, Set<String>>()
                def applicationSubscriptions = new HashMap<String, SourceApplicationSubscription.Builder>()
                results.each {
                    def subscription = SourceArtifactSubscription.builder()
                            .subscriberUuid(it.getString(0))
                            .appUuid(it.getString(1))
                            .artifactQualifiedName(it.getString(2))
                            .putSubscriptionLastAccessed(SourceArtifactSubscriptionType.valueOf(it.getString(3)), it.getInstant(4))
                            .build()
                    subscriptionCounts.putIfAbsent(subscription.artifactQualifiedName(), new HashSet<>())
                    applicationSubscriptions.putIfAbsent(subscription.artifactQualifiedName(),
                            SourceApplicationSubscription.builder().artifactQualifiedName(subscription.artifactQualifiedName()))
                    def appSubscription = applicationSubscriptions.get(subscription.artifactQualifiedName())
                    def subscribers = subscriptionCounts.get(subscription.artifactQualifiedName())
                    subscribers.add(subscription.subscriberUuid())
                    appSubscription.subscribers(subscribers.size())
                    appSubscription.addAllTypes(subscription.subscriptionLastAccessed().keySet())
                }
                handler.handle(Future.succeededFuture(applicationSubscriptions.values().collect { it.build() }))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void addArtifactFailure(SourceArtifact artifact, TraceSpan failingSpan, Handler<AsyncResult<Void>> handler) {
        def params = new JsonArray()
        params.add(artifact.appUuid())
        params.add(artifact.artifactQualifiedName())
        params.add(failingSpan.traceId())
        params.add(Instant.ofEpochMilli(failingSpan.startTime()))
        params.add(failingSpan.endTime() - failingSpan.startTime())

        client.updateWithParams(ADD_ARTIFACT_FAILURE, params, {
            if (it.succeeded()) {
                handler.handle(Future.succeededFuture())
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void getArtifactFailures(SourceArtifact artifact, TraceQuery traceQuery,
                             Handler<AsyncResult<List<Trace>>> handler) {
        def params = new JsonArray()
        params.add(artifact.appUuid())
        params.add(artifact.artifactQualifiedName())
        client.queryWithParams(GET_ARTIFACT_FAILURES, params, {
            if (it.succeeded()) {
                def artifactFailureTraces = new ArrayList<Trace>()
                def results = it.result().results
                results.each {
                    artifactFailureTraces.add(Trace.builder()
                            .addOperationNames(it.getString(0))
                            .addTraceIds(it.getString(1))
                            .start(it.getInstant(2).toEpochMilli())
                            .duration(it.getInteger(3))
                            .isError(true)
                            .isPartial(true).build()
                    )
                }
                handler.handle(Future.succeededFuture(artifactFailureTraces))
            } else {
                handler.handle(Future.failedFuture(it.cause()))
            }
        })
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void refreshDatabase(Handler<AsyncResult<Void>> handler) {
        handler.handle(Future.succeededFuture())
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean needsManualRefresh() {
        return false
    }
}
