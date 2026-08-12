/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentImportedSbomsDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentVulnerabilitiesDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilitiesThreadLevelMetricDTO;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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

  @Before
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

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetVulnerabilitiesByThreatLevel_NoResults() {
    VulnerabilitiesThreadLevelMetricDTO result =
        dao.getVulnerabilitiesByThreatLevel(Collections.singleton(application.getId()));
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

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetVulnerabilitiesByThreatLevel_WithResults() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s1", "f1", "n1", "v1", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
        "r1", sbomMetadata.getId(), "d1", "l1", CvssV3Severity.LOW.getStartScoreRange(),
        CvssV3Severity.LOW.getDisplayName(), "f1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity1, coordinateSecurity1.getRefId(),
        "state", "justification", "response", "detail");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "r2", sbomMetadata.getId(), "d2", "l2",
        CvssV3Severity.LOW.getEndScoreRange(),
        CvssV3Severity.LOW.getDisplayName(), "f2");

    ThirdPartyFileCoordinate coordinate2 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s2", "f2", "n2", "v2", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity3 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2,
        "r3", sbomMetadata.getId(), "d3", "l3", CvssV3Severity.MEDIUM.getStartScoreRange(),
        CvssV3Severity.MEDIUM.getDisplayName(), "f3");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity3, coordinateSecurity3.getRefId(),
        "state", "justification", "response", "detail");

    ThirdPartyCoordinateSecurity coordinateSecurity4 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2,
        "r4", sbomMetadata.getId(), "d4", "l4", CvssV3Severity.HIGH.getEndScoreRange(),
        CvssV3Severity.HIGH.getDisplayName(), "f4");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity4, coordinateSecurity4.getRefId(),
        "state", "justification", "response", "detail");

    ThirdPartyCoordinateSecurity coordinateSecurity5 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2,
        "r5", sbomMetadata.getId(), "d5", "l5", CvssV3Severity.HIGH.getEndScoreRange(),
        CvssV3Severity.HIGH.getDisplayName(), "f5");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity5, coordinateSecurity5.getRefId(),
        "state", "justification", "response", "detail");

    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "r6", sbomMetadata.getId(), "d6",
        "l6", CvssV3Severity.HIGH.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "f6");

    ThirdPartyFileCoordinate coordinate3 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s3", "f3",
            "n3", "v3", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity7 = tempEntity.newThirdPartyCoordinateSecurity(coordinate3,
        "r7", sbomMetadata.getId(), "d7", "l7", CvssV3Severity.CRITICAL.getStartScoreRange(),
        CvssV3Severity.CRITICAL.getDisplayName(), "f7");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity7, coordinateSecurity7.getRefId(),
        "state", "justification", "response", "detail");

    tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r8", sbomMetadata.getId(), "d8", "l8",
        CvssV3Severity.CRITICAL.getStartScoreRange(), CvssV3Severity.CRITICAL.getDisplayName(), "f8");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r9", sbomMetadata.getId(), "d9", "l9",
        CvssV3Severity.CRITICAL.getStartScoreRange(), CvssV3Severity.CRITICAL.getDisplayName(), "f9");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r10", sbomMetadata.getId(), "d10", "l10",
        CvssV3Severity.CRITICAL.getStartScoreRange(), CvssV3Severity.CRITICAL.getDisplayName(), "f10");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r11", sbomMetadata.getId(), "d11", "l11",
        CvssV3Severity.CRITICAL.getStartScoreRange(), CvssV3Severity.CRITICAL.getDisplayName(), "f11");

    // Should not have any impact on counters as NONE is not included in the query
    tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r12", sbomMetadata.getId(), "d12", "l12",
        CvssV3Severity.NONE.getStartScoreRange(), CvssV3Severity.NONE.getDisplayName(), "f12");
    ThirdPartyCoordinateSecurity coordinateSecurity13 = tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r13",
        sbomMetadata.getId(), "d13", "l13", CvssV3Severity.NONE.getStartScoreRange(),
        CvssV3Severity.NONE.getDisplayName(), "f13");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity13, coordinateSecurity13.getRefId(),
        "state", "justification", "response", "detail");

    // This new application will not be part of the query so its SBOM data shouldn't affect the results
    ThirdPartySbomMetadata sbomMetadataFromOtherApplication = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(tempEntity.newApplicationWithParent().getId())
        .build();

    ThirdPartyFileCoordinate coordinate4 = tempEntity.newThirdPartyFileCoordinate(
        sbomMetadataFromOtherApplication.getThirdPartyFileId(), "s4", "f4", "n4", "v4", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity14 = tempEntity.newThirdPartyCoordinateSecurity(coordinate4,
        "r14", sbomMetadataFromOtherApplication.getId(), "d14", "l14",
        CvssV3Severity.LOW.getStartScoreRange(), CvssV3Severity.LOW.getDisplayName(), "f14");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity14, coordinateSecurity14.getRefId(),
        "state", "justification", "response", "detail");

    // Check that we can send a large number of application IDs
    Set<String> applicationIds = IntStream.range(1, 1_000_000)
        .boxed()
        .map(String::valueOf)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    applicationIds.add(application.getId());

    VulnerabilitiesThreadLevelMetricDTO result = dao.getVulnerabilitiesByThreatLevel(applicationIds);
    assertThat(result).isNotNull();

    assertThat(result.getLow()).isEqualTo(2);
    assertThat(result.getLowAnnotated()).isOne();
    assertThat(result.getLowUnannotated()).isOne();

    assertThat(result.getMedium()).isOne();
    assertThat(result.getMediumAnnotated()).isOne();
    assertThat(result.getMediumUnannotated()).isZero();

    assertThat(result.getHigh()).isEqualTo(3);
    assertThat(result.getHighAnnotated()).isEqualTo(2);
    assertThat(result.getHighUnannotated()).isOne();

    assertThat(result.getCritical()).isEqualTo(5);
    assertThat(result.getCriticalAnnotated()).isOne();
    assertThat(result.getCriticalUnannotated()).isEqualTo(4);

    assertThat(result.getTotalVulnerabilities()).isEqualTo(11);
    assertThat(result.getTotalVulnerabilitiesAnnotated()).isEqualTo(5);
    assertThat(result.getTotalVulnerabilitiesUnannotated()).isEqualTo(6);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetVulnerabilitiesByThreatLevel_WithoutFilteringByApplications() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s1", "f1", "n1", "v1", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1,
        "r1", sbomMetadata.getId(), "d1", "l1", CvssV3Severity.LOW.getStartScoreRange(),
        CvssV3Severity.LOW.getDisplayName(), "f1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity1, coordinateSecurity1.getRefId(),
        "state", "justification", "response", "detail");

    ThirdPartySbomMetadata sbomMetadata2 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(tempEntity.newApplicationWithParent().getId())
        .build();

    ThirdPartyFileCoordinate coordinate2 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata2.getThirdPartyFileId(), "s2", "f2", "n2", "v2", "", "");

    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "r2", sbomMetadata2.getId(), "d2", "l2",
        CvssV3Severity.CRITICAL.getStartScoreRange(), CvssV3Severity.CRITICAL.getDisplayName(), "f2");

    VulnerabilitiesThreadLevelMetricDTO result = dao.getVulnerabilitiesByThreatLevel(null);
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

    assertThat(result.getCritical()).isOne();
    assertThat(result.getCriticalAnnotated()).isZero();
    assertThat(result.getCriticalUnannotated()).isOne();

    assertThat(result.getTotalVulnerabilities()).isEqualTo(2);
    assertThat(result.getTotalVulnerabilitiesAnnotated()).isOne();
    assertThat(result.getTotalVulnerabilitiesUnannotated()).isOne();

    assertThat(dao.getVulnerabilitiesByThreatLevel(Collections.emptySet()))
        .usingRecursiveComparison()
        .isEqualTo(result);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetVulnerabilitiesByThreatLevel_OnlyActiveSboms() {
    CvssV3Severity severity = CvssV3Severity.LOW;

    ThirdPartySbomMetadata sbomMetadataActive = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ThirdPartyFileCoordinate coordinate1 = tempEntity
        .newThirdPartyFileCoordinate(sbomMetadataActive.getThirdPartyFileId(), "s1", "f1", "n1", "v1", "", "");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "r1", sbomMetadataActive.getId(), "d1",
        "l1", severity.getStartScoreRange(), severity.getDisplayName(), "f1");

    ThirdPartySbomMetadata sbomMetadataPending = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withStatus(PENDING)
        .build();

    ThirdPartyFileCoordinate coordinate2 = tempEntity
        .newThirdPartyFileCoordinate(sbomMetadataPending.getThirdPartyFileId(), "s2", "f2", "n2", "v2", "", "");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "r2", sbomMetadataPending.getId(), "d2",
        "l2", severity.getStartScoreRange(), severity.getDisplayName(), "f2");

    VulnerabilitiesThreadLevelMetricDTO result = dao.getVulnerabilitiesByThreatLevel(null);
    assertThat(result).isNotNull();

    assertThat(result.getLow()).isOne();
    assertThat(result.getLowAnnotated()).isZero();
    assertThat(result.getLowUnannotated()).isOne();
    assertThat(result.getTotalVulnerabilities()).isOne();
    assertThat(result.getTotalVulnerabilitiesAnnotated()).isZero();
    assertThat(result.getTotalVulnerabilitiesUnannotated()).isOne();
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

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRecentHighPriorityVulnerabilities_NoResults() {
    List<RecentVulnerabilitiesDTO> result =
        dao.getRecentHighPriorityVulnerabilities(Collections.singleton(application.getId()));
    assertThat(result).isNotNull();
    assertThat(result).hasSize(0);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRecentHighPriorityVulnerabilities() {
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

    // Check that we can send a large number of application IDs
    Set<String> applicationIds = IntStream.range(1, 1_000_000)
        .boxed()
        .map(String::valueOf)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    applicationIds.add(application.getId());
    List<RecentVulnerabilitiesDTO> result = dao.getRecentHighPriorityVulnerabilities(applicationIds);

    assertThat(result.get(0).getRefId()).isEqualTo("r5");
    assertThat(result.get(0).getSeverity()).isEqualTo(7.9);
    assertThat(result.get(0).getSeverityStatus()).isEqualTo("high");
    assertThat(result.get(0).getCreatedAt().getTime()).isEqualTo(now.getTime());

    assertThat(result.get(1).getRefId()).isEqualTo("r2");
    assertThat(result.get(1).getSeverity()).isEqualTo(7.5);
    assertThat(result.get(1).getSeverityStatus()).isEqualTo("high");
    assertThat(result.get(1).getCreatedAt().getTime()).isEqualTo(now.getTime());

    assertThat(result.get(2).getRefId()).isEqualTo("r21");
    assertThat(result.get(2).getSeverity()).isEqualTo(7.2);
    assertThat(result.get(2).getSeverityStatus()).isEqualTo("high");
    assertThat(result.get(2).getCreatedAt().getTime()).isEqualTo(yesterday.getTime());

    assertThat(result.get(3).getRefId()).isEqualTo("r12");
    assertThat(result.get(3).getSeverity()).isEqualTo(10.0);
    assertThat(result.get(3).getSeverityStatus()).isEqualTo("critical");
    assertThat(result.get(3).getCreatedAt().getTime()).isEqualTo(sixMonthsAgo.getTime());

    assertThat(result.get(4).getRefId()).isEqualTo("r11");
    assertThat(result.get(4).getSeverity()).isEqualTo(8.1);
    assertThat(result.get(4).getSeverityStatus()).isEqualTo("high");
    assertThat(result.get(4).getCreatedAt().getTime()).isEqualTo(sixMonthsAgo.getTime());

    assertThat(result.get(5).getRefId()).isEqualTo("r8");
    assertThat(result.get(5).getSeverity()).isEqualTo(9.5);
    assertThat(result.get(5).getSeverityStatus()).isEqualTo("critical");
    assertThat(result.get(5).getCreatedAt().getTime()).isEqualTo(oneYearAgo.getTime());
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRecentHighPriorityVulnerabilities_Duplicates() {
    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();

    ThirdPartyScan app1Scan1 = tempEntity.newThirdPartyScan();
    ThirdPartyScan app1Scan2 = tempEntity.newThirdPartyScan();
    ThirdPartyScan app2Scan1 = tempEntity.newThirdPartyScan();
    ThirdPartyScan app2Scan2 = tempEntity.newThirdPartyScan();

    Date now = new Date();
    Date oneDayAgo = DateUtils.addDays(now, -1);

    newSbomMetadataBuilder(daoFactory).withCreatedAt(now)
        .withApplicationId(app1.getId())
        .withThirdPartyFileId(app1Scan1.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneDayAgo)
        .withApplicationId(app1.getId())
        .withThirdPartyFileId(app1Scan2.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(now)
        .withApplicationId(app2.getId())
        .withThirdPartyFileId(app2Scan1.getThirdPartyFileId())
        .build();
    newSbomMetadataBuilder(daoFactory).withCreatedAt(oneDayAgo)
        .withApplicationId(app2.getId())
        .withThirdPartyFileId(app2Scan2.getThirdPartyFileId())
        .build();

    // Each SBOM scan had the same component
    ThirdPartyFileCoordinate app1Scan1Component =
        tempEntity.newThirdPartyFileCoordinate(app1Scan1.getThirdPartyFileId(), "s", "SPDX", "n1", "v1", "h1",
            "u1", ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate app1Scan2Component =
        tempEntity.newThirdPartyFileCoordinate(app1Scan2.getThirdPartyFileId(), "s", "SPDX", "n1", "v1", "h1",
            "u1", ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate app2Scan1Component =
        tempEntity.newThirdPartyFileCoordinate(app2Scan2.getThirdPartyFileId(), "s", "SPDX", "n1", "v1", "h1",
            "u1", ThirdPartyDependencyType.DIRECT);
    ThirdPartyFileCoordinate app2Scan2Component =
        tempEntity.newThirdPartyFileCoordinate(app2Scan2.getThirdPartyFileId(), "s", "SPDX", "n1", "v1", "h1",
            "u1", ThirdPartyDependencyType.DIRECT);

    // This component had the same vulnerability
    tempEntity.newThirdPartyCoordinateSecurity(app1Scan1Component, "r1", "d1", "l1", 7.0, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(app1Scan2Component, "r1", "d1", "l1", 7.0, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(app2Scan1Component, "r1", "d1", "l1", 7.0, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(app2Scan2Component, "r1", "d1", "l1", 7.0, "sd1", "f1");

    List<RecentVulnerabilitiesDTO> result =
        dao.getRecentHighPriorityVulnerabilities(new HashSet<>(Arrays.asList(app1.getId(), app2.getId())));

    // Results should return distinct vulnerabilities
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getRefId()).isEqualTo("r1");
    assertThat(result.get(0).getSeverity()).isEqualTo(7.0);
    assertThat(result.get(0).getSeverityStatus()).isEqualTo("high");
    assertThat(result.get(0).getCreatedAt().getTime()).isEqualTo(now.getTime());
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetReleaseStatus() {
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

    ThirdPartySbomMetadata thirdPartySbomMetadata1 = newSbomMetadataBuilder(daoFactory)
        .withCreatedAt(now)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan1.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata2 = newSbomMetadataBuilder(daoFactory)
        .withCreatedAt(oneYearAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan2.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata3 = newSbomMetadataBuilder(daoFactory)
        .withCreatedAt(sixMonthsAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan3.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata4 = newSbomMetadataBuilder(daoFactory)
        .withCreatedAt(twoMonthsAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan4.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata5 = newSbomMetadataBuilder(daoFactory)
        .withCreatedAt(oneMonthAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan5.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata6 = newSbomMetadataBuilder(daoFactory)
        .withCreatedAt(oneWeekAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan6.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata7 = newSbomMetadataBuilder(daoFactory)
        .withCreatedAt(yesterday)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan7.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata8 = newSbomMetadataBuilder(daoFactory)
        .withCreatedAt(yesterday)
        .withStatus(PENDING)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan8.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata9 = newSbomMetadataBuilder(daoFactory)
        .withCreatedAt(twoDaysAgo)
        .withApplicationId(app.getId())
        .withThirdPartyFileId(thirdPartyScan9.getThirdPartyFileId())
        .build();
    ThirdPartySbomMetadata thirdPartySbomMetadata10 = newSbomMetadataBuilder(daoFactory)
        .withCreatedAt(fourHoursAgo)
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
        "r1", thirdPartySbomMetadata1.getId(), "d1", "l1", 5.5, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
        "r2", thirdPartySbomMetadata1.getId(), "d2", "l2", 7.5, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate1,
        "r3", thirdPartySbomMetadata2.getId(), "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
        "r4", thirdPartySbomMetadata2.getId(), "d1", "l1", 5.5, "sd1", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
        "r5", thirdPartySbomMetadata3.getId(), "d2", "l2", 7.9, "sd2", "f1");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate2,
        "r6", thirdPartySbomMetadata3.getId(), "d3", "l3", 3.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
        "r7", thirdPartySbomMetadata4.getId(), "d3", "l3", 1.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
        "r8", thirdPartySbomMetadata5.getId(), "d3", "l3", 9.5, "sd4", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
        "r9", thirdPartySbomMetadata5.getId(), "d3", "l3", 2.5, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate3,
        "r10", thirdPartySbomMetadata6.getId(), "d3", "l3", 5.1, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
        "r11", thirdPartySbomMetadata6.getId(), "d3", "l3", 8.1, "sd3", "f3");
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity =
        tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate4,
            "r12", thirdPartySbomMetadata7.getId(), "d3", "l3", 10.0, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate5,
        "r13", thirdPartySbomMetadata7.getId(), "d3", "l3", 9.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate6,
        "r14", thirdPartySbomMetadata8.getId(), "d3", "l3", 1.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate7,
        "r15", thirdPartySbomMetadata8.getId(), "d3", "l3", 0.1, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate7,
        "r16", thirdPartySbomMetadata8.getId(), "d3", "l3", 0, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate8,
        "r17", thirdPartySbomMetadata9.getId(), "d3", "l3", 3.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate9,
        "r18", thirdPartySbomMetadata9.getId(), "d3", "l3", 4.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate10,
        "r19", thirdPartySbomMetadata9.getId(), "d3", "l3", 5.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate11,
        "r20", thirdPartySbomMetadata10.getId(), "d3", "l3", 6.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate11,
        "r21", thirdPartySbomMetadata10.getId(), "d3", "l3", 7.2, "sd3", "f3");
    tempEntity.newThirdPartyCoordinateSecurity(thirdPartyFileCoordinate12,
        "r22", thirdPartySbomMetadata10.getId(), "d3", "l3", 8.2, "sd3", "f3");

    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(thirdPartyCoordinateSecurity, "r1", "ACTIVE",
        "J1", "r1", "d1");

    // Check that we can send a large number of application IDs
    Set<String> applicationIds = IntStream.range(1, 1_000_000)
        .boxed()
        .map(String::valueOf)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    applicationIds.add(application.getId());

    long releaseReadyCount = dao.getSbomReleaseStatusReleaseReady(applicationIds);
    long needsAttentionCount = dao.getSbomReleaseStatusNeedsAttention(applicationIds);
    long partiallyReadyCount = dao.getSbomReleaseStatusPartiallyReady(applicationIds);

    assertThat(releaseReadyCount).isEqualTo(3);
    assertThat(needsAttentionCount).isEqualTo(6);
    assertThat(partiallyReadyCount).isEqualTo(1);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRecentImportedSboms() {
    insertSbomMetadata();
    // Check that we can send a large number of application IDs
    Set<String> applicationIds = IntStream.range(1, 1_000_000)
        .boxed()
        .map(String::valueOf)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    applicationIds.add(application.getId());
    List<RecentImportedSbomsDTO> recentImportedSboms = dao.getRecentImportedSboms(applicationIds);
    assertThat(recentImportedSboms).hasSize(7);

    Instant today = Instant.now().truncatedTo(ChronoUnit.DAYS);
    Instant twoDaysAgoT = today.minus(1, ChronoUnit.DAYS);

    assertThat(recentImportedSboms.get(0).getImportDate().toInstant().truncatedTo(ChronoUnit.DAYS)).isEqualTo(today);
    assertThat(recentImportedSboms.get(2).getImportDate().toInstant().truncatedTo(ChronoUnit.DAYS))
        .isEqualTo(twoDaysAgoT);
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testGetRecentImportedSboms_TestSbomsWithoutComponents() {
    insertSbomMetadata();
    Application tempApp = tempEntity.newApplicationWithParent();
    ThirdPartyScan thirdPartyScan1 = tempEntity.newThirdPartyScan();

    ThirdPartySbomMetadata sbomMetadata = newSbomMetadataBuilder(daoFactory)
        .withCreatedAt(new Date())
        .withApplicationId(tempApp.getId())
        .withThirdPartyFileId(thirdPartyScan1.getThirdPartyFileId())
        .build();
    Set<String> applicationIds = new HashSet<>();
    applicationIds.add(application.getId());
    List<RecentImportedSbomsDTO> recentImportedSboms = dao.getRecentImportedSboms(applicationIds);
    assertThat(recentImportedSboms).hasSize(7);

    assertThat(recentImportedSboms.stream()
        .noneMatch(
            sbom -> sbom.getSbomVersion().equals(sbomMetadata.getSbomVersion()))).isTrue();
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
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testInsertSafelyBatch_mixedNewAndExisting_postgres() {
    testInsertSafelyBatch_mixedNewAndExisting();
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

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testInsertSafelyBatch_matchesExistingRefIdCaseInsensitively_postgres() {
    testInsertSafelyBatch_matchesExistingRefIdCaseInsensitively();
  }
}
