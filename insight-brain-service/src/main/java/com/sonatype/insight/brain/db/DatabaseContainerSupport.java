/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import com.sonatype.insight.brain.service.InsightConfig;

/**
 * Interface for classes that need database support.
 * In Spring Boot, database initialization is handled via configuration classes.
 */
public interface DatabaseContainerSupport
{
  /**
   * Create the {@link DatabaseContainer} for the application.
   */
  DatabaseContainer createDatabaseContainer(final InsightConfig insightConfig);
}
