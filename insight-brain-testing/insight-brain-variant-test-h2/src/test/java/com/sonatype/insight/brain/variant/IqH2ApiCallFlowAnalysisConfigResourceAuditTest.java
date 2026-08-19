/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.sonatype.clm.dto.model.callflowanalysis.ApiCallFlowAnalysisConfigDTO;
import com.sonatype.clm.dto.model.callflowanalysis.CallFlowAlgorithm;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.configuration.CallFlowAnalysisConfigDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.CallFlowAnalysisConfig;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2ApiCallFlowAnalysisConfigResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Organization org;

  private CallFlowAnalysisConfigDAO callFlowDao;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void setUp() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    org = ctx.tempEntity().newOrganization();
    callFlowDao = ctx.lookup(CallFlowAnalysisConfigDAO.class);
  }

  @AfterEach
  void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Test
  void testUpsert_Unauthorized() throws Exception {
    upsert(unauthorizedUser(), org, buildCallFlowAnalysisConfig(org.getId()));
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CALL_FLOW_ANALYSIS, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testUpsert_BadRequest() throws Exception {
    upsert(null, org, buildCallFlowAnalysisConfigBadRequest());
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CALL_FLOW_ANALYSIS, "bad-request");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testUpsert_Authorized() throws Exception {
    upsert(null, org, buildCallFlowAnalysisConfig(org.getId()));
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CALL_FLOW_ANALYSIS, null);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testDelete_Unauthorized() throws Exception {
    delete(unauthorizedUser(), org);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_CONFIGURE_CALL_FLOW_ANALYSIS, "unauthorized");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testDelete_Authorized_NotFound() throws Exception {
    delete(null, org);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_CONFIGURE_CALL_FLOW_ANALYSIS, "not-found");
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testDelete_Authorized() throws Exception {
    insertElementToSearch();
    delete(null, org);
    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_CONFIGURE_CALL_FLOW_ANALYSIS, null);
    assertOrganizationData(auditDTO, org);
  }

  @Test
  void testUpsertCallFlowAnalysisConfig_AuditData() throws Exception {
    upsert(null, org, buildCallFlowAnalysisConfig(org.getId()));
    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_CALL_FLOW_ANALYSIS, null);
    List<String> list = Collections.singletonList("foo");
    Map<String, List<String>> map = new HashMap<>();
    map.put("namespaces", list);
    assertCustomData(auditDTO, "namespaces", map.get("namespaces"));
  }

  private void upsert(
      Consumer<HttpRequest> user,
      Owner owner,
      ApiCallFlowAnalysisConfigDTO callFlowAnalysisConfig) throws Exception
  {
    restRequest(user, owner).body(callFlowAnalysisConfig).put();
  }

  private void delete(Consumer<HttpRequest> user, Owner owner) throws Exception {
    restRequest(user, owner).delete();
  }

  private HttpRequest restRequest(Consumer<HttpRequest> user, Owner owner) {
    return ctx.restRequest()
        .with(user)
        .path(PublicApiPaths.CALL_FLOW_ANALYSIS_CONFIG)
        .parameter(owner.getType(),
            owner.getId());
  }

  private ApiCallFlowAnalysisConfigDTO buildCallFlowAnalysisConfig(String ownerId) {
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.enabled = true;
    apiCallFlowAnalysisConfigDTO.ownerId = ownerId;
    apiCallFlowAnalysisConfigDTO.threadCount = 1;
    apiCallFlowAnalysisConfigDTO.namespaces = Collections.singletonList("foo");
    return apiCallFlowAnalysisConfigDTO;
  }

  private ApiCallFlowAnalysisConfigDTO buildCallFlowAnalysisConfigBadRequest() {
    ApiCallFlowAnalysisConfigDTO apiCallFlowAnalysisConfigDTO = new ApiCallFlowAnalysisConfigDTO();
    apiCallFlowAnalysisConfigDTO.threadCount = 1;
    apiCallFlowAnalysisConfigDTO.namespaces = new ArrayList<>();
    return apiCallFlowAnalysisConfigDTO;
  }

  private void insertElementToSearch() {
    callFlowDao.insert(new CallFlowAnalysisConfig(
        true,
        Collections.singletonList("foo"),
        CallFlowAlgorithm.CLASS_HIERARCHY_ANALYSIS,
        2,
        org.getId()));
  }

  private static final class TestLogOutput
      extends LogOutput
  {
    TestLogOutput(String... loggerNames) {
      super(loggerNames);
    }

    void tearDown() {
      after();
    }
  }
}
