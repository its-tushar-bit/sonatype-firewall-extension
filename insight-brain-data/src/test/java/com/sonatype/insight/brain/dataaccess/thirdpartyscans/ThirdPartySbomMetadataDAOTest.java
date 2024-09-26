/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static com.sonatype.insight.brain.utils.SbomMetadataBuilder.newSbomMetadataBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ThirdPartySbomMetadataDAOTest
    extends AbstractDbDAOTest
{
  private ThirdPartySbomMetadataDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createThirdPartySbomMetadataDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ThirdPartySbomMetadata entity = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();
    assertThat(entity.getId()).isNotNull();

    // Read
    ThirdPartySbomMetadata fetchedThirdPartySbomMetadata = dao.getById(entity.getId());
    assertThirdPartySbomMetadata(fetchedThirdPartySbomMetadata, entity);

    // Update
    entity.setSbomVersion("new version");
    entity.setSerialNumber("new serial number");
    dao.update(entity);

    fetchedThirdPartySbomMetadata = dao.getById(entity.getId());
    assertThirdPartySbomMetadata(fetchedThirdPartySbomMetadata, entity);

    // Delete
    dao.delete(entity);
    fetchedThirdPartySbomMetadata = dao.getById(entity.getId());
    assertThat(fetchedThirdPartySbomMetadata).isNull();
  }

  @Test
  public void testGetAll() {
    // Create one entry
    ThirdPartySbomMetadata entity = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();
    assertThat(entity.getId()).isNotNull();

    // Read all
    List<ThirdPartySbomMetadata> allSbomMetadata = dao.getAll();
    assertThat(allSbomMetadata.size()).isOne();

    // Create another entry
    ThirdPartySbomMetadata anotherEntity = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();
    allSbomMetadata = dao.getAll();
    assertThat(allSbomMetadata)
        .hasSize(2)
        .extracting(
            ThirdPartySbomMetadata::getId, ThirdPartySbomMetadata::getThirdPartyFileId,
            ThirdPartySbomMetadata::getApplicationId, ThirdPartySbomMetadata::getFilename,
            ThirdPartySbomMetadata::getSerialNumber, ThirdPartySbomMetadata::getSbomVersion,
            ThirdPartySbomMetadata::getSpec, ThirdPartySbomMetadata::getSpecFormat,
            ThirdPartySbomMetadata::getSpecVersion, ThirdPartySbomMetadata::getStatus,
            ThirdPartySbomMetadata::getCreatedAt, ThirdPartySbomMetadata::getMetadataJson,
            ThirdPartySbomMetadata::getScanType)
        .contains(
            tuple(entity.getId(), entity.getThirdPartyFileId(), entity.getApplicationId(), entity.getFilename(),
                entity.getSerialNumber(), entity.getSbomVersion(), entity.getSpec(),
                entity.getSpecFormat(), entity.getSpecVersion(), entity.getStatus(), entity.getCreatedAt(),
                entity.getMetadataJson(), entity.getScanType()),
            tuple(anotherEntity.getId(), anotherEntity.getThirdPartyFileId(), anotherEntity.getApplicationId(),
                anotherEntity.getFilename(), anotherEntity.getSerialNumber(), anotherEntity.getSbomVersion(),
                anotherEntity.getSpec(), anotherEntity.getSpecFormat(), anotherEntity.getSpecVersion(),
                anotherEntity.getStatus(), anotherEntity.getCreatedAt(), anotherEntity.getMetadataJson(),
                entity.getScanType()));
  }

  @Test
  public void testGetByThirdPartyFileIds() {
    ThirdPartySbomMetadata entity = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();
    assertThat(entity.getId()).isNotNull();

    final List<ThirdPartySbomMetadata> sbomMetadataList =
        dao.getByThirdPartyFileIds(Collections.singletonList(entity.getThirdPartyFileId()));
    assertThat(sbomMetadataList).isNotEmpty();
    for (ThirdPartySbomMetadata sbomMetadata : sbomMetadataList) {
      assertThirdPartySbomMetadata(entity, sbomMetadata);
    }
  }

  @Test
  public void testGetByThirdPartyFileId() {
    ThirdPartySbomMetadata entity = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();
    assertThat(entity.getId()).isNotNull();

    final ThirdPartySbomMetadata sbomMetadata = dao.getByThirdPartyFileId(entity.getThirdPartyFileId());
    assertThat(sbomMetadata).isNotNull();
    assertThirdPartySbomMetadata(entity, sbomMetadata);
  }

  @Test
  public void testDeleteByThirdPartyFileId() {
    ThirdPartySbomMetadata entity = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();
    assertThat(entity.getId()).isNotNull();

    ThirdPartySbomMetadata sbomMetadata = dao.getByThirdPartyFileId(entity.getThirdPartyFileId());
    assertThat(sbomMetadata).isNotNull();
    assertThirdPartySbomMetadata(entity, sbomMetadata);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByThirdPartyFileId(tx, entity.getThirdPartyFileId());
      tx.commit();

      sbomMetadata = dao.getByThirdPartyFileId(entity.getThirdPartyFileId());
      assertThat(sbomMetadata).isNull();
    }
  }

  @Test
  public void testGetByApplicationId() {
    ThirdPartySbomMetadata entity = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();
    assertThat(entity.getId()).isNotNull();

    final List<ThirdPartySbomMetadata> sbomMetadata = dao.getByApplicationId(entity.getApplicationId());
    assertThat(sbomMetadata).isNotNull()
        .hasSize(1);
    assertThirdPartySbomMetadata(sbomMetadata.get(0), entity);
  }

  @Test
  public void testGetActiveByApplicationId() {
    ThirdPartySbomMetadata activeSbom = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withStatus("PENDING")
        .build();

    List<ThirdPartySbomMetadata> sbomMetadata = dao.getActiveByApplicationId(application.getId());

    assertThat(sbomMetadata).hasSize(1);
    assertThirdPartySbomMetadata(sbomMetadata.get(0), activeSbom);
  }

  // TODO - Fix and Re-enable https://sonatype.atlassian.net/browse/CLM-31571
  @Ignore
  @Test
  public void testGetLatestActiveByApplicationId() {
    ThirdPartySbomMetadata entity1 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).withApplicationId("appId")
        .build();
    ThirdPartySbomMetadata entity2 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).withApplicationId("appId")
        .build();
    assertThat(entity1.getId()).isNotNull();
    assertThat(entity2.getId()).isNotNull();

    final ThirdPartySbomMetadata sbomMetadata = dao.getLatestActiveByApplicationId("appId");
    assertThat(sbomMetadata).isNotNull();
    assertThat(sbomMetadata.getCreatedAt()).isAfter(entity1.getCreatedAt());
    assertThirdPartySbomMetadata(sbomMetadata, entity2);
  }

  @Test
  public void testGetByApplicationIdAndSbomVersion() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSbomVersion("version1")
        .build();

    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withSbomVersion("version2")
        .build();

    Application anotherApp = tempEntity.newApplicationWithParent();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(anotherApp.getId())
        .withSbomVersion("version1")
        .build();

    final ThirdPartySbomMetadata savedSbomMetadata =
        dao.getByApplicationIdAndSbomVersion(sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion());
    assertThat(savedSbomMetadata).isNotNull();
    assertThirdPartySbomMetadata(savedSbomMetadata, sbomMetadata);
  }

  @Test
  public void testDeleteByApplicationId() {
    ThirdPartySbomMetadata entity = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();
    assertThat(entity.getId()).isNotNull();

    List<ThirdPartySbomMetadata> sbomMetadata = dao.getByApplicationId(entity.getApplicationId());
    assertThat(sbomMetadata).isNotNull()
        .hasSize(1);
    assertThirdPartySbomMetadata(sbomMetadata.get(0), entity);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByApplicationId(tx, entity.getApplicationId());
      tx.commit();

      sbomMetadata = dao.getByApplicationId(entity.getThirdPartyFileId());
      assertThat(sbomMetadata).isEmpty();
    }
  }

  @Test
  public void testGetActiveSbomCount() {
    IntStream.rangeClosed(1, 3).forEach(i -> createSbomMetadata(true, "ACTIVE"));
    long sbomCount = dao.getActiveSbomCount();
    assertThat(sbomCount).isEqualTo(3);
  }

  @Test
  public void testGetActiveSbomCount_Different_Statuses() {
    IntStream.rangeClosed(1, 3).forEach(i -> createSbomMetadata(true, "ACTIVE"));
    IntStream.rangeClosed(1, 4).forEach(i -> createSbomMetadata(true, "PENDING"));
    long sbomCount = dao.getActiveSbomCount();
    assertThat(sbomCount).isEqualTo(3);
  }

  @Test
  public void testGetActiveSbomCount_Empty_Table() {
    long sbomCount = dao.getActiveSbomCount();
    assertThat(sbomCount).isZero();
  }

  void assertThirdPartySbomMetadata(ThirdPartySbomMetadata actual, ThirdPartySbomMetadata expected) {
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getCreatedAt()).isEqualTo(expected.getCreatedAt());
    assertThat(actual.getApplicationId()).isEqualTo(expected.getApplicationId());
    assertThat(actual.getSbomVersion()).isEqualTo(expected.getSbomVersion());
    assertThat(actual.getThirdPartyFileId()).isEqualTo(expected.getThirdPartyFileId());
    assertThat(actual.getFilename()).isEqualTo(expected.getFilename());
    assertThat(actual.getSerialNumber()).isEqualTo(expected.getSerialNumber());
    assertThat(actual.getSpec()).isEqualTo(expected.getSpec());
    assertThat(actual.getSpecFormat()).isEqualTo(expected.getSpecFormat());
    assertThat(actual.getSpecVersion()).isEqualTo(expected.getSpecVersion());
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
    assertThat(actual.getMetadataJson()).isEqualTo(expected.getMetadataJson());
    assertThat(actual.getScanType()).isEqualTo(expected.getScanType());
  }

  ThirdPartySbomMetadata createSbomMetadata(boolean save, String status) {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata metadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(status, application.getId(), thirdPartyFile.getId());

    if (save) {
      dao.insert(metadata);
    }
    return metadata;
  }

  @Test
  public void testGetSbomsHistoryMetrics_DistinctApplications() {
    String activeState = "ACTIVE";
    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);

    newSbomMetadataBuilder(daoFactory).withCreatedAt(now).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneYearAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(sixMonthsAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoMonthsAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneMonthAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneWeekAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus("PENDING").build();

    ApiSbomApplicationsHistoryMetricDTO result = dao.getSbomsHistoryMetrics();
    assertThat(result).isNotNull();
    assertThat(result.totalScannedApplications).isEqualTo(7);
    assertThat(result.applicationsUpdatedLastMonth).isEqualTo(4);
    assertThat(result.applicationsUpdatedLastWeek).isEqualTo(3);
    assertThat(result.applicationsUpdatedLastYear).isEqualTo(7);
  }

  @Test
  public void testGetSbomsHistoryMetrics_ValidateSameApplicationsId() {
    Application application = tempEntity.newApplicationWithParent();
    String activeState = "ACTIVE";
    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);

    //count just one time
    newSbomMetadataBuilder(daoFactory).withCreatedAt(now).withStatus(activeState)
        .withApplicationId(application.getId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneYearAgo).withStatus(activeState)
        .withApplicationId(application.getId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneWeekAgo).withStatus(activeState)
        .withApplicationId(application.getId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneMonthAgo).withStatus(activeState)
        .withApplicationId(application.getId()).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(sixMonthsAgo).withStatus(activeState)
        .withApplicationId(application.getId()).build();

    newSbomMetadataBuilder(daoFactory).withCreatedAt(sixMonthsAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoMonthsAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneWeekAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday)
        .withStatus("PENDING").build();

    ApiSbomApplicationsHistoryMetricDTO result = dao.getSbomsHistoryMetrics();
    assertThat(result).isNotNull();
    assertThat(result.totalScannedApplications).isEqualTo(5);
    assertThat(result.applicationsUpdatedLastMonth).isEqualTo(3);
    assertThat(result.applicationsUpdatedLastWeek).isEqualTo(3);
    assertThat(result.applicationsUpdatedLastYear).isEqualTo(5);
  }

  @Test
  public void testGetSbomsHistoryMetrics_UpdatedVEX() {
    String activeState = "ACTIVE";
    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date twoYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);

    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneYearAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(sixMonthsAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoMonthsAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneMonthAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneWeekAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus("PENDING").build();

    ThirdPartySbomMetadata sbomMetadata = newSbomMetadataBuilder(daoFactory).withCreatedAt(twoYearAgo)
        .withStatus(activeState).build();
    ThirdPartyFileCoordinate coordinate = tempEntity.newThirdPartyFileCoordinate(
        sbomMetadata.getThirdPartyFileId(), "s4", "f4", "n4", "v4", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity(coordinate,
        "r14", "d14", "l14", CvssV3Severity.LOW.getStartScoreRange(),
        CvssV3Severity.LOW.getDisplayName(), "f14");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity, coordinateSecurity.getRefId(),
        "state", "justification", "response", "detail", now, now);

    ThirdPartySbomMetadata sbomMetadata2 = newSbomMetadataBuilder(daoFactory).withCreatedAt(twoYearAgo)
        .withStatus(activeState).build();
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(
        sbomMetadata2.getThirdPartyFileId(), "s4", "f4", "n4", "v4", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity2 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2,
        "r14", "d14", "l14", CvssV3Severity.LOW.getStartScoreRange(),
        CvssV3Severity.LOW.getDisplayName(), "f14");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity2, coordinateSecurity2.getRefId(),
        "state", "justification", "response", "detail", null, oneWeekAgo);

    ApiSbomApplicationsHistoryMetricDTO result = dao.getSbomsHistoryMetrics();
    assertThat(result).isNotNull();
    assertThat(result.totalScannedApplications).isEqualTo(8);
    assertThat(result.applicationsUpdatedLastMonth).isEqualTo(5);
    assertThat(result.applicationsUpdatedLastWeek).isEqualTo(4);
    assertThat(result.applicationsUpdatedLastYear).isEqualTo(8);
  }

  @Test
  public void testGetSbomsHistoryMetrics_EmptyResults() {
    ApiSbomApplicationsHistoryMetricDTO result = dao.getSbomsHistoryMetrics();
    assertThat(result).isNotNull();
    assertThat(result.totalScannedApplications).isZero();
    assertThat(result.applicationsUpdatedLastMonth).isZero();
    assertThat(result.applicationsUpdatedLastWeek).isZero();
    assertThat(result.applicationsUpdatedLastYear).isZero();
  }

  @Test
  public void testGetPendingSbomsOlderThanDuration_pendingSboms() {
    Date now = new Date();
    Date twoDaysAgo = DateUtils.addDays(now, -2);
    Date twentyFourHoursAgo = DateUtils.addDays(now, -1);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date threeHoursAgo = DateUtils.addHours(now, -3);

    ThirdPartySbomMetadata twoDaysAgoPendingMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus("PENDING")
        .withCreatedAt(twoDaysAgo).build();
    assertThat(twoDaysAgoPendingMetadata.getId()).isNotNull();

    ThirdPartySbomMetadata twentyFourHoursAgoPendingMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus("PENDING")
        .withCreatedAt(twentyFourHoursAgo).build();
    assertThat(twentyFourHoursAgoPendingMetadata.getId()).isNotNull();

    ThirdPartySbomMetadata twoMonthsAgoActiveMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus("ACTIVE")
        .withCreatedAt(twoMonthsAgo).build();
    assertThat(twoMonthsAgoActiveMetadata.getId()).isNotNull();

    ThirdPartySbomMetadata threeHoursAgoPendingMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus("PENDING")
        .withCreatedAt(threeHoursAgo).build();
    assertThat(threeHoursAgoPendingMetadata.getId()).isNotNull();

    assertThat(dao.getAll()).isNotNull()
        .hasSize(4);

    List<ThirdPartySbomMetadata> sbomMetadataList = dao.getPendingSbomsOlderThanDuration(Duration.ofHours(24));

    assertThat(sbomMetadataList).hasSize(2);
    assertThat(sbomMetadataList).extracting(ThirdPartySbomMetadata::getId)
        .containsExactlyInAnyOrder(twoDaysAgoPendingMetadata.getId(), twentyFourHoursAgoPendingMetadata.getId());
  }

  @Test
  public void testGetPendingSbomsOlderThanDuration_noPendingSboms() {
    Date now = new Date();
    Date twoDaysAgo = DateUtils.addDays(now, -2);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date threeHoursAgo = DateUtils.addHours(now, -3);
    ThirdPartySbomMetadata twoDaysAgoActiveMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus("ACTIVE")
        .withCreatedAt(twoDaysAgo).build();
    assertThat(twoDaysAgoActiveMetadata.getId()).isNotNull();

    ThirdPartySbomMetadata twoMonthsAgoActiveMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus("ACTIVE")
        .withCreatedAt(twoMonthsAgo).build();
    assertThat(twoMonthsAgoActiveMetadata.getId()).isNotNull();

    ThirdPartySbomMetadata threeHoursAgoActiveMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus("ACTIVE")
        .withCreatedAt(threeHoursAgo).build();
    assertThat(threeHoursAgoActiveMetadata.getId()).isNotNull();

    assertThat(dao.getAll())
        .hasSize(3);

    List<ThirdPartySbomMetadata> sbomMetadataList = dao.getPendingSbomsOlderThanDuration(Duration.ofHours(24));

    assertThat(sbomMetadataList).hasSize(0);
  }

  @Test
  public void testHasSbomMetadata_isFalse() {
    final boolean hasSbomMetadata = dao.hasSbomMetadata("scanId");
    assertThat(hasSbomMetadata).isFalse();
  }

  @Test
  public void testHasSbomMetadata_isTrue() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan("scanRequestId", "scanId", thirdPartyFile);
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), "ACTIVE", "xyz");

    final boolean hasSbomMetadata = dao.hasSbomMetadata("scanId");
    assertThat(hasSbomMetadata).isTrue();
  }

  @Test
  public void testGetByApplicationId_withPagination() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata1 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withCreatedAt(new Date(0))
        .withSbomVersion("version1")
        .build();

    ThirdPartySbomMetadata sbomMetadata2 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withCreatedAt(new Date(1))
        .withSbomVersion("version2")
        .build();

    ThirdPartySbomMetadata sbomMetadata3 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withCreatedAt(new Date(2))
        .withSbomVersion("version3")
        .build();

    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app.getId())
        .withCreatedAt(new Date(3))
        .withSbomVersion("version4")
        .withStatus("PENDING")
        .build();

    Application anotherApp = tempEntity.newApplicationWithParent();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(anotherApp.getId())
        .withCreatedAt(new Date(0))
        .withSbomVersion("version1")
        .build();

    String applicationId = app.getId();
    int page = 1;
    int pageSize = 2;

    List<ThirdPartySbomMetadata> results = dao.getByApplicationIdAndStatus(applicationId, "ACTIVE", page, pageSize);

    assertThat(results).hasSize(2);
    assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(sbomMetadata3);
    assertThat(results.get(1)).usingRecursiveComparison().isEqualTo(sbomMetadata2);

    page = 2;
    results = dao.getByApplicationIdAndStatus(applicationId, "ACTIVE", page, pageSize);

    assertThat(results).hasSize(1);
    assertThat(results.get(0)).usingRecursiveComparison().isEqualTo(sbomMetadata1);

    page = 3;
    results = dao.getByApplicationIdAndStatus(applicationId, "ACTIVE", page, pageSize);

    assertThat(results).isEmpty();

    page = 1;
    pageSize = 0;
    results = dao.getByApplicationIdAndStatus(applicationId, "ACTIVE", page, pageSize);

    assertThat(results).isEmpty();
  }

  @Test
  public void testGetSbomCount() {
    Application app1 = tempEntity.newApplicationWithParent();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app1.getId())
        .withSbomVersion("version1")
        .build();

    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app1.getId())
        .withStatus("PENDING")
        .withSbomVersion("version4")
        .build();

    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app1.getId())
        .withSbomVersion("version2")
        .build();

    Application app2 = tempEntity.newApplicationWithParent();
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(app2.getId())
        .withSbomVersion("version3")
        .build();

    Application app3 = tempEntity.newApplicationWithParent();

    long countApp1 = dao.getActiveSbomCount(app1.getId());
    long countApp2 = dao.getActiveSbomCount(app2.getId());
    long countApp3 = dao.getActiveSbomCount(app3.getId());
    long countNonExistentApp = dao.getActiveSbomCount("not_exist_id");

    assertThat(countApp1).isEqualTo(2);
    assertThat(countApp2).isEqualTo(1);
    assertThat(countApp3).isEqualTo(0);
    assertThat(countNonExistentApp).isEqualTo(0);
  }

  @Test
  public void testGetByApplicationId_withPaginationEmptyResult() {
    Application app = tempEntity.newApplicationWithParent();

    String applicationId = app.getId();
    int page = 1;
    int pageSize = 2;

    List<ThirdPartySbomMetadata> results = dao.getByApplicationIdAndStatus(applicationId, "ACTIVE", page, pageSize);

    assertThat(results).isEmpty();
  }
}
