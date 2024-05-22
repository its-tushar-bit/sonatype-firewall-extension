/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.SbomsAnalyzedMetricsDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilitiesThreadLevelMetricDTO;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.utils.SbomMetadataBuilder.newSbomMetadataBuilder;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiSbomDashboardResourceTest
    extends AbstractResourceTest
{
  @Before
  public void setUp() throws Exception {
    setFeatures(LicensedFeature.SBOM_MANAGER);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.SBOM_DASHBOARD_RESOURCE_PATH);
  }

  @Test
  public void testGetSbomsAnalyzedMetrics() throws Exception {
    insertSbomData();
    HttpResponse response = restRequest().path(ApiSbomDashboardResource.SBOMS_ANALYZED_PATH).get();
    assertResponseStatus(200, response);

    SbomsAnalyzedMetricsDTO result = response.getBody(SbomsAnalyzedMetricsDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.getTotal()).isEqualTo(7);
    assertThat(result.getThreshold()).isEqualTo(testProductLicense.getMaxSboms().longValue());
  }

  @Test
  public void testGetSbomsHistoryMetrics() throws Exception {
    insertSbomData();
    HttpResponse response = restRequest().path(ApiSbomDashboardResource.SBOMS_HISTORY_METRICS_PATH).get();
    assertResponseStatus(200, response);

    ApiSbomApplicationsHistoryMetricDTO result = response.getBody(ApiSbomApplicationsHistoryMetricDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.totalScannedApplications).isEqualTo(7);
    assertThat(result.applicationsUpdatedLastYear).isEqualTo(7);
    assertThat(result.applicationsUpdatedLastMonth).isEqualTo(4);
    assertThat(result.applicationsUpdatedLastWeek).isEqualTo(3);
  }

  @Test
  public void testGetVulnerabilitiesByThreatLevel() throws Exception {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(tempEntity.newApplicationWithParent().getId())
        .build();

    ThirdPartyFileCoordinate coordinate =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s", "f", "n", "v", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity(coordinate, "r", "d",
        "l", CvssV3Severity.LOW.getStartScoreRange(), CvssV3Severity.LOW.getDisplayName(), "f");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity, coordinateSecurity.getRefId(),
        "state", "justification", "response", "detail");

    HttpResponse response = restRequest()
        .path(ApiSbomDashboardResource.SBOMS_VULNERABILITES_BY_THREAT_LEVEL_PATH)
        .get();
    assertResponseStatus(200, response);

    VulnerabilitiesThreadLevelMetricDTO result = response.getBody(VulnerabilitiesThreadLevelMetricDTO.class);
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
  }

  private void insertSbomData() {
    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);

    newSbomMetadataBuilder(daoFactory).withCreatedAt(now).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneYearAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(sixMonthsAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoMonthsAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneMonthAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneWeekAgo).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus("PENDING").build();
  }
}
