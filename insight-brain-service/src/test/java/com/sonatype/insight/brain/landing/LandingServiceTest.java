/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.core.UriBuilder;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;

import com.google.inject.Binder;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LandingServiceTest
    extends AbstractComponentTest
{
  @Inject
  private LandingService landingService;

  private BaseUrl baseUrl;

  private final String BASE_URL = "http://localhost:8070";

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    baseUrl = mock(BaseUrl.class);
    binder.bind(BaseUrl.class).toInstance(baseUrl);
    when(baseUrl.redirect()).thenReturn(UriBuilder.fromUri(BASE_URL));
  }

  @Test
  public void testGetDestination() {
    URI dst = landingService.getDestination();
    assertThat(dst, is(notNullValue()));
    assertThat(dst.toString(), is(BASE_URL + InsightBrainService.BRAIN_ASSET_PATH + "index.html#/reports/violations"));
  }
}
