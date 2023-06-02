/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.integration;

import java.util.Calendar;
import java.util.stream.IntStream;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.CIEvaluationStatDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CIEvaluationStatServiceTest
    extends AbstractComponentTest
{
  @Inject
  private CIEvaluationStatService ciEvaluationStatService;

  @Test
  public void testGetDataForAppsWithoutCITriggeredEvaluations() {
    // Set up an org with some applications - 2 with evaluations, 1 of which has CI and not CI, configurable total
    int numTotalApps = 9;
    setUpApplications(numTotalApps, true);

    // 05/17/2021
    long sinceUtcTimestamp = 1621220400000L;
    CIEvaluationStatDTO ciEvaluationStatDTO =
        ciEvaluationStatService.getDataForAppsWithoutCITriggeredEvaluations(sinceUtcTimestamp);

    int expectedNumAppsWithoutCI = numTotalApps - 1;
    assertThat(ciEvaluationStatDTO.numAppsWithoutCITriggeredEvals).isEqualTo(expectedNumAppsWithoutCI);
    assertThat(ciEvaluationStatDTO.numTotalApps).isEqualTo(numTotalApps);
  }

  @Test
  public void testGetDataForAppsWithoutCITriggeredEvaluations_WhenNoAppsHaveEvaluations() {
    // Set up an org with some applications, but no evaluations
    int numTotalApps = 3;
    setUpApplications(numTotalApps, false);

    // 05/17/2021
    long sinceUtcTimestamp = 1621220400000L;
    CIEvaluationStatDTO ciEvaluationStatDTO =
        ciEvaluationStatService.getDataForAppsWithoutCITriggeredEvaluations(sinceUtcTimestamp);

    assertThat(ciEvaluationStatDTO.numAppsWithoutCITriggeredEvals).isEqualTo(numTotalApps);
    assertThat(ciEvaluationStatDTO.numTotalApps).isEqualTo(numTotalApps);
  }

  @Test
  public void testGetPercentageOfAppsWithCITriggeredEvaluations_WhenNoAppsExist() {
    // 05/17/2021
    long sinceUtcTimestamp = 1621220400000L;
    CIEvaluationStatDTO ciEvaluationStatDTO =
        ciEvaluationStatService.getDataForAppsWithoutCITriggeredEvaluations(sinceUtcTimestamp);

    assertThat(ciEvaluationStatDTO.numAppsWithoutCITriggeredEvals).isZero();
    assertThat(ciEvaluationStatDTO.numTotalApps).isZero();
  }

  private void setUpApplications(final int maxApplications, final boolean includeEvaluations) {
    Organization organization = tempEntity.newOrganization();

    if (includeEvaluations) {
      Application application = tempEntity.newApplication(organization.getId());
      Application application2 = tempEntity.newApplication(organization.getId());

      // Add 1 policy evaluation with a scan trigger type not CI, 1 with a scan trigger type of CI, and a 2nd for the
      // same app (i.e. app2 should not count as an app without CI just because it has an eval of type not CI)
      Calendar now = Calendar.getInstance();
      tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_BUILD, "testScanId1",
          false, false, false, now.getTime(), "testCommitHash1", ScanTriggerType.IDE);
      now.add(Calendar.MINUTE, 10);

      tempEntity.newPolicyEvaluation(application2.getId(), Stage.ID_BUILD, "testScanId2",
          false, false, false, now.getTime(), "testCommitHash2", ScanTriggerType.CONTINUOUS_INTEGRATION);
      now.add(Calendar.MINUTE, 10);

      tempEntity.newPolicyEvaluation(application2.getId(), Stage.ID_BUILD, "testScanId3",
          false, false, false, now.getTime(), "testCommitHash3", ScanTriggerType.IDE);
    }

    int effectiveMax = includeEvaluations ? maxApplications - 2 : maxApplications;
    IntStream.range(0, effectiveMax)
        .forEach(i -> tempEntity.newApplication(organization.getId()));
  }
}
