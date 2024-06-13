/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.dashboard;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyDependencyType;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentVulnerabilitiesDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ReleaseStatusDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Inject;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.utils.SbomMetadataBuilder.newSbomMetadataBuilder;
import static org.assertj.core.api.Assertions.assertThat;

public class SbomDashboardServiceTest extends AbstractComponentTest
{
  @Inject
  private SbomDashboardService service;

  private Application app;

  private Organization org;

  @Before
  public void before() {
    org = tempEntity.newOrganization();
  }

  @Test
  @PostgresTest
  public void testGetRecentHighPriorityVulnerabilities() {
    app = tempEntity.newApplicationWithParent(org);

    ThirdPartyScan thirdPartyScan1 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan2 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan3 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan4 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan5 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan6 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan7 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan8 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan9 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan10 = tempEntity.newThirdPartyScan();

    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);
    Date twoDaysAgo = DateUtils.addDays(now, -2);
    Date fourHoursAgo = DateUtils.addHours(now, -4);

    newSbomMetadataBuilder(daoFactory).withCreatedAt(now).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan1.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneYearAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan2.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(sixMonthsAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan3.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoMonthsAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan4.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneMonthAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan5.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneWeekAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan6.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan7.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus("PENDING").withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan8.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoDaysAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan9.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(fourHoursAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan10.getThirdPartyFileId()).build();

    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan1.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h1", "u1",
            ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate2 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan1.getThirdPartyFileId(),
            "s", "SPDX", "n2", "v1", "h2", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate3 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan2.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h3", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate4 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h4", "u1",
            ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate5 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h5", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate6 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h6", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate7 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan4.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h7", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate8 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan4.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h8", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate9 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan5.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h9", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate10 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan6.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h10", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate11 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan7.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h11", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate12 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan8.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h12", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan9.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h13", "u1",
        ThirdPartyDependencyType.TRANSITIVE);
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h14", "u1",
        ThirdPartyDependencyType.TRANSITIVE);
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h15", "u1",
        ThirdPartyDependencyType.TRANSITIVE);
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h16", "u1",
        ThirdPartyDependencyType.TRANSITIVE);

    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
        "r1", "d1", "l1", 5.5, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
        "r2", "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
        "r3", "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
        "r4", "d1", "l1", 5.5, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
        "r5", "d2", "l2", 7.9, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
        "r6", "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
        "r7", "d3", "l3", 1.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
        "r8", "d3", "l3", 9.5, "sd4", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
        "r9", "d3", "l3", 2.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
        "r10", "d3", "l3", 5.1, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
        "r11", "d3", "l3", 8.1, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
        "r12", "d3", "l3", 10.0, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate5,
        "r13", "d3", "l3", 2.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate6,
        "r14", "d3", "l3", 1.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate7,
        "r15", "d3", "l3", 0.1, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate7,
        "r16", "d3", "l3", 0, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate8,
        "r17", "d3", "l3", 3.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate9,
        "r18", "d3", "l3", 4.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate10,
        "r19", "d3", "l3", 5.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate11,
        "r20", "d3", "l3", 6.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate11,
        "r21", "d3", "l3", 7.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate12,
        "r22", "d3", "l3", 8.2, "sd3", "f3");

    RecentVulnerabilitiesDTO recentVulnerabilitiesDTO = service.getRecentHighPriorityVulnerabilities().get(0);
    assertThat(recentVulnerabilitiesDTO.getRefId()).isEqualTo("r5");
    assertThat(recentVulnerabilitiesDTO.getSeverity()).isEqualTo(7.9);
    assertThat(recentVulnerabilitiesDTO.getSeverityStatus()).isEqualTo("high");
    assertThat(recentVulnerabilitiesDTO.getCreatedAt().getTime()).isEqualTo(now.getTime());

    RecentVulnerabilitiesDTO recentVulnerabilitiesDTO1 = service.getRecentHighPriorityVulnerabilities().get(1);
    assertThat(recentVulnerabilitiesDTO1.getRefId()).isEqualTo("r2");
    assertThat(recentVulnerabilitiesDTO1.getSeverity()).isEqualTo(7.5);
    assertThat(recentVulnerabilitiesDTO1.getSeverityStatus()).isEqualTo("high");
    assertThat(recentVulnerabilitiesDTO1.getCreatedAt().getTime()).isEqualTo(now.getTime());

    RecentVulnerabilitiesDTO recentVulnerabilitiesDTO2 = service.getRecentHighPriorityVulnerabilities().get(2);
    assertThat(recentVulnerabilitiesDTO2.getRefId()).isEqualTo("r21");
    assertThat(recentVulnerabilitiesDTO2.getSeverity()).isEqualTo(7.2);
    assertThat(recentVulnerabilitiesDTO2.getSeverityStatus()).isEqualTo("high");
    assertThat(recentVulnerabilitiesDTO2.getCreatedAt().getTime()).isEqualTo(yesterday.getTime());

    RecentVulnerabilitiesDTO recentVulnerabilitiesDTO3 = service.getRecentHighPriorityVulnerabilities().get(3);
    assertThat(recentVulnerabilitiesDTO3.getRefId()).isEqualTo("r12");
    assertThat(recentVulnerabilitiesDTO3.getSeverity()).isEqualTo(10.0);
    assertThat(recentVulnerabilitiesDTO3.getSeverityStatus()).isEqualTo("critical");
    assertThat(recentVulnerabilitiesDTO3.getCreatedAt().getTime()).isEqualTo(sixMonthsAgo.getTime());

    RecentVulnerabilitiesDTO recentVulnerabilitiesDTO4 = service.getRecentHighPriorityVulnerabilities().get(4);
    assertThat(recentVulnerabilitiesDTO4.getRefId()).isEqualTo("r11");
    assertThat(recentVulnerabilitiesDTO4.getSeverity()).isEqualTo(8.1);
    assertThat(recentVulnerabilitiesDTO4.getSeverityStatus()).isEqualTo("high");
    assertThat(recentVulnerabilitiesDTO4.getCreatedAt().getTime()).isEqualTo(sixMonthsAgo.getTime());

    RecentVulnerabilitiesDTO recentVulnerabilitiesDTO5 = service.getRecentHighPriorityVulnerabilities().get(5);
    assertThat(recentVulnerabilitiesDTO5.getRefId()).isEqualTo("r8");
    assertThat(recentVulnerabilitiesDTO5.getSeverity()).isEqualTo(9.5);
    assertThat(recentVulnerabilitiesDTO5.getSeverityStatus()).isEqualTo("critical");
    assertThat(recentVulnerabilitiesDTO5.getCreatedAt().getTime()).isEqualTo(oneYearAgo.getTime());
  }

  @Test
  @PostgresTest
  public void testGetRecentHighPriorityVulnerabilities_Noapps() {
    List<RecentVulnerabilitiesDTO> listOfRecentVulnerabilitiesDTO = service.getRecentHighPriorityVulnerabilities();
    assertThat(listOfRecentVulnerabilitiesDTO).hasSize(0);
  }

  @Test
  @PostgresTest
  public void testGetSbomReleaseStatus() {
    app = tempEntity.newApplicationWithParent(org);

    ThirdPartyScan thirdPartyScan1 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan2 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan3 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan4 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan5 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan6 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan7 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan8 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan9 = tempEntity.newThirdPartyScan();
    ThirdPartyScan thirdPartyScan10 = tempEntity.newThirdPartyScan();

    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);
    Date twoDaysAgo = DateUtils.addDays(now, -2);
    Date fourHoursAgo = DateUtils.addHours(now, -4);

    newSbomMetadataBuilder(daoFactory).withCreatedAt(now).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan1.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneYearAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan2.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(sixMonthsAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan3.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoMonthsAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan4.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneMonthAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan5.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneWeekAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan6.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan7.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus("PENDING").withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan8.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoDaysAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan9.getThirdPartyFileId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(fourHoursAgo).withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan10.getThirdPartyFileId()).build();

    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan1.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h1", "u1",
            ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate2 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan1.getThirdPartyFileId(),
            "s", "SPDX", "n2", "v1", "h2", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate3 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan2.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h3", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate4 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h4", "u1",
            ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate5 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h5", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate6 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h6", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate7 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan4.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h7", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate8 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan4.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h8", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate9 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan5.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h9", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate10 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan6.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h10", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate11 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan7.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h11", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate12 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan8.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h12", "u1",
            ThirdPartyDependencyType.TRANSITIVE);
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan9.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h13", "u1",
        ThirdPartyDependencyType.TRANSITIVE);
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h14", "u1",
        ThirdPartyDependencyType.TRANSITIVE);
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h15", "u1",
        ThirdPartyDependencyType.TRANSITIVE);
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h16", "u1",
        ThirdPartyDependencyType.TRANSITIVE);

    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
        "r1", "d1", "l1", 5.5, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
        "r2", "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
        "r3", "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
        "r4", "d1", "l1", 5.5, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
        "r5", "d2", "l2", 7.9, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
        "r6", "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
        "r7", "d3", "l3", 1.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
        "r8", "d3", "l3", 9.5, "sd4", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
        "r9", "d3", "l3", 2.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
        "r10", "d3", "l3", 5.1, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
        "r11", "d3", "l3", 8.1, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
        "r12", "d3", "l3", 10.0, "sd3", "f3");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
            "r12", "d3", "l3", 10.0, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate5,
        "r13", "d3", "l3", 2.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate6,
        "r14", "d3", "l3", 1.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate7,
        "r15", "d3", "l3", 0.1, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate7,
        "r16", "d3", "l3", 0, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate8,
        "r17", "d3", "l3", 3.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate9,
        "r18", "d3", "l3", 4.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate10,
        "r19", "d3", "l3", 5.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate11,
        "r20", "d3", "l3", 6.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate11,
        "r21", "d3", "l3", 7.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate12,
        "r22", "d3", "l3", 8.2, "sd3", "f3");

    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity, "r1", "ACTIVE",
        "J1", "r1", "d1");

    ReleaseStatusDTO releaseStatusDTO = service.getSbomReleaseStatus();
    assertThat(releaseStatusDTO.getNeedsAttentionCount()).isEqualTo(4);
    assertThat(releaseStatusDTO.getPartiallyReadyCount()).isEqualTo(1);
    assertThat(releaseStatusDTO.getReleaseReadyCount()).isEqualTo(5);
  }
}
