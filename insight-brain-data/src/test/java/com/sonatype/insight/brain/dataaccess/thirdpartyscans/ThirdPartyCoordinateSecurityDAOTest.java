/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilitiesThreadLevelMetricDTO;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        new ThirdPartyCoordinateSecurity(coordinateFile.getId(), "refid", "description",
            "link", 6.8f, null);
    dao.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Get
    ThirdPartyCoordinateSecurity retrievedCoordinateSecurity = dao.getById(entity.getId());
    assertThirdPartyCoordinateSecurity("refid", "description", "link", 6.8f, null, coordinateFile.getId(),
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
  public void testGetByCoordinateFileIdAndRefId() {
    ThirdPartyCoordinateSecurity coordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity();
    ThirdPartyCoordinateSecurity retrievedCoordinateSecurity =
        dao.getByCoordinateFileIdAndRefId(coordinateSecurity.getFileCoordinateId(), coordinateSecurity.getRefId());

    assertThirdPartyCoordinateSecurity(coordinateSecurity.getRefId(), coordinateSecurity.getDescription(),
        coordinateSecurity.getLink(), coordinateSecurity.getSeverity(), coordinateSecurity.getFixedBy(),
        coordinateSecurity.getFileCoordinateId(), retrievedCoordinateSecurity);
  }

  @Test
  public void testGetByCoordinateFileIdAndRefId_caseInsensitive() {
    ThirdPartyCoordinateSecurity coordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity();
    ThirdPartyCoordinateSecurity retrievedCoordinateSecurity =
        dao.getByCoordinateFileIdAndRefId(coordinateSecurity.getFileCoordinateId(),
            coordinateSecurity.getRefId().toUpperCase());

    assertThirdPartyCoordinateSecurity(coordinateSecurity.getRefId(), coordinateSecurity.getDescription(),
        coordinateSecurity.getLink(), coordinateSecurity.getSeverity(), coordinateSecurity.getFixedBy(),
        coordinateSecurity.getFileCoordinateId(), retrievedCoordinateSecurity);
  }

  @Test
  public void testGetByFileCoordinateIdList() {
    List<ThirdPartyCoordinateSecurity> coordinateSecurityList = newThirdPartyCoordinateSecurityList();
    List<String> listId =
        coordinateSecurityList.stream().map(ThirdPartyCoordinateSecurity::getFileCoordinateId)
            .collect(Collectors.toList());
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
  @PostgresTest
  public void testGetVulnerabilitiesByThreatLevel_WithResults() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s1", "f1", "n1", "v1", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "r1",
        "d1", "l1", CvssV3Severity.LOW.getStartScoreRange(), CvssV3Severity.LOW.getDisplayName(), "f1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity1, coordinateSecurity1.getRefId(),
        "state", "justification", "response", "detail");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "r2", "d2", "l2", CvssV3Severity.LOW.getEndScoreRange(),
        CvssV3Severity.LOW.getDisplayName(), "f2");

    ThirdPartyFileCoordinate coordinate2 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s2", "f2", "n2", "v2", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity3 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "r3",
        "d3", "l3", CvssV3Severity.MEDIUM.getStartScoreRange(), CvssV3Severity.MEDIUM.getDisplayName(), "f3");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity3, coordinateSecurity3.getRefId(),
        "state", "justification", "response", "detail");

    ThirdPartyCoordinateSecurity coordinateSecurity4 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "r4",
        "d4", "l4", CvssV3Severity.HIGH.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "f4");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity4, coordinateSecurity4.getRefId(),
        "state", "justification", "response", "detail");

    ThirdPartyCoordinateSecurity coordinateSecurity5 = tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "r5",
        "d5", "l5", CvssV3Severity.HIGH.getEndScoreRange(), CvssV3Severity.HIGH.getDisplayName(), "f5");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity5, coordinateSecurity5.getRefId(),
        "state", "justification", "response", "detail");

    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "r6", "d6", "l6", CvssV3Severity.HIGH.getEndScoreRange(),
        CvssV3Severity.HIGH.getDisplayName(), "f6");

    ThirdPartyFileCoordinate coordinate3 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s3", "f3", "n3", "v3", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity7 = tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r7",
        "d7", "l7", CvssV3Severity.CRITICAL.getStartScoreRange(), CvssV3Severity.CRITICAL.getDisplayName(), "f7");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity7, coordinateSecurity7.getRefId(),
        "state", "justification", "response", "detail");

    tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r8", "d8", "l8",
        CvssV3Severity.CRITICAL.getStartScoreRange(), CvssV3Severity.CRITICAL.getDisplayName(), "f8");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r9", "d9", "l9",
        CvssV3Severity.CRITICAL.getStartScoreRange(), CvssV3Severity.CRITICAL.getDisplayName(), "f9");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r10", "d10", "l10",
        CvssV3Severity.CRITICAL.getStartScoreRange(), CvssV3Severity.CRITICAL.getDisplayName(), "f10");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r11", "d11", "l11",
        CvssV3Severity.CRITICAL.getStartScoreRange(), CvssV3Severity.CRITICAL.getDisplayName(), "f11");

    // Should not have any impact on counters as NONE is not included in the query
    tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r12", "d12", "l12",
        CvssV3Severity.NONE.getStartScoreRange(), CvssV3Severity.NONE.getDisplayName(), "f12");
    ThirdPartyCoordinateSecurity coordinateSecurity13 = tempEntity.newThirdPartyCoordinateSecurity(coordinate3, "r13",
        "d13", "l13", CvssV3Severity.NONE.getStartScoreRange(), CvssV3Severity.NONE.getDisplayName(), "f13");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity13, coordinateSecurity13.getRefId(),
        "state", "justification", "response", "detail");

    // This new application will not be part of the query so its SBOM data shouldn't affect the results
    ThirdPartySbomMetadata sbomMetadataFromOtherApplication = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(tempEntity.newApplicationWithParent().getId())
        .build();

    ThirdPartyFileCoordinate coordinate4 = tempEntity.newThirdPartyFileCoordinate(
        sbomMetadataFromOtherApplication.getThirdPartyFileId(), "s4", "f4", "n4", "v4", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity14 = tempEntity.newThirdPartyCoordinateSecurity(coordinate4, "r14",
        "d14", "l14", CvssV3Severity.LOW.getStartScoreRange(), CvssV3Severity.LOW.getDisplayName(), "f14");
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
  @PostgresTest
  public void testGetVulnerabilitiesByThreatLevel_WithoutFilteringByApplications() {
    ThirdPartySbomMetadata sbomMetadata = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ThirdPartyFileCoordinate coordinate1 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata.getThirdPartyFileId(), "s1", "f1", "n1", "v1", "", "");

    ThirdPartyCoordinateSecurity coordinateSecurity1 = tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "r1",
        "d1", "l1", CvssV3Severity.LOW.getStartScoreRange(), CvssV3Severity.LOW.getDisplayName(), "f1");
    tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(coordinateSecurity1, coordinateSecurity1.getRefId(),
        "state", "justification", "response", "detail");

    ThirdPartySbomMetadata sbomMetadata2 = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(tempEntity.newApplicationWithParent().getId())
        .build();

    ThirdPartyFileCoordinate coordinate2 =
        tempEntity.newThirdPartyFileCoordinate(sbomMetadata2.getThirdPartyFileId(), "s2", "f2", "n2", "v2", "", "");

    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "r2", "d2", "l2",
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

  private void assertThirdPartyCoordinateSecurity(
      final String refId,
      final String description,
      final String link,
      final double score,
      final String fixedBy,
      final String cooedinateFileId, final ThirdPartyCoordinateSecurity actual)
  {
    assertThat(actual.getRefId()).isEqualTo(refId);
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
}
