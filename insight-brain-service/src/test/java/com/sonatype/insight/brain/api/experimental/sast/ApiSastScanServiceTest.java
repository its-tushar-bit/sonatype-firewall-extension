/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.sast.SastFindingDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastPullRequestCommentDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastRemediationDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastScanDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastScmScanContextDAO;
import com.sonatype.insight.brain.model.sast.SastPullRequestComment;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static com.sonatype.insight.brain.api.experimental.sast.SastTestUtil.buildTestSastScanRequestDTO;
import static com.sonatype.insight.brain.api.experimental.sast.SastTestUtil.buildTestSastScanRequestDTOWith2Findings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class ApiSastScanServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiSastScanService apiSastScanService;

  @Inject
  private SastScanDAO sastScanDAO;

  @Inject
  private SastFindingDAO sastFindingDAO;

  @Inject
  private SastRemediationDAO sastRemediationDAO;

  @Inject
  private SastScmScanContextDAO sastScmScanContextDAO;

  @Inject
  private SastPullRequestCommentDAO sastPullRequestCommentDAO;

  @Mock
  private SastPullRequestCommentingService sastPullRequestCommentingService;

  private SastTestUtil sastTestUtil;

  @Before
  public void before() {
    sastTestUtil = new SastTestUtil(sastScanDAO, sastFindingDAO, sastRemediationDAO);
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(SastPullRequestCommentingService.class).toInstance(sastPullRequestCommentingService);
    super.configure(binder);
  }

  @Test
  public void testCreateSastScan_Success() {
    // Given an application public id
    final String applicationId = tempEntity.newApplicationWithParent("myApp").getId();

    // And a payload
    final SastScanRequestDTO sastScanRequestDTO = buildTestSastScanRequestDTO();

    // When a sast scan is created
    assertThat(sastScanDAO.getAll()).isEmpty();
    final SastScanResponseDTO sastScanResponseDTO = apiSastScanService.createSastScan(
        "myApp", sastScanRequestDTO);

    // Then assert the proper SastScan fields are populated
    sastTestUtil.assertSastScan(applicationId, sastScanResponseDTO);

    // Assert that the SAST scan record contains the SCM context
    final SastScan sastScan = sastScanDAO.getById(sastScanResponseDTO.sastScanId);
    assertThat(sastScan.getSastScmScanContextId())
        .isNotNull();

    // Assert that a SastScmScanContext record was created
    assertThat(sastScmScanContextDAO.getCount()).isEqualTo(1);
  }

  @Test
  public void testCreateSastScan_FindingsOrderedByDescendingSeverity() {
    // Given an application
    tempEntity.newApplicationWithParent("myApp");

    // And a SastScanRequestDTO with 2 findings with different severities
    final SastScanRequestDTO sastScanRequestDTO = buildTestSastScanRequestDTOWith2Findings();
    assertThat(sastScanRequestDTO.findings).hasSize(2);
    assertThat(sastScanRequestDTO.findings.get(0).severity).isEqualTo("Low");
    assertThat(sastScanRequestDTO.findings.get(0).ruleName).isEqualTo("myRuleName");
    assertThat(sastScanRequestDTO.findings.get(1).severity).isEqualTo("High");
    assertThat(sastScanRequestDTO.findings.get(1).ruleName).isEqualTo("myRuleName2");

    // When a sast scan is created
    assertThat(sastScanDAO.getAll()).isEmpty();
    final SastScanResponseDTO sastScanResponseDTO = apiSastScanService.createSastScan(
        "myApp", sastScanRequestDTO);

    // Then assert the findings are ordered by severity in descending order
    assertThat(sastScanResponseDTO.findings).hasSize(2);
    assertThat(sastScanResponseDTO.findings.get(0).severity).isEqualTo("High");
    assertThat(sastScanResponseDTO.findings.get(0).ruleName).isEqualTo("myRuleName2");
    assertThat(sastScanResponseDTO.findings.get(1).severity).isEqualTo("Low");
    assertThat(sastScanResponseDTO.findings.get(1).ruleName).isEqualTo("myRuleName");
  }

  @Test
  public void testGetSastScan_SastScanIdDoesNotExist() {
    // Given an application
    tempEntity.newApplicationWithParent("myApp");

    // Expect a NotFoundException to be thrown when getting a non-existent sast scan
    assertThatThrownBy(() ->
        apiSastScanService.getSastScan("myApp", "someNonExistingSastScanId"))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("SastScan with ID someNonExistingSastScanId does not exist.");
  }

  @Test
  public void testGetSastScan_AppNotAssociatedWithSastScan() {
    // Given 2 different applications
    tempEntity.newApplicationWithParent("testApp1");
    tempEntity.newApplicationWithParent("testApp2");

    // And a sast scan with app1
    final SastScanResponseDTO createSastScanResult = apiSastScanService.createSastScan(
        "testApp1",
        buildTestSastScanRequestDTO());

    // Expect getSastScan to throw an exception when an existing App public id
    // does not match the one associated with the sast scan
    assertThatThrownBy(() ->
        apiSastScanService.getSastScan("testApp2", createSastScanResult.sastScanId))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Could not find SastScan");
  }

  @Test
  public void testGetSastScan_Success() {
    // Given an application
    final String applicationId = tempEntity.newApplicationWithParent("myApp").getId();

    // And a sast scan with a sastScanId
    assertThat(sastScanDAO.getAll()).isEmpty();
    final SastScanResponseDTO createdSastScanResult = apiSastScanService.createSastScan(
        "myApp",
        buildTestSastScanRequestDTO());
    final List<SastScan> actualSastScans = sastScanDAO.getAll();
    assertThat(actualSastScans).hasSize(1);
    final SastScan actualSastScan = actualSastScans.get(0);
    assertThat(createdSastScanResult.sastScanId).isEqualTo(actualSastScan.getId());

    // When a sast scan with the id is read
    final SastScanResponseDTO getSastScanResult = apiSastScanService.getSastScan("myApp",
        createdSastScanResult.sastScanId);

    // Then assert the proper SastScan fields are populated
    sastTestUtil.assertSastScanWithScmContext(applicationId, getSastScanResult);
  }

  @Test
  public void testGetSastScan_FindingsOrderedByDescendingSeverity() {
    // Given an application
    tempEntity.newApplicationWithParent("myApp");

    // And a SastScanRequestDTO with 2 findings with different severities
    final SastScanRequestDTO sastScanRequestDTO = buildTestSastScanRequestDTOWith2Findings();
    assertThat(sastScanRequestDTO.findings).hasSize(2);
    assertThat(sastScanRequestDTO.findings.get(0).severity).isEqualTo("Low");
    assertThat(sastScanRequestDTO.findings.get(0).ruleName).isEqualTo("myRuleName");
    assertThat(sastScanRequestDTO.findings.get(1).severity).isEqualTo("High");
    assertThat(sastScanRequestDTO.findings.get(1).ruleName).isEqualTo("myRuleName2");

    // And a sast scan with a sastScanId
    assertThat(sastScanDAO.getAll()).isEmpty();
    final SastScanResponseDTO createdSastScanResult = apiSastScanService.createSastScan(
        "myApp",
        sastScanRequestDTO);
    final List<SastScan> actualSastScans = sastScanDAO.getAll();
    assertThat(actualSastScans).hasSize(1);
    final SastScan actualSastScan = actualSastScans.get(0);
    assertThat(createdSastScanResult.sastScanId).isEqualTo(actualSastScan.getId());

    // When a sast scan with the id is read
    final SastScanResponseDTO getSastScanResult = apiSastScanService.getSastScan("myApp",
        createdSastScanResult.sastScanId);

    // Then assert the findings are ordered by severity in descending order
    assertThat(getSastScanResult.findings).hasSize(2);
    assertThat(getSastScanResult.findings.get(0).severity).isEqualTo("High");
    assertThat(getSastScanResult.findings.get(0).ruleName).isEqualTo("myRuleName2");
    assertThat(getSastScanResult.findings.get(1).severity).isEqualTo("Low");
    assertThat(getSastScanResult.findings.get(1).ruleName).isEqualTo("myRuleName");
  }

  @Test
  public void testCreateSastScan_InvalidSastFindingSeverityValue() {
    // Given an application
    tempEntity.newApplicationWithParent("myApp");

    // And a SastScanDTO with an invalid SastFindingSeverity text value
    final SastScanRequestDTO sastScanRequestDTO = buildTestSastScanRequestDTO();
    String uuid = UUID.randomUUID().toString();
    sastScanRequestDTO.findings.get(0).severity = uuid;

    // Expect a BadRequestException to be thrown
    assertThatThrownBy(() -> apiSastScanService.createSastScan("myApp", sastScanRequestDTO))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid name for SastFindingSeverity: " + uuid);
  }

  @Test
  public void testCreateSastScan_InvalidSastFindingConfidenceValue() {
    // Given an application
    tempEntity.newApplicationWithParent("myApp");

    // And a SastScanDTO with an invalid SastFindingConfidence text value
    final SastScanRequestDTO sastScanRequestDTO = buildTestSastScanRequestDTO();
    sastScanRequestDTO.findings.get(0).confidence = UUID.randomUUID().toString();

    // Expect a BadRequestException to be thrown
    assertThatThrownBy(() -> apiSastScanService.createSastScan("myApp", sastScanRequestDTO))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("Invalid value for SastFindingConfidence");
  }

  @Test
  public void testCreateSastScan_MissingScmContext_Success() {
    // Given an application public id
    final String applicationId = tempEntity.newApplicationWithParent("myApp").getId();

    // And a payload and no SCM context
    final SastScanRequestDTO sastScanRequestDTO = buildTestSastScanRequestDTO();
    sastScanRequestDTO.scmContext = null;

    // When a sast scan is created
    assertThat(sastScanDAO.getAll()).isEmpty();
    final SastScanResponseDTO sastScanResponseDTO = apiSastScanService.createSastScan(
        "myApp", sastScanRequestDTO);

    // Then assert the proper SastScan fields are populated
    sastTestUtil.assertSastScan(applicationId, sastScanResponseDTO);

    // Assert that the SAST scan record does not contain the SCM context
    final SastScan sastScan = sastScanDAO.getById(sastScanResponseDTO.sastScanId);
    assertThat(sastScan.getSastScmScanContextId())
        .isNull();

    // Assert that a SastScmScanContext record was not created
    assertThat(sastScmScanContextDAO.getCount()).isZero();
  }

  @Test
  public void testCreateSastScan_HasScmContext_DoesPRComment() {
    // Given an application
    tempEntity.newApplicationWithParent("myApp");

    // And a payload with SCM context
    final SastScanRequestDTO sastScanRequestDTO = buildTestSastScanRequestDTO();

    // When a sast scan is created
    apiSastScanService.createSastScan("myApp", sastScanRequestDTO);

    // PR commenting is attempted
    Mockito.verify(sastPullRequestCommentingService).createOrUpdateSastPullRequestComment(any(), anyString());
  }

  @Test
  public void testCreateSastScan_MissingScmContext_DoesNotPRComment() {
    // Given an application
    tempEntity.newApplicationWithParent("myApp");

    // And a payload and no SCM context
    final SastScanRequestDTO sastScanRequestDTO = buildTestSastScanRequestDTO();
    sastScanRequestDTO.scmContext = null;

    // When a sast scan is created
    apiSastScanService.createSastScan("myApp", sastScanRequestDTO);

    // PR commenting is not attempted
    Mockito.verify(sastPullRequestCommentingService, Mockito.never())
        .createOrUpdateSastPullRequestComment(any(), anyString());
  }

  @Test
  public void testGetSastScan_DoesNotIncludePullRequestUrl_WhenPullRequestCommentDoesNotExist() {
    // Given an application
    tempEntity.newApplicationWithParent("myApp");

    // And a sast scan with a sastScanId
    assertThat(sastScanDAO.getAll()).isEmpty();
    final SastScanResponseDTO createdSastScanResult = apiSastScanService.createSastScan(
        "myApp",
        buildTestSastScanRequestDTO());

    // And no PR comment exists
    assertThat(sastPullRequestCommentDAO.getBySastScanId(createdSastScanResult.sastScanId)).isNull();

    // Assert that no PR URL exists
    final SastScanResponseDTO getSastScanResult = apiSastScanService.getSastScan("myApp",
        createdSastScanResult.sastScanId);
    assertThat(getSastScanResult.sastScmScanContext.sastPullRequestURL).isNull();
  }

  @Test
  public void testGetSastScan_DoesIncludePullRequestUrl_WhenPullRequestCommentDoesExist() {
    // Given an application
    tempEntity.newApplicationWithParent("myApp");

    // And a sast scan with a sastScanId
    assertThat(sastScanDAO.getAll()).isEmpty();
    final SastScanResponseDTO createdSastScanResult = apiSastScanService.createSastScan(
        "myApp",
        buildTestSastScanRequestDTO());

    // With a PR comment available
    final SastPullRequestComment sastPullRequestComment = tempEntity.createSastPullRequestCommentBySastScanId(
        createdSastScanResult.sastScanId,
        "https://github.com/sonatype/insight-brain/pull/10894",
        "commit-hash",
        "content-hash",
        "discussion_r1450570374"
    );

    // Assert that PR URL exists
    final SastScanResponseDTO getSastScanResult = apiSastScanService.getSastScan("myApp",
        createdSastScanResult.sastScanId);
    assertThat(getSastScanResult.sastScmScanContext.sastPullRequestURL)
        .isEqualTo(sastPullRequestComment.getPullRequestUrl());
  }
}
