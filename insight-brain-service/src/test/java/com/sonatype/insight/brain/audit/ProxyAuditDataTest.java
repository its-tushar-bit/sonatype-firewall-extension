/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.function.Function;

import org.junit.Test;
import org.mockito.stubbing.Answer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProxyAuditDataTest
{
  @Test
  public void testContinueAsync() {
    Function<AuditData, AuditData> taskSubmitter = auditData -> auditData;
    AuditData auditData = mock(AuditData.class);
    ProxyAuditData proxyAuditData = new ProxyAuditData(auditData);

    AuditData childProxyAuditData = proxyAuditData.continueAsync(taskSubmitter);

    proxyAuditData.commit();
    verify(auditData, never()).commit();
    assertThat(childProxyAuditData, is(instanceOf(ProxyAuditData.class)));
    assertThat(childProxyAuditData, not(proxyAuditData));
    childProxyAuditData.commit();
    verify(auditData).commit();
  }

  @Test
  public void testForSubEvent() {
    AuditData mockParentAuditData = mock(AuditData.class);
    AuditData mockChildAuditData = mock(AuditData.class);
    String[] result = new String[1];
    doAnswer((Answer<Void>) invocation -> {
      result[0] = "result";
      return null;
    }).when(mockChildAuditData).commit();
    when(mockParentAuditData.forSubEvent(AuditEvent.AUTHENTICATION_FAILURE, false)).thenReturn(mockChildAuditData);
    ProxyAuditData proxyAuditData = new ProxyAuditData(mockParentAuditData);

    AuditData childProxyAuditData = proxyAuditData.forSubEvent(AuditEvent.AUTHENTICATION_FAILURE, false);

    assertThat(childProxyAuditData, is(instanceOf(ProxyAuditData.class)));
    assertThat(childProxyAuditData, not(proxyAuditData));
    verify(mockParentAuditData).forSubEvent(AuditEvent.AUTHENTICATION_FAILURE, false);
    childProxyAuditData.commit();
    assertThat(result[0], is("result"));
  }

  @Test(expected = NullPointerException.class)
  public void testForSubEvent_Null() {
    new ProxyAuditData(mock(AuditData.class)).forSubEvent(null, false);
  }

  @Test
  public void testCommit() {
    AuditData auditData = mock(AuditData.class);
    ProxyAuditData proxyAuditData = new ProxyAuditData(auditData);
    assertThat(proxyAuditData.getAuditData(), is(auditData));

    proxyAuditData.commit();

    verify(auditData).commit();
    assertThat(proxyAuditData.getAuditData(), is(NoopAuditData.INSTANCE));
  }

  @Test
  public void testCommitSubEvents() {
    AuditData auditData = mock(AuditData.class);
    ProxyAuditData proxyAuditData = new ProxyAuditData(auditData);

    proxyAuditData.commitSubEvents();

    verify(auditData).commitSubEvents();
    assertThat(proxyAuditData.getAuditData(), is(auditData));
  }
}
