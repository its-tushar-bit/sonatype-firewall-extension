/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.api.experimental.sast.SastScanRequestDTO.SastFindingRequestDTO;
import com.sonatype.insight.brain.api.experimental.sast.SastScanRequestDTO.SastRemediationRequestDTO;
import com.sonatype.insight.brain.api.experimental.sast.SastScanResponseDTO.SastFindingResponseDTO;
import com.sonatype.insight.brain.api.experimental.sast.SastScanResponseDTO.SastRemediationResponseDTO;
import com.sonatype.insight.brain.api.experimental.sast.SastScanResponseDTO.SastScmScanContextResponseDTO;
import com.sonatype.insight.brain.dataaccess.sast.SastFindingDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastRemediationDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastScanDAO;
import com.sonatype.insight.brain.model.sast.SastFindingSeverity;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastFindingConfidence;
import com.sonatype.insight.brain.model.sast.SastRemediation;
import com.sonatype.insight.brain.model.sast.SastScan;

import com.google.common.collect.ImmutableMap;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

class SastTestUtil
{
  private final SastScanDAO sastScanDAO;

  private final SastFindingDAO sastFindingDAO;

  private final SastRemediationDAO sastRemediationDAO;

  public SastTestUtil(
      final SastScanDAO sastScanDAO,
      final SastFindingDAO sastFindingDAO,
      final SastRemediationDAO sastRemediationDAO)
  {
    this.sastScanDAO = sastScanDAO;
    this.sastFindingDAO = sastFindingDAO;
    this.sastRemediationDAO = sastRemediationDAO;
  }

  static SastScanRequestDTO buildTestSastScanRequestDTOWith2Findings() {
    final SastScanRequestDTO sastScanRequestDTO = buildTestSastScanRequestDTO();
    final SastScmContextDTO scmContext = new SastScmContextDTO();
    scmContext.branchName = "testBranchName";
    scmContext.commitHash = "testCommitHash";
    sastScanRequestDTO.scmContext = scmContext;

    sastScanRequestDTO.findings.get(0).severity = "Low";
    final SastFindingRequestDTO sastFindingRequestDTO = new SastFindingRequestDTO();
    sastFindingRequestDTO.severity = "High";
    sastFindingRequestDTO.ruleName = "myRuleName2";
    sastFindingRequestDTO.lineNumber = null;
    sastFindingRequestDTO.cwe = "myCwe2";
    sastFindingRequestDTO.confidence = "LOW";
    sastFindingRequestDTO.description = "myDescription2";
    sastFindingRequestDTO.coordinate = ImmutableMap.of("myCoordinateKey2", "myCoordinateValue2");
    sastScanRequestDTO.findings.add(sastFindingRequestDTO);
    return sastScanRequestDTO;
  }

  static SastScanRequestDTO buildTestSastScanRequestDTO() {
    final SastScanRequestDTO sastScanRequestDTO = new SastScanRequestDTO();
    final SastFindingRequestDTO sastFindingRequestDTO = new SastFindingRequestDTO();
    final SastRemediationRequestDTO sastRemediationRequestDTO = new SastRemediationRequestDTO();

    sastScanRequestDTO.findings = newArrayList(sastFindingRequestDTO);

    final SastScmContextDTO scmContext = new SastScmContextDTO();
    scmContext.branchName = "testBranchName";
    scmContext.commitHash = "testCommitHash";
    sastScanRequestDTO.scmContext = scmContext;

    sastFindingRequestDTO.remediations = newArrayList(sastRemediationRequestDTO);

    sastFindingRequestDTO.ruleName = "myRuleName";
    sastFindingRequestDTO.cwe = "myCwe";
    sastFindingRequestDTO.coordinate = ImmutableMap.of("myCoordinateKey", "myCoordinateValue");
    sastFindingRequestDTO.description = "myDescription";
    sastFindingRequestDTO.confidence = "HIGH";
    sastFindingRequestDTO.severity = "Critical";
    sastFindingRequestDTO.lineNumber = 1970;

    sastRemediationRequestDTO.content = "myContent";
    return sastScanRequestDTO;
  }

  /**
   * Asserts the sastScanResponseDTO and database contain the expected data
   */
  void assertSastScan(final String expectedApplicationId, final SastScanResponseDTO sastScanResponseDTO) {
    assertSastScanDTO(sastScanResponseDTO);
    assertSastScanEntity(expectedApplicationId, sastScanResponseDTO);
  }

  void assertSastScanWithScmContext(final String expectedApplicationId, final SastScanResponseDTO sastScanResponseDTO) {
    assertSastScanDTOWithScmContext(sastScanResponseDTO);
    assertSastScanEntity(expectedApplicationId, sastScanResponseDTO);
  }

