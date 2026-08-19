/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.service.TestInsightBrainService;

/**
 * Factory for creating test instances of InsightBrainService.
 * Now uses Spring Boot-based SpringTestInsightBrainService instead of Dropwizard.
 */
public class DefaultInsightBrainServiceFactory
    implements InsightBrainServiceFactory
{
  @Override
  public TestInsightBrainService createTestInsightBrainService() {
    return new SpringTestInsightBrainService();
  }
}
