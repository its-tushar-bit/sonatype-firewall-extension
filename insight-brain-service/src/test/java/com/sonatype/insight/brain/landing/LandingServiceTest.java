/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sonatype.insight.brain.service.AssetPaths;
import com.sonatype.insight.brain.service.BaseUrl;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LandingServiceTest
{
  private static final String BASE_URL = "http://localhost:8070";

  @Mock
  private BaseUrl baseUrl;

  private LandingService landingService;

  @Before
  public void setUp() {
    landingService = new LandingService(baseUrl);
    when(baseUrl.redirect()).thenReturn(UriBuilder.fromUri(BASE_URL));
  }

  @Test
  public void testGetDestination() {
    URI dst = landingService.getDestination();

    assertThat(dst).isNotNull();
    assertThat(dst.toString()).isEqualTo(BASE_URL + AssetPaths.BRAIN_ASSET_PATH + "index.html");
  }

  @Test
  public void testGetGuideDestination() {
    URI dst = landingService.getGuideDestination();
    assertThat(dst).isNotNull();
    assertThat(dst.toString()).isEqualTo(BASE_URL + AssetPaths.BRAIN_ASSET_PATH + "guide/index.html");
  }
}
