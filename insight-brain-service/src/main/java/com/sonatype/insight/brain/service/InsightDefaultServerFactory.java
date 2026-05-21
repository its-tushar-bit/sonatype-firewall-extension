/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Collections;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.dropwizard.core.server.DefaultServerFactory;

/**
 * Custom {@link DefaultServerFactory} with updated defaults. We used to set them externally in InsightConfig, but if
 * someone chose to customize one of the properties then the newly deserialized class would not include our changes.
 * Setting them in the constructor means they always get applied first. Uses mixin to apply "JsonDeserialize.as".
 */
@JsonDeserialize(as = InsightDefaultServerFactory.class)
public class InsightDefaultServerFactory
    extends DefaultServerFactory
{
  public static class Module
      extends SimpleModule
  {
    private static final long serialVersionUID = 7897301364271583290L;

    public Module() {
      // makes it look like JsonDeserialize.as was on original class
      setMixInAnnotation(DefaultServerFactory.class, InsightDefaultServerFactory.class);
    }
  }

  public InsightDefaultServerFactory() {
    setRegisterDefaultExceptionMappers(false);
    setEnableVirtualThreads(true);
    setEnableAdminVirtualThreads(true);

    setApplicationConnectors(Collections
        .singletonList(new InsightHttpConnectorFactory(InsightConfigurationFactory.DEFAULT_APPLICATION_PORT)));
    setAdminConnectors(
        Collections.singletonList(new InsightHttpConnectorFactory(InsightConfigurationFactory.DEFAULT_ADMIN_PORT)));
  }
}
