/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static com.sonatype.insight.brain.utils.SbomMetadataBuilder.newSbomMetadataBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class ThirdPartyCoordinateSecurityDAOTest
    extends AbstractDbDAOTest
{
  private static final Comparator<ThirdPartyCoordinateSecurity> THIRD_PARTY_COORDINATE_SECURITY_COMPARATOR =
      Comparator.comparing(ThirdPartyCoordinateSecurity::getRefId)
          .thenComparing(ThirdPartyCoordinateSecurity::getFileCoordinateId);

  private ThirdPartyVulnerabilityExploitabilityExchangeDAO thirdPartyVulnerabilityExploitabilityExchangeDAO;

  private ThirdPartyCoordinateSecurityDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    thirdPartyVulnerabilityExploitabilityExchangeDAO =
        daoFactory.createThirdPartyVulnerabilityExploitabilityExchangeDAO();
    dao = daoFactory.createThirdPartyCoordinateSecurityDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ThirdPartyFileCoordinate coordinateFile = tempEntity.newThirdPartyFileCoordinate();

    ThirdPartyCoordinateSecurity entity =
        new ThirdPartyCoordinateSecurity(coordinateFile.getId(), "refid", "metadataId", "description",
            "link", 6.8f, null);
    dao.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Get
    ThirdPartyCoordinateSecurity retrievedCoordinateSecurity = dao.getById(entity.getId());
    assertThirdPartyCoordinateSecurity("refid", "metadataId", "description", "link", 6.8f, null, coordinateFile.getId(),
        retrievedCoordinateSecurity);

    // Update
    retrievedCoordinateSecurity.setDescription("Description updated");
    dao.update(retrievedCoordinateSecurity);
    ThirdPartyCoordinateSecurity updated = dao.getById(retrievedCoordinateSecurity.getId());
    assertThat(updated.getDescription()).isEqualTo("Description updated");

    // Delete
    dao.delete(retrievedCoordinateSecurity);
    retrievedCoordinateSecurity = dao.getById(retrievedCoordinateSecurity.getId());
    assertThat(retrievedCoordinateSecurity).isNull();
  }

  @Test
  public void testInsertSafely() {
    ThirdPartyFileCoordinate coordinateFile = tempEntity.newThirdPartyFileCoordinate();

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      ThirdPartyCoordinateSecurity e1 =
          new ThirdPartyCoordinateSecurity(coordinateFile.getId(), "refid", "metadataId", "description",
              "link", 6.8f, null);
      ThirdPartyCoordinateSecurity saved = dao.insertSafely(tx, e1);
      assertThat(saved.getId()).isNotNull();

      ThirdPartyCoordinateSecurity e2 =
          new ThirdPartyCoordinateSecurity(coordinateFile.getId(), "refid", "metadataId", "description2",
              "link2", 7.8f, null);

      saved = dao.insertSafely(tx, e2);
      assertThat(saved.getId()).isEqualTo(e1.getId());
    }
  }

  @Test
  public void testGetByFileCoordinateIdAndRefId() {
    ThirdPartyCoordinateSecurity coordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity();
    ThirdPartyCoordinateSecurity retrievedCoordinateSecurity =
        dao.getByFileCoordinateIdAndRefId(coordinateSecurity.getFileCoordinateId(), coordinateSecurity.getRefId());

    assertThirdPartyCoordinateSecurity(coordinateSecurity.getRefId(), null, coordinateSecurity.getDescription(),
        coordinateSecurity.getLink(), coordinateSecurity.getSeverity(), coordinateSecurity.getFixedBy(),
        coordinateSecurity.getFileCoordinateId(), retrievedCoordinateSecurity);
  }

  @Test
  public void testGetByFileCoordinateIdAndRefId_caseInsensitive() {
    ThirdPartyCoordinateSecurity coordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity();
    ThirdPartyCoordinateSecurity retrievedCoordinateSecurity =
        dao.getByFileCoordinateIdAndRefId(coordinateSecurity.getFileCoordinateId(),
            coordinateSecurity.getRefId().toUpperCase());

    assertThirdPartyCoordinateSecurity(coordinateSecurity.getRefId(), null, coordinateSecurity.getDescription(),
        coordinateSecurity.getLink(), coordinateSecurity.getSeverity(), coordinateSecurity.getFixedBy(),
        coordinateSecurity.getFileCoordinateId(), retrievedCoordinateSecurity);
  }

  @Test
  public void testGetByFileCoordinateIdList() {
    List<ThirdPartyCoordinateSecurity> coordinateSecurityList = newThirdPartyCoordinateSecurityList();
    List<String> listId =
        coordinateSecurityList.stream()
            .map(ThirdPartyCoordinateSecurity::getFileCoordinateId)
            .collect(Collectors.toList());
    List<ThirdPartyCoordinateSecurity> results = dao.getByFileCoordinateIds(listId);

    assertThat(results).usingElementComparator(THIRD_PARTY_COORDINATE_SECURITY_COMPARATOR)
        .containsExactlyInAnyOrderElementsOf(coordinateSecurityList);
  }

  @Test
  public void testGetByFileCoordinateIdList_Batched() {
    List<ThirdPartyCoordinateSecurity> coordinateSecurityList = newThirdPartyCoordinateSecurityList();
    List<String> listId =
        coordinateSecurityList.stream()
            .map(ThirdPartyCoordinateSecurity::getFileCoordinateId)
            .collect(Collectors.toList());
    dao = spy(dao);
    doReturn(2).when(dao).getInOperatorThreshold(ArgumentMatchers.any());
    List<ThirdPartyCoordinateSecurity> results = dao.getByFileCoordinateIds(listId);

    assertThat(results).usingElementComparator(THIRD_PARTY_COORDINATE_SECURITY_COMPARATOR)
        .containsExactlyInAnyOrderElementsOf(coordinateSecurityList);
  }

  @Test
  public void testGetByFileCoordinateId() {
    ThirdPartyFileCoordinate coord =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s1", "f1", "n1", "v1");
    ThirdPartyCoordinateSecurity coordSec1 =
        tempEntity.newThirdPartyCoordinateSecurity(coord, "r1", "d1", "l1", 1.1f, "Low", "f1");
    ThirdPartyCoordinateSecurity coordSec2 =
        tempEntity.newThirdPartyCoordinateSecurity(coord, "r2", "d1", "l2", 1.1f, "Low", "f2");

    final List<ThirdPartyCoordinateSecurity> results = dao.getByFileCoordinateId(coord.getId());
    assertThat(results).usingElementComparator(THIRD_PARTY_COORDINATE_SECURITY_COMPARATOR)
        .containsExactlyInAnyOrderElementsOf(Arrays.asList(coordSec1, coordSec2));
  }

  @Test
  public void testDelete_Cascade() {
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s1", "f1", "n1", "v1");
    ThirdPartyCoordinateSecurity coordSec1 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r1", "d1", "l1", 1.1f, "Low", "f1");
    ThirdPartyCoordinateSecurity coordSec2 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r2", "d1", "l2", 1.1f, "Low", "f2");

    ThirdPartyVulnerabilityExploitabilityExchange vexData =
        tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordSec1, "r1", "state",
            "justification", "response", "detail");
    ThirdPartyVulnerabilityExploitabilityExchange vexData2 =
        tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordSec2, "r2", "state2",
            "justification2", "response2", "detail2");

    dao.delete(coordSec1);

    assertThat(dao.getById(coordSec1.getId())).isNull();
    assertThat(dao.getById(coordSec2.getId())).isNotNull();
    assertThat(thirdPartyVulnerabilityExploitabilityExchangeDAO.getById(vexData.getId())).isNull();
    assertThat(thirdPartyVulnerabilityExploitabilityExchangeDAO.getById(vexData2.getId())).isNotNull();
  }

  @Test
  public void testDeleteByFileCoordinateId() {
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s1", "f1", "n1", "v1");
    ThirdPartyCoordinateSecurity coordSec1 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r1", "d1", "l1", 1.1f, "Low", "f1");
    ThirdPartyCoordinateSecurity coordSec2 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r2", "d1", "l2", 1.1f, "Low", "f2");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByFileCoordinateId(tx, coord1.getId());
      tx.commit();
    }
    assertThat(dao.getById(coordSec1.getId())).isNull();
    assertThat(dao.getById(coordSec2.getId())).isNull();
  }

  private void assertThirdPartyCoordinateSecurity(
      final String refId,
      final String sbomMetadataId,
      final String description,
      final String link,
      final double score,
      final String fixedBy,
      final String cooedinateFileId,
      final ThirdPartyCoordinateSecurity actual)
  {
    assertThat(actual.getRefId()).isEqualTo(refId);
    assertThat(actual.getSbomMetadataId()).isEqualTo(sbomMetadataId);
    assertThat(actual.getDescription()).isEqualTo(description);
    assertThat(actual.getLink()).isEqualTo(link);
    assertThat(actual.getSeverity()).isEqualTo(score);
    assertThat(actual.getFixedBy()).isEqualTo(fixedBy);
    assertThat(actual.getFileCoordinateId()).isEqualTo(cooedinateFileId);
  }

  private List<ThirdPartyCoordinateSecurity> newThirdPartyCoordinateSecurityList() {
    List<ThirdPartyCoordinateSecurity> list = new ArrayList<>();

    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(
        tempEntity.newThirdPartyFileCoordinate(), "r1", "d1", "l1", 5.5f, "Medium", "1.1");
    list.add(thirdPartyCoordinateSecurity1);
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 = tempEntity
        .newThirdPartyCoordinateSecurity(tempEntity.newThirdPartyFileCoordinate(), "r2", "d2", "l2", 1f, "Low", "1.2");
    list.add(thirdPartyCoordinateSecurity2);
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity3 = tempEntity.newThirdPartyCoordinateSecurity(
        tempEntity.newThirdPartyFileCoordinate(), "r3", "d3", "l3", 10f, "Critical", "1.3");
    list.add(thirdPartyCoordinateSecurity3);

    return list;
  }

  private void insertSbomMetadata() {
    Application app = application;

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

    newSbomMetadataBuilder(daoFactory).withCreatedAt(now)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan1.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneYearAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan2.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(sixMonthsAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan3.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoMonthsAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan4.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneMonthAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan5.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneWeekAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan6.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan7.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(yesterday)
        .withStatus(PENDING)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan8.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(twoDaysAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan9.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(fourHoursAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan10.getThirdPartyFileId())
        .build();

    ThirdPartyFileCoordinate thirdPartyFileCoordinate1 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan1.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h1", "u1", ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate2 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan1.getThirdPartyFileId(),
            "s", "SPDX", "n2", "v1", "h2", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate3 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan2.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h3", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate4 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h4", "u1", ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate5 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h5", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate6 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan3.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h6", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate7 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan4.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h7", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate8 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan4.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h8", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate9 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan5.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h9", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate10 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan6.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h10", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate11 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan7.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h11", "u1", ThirdPartyDependencyType.TRANSITIVE);
    ThirdPartyFileCoordinate thirdPartyFileCoordinate12 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyScan8.getThirdPartyFileId(),
            "s", "SPDX", "n1", "v1", "h12", "u1", ThirdPartyDependencyType.TRANSITIVE);
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan9.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h13", "u1", ThirdPartyDependencyType.TRANSITIVE);
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h14", "u1", ThirdPartyDependencyType.TRANSITIVE);
    tempEntity.newThirdPartyFileCoordinate(thirdPartyScan10.getThirdPartyFileId(),
        "s", "SPDX", "n1", "v1", "h15", "u1", ThirdPartyDependencyType.TRANSITIVE);
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
  }

  @Test
  public void testInsertSafelyBatch_mixedNewAndExisting() {
    ThirdPartyFileCoordinate coord = tempEntity.newThirdPartyFileCoordinate();

    ThirdPartyCoordinateSecurity existing =
        new ThirdPartyCoordinateSecurity(coord.getId(), "REF-EXISTING", "sbom-md", "existing desc",
            "existing-link", 5.0f, null);
    dao.insert(existing);

    ThirdPartyCoordinateSecurity duplicateOfExisting =
        new ThirdPartyCoordinateSecurity(coord.getId(), "REF-EXISTING", "sbom-md", "should-be-ignored",
            "should-be-ignored", 9.9f, null);
    ThirdPartyCoordinateSecurity newRow =
        new ThirdPartyCoordinateSecurity(coord.getId(), "REF-NEW", "sbom-md", "new desc",
            "new-link", 7.0f, null);
    ThirdPartyCoordinateSecurity duplicateInInput =
        new ThirdPartyCoordinateSecurity(coord.getId(), "REF-NEW", "sbom-md", "same-as-newRow",
            "same-as-newRow", 7.0f, null);

    List<ThirdPartyCoordinateSecurity> resolved;
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      resolved = dao.insertSafelyBatch(tx,
          Arrays.asList(duplicateOfExisting, newRow, duplicateInInput));
      tx.commit();
    }

    assertThat(resolved).hasSize(3);
    assertThat(resolved.get(0).getId()).isEqualTo(existing.getId());
    assertThat(resolved.get(0).getDescription()).isEqualTo("existing desc");
    assertThat(resolved.get(1).getId()).isNotNull();
    assertThat(resolved.get(1).getRefId()).isEqualTo("REF-NEW");
    assertThat(resolved.get(2).getId()).isEqualTo(resolved.get(1).getId());

    List<ThirdPartyCoordinateSecurity> stored = dao.getByFileCoordinateIds(List.of(coord.getId()));
    assertThat(stored).hasSize(2);
  }

  @Test
  public void testInsertSafelyBatch_matchesExistingRefIdCaseInsensitively() {
    ThirdPartyFileCoordinate coord = tempEntity.newThirdPartyFileCoordinate();

    ThirdPartyCoordinateSecurity existing =
        new ThirdPartyCoordinateSecurity(coord.getId(), "CVE-2024-1234", "sbom-md", "existing",
            "existing-link", 5.0f, null);
    dao.insert(existing);

    ThirdPartyCoordinateSecurity mixedCase =
        new ThirdPartyCoordinateSecurity(coord.getId(), "cve-2024-1234", "sbom-md", "should-be-ignored",
            "should-be-ignored", 9.9f, null);

    List<ThirdPartyCoordinateSecurity> resolved;
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      resolved = dao.insertSafelyBatch(tx, Arrays.asList(mixedCase));
      tx.commit();
    }

    assertThat(resolved).hasSize(1);
    assertThat(resolved.get(0).getId()).isEqualTo(existing.getId());
    assertThat(resolved.get(0).getDescription()).isEqualTo("existing");

    List<ThirdPartyCoordinateSecurity> stored = dao.getByFileCoordinateIds(List.of(coord.getId()));
    assertThat(stored).hasSize(1);
  }

}
