/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.developer.integrationdashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.api.IntegrationStatusDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class IntegrationServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  private IntegrationService integrationService;

  @Inject
  private ApplicationDAO appDAO;

  private Application app1;

  private Organization org1;

  @BeforeEach
  public void deleteUnusedAppFromSuperclass() {
    appDAO.delete(app);
  }

  @Test
  public void testGetIntegrationStatuses__NoPermissionSeesNoStatuses() {
    setUpAppsWithRisk();
    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> integrationStatuses = result.getResults();
    assertThat(integrationStatuses)
        .hasSize(0);
  }

  @Test
  public void testGetIntegrationStatuses__GlobalPermissionSeesAllStatuses() {
    setUpAppsWithRisk();
    grantGlobalPermission(Permission.READ);
    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> integrationStatuses = result.getResults();
    assertThat(integrationStatuses)
        .hasSize(4);
  }

  @Test
  public void testGetIntegrationStatuses__OneAppPermissionSeesOneAppStatuses() {
    setUpAppsWithRisk();
    grantReadPermission(app1.getId());
    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> integrationStatuses = result.getResults();
    assertThat(integrationStatuses)
        .hasSize(1);
    final IntegrationStatusDTO app1Dto = integrationStatuses.get(0);
    assertThat(app1Dto.getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(app1Dto.getOrganizationId())
        .isEqualTo(org1.getId());
  }

  private void setUpAppsWithRisk() {
    org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    Organization org3 = tempEntity.newOrganization();

    app1 = tempEntity.newApplication("app1", "app1", org1.getId());
    Application app2 = tempEntity.newApplication("app2", "app2", org1.getId());
    // No risk apps
    tempEntity.newApplication("app3", "app3", org2.getId());
    tempEntity.newApplication("app4", "app4", org3.getId());

    final Policy orgPolicy = tempEntity.newPolicy(org1.getId(), "org owned policy", 3);
    final Policy app1Policy = tempEntity.newPolicy(app1.getId(), "app1 owned policy", 5);

    final PolicyEvaluation app1PolicyEvaluation =
        tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scan-id-1", new Date(0L));
    final PolicyEvaluation app2PolicyEvaluation =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scan-id-2", new Date(0L));

    tempEntity.newPolicyViolation(app1PolicyEvaluation, orgPolicy);
    tempEntity.newPolicyViolation(app1PolicyEvaluation, app1Policy);
    tempEntity.newPolicyViolation(app2PolicyEvaluation, orgPolicy);
  }
}
