/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.telemetry;

/**
 * Test probe woven by AspectJ CTW so {@link GuideUsageEventAspect} can be verified end-to-end.
 *
 * <p>
 * Lives in {@code src/main/java} (not {@code src/test/java}) because {@code aspectj-maven-plugin}
 * is configured to run only the {@code compile} goal &mdash; the {@code test-compile} goal is
 * disabled, so a probe under test sources would never be woven. Keeping it here is the minimum
 * cost to exercise the woven path; the class is otherwise unreferenced by production code.
 */
public class GuideUsageWeavingProbe
{
  @GuideUsageEvent(operationType = GuideOperationType.COMPONENT_LOOKUP)
  public String lookup(final String purl) {
    return purl;
  }
}
