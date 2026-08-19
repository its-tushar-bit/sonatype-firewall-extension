/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProxyAuditDataTest
{
  @Test
  public void testContinueAsync() {
    AuditData auditData = mock(AuditData.class);
    ProxyAuditData proxyAuditData = new ProxyAuditData(auditData);

    AuditData childProxyAuditData = proxyAuditData.continueAsync(Function.identity());

    proxyAuditData.commit();
    verify(auditData, never()).commit();
    assertThat(childProxyAuditData).isInstanceOf(ProxyAuditData.class);
    assertThat(childProxyAuditData).isNotEqualTo(proxyAuditData);
    childProxyAuditData.commit();
    verify(auditData).commit();
  }

  @Test
  public void testForSubEvent() {
    AuditData mockParentAuditData = mock(AuditData.class);
    AuditData mockChildAuditData = mock(AuditData.class);
    when(mockParentAuditData.forSubEvent(AuditEvent.LOGIN, true, false)).thenReturn(mockChildAuditData);
    ProxyAuditData proxyAuditData = new ProxyAuditData(mockParentAuditData);

    AuditData childProxyAuditData = proxyAuditData.forSubEvent(AuditEvent.LOGIN, true, false);
    assertThat(childProxyAuditData).isInstanceOf(ProxyAuditData.class);
    assertThat(childProxyAuditData).isNotEqualTo(proxyAuditData);

    childProxyAuditData.commit();
    verify(mockChildAuditData).commit();
  }

  @Test
  public void testForSubEvent_Null() {
    assertThrows(NullPointerException.class,
        () -> new ProxyAuditData(mock(AuditData.class)).forSubEvent(null, false, false));
  }

  @Test
  public void testCommit() {
    AuditData auditData = mock(AuditData.class);
    ProxyAuditData proxyAuditData = new ProxyAuditData(auditData);

    proxyAuditData.commit();
    verify(auditData).commit();

    proxyAuditData.commit();
    verify(auditData).commit();
  }

  @Test
  public void testCommitSubEvents() {
    AuditData auditData = mock(AuditData.class);
    ProxyAuditData proxyAuditData = new ProxyAuditData(auditData);

    proxyAuditData.commitSubEvents();
    verify(auditData).commitSubEvents();

    proxyAuditData.commitSubEvents();
    verify(auditData, times(2)).commitSubEvents();
  }
}
