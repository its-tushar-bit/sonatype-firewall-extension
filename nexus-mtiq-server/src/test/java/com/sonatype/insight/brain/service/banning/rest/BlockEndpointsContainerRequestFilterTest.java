/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.banning.BlockIfMultiTenant;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;

public class BlockEndpointsContainerRequestFilterTest
{
  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Mock
  private ResourceInfo mockResourceInfo;

  @Mock
  private ContainerRequestContext mockContainerRequestContext;

  @Captor
  private ArgumentCaptor<Response> responseCaptor;

  private BlockEndpointsContainerRequestFilter underTest;

  @Before
  public void before() {
    underTest = new BlockEndpointsContainerRequestFilter(mockResourceInfo);
  }

  @Test
  public void testFilter_DoesNothing_WhenClassOrMethodsNotAnnotated() {
    underTest.filter(mockContainerRequestContext);

    verify(mockContainerRequestContext, never()).abortWith(any());
  }

  @Test
  public void testFilter_MethodAnnotated_SendsNotFoundResponse() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(BlockedEndpointTest.class.getMethod("blocked"));

    underTest.filter(mockContainerRequestContext);

    assertNotFoundResponseIsSent();
  }

  @Test
  public void testFilter_MethodNotAnnotated_DoesNothing() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(BlockedEndpointTest.class.getMethod("notBlocked"));

    underTest.filter(mockContainerRequestContext);

    verify(mockContainerRequestContext, never()).abortWith(any());
  }

  @Test
  public void testFilter_MethodAnnotated_ProxySubclass_SendsNotFoundResponse() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(BlockedEndpointTestProxy$$.class.getMethod("blocked"));

    underTest.filter(mockContainerRequestContext);

    assertNotFoundResponseIsSent();
  }

  @Test(expected = IllegalStateException.class)
  public void testFilter_MethodNotAnnotated_ProxySubclass_ThrowsException() throws Exception {
    when(mockResourceInfo.getResourceMethod()).thenReturn(
        BlockedEndpointTestProxy$$.class.getMethod("onlyInBlockedAnnotationTestProxy"));

    underTest.filter(mockContainerRequestContext);
  }

  @Test
  public void testFilter_ClassAnnotated_SendsNotFoundResponse() {
    when(mockResourceInfo.getResourceClass()).thenAnswer(invocationOnMock -> BlockedClassTest.class);

    underTest.filter(mockContainerRequestContext);

    assertNotFoundResponseIsSent();
  }

  @Test
  public void testFilter_ClassNotAnnotated_DoesNothing() {
    when(mockResourceInfo.getResourceClass()).thenAnswer(invocationOnMock -> NonBlockedClassTest.class);

    underTest.filter(mockContainerRequestContext);

    verify(mockContainerRequestContext, never()).abortWith(any());
  }

  @Test
  public void testFilter_ClassAnnotated_ProxySubclass_SendsNotFoundResponse() {
    when(mockResourceInfo.getResourceClass()).thenAnswer(invocationOnMock -> BlockedClassTestProxy$$.class);

    underTest.filter(mockContainerRequestContext);

    assertNotFoundResponseIsSent();
  }

  private void assertNotFoundResponseIsSent() {
    verify(mockContainerRequestContext).abortWith(responseCaptor.capture());
    assertThat(responseCaptor.getValue()).isNotNull();
    assertThat(responseCaptor.getValue().getStatusInfo().toEnum()).isEqualTo(Status.NOT_FOUND);
  }

  private static class BlockedEndpointTest
  {
    @BlockIfMultiTenant
    public void blocked() {
    }

    @SuppressWarnings("unused")
    public void notBlocked() {
    }
  }

  private static class BlockedEndpointTestProxy$$
      extends BlockedEndpointTest
  {
    @Override
    public void blocked() {
    }

    @SuppressWarnings("unused")
    public void onlyInBlockedAnnotationTestProxy() {
    }
  }

  @BlockIfMultiTenant
  private static class BlockedClassTest
  {
    @SuppressWarnings("unused")
    public void blocked() {
    }
  }

  private static class BlockedClassTestProxy$$
      extends BlockedClassTest
  {
    @Override
    // unused
    public void blocked() {
    }
  }

  private static class NonBlockedClassTest
  {
    @SuppressWarnings("unused")
    public void notBlocked() {
    }
  }
}
