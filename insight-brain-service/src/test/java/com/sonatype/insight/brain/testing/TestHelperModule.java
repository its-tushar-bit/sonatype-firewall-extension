/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.PolicyEvaluationHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * Spring configuration providing test helper components.
 * This module binds test-only components that cannot be bound in production modules.
 */
@TestConfiguration
public class TestHelperModule
{
  @Bean
  public PolicyEvaluationHelper policyEvaluationHelper() {
    return new PolicyEvaluationHelper();
  }
}
