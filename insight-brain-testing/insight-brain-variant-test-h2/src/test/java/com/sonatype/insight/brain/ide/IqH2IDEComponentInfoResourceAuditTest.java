/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.util.Collections;
import java.util.HashMap;
import java.util.function.Consumer;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.hds.VersionScoringService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.convertToHdsUrl;
import static com.sonatype.insight.brain.hds.ComponentInfoResourceTestUtils.newComponentDetails;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;

/**
 * Kept in the {@code com.sonatype.insight.brain.ide} package because
 * {@link IDEComponentInfoResource#APPLICATION_COMPONENT_DETAILS_PATH} is package-private. Reproduces the
 * {@code AbstractAuditTest}/{@code AbstractComponentInfoResourceAuditTest}/{@code
 * AbstractComponentInfoResourceAuditBaseTest} scaffolding that the legacy {@code IDEComponentInfoResourceAuditTest}
 * inherited.
 */
@IqH2Test
class IqH2IDEComponentInfoResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("g1",
      "a1", "v1", "", "jar");

  private static final String COMPONENT_HASH = "hash";

  private Application application;

  private User unauthorizedUser;

  private MultiLicenseDAO multiLicenseDAO;

  private final TestLogOutput logOutput =
      new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    multiLicenseDAO = ctx.lookup(MultiLicenseDAO.class);
    application = ctx.tempEntity().newApplicationWithParent();
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

  private AuditDTO assertAuditComponentInfo(Owner owner, ComponentIdentifier componentIdentifier) {
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertOwnerData(auditDTO, owner);
    assertCustomObject(auditDTO, "componentIdentifier", componentIdentifier);
    return auditDTO;
  }

  private AuditDTO assertAuditComponentInfo(Owner owner, ComponentIdentifier componentIdentifier, String hash) {
    AuditDTO auditDTO = assertAuditComponentInfo(owner, componentIdentifier);
    assertCustomData(auditDTO, "componentHash", hash);
    return auditDTO;
  }

  private void setupHdsResponseForComponent(final HttpRequest httpRequest) {
    ComponentDetails hdsComponentDetails = newComponentDetails(COMPONENT_IDENTIFIER, multiLicenseDAO);
    ComponentDetailsList hdsComponentDetailsList = new ComponentDetailsList();
    hdsComponentDetailsList.setList(Collections.singletonList(hdsComponentDetails));
    ctx.hdsRespondWith(hdsComponentDetailsList).atUri(convertToHdsUrl(httpRequest.getUrl()));
    ctx.hdsRespondWith(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()))
        .atUri("rest/component/dependencies");
    ctx.hdsRespondWith(new VersionScoringService[]{}).atUri(HDS_BULK_SCORE_VERSIONING_PATH);
  }

  @Test
  void testGetComponentDetails_CoordinatesOnly() throws Exception {
    detailsRequest(application.getPublicId(), COMPONENT_IDENTIFIER, null).get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER, null);
  }

  @Test
  void testGetComponentDetails_HashOnly() throws Exception {
    detailsRequest(application.getPublicId(), null, COMPONENT_HASH).get();

    assertAuditComponentInfo(application, null, COMPONENT_HASH);
  }

  @Test
  void testGetComponentDetails_CoordinatesAndHash() throws Exception {
    detailsRequest(application.getPublicId(), COMPONENT_IDENTIFIER, COMPONENT_HASH).get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER, COMPONENT_HASH);
  }

  @Test
  void testGetComponentDetails_Unauthorized() throws Exception {
    detailsRequest(application.getPublicId(), COMPONENT_IDENTIFIER, null).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  void testGetComponentDetailsList() throws Exception {
    HttpRequest detailsListRequest = detailsListRequest(application.getPublicId(), COMPONENT_IDENTIFIER);
    setupHdsResponseForComponent(detailsListRequest);

    detailsListRequest.get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER);
  }

  @Test
  void testGetComponentDetailsList_Unauthorized() throws Exception {
    detailsListRequest(application.getPublicId(), COMPONENT_IDENTIFIER).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  void testGetComponentDetailsForAllVersions() throws Exception {
    HttpRequest detailsAllVersionsRequest = detailsAllVersionsRequest(application.getPublicId(), COMPONENT_IDENTIFIER);
    setupHdsResponseForComponent(detailsAllVersionsRequest);

    detailsAllVersionsRequest.get();

    assertAuditComponentInfo(application, COMPONENT_IDENTIFIER);
  }

  @Test
  void testGetComponentDetailsForAllVersions_Unauthorized() throws Exception {
    detailsAllVersionsRequest(application.getPublicId(), COMPONENT_IDENTIFIER).with(unauthorizedUser()).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  private HttpRequest detailsAllVersionsRequest(String applicationId, ComponentIdentifier componentIdentifier) {
    return detailsRequest(applicationId, componentIdentifier, null).path("/allVersions");
  }

  private HttpRequest detailsListRequest(String applicationId, ComponentIdentifier componentIdentifier) {
    return detailsRequest(applicationId, componentIdentifier, null).path("/list");
  }

  private HttpRequest detailsRequest(String applicationId, ComponentIdentifier componentIdentifier, String hash) {
    return resourceRequest().parameter(applicationId)
        .query("componentIdentifier", componentIdentifier)
        .query("hash", hash);
  }

  private HttpRequest resourceRequest() {
    return ctx.restRequest()
        .path(IDEComponentInfoResource.RESOURCE_PATH, IDEComponentInfoResource.APPLICATION_COMPONENT_DETAILS_PATH);
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
