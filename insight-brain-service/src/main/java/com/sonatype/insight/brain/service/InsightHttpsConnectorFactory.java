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
 * <p>
 * This is because @JsonDeserialize(as = InsightHttpConnectorFactory.class) is added to HttpConnectorFactory and since
 * HttpConnectorFactory is the superclass of HttpsConnectorFactory, then it will try to deserialize
 * HttpsConnectorFactory as InsightHttpConnectorFactory, which fails since InsightHttpConnectorFactory extends
 * HttpConnectorFactory.
 * <p>
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

    /*
     * In Jetty 10 a number of security improvements were made including better checks of SNI Host names
     * https://github.com/jetty/jetty.project/issues/5379. Modern browsers will provide the SNI Host name but there is
     * no guarantee that our integrations will. Disabling for now to prevent issues.This blog post helps explain the
     * change: https://peterobrien.blog/2024/02/29/invalid-sni-what-is-it-and-how-to-fix-it/
     */
    setDisableSniHostCheck(true);
  }
}
