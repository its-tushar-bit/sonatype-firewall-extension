/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal Spring test configuration for basic unit tests.
 * Provides only the essential beans needed for simple tests.
 *
 * <p>
 * For tests that need database access or full service injection, extend
 * BrainInjectedTest from insight-brain-service which provides database infrastructure.
 * </p>
 */
@Configuration
public class SpringTestConfiguration
{
  /**
   * Provide an ObjectMapper for tests.
   */
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
