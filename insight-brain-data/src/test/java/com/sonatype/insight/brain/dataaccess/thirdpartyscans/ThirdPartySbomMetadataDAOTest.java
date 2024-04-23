/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

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
            ThirdPartySbomMetadata::getCreatedAt, ThirdPartySbomMetadata::getMetadataJson)
        .contains(
            tuple(entity.getId(), entity.getThirdPartyFileId(), entity.getApplicationId(), entity.getFilename(),
                entity.getSerialNumber(), entity.getSbomVersion(), entity.getSpec(),
                entity.getSpecFormat(), entity.getSpecVersion(), entity.getStatus(), entity.getCreatedAt(),
                entity.getMetadataJson()),
            tuple(anotherEntity.getId(), anotherEntity.getThirdPartyFileId(), anotherEntity.getApplicationId(),
                anotherEntity.getFilename(), anotherEntity.getSerialNumber(), anotherEntity.getSbomVersion(),
                anotherEntity.getSpec(), anotherEntity.getSpecFormat(), anotherEntity.getSpecVersion(),
                anotherEntity.getStatus(), anotherEntity.getCreatedAt(), anotherEntity.getMetadataJson()));
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
  public void testGetSbomsHistoryMetrics() {
    String activeState = "ACTIVE";
    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);

    createSbomMetadata(true, activeState, now);
    createSbomMetadata(true, activeState, oneYearAgo);
    createSbomMetadata(true, activeState, sixMonthsAgo);
    createSbomMetadata(true, activeState, twoMonthsAgo);
    createSbomMetadata(true, activeState, oneMonthAgo);
    createSbomMetadata(true, activeState, oneWeekAgo);
    createSbomMetadata(true, activeState, yesterday);
    createSbomMetadata(true, "PENDING", yesterday);

    ApiSbomApplicationsHistoryMetricDTO result = dao.getSbomsHistoryMetrics();
    assertThat(result).isNotNull();
    assertThat(result.totalScannedApplications).isEqualTo(7);
    assertThat(result.applicationsUpdatedLastMonth).isEqualTo(4);
    assertThat(result.applicationsUpdatedLastWeek).isEqualTo(3);
    assertThat(result.applicationsUpdatedLastYear).isEqualTo(7);
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

  private void createSbomMetadata(boolean save, String status, Date createdAt) {
    Application application = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartySbomMetadata metadata =
        ThirdPartySbomMetadataTestUtil.createSbomMetadata(status, application.getId(), thirdPartyFile.getId());
    if (createdAt != null) {
      metadata.setCreatedAt(createdAt);
    }

    if (save) {
      dao.insert(metadata);
    }
  }
}
