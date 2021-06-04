/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.dropwizard.jetty.HttpConnectorFactory;

/**
 * Custom {@link HttpConnectorFactory} with our default port and idle timeout.
 */
@JsonDeserialize(as = InsightHttpConnectorFactory.class)
public class InsightHttpConnectorFactory
    extends HttpConnectorFactory
{
  public static class Module
      extends SimpleModule
  {
    private static final long serialVersionUID = 8721875650179228799L;

    public Module() {
      setMixInAnnotation(HttpConnectorFactory.class, InsightHttpConnectorFactory.class);
    }
  }

  public InsightHttpConnectorFactory() {
    this(InsightConfigurationFactory.DEFAULT_APPLICATION_PORT);
  }

  public InsightHttpConnectorFactory(int port) {
    setPort(port);
    setIdleTimeout(InsightConfigurationFactory.DEFAULT_IDLE_TIMEOUT);
    setUseForwardedHeaders(true);
  }
}
