/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.util.Duration;
import org.eclipse.jetty.server.HttpConfiguration;

/**
 * Custom {@link DefaultServerFactory} with updated defaults. We used to set them externally in InsightConfig, but if
 * someone chose to customize one of the properties then the newly deserialized class would not include our changes.
 * Setting them in the constructor means they always get applied first. Uses mixin to apply "JsonDeserialize.as".
 */
@JsonDeserialize(as = HttpConfig.class)
public class HttpConfig
    extends DefaultServerFactory
{
  public static class Module
      extends SimpleModule
  {
    private static final long serialVersionUID = 7897301364271583290L;

    public Module() {
      // makes it look like JsonDeserialize.as was on original class
      setMixInAnnotation(HttpConfiguration.class, HttpConfig.class);
    }
  }

  public HttpConfig() {
    setRegisterDefaultExceptionMappers(false);
    
    HttpConnectorFactory applicationConnector = (HttpConnectorFactory) getApplicationConnectors().get(0);
    applicationConnector.setPort(8070);
    applicationConnector.setIdleTimeout(Duration.minutes(15));
    HttpConnectorFactory adminConnector = (HttpConnectorFactory) getAdminConnectors().get(0);
    adminConnector.setPort(8071);
  }
}
