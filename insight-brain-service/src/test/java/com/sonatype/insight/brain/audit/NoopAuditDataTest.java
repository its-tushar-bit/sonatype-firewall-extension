/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NoopAuditDataTest
{
  @Test
  public void testContinueAsync() {
    assertThat(NoopAuditData.INSTANCE.continueAsync((Function<AuditData, AuditData>) auditData -> auditData))
        .isEqualTo(NoopAuditData.INSTANCE);
  }

  @Test
  public void testForSubEvent() {
    assertThat(NoopAuditData.INSTANCE.forSubEvent(null, false, false)).isEqualTo(NoopAuditData.INSTANCE);
  }
}
