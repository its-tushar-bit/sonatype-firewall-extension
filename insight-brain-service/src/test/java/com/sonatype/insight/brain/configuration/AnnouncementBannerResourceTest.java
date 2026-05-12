/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration;

import com.sonatype.insight.brain.model.configuration.AnnouncementBanner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnnouncementBannerResourceTest
{
  @Mock
  private AnnouncementBannerService service;

  @InjectMocks
  private AnnouncementBannerResource resource;

  @Test
  public void testGet_delegatesToService() {
    AnnouncementBanner banner = new AnnouncementBanner();
    banner.setEnabled(true);
    banner.setMessage("hello");
    when(service.getBanner()).thenReturn(banner);

    AnnouncementBanner result = resource.getAnnouncementBanner();

    assertThat(result).isSameAs(banner);
  }
}
