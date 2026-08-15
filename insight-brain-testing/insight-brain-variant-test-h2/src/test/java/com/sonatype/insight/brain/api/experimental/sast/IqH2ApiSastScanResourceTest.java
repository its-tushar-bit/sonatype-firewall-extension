/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.sast;

import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.sast.SastFindingDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastRemediationDAO;
import com.sonatype.insight.brain.dataaccess.sast.SastScanDAO;
import com.sonatype.insight.brain.model.Application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.experimental.sast.SastTestUtil.buildTestSastScanRequestDTO;
import static com.sonatype.insight.brain.api.experimental.sast.SastTestUtil.buildTestSastScanRequestDTOWith2Findings;
import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiSastScanResourceTest
{
  private IqTestContext ctx;

  private SastTestUtil sastTestUtil;

  @BeforeEach
  void setUp() {
    SastScanDAO sastScanDAO = ctx.lookup(SastScanDAO.class);
    SastFindingDAO sastFindingDAO = ctx.lookup(SastFindingDAO.class);
    SastRemediationDAO sastRemediationDAO = ctx.lookup(SastRemediationDAO.class);
    sastTestUtil = new SastTestUtil(sastScanDAO, sastFindingDAO, sastRemediationDAO);
  }

  @Test
  void testCreateSastScan_Success() throws Exception {
    // Given an existing application
    final Application application = ctx.tempEntity().newApplicationWithParent();

    // When a sast scan is created with an existing application
    final HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_SCAN_DATA_PATH)
        .parameter(application.getPublicId())
        .body(buildTestSastScanRequestDTO())
        .post();

    // Then assert the request is successful and the response body is a childless SastScanDTO
    ctx.assertResponseStatus(200, response);
    final SastScanResponseDTO sastScanResponseDTO = response.getBody(SastScanResponseDTO.class);

    // Then assert the proper SastScan fields are populated
    sastTestUtil.assertSastScan(application.getId(), sastScanResponseDTO);
  }

  @Test
  void testGetSastScan_Success() throws Exception {
    // Given an existing application
    final Application application = ctx.tempEntity().newApplicationWithParent();

    // And an existing sast scan
    final HttpResponse createResponse = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_SCAN_DATA_PATH)
        .parameter(application.getPublicId())
        .body(buildTestSastScanRequestDTO())
        .post();
    ctx.assertResponseStatus(200, createResponse);
    final String sastScanId = createResponse.getBody(SastScanResponseDTO.class).sastScanId;

    // When a GET request is executed for the sastScanId
    final HttpResponse getResponse = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_SCAN_DATA_PATH + "/{sastScanId}")
        .parameter(application.getPublicId(), sastScanId)
        .get();

    // Then the response should be successful with a SastScanDTO body containing children and Ids
    ctx.assertResponseStatus(200, getResponse);
    final SastScanResponseDTO sastScanResponseDTO = getResponse.getBody(SastScanResponseDTO.class);

    // Then assert the proper SastScan fields are populated
    sastTestUtil.assertSastScan(application.getId(), sastScanResponseDTO);
  }

  @Test
  void testCreateSastScan_FindingsOrderedByDescendingSeverity() throws Exception {
    // Given an existing application
    final Application application = ctx.tempEntity().newApplicationWithParent();

    // And a SastScanRequestDTO with 2 findings with different severities
    final SastScanRequestDTO sastScanRequestDTO = buildTestSastScanRequestDTOWith2Findings();
    assertThat(sastScanRequestDTO.findings).hasSize(2);
    assertThat(sastScanRequestDTO.findings.get(0).severity).isEqualTo("Low");
    assertThat(sastScanRequestDTO.findings.get(0).ruleName).isEqualTo("myRuleName");
    assertThat(sastScanRequestDTO.findings.get(1).severity).isEqualTo("High");
    assertThat(sastScanRequestDTO.findings.get(1).ruleName).isEqualTo("myRuleName2");

    // When a sast scan is created with an existing application
    final HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_SCAN_DATA_PATH)
        .parameter(application.getPublicId())
        .body(sastScanRequestDTO)
        .post();

    // Then assert the request is successful and the response body is a childless SastScanDTO
    ctx.assertResponseStatus(200, response);
    final SastScanResponseDTO sastScanResponseDTO = response.getBody(SastScanResponseDTO.class);

    // Then assert the findings are ordered by severity in descending order
    assertThat(sastScanResponseDTO.findings).hasSize(2);
    assertThat(sastScanResponseDTO.findings.get(0).severity).isEqualTo("High");
    assertThat(sastScanResponseDTO.findings.get(0).ruleName).isEqualTo("myRuleName2");
    assertThat(sastScanResponseDTO.findings.get(1).severity).isEqualTo("Low");
    assertThat(sastScanResponseDTO.findings.get(1).ruleName).isEqualTo("myRuleName");
  }

  @Test
  void testGetSastScan_FindingsOrderedByDescendingSeverity() throws Exception {
    // Given an existing application
    final Application application = ctx.tempEntity().newApplicationWithParent();

    // And a SastScanRequestDTO with 2 findings with different severities
    final SastScanRequestDTO sastScanRequestDTO = buildTestSastScanRequestDTOWith2Findings();
    assertThat(sastScanRequestDTO.findings).hasSize(2);
    assertThat(sastScanRequestDTO.findings.get(0).severity).isEqualTo("Low");
    assertThat(sastScanRequestDTO.findings.get(0).ruleName).isEqualTo("myRuleName");
    assertThat(sastScanRequestDTO.findings.get(1).severity).isEqualTo("High");
    assertThat(sastScanRequestDTO.findings.get(1).ruleName).isEqualTo("myRuleName2");

    // And an existing sast scan
    final HttpResponse createResponse = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_SCAN_DATA_PATH)
        .parameter(application.getPublicId())
        .body(sastScanRequestDTO)
        .post();
    ctx.assertResponseStatus(200, createResponse);
    final String sastScanId = createResponse.getBody(SastScanResponseDTO.class).sastScanId;

    // When a GET request is executed for the sastScanId
    final HttpResponse getResponse = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_SCAN_DATA_PATH + "/{sastScanId}")
        .parameter(application.getPublicId(), sastScanId)
        .get();

    // Then the response should be successful with a SastScanDTO body containing children and Ids
    ctx.assertResponseStatus(200, getResponse);
    final SastScanResponseDTO sastScanResponseDTO = getResponse.getBody(SastScanResponseDTO.class);

    // Then assert the findings are ordered by severity in descending order
    assertThat(sastScanResponseDTO.findings).hasSize(2);
    assertThat(sastScanResponseDTO.findings.get(0).severity).isEqualTo("High");
    assertThat(sastScanResponseDTO.findings.get(0).ruleName).isEqualTo("myRuleName2");
    assertThat(sastScanResponseDTO.findings.get(1).severity).isEqualTo("Low");
    assertThat(sastScanResponseDTO.findings.get(1).ruleName).isEqualTo("myRuleName");
  }

  @Test
  void testCreateSastScan_MissingScmContext_Success() throws Exception {
    // Given an existing application and no SCM context
    final Application application = ctx.tempEntity().newApplicationWithParent();
    final SastScanRequestDTO sastScanRequestDTO = buildTestSastScanRequestDTO();
    sastScanRequestDTO.scmContext = null;

    // When a sast scan is created with an existing application
    final HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.EXPERIMENTAL_SAST_SCAN_DATA_PATH)
        .parameter(application.getPublicId())
        .body(sastScanRequestDTO)
        .post();

    // Then assert the request is successful and the response body is a childless SastScanDTO
    ctx.assertResponseStatus(200, response);
    final SastScanResponseDTO sastScanResponseDTO = response.getBody(SastScanResponseDTO.class);

    // Then assert the proper SastScan fields are populated
    sastTestUtil.assertSastScan(application.getId(), sastScanResponseDTO);
  }
}
