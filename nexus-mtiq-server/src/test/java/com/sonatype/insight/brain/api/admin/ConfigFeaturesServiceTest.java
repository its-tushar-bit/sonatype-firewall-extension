/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.features.NonLicensedFeature;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig.Feature;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigFeaturesServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ConfigFeaturesService configFeaturesService;

  @Inject
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Test
  public void testGetFeatures() {
    assertThat(configFeaturesService.getFeatures()).contains(
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED,
        SystemConfigurationPropertyFeature.CODE_INSIGHTS,
        SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER,
        SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE,
        SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API,
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED,
        SystemConfigurationPropertyFeature.PR_COMMENTING,
        SystemConfigurationPropertyFeature.CROWD_INTEGRATION,
        SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES,
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION,
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING,
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED,
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING
    );
  }

  @Test
  public void testGetFeatures_CODE_INSIGHTS_disabled() {
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.CODE_INSIGHTS, "true");
    assertThat(configFeaturesService.getFeatures()).doesNotContain(
        SystemConfigurationPropertyFeature.CODE_INSIGHTS
    );
  }

  @Test
  public void testGetAllFeatures() {
    assertThat(configFeaturesService.getAllFeatures()).contains(
        SystemConfigurationPropertyFeature.CODE_INSIGHTS,
        SystemConfigurationPropertyFeature.INNER_SOURCE_TRANSITIVE_WAIVER,
        SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE,
        SystemConfigurationPropertyFeature.DEPENDENCY_DATA_IN_API,
        SystemConfigurationPropertyFeature.REPORTS_LIST_CAN_BE_ENABLED,
        SystemConfigurationPropertyFeature.PR_COMMENTING,
        SystemConfigurationPropertyFeature.CROWD_INTEGRATION,
        SystemConfigurationPropertyFeature.ENABLE_UNAUTHENTICATED_PAGES,
        SystemConfigurationPropertyFeature.INNER_SOURCE_REPOSITORY_INTEGRATION,
        SystemConfigurationPropertyFeature.DEFAULT_BRANCH_MONITORING,
        SystemConfigurationPropertyFeature.DASHBOARD_CAN_BE_ENABLED,
        SystemConfigurationPropertyFeature.PR_LINE_COMMENTING,
        SystemConfigurationPropertyFeature.SCAN_NPM_DEV_AND_OPT_DEPENDENCIES,
        SystemConfigurationPropertyFeature.VULNERABILITY_SOURCE,
        SystemConfigurationPropertyFeature.API_PAGE,
        SystemConfigurationPropertyFeature.TRANSITIVE_SOLVER,
        SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE,
        SystemConfigurationPropertyFeature.SCAN_POM_FILES_IN_META_INF_DIRECTORY
    );
  }

  @Test
  public void testGetFeatures_NoDuplicates() {
    List<com.sonatype.insight.license.model.Feature> allFeatures = new ArrayList<>();
    allFeatures.addAll(Arrays.asList(LicensedFeature.values()));
    allFeatures.addAll(Arrays.asList(NonLicensedFeature.values()));
    allFeatures.addAll(Arrays.asList(Feature.values()));
    List<String> allFeatureIdsList = allFeatures.stream()
        .map(com.sonatype.insight.license.model.Feature::getId)
        .collect(Collectors.toList());
    Set<String> allFeatureIdsSet = new LinkedHashSet<>(allFeatureIdsList);

    assertThat(allFeatureIdsSet).hasSize(allFeatureIdsList.size());
  }
}
