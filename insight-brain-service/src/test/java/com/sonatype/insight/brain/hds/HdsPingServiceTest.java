/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadGatewayException;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class HdsPingServiceTest
    extends AbstractComponentTest
{
  @Inject
  private HdsPingService hdsPingService;

  @Mock
  private PingHdsClient pingHdsClientMock;

  @Override
  public void configure(Binder binder) {
    binder.bind(PingHdsClient.class).toInstance(pingHdsClientMock);
    super.configure(binder);
  }

  @Test
  public void testPingHds_alive() {
    when(pingHdsClientMock.get(String.class, "ping")).thenReturn("alive");

    PingResponseDTO status = hdsPingService.pingHds();

    assertThat(status.alive).isTrue();
    assertThat(status.errorMessage).isNull();
    assertThat(status.incidentId).isNull();
  }

  @Test
  public void testPingHds_Unreachable() {
    when(pingHdsClientMock.get(String.class, "ping")).thenThrow(new BadGatewayException("Unreachable"));

    PingResponseDTO status = hdsPingService.pingHds();

    assertThat(status.alive).isFalse();
    assertThat(status.errorMessage).isEqualTo("Unreachable");
    assertThat(status.incidentId).matches("[0-9a-fA-F]{16}");
  }
}
