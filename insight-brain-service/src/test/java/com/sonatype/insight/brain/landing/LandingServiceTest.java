/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class LandingServiceTest
    extends AbstractComponentTest
{
  @Inject
  private LandingService landingService;

  private BaseUrl baseUrl;

  private static final String BASE_URL = "http://localhost:8070";

  @Override
  public void configure(Binder binder) {
    baseUrl = mock(BaseUrl.class);
    binder.bind(BaseUrl.class).toInstance(baseUrl);
    lenient().when(baseUrl.redirect()).thenReturn(UriBuilder.fromUri(BASE_URL));
    super.configure(binder);
  }

  @Test
  public void testGetDestination() {
    URI dst = landingService.getDestination();
    assertThat(dst).isNotNull();
    assertThat(dst.toString()).isEqualTo(BASE_URL + InsightBrainService.BRAIN_ASSET_PATH + "index.html");
  }
}
