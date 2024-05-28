/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.developer.integrationdashboard;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.developer.integrationdashboard.api.IntegrationStatusDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.verify;

public class IntegrationServiceTest
    extends AbstractComponentTest
{
  @Mock
  private TelemetrySender telemetrySender;

  @Captor
  private ArgumentCaptor<TelemetryData> telemetryDataArgumentCaptor;

  @Inject
  private IntegrationService integrationService;

  private Application app1;

  private Application app2;

  private Application app3;

  private Application app4;

  private Organization org1;

  private Organization org2;

  private Organization org3;

  @Override
  public void configure(Binder binder) {
    binder.bind(TelemetrySender.class).toInstance(telemetrySender);
    super.configure(binder);
  }

  @Test
  public void testGetIntegrationSummaries_InvalidPageOrPageSizeThrowsException_h2() {
    assertThatThrownBy(() -> integrationService.getIntegrationStatuses(-1, 100, null, null, null, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Page and Page size must be greater than 0");

    assertThatThrownBy(() -> integrationService.getIntegrationStatuses(2, 0, null, null, null, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Page and Page size must be greater than 0");
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_InvalidPageOrPageSizeThrowsException_postgres() {
    assertThatThrownBy(() -> integrationService.getIntegrationStatuses(-1, 100, null, null, null, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Page and Page size must be greater than 0");

    assertThatThrownBy(() -> integrationService.getIntegrationStatuses(2, 0, null, null, null, null))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Page and Page size must be greater than 0");
  }

  @Test
  public void testGetIntegrationSummaries_Pagination_h2() {
    setUpAppsWithBuildStageRisk();

    // PAGE ONE
    int page = 1;
    final int pageSize = 3;
    final ApiPageResult<IntegrationStatusDTO> pageOneResult =
        integrationService.getIntegrationStatuses(page, pageSize, null, null, null, null);

    assertThat(pageOneResult.getPage())
        .isEqualTo(page);
    assertThat(pageOneResult.getPageSize())
        .isEqualTo(pageSize);
    // 4 apps were created in total
    assertThat(pageOneResult.getTotal())
        .isEqualTo(4);
    // 4 total apps with pageSize of 3 == 2 pages
    assertThat(pageOneResult.getPageCount())
        .isEqualTo(2);

    final List<IntegrationStatusDTO> pageOneApps = pageOneResult.getResults();
    assertThat(pageOneApps)
        .hasSize(pageSize);
    // Apps are ordered by name ASC by default
    assertThat(pageOneApps.get(0).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(pageOneApps.get(0).getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(pageOneApps.get(1).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(pageOneApps.get(1).getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(pageOneApps.get(2).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(pageOneApps.get(2).getOrganizationId())
        .isEqualTo(org2.getId());

    // PAGE TWO
    page = 2;
    final ApiPageResult<IntegrationStatusDTO> pageTwoResult =
        integrationService.getIntegrationStatuses(page, pageSize, null, null, null, null);

    assertThat(pageTwoResult.getPage())
        .isEqualTo(page);
    assertThat(pageTwoResult.getPageSize())
        .isEqualTo(pageSize);
    // 4 apps were created in total
    assertThat(pageTwoResult.getTotal())
        .isEqualTo(4);
    // 4 total apps with pageSize of 3 == 2 pages
    assertThat(pageTwoResult.getPageCount())
        .isEqualTo(2);

    final List<IntegrationStatusDTO> pageTwoApps = pageTwoResult.getResults();
    // Total of 4 apps - pageSize of 3 == 1 result overflow
    assertThat(pageTwoApps)
        .hasSize(1);
    assertThat(pageTwoApps.get(0).getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(pageTwoApps.get(0).getOrganizationId())
        .isEqualTo(org3.getId());
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_Pagination_postgres() {
    setUpAppsWithBuildStageRisk();

    // PAGE ONE
    int page = 1;
    final int pageSize = 3;
    final ApiPageResult<IntegrationStatusDTO> pageOneResult =
        integrationService.getIntegrationStatuses(page, pageSize, null, null, null, null);

    assertThat(pageOneResult.getPage())
        .isEqualTo(page);
    assertThat(pageOneResult.getPageSize())
        .isEqualTo(pageSize);
    // 4 apps were created in total
    assertThat(pageOneResult.getTotal())
        .isEqualTo(4);
    // 4 total apps with pageSize of 3 == 2 pages
    assertThat(pageOneResult.getPageCount())
        .isEqualTo(2);

    final List<IntegrationStatusDTO> pageOneApps = pageOneResult.getResults();
    assertThat(pageOneApps)
        .hasSize(pageSize);
    // Apps are ordered by name ASC by default
    assertThat(pageOneApps.get(0).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(pageOneApps.get(0).getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(pageOneApps.get(1).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(pageOneApps.get(1).getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(pageOneApps.get(2).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(pageOneApps.get(2).getOrganizationId())
        .isEqualTo(org2.getId());

    // PAGE TWO
    page = 2;
    final ApiPageResult<IntegrationStatusDTO> pageTwoResult =
        integrationService.getIntegrationStatuses(page, pageSize, null, null, null, null);

    assertThat(pageTwoResult.getPage())
        .isEqualTo(page);
    assertThat(pageTwoResult.getPageSize())
        .isEqualTo(pageSize);
    // 4 apps were created in total
    assertThat(pageTwoResult.getTotal())
        .isEqualTo(4);
    // 4 total apps with pageSize of 3 == 2 pages
    assertThat(pageTwoResult.getPageCount())
        .isEqualTo(2);

    final List<IntegrationStatusDTO> pageTwoApps = pageTwoResult.getResults();
    // Total of 4 apps - pageSize of 3 == 1 result overflow
    assertThat(pageTwoApps)
        .hasSize(1);
    assertThat(pageTwoApps.get(0).getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(pageTwoApps.get(0).getOrganizationId())
        .isEqualTo(org3.getId());
  }

  @Test
  public void testGetIntegrationSummaries_Pagination_PageSizeGreaterThanResult_h2() {
    setUpAppsWithBuildStageRisk();

    int page = 1;
    final int pageSize = 100;
    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(page, pageSize, null, null, null, null);

    assertThat(result.getPage())
        .isEqualTo(page);
    assertThat(result.getPageSize())
        .isEqualTo(pageSize);
    // 4 apps were created in total
    assertThat(result.getTotal())
        .isEqualTo(4);
    // 4 total apps but pageSize of 100 == 1 page
    assertThat(result.getPageCount())
        .isEqualTo(1);

    final List<IntegrationStatusDTO> apps = result.getResults();
    // All 4 apps should be included in first page results
    assertThat(apps)
        .hasSize(4);
    // Apps are ordered by name ASC by default
    assertThat(apps.get(0).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(apps.get(0).getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(apps.get(1).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(apps.get(1).getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(apps.get(2).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(apps.get(2).getOrganizationId())
        .isEqualTo(org2.getId());
    assertThat(apps.get(3).getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(apps.get(3).getOrganizationId())
        .isEqualTo(org3.getId());
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_Pagination_PageSizeGreaterThanResult_postgres() {
    setUpAppsWithBuildStageRisk();

    int page = 1;
    final int pageSize = 100;
    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(page, pageSize, null, null, null, null);

    assertThat(result.getPage())
        .isEqualTo(page);
    assertThat(result.getPageSize())
        .isEqualTo(pageSize);
    // 4 apps were created in total
    assertThat(result.getTotal())
        .isEqualTo(4);
    // 4 total apps but pageSize of 100 == 1 page
    assertThat(result.getPageCount())
        .isEqualTo(1);

    final List<IntegrationStatusDTO> apps = result.getResults();
    // All 4 apps should be included in first page results
    assertThat(apps)
        .hasSize(4);
    // Apps are ordered by name ASC by default
    assertThat(apps.get(0).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(apps.get(0).getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(apps.get(1).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(apps.get(1).getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(apps.get(2).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(apps.get(2).getOrganizationId())
        .isEqualTo(org2.getId());
    assertThat(apps.get(3).getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(apps.get(3).getOrganizationId())
        .isEqualTo(org3.getId());
  }

  @Test
  public void testGetIntegrationSummaries_Pagination_PageNumGreaterThanResult_h2() {
    setUpAppsWithBuildStageRisk();

    int page = 100;
    final int pageSize = 100;
    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(page, pageSize, null, null, null, null);

    assertThat(result.getPage())
        .isEqualTo(page);
    assertThat(result.getPageSize())
        .isEqualTo(pageSize);
    // 4 apps were created in total
    assertThat(result.getTotal())
        .isEqualTo(4);
    // 4 total apps but pageSize of 100 == 1 page
    assertThat(result.getPageCount())
        .isEqualTo(1);
    // There should be no content for page 100
    assertThat(result.getResults())
        .isEmpty();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_Pagination_PageNumGreaterThanResult_postgres() {
    setUpAppsWithBuildStageRisk();

    int page = 100;
    final int pageSize = 100;
    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(page, pageSize, null, null, null, null);

    assertThat(result.getPage())
        .isEqualTo(page);
    assertThat(result.getPageSize())
        .isEqualTo(pageSize);
    // 4 apps were created in total
    assertThat(result.getTotal())
        .isEqualTo(4);
    // 4 total apps but pageSize of 100 == 1 page
    assertThat(result.getPageCount())
        .isEqualTo(1);
    // There should be no content for page 100
    assertThat(result.getResults())
        .isEmpty();
  }

  @Test
  public void testGetIntegrationSummaries_OrderByName_h2() {
    setUpAppsWithBuildStageRisk();

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, "-NAME", null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);
    assertThat(appSummaries.get(0).getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(appSummaries.get(0).getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(appSummaries.get(1).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(appSummaries.get(1).getOrganizationId())
        .isEqualTo(org2.getId());
    assertThat(appSummaries.get(2).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(appSummaries.get(2).getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(appSummaries.get(3).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(appSummaries.get(3).getOrganizationId())
        .isEqualTo(org1.getId());
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_OrderByName_postgres() {
    setUpAppsWithBuildStageRisk();

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, "-NAME", null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);
    assertThat(appSummaries.get(0).getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(appSummaries.get(0).getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(appSummaries.get(1).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(appSummaries.get(1).getOrganizationId())
        .isEqualTo(org2.getId());
    assertThat(appSummaries.get(2).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(appSummaries.get(2).getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(appSummaries.get(3).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(appSummaries.get(3).getOrganizationId())
        .isEqualTo(org1.getId());
  }

  @Test
  public void testGetIntegrationSummaries_OrderByLastCommit_h2() {
    setUpAppsWithBuildStageRisk();

    final Date commitTime = new Date();
    final Date latestCommitTime1 = new Date(commitTime.getTime() + 5000);
    final Date latestCommitTime2 = new Date(commitTime.getTime() + 10_000);

    // App 1
    tempEntity.newSourceControlDefaultBranchCommitHistory(app1.getId(), "commit1", commitTime, null);
    tempEntity.newSourceControlDefaultBranchCommitHistory(app1.getId(), "commit2", latestCommitTime1, null);

    // App 2
    tempEntity.newSourceControlDefaultBranchCommitHistory(app2.getId(), "commit1", commitTime, null);
    tempEntity.newSourceControlDefaultBranchCommitHistory(app2.getId(), "commit2", latestCommitTime2, null);

    // ASC
    final ApiPageResult<IntegrationStatusDTO> ascResult =
        integrationService.getIntegrationStatuses(1, 100, "COMMIT", null, null, null);
    final List<IntegrationStatusDTO> ascAppSummaries = ascResult.getResults();
    assertThat(ascAppSummaries)
        .hasSize(4);
    assertThat(ascAppSummaries.get(0).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(ascAppSummaries.get(1).getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(ascAppSummaries.get(2).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(ascAppSummaries.get(3).getApplicationId())
        .isEqualTo(app2.getId());

    // DESC
    final ApiPageResult<IntegrationStatusDTO> descResult =
        integrationService.getIntegrationStatuses(1, 100, "-COMMIT", null, null, null);
    final List<IntegrationStatusDTO> descAppSummaries = descResult.getResults();
    assertThat(descAppSummaries)
        .hasSize(4);
    assertThat(descAppSummaries.get(0).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(descAppSummaries.get(1).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(descAppSummaries.get(2).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(descAppSummaries.get(3).getApplicationId())
        .isEqualTo(app4.getId());
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_OrderByLastCommit_postgres() {
    setUpAppsWithBuildStageRisk();

    final Date commitTime = new Date();
    final Date latestCommitTime1 = new Date(commitTime.getTime() + 5000);
    final Date latestCommitTime2 = new Date(commitTime.getTime() + 10_000);

    // App 1
    tempEntity.newSourceControlDefaultBranchCommitHistory(app1.getId(), "commit1", commitTime, null);
    tempEntity.newSourceControlDefaultBranchCommitHistory(app1.getId(), "commit2", latestCommitTime1, null);

    // App 2
    tempEntity.newSourceControlDefaultBranchCommitHistory(app2.getId(), "commit1", commitTime, null);
    tempEntity.newSourceControlDefaultBranchCommitHistory(app2.getId(), "commit2", latestCommitTime2, null);

    // ASC
    final ApiPageResult<IntegrationStatusDTO> ascResult =
        integrationService.getIntegrationStatuses(1, 100, "COMMIT", null, null, null);
    final List<IntegrationStatusDTO> ascAppSummaries = ascResult.getResults();
    assertThat(ascAppSummaries)
        .hasSize(4);
    assertThat(ascAppSummaries.get(0).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(ascAppSummaries.get(1).getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(ascAppSummaries.get(2).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(ascAppSummaries.get(3).getApplicationId())
        .isEqualTo(app2.getId());

    // DESC
    final ApiPageResult<IntegrationStatusDTO> descResult =
        integrationService.getIntegrationStatuses(1, 100, "-COMMIT", null, null, null);
    final List<IntegrationStatusDTO> descAppSummaries = descResult.getResults();
    assertThat(descAppSummaries)
        .hasSize(4);
    assertThat(descAppSummaries.get(0).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(descAppSummaries.get(1).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(descAppSummaries.get(2).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(descAppSummaries.get(3).getApplicationId())
        .isEqualTo(app4.getId());
  }

  @Test
  public void testGetIntegrationSummaries_OrderByLastEvaluation_h2() {
    setUpAppsWithBuildStageRisk();

    final Date evalTime = new Date();
    final Date latestEvalTime1 = new Date(evalTime.getTime() + 6000);
    final Date latestEvalTime2 = new Date(evalTime.getTime() + 12_000);

    // App 3
    tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, "scanId1", evalTime);
    tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, "scanId2", latestEvalTime1);

    // App 2
    tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1", evalTime);
    tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId2", latestEvalTime2);

    // ASC (oldest -> newest)
    final ApiPageResult<IntegrationStatusDTO> ascResult =
        integrationService.getIntegrationStatuses(1, 100, "EVALUATION", null, null, null);
    final List<IntegrationStatusDTO> ascAppSummaries = ascResult.getResults();
    assertThat(ascAppSummaries)
        .hasSize(4);
    assertThat(ascAppSummaries.get(0).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(ascAppSummaries.get(1).getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(ascAppSummaries.get(2).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(ascAppSummaries.get(3).getApplicationId())
        .isEqualTo(app2.getId());

    // DESC (newest -> oldest)
    final ApiPageResult<IntegrationStatusDTO> descResult =
        integrationService.getIntegrationStatuses(1, 100, "-EVALUATION", null, null, null);
    final List<IntegrationStatusDTO> descAppSummaries = descResult.getResults();
    assertThat(descAppSummaries)
        .hasSize(4);
    assertThat(descAppSummaries.get(0).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(descAppSummaries.get(1).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(descAppSummaries.get(2).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(descAppSummaries.get(3).getApplicationId())
        .isEqualTo(app4.getId());
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_OrderByLastEvaluation_postgres() {
    setUpAppsWithBuildStageRisk();

    final Date evalTime = new Date();
    final Date latestEvalTime1 = new Date(evalTime.getTime() + 6000);
    final Date latestEvalTime2 = new Date(evalTime.getTime() + 12_000);

    // App 3
    tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, "scanId1", evalTime);
    tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, "scanId2", latestEvalTime1);

    // App 2
    tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1", evalTime);
    tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId2", latestEvalTime2);

    // ASC (oldest -> newest)
    final ApiPageResult<IntegrationStatusDTO> ascResult =
        integrationService.getIntegrationStatuses(1, 100, "EVALUATION", null, null, null);
    final List<IntegrationStatusDTO> ascAppSummaries = ascResult.getResults();
    assertThat(ascAppSummaries)
        .hasSize(4);
    assertThat(ascAppSummaries.get(0).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(ascAppSummaries.get(1).getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(ascAppSummaries.get(2).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(ascAppSummaries.get(3).getApplicationId())
        .isEqualTo(app2.getId());

    // DESC (newest -> oldest)
    final ApiPageResult<IntegrationStatusDTO> descResult =
        integrationService.getIntegrationStatuses(1, 100, "-EVALUATION", null, null, null);
    final List<IntegrationStatusDTO> descAppSummaries = descResult.getResults();
    assertThat(descAppSummaries)
        .hasSize(4);
    assertThat(descAppSummaries.get(0).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(descAppSummaries.get(1).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(descAppSummaries.get(2).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(descAppSummaries.get(3).getApplicationId())
        .isEqualTo(app4.getId());
  }

  @Test
  public void testGetIntegrationSummaries_OrderByTotalRisk_h2() {
    setUpAppsWithBuildStageRisk();

    // App 1 = 8 total risk
    // App 2 = 3 total risk
    // App 3 = 0 total risk
    // App 4 = 0 total risk

    // ASC
    final ApiPageResult<IntegrationStatusDTO> ascResult =
        integrationService.getIntegrationStatuses(1, 100, "TOTAL_RISK", null, null, null);
    final List<IntegrationStatusDTO> ascAppSummaries = ascResult.getResults();
    assertThat(ascAppSummaries)
        .hasSize(4);
    assertThat(ascAppSummaries.get(0).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(ascAppSummaries.get(1).getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(ascAppSummaries.get(2).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(ascAppSummaries.get(3).getApplicationId())
        .isEqualTo(app1.getId());

    // DESC
    final ApiPageResult<IntegrationStatusDTO> descResult =
        integrationService.getIntegrationStatuses(1, 100, "-TOTAL_RISK", null, null, null);
    final List<IntegrationStatusDTO> descAppSummaries = descResult.getResults();
    assertThat(descAppSummaries)
        .hasSize(4);

    assertThat(descAppSummaries.get(0).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(descAppSummaries.get(1).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(descAppSummaries.get(2).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(descAppSummaries.get(3).getApplicationId())
        .isEqualTo(app4.getId());

  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_OrderByTotalRisk_postgres() {
    setUpAppsWithBuildStageRisk();

    // App 1 = 8 total risk
    // App 2 = 3 total risk
    // App 3 = 0 total risk
    // App 4 = 0 total risk

    // ASC
    final ApiPageResult<IntegrationStatusDTO> ascResult =
        integrationService.getIntegrationStatuses(1, 100, "TOTAL_RISK", null, null, null);
    final List<IntegrationStatusDTO> ascAppSummaries = ascResult.getResults();
    assertThat(ascAppSummaries)
        .hasSize(4);
    assertThat(ascAppSummaries.get(0).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(ascAppSummaries.get(1).getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(ascAppSummaries.get(2).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(ascAppSummaries.get(3).getApplicationId())
        .isEqualTo(app1.getId());

    // DESC
    final ApiPageResult<IntegrationStatusDTO> descResult =
        integrationService.getIntegrationStatuses(1, 100, "-TOTAL_RISK", null, null, null);
    final List<IntegrationStatusDTO> descAppSummaries = descResult.getResults();
    assertThat(descAppSummaries)
        .hasSize(4);

    assertThat(descAppSummaries.get(0).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(descAppSummaries.get(1).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(descAppSummaries.get(2).getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(descAppSummaries.get(3).getApplicationId())
        .isEqualTo(app4.getId());

  }

  @Test
  public void testGetIntegrationSummaries_FilterOnName_h2() {
    final Organization org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("SeaL", "app1", org.getId());
    app2 = tempEntity.newApplication("rEAl", "app2", org.getId());
    app3 = tempEntity.newApplication("blob", "app3", org.getId());

    // CONTAINS
    final ApiPageResult<IntegrationStatusDTO> containsResult =
        integrationService.getIntegrationStatuses(1, 100, null, "ea", null, null);
    final List<IntegrationStatusDTO> appSummaries = containsResult.getResults();
    assertThat(appSummaries)
        .hasSize(2);
    assertThat(appSummaries.get(0).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(appSummaries.get(1).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(appSummaries)
        .extracting("applicationId")
        .doesNotContain(app3.getId());

    // DOES NOT CONTAIN
    final ApiPageResult<IntegrationStatusDTO> notContainsResult =
        integrationService.getIntegrationStatuses(1, 100, null, "298df", null, null);
    assertThat(notContainsResult.getResults())
        .isEmpty();
  }

  @Test
  public void testGetIntegrationSummaries_FilterOnScmAndCiIntegrationStatuses() throws Exception {
    setUpAppsWithBuildStageRisk();
    final String repoUrl = "https://example.com/organization/project";
    tempEntity.newSourceControl(
        ROOT_ORGANIZATION_ID,
        null,
        new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"),
        SourceControlProvider.GITHUB
    );
    // Explicitly set SCM Integration Status to false for app 1
    tempEntity.newSourceControl(
        app1.getId(),
        repoUrl,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        null,
        false,
        false,
        "/target/*",
        true,
        false
    );
    // Explicitly set SCM Integration Status to true for app 2
    tempEntity.newSourceControl(
        app2.getId(),
        repoUrl,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        null,
        true,
        true,
        "/target/*",
        true,
        true
    );
    // Explicitly set SCM Integration Status to true for app 4
    tempEntity.newSourceControl(
        app4.getId(),
        repoUrl,
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        null,
        true,
        true,
        "/target/*",
        true,
        true
    );
    // Explicitly set CI Integration Status to true for app 1
    tempEntity.newPolicyEvaluation(
        app1.getId(),
        Stage.ID_BUILD,
        "scan-id-3",
        false,
        false,
        false,
        new Date(),
        "hash-1",
        ScanTriggerType.CONTINUOUS_INTEGRATION
    );
    // Explicitly set CI Integration Status to true for app 2
    tempEntity.newPolicyEvaluation(
        app2.getId(),
        Stage.ID_BUILD,
        "scan-id-4",
        false,
        false,
        false,
        new Date(),
        "hash-2",
        ScanTriggerType.CONTINUOUS_INTEGRATION
    );

    // Scenario 1: both CI and SCM filter parameters are null
    final ApiPageResult<IntegrationStatusDTO> resultOne =
        integrationService.getIntegrationStatuses(1, 10, null, null, null, null);
    assertThat(resultOne.getTotal()).isEqualTo(4);

    final List<IntegrationStatusDTO> appSummariesOne = resultOne.getResults();
    assertThat(appSummariesOne).hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummariesOne.get(0);
    assertThat(app1Dto.getApplicationId()).isEqualTo(app1.getId());
    assertThat(app1Dto.isAutomatedSourceControlFeedbackEnabled()).isFalse();
    assertThat(app1Dto.isCiIntegrationEnabled()).isTrue();

    final IntegrationStatusDTO app4Dto = appSummariesOne.get(3);
    assertThat(app4Dto.getApplicationId()).isEqualTo(app4.getId());
    assertThat(app4Dto.isAutomatedSourceControlFeedbackEnabled()).isTrue();
    assertThat(app4Dto.isCiIntegrationEnabled()).isFalse();

    // Scenario 2: both CI and SCM filter parameters are set to true
    final ApiPageResult<IntegrationStatusDTO> resultTwo =
        integrationService.getIntegrationStatuses(1, 10, null, null, true, true);
    assertThat(resultTwo.getTotal()).isEqualTo(1);

    final List<IntegrationStatusDTO> appSummariesTwo = resultTwo.getResults();
    assertThat(appSummariesTwo).hasSize(1);

    final IntegrationStatusDTO app2Dto = appSummariesTwo.get(0);
    assertThat(app2Dto.getApplicationId()).isEqualTo(app2.getId());
    assertThat(app2Dto.isAutomatedSourceControlFeedbackEnabled()).isTrue();
    assertThat(app2Dto.isCiIntegrationEnabled()).isTrue();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_FilterOnName_postgres() {
    final Organization org = tempEntity.newOrganization();
    app1 = tempEntity.newApplication("SeaL", "app1", org.getId());
    app2 = tempEntity.newApplication("rEAl", "app2", org.getId());
    app3 = tempEntity.newApplication("blob", "app3", org.getId());

    // CONTAINS
    final ApiPageResult<IntegrationStatusDTO> containsResult =
        integrationService.getIntegrationStatuses(1, 100, null, "ea", null, null);
    final List<IntegrationStatusDTO> appSummaries = containsResult.getResults();
    assertThat(appSummaries)
        .hasSize(2);
    assertThat(appSummaries.get(0).getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(appSummaries.get(1).getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(appSummaries)
        .extracting("applicationId")
        .doesNotContain(app3.getId());

    // DOES NOT CONTAIN
    final ApiPageResult<IntegrationStatusDTO> notContainsResult =
        integrationService.getIntegrationStatuses(1, 100, null, "298df", null, null);
    assertThat(notContainsResult.getResults())
        .isEmpty();
  }

  @Test
  public void testGetIntegrationSummaries_FilterOnScmIntegrationStatus_h2()  throws Exception {
    setUpAppsWithBuildStageRisk();
    final String repoUrl = "https://example.com/organization/project";
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"),
        SourceControlProvider.GITHUB);
    // Explicitly set SCM Integration Status to false for app 1
    tempEntity.newSourceControl(app1.getId(), repoUrl, null, null, null, null, false,
            null, null, null, true, true, "/target/*", true, false);
    // Explicitly set SCM Integration Status to true for app 2
    tempEntity.newSourceControl(app2.getId(), repoUrl, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, true);
    // App 3 source control record does not exist == source control disabled == ASCF disabled

    final ApiPageResult<IntegrationStatusDTO> resultOne =
        integrationService.getIntegrationStatuses(1, 100, null, null, false, null);
    assertThat(resultOne.getTotal()).isEqualTo(3);
    final List<IntegrationStatusDTO> appSummariesOne = resultOne.getResults();
    assertThat(appSummariesOne).hasSize(3);

    final IntegrationStatusDTO app1Dto = appSummariesOne.get(0);
    assertThat(app1Dto.getApplicationId()).isEqualTo(app1.getId());
    assertThat(app1Dto.isAutomatedSourceControlFeedbackEnabled()).isFalse();

    final IntegrationStatusDTO app3Dto = appSummariesOne.get(1);
    assertThat(app3Dto.getApplicationId()).isEqualTo(app3.getId());
    assertThat(app3Dto.isAutomatedSourceControlFeedbackEnabled()).isFalse();

    final ApiPageResult<IntegrationStatusDTO> resultTwo =
        integrationService.getIntegrationStatuses(1, 100, null, null, true, null);
    final List<IntegrationStatusDTO> appSummariesTwo = resultTwo.getResults();
    assertThat(appSummariesTwo).hasSize(1);

    final IntegrationStatusDTO app2Dto = appSummariesTwo.get(0);
    assertThat(app2Dto.getApplicationId()).isEqualTo(app2.getId());
    assertThat(app2Dto.isAutomatedSourceControlFeedbackEnabled()).isTrue();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_FilterOnScmIntegrationStatus_postgres()  throws Exception {
    setUpAppsWithBuildStageRisk();
    final String repoUrl = "https://example.com/organization/project";
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"),
        SourceControlProvider.GITHUB);
    // Set ASCF to false for app 1
    tempEntity.newSourceControl(app1.getId(), repoUrl, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, false);
    // Set ASCF to true for app 2
    tempEntity.newSourceControl(app2.getId(), repoUrl, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, true);
    // App 3 source control record does not exist == source control disabled == ASCF disabled

    final ApiPageResult<IntegrationStatusDTO> resultOne =
        integrationService.getIntegrationStatuses(1, 100, null, null, false, null);
    assertThat(resultOne.getTotal()).isEqualTo(3);
    final List<IntegrationStatusDTO> appSummariesOne = resultOne.getResults();
    assertThat(appSummariesOne).hasSize(3);

    final IntegrationStatusDTO app1Dto = appSummariesOne.get(0);
    assertThat(app1Dto.getApplicationId()).isEqualTo(app1.getId());
    assertThat(app1Dto.isAutomatedSourceControlFeedbackEnabled()).isFalse();

    final IntegrationStatusDTO app3Dto = appSummariesOne.get(1);
    assertThat(app3Dto.getApplicationId()).isEqualTo(app3.getId());
    assertThat(app3Dto.isAutomatedSourceControlFeedbackEnabled()).isFalse();

    final ApiPageResult<IntegrationStatusDTO> resultTwo =
        integrationService.getIntegrationStatuses(1, 100, null, null, true, null);
    final List<IntegrationStatusDTO> appSummariesTwo = resultTwo.getResults();
    assertThat(appSummariesTwo).hasSize(1);

    final IntegrationStatusDTO app2Dto = appSummariesTwo.get(0);
    assertThat(app2Dto.getApplicationId()).isEqualTo(app2.getId());
    assertThat(app2Dto.isAutomatedSourceControlFeedbackEnabled()).isTrue();
  }

  @Test
  public void testGetIntegrationSummaries_FilterOnCiIntegrationStatus_h2() {
    setUpAppsWithBuildStageRisk();
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan-id-3",
        false, false, false, new Date(), "hash-1", ScanTriggerType.CONTINUOUS_INTEGRATION);

    final ApiPageResult<IntegrationStatusDTO> resultOne =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, false);
    final List<IntegrationStatusDTO> appSummariesOne = resultOne.getResults();
    assertThat(resultOne.getTotal()).isEqualTo(3);

    assertThat(appSummariesOne).hasSize(3);
    assertThat(appSummariesOne.get(0).isCiIntegrationEnabled()).isFalse();
    assertThat(appSummariesOne.get(1).isCiIntegrationEnabled()).isFalse();
    assertThat(appSummariesOne.get(2).isCiIntegrationEnabled()).isFalse();

    final ApiPageResult<IntegrationStatusDTO> resultTwo =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, true);
    final List<IntegrationStatusDTO> appSummariesTwo = resultTwo.getResults();

    assertThat(appSummariesTwo).hasSize(1);
    assertThat(appSummariesTwo.get(0).isCiIntegrationEnabled()).isTrue();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_FilterOnCiIntegrationStatus_postgres() {
    setUpAppsWithBuildStageRisk();
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan-id-3",
        false, false, false, new Date(), "hash-1", ScanTriggerType.CONTINUOUS_INTEGRATION);

    final ApiPageResult<IntegrationStatusDTO> resultOne =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, false);
    final List<IntegrationStatusDTO> appSummariesOne = resultOne.getResults();
    assertThat(resultOne.getTotal()).isEqualTo(3);

    assertThat(appSummariesOne).hasSize(3);
    assertThat(appSummariesOne.get(0).isCiIntegrationEnabled()).isFalse();
    assertThat(appSummariesOne.get(1).isCiIntegrationEnabled()).isFalse();
    assertThat(appSummariesOne.get(2).isCiIntegrationEnabled()).isFalse();

    final ApiPageResult<IntegrationStatusDTO> resultTwo =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, true);
    final List<IntegrationStatusDTO> appSummariesTwo = resultTwo.getResults();

    assertThat(appSummariesTwo).hasSize(1);
    assertThat(appSummariesTwo.get(0).isCiIntegrationEnabled()).isTrue();
  }

  @Test
  public void testGetIntegrationSummaries_SetCICDStatus_h2() {
    setUpAppsWithBuildStageRisk();
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan-id-3",
        false, false, false, new Date(), "hash-1", ScanTriggerType.CONTINUOUS_INTEGRATION);
    tempEntity.newPolicyEvaluation(app4.getId(), Stage.ID_BUILD, "scan-id4",
        false, false, false, new Date(), "hash-4", ScanTriggerType.CONTINUOUS_INTEGRATION);

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(app1Dto.getOrganizationId())
        .isEqualTo(org1.getId());

    final IntegrationStatusDTO app4Dto = appSummaries.get(3);
    assertThat(app4Dto.getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(app4Dto.getOrganizationId())
        .isEqualTo(org3.getId());

    assertThat(app1Dto.isCiIntegrationEnabled())
        .isTrue();
    assertThat(appSummaries.get(1).isCiIntegrationEnabled())
        .isFalse();
    assertThat(appSummaries.get(2).isCiIntegrationEnabled())
        .isFalse();
    assertThat(appSummaries.get(3).isCiIntegrationEnabled())
        .isTrue();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_SetCICDStatus_postgres() {
    setUpAppsWithBuildStageRisk();
    tempEntity.newPolicyEvaluation(app1.getId(), Stage.ID_BUILD, "scan-id-3",
        false, false, false, new Date(), "hash-1", ScanTriggerType.CONTINUOUS_INTEGRATION);
    tempEntity.newPolicyEvaluation(app4.getId(), Stage.ID_BUILD, "scan-id4",
        false, false, false, new Date(), "hash-4", ScanTriggerType.CONTINUOUS_INTEGRATION);

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(app1Dto.getOrganizationId())
        .isEqualTo(org1.getId());

    final IntegrationStatusDTO app4Dto = appSummaries.get(3);
    assertThat(app4Dto.getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(app4Dto.getOrganizationId())
        .isEqualTo(org3.getId());

    assertThat(app1Dto.isCiIntegrationEnabled())
        .isTrue();
    assertThat(appSummaries.get(1).isCiIntegrationEnabled())
        .isFalse();
    assertThat(appSummaries.get(2).isCiIntegrationEnabled())
        .isFalse();
    assertThat(appSummaries.get(3).isCiIntegrationEnabled())
        .isTrue();
  }

  @Test
  public void testGetIntegrationSummaries_SetAutoSourceControlFeedbackStatus_h2() throws Exception {
    setUpAppsWithBuildStageRisk();

    final String repoUrl = "https://example.com/organization/project";
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"),
        SourceControlProvider.GITHUB);
    // Set ASCF to false for app 1
    tempEntity.newSourceControl(app1.getId(), repoUrl, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, false);
    // Set ASCF to true for app 2
    tempEntity.newSourceControl(app2.getId(), repoUrl, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, true);
    // App 3 source control record does not exist == source control disabled == ASCF disabled

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(app1Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app1Dto.isAutomatedSourceControlFeedbackEnabled())
        .isFalse();

    final IntegrationStatusDTO app2Dto = appSummaries.get(1);
    assertThat(app2Dto.getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(app2Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app2Dto.isAutomatedSourceControlFeedbackEnabled())
        .isTrue();

    final IntegrationStatusDTO app3Dto = appSummaries.get(2);
    assertThat(app3Dto.getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(app3Dto.getOrganizationId())
        .isEqualTo(org2.getId());
    assertThat(app3Dto.isAutomatedSourceControlFeedbackEnabled())
        .isFalse();

    final IntegrationStatusDTO app4Dto = appSummaries.get(3);
    assertThat(app4Dto.getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(app4Dto.getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(app4Dto.isAutomatedSourceControlFeedbackEnabled())
        .isFalse();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_SetAutoSourceControlFeedbackStatus_postgres() throws Exception {
    setUpAppsWithBuildStageRisk();

    final String repoUrl = "https://example.com/organization/project";
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"),
        SourceControlProvider.GITHUB);
    // Set ASCF to false for app 1
    tempEntity.newSourceControl(app1.getId(), repoUrl, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, false);
    // Set ASCF to true for app 2
    tempEntity.newSourceControl(app2.getId(), repoUrl, null, null, null, null, false,
        null, null, null, true, true, "/target/*", true, true);
    // App 3 source control record does not exist == source control disabled == ASCF disabled

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(app1Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app1Dto.isAutomatedSourceControlFeedbackEnabled())
        .isFalse();

    final IntegrationStatusDTO app2Dto = appSummaries.get(1);
    assertThat(app2Dto.getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(app2Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app2Dto.isAutomatedSourceControlFeedbackEnabled())
        .isTrue();

    final IntegrationStatusDTO app3Dto = appSummaries.get(2);
    assertThat(app3Dto.getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(app3Dto.getOrganizationId())
        .isEqualTo(org2.getId());
    assertThat(app3Dto.isAutomatedSourceControlFeedbackEnabled())
        .isFalse();

    final IntegrationStatusDTO app4Dto = appSummaries.get(3);
    assertThat(app4Dto.getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(app4Dto.getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(app4Dto.isAutomatedSourceControlFeedbackEnabled())
        .isFalse();
  }

  @Test
  public void testGetIntegrationSummaries_SetLastCommit_h2() {
    setUpAppsWithBuildStageRisk();

    final Date commitTime = new Date();
    final Date latestCommitTime1 = new Date(commitTime.getTime() + 5000);
    final Date latestCommitTime2 = new Date(commitTime.getTime() + 10_000);

    // App 1
    tempEntity.newSourceControlDefaultBranchCommitHistory(app1.getId(), "commit1", commitTime, null);
    tempEntity.newSourceControlDefaultBranchCommitHistory(app1.getId(), "commit2", latestCommitTime1, null);

    // App 2
    tempEntity.newSourceControlDefaultBranchCommitHistory(app2.getId(), "commit1", commitTime, null);
    tempEntity.newSourceControlDefaultBranchCommitHistory(app2.getId(), "commit2", latestCommitTime2, null);

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(app1Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app1Dto.getLastCommitTimestamp())
        .isEqualTo(latestCommitTime1.getTime());

    final IntegrationStatusDTO app2Dto = appSummaries.get(1);
    assertThat(app2Dto.getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(app2Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app2Dto.getLastCommitTimestamp())
        .isEqualTo(latestCommitTime2.getTime());

    final IntegrationStatusDTO app3Dto = appSummaries.get(2);
    assertThat(app3Dto.getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(app3Dto.getOrganizationId())
        .isEqualTo(org2.getId());
    assertThat(app3Dto.getLastCommitTimestamp())
        .isZero();

    final IntegrationStatusDTO app4Dto = appSummaries.get(3);
    assertThat(app4Dto.getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(app4Dto.getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(app4Dto.getLastCommitTimestamp())
        .isZero();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_SetLastCommit_postgres() {
    setUpAppsWithBuildStageRisk();

    final Date commitTime = new Date();
    final Date latestCommitTime1 = new Date(commitTime.getTime() + 5000);
    final Date latestCommitTime2 = new Date(commitTime.getTime() + 10_000);

    // App 1
    tempEntity.newSourceControlDefaultBranchCommitHistory(app1.getId(), "commit1", commitTime, null);
    tempEntity.newSourceControlDefaultBranchCommitHistory(app1.getId(), "commit2", latestCommitTime1, null);

    // App 2
    tempEntity.newSourceControlDefaultBranchCommitHistory(app2.getId(), "commit1", commitTime, null);
    tempEntity.newSourceControlDefaultBranchCommitHistory(app2.getId(), "commit2", latestCommitTime2, null);

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(app1Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app1Dto.getLastCommitTimestamp())
        .isEqualTo(latestCommitTime1.getTime());

    final IntegrationStatusDTO app2Dto = appSummaries.get(1);
    assertThat(app2Dto.getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(app2Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app2Dto.getLastCommitTimestamp())
        .isEqualTo(latestCommitTime2.getTime());

    final IntegrationStatusDTO app3Dto = appSummaries.get(2);
    assertThat(app3Dto.getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(app3Dto.getOrganizationId())
        .isEqualTo(org2.getId());
    assertThat(app3Dto.getLastCommitTimestamp())
        .isZero();

    final IntegrationStatusDTO app4Dto = appSummaries.get(3);
    assertThat(app4Dto.getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(app4Dto.getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(app4Dto.getLastCommitTimestamp())
        .isZero();
  }

  @Test
  public void testGetIntegrationSummaries_SetLastEvaluation_h2() {
    setUpAppsWithBuildStageRisk();

    final Date evalTime = new Date();
    final Date latestEvalTime1 = new Date(evalTime.getTime() + 6000);
    final Date latestEvalTime2 = new Date(evalTime.getTime() + 12_000);

    // App 1
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1", evalTime);
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId2", latestEvalTime1);

    // App 2
    tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1", evalTime);
    tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId2", latestEvalTime2);

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(app1Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app1Dto.getLastEvaluationTimestamp())
        .isEqualTo(latestEvalTime1.getTime());

    final IntegrationStatusDTO app2Dto = appSummaries.get(1);
    assertThat(app2Dto.getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(app2Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app2Dto.getLastEvaluationTimestamp())
        .isEqualTo(latestEvalTime2.getTime());

    final IntegrationStatusDTO app3Dto = appSummaries.get(2);
    assertThat(app3Dto.getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(app3Dto.getOrganizationId())
        .isEqualTo(org2.getId());
    assertThat(app3Dto.getLastEvaluationTimestamp())
        .isZero();

    final IntegrationStatusDTO app4Dto = appSummaries.get(3);
    assertThat(app4Dto.getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(app4Dto.getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(app4Dto.getLastEvaluationTimestamp())
        .isZero();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_SetLastEvaluation_postgres() {
    setUpAppsWithBuildStageRisk();

    final Date evalTime = new Date();
    final Date latestEvalTime1 = new Date(evalTime.getTime() + 6000);
    final Date latestEvalTime2 = new Date(evalTime.getTime() + 12_000);

    // App 1
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId1", evalTime);
    tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "scanId2", latestEvalTime1);

    // App 2
    tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId1", evalTime);
    tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, "scanId2", latestEvalTime2);

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(app1Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app1Dto.getLastEvaluationTimestamp())
        .isEqualTo(latestEvalTime1.getTime());

    final IntegrationStatusDTO app2Dto = appSummaries.get(1);
    assertThat(app2Dto.getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(app2Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app2Dto.getLastEvaluationTimestamp())
        .isEqualTo(latestEvalTime2.getTime());

    final IntegrationStatusDTO app3Dto = appSummaries.get(2);
    assertThat(app3Dto.getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(app3Dto.getOrganizationId())
        .isEqualTo(org2.getId());
    assertThat(app3Dto.getLastEvaluationTimestamp())
        .isZero();

    final IntegrationStatusDTO app4Dto = appSummaries.get(3);
    assertThat(app4Dto.getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(app4Dto.getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(app4Dto.getLastEvaluationTimestamp())
        .isZero();
  }

  @Test
  public void testGetIntegrationSummaries_SetTotalRisk_h2() {
    setUpAppsWithBuildStageRisk();

    // Add non-build stage risk
    final Application app5 = tempEntity.newApplication("app5", "app5", org3.getId());
    final Policy app5Policy = tempEntity.newPolicy(app5.getId(), "app5 owned policy", 5);
    final PolicyEvaluation app5PolicyEvaluation1 =
        tempEntity.newPolicyEvaluation(app5.getId(), ReleaseStageType.ID, "scan-id-1", new Date(0L));
    tempEntity.newPolicyViolation(app5PolicyEvaluation1, app5Policy);

    // Add risk of 0
    final Application app6 = tempEntity.newApplication("app6", "app6", org3.getId());
    final Policy app6Policy = tempEntity.newPolicy(app5.getId(), "app6 owned policy", 5);
    final PolicyEvaluation app6PolicyEvaluation =
        tempEntity.newPolicyEvaluation(app6.getId(), BuildStageType.ID, "scan-id-2", new Date(0L));
    tempEntity.newPolicyViolation(app6PolicyEvaluation, app6Policy, 0, app6Policy.getThreatCategory(), "Group1",
        "Artifact1", "Version1");

    // App 1 = 8 total build stage risk
    // App 2 = 3 total build stage risk
    // App 3 = -1 total build stage risk (no app evals)
    // App 4 = -1 total build stage risk (no app evals)
    // App 5 = -1 total build stage risk (only non-build stage risk)
    // App 6 = 0 total build stage risk (no risk found in app eval)

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(6);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(app1Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app1Dto.getTotalRiskScore())
        .isEqualTo(8);

    final IntegrationStatusDTO app2Dto = appSummaries.get(1);
    assertThat(app2Dto.getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(app2Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app2Dto.getTotalRiskScore())
        .isEqualTo(3);

    final IntegrationStatusDTO app3Dto = appSummaries.get(2);
    assertThat(app3Dto.getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(app3Dto.getOrganizationId())
        .isEqualTo(org2.getId());
    assertThat(app3Dto.getTotalRiskScore())
        .isEqualTo(-1);

    final IntegrationStatusDTO app4Dto = appSummaries.get(3);
    assertThat(app4Dto.getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(app4Dto.getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(app4Dto.getTotalRiskScore())
        .isEqualTo(-1);

    final IntegrationStatusDTO app5Dto = appSummaries.get(4);
    assertThat(app5Dto.getApplicationId())
        .isEqualTo(app5.getId());
    assertThat(app5Dto.getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(app5Dto.getTotalRiskScore())
        .isEqualTo(-1);

    final IntegrationStatusDTO app6Dto = appSummaries.get(5);
    assertThat(app6Dto.getApplicationId())
        .isEqualTo(app6.getId());
    assertThat(app6Dto.getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(app6Dto.getTotalRiskScore())
        .isZero();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_SetTotalRisk_postgres() {
    setUpAppsWithBuildStageRisk();

    // Add non-build stage risk
    final Application app5 = tempEntity.newApplication("app5", "app5", org3.getId());
    final Policy app5Policy = tempEntity.newPolicy(app5.getId(), "app5 owned policy", 5);
    final PolicyEvaluation app5PolicyEvaluation1 =
        tempEntity.newPolicyEvaluation(app5.getId(), ReleaseStageType.ID, "scan-id-1", new Date(0L));
    tempEntity.newPolicyViolation(app5PolicyEvaluation1, app5Policy);

    // Add risk of 0
    final Application app6 = tempEntity.newApplication("app6", "app6", org3.getId());
    final Policy app6Policy = tempEntity.newPolicy(app5.getId(), "app6 owned policy", 5);
    final PolicyEvaluation app6PolicyEvaluation =
        tempEntity.newPolicyEvaluation(app6.getId(), BuildStageType.ID, "scan-id-2", new Date(0L));
    tempEntity.newPolicyViolation(app6PolicyEvaluation, app6Policy, 0, app6Policy.getThreatCategory(), "Group1",
        "Artifact1", "Version1");

    // App 1 = 8 total build stage risk
    // App 2 = 3 total build stage risk
    // App 3 = -1 total build stage risk (no app evals)
    // App 4 = -1 total build stage risk (no app evals)
    // App 5 = -1 total build stage risk (only non-build stage risk)
    // App 6 = 0 total build stage risk (no risk found in app eval)

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(6);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.getApplicationId())
        .isEqualTo(app1.getId());
    assertThat(app1Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app1Dto.getTotalRiskScore())
        .isEqualTo(8);

    final IntegrationStatusDTO app2Dto = appSummaries.get(1);
    assertThat(app2Dto.getApplicationId())
        .isEqualTo(app2.getId());
    assertThat(app2Dto.getOrganizationId())
        .isEqualTo(org1.getId());
    assertThat(app2Dto.getTotalRiskScore())
        .isEqualTo(3);

    final IntegrationStatusDTO app3Dto = appSummaries.get(2);
    assertThat(app3Dto.getApplicationId())
        .isEqualTo(app3.getId());
    assertThat(app3Dto.getOrganizationId())
        .isEqualTo(org2.getId());
    assertThat(app3Dto.getTotalRiskScore())
        .isEqualTo(-1);

    final IntegrationStatusDTO app4Dto = appSummaries.get(3);
    assertThat(app4Dto.getApplicationId())
        .isEqualTo(app4.getId());
    assertThat(app4Dto.getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(app4Dto.getTotalRiskScore())
        .isEqualTo(-1);

    final IntegrationStatusDTO app5Dto = appSummaries.get(4);
    assertThat(app5Dto.getApplicationId())
        .isEqualTo(app5.getId());
    assertThat(app5Dto.getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(app5Dto.getTotalRiskScore())
        .isEqualTo(-1);

    final IntegrationStatusDTO app6Dto = appSummaries.get(5);
    assertThat(app6Dto.getApplicationId())
        .isEqualTo(app6.getId());
    assertThat(app6Dto.getOrganizationId())
        .isEqualTo(org3.getId());
    assertThat(app6Dto.getTotalRiskScore())
        .isZero();
  }

  @Test
  public void testGetIntegrationSummaries_SetSastScan_NoScmContext_h2() {
    setUpAppsWithBuildStageRisk();
    // 3 Scans of the same app. We should find the last one.
    tempEntity.newSastScanWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2023-12-01").atStartOfDay(ZoneId.systemDefault()).toInstant()));
    tempEntity.newSastScanWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2023-12-02").atStartOfDay(ZoneId.systemDefault()).toInstant()));
    tempEntity.newSastScanWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2023-12-03").atStartOfDay(ZoneId.systemDefault()).toInstant()));

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.isHasSastReport())
        .isTrue();
    assertThat(app1Dto.getLastSastReportId())
        .isNotEmpty();
    assertThat(app1Dto.getLastSastReportTime())
        .isEqualTo(LocalDate.parse("2023-12-03").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

    assertThat(appSummaries.get(1).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(2).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(3).isHasSastReport())
        .isFalse();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_SetSastScan_NoScmContext_postgres() {
    setUpAppsWithBuildStageRisk();
    // 3 Scans of the same app. We should find the last one.
    tempEntity.newSastScanWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2023-12-01").atStartOfDay(ZoneId.systemDefault()).toInstant()));
    tempEntity.newSastScanWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2023-12-02").atStartOfDay(ZoneId.systemDefault()).toInstant()));
    tempEntity.newSastScanWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2023-12-03").atStartOfDay(ZoneId.systemDefault()).toInstant()));

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.isHasSastReport())
        .isTrue();
    assertThat(app1Dto.getLastSastReportId())
        .isNotEmpty();
    assertThat(app1Dto.getLastSastReportTime())
        .isEqualTo(LocalDate.parse("2023-12-03").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

    assertThat(appSummaries.get(1).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(2).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(3).isHasSastReport())
        .isFalse();
  }

  @Test
  public void testGetIntegrationSummaries_SetSastScan_WithScmContext_OverrideBaseBranch_BranchScanExists_H2()
      throws Exception
  {
    setUpAppsWithBuildStageRisk();
    final String baseBranch = "TEST_BRANCH";
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setBaseBranch(baseBranch);
    sourceControl.setOwnerId(app1.getId());
    sourceControl.setRepositoryUrl("https://github.com/org/proj");
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);

    // 3 scans of the same app, but the latest is not of the base branch
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-01").atStartOfDay(ZoneId.systemDefault()).toInstant()), baseBranch);
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant()), baseBranch);
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-03").atStartOfDay(ZoneId.systemDefault()).toInstant()), "branch");

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.isHasSastReport())
        .isTrue();
    assertThat(app1Dto.getLastSastReportId())
        .isNotEmpty();
    assertThat(app1Dto.getLastSastReportTime())
        .isEqualTo(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

    assertThat(appSummaries.get(1).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(2).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(3).isHasSastReport())
        .isFalse();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_SetSastScan_WithScmContext_OverrideBaseBranch_BranchScanExists_Postgres()
      throws Exception
  {
    setUpAppsWithBuildStageRisk();
    final String baseBranch = "TEST_BRANCH";
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setBaseBranch(baseBranch);
    sourceControl.setOwnerId(app1.getId());
    sourceControl.setRepositoryUrl("https://github.com/org/proj");
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);

    // 3 scans of the same app, but the latest is not of the base branch
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-01").atStartOfDay(ZoneId.systemDefault()).toInstant()), baseBranch);
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant()), baseBranch);
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-03").atStartOfDay(ZoneId.systemDefault()).toInstant()), "branch");

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.isHasSastReport())
        .isTrue();
    assertThat(app1Dto.getLastSastReportId())
        .isNotEmpty();
    assertThat(app1Dto.getLastSastReportTime())
        .isEqualTo(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

    assertThat(appSummaries.get(1).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(2).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(3).isHasSastReport())
        .isFalse();
  }

  @Test
  public void testGetIntegrationSummaries_SetSastScan_WithScmContext_InheritRootBaseBranch_BranchScanExists_H2()
      throws Exception
  {
    setUpAppsWithBuildStageRisk();
    final String baseBranch = "TEST_BRANCH";
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB, null, null,
        baseBranch);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(app1.getId());
    sourceControl.setRepositoryUrl("https://github.com/org/proj");
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);

    // 3 scans of the same app, but the latest is not of the base branch
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-01").atStartOfDay(ZoneId.systemDefault()).toInstant()), baseBranch);
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant()), baseBranch);
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-03").atStartOfDay(ZoneId.systemDefault()).toInstant()), "branch");

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.isHasSastReport())
        .isTrue();
    assertThat(app1Dto.getLastSastReportId())
        .isNotEmpty();
    assertThat(app1Dto.getLastSastReportTime())
        .isEqualTo(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

    assertThat(appSummaries.get(1).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(2).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(3).isHasSastReport())
        .isFalse();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_SetSastScan_WithScmContext_InheritRootBaseBranch_BranchScanExists_Postgres()
      throws Exception
  {
    setUpAppsWithBuildStageRisk();
    final String baseBranch = "TEST_BRANCH";
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB, null, null,
        baseBranch);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setOwnerId(app1.getId());
    sourceControl.setRepositoryUrl("https://github.com/org/proj");
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);

    // 3 scans of the same app, but the latest is not of the base branch
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-01").atStartOfDay(ZoneId.systemDefault()).toInstant()), baseBranch);
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant()), baseBranch);
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-03").atStartOfDay(ZoneId.systemDefault()).toInstant()), "branch");

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.isHasSastReport())
        .isTrue();
    assertThat(app1Dto.getLastSastReportId())
        .isNotEmpty();
    assertThat(app1Dto.getLastSastReportTime())
        .isEqualTo(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

    assertThat(appSummaries.get(1).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(2).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(3).isHasSastReport())
        .isFalse();
  }

  @Test
  public void testGetIntegrationSummaries_SetSastScan_WithScmContext_InheritOrgBaseBranch_BranchScanExists_H2()
      throws Exception
  {
    setUpAppsWithBuildStageRisk();
    final String baseBranch = "TEST_BRANCH";
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final Application appWithOrgParent = tempEntity.newApplication(org3.getId());

    final SourceControl sourceControlOrg = new SourceControl();
    sourceControlOrg.setOwnerId(org3.getId());
    sourceControlOrg.setBaseBranch(baseBranch);
    tempEntity.newSourceControl(sourceControlOrg);

    final SourceControl sourceControlAppWithOrgParent = new SourceControl();
    sourceControlAppWithOrgParent.setOwnerId(appWithOrgParent.getId());
    sourceControlAppWithOrgParent.setRepositoryUrl("https://github.com/org/proj");
    sourceControlAppWithOrgParent.setToken(new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControlAppWithOrgParent);

    // 3 scans of the same app, but the latest is not of the base branch
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(appWithOrgParent.getId(),
        Date.from(LocalDate.parse("2024-01-01").atStartOfDay(ZoneId.systemDefault()).toInstant()), baseBranch);
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(appWithOrgParent.getId(),
        Date.from(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant()), baseBranch);
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(appWithOrgParent.getId(),
        Date.from(LocalDate.parse("2024-01-03").atStartOfDay(ZoneId.systemDefault()).toInstant()), "branch");

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(5);

    final IntegrationStatusDTO app1Dto = appSummaries.get(4);
    assertThat(app1Dto.isHasSastReport())
        .isTrue();
    assertThat(app1Dto.getLastSastReportId())
        .isNotEmpty();
    assertThat(app1Dto.getLastSastReportTime())
        .isEqualTo(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

    assertThat(appSummaries.get(0).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(1).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(2).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(3).isHasSastReport())
        .isFalse();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_SetSastScan_WithScmContext_InheritOrgBaseBranch_BranchScanExists_Postgres()
      throws Exception
  {
    setUpAppsWithBuildStageRisk();
    final String baseBranch = "TEST_BRANCH";
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final Application appWithOrgParent = tempEntity.newApplication(org3.getId());

    final SourceControl sourceControlOrg = new SourceControl();
    sourceControlOrg.setOwnerId(org3.getId());
    sourceControlOrg.setBaseBranch(baseBranch);
    tempEntity.newSourceControl(sourceControlOrg);

    final SourceControl sourceControlAppWithOrgParent = new SourceControl();
    sourceControlAppWithOrgParent.setOwnerId(appWithOrgParent.getId());
    sourceControlAppWithOrgParent.setRepositoryUrl("https://github.com/org/proj");
    sourceControlAppWithOrgParent.setToken(new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControlAppWithOrgParent);

    // 3 scans of the same app, but the latest is not of the base branch
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(appWithOrgParent.getId(),
        Date.from(LocalDate.parse("2024-01-01").atStartOfDay(ZoneId.systemDefault()).toInstant()), baseBranch);
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(appWithOrgParent.getId(),
        Date.from(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant()), baseBranch);
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(appWithOrgParent.getId(),
        Date.from(LocalDate.parse("2024-01-03").atStartOfDay(ZoneId.systemDefault()).toInstant()), "branch");

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(5);

    final IntegrationStatusDTO app1Dto = appSummaries.get(4);
    assertThat(app1Dto.isHasSastReport())
        .isTrue();
    assertThat(app1Dto.getLastSastReportId())
        .isNotEmpty();
    assertThat(app1Dto.getLastSastReportTime())
        .isEqualTo(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

    assertThat(appSummaries.get(0).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(1).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(2).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(3).isHasSastReport())
        .isFalse();
  }

  @Test
  public void testGetIntegrationSummaries_SetSastScan_WithScmContext_NoBranchScanExists_H2() throws Exception {
    setUpAppsWithBuildStageRisk();
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setBaseBranch("TEST_BRANCH");
    sourceControl.setOwnerId(app1.getId());
    sourceControl.setRepositoryUrl("https://github.com/org/proj");
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);

    // 3 scans of the same app, but none of the base branch - default to latest
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-01").atStartOfDay(ZoneId.systemDefault()).toInstant()), "branch1");
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant()), "branch2");
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-03").atStartOfDay(ZoneId.systemDefault()).toInstant()), "branch3");

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.isHasSastReport())
        .isTrue();
    assertThat(app1Dto.getLastSastReportId())
        .isNotEmpty();
    assertThat(app1Dto.getLastSastReportTime())
        .isEqualTo(LocalDate.parse("2024-01-03").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

    assertThat(appSummaries.get(1).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(2).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(3).isHasSastReport())
        .isFalse();
  }

  @Test
  @PostgresTest
  public void testGetIntegrationSummaries_SetSastScan_WithScmContext_NoBranchScanExists_Postgres() throws Exception {
    setUpAppsWithBuildStageRisk();
    tempEntity.newSourceControl(Organization.ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    final SourceControl sourceControl = new SourceControl();
    sourceControl.setBaseBranch("TEST_BRANCH");
    sourceControl.setOwnerId(app1.getId());
    sourceControl.setRepositoryUrl("https://github.com/org/proj");
    sourceControl.setToken(new DefaultPlexusCipher().encrypt("root-token", "CMMDwoV"));
    tempEntity.newSourceControl(sourceControl);

    // 3 scans of the same app, but none of the base branch - default to latest
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-01").atStartOfDay(ZoneId.systemDefault()).toInstant()), "branch1");
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-02").atStartOfDay(ZoneId.systemDefault()).toInstant()), "branch2");
    tempEntity.newSastScanWithScmContextWithCustomTimestamp(app1.getId(),
        Date.from(LocalDate.parse("2024-01-03").atStartOfDay(ZoneId.systemDefault()).toInstant()), "branch3");

    final ApiPageResult<IntegrationStatusDTO> result =
        integrationService.getIntegrationStatuses(1, 100, null, null, null, null);
    final List<IntegrationStatusDTO> appSummaries = result.getResults();
    assertThat(appSummaries)
        .hasSize(4);

    final IntegrationStatusDTO app1Dto = appSummaries.get(0);
    assertThat(app1Dto.isHasSastReport())
        .isTrue();
    assertThat(app1Dto.getLastSastReportId())
        .isNotEmpty();
    assertThat(app1Dto.getLastSastReportTime())
        .isEqualTo(LocalDate.parse("2024-01-03").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());

    assertThat(appSummaries.get(1).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(2).isHasSastReport())
        .isFalse();
    assertThat(appSummaries.get(3).isHasSastReport())
        .isFalse();
  }

  @Test
  public void testGetIntegrationStatuses_shouldSendTelemetryForFilterAndSearchUsageWhenAllFalse() {
    tempEntity.newApplication("app1", "app1", tempEntity.newOrganization().getId());

    // === everything false, order by name ===
    integrationService.getIntegrationStatuses(
        1,
        10,
        IntegrationStatusOrderByEnum.NAME.name(),
        null,
        null,
        null);

    assertTelemetrySentForAppIntegrationsFilters(buildAppIntegrationFilterTelemetry(
        false,
        false,
        false,
        IntegrationStatusOrderByEnum.NAME.name()
    ));
  }

  @Test
  public void testGetIntegrationStatuses_shouldSendTelemetryForFilterAndSearchUsageWhenSearchIncluded() {
    tempEntity.newApplication("app1", "app1", tempEntity.newOrganization().getId());

    // === search included; order by commit ===
    integrationService.getIntegrationStatuses(
        1,
        10,
        IntegrationStatusOrderByEnum.COMMIT.name(),
        "some-search",
        null,
        null);

    assertTelemetrySentForAppIntegrationsFilters(buildAppIntegrationFilterTelemetry(
        true,
        false,
        false,
        IntegrationStatusOrderByEnum.COMMIT.name()
    ));
  }

  @Test
  public void testGetIntegrationStatuses_shouldSendTelemetryForFilterAndSearchUsageWhenAppsFilteredByScm() {
    tempEntity.newApplication("app1", "app1", tempEntity.newOrganization().getId());

    // === search and scm filter included; order by total risk ===
    integrationService.getIntegrationStatuses(
        1,
        10,
        IntegrationStatusOrderByEnum.TOTAL_RISK.name(),
        "some-search",
        true,
        null);

    assertTelemetrySentForAppIntegrationsFilters(buildAppIntegrationFilterTelemetry(
        true,
        true,
        false,
        IntegrationStatusOrderByEnum.TOTAL_RISK.name()
    ));
  }

  @Test
  public void testGetIntegrationStatuses_shouldSendTelemetryForFilterAndSearchUsageWhenCiCdIncluded() {
    tempEntity.newApplication("app1", "app1", tempEntity.newOrganization().getId());

    // === search, scm filter, and cicd filter included; order by total evaluation ===
    integrationService.getIntegrationStatuses(
        1,
        10,
        IntegrationStatusOrderByEnum.EVALUATION.name(),
        "some-search",
        true,
        true);

    assertTelemetrySentForAppIntegrationsFilters(buildAppIntegrationFilterTelemetry(
        true,
        true,
        true,
        IntegrationStatusOrderByEnum.EVALUATION.name()
    ));
  }

  private TelemetryData buildAppIntegrationFilterTelemetry(
      final boolean includesAppNameSearch,
      final boolean includesScmIntegrationFilter,
      final boolean includesCiCdIntegrationFilter,
      final String orderBy
  )
  {
    final TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.DEVELOPER_INTEGRATIONS_DASHBOARD);
    final Map<String , Object> attributes = new HashMap<>();
    attributes.put("includes_app_name_search", includesAppNameSearch);
    attributes.put("includes_scm_integration_filter", includesScmIntegrationFilter);
    attributes.put("includes_ci_cd_integration_filter", includesCiCdIntegrationFilter);
    attributes.put("order_by", orderBy);
    telemetryData.setAttributes(attributes);

    return telemetryData;
  }

  private void assertTelemetrySentForAppIntegrationsFilters(final TelemetryData expectedTelemetryData) {
    verify(telemetrySender).send(telemetryDataArgumentCaptor.capture());

    final TelemetryData actualTelemetryData = telemetryDataArgumentCaptor.getValue();
    assertThat(expectedTelemetryData.getPurpose()).isEqualTo(actualTelemetryData.getPurpose());

    final Map<String, Object> actualAttributes = actualTelemetryData.getAttributes();
    final Map<String, Object> expectedAttributes = expectedTelemetryData.getAttributes();

    assertThat(actualAttributes).isEqualTo(expectedAttributes);
  }

  private void setUpAppsWithBuildStageRisk() {
    org1 = tempEntity.newOrganization();
    org2 = tempEntity.newOrganization();
    org3 = tempEntity.newOrganization();

    app1 = tempEntity.newApplication("app1", "app1", org1.getId());
    app2 = tempEntity.newApplication("app2", "app2", org1.getId());
    // No risk apps
    app3 = tempEntity.newApplication("app3", "app3", org2.getId());
    app4 = tempEntity.newApplication("app4", "app4", org3.getId());

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
