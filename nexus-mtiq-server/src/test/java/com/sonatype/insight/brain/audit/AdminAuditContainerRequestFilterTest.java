/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import static com.sonatype.insight.brain.api.admin.authorization.AuthContextProperties.SUBJECT_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class AdminAuditContainerRequestFilterTest
{
  @Rule
  public TestAuditSession testAuditSession = new TestAuditSession();

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private ResourceInfo mockResourceInfo;

  @Mock
  private ContainerRequestContext mockContainerRequestContext;

  @Mock(answer = Answers.CALLS_REAL_METHODS)
  private AuditData mockAuditData;

  @Captor
  private ArgumentCaptor<String> auditedUserNameCaptor;

  private AdminAuditContainerRequestFilter underTest;

  @Before
  public void before() {
    testAuditSession.set(mockAuditData);
    underTest = new AdminAuditContainerRequestFilter(mockResourceInfo);
  }

  @Test
  public void testFilter_NullMethod_DoesNothing() {
    underTest.filter(mockContainerRequestContext);

    verify(mockAuditData, never()).setEvent(any());
  }

  @Test
  public void testFilter_AuditedMethod_SetsEvent() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));

    underTest.filter(mockContainerRequestContext);

    verify(mockAuditData).setEvent(AuditEvent.AUTHENTICATION_FAILURE);
  }

  @Test
  public void testFilter_NonAuditedMethod_DoesNothing() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("notAudited"));

    underTest.filter(mockContainerRequestContext);

    verify(mockAuditData, never()).setEvent(any());
    verify(mockAuditData, never()).setUsername(any());
  }

  @Test
  public void testFilter_AuditedMethod_ProxySubclass_SetsEvent() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTestProxy$$.class.getMethod("audited"));
    when(mockContainerRequestContext.getProperty(SUBJECT_USER)).thenReturn("test@test.com");

    underTest.filter(mockContainerRequestContext);

    verify(mockAuditData).setEvent(AuditEvent.AUTHENTICATION_FAILURE);
    verify(mockAuditData).setUsername(auditedUserNameCaptor.capture());
    assertThat(auditedUserNameCaptor.getValue()).isEqualTo("test@test.com");
  }

  @Test(expected = IllegalStateException.class)
  public void testFilter_NoAuditedMethod_ProxySubclass_ThrowsException() throws Exception {
    when(mockResourceInfo.getResourceMethod())
        .thenReturn(AuditedAnnotationTestProxy$$.class.getMethod("onlyInAuditedAnnotationTestProxy$$"));

    underTest.filter(mockContainerRequestContext);
  }

  private static class AuditedAnnotationTest
  {
    @Audited(value = AuditEvent.AUTHENTICATION_FAILURE)
    public void audited() {
    }

    @SuppressWarnings("unused")
    public void notAudited() {
    }
  }

  private static class AuditedAnnotationTestProxy$$
      extends AuditedAnnotationTest
  {
    @Override
    public void audited() {
    }

    @SuppressWarnings("unused")
    public void onlyInAuditedAnnotationTestProxy$$() {
    }

    @Override
    public void notAudited() {
    }
  }
}
