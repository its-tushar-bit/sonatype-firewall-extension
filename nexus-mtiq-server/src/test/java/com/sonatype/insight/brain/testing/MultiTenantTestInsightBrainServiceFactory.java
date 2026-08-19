/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.service.SpringMultiTenantTestInsightBrainService;
import com.sonatype.insight.brain.service.TestInsightBrainService;

/**
 * Factory for creating multi-tenant test IQ server instances.
 * Uses Spring Boot-based implementation.
 */
public class MultiTenantTestInsightBrainServiceFactory
    implements InsightBrainServiceFactory
{
  @Override
  public TestInsightBrainService createTestInsightBrainService() {
    return new SpringMultiTenantTestInsightBrainService();
  }
}
