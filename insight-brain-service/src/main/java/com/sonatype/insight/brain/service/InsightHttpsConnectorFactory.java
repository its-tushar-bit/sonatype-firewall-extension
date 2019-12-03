/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.jetty.Jetty93InstrumentedConnectionFactory;
import io.dropwizard.jetty.SslReload;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import org.eclipse.jetty.util.thread.ThreadPool;

/**
 * Custom {@link HttpsConnectorFactory} that is necessary due to deserializing {@link HttpConnectorFactory} as
 * {@link InsightHttpConnectorFactory}.
 *
 * This is because @JsonDeserialize(as = InsightHttpConnectorFactory.class) is added to HttpConnectorFactory and since
 * HttpConnectorFactory is the superclass of HttpsConnectorFactory, then it will try to deserialize
 * HttpsConnectorFactory as InsightHttpConnectorFactory, which fails since InsightHttpConnectorFactory extends
 * HttpConnectorFactory.
 *
 * By using this class it will deserialize HttpsConnectorFactory as InsightHttpsConnectorFactory, which succeeds since
 * InsightHttpsConnectorFactory extends HttpsConnectorFactory.
 */
@JsonDeserialize(as = InsightHttpsConnectorFactory.class)
public class InsightHttpsConnectorFactory
    extends HttpsConnectorFactory
{
  public static class Module
      extends SimpleModule
  {
    private static final long serialVersionUID = -2910685634440587992L;

    public Module() {
      setMixInAnnotation(HttpsConnectorFactory.class, InsightHttpsConnectorFactory.class);
    }
  }

  public InsightHttpsConnectorFactory() {
    setIdleTimeout(InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
  }

  @Override
  public Connector build(Server server, MetricRegistry metrics, String name, ThreadPool threadPool) {
    final HttpConfiguration httpConfig = buildHttpConfiguration();

    final HttpConnectionFactory httpConnectionFactory = buildHttpConnectionFactory(httpConfig);

    // NOTE: The use of SslContextFactory.Server here is the only change that prevents just using super.build() 
    final SslContextFactory sslContextFactory = configureSslContextFactory(new SslContextFactory.Server());
    sslContextFactory.addLifeCycleListener(logSslInfoOnStart(sslContextFactory));

    server.addBean(sslContextFactory);
    server.addBean(new SslReload(sslContextFactory, this::configureSslContextFactory));

    final SslConnectionFactory sslConnectionFactory =
        new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.toString());

    final Scheduler scheduler = new ScheduledExecutorScheduler();

    final ByteBufferPool bufferPool = buildBufferPool();

    return buildConnector(server, scheduler, bufferPool, name, threadPool,
        new Jetty93InstrumentedConnectionFactory(sslConnectionFactory, metrics.timer(httpConnections())),
        httpConnectionFactory);
  }
}
