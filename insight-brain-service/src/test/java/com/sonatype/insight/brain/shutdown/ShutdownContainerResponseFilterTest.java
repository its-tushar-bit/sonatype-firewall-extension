/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.Test;
import org.mockito.Mock;

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
