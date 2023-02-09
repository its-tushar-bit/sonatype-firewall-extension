/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.admin;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.ApiConfigFeaturesService.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.sonatype.insight.brain.service.AbstractMultiTenantResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_CONFIG_FEATURES_PATH;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.REPORTS_LIST_DISABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;

public class ConfigFeaturesResourceTest
    extends AbstractMultiTenantResourceTest
{
  private final SystemConfigurationPropertyDAO configurationPropertyDAO = new SystemConfigurationPropertyDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.adminRequest().path("api/").path(ADMIN_CONFIG_FEATURES_PATH).query("tenant=global");
  }

  @Test
  public void testFeatures() throws Exception {
    // Get all features
    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    assertThat(features).contains("code-insights", "pr-commenting", "pr-line-commenting");
  }

  @Test
  public void testFeatures_all() throws Exception {
    // Get all features
    HttpResponse response = restRequest().path("all").get();
    assertResponseStatus(200, response);
    String[] features = response.getBody(String[].class);
    SystemConfigurationPropertyFeature.values();
    List<String> allFeatures = Arrays.stream(SystemConfigurationPropertyFeature.values())
        .map(feature -> feature.getId()).collect(Collectors.toList());

    assertThat(features).containsAll(allFeatures);
  }

  @Test
  public void testEnableFeature_Dashboard() throws Exception {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    AbstractBrainServiceTest.assertResponseStatus(NO_CONTENT_204, restRequest().path("dashboard").post());
    assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
  }

  @Test
  public void testEnableFeature_ReportsList() throws Exception {
    tempEntity.newSystemConfigurationProperty(REPORTS_LIST_DISABLED, "true");
    AbstractBrainServiceTest.assertResponseStatus(NO_CONTENT_204, restRequest().path("reportsList").post());
    assertThat(configurationPropertyDAO.getByName(REPORTS_LIST_DISABLED)).isNull();
  }

  @Test
  public void testEnableFeature_SecurityVulnerabilitySourcePolicyCondition() throws Exception {
    AbstractBrainServiceTest.assertResponseStatus(NO_CONTENT_204,
        restRequest().path("vulnerabilitySource").post());
    assertThat(configurationPropertyDAO.getByName(SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED)).isNull();
  }

  @Test
  public void testDisableFeature_Dashboard() throws Exception {
    AbstractBrainServiceTest.assertResponseStatus(NO_CONTENT_204, restRequest().path("dashboard").delete());
    assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
  }

  @Test
  public void testDisableFeature_ReportsList() throws Exception {
    AbstractBrainServiceTest.assertResponseStatus(NO_CONTENT_204, restRequest().path("reportsList").delete());
    assertThat(configurationPropertyDAO.getByName(REPORTS_LIST_DISABLED)).isNotNull();
  }

  @Test
  public void testDisableFeature_SecurityVulnerabilitySourcePolicyCondition() throws Exception {
    new SystemConfigurationPropertyDAO().delete(
        configurationPropertyDAO.getByName(SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED));
    AbstractBrainServiceTest.assertResponseStatus(NO_CONTENT_204,
        restRequest().path("vulnerabilitySource").delete());
    assertThat(configurationPropertyDAO.getByName(SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED))
        .isNotNull();
  }
}
