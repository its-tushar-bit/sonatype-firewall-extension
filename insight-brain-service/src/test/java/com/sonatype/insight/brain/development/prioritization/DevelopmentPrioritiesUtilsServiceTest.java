/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.development.prioritization;

import com.sonatype.insight.brain.features.FeaturesService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static com.google.common.collect.Sets.newHashSet;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.PRIORITIZED_FINDINGS_REPORT;
import static com.sonatype.insight.license.model.LicensedFeature.DEVELOPER_DASHBOARD;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DevelopmentPrioritiesUtilsServiceTest
{
  @Mock
  private FeaturesService featuresService;

  private DevelopmentPrioritiesUtilsService developmentPrioritiesUtilsService;

  @Before
  public void setup() {
    developmentPrioritiesUtilsService = new DevelopmentPrioritiesUtilsService(featuresService);
  }

  @Test
  public void noFeaturesAvailable() {
    assertThat(developmentPrioritiesUtilsService.arePrioritiesFeaturesEnabled()).isFalse();
  }

  @Test
  public void onlyPrioritizedFindingsEnabled() {
    when(featuresService.getFeatures())
        .thenReturn(newHashSet(PRIORITIZED_FINDINGS_REPORT));
    assertThat(developmentPrioritiesUtilsService.arePrioritiesFeaturesEnabled()).isFalse();
  }

  @Test
  public void onlyDeveloperDashboardEnabled() {
    when(featuresService.getFeatures())
        .thenReturn(newHashSet(DEVELOPER_DASHBOARD));
    assertThat(developmentPrioritiesUtilsService.arePrioritiesFeaturesEnabled()).isFalse();
  }

  @Test
  public void bothFeaturesEnabled() {
    when(featuresService.getFeatures())
        .thenReturn(newHashSet(DEVELOPER_DASHBOARD, PRIORITIZED_FINDINGS_REPORT));
    assertThat(developmentPrioritiesUtilsService.arePrioritiesFeaturesEnabled()).isTrue();
  }
}
