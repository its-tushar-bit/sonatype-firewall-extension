/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.FEATURE_DASHBOARD;
import static com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.FEATURE_REPORTS_LIST;
import static com.sonatype.insight.brain.api.experimental.ApiConfigFeaturesService.FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.DASHBOARD_DISABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.REPORTS_LIST_DISABLED;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jetty.http.HttpStatus.NO_CONTENT_204;

public class ApiConfigFeaturesResourceTest
    extends AbstractResourceTest
{
  private SystemConfigurationPropertyDAO configurationPropertyDAO = new SystemConfigurationPropertyDAO();

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.CONFIG_FEATURES_PATH);
  }

  @Test
  public void testEnableFeature_Dashboard() throws Exception {
    tempEntity.newSystemConfigurationProperty(DASHBOARD_DISABLED, "true");
    assertResponseStatus(NO_CONTENT_204, restRequest().path(FEATURE_DASHBOARD).post());
    assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNull();
  }

  @Test
  public void testEnableFeature_ReportsList() throws Exception {
    tempEntity.newSystemConfigurationProperty(REPORTS_LIST_DISABLED, "true");
    assertResponseStatus(NO_CONTENT_204, restRequest().path(FEATURE_REPORTS_LIST).post());
    assertThat(configurationPropertyDAO.getByName(REPORTS_LIST_DISABLED)).isNull();
  }

  @Test
  public void testEnableFeature_SecurityVulnerabilitySourcePolicyCondition() throws Exception {
    assertResponseStatus(NO_CONTENT_204,
        restRequest().path(FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION).post());
    assertThat(configurationPropertyDAO.getByName(SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED)).isNull();
  }

  @Test
  public void testDisableFeature_Dashboard() throws Exception {
    assertResponseStatus(NO_CONTENT_204, restRequest().path(FEATURE_DASHBOARD).delete());
    assertThat(configurationPropertyDAO.getByName(DASHBOARD_DISABLED)).isNotNull();
  }

  @Test
  public void testDisableFeature_ReportsList() throws Exception {
    assertResponseStatus(NO_CONTENT_204, restRequest().path(FEATURE_REPORTS_LIST).delete());
    assertThat(configurationPropertyDAO.getByName(REPORTS_LIST_DISABLED)).isNotNull();
  }

  @Test
  public void testDisableFeature_SecurityVulnerabilitySourcePolicyCondition() throws Exception {
    new SystemConfigurationPropertyDAO().delete(
        configurationPropertyDAO.getByName(SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED));
    assertResponseStatus(NO_CONTENT_204,
        restRequest().path(FEATURE_SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION).delete());
    assertThat(configurationPropertyDAO.getByName(SECURITY_VULNERABILITY_SOURCE_POLICY_CONDITION_DISABLED))
        .isNotNull();
  }
}
