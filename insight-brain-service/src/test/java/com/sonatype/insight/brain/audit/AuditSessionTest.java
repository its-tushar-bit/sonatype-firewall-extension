/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class AuditSessionTest
{
  @Before
  public void before() {
    AuditData.set(null);
  }

  @Test
  public void testClose_commit() {
    AuditData auditData = spy(AuditData.get());
    try (AuditSession auditSession = new AuditSession(auditData)) {
      // noop
    }
    verify(auditData).commit();
  }

  @Test
  public void testClose_nesting_behavior() {
    AuditData.set(mock(AuditData.class));
    AuditData auditData1 = AuditData.get();

    AuditData auditData2 = mock(AuditData.class);
    try (AuditSession auditSession = new AuditSession(auditData2)) {
      assertThat(AuditData.get(), is(auditData2));
    }
    assertThat(AuditData.get(), is(auditData1));
  }
}
