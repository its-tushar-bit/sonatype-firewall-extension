/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.dashboard;

import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentImportedSbomsDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentVulnerabilitiesDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ReleaseStatusDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilitiesThreadLevelMetricDTO;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class SbomDashboardServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private SbomDashboardService service;

  private ThirdPartyFileCoordinate component;

  @Before
  public void before() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    component =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");
    ThirdPartyCoordinateSecurity vulnerability =
        tempEntity.newThirdPartyCoordinateSecurity(component, "cve", "d1", "l1", 9,
            "d1", "f1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(vulnerability, "cve", "resolved",
        "code_not_reachable", "response", "details");
  }

  @Test
  public void testGetRecentHighPriorityVulnerabilities_Unauthenticated() {
    List<RecentVulnerabilitiesDTO> results = service.getRecentHighPriorityVulnerabilities();
    assertThat(results).isEmpty();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRecentHighPriorityVulnerabilities_Authorized() {
    grantReadPermission(app.getId());

    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);

    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withCreatedAt(sixMonthsAgo)
        .build();

    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s1", "f1",
            "n1", "v1", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
        "r1", "d1", "l1", CvssV3Severity.CRITICAL.getStartScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "f1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity1, coordinateSecurity1.getRefId(),
        "state", "justification", "response", "detail");

    // No permission on this application yet
    Application newApplication = tempEntity.newApplicationWithParent();

    ThirdPartySbomMetadata sbomMetadata2 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(newApplication.getId())
        .withCreatedAt(oneYearAgo)
        .build();

    ThirdPartyFileCoordinate coordinate2 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata2.getThirdPartyFileId(), "s2", "f2",
            "n2", "v2", "", "");

    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "r2", "d2", "l2",
        CvssV3Severity.HIGH.getStartScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "f2");

    List<RecentVulnerabilitiesDTO> results = service.getRecentHighPriorityVulnerabilities();

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getRefId()).isEqualTo("r1");

    grantReadPermission(newApplication.getId());

    results = service.getRecentHighPriorityVulnerabilities();

    assertThat(results).hasSize(2);
    assertThat(results.get(0).getRefId()).isEqualTo("r1");
    assertThat(results.get(1).getRefId()).isEqualTo("r2");
  }

  @Test
  public void testGetSbomReleaseStatus_Unauthenticated() {
    ReleaseStatusDTO results = service.getSbomReleaseStatus();
    assertThat(results).isNotNull();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomReleaseStatus_Authorized() {
    grantReadPermission(app.getId());

    Date now = new Date();

    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withCreatedAt(now)
        .build();

    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s1", "f1",
            "n1", "v1", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
        sbomMetadata.getId(), "r1", "d1", "l1", CvssV3Severity.CRITICAL.getStartScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "f1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity1, coordinateSecurity1.getRefId(),
        "state", "justification", "response", "detail");

    // No permission on this application yet
    Application newApplication = tempEntity.newApplicationWithParent();

    ThirdPartySbomMetadata sbomMetadata2 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(newApplication.getId())
        .withCreatedAt(now)
        .build();

    ThirdPartyFileCoordinate coordinate2 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata2.getThirdPartyFileId(), "s2", "f2",
            "n2", "v2", "", "");

    tempEntity
        .newThirdPartyCoordinateSecurity(coordinate2, "r2", sbomMetadata2.getId(), "d2", "l2",
            10, CvssV3Severity.HIGH.getDisplayName(), "f2");

    ReleaseStatusDTO results = service.getSbomReleaseStatus();

    assertThat(results.getReleaseReadyCount()).isEqualTo(1L);
    assertThat(results.getNeedsAttentionCount()).isEqualTo(0L);
    assertThat(results.getPartiallyReadyCount()).isEqualTo(0L);

    grantReadPermission(newApplication.getId());

    results = service.getSbomReleaseStatus();

    assertThat(results.getReleaseReadyCount()).isEqualTo(1L);
    assertThat(results.getNeedsAttentionCount()).isEqualTo(1L);
    assertThat(results.getPartiallyReadyCount()).isEqualTo(0L);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRecentImportSboms_Authorized() {
    grantReadPermission(app.getId());

    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);

    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withCreatedAt(sixMonthsAgo)
        .build();

    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s1", "f1",
            "n1", "v1", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
        "r1", "d1", "l1", CvssV3Severity.CRITICAL.getStartScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "f1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity1, coordinateSecurity1.getRefId(),
        "state", "justification", "response", "detail");

    // No permission on this application yet
    Application newApplication = tempEntity.newApplicationWithParent();

    ThirdPartySbomMetadata sbomMetadata2 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(newApplication.getId())
        .withCreatedAt(oneYearAgo)
        .build();

    ThirdPartyFileCoordinate coordinate2 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata2.getThirdPartyFileId(), "s2", "f2",
            "n2", "v2", "", "");

    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "r2", "d2", "l2",
        CvssV3Severity.HIGH.getStartScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "f2");

    List<RecentImportedSbomsDTO> results = service.getRecentSbomsImported();

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getApplicationName()).isEqualTo(app.getName());

    grantReadPermission(newApplication.getId());

    results = service.getRecentSbomsImported();

    assertThat(results).hasSize(2);
    assertThat(results.get(0).getApplicationName()).isEqualTo(app.getName());
    assertThat(results.get(1).getApplicationName()).isEqualTo(newApplication.getName());
  }

  @Test
  public void testGetRecentImportSboms_Unauthenticated() {
    List<RecentImportedSbomsDTO> results = service.getRecentSbomsImported();
    assertThat(results).isEmpty();
  }

  @Test
  public void testGetVulnerabilitiesByThreatLevel_Unauthenticated() {
    VulnerabilitiesThreadLevelMetricDTO result = service.getVulnerabilitiesByThreatLevel();
    assertEmptyResult(result);
  }

  @Test
  public void testGetVulnerabilitiesByThreatLevel_Unauthorized() {
    login();
    VulnerabilitiesThreadLevelMetricDTO result = service.getVulnerabilitiesByThreatLevel();
    assertEmptyResult(result);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetVulnerabilitiesByThreatLevel_Authorized() {
    grantReadPermission(app.getId());

    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .build();

    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s1", "f1", "n1", "v1", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
        "r1", sbomMetadata.getId(), "d1", "l1", CvssV3Severity.LOW.getStartScoreRange(),
        CvssV3Severity.LOW.getDisplayName(), "f1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity1, coordinateSecurity1.getRefId(),
        "state", "justification", "response", "detail");

    // No permission on this application yet
    Application newApplication = tempEntity.newApplicationWithParent();

    ThirdPartySbomMetadata sbomMetadata2 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(newApplication.getId())
        .build();

    ThirdPartyFileCoordinate coordinate2 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata2.getThirdPartyFileId(), "s2", "f2", "n2", "v2", "", "");

    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "r2", sbomMetadata2.getId(), "d2", "l2",
        CvssV3Severity.HIGH.getStartScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "f2");

    VulnerabilitiesThreadLevelMetricDTO result = service.getVulnerabilitiesByThreatLevel();
    assertThat(result).isNotNull();

    assertThat(result.getLow()).isOne();
    assertThat(result.getLowAnnotated()).isOne();
    assertThat(result.getLowUnannotated()).isZero();

    assertThat(result.getMedium()).isZero();
    assertThat(result.getMediumAnnotated()).isZero();
    assertThat(result.getMediumUnannotated()).isZero();

    assertThat(result.getHigh()).isZero();
    assertThat(result.getHighAnnotated()).isZero();
    assertThat(result.getHighUnannotated()).isZero();

    assertThat(result.getCritical()).isZero();
    assertThat(result.getCriticalAnnotated()).isZero();
    assertThat(result.getCriticalUnannotated()).isZero();

    assertThat(result.getTotalVulnerabilities()).isOne();
    assertThat(result.getTotalVulnerabilitiesAnnotated()).isOne();
    assertThat(result.getTotalVulnerabilitiesUnannotated()).isZero();

    // Permission granted in all applications now
    grantReadPermission(newApplication.getId());

    result = service.getVulnerabilitiesByThreatLevel();
    assertThat(result).isNotNull();

    assertThat(result.getLow()).isOne();
    assertThat(result.getLowAnnotated()).isOne();
    assertThat(result.getLowUnannotated()).isZero();

    assertThat(result.getMedium()).isZero();
    assertThat(result.getMediumAnnotated()).isZero();
    assertThat(result.getMediumUnannotated()).isZero();

    assertThat(result.getHigh()).isOne();
    assertThat(result.getHighAnnotated()).isZero();
    assertThat(result.getHighUnannotated()).isOne();

    assertThat(result.getCritical()).isZero();
    assertThat(result.getCriticalAnnotated()).isZero();
    assertThat(result.getCriticalUnannotated()).isZero();

    assertThat(result.getTotalVulnerabilities()).isEqualTo(2);
    assertThat(result.getTotalVulnerabilitiesAnnotated()).isOne();
    assertThat(result.getTotalVulnerabilitiesUnannotated()).isOne();
  }

  private void assertEmptyResult(VulnerabilitiesThreadLevelMetricDTO result) {
    assertThat(result).isNotNull();

    assertThat(result.getLow()).isZero();
    assertThat(result.getLowAnnotated()).isZero();
    assertThat(result.getLowUnannotated()).isZero();

    assertThat(result.getMedium()).isZero();
    assertThat(result.getMediumAnnotated()).isZero();
    assertThat(result.getMediumUnannotated()).isZero();

    assertThat(result.getHigh()).isZero();
    assertThat(result.getHighAnnotated()).isZero();
    assertThat(result.getHighUnannotated()).isZero();

    assertThat(result.getCritical()).isZero();
    assertThat(result.getCriticalAnnotated()).isZero();
    assertThat(result.getCriticalUnannotated()).isZero();

    assertThat(result.getTotalVulnerabilities()).isZero();
    assertThat(result.getTotalVulnerabilitiesAnnotated()).isZero();
    assertThat(result.getTotalVulnerabilitiesUnannotated()).isZero();
  }
}
