/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;

import com.sonatype.clm.dto.model.callflowanalysis.ApiCallFlowAnalysisConfigDTO;
import com.sonatype.clm.dto.model.callflowanalysis.CallFlowAlgorithm;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.configuration.CallFlowAnalysisConfigDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.CallFlowAnalysisConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiCallFlowAnalysisConfigResourceTest
{
  private IqTestContext ctx;

  private Application application;

  private CallFlowAnalysisConfigDAO callFlowDao;

  @BeforeEach
  void before() {
    application = ctx.tempEntity().newApplicationWithParent("ApiCallFlowAnalysis");
    callFlowDao = ctx.lookup(CallFlowAnalysisConfigDAO.class);
  }

  @Test
  void testUpsertApiCallFlowAnalysisConfig_Successful() throws Exception {
    HttpRequest request = ctx.restRequest()
        .path(PublicApiPaths.CALL_FLOW_ANALYSIS_CONFIG)
        .parameter(application.getType(), application.getId());
    ApiCallFlowAnalysisConfigDTO callFlowAnalysisConfig = buildCallFlowAnalysisConfig(application.getId());

    HttpResponse response = request.body(callFlowAnalysisConfig).put();
    ctx.assertResponseStatus(200, response);
    ApiCallFlowAnalysisConfigDTO configResponse = response.getBody(ApiCallFlowAnalysisConfigDTO.class);
    assertThat(configResponse).isNotNull();
    assertThat(configResponse.id).isNotNull();
    assertThat(configResponse.ownerId).isEqualTo(application.getId());
  }

  @Test
  void testUpsertApiCallFlowAnalysisConfig_BadRequest() throws Exception {
    HttpRequest request = ctx.restRequest()
        .path(PublicApiPaths.CALL_FLOW_ANALYSIS_CONFIG)
        .parameter(application.getType(), application.getId());
    ApiCallFlowAnalysisConfigDTO callFlowAnalysisConfig = buildCallFlowAnalysisConfigBadRequest(application.getId());

    HttpResponse response = request.body(callFlowAnalysisConfig).put();
    callFlowAnalysisConfig.ownerId = application.getPublicId();
    response = request.body(callFlowAnalysisConfig).put();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("ownerId does not match");

    callFlowAnalysisConfig.ownerId = null;
    response = request.body(callFlowAnalysisConfig).put();

    ctx.assertResponseStatus(400, response);
    assertThat(response.getBodyText()).isEqualTo("ownerId cannot be null");
  }

  @Test
  void testGetApiCallFlowAnalysisConfig_Successful() throws Exception {
    // insert element to search
    insertElementToSearch();
    HttpRequest request = ctx.restRequest()
        .path(PublicApiPaths.CALL_FLOW_ANALYSIS_CONFIG)
        .parameter(application.getType(), application.getId());

    HttpResponse response = request.get();
    ctx.assertResponseStatus(200, response);
    ApiCallFlowAnalysisConfigDTO configResponse = response.getBody(ApiCallFlowAnalysisConfigDTO.class);
    assertThat(configResponse).isNotNull();
    assertThat(configResponse.id).isNotNull();
    assertThat(configResponse.ownerId).isEqualTo(application.getId());
  }

  @Test
  void testGetApiCallFlowAnalysisConfig_NotFound() throws Exception {
    HttpRequest request = ctx.restRequest()
        .path(PublicApiPaths.CALL_FLOW_ANALYSIS_CONFIG)
        .parameter(application.getType(), application.getId());
    ApiCallFlowAnalysisConfigDTO callFlowAnalysisConfig = buildCallFlowAnalysisConfigBadRequest(application.getId());

    HttpResponse response = request.body(callFlowAnalysisConfig).get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Call Flow Analysis Config not found for ownerId " + application.getId());
  }

  @Test
  void testGetApiCallFlowAnalysisConfigByPublicId_Successful() throws Exception {
    // insert element to search
    insertElementToSearch();
    HttpRequest request = ctx.restRequest()
        .path(PublicApiPaths.CALL_FLOW_ANALYSIS_CONFIG, "publicId")
        .parameter(application.getType(), application.getPublicId());

    HttpResponse response = request.get();
    ctx.assertResponseStatus(200, response);
    ApiCallFlowAnalysisConfigDTO configResponse = response.getBody(ApiCallFlowAnalysisConfigDTO.class);
    assertThat(configResponse).isNotNull();
    assertThat(configResponse.id).isNotNull();
    assertThat(configResponse.ownerId).isEqualTo(application.getId());
  }

  @Test
  void testGetApiCallFlowAnalysisConfigByPublicId_NotFound() throws Exception {
    HttpRequest request = ctx.restRequest()
        .path(PublicApiPaths.CALL_FLOW_ANALYSIS_CONFIG, "publicId")
        .parameter(application.getType(), application.getPublicId());
    ApiCallFlowAnalysisConfigDTO callFlowAnalysisConfig = buildCallFlowAnalysisConfigBadRequest(application.getId());

    HttpResponse response = request.body(callFlowAnalysisConfig).get();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Call Flow Analysis Config not found for ownerId " + application.getId());
  }

  @Test
  void testDeleteApiCallFlowAnalysisConfig_Successful() throws Exception {
    // insert element to search
    insertElementToSearch();
    HttpRequest request = ctx.restRequest()
        .path(PublicApiPaths.CALL_FLOW_ANALYSIS_CONFIG)
        .parameter(application.getType(), application.getId());

    HttpResponse response = request.delete();
    // void method
    ctx.assertResponseStatus(204, response);
  }

  @Test
  void testDeleteApiCallFlowAnalysisConfig_NotFound() throws Exception {
    HttpRequest request = ctx.restRequest()
        .path(PublicApiPaths.CALL_FLOW_ANALYSIS_CONFIG)
        .parameter(application.getType(), application.getId());
    ApiCallFlowAnalysisConfigDTO callFlowAnalysisConfig = buildCallFlowAnalysisConfigBadRequest(application.getId());

    HttpResponse response = request.body(callFlowAnalysisConfig).delete();
    ctx.assertResponseStatus(404, response);
    assertThat(response.getBodyText()).isEqualTo(
        "Call Flow Analysis Config not found for ownerId " + application.getId());
  }

  private ApiCallFlowAnalysisConfigDTO buildCallFlowAnalysisConfig(String ownerId) {
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.enabled = true;
    apiCallFlowAnalysisConfigDTO.ownerId = ownerId;
    apiCallFlowAnalysisConfigDTO.threadCount = 1;
    apiCallFlowAnalysisConfigDTO.namespaces = new ArrayList<>();
    return apiCallFlowAnalysisConfigDTO;
  }

  private ApiCallFlowAnalysisConfigDTO buildCallFlowAnalysisConfigBadRequest(String ownerId) {
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.ownerId = ownerId;
    apiCallFlowAnalysisConfigDTO.threadCount = 1;
    apiCallFlowAnalysisConfigDTO.enabled = true;
    return apiCallFlowAnalysisConfigDTO;
  }

  private void insertElementToSearch() {
    callFlowDao.insert(new CallFlowAnalysisConfig(
        true, new ArrayList<>(), CallFlowAlgorithm.CLASS_HIERARCHY_ANALYSIS, 2, application.getId()));
  }
}
