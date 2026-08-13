/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.dashboard;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import jakarta.ws.rs.core.Response.Status;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v2.dto.SbomsAnalyzedMetricsDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentImportedSbomsDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentVulnerabilitiesDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ReleaseStatusDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilitiesThreadLevelMetricDTO;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static com.sonatype.insight.brain.sbom.dashboard.SbomDashboardResource.SBOMS_ANALYZED_PATH;
import static com.sonatype.insight.brain.sbom.dashboard.SbomDashboardResource.SBOMS_HIGH_PRIORITY_VULNERABILITIES;
import static com.sonatype.insight.brain.sbom.dashboard.SbomDashboardResource.SBOMS_HISTORY_METRICS_PATH;
import static com.sonatype.insight.brain.sbom.dashboard.SbomDashboardResource.SBOMS_RECENTLY_IMPORTED;
import static com.sonatype.insight.brain.sbom.dashboard.SbomDashboardResource.SBOMS_VULNERABILITES_BY_THREAT_LEVEL_PATH;
import static com.sonatype.insight.brain.sbom.dashboard.SbomDashboardResource.SBOM_RELEASE_STATUS;
import static com.sonatype.insight.brain.utils.SbomMetadataBuilder.newSbomMetadataBuilder;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * IQ Server on PostgreSQL variant of {@code SbomDashboardResourceTest}. Placed in the original
 * resource's package to access its package-private path constants.
 */
@IqPostgresTest
class IqPostgresSbomDashboardResourceTest
{
  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private Application app;

  private Organization org;

  @BeforeEach
  void before() throws Exception {
    org = ctx.tempEntity().newOrganization();
    app = ctx.tempEntity().newApplicationWithParent(org);
    ctx.setFeatures(LicensedFeature.SBOM_MANAGER);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(SbomDashboardResource.RESOURCE_BASE_PATH);
  }