  /**
   * Asserts the database contains the expected data for a sast scan
   */
  void assertSastScanEntity(final String expectedApplicationId, final SastScanResponseDTO sastScanResponseDTO) {
    final String expectedSastScanId = sastScanResponseDTO.sastScanId;
    final Date expectedCreatedAt = sastScanResponseDTO.createdAt;
    final String expectedSastFindingId = sastScanResponseDTO.findings.get(0).sastFindingId;
    final String expectedSastRemediationId = sastScanResponseDTO.findings.get(0).remediations.get(0).sastRemediationId;

    final SastScan sastScan = sastScanDAO.getByIdNotNull(expectedSastScanId);
    assertThat(sastScan.getApplicationId()).isEqualTo(expectedApplicationId);
    assertThat(sastScan.getId()).isEqualTo(expectedSastScanId);
    assertThat(sastScan.getCreatedAt()).isEqualTo(expectedCreatedAt);

    final List<SastFinding> sastFindings = sastFindingDAO.getBySastScanIdOrderBySeverityDesc(expectedSastScanId);
    assertThat(sastFindings).hasSize(1);
    final SastFinding sastFinding = sastFindings.get(0);
    assertThat(sastFinding.getId()).isEqualTo(expectedSastFindingId);
    assertThat(sastFinding.getSastScanId()).isEqualTo(expectedSastScanId);
    assertThat(sastFinding.getSeverity()).isEqualTo(SastFindingSeverity.CRITICAL);
    assertThat(sastFinding.getConfidenceEnum()).isEqualTo(SastFindingConfidence.HIGH);
    assertThat(sastFinding.getRuleName()).isEqualTo("myRuleName");
    assertThat(sastFinding.getCoordinate()).isEqualTo("{\"myCoordinateKey\":\"myCoordinateValue\"}");
    assertThat(sastFinding.getCwe()).isEqualTo("myCwe");
    assertThat(sastFinding.getDescription()).isEqualTo("myDescription");
    assertThat(sastFinding.getLineNumber()).isEqualTo(1970);

    final List<SastRemediation> sastRemediations = sastRemediationDAO.getBySastFindingId(expectedSastFindingId);
    assertThat(sastRemediations).hasSize(1);
    final SastRemediation sastRemediation = sastRemediations.get(0);
    assertThat(sastRemediation.getId()).isEqualTo(expectedSastRemediationId);
    assertThat(sastRemediation.getSastFindingId()).isEqualTo(expectedSastFindingId);
    assertThat(sastRemediation.getContent()).isEqualTo("myContent");
  }

  /**
   * Asserts that the sastScanResponseDTO contains the expected data
   */
  private static void assertSastScanDTO(final SastScanResponseDTO actualSastScanResponseDTO) {
    assertThat(actualSastScanResponseDTO.sastScanId).isNotNull();
    assertThat(actualSastScanResponseDTO.createdAt).isNotNull();
    assertThat(actualSastScanResponseDTO.findings).hasSize(1);

    final SastFindingResponseDTO sastFindingResponseDTO = actualSastScanResponseDTO.findings.get(0);
    assertThat(sastFindingResponseDTO.sastFindingId).isNotNull();
    assertThat(sastFindingResponseDTO.ruleName).isEqualTo("myRuleName");
    assertThat(sastFindingResponseDTO.cwe).isEqualTo("myCwe");
    assertThat(sastFindingResponseDTO.coordinate)
        .isEqualTo(ImmutableMap.of("myCoordinateKey", "myCoordinateValue"));
    assertThat(sastFindingResponseDTO.description).isEqualTo("myDescription");
    assertThat(sastFindingResponseDTO.confidence).isEqualTo("HIGH");
    assertThat(sastFindingResponseDTO.severity).isEqualTo("Critical");
    assertThat(sastFindingResponseDTO.lineNumber).isEqualTo(1970);
    assertThat(sastFindingResponseDTO.remediations).hasSize(1);

    final SastRemediationResponseDTO sastRemediationResponseDTO = sastFindingResponseDTO.remediations.get(0);
    assertThat(sastRemediationResponseDTO.sastRemediationId).isNotNull();
    assertThat(sastRemediationResponseDTO.content).isEqualTo("myContent");
  }

  private static void assertSastScanDTOWithScmContext(final SastScanResponseDTO actualSastScanResponseDTO) {
    assertSastScanDTO(actualSastScanResponseDTO);

    final SastScmScanContextResponseDTO sastScmScanContext = actualSastScanResponseDTO.sastScmScanContext;
    assertThat(sastScmScanContext).isNotNull();
    assertThat(sastScmScanContext.commitHash).isEqualTo("testCommitHash");
    assertThat(sastScmScanContext.branchName).isEqualTo("testBranchName");
  }
}
