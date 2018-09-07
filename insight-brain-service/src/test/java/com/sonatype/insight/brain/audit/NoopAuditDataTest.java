/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class NoopAuditDataTest
{
  @Test
  public void testContinueAsync() {
    assertThat(NoopAuditData.INSTANCE.continueAsync(auditData -> auditData), is(NoopAuditData.INSTANCE));
  }

  @Test
  public void testForSubEvent() {
    assertThat(NoopAuditData.INSTANCE.forSubEvent(null, false), is(NoopAuditData.INSTANCE));
  }
}