  @Test
  void testGetRecentHighPriorityVulnerabilities() throws Exception {
    ThirdPartyScan thirdPartyScan1 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan2 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan3 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan4 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan5 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan6 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan7 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan8 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan9 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan10 = ctx.tempEntity().newThirdPartyScan();

    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);
    Date twoDaysAgo = DateUtils.addDays(now, -2);
    Date fourHoursAgo = DateUtils.addHours(now, -4);

    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(now)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan1.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(oneYearAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan2.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(sixMonthsAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan3.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(twoMonthsAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan4.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(oneMonthAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan5.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(oneWeekAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan6.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(yesterday)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan7.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(yesterday)
        .withStatus(PENDING)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan8.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(twoDaysAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan9.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(fourHoursAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan10.getThirdPartyFileId())
        .build();

    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan1.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h1", "u1",
                ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate2 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan1.getThirdPartyFileId(),
                "s", "SPDX", "n2", "v1", "h2", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate3 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan2.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h3", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate4 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h4", "u1",
                ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate5 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h5", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate6 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h6", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate7 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan4.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h7", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate8 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan4.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h8", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate9 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan5.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h9", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate10 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan6.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h10", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate11 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan7.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h11", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate12 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan8.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h12", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan9.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h13", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h14", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h15", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h16", "u1",
            ThirdPartyDependencyType.TRANSITIVE);

    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r1", "d1", "l1", 5.5, "sd1", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r2", "d2", "l2", 7.5, "sd2", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r3", "d3", "l3", 3.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r4", "d1", "l1", 5.5, "sd1", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r5", "d2", "l2", 7.9, "sd2", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r6", "d3", "l3", 3.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
            "r7", "d3", "l3", 1.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
            "r8", "d3", "l3", 9.5, "sd4", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
            "r9", "d3", "l3", 2.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
            "r10", "d3", "l3", 5.1, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
            "r11", "d3", "l3", 8.1, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
            "r12", "d3", "l3", 10.0, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate5,
            "r13", "d3", "l3", 2.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate6,
            "r14", "d3", "l3", 1.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate7,
            "r15", "d3", "l3", 0.1, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate7,
            "r16", "d3", "l3", 0, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate8,
            "r17", "d3", "l3", 3.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate9,
            "r18", "d3", "l3", 4.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate10,
            "r19", "d3", "l3", 5.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate11,
            "r20", "d3", "l3", 6.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate11,
            "r21", "d3", "l3", 7.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate12,
            "r22", "d3", "l3", 8.2, "sd3", "f3");

    HttpResponse response = restRequest()
        .path(SBOMS_HIGH_PRIORITY_VULNERABILITIES)
        .parameter()
        .get();

    ctx.assertResponseStatus(Status.OK.getStatusCode(), response);
    List<RecentVulnerabilitiesDTO> resultDtoList = Arrays.asList(response.getBody(RecentVulnerabilitiesDTO[].class));

    assertThat(resultDtoList.size()).isEqualTo(6);

    RecentVulnerabilitiesDTO recentVulnerabilitiesDTO = resultDtoList.get(0);
    assertThat(recentVulnerabilitiesDTO.getRefId()).isEqualTo("r5");
    assertThat(recentVulnerabilitiesDTO.getSeverity()).isEqualTo(7.9);
    assertThat(recentVulnerabilitiesDTO.getSeverityStatus()).isEqualTo("high");
    assertThat(recentVulnerabilitiesDTO.getCreatedAt()).isEqualTo(now);

    RecentVulnerabilitiesDTO recentVulnerabilitiesDTO1 = resultDtoList.get(1);
    assertThat(recentVulnerabilitiesDTO1.getRefId()).isEqualTo("r2");
    assertThat(recentVulnerabilitiesDTO1.getSeverity()).isEqualTo(7.5);
    assertThat(recentVulnerabilitiesDTO1.getSeverityStatus()).isEqualTo("high");
    assertThat(recentVulnerabilitiesDTO1.getCreatedAt()).isEqualTo(now);

    RecentVulnerabilitiesDTO recentVulnerabilitiesDTO2 = resultDtoList.get(2);
    assertThat(recentVulnerabilitiesDTO2.getRefId()).isEqualTo("r21");
    assertThat(recentVulnerabilitiesDTO2.getSeverity()).isEqualTo(7.2);
    assertThat(recentVulnerabilitiesDTO2.getSeverityStatus()).isEqualTo("high");
    assertThat(recentVulnerabilitiesDTO2.getCreatedAt()).isEqualTo(yesterday);

    RecentVulnerabilitiesDTO recentVulnerabilitiesDTO3 = resultDtoList.get(3);
    assertThat(recentVulnerabilitiesDTO3.getRefId()).isEqualTo("r12");
    assertThat(recentVulnerabilitiesDTO3.getSeverity()).isEqualTo(10.0);
    assertThat(recentVulnerabilitiesDTO3.getSeverityStatus()).isEqualTo("critical");
    assertThat(recentVulnerabilitiesDTO3.getCreatedAt()).isEqualTo(sixMonthsAgo);

    RecentVulnerabilitiesDTO recentVulnerabilitiesDTO4 = resultDtoList.get(4);
    assertThat(recentVulnerabilitiesDTO4.getRefId()).isEqualTo("r11");
    assertThat(recentVulnerabilitiesDTO4.getSeverity()).isEqualTo(8.1);
    assertThat(recentVulnerabilitiesDTO4.getSeverityStatus()).isEqualTo("high");
    assertThat(recentVulnerabilitiesDTO4.getCreatedAt()).isEqualTo(sixMonthsAgo);

    RecentVulnerabilitiesDTO recentVulnerabilitiesDTO5 = resultDtoList.get(5);
    assertThat(recentVulnerabilitiesDTO5.getRefId()).isEqualTo("r8");
    assertThat(recentVulnerabilitiesDTO5.getSeverity()).isEqualTo(9.5);
    assertThat(recentVulnerabilitiesDTO5.getSeverityStatus()).isEqualTo("critical");
    assertThat(recentVulnerabilitiesDTO5.getCreatedAt()).isEqualTo(oneYearAgo);
  }

  @Test
  void testGetSbomReleaseStatus() throws Exception {
    ThirdPartyScan thirdPartyScan1 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan2 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan3 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan4 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan5 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan6 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan7 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan8 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan9 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan10 = ctx.tempEntity().newThirdPartyScan();

    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);
    Date twoDaysAgo = DateUtils.addDays(now, -2);
    Date fourHoursAgo = DateUtils.addHours(now, -4);

    ThirdPartySbomMetadata thirdPartySbomMetadata1 = newSbomMetadataBuilder(ctx.daoFactory())
        .withCreatedAt(now)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan1.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata2 = newSbomMetadataBuilder(ctx.daoFactory())
        .withCreatedAt(oneYearAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan2.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata3 = newSbomMetadataBuilder(ctx.daoFactory())
        .withCreatedAt(sixMonthsAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan3.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata4 = newSbomMetadataBuilder(ctx.daoFactory())
        .withCreatedAt(twoMonthsAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan4.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata5 = newSbomMetadataBuilder(ctx.daoFactory())
        .withCreatedAt(oneMonthAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan5.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata6 = newSbomMetadataBuilder(ctx.daoFactory())
        .withCreatedAt(oneWeekAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan6.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata7 = newSbomMetadataBuilder(ctx.daoFactory())
        .withCreatedAt(yesterday)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan7.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata8 = newSbomMetadataBuilder(ctx.daoFactory())
        .withCreatedAt(yesterday)
        .withStatus(PENDING)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan8.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata9 = newSbomMetadataBuilder(ctx.daoFactory())
        .withCreatedAt(twoDaysAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan9.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata10 = newSbomMetadataBuilder(ctx.daoFactory())
        .withCreatedAt(fourHoursAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan10.getThirdPartyFileId())
        .build();

    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan1.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h1", "u1", ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate2 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan1.getThirdPartyFileId(),
                "s", "SPDX", "n2", "v1", "h2", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate3 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan2.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h3", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate4 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h4", "u1", ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate5 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h5", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate6 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h6", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate7 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan4.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h7", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate8 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan4.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h8", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate9 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan5.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h9", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate10 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan6.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h10", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate11 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan7.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h11", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate12 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan8.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h12", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan9.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h13", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h14", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h15", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h16", "u1",
            ThirdPartyDependencyType.TRANSITIVE);

    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r1", thirdPartySbomMetadata1.getId(), "d1", "l1", 5.5, "sd1", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r2", thirdPartySbomMetadata1.getId(), "d2", "l2", 7.5, "sd2", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r3", thirdPartySbomMetadata2.getId(), "d3", "l3", 3.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r4", thirdPartySbomMetadata2.getId(), "d1", "l1", 5.5, "sd1", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r5", thirdPartySbomMetadata3.getId(), "d2", "l2", 7.9, "sd2", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r6", thirdPartySbomMetadata3.getId(), "d3", "l3", 3.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
            "r7", thirdPartySbomMetadata4.getId(), "d3", "l3", 1.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
            "r8", thirdPartySbomMetadata5.getId(), "d3", "l3", 9.5, "sd4", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
            "r9", thirdPartySbomMetadata5.getId(), "d3", "l3", 2.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
            "r10", thirdPartySbomMetadata6.getId(), "d3", "l3", 5.1, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
            "r11", thirdPartySbomMetadata6.getId(), "d3", "l3", 8.1, "sd3", "f3");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        ctx.tempEntity()
            .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
                "r12", thirdPartySbomMetadata7.getId(), "d3", "l3", 10.0, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate5,
            "r13", thirdPartySbomMetadata7.getId(), "d3", "l3", 9.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate6,
            "r14", thirdPartySbomMetadata8.getId(), "d3", "l3", 1.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate7,
            "r15", thirdPartySbomMetadata8.getId(), "d3", "l3", 0.1, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate7,
            "r16", thirdPartySbomMetadata8.getId(), "d3", "l3", 0, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate8,
            "r17", thirdPartySbomMetadata9.getId(), "d3", "l3", 3.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate9,
            "r18", thirdPartySbomMetadata9.getId(), "d3", "l3", 4.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate10,
            "r19", thirdPartySbomMetadata9.getId(), "d3", "l3", 5.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate11,
            "r20", thirdPartySbomMetadata10.getId(), "d3", "l3", 6.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate11,
            "r21", thirdPartySbomMetadata10.getId(), "d3", "l3", 7.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate12,
            "r22", thirdPartySbomMetadata10.getId(), "d3", "l3", 8.2, "sd3", "f3");

    ctx.tempEntity()
        .newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity, "r1", "ACTIVE",
            "J1", "r1", "d1");

    HttpResponse response = restRequest()
        .path(SBOM_RELEASE_STATUS)
        .parameter()
        .get();

    ctx.assertResponseStatus(Status.OK.getStatusCode(), response);

    ReleaseStatusDTO releaseStatusDTO = response.getBody(ReleaseStatusDTO.class);

    assertThat(releaseStatusDTO.getNeedsAttentionCount()).isEqualTo(6);
    assertThat(releaseStatusDTO.getPartiallyReadyCount()).isEqualTo(1);
    assertThat(releaseStatusDTO.getReleaseReadyCount()).isEqualTo(3);
  }

  @Test
  void testGetRecentImportSboms() throws Exception {
    insertSbomDataAndComponentVulnerabilities();
    HttpResponse response = restRequest()
        .path(SBOMS_RECENTLY_IMPORTED)
        .get();

    ctx.assertResponseStatus(Status.OK.getStatusCode(), response);
    List<RecentImportedSbomsDTO> resultDtoList = Arrays.asList(response.getBody(
        RecentImportedSbomsDTO[].class));

    assertThat(resultDtoList).hasSize(7);

    Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
    Instant twoDaysAgoT = today.minus(1, ChronoUnit.DAYS);

    assertThat(resultDtoList.get(0).getImportDate().toInstant().truncatedTo(ChronoUnit.DAYS)).isEqualTo(today);
    assertThat(resultDtoList.get(2).getImportDate().toInstant().truncatedTo(ChronoUnit.DAYS)).isEqualTo(twoDaysAgoT);
  }

  @Test
  void testGetRecentImportSboms_TestSbomsWithoutComponents() throws Exception {
    insertSbomDataAndComponentVulnerabilities();
    Application tempApp = ctx.tempEntity().newApplicationWithParent();
    ThirdPartyScan thirdPartyScan1 = ctx.tempEntity().newThirdPartyScan();

    ThirdPartySbomMetadata sbomMetadata = newSbomMetadataBuilder(ctx.daoFactory())
        .withCreatedAt(new Date())
        .withApplicationId(tempApp.getId())
        .withThirdPartyFileId(thirdPartyScan1.getThirdPartyFileId())
        .build();

    HttpResponse response = restRequest()
        .path(SBOMS_RECENTLY_IMPORTED)
        .get();

    ctx.assertResponseStatus(Status.OK.getStatusCode(), response);
    List<RecentImportedSbomsDTO> resultDtoList = Arrays.asList(response.getBody(
        RecentImportedSbomsDTO[].class));

    assertThat(resultDtoList).hasSize(7);
    assertThat(resultDtoList.stream()
        .noneMatch(
            sbom -> sbom.getSbomVersion().equals(sbomMetadata.getSbomVersion()))).isTrue();
  }

  @Test
  void testGetSbomsAnalyzedMetrics() throws Exception {
    insertNewSbomDataWithoutComponents();
    HttpResponse response = restRequest().path(SBOMS_ANALYZED_PATH).get();
    ctx.assertResponseStatus(200, response);

    SbomsAnalyzedMetricsDTO result = response.getBody(SbomsAnalyzedMetricsDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.getTotal()).isEqualTo(7);
    assertThat(result.getThreshold())
        .isEqualTo(ctx.lookup(TestProductLicense.class).getMaxSboms().longValue());
  }

  @Test
  void testGetSbomsHistoryMetrics() throws Exception {
    insertNewSbomDataWithoutComponents();
    HttpResponse response = restRequest().path(SBOMS_HISTORY_METRICS_PATH).get();
    ctx.assertResponseStatus(200, response);

    ApiSbomApplicationsHistoryMetricDTO result = response.getBody(ApiSbomApplicationsHistoryMetricDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.totalScannedApplications).isEqualTo(7);
    assertThat(result.applicationsUpdatedLastYear).isEqualTo(7);
    assertThat(result.applicationsUpdatedLastMonth).isEqualTo(4);
    assertThat(result.applicationsUpdatedLastWeek).isEqualTo(3);
  }

  @Test
  void testGetVulnerabilitiesByThreatLevel() throws Exception {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(ctx.daoFactory())
        .withApplicationId(ctx.tempEntity().newApplicationWithParent().getId())
        .build();

    ThirdPartyFileCoordinate coordinate =
        ctx.tempEntity().newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s", "f", "n", "v", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity = ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(coordinate, "r",
            sbomMetadata.getId(), "d", "l", CvssV3Severity.LOW.getStartScoreRange(),
            CvssV3Severity.LOW.getDisplayName(), "f");
    ctx.tempEntity()
        .newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity, coordinateSecurity.getRefId(),
            "state", "justification", "response", "detail");

    HttpResponse response = restRequest()
        .path(SBOMS_VULNERABILITES_BY_THREAT_LEVEL_PATH)
        .get();
    ctx.assertResponseStatus(200, response);

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

  private void insertNewSbomDataWithoutComponents() {
    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);

    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(now).build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(oneYearAgo).build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(sixMonthsAgo).build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(twoMonthsAgo).build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(oneMonthAgo).build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(oneWeekAgo).build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(yesterday).build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(yesterday).withStatus(PENDING).build();
  }

  private void insertSbomDataAndComponentVulnerabilities() {
    ThirdPartyScan thirdPartyScan1 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan2 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan3 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan4 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan5 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan6 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan7 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan8 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan9 = ctx.tempEntity().newThirdPartyScan();
    ThirdPartyScan thirdPartyScan10 = ctx.tempEntity().newThirdPartyScan();

    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);
    Date twoDaysAgo = DateUtils.addDays(now, -2);
    Date fourHoursAgo = DateUtils.addHours(now, -4);

    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(now)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan1.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(oneYearAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan2.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(sixMonthsAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan3.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(twoMonthsAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan4.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(oneMonthAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan5.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(oneWeekAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan6.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(yesterday)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan7.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(yesterday)
        .withStatus(PENDING)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan8.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(twoDaysAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan9.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(ctx.daoFactory()).withCreatedAt(fourHoursAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan10.getThirdPartyFileId())
        .build();

    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan1.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h1", "u1",
                ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate2 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan1.getThirdPartyFileId(),
                "s", "SPDX", "n2", "v1", "h2", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate3 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan2.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h3", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate4 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h4", "u1",
                ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate5 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h5", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate6 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h6", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate7 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan4.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h7", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate8 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan4.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h8", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate9 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan5.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h9", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate10 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan6.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h10", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate11 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan7.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h11", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate12 =
        ctx.tempEntity()
            .newThirdPartyFileCoordinate(thirdPartyScan8.getThirdPartyFileId(),
                "s", "SPDX", "n1", "v1", "h12", "u1",
                ThirdPartyDependencyType.TRANSITIVE);
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan9.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h13", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h14", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h15", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ctx.tempEntity()
        .newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h16", "u1",
            ThirdPartyDependencyType.TRANSITIVE);

    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r1", "d1", "l1", 5.5, "sd1", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r2", "d2", "l2", 7.5, "sd2", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
            "r3", "d3", "l3", 3.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r4", "d1", "l1", 5.5, "sd1", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r5", "d2", "l2", 7.9, "sd2", "f1");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
            "r6", "d3", "l3", 3.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
            "r7", "d3", "l3", 1.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
            "r8", "d3", "l3", 9.5, "sd4", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
            "r9", "d3", "l3", 2.5, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
            "r10", "d3", "l3", 5.1, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
            "r11", "d3", "l3", 8.1, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
            "r12", "d3", "l3", 10.0, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate5,
            "r13", "d3", "l3", 2.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate6,
            "r14", "d3", "l3", 1.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate7,
            "r15", "d3", "l3", 0.1, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate7,
            "r16", "d3", "l3", 0, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate8,
            "r17", "d3", "l3", 3.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate9,
            "r18", "d3", "l3", 4.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate10,
            "r19", "d3", "l3", 5.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate11,
            "r20", "d3", "l3", 6.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate11,
            "r21", "d3", "l3", 7.2, "sd3", "f3");
    ctx.tempEntity()
        .newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate12,
            "r22", "d3", "l3", 8.2, "sd3", "f3");
  }
}
