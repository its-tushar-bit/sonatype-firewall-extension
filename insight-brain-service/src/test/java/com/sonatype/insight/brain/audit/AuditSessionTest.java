/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AuditSessionTest
{
  @RegisterExtension
  public TestAuditSession testAuditSession = new TestAuditSession();

  @Test
  public void testConstructor_RejectNullData() {
    assertThrows(NullPointerException.class, () -> {
      try (AuditSession auditSession = new AuditSession(null)) {
        // noop
      }
    });
  }

  @Test
  public void testConstructor_SetThreadLocal() {
    AuditData auditData = mock(AuditData.class);
    try (AuditSession auditSession = new AuditSession(auditData)) {
      assertThat(AuditSession.getCurrent()).isEqualTo(auditData);
    }
  }

  @Test
  public void testClose_CommitData() {
    AuditData auditData = mock(AuditData.class);
    try (AuditSession auditSession = new AuditSession(auditData)) {
      // noop
    }
    verify(auditData).commit();
  }

  @Test
  public void testClose_RestoreThreadLocal() {
    AuditData previous = mock(AuditData.class);
    testAuditSession.set(previous);
    try (AuditSession auditSession = new AuditSession(mock(AuditData.class))) {
      // noop
    }
    assertThat(AuditSession.getCurrent()).isEqualTo(previous);
  }
}
