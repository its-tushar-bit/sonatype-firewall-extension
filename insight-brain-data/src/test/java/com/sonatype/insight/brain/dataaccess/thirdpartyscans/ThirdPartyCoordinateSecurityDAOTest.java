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
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.RecentVulnerabilitiesDTO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.thirdpartyscans.VulnerabilitiesThreadLevelMetricDTO;
import com.sonatype.insight.brain.utils.CvssV3Severity;
import com.sonatype.insight.brain.utils.SbomMetadataBuilder;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.utils.SbomMetadataBuilder.newSbomMetadataBuilder;
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

  @Test
  @PostgresTest
  public void testGetVulnerabilitiesByThreatLevel_OnlyActiveSboms() {
    CvssV3Severity severity = CvssV3Severity.LOW;

    ThirdPartySbomMetadata sbomMetadataActive = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .build();

    ThirdPartyFileCoordinate coordinate1 = tempEntity
        .newThirdPartyFileCoordinate(sbomMetadataActive.getThirdPartyFileId(), "s1", "f1", "n1", "v1", "", "");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate1, "r1", "d1", "l1", severity.getStartScoreRange(),
        severity.getDisplayName(), "f1");

    ThirdPartySbomMetadata sbomMetadataPending = SbomMetadataBuilder.newSbomMetadataBuilder(daoFactory)
        .withApplicationId(application.getId())
        .withStatus("PENDING")
        .build();

    ThirdPartyFileCoordinate coordinate2 = tempEntity
        .newThirdPartyFileCoordinate(sbomMetadataPending.getThirdPartyFileId(), "s2", "f2", "n2", "v2", "", "");
    tempEntity.newThirdPartyCoordinateSecurity(coordinate2, "r2", "d2", "l2", severity.getStartScoreRange(),
        severity.getDisplayName(), "f2");

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

  @Test
  @PostgresTest
  public void testGetRecentHighPriorityVulnerabilities_NoResults() {
    List<RecentVulnerabilitiesDTO> result =
        dao.getRecentHighPriorityVulnerabilities(Collections.singleton(application.getId()));
    assertThat(result).isNotNull();
    assertThat(result).hasSize(0);
  }

  @Test
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
}
