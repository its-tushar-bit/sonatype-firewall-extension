/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.search.SearchConfig;
import java.util.Map;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * Ensures Spring test contexts expose the legacy {@code search.type} property whenever a test
 * resolves a non-null {@link SearchConfig} through the fixture-aware test harness.
 */
public class SearchTestPropertyInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext>
{
  private static final String PROPERTY_SOURCE_NAME = "searchTestProperties";

  @Override
  public void initialize(final ConfigurableApplicationContext applicationContext) {
    SearchConfig searchConfig = SpringTestConfiguration.resolveSearchConfigForCurrentTest();
    if (searchConfig == null) {
      return;
    }

    applicationContext.getEnvironment()
        .getPropertySources()
        .addFirst(
            new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of("search.type", "test")));
  }
}
