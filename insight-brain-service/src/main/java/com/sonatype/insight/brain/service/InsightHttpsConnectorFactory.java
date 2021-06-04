/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;

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
    setUseForwardedHeaders(true);
  }
}
