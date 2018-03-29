/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.IOException;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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
  public void testPingHds_alive() throws Exception {
    when(pingHdsClientMock.get(String.class, "ping")).thenReturn("alive");

    boolean status = hdsPingService.pingHds();

    assertThat(status, is(true));
  }

  @Test
  public void testPingHds_Unreachable() throws Exception {
    when(pingHdsClientMock.get(String.class, "ping")).thenThrow(new IOException());

    boolean status = hdsPingService.pingHds();

    assertThat(status, is(false));
  }
}
