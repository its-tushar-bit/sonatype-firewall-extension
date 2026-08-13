/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiLabelDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.Color.fromValue;

@IqH2Test
class IqH2ApiLabelResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private static final String LABEL_NAME = "babababa";

  private static final String LABEL_DESCRIPTION = "dadadada";

  private static final String LABEL_COLOR = Color.dark_blue.toValue();

  private static final String NEW_LABEL_NAME = "hohohoho";

  private static final String NEW_LABEL_DESCRIPTION = "hehehehe";

  private static final String NEW_LABEL_COLOR = Color.dark_red.toValue();

  private Application application;

  private Organization organization;

  private User unauthorizedUserEntity;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void init() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUserEntity = ctx.tempEntity().newUser();
    application = ctx.tempEntity().newApplicationWithParent();
    organization = ctx.tempEntity().newOrganization();
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
    return unauthorizedUserEntity.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUserEntity);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest();
  }

  private HttpRequest restRequest(OwnerType ownerType, String ownerId, ApiLabelDTO apiLabelDTO) {
    return restRequest().path(PublicApiPaths.LABEL_RESOURCE_PATH).parameter(ownerType, ownerId).body(apiLabelDTO);
  }

  private void assertLabelData(final AuditDTO auditDTO, final Label label) {
    assertCustomData(auditDTO, "labelId", label.getId());
    assertCustomData(auditDTO, "labelName", label.getLabel());
    assertCustomData(auditDTO, "labelDescription", label.getDescription());
    assertCustomData(auditDTO, "labelColor", label.getColor().toValue());
  }

  private void assertLabelData(AuditDTO auditDTO, ApiLabelDTO labelDTO) {
    assertCustomData(auditDTO, "labelId", labelDTO.id);
    assertCustomData(auditDTO, "labelName", labelDTO.label);
    assertCustomData(auditDTO, "labelDescription", labelDTO.description);
    assertCustomData(auditDTO, "labelColor", labelDTO.color);
  }

  @Test
  void testCreateLabel_AppLevel() throws Exception {
    ApiLabelDTO apiLabelDTO = new ApiLabelDTO(LABEL_NAME, LABEL_DESCRIPTION, LABEL_COLOR);

    final ApiLabelDTO addedLabel = restRequest(OwnerType.APPLICATION, application.getPublicId(), apiLabelDTO).post()
        .getBody(ApiLabelDTO.class);

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertLabelData(auditDTO, addedLabel);
  }

  @Test
  void testCreateLabel_OrgLevel() throws Exception {
    ApiLabelDTO apiLabelDTO = new ApiLabelDTO(LABEL_NAME, LABEL_DESCRIPTION, LABEL_COLOR);

    final ApiLabelDTO addedLabel = restRequest(OwnerType.ORGANIZATION, organization.getPublicId(), apiLabelDTO).post()
        .getBody(ApiLabelDTO.class);

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertLabelData(auditDTO, addedLabel);
  }

  @Test
  void testCreateLabel_RepoLevel() throws Exception {
    final Repository repository = ctx.tempEntity().newRepository();
    ApiLabelDTO apiLabelDTO = new ApiLabelDTO(LABEL_NAME, LABEL_DESCRIPTION, LABEL_COLOR);

    restRequest(OwnerType.REPOSITORY, repository.getId(), apiLabelDTO).post();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LABEL, "not-found");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  void testCreateLabel_Unauthorized() throws Exception {
    ApiLabelDTO apiLabelDTO = new ApiLabelDTO(LABEL_NAME, LABEL_DESCRIPTION, LABEL_COLOR);

    restRequest(OwnerType.APPLICATION, application.getPublicId(), apiLabelDTO).with(unauthorizedUser()).post();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  void testUpdateLabel_AppLevel() throws Exception {
    final Label label =
        ctx.tempEntity().newLabel(application.getId(), LABEL_NAME, LABEL_DESCRIPTION, fromValue(LABEL_COLOR));

    ApiLabelDTO apiLabelDTO = new ApiLabelDTO(NEW_LABEL_NAME, NEW_LABEL_DESCRIPTION, NEW_LABEL_COLOR);
    apiLabelDTO.id = label.getId();

    restRequest(OwnerType.APPLICATION, application.getPublicId(), apiLabelDTO).put();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertLabelData(auditDTO, apiLabelDTO);
  }

  @Test
  void testUpdateLabel_OrgLevel() throws Exception {
    String organizationId = organization.getId();
    final Label label =
        ctx.tempEntity().newLabel(organizationId, LABEL_NAME, LABEL_DESCRIPTION, fromValue(LABEL_COLOR));

    ApiLabelDTO apiLabelDTO = new ApiLabelDTO(NEW_LABEL_NAME, NEW_LABEL_DESCRIPTION, NEW_LABEL_COLOR);
    apiLabelDTO.id = label.getId();

    restRequest(OwnerType.ORGANIZATION, organization.getPublicId(), apiLabelDTO).put();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertLabelData(auditDTO, apiLabelDTO);
  }

  @Test
  void testDeleteLabel_AppLevel() throws Exception {
    Label toBeDeleted =
        ctx.tempEntity().newLabel(application.getId(), LABEL_NAME, LABEL_DESCRIPTION, fromValue(LABEL_COLOR));

    restRequest().path(PublicApiPaths.LABEL_RESOURCE_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId())
        .subpath(toBeDeleted.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertLabelData(auditDTO, toBeDeleted);
  }

  @Test
  void testDeleteLabel_OrgLevel() throws Exception {
    Label toBeDeleted = ctx.tempEntity().newLabel(organization.getId());

    restRequest().path(PublicApiPaths.LABEL_RESOURCE_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getPublicId())
        .subpath(toBeDeleted.getId())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertLabelData(auditDTO, toBeDeleted);
  }

  @Test
  void testDeleteLabel_Unauthorized() throws Exception {
    restRequest().path(PublicApiPaths.LABEL_RESOURCE_PATH)
        .parameter(OwnerType.APPLICATION, application.getPublicId())
        .path("labelId")
        .with(unauthorizedUser())
        .delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  void testUpdateLabel_Unauthorized() throws Exception {
    ctx.tempEntity().newLabel(application.getId(), LABEL_NAME, LABEL_DESCRIPTION, fromValue(LABEL_COLOR));

    ApiLabelDTO apiLabelDTO = new ApiLabelDTO(LABEL_NAME, LABEL_DESCRIPTION, LABEL_COLOR);

    restRequest(OwnerType.APPLICATION, application.getPublicId(), apiLabelDTO).with(unauthorizedUser()).put();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
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
