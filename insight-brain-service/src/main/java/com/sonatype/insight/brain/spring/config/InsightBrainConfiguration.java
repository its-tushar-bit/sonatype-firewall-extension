/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.security.SecurityAopConfiguration;
import com.sonatype.insight.brain.security.ShiroAuthenticatorConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Main Spring configuration that imports all module configurations.
 * <p>
 * This consolidates the Spring configuration classes that make up the application.
 * <p>
 * Individual configurations:
 * <ul>
 * <li>{@link DropwizardConfigConfiguration} - Loads config.yml into InsightConfig</li>
 * <li>{@link CoreConfiguration} - MetricRegistry, ObjectMapper</li>
 * <li>{@link DatabaseConfiguration} - DatabaseContainer, DataStores</li>
 * <li>{@link JerseyConfiguration} - JAX-RS servlet registration</li>
 * <li>{@link SecurityConfiguration} - Shiro security manager</li>
 * <li>{@link SearchConfiguration} - Lucene/OpenSearch clients</li>
 * <li>{@link ScheduledConfiguration} - Quartz scheduler</li>
 * <li>{@link JooqConfiguration} - jOOQ DSLContext</li>
 * <li>{@link WebConfiguration} - Static resources</li>
 * <li>{@link FilterConfiguration} - Servlet filters</li>
 * </ul>
 */
@Configuration
@Import({
  DropwizardConfigConfiguration.class,
  CoreConfiguration.class,
  DatabaseConfiguration.class,
  JerseyConfiguration.class,
  SecurityConfiguration.class,
  SearchConfiguration.class,
  ScheduledConfiguration.class,
  JooqConfiguration.class,
  WebConfiguration.class,
  FilterConfiguration.class,
  ShiroAuthenticatorConfiguration.class,
  SecurityAopConfiguration.class
})
public class InsightBrainConfiguration
{
}
