/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.common.test;

import java.util.HashMap;
import java.util.Map;

public interface InsightTestFixture
    extends AutoCloseable
{
  /**
   * Indicates if the fixture can be re-used by the next test or if it should be deleted and re-initialized
   */
  boolean isFixtureReusable();

  default Map<String, Object> getMetadata() {
    return new HashMap<>();
  }
}
