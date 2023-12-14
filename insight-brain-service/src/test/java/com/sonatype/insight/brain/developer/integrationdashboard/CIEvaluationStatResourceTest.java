/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.util.Calendar;
import java.util.stream.IntStream;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.developer.integrationdashboard.api.CIEvaluationStatDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CIEvaluationStatResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetDataForAppsWithoutCITriggeredEvaluations() throws Exception {
    // Set up an org with some applications - 2 evals with a scan trigger type of CI, 1 not CI, configurable total
    int numTotalApps = 18;
    setUpApplications(numTotalApps);

    // 05/17/2021
    long sinceUtcTimestamp = 1621220400000L;
    HttpResponse response = ciStatRequest(sinceUtcTimestamp).get();
    assertResponseStatus(200, response);

    CIEvaluationStatDTO responseData = response.getBody(CIEvaluationStatDTO.class);

    int expectedNumAppsWithoutCI = numTotalApps - 2;
    assertThat(responseData.numAppsWithoutCITriggeredEvals).isEqualTo(expectedNumAppsWithoutCI);
    assertThat(responseData.numTotalApps).isEqualTo(numTotalApps);
  }

  private HttpRequest ciStatRequest(final long sinceUtcTimestamp) {
    return restRequest().path(CIEvaluationStatResource.RESOURCE_PATH).query("sinceUtcTimestamp", sinceUtcTimestamp);
  }

  private void setUpApplications(final int maxApplications) {
    Organization organization = tempEntity.newOrganization();

    Application application = tempEntity.newApplication(organization.getId());
    Application application2 = tempEntity.newApplication(organization.getId());
    Application application3 = tempEntity.newApplication(organization.getId());

    // Add 2 policy evaluations with a scan trigger type of CI, 1 with a scan trigger type not CI
    Calendar now = Calendar.getInstance();
    tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "testScanId1",
        false, false, false, now.getTime(), "testCommitHash1", ScanTriggerType.CONTINUOUS_INTEGRATION);
    now.add(Calendar.MINUTE, 10);

    tempEntity.newPolicyEvaluation(application2.getId(), Stage.ID_BUILD, "testScanId2",
        false, false, false, now.getTime(), "testCommitHash2", ScanTriggerType.CONTINUOUS_INTEGRATION);
    now.add(Calendar.MINUTE, 10);

    tempEntity.newPolicyEvaluation(application3.getId(), Stage.ID_BUILD, "testScanId3",
        false, false, false, now.getTime(), "testCommitHash3", ScanTriggerType.CLI);

    // Desired max - 3 that have already been added
    int effectiveMax = maxApplications - 3;
    IntStream.range(0, effectiveMax)
        .forEach(i -> tempEntity.newApplication(organization.getId()));
  }
}
