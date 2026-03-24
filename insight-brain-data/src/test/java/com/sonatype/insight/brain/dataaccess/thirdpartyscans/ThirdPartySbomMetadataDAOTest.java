/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.datastore.ThirdPartyScansDataStore;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.SearchIndexChange.ChangeType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.ComplianceStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ApiSbomApplicationsHistoryMetricDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.scan.file.SbomFormat;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.dataaccess.TemporaryEntity.uuid;
import static com.sonatype.insight.brain.db.IdUtil.newUUID;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.ACTIVE;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.UPLOADED;
import static com.sonatype.insight.brain.utils.SbomMetadataBuilder.buildMetadataJson;
import static com.sonatype.insight.brain.utils.SbomMetadataBuilder.newSbomMetadataBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ThirdPartySbomMetadataDAOTest
    extends AbstractDbDAOTest
{
  private ThirdPartySbomMetadataDAO dao;

  private ThirdPartyFileDAO thirdPartyFileDAO;

  private SearchIndexChangeDAO searchIndexChangeDAO;

  @Before
  @Override
  public void setup() {
    super.setup();

    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    systemConfigurationPropertyDAO.update(
        new SystemConfigurationProperty(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true"));
    this.dao = daoFactory.createThirdPartySbomMetadataDAO();
    this.thirdPartyFileDAO = daoFactory.createThirdPartyFileDAO();
    this.searchIndexChangeDAO = daoFactory.createSearchIndexChangeDAO();
    searchIndexChangeDAO.getAll().forEach(searchIndexChangeDAO::delete);
  }

  @Test
  public void testCRUD_H2() {
    testCRUD();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testCRUD_Postgres() {
    testCRUD();
  }

  private void testCRUD() {
    // Create
    ThirdPartySbomMetadata entity = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();
    assertThat(entity.getId()).isNotNull();

    // Read
    ThirdPartySbomMetadata fetchedThirdPartySbomMetadata = dao.getById(entity.getId());
    assertThirdPartySbomMetadata(fetchedThirdPartySbomMetadata, entity);

    // Update
    entity.setSbomVersion("new version");
    entity.setSerialNumber("new serial number");
    entity.setIsValid(false);
    dao.update(entity);

    fetchedThirdPartySbomMetadata = dao.getById(entity.getId());
    assertThirdPartySbomMetadata(fetchedThirdPartySbomMetadata, entity);

    // Delete
    dao.delete(entity);
    ThirdPartySbomMetadata updated = dao.getById(entity.getId());
    assertThat(updated).isNull();
    assertSearchIndexUpdated(fetchedThirdPartySbomMetadata);
  }

  @Test
  public void testIsValid_whenNull() throws SQLException {
    ThirdPartyFile thirdPartyFile = new ThirdPartyFile("third-party-file", new Date());
    thirdPartyFileDAO.insert(thirdPartyFile);

    ThirdPartyScansDataStore thirdPartyScansDataStore = databaseRule.getThirdPartyScansDataStore();
    String sql = "INSERT INTO " + thirdPartyScansDataStore.getDatabaseSchema() +
        ".sbom_metadata (sbom_metadata_id, third_party_file_id, application_id, file_name, " +
        "serial_number, sbom_version, spec, spec_format, spec_version, status, created_at, " +
        "metadata_json, scan_type, validation_skipped) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, null);";

    String randomString = TemporaryEntity.uuid().substring(0, 10);
    String id = newUUID();

    try (Connection connection = thirdPartyScansDataStore.getDataSource().getConnection()) {
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, id);
        statement.setString(2, thirdPartyFile.getId());
        statement.setString(3, randomString);
        statement.setString(4, randomString);
        statement.setString(5, randomString);
        statement.setString(6, randomString);
        statement.setString(7, randomString);
        statement.setString(8, SbomFormat.XML.toString());
        statement.setString(9, randomString);
        statement.setString(10, "ACTIVE");
        statement.setDate(11, new java.sql.Date(new Date().getTime()));
        statement.setString(12, buildMetadataJson());
        statement.setString(13, "SBOM");
        statement.executeUpdate();
      }
    }

    // Read
    ThirdPartySbomMetadata fetchedThirdPartySbomMetadata = dao.getById(id);
    assertThat(fetchedThirdPartySbomMetadata.getIsValid()).isTrue();

    // Update
    fetchedThirdPartySbomMetadata.setIsValid(false);
    dao.update(fetchedThirdPartySbomMetadata);

    ThirdPartySbomMetadata updatedThirdPartySbomMetadata = dao.getById(fetchedThirdPartySbomMetadata.getId());
    assertThat(updatedThirdPartySbomMetadata.getIsValid()).isFalse();
    assertThirdPartySbomMetadata(updatedThirdPartySbomMetadata, fetchedThirdPartySbomMetadata);
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
  public void testDeleteByThirdPartyFileId_H2() {
    testDeleteByThirdPartyFileId();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testDeleteByThirdPartyFileId_Postgres() {
    testDeleteByThirdPartyFileId();
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

      ThirdPartySbomMetadata updated = dao.getByThirdPartyFileId(entity.getThirdPartyFileId());
      assertThat(updated).isNull();
      assertSearchIndexUpdated(sbomMetadata);
    }
  }

  private void assertSearchIndexUpdated(final ThirdPartySbomMetadata sbomMetadata) {
    List<SearchIndexChange> searchIndexChanges = searchIndexChangeDAO.getAll();
    assertThat(searchIndexChanges).hasSize(1);
    assertThat(searchIndexChanges.get(0).getChangeType()).isEqualTo(ChangeType.SBOM);
    assertThat(searchIndexChanges.get(0).getChangeData()).isEqualTo(
        String.format("%s:%s", sbomMetadata.getApplicationId(), sbomMetadata.getSbomVersion()));
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
        .withStatus(PENDING)
        .build();

    List<ThirdPartySbomMetadata> sbomMetadata = dao.getActiveByApplicationId(application.getId());

    assertThat(sbomMetadata).hasSize(1);
    assertThirdPartySbomMetadata(sbomMetadata.get(0), activeSbom);
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
  public void testGetActiveSbomCount() {
    IntStream.rangeClosed(1, 3).forEach(i -> createSbomMetadata(true, ACTIVE));
    long sbomCount = dao.getActiveSbomCount();
    assertThat(sbomCount).isEqualTo(3);
  }

  @Test
  public void testGetActiveSbomCount_Different_Statuses() {
    IntStream.rangeClosed(1, 3).forEach(i -> createSbomMetadata(true, ACTIVE));
    IntStream.rangeClosed(1, 4).forEach(i -> createSbomMetadata(true, PENDING));
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
    assertThat(actual.getIsValid()).isEqualTo(expected.getIsValid());
    assertThat(actual.getOriginalBinaryFileName()).isEqualTo(expected.getOriginalBinaryFileName());
  }

  ThirdPartySbomMetadata createSbomMetadata(boolean save, ThirdPartySbomMetadataStatus status) {
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
    var activeState = ACTIVE;
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
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus(PENDING).build();

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
    var activeState = ACTIVE;
    Date now = new Date();
    Date oneYearAgo = DateUtils.addYears(now, -1);
    Date sixMonthsAgo = DateUtils.addMonths(now, -6);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date oneMonthAgo = DateUtils.addMonths(now, -1);
    Date oneWeekAgo = DateUtils.addWeeks(now, -1);
    Date yesterday = DateUtils.addDays(now, -1);

    // count just one time
    newSbomMetadataBuilder(daoFactory).withCreatedAt(now)
        .withStatus(activeState)
        .withApplicationId(application.getId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneYearAgo)
        .withStatus(activeState)
        .withApplicationId(application.getId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneWeekAgo)
        .withStatus(activeState)
        .withApplicationId(application.getId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneMonthAgo)
        .withStatus(activeState)
        .withApplicationId(application.getId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(sixMonthsAgo)
        .withStatus(activeState)
        .withApplicationId(application.getId())
        .build();

    newSbomMetadataBuilder(daoFactory).withCreatedAt(sixMonthsAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoMonthsAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneWeekAgo).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus(activeState).build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday)
        .withStatus(PENDING)
        .build();

    ApiSbomApplicationsHistoryMetricDTO result = dao.getSbomsHistoryMetrics();
    assertThat(result).isNotNull();
    assertThat(result.totalScannedApplications).isEqualTo(5);
    assertThat(result.applicationsUpdatedLastMonth).isEqualTo(3);
    assertThat(result.applicationsUpdatedLastWeek).isEqualTo(3);
    assertThat(result.applicationsUpdatedLastYear).isEqualTo(5);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetApplications_WithResults() {

    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withCreatedAt(new Date())
        .build();

    PolicyEvaluation policyEvaluationOlder = tempEntity.newPolicyEvaluation(application.getId(),
        ComplianceStageType.ID, "scanId1AppOlder");
    Policy policyOlder = tempEntity.newPolicy(application, 8);
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluationOlder, policyOlder, "g1",
        "a1", "v1", "h1", "r1");

    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withCreatedAt(new Date())
        .build();

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
        "cve-2", sbomMetadata.getId(), "description2", "link2", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix1");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity1);

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity2 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2,
        "cve-2", sbomMetadata.getId(), "description2", "link2", CvssV3Severity.NONE.getStartScoreRange(),
        CvssV3Severity.NONE.getDisplayName(), "fix2");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-3", sbomMetadata.getId(), "description3", "link3",
        CvssV3Severity.LOW.getStartScoreRange() + 0.2f, CvssV3Severity.LOW.getDisplayName(), "fix3");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-4", sbomMetadata.getId(), "description4", "link4",
        CvssV3Severity.MEDIUM.getStartScoreRange() + 1f, CvssV3Severity.MEDIUM.getDisplayName(), "fix4");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-5", sbomMetadata.getId(), "description5", "link5",
        CvssV3Severity.HIGH.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix5");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-6", sbomMetadata.getId(), "description6", "link6",
        CvssV3Severity.HIGH.getEndScoreRange() - 0.1f, CvssV3Severity.HIGH.getDisplayName(), "fix6");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-7", sbomMetadata.getId(), "description7", "link7",
        CvssV3Severity.CRITICAL.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix7");

    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity2);

    // fixed first policy violation
    policyViolation.setFixTime(new Date());
    tempEntity.updatePolicyViolation(policyViolation);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        ComplianceStageType.ID, "scanId1App1");
    Policy policy = tempEntity.newPolicy(application);
    tempEntity.newPolicyViolation(policyEvaluation, policy, "g1",
        "a1", "v1", "h1", "r1");

    SbomApplicationListSummaryDTO resultDtoList = dao.getSbomApplicationsWithRecentlyImportedSbomVersion(
        Set.of(application.getId()),
        SbomApplicationsSortableField.IMPORT_DATE,
        false,
        1,
        3);

    assertThat(resultDtoList.getApplications()).hasSize(1);
    assertThat(resultDtoList.getTotalCount()).isEqualTo(1);
    SbomApplicationSummaryDTO applicationPageApplicationSummaryDTO = resultDtoList.getApplications().get(0);
    assertThat(applicationPageApplicationSummaryDTO.getReleaseStatusPercentage()).isEqualTo(25.0);
    assertThat(applicationPageApplicationSummaryDTO.getVulnerabilitySummary().getNone())
        .isEqualTo(1);
    assertThat(applicationPageApplicationSummaryDTO.getVulnerabilitySummary().getLow())
        .isEqualTo(1);
    assertThat(applicationPageApplicationSummaryDTO.getVulnerabilitySummary().getMedium())
        .isEqualTo(1);
    assertThat(applicationPageApplicationSummaryDTO.getVulnerabilitySummary().getHigh())
        .isEqualTo(2);
    assertThat(applicationPageApplicationSummaryDTO.getVulnerabilitySummary().getCritical())
        .isEqualTo(2);

    assertThat(applicationPageApplicationSummaryDTO.getPolicyViolationSummary()
        .getCritical()).isEqualTo(0);
    assertThat(applicationPageApplicationSummaryDTO.getPolicyViolationSummary()
        .getSevere()).isEqualTo(1);
    assertThat(applicationPageApplicationSummaryDTO.getPolicyViolationSummary()
        .getModerate()).isEqualTo(0);
    assertThat(applicationPageApplicationSummaryDTO.getPolicyViolationSummary()
        .getLow()).isEqualTo(0);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetApplications_WithResults_SortByReleaseStatusPercentage() {
    Application application1 = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application1.getId())
        .build();

    Application application2 = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata1 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application2.getId())
        .build();

    Application application3 = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata4 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application3.getId())
        .build();

    Date yesterday = DateUtils.addDays(new Date(), -1);
    ThirdPartySbomMetadata sbomMetadata2 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withCreatedAt(new Date())
        .build();
    ThirdPartySbomMetadata sbomMetadata3 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withCreatedAt(yesterday)
        .build();

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    PackageUrlIdentifier packageUrlIdentifier = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier);
    ThirdPartyFileCoordinate coordinate = tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(),
        "s", packageUrlIdentifier.getFormat(), packageUrlIdentifier.getName(), packageUrlIdentifier.getVersion(),
        "h", packageUrlIdentifier.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity(coordinate,
        "cve-2", sbomMetadata.getId(), "description", "link", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity);

    ComponentIdentifier componentIdentifier4 = ComponentIdentifier.createNpmCoordinates("p", "v");
    PackageUrlIdentifier packageUrlIdentifier4 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier4);
    ThirdPartyFileCoordinate coordinate4 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata4.getThirdPartyFileId(),
        "s", packageUrlIdentifier.getFormat(), packageUrlIdentifier.getName(), packageUrlIdentifier4.getVersion(),
        "h", packageUrlIdentifier.getPackageUrl());
    tempEntity.newThirdPartyCoordinateSecurity(coordinate4,
        "cve-2", sbomMetadata4.getId(), "description", "link", CvssV3Severity.LOW.getEndScoreRange(),
        CvssV3Severity.LOW.getDisplayName(), "fix");

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createNpmCoordinates("p1", "v1");
    PackageUrlIdentifier packageUrlIdentifier1 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier1);
    ThirdPartyFileCoordinate coordinate1 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata2.getThirdPartyFileId(),
        "s1", packageUrlIdentifier1.getFormat(), packageUrlIdentifier1.getName(), packageUrlIdentifier1.getVersion(),
        "h1", packageUrlIdentifier1.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
        "cve-2", sbomMetadata2.getId(), "description2", "link2", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix1");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity1);

    tempEntity.newThirdPartyCoordinateSecurity(coordinate,
        "cve-21", sbomMetadata1.getId(), "description", "link", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix");

    ComponentIdentifier componentIdentifier2 = ComponentIdentifier.createNpmCoordinates("p2", "v2");
    PackageUrlIdentifier packageUrlIdentifier2 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier2);
    ThirdPartyFileCoordinate coordinate2 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata2.getThirdPartyFileId(),
        "s2", packageUrlIdentifier2.getFormat(), packageUrlIdentifier2.getName(), packageUrlIdentifier2.getVersion(),
        "h2", packageUrlIdentifier2.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity2 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2,
        "cve-2", sbomMetadata2.getId(), "description2", "link2", CvssV3Severity.NONE.getStartScoreRange(),
        CvssV3Severity.NONE.getDisplayName(), "fix2");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-3", sbomMetadata2.getId(), "description3", "link3",
        CvssV3Severity.LOW.getStartScoreRange() + 0.2f, CvssV3Severity.LOW.getDisplayName(), "fix3");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-4", sbomMetadata2.getId(), "description4", "link4",
        CvssV3Severity.MEDIUM.getStartScoreRange() + 1f, CvssV3Severity.MEDIUM.getDisplayName(), "fix4");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-5", sbomMetadata2.getId(), "description5", "link5",
        CvssV3Severity.HIGH.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix5");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-6", sbomMetadata2.getId(), "description6", "link6",
        CvssV3Severity.HIGH.getEndScoreRange() - 0.1f, CvssV3Severity.HIGH.getDisplayName(), "fix6");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "cve-7", sbomMetadata2.getId(), "description7", "link7",
        CvssV3Severity.CRITICAL.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "fix7");

    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity2);

    ComponentIdentifier componentIdentifier3 = ComponentIdentifier.createNpmCoordinates("p3", "v3");
    PackageUrlIdentifier packageUrlIdentifier3 = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier3);
    ThirdPartyFileCoordinate coordinate3 = tempEntity.newThirdPartyFileCoordinate(sbomMetadata3.getThirdPartyFileId(),
        "s3", packageUrlIdentifier3.getFormat(), packageUrlIdentifier3.getName(), packageUrlIdentifier3.getVersion(),
        "h3", packageUrlIdentifier3.getPackageUrl());

    ThirdPartyCoordinateSecurity coordinateSecurity3 = tempEntity.newThirdPartyCoordinateSecurity(coordinate3,
        "cve-3", sbomMetadata3.getId(), "description3", "link3", CvssV3Severity.CRITICAL.getEndScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "fix3");
    insertVEXToThirdPartyCoordinateSecurity(coordinateSecurity3);

    Policy policy = tempEntity.newPolicy(application);
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(application.getId(), ComplianceStageType.ID, "scanId1App1");
    tempEntity.newPolicyViolation(policyEvaluation, policy, "g1", "a1", "v1", "h1", "r1");

    SbomApplicationListSummaryDTO resultDtoList = dao.getSbomApplicationsWithRecentlyImportedSbomVersion(
        null,
        SbomApplicationsSortableField.RELEASE_STATUS_PERCENTAGE, false, 1, 5);

    assertThat(resultDtoList.getApplications()).hasSize(4);

    assertThat(resultDtoList.getApplications().subList(0, 2))
        .extracting(SbomApplicationSummaryDTO::getApplicationInternalId,
            SbomApplicationSummaryDTO::getReleaseStatusPercentage)
        .containsExactlyInAnyOrder(
            tuple(application1.getId(), 100.0),
            tuple(application3.getId(), 100.0));

    SbomApplicationSummaryDTO applicationPageApplicationSummaryDTO3 = resultDtoList.getApplications().get(2);
    assertThat(applicationPageApplicationSummaryDTO3.getApplicationInternalId()).isEqualTo(application.getId());
    assertThat(applicationPageApplicationSummaryDTO3.getReleaseStatusPercentage()).isEqualTo(25.0);

    SbomApplicationSummaryDTO applicationPageApplicationSummaryDTO4 = resultDtoList.getApplications().get(3);
    assertThat(applicationPageApplicationSummaryDTO4.getApplicationInternalId()).isEqualTo(application2.getId());
    assertThat(applicationPageApplicationSummaryDTO4.getReleaseStatusPercentage()).isEqualTo(0.0);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetApplications_WithLatestSBOMMetadataInPendingStatus() {
    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withCreatedAt(new Date())
        .build();

    SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withStatus(PENDING)
        .withCreatedAt(new Date())
        .build();

    SbomApplicationListSummaryDTO resultDtoList = dao.getSbomApplicationsWithRecentlyImportedSbomVersion(
        Set.of(application.getId()),
        SbomApplicationsSortableField.IMPORT_DATE,
        false,
        1,
        3);

    assertThat(resultDtoList.getApplications()).hasSize(1);
    assertThat(resultDtoList.getTotalCount()).isEqualTo(1);
  }

  @Test
  public void testGetSbomsHistoryMetrics_UpdatedVEX() {
    var activeState = ACTIVE;
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
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday).withStatus(PENDING).build();

    ThirdPartySbomMetadata sbomMetadata = newSbomMetadataBuilder(daoFactory).withCreatedAt(twoYearAgo)
        .withStatus(activeState)
        .build();
    ThirdPartyFileCoordinate coordinate = tempEntity.newThirdPartyFileCoordinate(
        sbomMetadata.getThirdPartyFileId(), "s4", "f4", "n4", "v4", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity(coordinate,
        "r14", "d14", "l14", CvssV3Severity.LOW.getStartScoreRange(),
        CvssV3Severity.LOW.getDisplayName(), "f14");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity, coordinateSecurity.getRefId(),
        "state", "justification", "response", "detail", now, now);

    ThirdPartySbomMetadata sbomMetadata2 = newSbomMetadataBuilder(daoFactory).withCreatedAt(twoYearAgo)
        .withStatus(activeState)
        .build();
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
  public void testGetInactiveSbomsBeforeOrAt_inactiveSboms() {
    Date now = new Date();
    Date twoDaysAgo = DateUtils.addDays(now, -2);
    Date twentyFourHoursAgo = DateUtils.addDays(now, -1);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date threeHoursAgo = DateUtils.addHours(now, -3);

    ThirdPartySbomMetadata twoDaysAgoUploadedMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(UPLOADED)
        .withCreatedAt(twoDaysAgo)
        .build();
    assertThat(twoDaysAgoUploadedMetadata.getId()).isNotNull();
    ThirdPartySbomMetadata twoDaysAgoPendingMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(PENDING)
        .withCreatedAt(twoDaysAgo)
        .build();
    assertThat(twoDaysAgoPendingMetadata.getId()).isNotNull();

    ThirdPartySbomMetadata twentyFourHoursAgoUploadedMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(UPLOADED)
        .withCreatedAt(twentyFourHoursAgo)
        .build();
    assertThat(twentyFourHoursAgoUploadedMetadata.getId()).isNotNull();
    ThirdPartySbomMetadata twentyFourHoursAgoPendingMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(PENDING)
        .withCreatedAt(twentyFourHoursAgo)
        .build();
    assertThat(twentyFourHoursAgoPendingMetadata.getId()).isNotNull();

    ThirdPartySbomMetadata twoMonthsAgoActiveMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(ACTIVE)
        .withCreatedAt(twoMonthsAgo)
        .build();
    assertThat(twoMonthsAgoActiveMetadata.getId()).isNotNull();

    ThirdPartySbomMetadata threeHoursAgoUploadedMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(UPLOADED)
        .withCreatedAt(threeHoursAgo)
        .build();
    assertThat(threeHoursAgoUploadedMetadata.getId()).isNotNull();
    ThirdPartySbomMetadata threeHoursAgoPendingMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(PENDING)
        .withCreatedAt(threeHoursAgo)
        .build();
    assertThat(threeHoursAgoPendingMetadata.getId()).isNotNull();

    assertThat(dao.getAll()).isNotNull()
        .hasSize(7);

    List<ThirdPartySbomMetadata> sbomMetadataList = dao.getInactiveSbomsBeforeOrAt(DateUtils.addDays(now, -1));

    assertThat(sbomMetadataList).hasSize(4);
    assertThat(sbomMetadataList)
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactlyInAnyOrder(
            twoDaysAgoUploadedMetadata.getId(),
            twoDaysAgoPendingMetadata.getId(),
            twentyFourHoursAgoUploadedMetadata.getId(),
            twentyFourHoursAgoPendingMetadata.getId());
  }

  @Test
  public void testGetInactiveSbomsBeforeOrAt_noInactiveSboms() {
    Date now = new Date();
    Date twoDaysAgo = DateUtils.addDays(now, -2);
    Date twoMonthsAgo = DateUtils.addMonths(now, -2);
    Date threeHoursAgo = DateUtils.addHours(now, -3);
    ThirdPartySbomMetadata twoDaysAgoActiveMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(ACTIVE)
        .withCreatedAt(twoDaysAgo)
        .build();
    assertThat(twoDaysAgoActiveMetadata.getId()).isNotNull();

    ThirdPartySbomMetadata twoMonthsAgoActiveMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(ACTIVE)
        .withCreatedAt(twoMonthsAgo)
        .build();
    assertThat(twoMonthsAgoActiveMetadata.getId()).isNotNull();

    ThirdPartySbomMetadata threeHoursAgoActiveMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withStatus(ACTIVE)
        .withCreatedAt(threeHoursAgo)
        .build();
    assertThat(threeHoursAgoActiveMetadata.getId()).isNotNull();

    assertThat(dao.getAll())
        .hasSize(3);

    List<ThirdPartySbomMetadata> sbomMetadataList = dao.getInactiveSbomsBeforeOrAt(DateUtils.addDays(now, -1));

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
    tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, "xyz");

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
        .withStatus(PENDING)
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

    List<ThirdPartySbomMetadata> results = dao.getByApplicationIdAndStatus(applicationId, ACTIVE, page, pageSize);

    assertThat(results).hasSize(2);
    assertThat(results.get(0)).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG).isEqualTo(sbomMetadata3);
    assertThat(results.get(1)).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG).isEqualTo(sbomMetadata2);

    page = 2;
    results = dao.getByApplicationIdAndStatus(applicationId, ACTIVE, page, pageSize);

    assertThat(results).hasSize(1);
    assertThat(results.get(0)).usingRecursiveComparison(JPA.RECURSIVE_COMPARISON_CONFIG).isEqualTo(sbomMetadata1);

    page = 3;
    results = dao.getByApplicationIdAndStatus(applicationId, ACTIVE, page, pageSize);

    assertThat(results).isEmpty();

    page = 1;
    pageSize = 0;
    results = dao.getByApplicationIdAndStatus(applicationId, ACTIVE, page, pageSize);

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
        .withStatus(PENDING)
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

    List<ThirdPartySbomMetadata> results = dao.getByApplicationIdAndStatus(applicationId, ACTIVE, page, pageSize);

    assertThat(results).isEmpty();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomApplicationVulnerabilities_SortyByField() {
    Organization organization1 = tempEntity.newOrganization("org1");
    Application application = tempEntity.newApplication(organization1.getId());

    ThirdPartyFile file1 = tempEntity.newThirdPartyFile("CycloneDX-bom.xml");
    ThirdPartyFile file2 = tempEntity.newThirdPartyFile("SPDX-spdx.json");
    ThirdPartyFile file3 = tempEntity.newThirdPartyFile("file.json");
    ThirdPartyFile file4 = tempEntity.newThirdPartyFile("SPDX-spdx-1.json");

    ThirdPartySbomMetadata sbom1 =
        tempEntity.newThirdPartySbomMetadata(file1.getId(), application.getId(), ACTIVE, file1.getFilename());
    ThirdPartySbomMetadata sbom2 =
        tempEntity.newThirdPartySbomMetadata(file4.getId(), application.getId(), ACTIVE, file4.getFilename());
    ThirdPartySbomMetadata sbom3 =
        tempEntity.newThirdPartySbomMetadata(file2.getId(), application.getId(), uuid().substring(0, 10), ACTIVE,
            file2.getFilename(), "SPDX", "JSON", "1.5", new Date(), false);

    ThirdPartySbomMetadata sbom4 =
        tempEntity.newThirdPartySbomMetadata(file3.getId(), application.getId(), PENDING, file3.getFilename());

    ThirdPartyFileCoordinate c1 = tempEntity.newThirdPartyFileCoordinate(file1, "s1", "f1", "n1", "v1");
    ThirdPartyFileCoordinate c2 = tempEntity.newThirdPartyFileCoordinate(file2, "s2", "f2", "n2", "v2");
    ThirdPartyFileCoordinate c3 = tempEntity.newThirdPartyFileCoordinate(file3, "s3", "f3", "n3", "v3");
    ThirdPartyFileCoordinate c4 = tempEntity.newThirdPartyFileCoordinate(file4, "s4", "f4", "n4", "v4");

    tempEntity.newThirdPartyCoordinateSecurity(c1, "r1", sbom1.getId(), "d1", "l1", 3.5F, "sd1", "f1"); // low
    tempEntity.newThirdPartyCoordinateSecurity(c1, "r2", sbom1.getId(), "d2", "l2", 7.5F, "sd2", "f2"); // high
    ThirdPartyCoordinateSecurity cs3 =
        tempEntity.newThirdPartyCoordinateSecurity(c2, "r3", sbom2.getId(), "d3", "l3", 1.5F, "sd3", "f3"); // low
    ThirdPartyCoordinateSecurity cs4 =
        tempEntity.newThirdPartyCoordinateSecurity(c2, "r4", sbom2.getId(), "d4", "l4", 0.5F, "sd4", "f4"); // low
    ThirdPartyCoordinateSecurity cs5 =
        tempEntity.newThirdPartyCoordinateSecurity(c2, "r5", sbom2.getId(), "d3", "l3", 6.9F, "sd3", "f3");// medium
    ThirdPartyCoordinateSecurity cs6 =
        tempEntity.newThirdPartyCoordinateSecurity(c2, "r6", sbom2.getId(), "d4", "l4", 7.0F, "sd4", "f4"); // high
    ThirdPartyCoordinateSecurity cs7 =
        tempEntity.newThirdPartyCoordinateSecurity(c2, "r7", sbom2.getId(), "d3", "l3", 9.0F, "sd3", "f3"); // critical
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r8", sbom2.getId(), "d4", "l4", 7.0F, "sd4", "f4"); // high
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r9", sbom2.getId(), "d5", "l5", 4.7F, "sd5", "f5"); // medium
    tempEntity.newThirdPartyCoordinateSecurity(c2, "r10", sbom2.getId(), "d6", "l6", 0F, "sd6", "f6"); // none
    tempEntity.newThirdPartyCoordinateSecurity(c3, "r7", sbom3.getId(), "d7", "l7", 1F, "sd7", "f7"); // low
    ThirdPartyCoordinateSecurity cs8 =
        tempEntity.newThirdPartyCoordinateSecurity(c4, "r1", sbom4.getId(), "d1", "l1", 3.5F, "sd1", "f1"); // low
    ThirdPartyCoordinateSecurity cs9 =
        tempEntity.newThirdPartyCoordinateSecurity(c4, "r2", sbom4.getId(), "d1", "l1", 6.9F, "sd1", "f1"); // medium

    insertVEXToThirdPartyCoordinateSecurity(cs3);
    insertVEXToThirdPartyCoordinateSecurity(cs4);
    insertVEXToThirdPartyCoordinateSecurity(cs5);
    insertVEXToThirdPartyCoordinateSecurity(cs6);
    insertVEXToThirdPartyCoordinateSecurity(cs7);
    insertVEXToThirdPartyCoordinateSecurity(cs8);
    insertVEXToThirdPartyCoordinateSecurity(cs9);

    ThirdPartySbomMetadataSummaryListDTO result =
        dao.getSbomApplicationVulnerabilities(application.getId(), 5, 1,
            SbomVersionsApplicationSortableField.RELEASE_STATUS, true);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(3);
    result.getResults().sort(Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getReleaseStatusPercentage));

    result =
        dao.getSbomApplicationVulnerabilities(application.getId(), 5, 1,
            SbomVersionsApplicationSortableField.RELEASE_STATUS, false);
    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(3);
    result.getResults()
        .sort(Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getReleaseStatusPercentage).reversed());

    result =
        dao.getSbomApplicationVulnerabilities(application.getId(), 5, 1,
            SbomVersionsApplicationSortableField.IMPORT_DATE, true);

    result.getResults()
        .sort(Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getImportDate));

    result =
        dao.getSbomApplicationVulnerabilities(application.getId(), 5, 1,
            SbomVersionsApplicationSortableField.IMPORT_DATE, false);

    result.getResults()
        .sort(Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getImportDate).reversed());

    result =
        dao.getSbomApplicationVulnerabilities(application.getId(), 5, 1,
            SbomVersionsApplicationSortableField.VULNERABILITY, false);
    result.getResults()
        .sort(Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getCritical)
            .reversed()
            .thenComparing(Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getHigh).reversed())
            .thenComparing(Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getMedium).reversed())
            .thenComparing(Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getLow).reversed())
            .thenComparing(Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getNone).reversed()));

    result =
        dao.getSbomApplicationVulnerabilities(application.getId(), 5, 1,
            SbomVersionsApplicationSortableField.VULNERABILITY, true);
    result.getResults()
        .sort(Comparator.comparing(ThirdPartySbomMetadataSummaryDTO::getCritical)
            .thenComparing(ThirdPartySbomMetadataSummaryDTO::getHigh)
            .thenComparing(ThirdPartySbomMetadataSummaryDTO::getMedium)
            .thenComparing(ThirdPartySbomMetadataSummaryDTO::getLow)
            .thenComparing(ThirdPartySbomMetadataSummaryDTO::getNone));
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetSbomApplicationVulnerabilities_nullIsValid() {
    Organization organization1 = tempEntity.newOrganization("org1");
    Application application = tempEntity.newApplication(organization1.getId());
    ThirdPartyFile file1 = tempEntity.newThirdPartyFile("CycloneDX-bom.xml");

    ThirdPartySbomMetadata sbomNullIsValid = new ThirdPartySbomMetadata();
    sbomNullIsValid.setSerialNumber(uuid().substring(0, 10));
    sbomNullIsValid.setSpec("CycloneDx");
    sbomNullIsValid.setSpecFormat("XML");
    sbomNullIsValid.setSpecVersion("1.5");
    sbomNullIsValid.setStatus(ACTIVE);
    sbomNullIsValid.setSbomVersion(uuid().substring(0, 10));
    sbomNullIsValid.setApplicationId(application.getId());
    sbomNullIsValid.setFilename("file.json");
    sbomNullIsValid.setThirdPartyFileId(file1.getId());
    sbomNullIsValid.setCreatedAt(new Date());
    sbomNullIsValid.setScanType("SBOM");

    dao.insert(sbomNullIsValid);

    ThirdPartySbomMetadataSummaryListDTO result =
        dao.getSbomApplicationVulnerabilities(application.getId(), 5, 1,
            SbomVersionsApplicationSortableField.IMPORT_DATE, true);

    assertThat(result).isNotNull();
    assertThat(result.getTotalResultsCount()).isEqualTo(1);
    List<ThirdPartySbomMetadataSummaryDTO> results = result.getResults();

    ThirdPartySbomMetadataSummaryDTO dto = results.get(0);
    assertThat(dto.getApplicationVersion()).isEqualTo(sbomNullIsValid.getSbomVersion());
    assertThat(dto.getIsValid()).isEqualTo(true);
  }

  @Test
  public void testGetActiveByApplicationId_Paged() {
    Application app = tempEntity.newApplicationWithParent();

    assertThat(dao.getActiveByApplicationId(app.getId(), 1, 2)).isEmpty();
    assertThat(dao.getActiveByApplicationId(app.getId(), 2, 2)).isEmpty();
    assertThat(dao.getActiveByApplicationId(app.getId(), 3, 2)).isEmpty();

    ThirdPartySbomMetadata sbom1 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(0));

    assertThat(dao.getActiveByApplicationId(app.getId(), 1, 2))
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactly(sbom1.getId());
    assertThat(dao.getActiveByApplicationId(app.getId(), 2, 2)).isEmpty();
    assertThat(dao.getActiveByApplicationId(app.getId(), 3, 2)).isEmpty();

    ThirdPartySbomMetadata sbom2 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(1));

    assertThat(dao.getActiveByApplicationId(app.getId(), 1, 2))
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactly(sbom1.getId(), sbom2.getId());
    assertThat(dao.getActiveByApplicationId(app.getId(), 2, 2)).isEmpty();
    assertThat(dao.getActiveByApplicationId(app.getId(), 3, 2)).isEmpty();

    ThirdPartySbomMetadata sbom3 = tempEntity.newThirdPartySbomMetadata(app.getId(), ACTIVE, new Date(2));

    assertThat(dao.getActiveByApplicationId(app.getId(), 1, 2))
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactly(sbom1.getId(), sbom2.getId());
    assertThat(dao.getActiveByApplicationId(app.getId(), 2, 2))
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactly(sbom3.getId());
    assertThat(dao.getActiveByApplicationId(app.getId(), 3, 2)).isEmpty();

    tempEntity.newThirdPartySbomMetadata(app.getId(), UPLOADED, new Date(3));
    tempEntity.newThirdPartySbomMetadata(app.getId(), PENDING, new Date(4));

    assertThat(dao.getActiveByApplicationId(app.getId(), 1, 2))
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactly(sbom1.getId(), sbom2.getId());
    assertThat(dao.getActiveByApplicationId(app.getId(), 2, 2))
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactly(sbom3.getId());
    assertThat(dao.getActiveByApplicationId(app.getId(), 3, 2)).isEmpty();
  }

  private void testGetActiveAtLatestOffset_NoSboms() {
    List<ThirdPartySbomMetadata> result = dao.getActiveAtLatestOffset(0, null, 1, 10);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetActiveAtLatestOffset_NoSboms_H2() {
    testGetActiveAtLatestOffset_NoSboms();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetActiveAtLatestOffset_NoSboms_Postgres() {
    testGetActiveAtLatestOffset_NoSboms();
  }

  private void testGetActiveAtLatestOffset_NoActiveSboms() {
    Application app = tempEntity.newApplicationWithParent();
    tempEntity.newThirdPartySbomMetadata(app.getId(), UPLOADED, "fileName1");
    tempEntity.newThirdPartySbomMetadata(app.getId(), PENDING, "fileName2");

    List<ThirdPartySbomMetadata> result = dao.getActiveAtLatestOffset(0, null, 1, 10);

    assertThat(result).isEmpty();
  }

  @Test
  public void testGetActiveAtLatestOffset_NoActiveSboms_H2() {
    testGetActiveAtLatestOffset_NoActiveSboms();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetActiveAtLatestOffset_NoActiveSboms_Postgres() {
    testGetActiveAtLatestOffset_NoActiveSboms();
  }

  private void testGetActiveAtLatestOffset_LatestOffset0ReturnsLatestPerApp() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplicationWithSpecificId("app1", "app1", "app1", org.getId());
    Application app2 = tempEntity.newApplicationWithSpecificId("app2", "app2", "app2", org.getId());
    Application app3 = tempEntity.newApplicationWithSpecificId("app3", "app3", "app3", org.getId());

    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(0));
    ThirdPartySbomMetadata app1Sbom2 = tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(1));
    ThirdPartySbomMetadata app2Sbom1 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(2));
    ThirdPartySbomMetadata app3Sbom1 = tempEntity.newThirdPartySbomMetadata(app3.getId(), ACTIVE, new Date(3));

    List<ThirdPartySbomMetadata> result = dao.getActiveAtLatestOffset(0, null, 1, 10);

    assertThat(result)
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactly(app3Sbom1.getId(), app2Sbom1.getId(), app1Sbom2.getId());
  }

  @Test
  public void testGetActiveAtLatestOffset_LatestOffset0ReturnsLatestPerApp_H2() {
    testGetActiveAtLatestOffset_LatestOffset0ReturnsLatestPerApp();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetActiveAtLatestOffset_LatestOffset0ReturnsLatestPerApp_Postgres() {
    testGetActiveAtLatestOffset_LatestOffset0ReturnsLatestPerApp();
  }

  private void testGetActiveAtLatestOffset_LatestOffset1ReturnsSecondLatestPerApp() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplicationWithSpecificId("app1", "app1", "app1", org.getId());
    Application app2 = tempEntity.newApplicationWithSpecificId("app2", "app2", "app2", org.getId());
    Application app3 = tempEntity.newApplicationWithSpecificId("app3", "app3", "app3", org.getId());

    ThirdPartySbomMetadata app1Sbom1 = tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(0));
    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(1));
    ThirdPartySbomMetadata app2Sbom1 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(2));
    tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(3));
    tempEntity.newThirdPartySbomMetadata(app3.getId(), ACTIVE, new Date(4));

    List<ThirdPartySbomMetadata> result = dao.getActiveAtLatestOffset(1, null, 1, 10);

    assertThat(result)
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactly(app2Sbom1.getId(), app1Sbom1.getId());
  }

  @Test
  public void testGetActiveAtLatestOffset_LatestOffset1ReturnsSecondLatestPerApp_H2() {
    testGetActiveAtLatestOffset_LatestOffset1ReturnsSecondLatestPerApp();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetActiveAtLatestOffset_LatestOffset1ReturnsSecondLatestPerApp_Postgres() {
    testGetActiveAtLatestOffset_LatestOffset1ReturnsSecondLatestPerApp();
  }

  private void testGetActiveAtLatestOffset_WithLastApplicationId() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplicationWithSpecificId("app1", "app1", "app1", org.getId());
    Application app2 = tempEntity.newApplicationWithSpecificId("app2", "app2", "app2", org.getId());
    Application app3 = tempEntity.newApplicationWithSpecificId("app3", "app3", "app3", org.getId());

    ThirdPartySbomMetadata app1Sbom1 = tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(0));
    ThirdPartySbomMetadata app2Sbom1 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(1));
    tempEntity.newThirdPartySbomMetadata(app3.getId(), ACTIVE, "app3File1");

    List<ThirdPartySbomMetadata> result = dao.getActiveAtLatestOffset(0, app2.getId(), 1, 10);

    assertThat(result)
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactly(app2Sbom1.getId(), app1Sbom1.getId());
  }

  @Test
  public void testGetActiveAtLatestOffset_WithLastApplicationId_H2() {
    testGetActiveAtLatestOffset_WithLastApplicationId();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetActiveAtLatestOffset_WithLastApplicationId_Postgres() {
    testGetActiveAtLatestOffset_WithLastApplicationId();
  }

  private void testGetActiveAtLatestOffset_Pagination() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplicationWithSpecificId("app1", "app1", "app1", org.getId());
    Application app2 = tempEntity.newApplicationWithSpecificId("app2", "app2", "app2", org.getId());
    Application app3 = tempEntity.newApplicationWithSpecificId("app3", "app3", "app3", org.getId());
    Application app4 = tempEntity.newApplicationWithSpecificId("app4", "app4", "app4", org.getId());

    ThirdPartySbomMetadata app1Sbom1 = tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(0));
    ThirdPartySbomMetadata app2Sbom1 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(1));
    ThirdPartySbomMetadata app3Sbom1 = tempEntity.newThirdPartySbomMetadata(app3.getId(), ACTIVE, new Date(2));
    ThirdPartySbomMetadata app4Sbom1 = tempEntity.newThirdPartySbomMetadata(app4.getId(), ACTIVE, new Date(3));

    List<ThirdPartySbomMetadata> page1 = dao.getActiveAtLatestOffset(0, null, 1, 2);
    assertThat(page1)
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactly(app4Sbom1.getId(), app3Sbom1.getId());

    List<ThirdPartySbomMetadata> page2 = dao.getActiveAtLatestOffset(0, null, 2, 2);
    assertThat(page2)
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactly(app2Sbom1.getId(), app1Sbom1.getId());

    List<ThirdPartySbomMetadata> page3 = dao.getActiveAtLatestOffset(0, null, 3, 2);
    assertThat(page3).isEmpty();
  }

  @Test
  public void testGetActiveAtLatestOffset_Pagination_H2() {
    testGetActiveAtLatestOffset_Pagination();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetActiveAtLatestOffset_Pagination_Postgres() {
    testGetActiveAtLatestOffset_Pagination();
  }

  private void testGetActiveAtLatestOffset_IgnoresInactiveStatus() {
    Organization org = tempEntity.newOrganization();
    Application app1 = tempEntity.newApplicationWithSpecificId("app1", "app1", "app1", org.getId());
    Application app2 = tempEntity.newApplicationWithSpecificId("app2", "app2", "app2", org.getId());

    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(0));
    ThirdPartySbomMetadata app1Sbom2 = tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, new Date(1));
    tempEntity.newThirdPartySbomMetadata(app1.getId(), PENDING, new Date(2));

    ThirdPartySbomMetadata app2Sbom1 = tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, new Date(3));
    tempEntity.newThirdPartySbomMetadata(app2.getId(), UPLOADED, new Date(4));

    List<ThirdPartySbomMetadata> result = dao.getActiveAtLatestOffset(0, null, 1, 10);

    assertThat(result)
        .extracting(ThirdPartySbomMetadata::getId)
        .containsExactly(app2Sbom1.getId(), app1Sbom2.getId());
  }

  @Test
  public void testGetActiveAtLatestOffset_IgnoresInactiveStatus_H2() {
    testGetActiveAtLatestOffset_IgnoresInactiveStatus();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetActiveAtLatestOffset_IgnoresInactiveStatus_Postgres() {
    testGetActiveAtLatestOffset_IgnoresInactiveStatus();
  }

  @Test
  public void testGetLastScanTimes_noScans() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory).build();
    assertThat(sbomMetadata.getId()).isNotNull();

    Map<String, Date> lastScanTimes = dao.getLastScanTimes(Set.of(sbomMetadata.getThirdPartyFileId()));
    assertThat(lastScanTimes).isEmpty();
  }

  @Test
  public void testGetLastScanTimes_multipleScansWithDifferentTimes() {
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile.getId(), app.getId(), ACTIVE, "test.json");
    Date now = new Date();
    Date oldTime = DateUtils.addDays(now, -3);
    Date middleTime = DateUtils.addDays(now, -2);
    Date recentTime = DateUtils.addDays(now, -1);
    String scanId1 = "scanId1";
    tempEntity.newThirdPartyScan("scanRequestId1", scanId1, thirdPartyFile);
    tempEntity.newPolicyEvaluation(app.getId(), ComplianceStageType.ID, scanId1, oldTime);
    String scanId2 = "scanId2";
    tempEntity.newThirdPartyScan("scanRequestId2", scanId2, thirdPartyFile);
    tempEntity.newPolicyEvaluation(app.getId(), ComplianceStageType.ID, scanId2, recentTime);
    String scanId3 = "scanId3";
    tempEntity.newThirdPartyScan("scanRequestId3", scanId3, thirdPartyFile);
    tempEntity.newPolicyEvaluation(app.getId(), ComplianceStageType.ID, scanId3, middleTime);

    Map<String, Date> lastScanTimes = dao.getLastScanTimes(Set.of(sbomMetadata.getThirdPartyFileId()));

    assertThat(lastScanTimes).hasSize(1);
    assertThat(lastScanTimes.get(sbomMetadata.getThirdPartyFileId())).isEqualTo(recentTime);
  }

  @Test
  public void testGetLastScanTimes_multipleThirdPartyFiles() {
    Application app = tempEntity.newApplicationWithParent();

    ThirdPartyFile thirdPartyFile1 = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata1 =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile1.getId(), app.getId(), ACTIVE, "test1.json");
    Date time1 = DateUtils.addDays(new Date(), -1);
    String scanId1 = "scanId1";
    tempEntity.newThirdPartyScan("scanRequestId1", scanId1, thirdPartyFile1);
    tempEntity.newPolicyEvaluation(app.getId(), ComplianceStageType.ID, scanId1, time1);

    ThirdPartyFile thirdPartyFile2 = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata2 =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile2.getId(), app.getId(), ACTIVE, "test2.json");
    Date time2 = DateUtils.addDays(new Date(), -2);
    String scanId2 = "scanId2";
    tempEntity.newThirdPartyScan("scanRequestId2", scanId2, thirdPartyFile2);
    tempEntity.newPolicyEvaluation(app.getId(), ComplianceStageType.ID, scanId2, time2);

    ThirdPartyFile thirdPartyFile3 = tempEntity.newThirdPartyFile();
    ThirdPartySbomMetadata sbomMetadata3 =
        tempEntity.newThirdPartySbomMetadata(thirdPartyFile3.getId(), app.getId(), ACTIVE, "test3.json");

    Map<String, Date> lastScanTimes = dao.getLastScanTimes(Set.of(
        sbomMetadata1.getThirdPartyFileId(),
        sbomMetadata2.getThirdPartyFileId(),
        sbomMetadata3.getThirdPartyFileId()));

    assertThat(lastScanTimes).hasSize(2);
    assertThat(lastScanTimes.get(sbomMetadata1.getThirdPartyFileId())).isEqualTo(time1);
    assertThat(lastScanTimes.get(sbomMetadata2.getThirdPartyFileId())).isEqualTo(time2);
    assertThat(lastScanTimes.get(sbomMetadata3.getThirdPartyFileId())).isNull();
  }

  @Test
  public void testGetMaxActiveSbomsAcrossApplications_noSboms() {
    long max = dao.getMaxActiveSbomsAcrossApplications();
    assertThat(max).isZero();
  }

  @Test
  public void testGetMaxActiveSbomsAcrossApplications_multipleSboms() {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    Application app3 = tempEntity.newApplicationWithParent();

    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, "app1File1");
    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, "app1File2");
    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, "app1File3");
    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, "app1File4");
    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, "app1File5");

    tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, "app2File1");
    tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, "app2File2");
    tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, "app2File3");

    tempEntity.newThirdPartySbomMetadata(app3.getId(), ACTIVE, "app3File1");

    long max = dao.getMaxActiveSbomsAcrossApplications();
    assertThat(max).isEqualTo(5);
  }

  @Test
  public void testGetMaxActiveSbomsAcrossApplications_ignoresInactiveStatus() {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();

    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, "app1File1");
    tempEntity.newThirdPartySbomMetadata(app1.getId(), ACTIVE, "app1File2");
    tempEntity.newThirdPartySbomMetadata(app1.getId(), PENDING, "app1File3");
    tempEntity.newThirdPartySbomMetadata(app1.getId(), UPLOADED, "app1File4");
    tempEntity.newThirdPartySbomMetadata(app1.getId(), UPLOADED, "app1File5");

    tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, "app2File1");
    tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, "app2File2");
    tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, "app2File3");
    tempEntity.newThirdPartySbomMetadata(app2.getId(), ACTIVE, "app2File4");

    long max = dao.getMaxActiveSbomsAcrossApplications();
    assertThat(max).isEqualTo(4);
  }

  private void insertVEXToThirdPartyCoordinateSecurity(ThirdPartyCoordinateSecurity coordinateSecurity) {
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity, coordinateSecurity.getRefId(),
        "state", "justification", "response", "detail");
  }
}
