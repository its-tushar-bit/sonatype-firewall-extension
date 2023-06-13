/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.integration;

import java.util.Date;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dashboard.DashboardResultsDTO;
import com.sonatype.insight.brain.dataaccess.CIApplicationFilter;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CIApplicationResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetCIApplicationRisks() throws Exception {
    setUpViolations();

    final CIApplicationFilter filter = new CIApplicationFilter(0, 100, new Date(1569553200000L));
    final HttpResponse response = ciAppRequest(filter);
    assertResponseStatus(200, response);

    final DashboardResultsDTO<?> dto = response.getBody(DashboardResultsDTO.class);
    assertThat(dto.dashboardResults).hasSize(3);
  }

  private void setUpViolations() {
    final Organization org = tempEntity.newOrganization();

    final Application app1 = tempEntity.newApplication(org.getId());
    final Application app2 = tempEntity.newApplication(org.getId());
    final Application app3 = tempEntity.newApplication(org.getId());

    final Policy policy1 = tempEntity.newPolicy(app1);
    final Policy policy2 = tempEntity.newPolicy(app2);
    final Policy policy3 = tempEntity.newPolicy(app3);

    final PolicyEvaluation evaluationApp1 =
        tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "test scan id", new Date(0));
    final PolicyEvaluation evaluationApp2 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "test scan id", new Date(0));
    final PolicyEvaluation evaluationApp3 =
        tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, "test scan id", new Date(0));

    tempEntity.newPolicyViolation(evaluationApp1, policy1);
    tempEntity.newPolicyViolation(evaluationApp2, policy2);
    tempEntity.newPolicyViolation(evaluationApp3, policy3);
  }

  private HttpResponse ciAppRequest(final CIApplicationFilter filter) throws Exception {
    return restRequest().path(CIApplicationResource.RESOURCE_PATH).body(filter).post();
  }
}
