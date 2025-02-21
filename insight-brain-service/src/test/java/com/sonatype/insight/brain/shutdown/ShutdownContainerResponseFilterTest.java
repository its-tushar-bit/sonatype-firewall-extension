/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ShutdownContainerResponseFilterTest
    extends AbstractComponentTest
{
  @Inject
  private ShutdownContainerResponseFilter shutdownContainerResponseFilter;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Mock
  private ContainerRequestContext mockContainerRequestContext;

  @Mock
  private ContainerResponseContext mockContainerResponseContext;

  @Override
  public void configure(final Binder binder) {
    super.configure(binder);
    binder.bind(ShutdownHandler.class).toInstance(mockShutdownHandler);
  }

  @Test
  public void testFilter_ShutdownTriggered() throws Exception {
    when(mockShutdownHandler.isTriggered()).thenReturn(true);
    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    when(mockContainerResponseContext.getHeaders()).thenReturn(headers);

    shutdownContainerResponseFilter.filter(mockContainerRequestContext, mockContainerResponseContext);
    assertThat(headers.getFirst("Connection")).isEqualTo("close");
  }

  @Test
  public void testFilter_NoShutdownTriggered() throws Exception {
    when(mockShutdownHandler.isTriggered()).thenReturn(false);
    MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    when(mockContainerResponseContext.getHeaders()).thenReturn(headers);

    shutdownContainerResponseFilter.filter(mockContainerRequestContext, mockContainerResponseContext);

    assertThat(mockContainerResponseContext.getHeaders()).doesNotContainKey("Connection");
  }
}
