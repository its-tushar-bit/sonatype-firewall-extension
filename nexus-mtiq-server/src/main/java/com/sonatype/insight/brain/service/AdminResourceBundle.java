/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Admin resource registry for MTIQ admin endpoints.
 * Manages registration of admin resources in Spring Boot/Jersey context.
 */
public class AdminResourceBundle
{
  private static final Logger log = LoggerFactory.getLogger(AdminResourceBundle.class);

  private final String basePath;

  private final Set<Object> registeredResources = new LinkedHashSet<>();

  public AdminResourceBundle(String basePath) {
    this.basePath = basePath;
  }

  public String getBasePath() {
    return basePath;
  }

  /**
   * Register a resource instance with the admin bundle.
   *
   * @param resource the resource to register
   */
  public void register(Object resource) {
    registeredResources.add(resource);
    log.debug("Registered admin resource: {}", resource.getClass().getName());
  }

  /**
   * Configure the Jersey ResourceConfig with all registered resources.
   *
   * @param resourceConfig the Jersey resource config to configure
   */
  public void configure(ResourceConfig resourceConfig) {
    resourceConfig.register(MultiPartFeature.class);
    for (Object resource : registeredResources) {
      resourceConfig.register(resource);
      log.info("Registered admin resource: {}", resource.getClass().getName());
    }
  }

  /**
   * Get all registered resource instances.
   *
   * @return set of registered resources
   */
  public Set<Object> getRegisteredResources() {
    return Collections.unmodifiableSet(registeredResources);
  }
}
