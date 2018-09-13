/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuditContainerRequestFilterTest
{
  @Mock
  private ResourceInfo mockResourceInfo;

  @Mock
  private ContainerRequestContext containerRequestContext;

  @Mock
  private AuditData mockAuditData;

  private AuditContainerRequestFilter auditContainerRequestFilter;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    AuditData.instance.remove();
    AuditData.set(mockAuditData);
    auditContainerRequestFilter = new AuditContainerRequestFilter(mockResourceInfo);
  }

  @Test
  public void testFilter_NullMethod_DoesNothing() {
    auditContainerRequestFilter.filter(containerRequestContext);

    verify(mockAuditData, never()).setEvent(any());
  }

  @Test
  public void testFilter_AuditedMethod_SetsEvent() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("audited"));

    auditContainerRequestFilter.filter(containerRequestContext);

    verify(mockAuditData).setEvent(AuditEvent.AUTHENTICATION_FAILURE);
  }

  @Test
  public void testFilter_NonAuditedMethod_DoesNothing() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTest.class.getMethod("notAudited"));

    auditContainerRequestFilter.filter(containerRequestContext);

    verify(mockAuditData, never()).setEvent(any());
  }

  @Test
  public void testFilter_AuditedMethod_Guice_SetsEvent() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(AuditedAnnotationTestGuice$$.class.getMethod("audited"));

    auditContainerRequestFilter.filter(containerRequestContext);

    verify(mockAuditData).setEvent(AuditEvent.AUTHENTICATION_FAILURE);
  }

  @Test(expected = IllegalStateException.class)
  public void testFilter_NoAuditedMethod_Guice_ThrowsException() throws Exception {
    when(mockResourceInfo.getResourceMethod())
        .thenReturn(AuditedAnnotationTestGuice$$.class.getMethod("onlyInAuditedAnnotationTestGuice$$"));

    auditContainerRequestFilter.filter(containerRequestContext);
  }

  @Test
  public void testPriority_IsPresent() {
    Priority priority = AuditContainerRequestFilter.class.getAnnotation(Priority.class);

    assertThat(priority.value(), lessThan(Priorities.AUTHENTICATION));
  }

  private static class AuditedAnnotationTest
  {
    @Audited(value = AuditEvent.AUTHENTICATION_FAILURE)
    public void audited() { }

    public void notAudited() { }
  }

  private static class AuditedAnnotationTestGuice$$
      extends AuditedAnnotationTest
  {
    @Override
    public void audited() { }

    public void onlyInAuditedAnnotationTestGuice$$() { }

    @Override
    public void notAudited() { }
  }
}
