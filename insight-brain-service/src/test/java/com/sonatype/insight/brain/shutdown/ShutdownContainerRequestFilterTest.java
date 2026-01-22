/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.shutdown;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShutdownContainerRequestFilterTest
    extends AbstractComponentTest
{
  @Inject
  private ShutdownContainerRequestFilter shutdownContainerRequestFilter;

  @Mock
  private ShutdownHandler mockShutdownHandler;

  @Mock
  private ContainerRequestContext mockContainerRequestContext;

  @Captor
  private ArgumentCaptor<Response> responseArgumentCaptor;

  @Override
  public void configure(final Binder binder) {
    super.configure(binder);
    binder.bind(ShutdownHandler.class).toInstance(mockShutdownHandler);
  }

  @Test
  public void testFilter_AfterGracePeriod() throws Exception {
    when(mockShutdownHandler.isAfterGracePeriod()).thenReturn(true);

    shutdownContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockContainerRequestContext).abortWith(responseArgumentCaptor.capture());
    Response response = responseArgumentCaptor.getValue();
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(Status.SERVICE_UNAVAILABLE.getStatusCode());
  }

  @Test
  public void testFilter_BeforeGracePeriod() throws Exception {
    when(mockShutdownHandler.isAfterGracePeriod()).thenReturn(false);

    shutdownContainerRequestFilter.filter(mockContainerRequestContext);

    verify(mockContainerRequestContext, never()).abortWith(any());
  }
}
