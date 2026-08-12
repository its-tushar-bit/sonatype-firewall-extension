/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.label.ComponentLabelResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * H2 port of {@code ComponentLabelResourceAuditTest}.
 */
@IqH2Test
class IqH2ComponentLabelResourceAuditTest
    implements AuditTestSupport
{
  private static final String COMPONENT_HASH = "bababababa";

  private IqTestContext ctx;

  private Label label;

  private Label labelWithIdOnly;

  private User unauthorizedUser;

  private final TestLogOutput logOutput = new TestLogOutput(AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
    label = ctx.tempEntity().newLabel(Organization.ROOT_ORGANIZATION_ID);
    labelWithIdOnly = new Label();
    labelWithIdOnly.setId(label.getId());
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

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest(final Owner owner, final String hash) {
    return ctx.restRequest()
        .path(ComponentLabelResource.RESOURCE_PATH)
        .parameter(owner.getType(), owner.getType().equals(OwnerType.APPLICATION) ? owner.getPublicId() : owner.getId(),
            hash);
  }

  private HttpRequest restRequest(Owner owner, String hash, String labelId) {
    return restRequest(owner, hash).path(labelId);
  }

  private void assertComponentLabelData(final AuditDTO auditDTO) {
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
    assertCustomData(auditDTO, "labelId", label.getId());
    assertCustomData(auditDTO, "labelName", label.getLabel());
  }

  @Test
  void testSetComponentLabel_AppLevel() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    restRequest(application, COMPONENT_HASH).body(labelWithIdOnly).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertComponentLabelData(auditDTO);
  }

  @Test
  void testSetComponentLabel_OrgLevel() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    restRequest(organization, COMPONENT_HASH).body(labelWithIdOnly).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertComponentLabelData(auditDTO);
  }

  @Test
  void testSetComponentLabel_RepoLevel() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    restRequest(repository, COMPONENT_HASH).body(labelWithIdOnly).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, null);
    assertRepositoryData(auditDTO, repository);
    assertComponentLabelData(auditDTO);
  }

  @Test
  void testSetComponentLabel_RepoContainerLevel() throws Exception {
    restRequest(RepositoryContainer.SINGLETON, COMPONENT_HASH).body(labelWithIdOnly).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, null);
    assertRepositoryContainerData(auditDTO);
    assertComponentLabelData(auditDTO);
  }

  @Test
  void testSetComponentLabel_Unauthorized() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    restRequest(application, COMPONENT_HASH).body(labelWithIdOnly).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.ASSIGN_COMPONENT_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  void testDeleteComponentLabel_AppLevel() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newComponentLabel(application.getId(), label.getId(), COMPONENT_HASH);
    restRequest(application, COMPONENT_HASH, label.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, null);
    assertApplicationData(auditDTO, application);
    assertComponentLabelData(auditDTO);
  }

  @Test
  void testDeleteComponentLabel_OrgLevel() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();
    ctx.tempEntity().newComponentLabel(organization.getId(), label.getId(), COMPONENT_HASH);
    restRequest(organization, COMPONENT_HASH, label.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, null);
    assertOrganizationData(auditDTO, organization);
    assertComponentLabelData(auditDTO);
  }

  @Test
  void testDeleteComponentLabel_RepoLevel() throws Exception {
    Repository repository = ctx.tempEntity().newRepository();
    ctx.tempEntity().newComponentLabel(repository.getId(), label.getId(), COMPONENT_HASH);
    restRequest(repository, COMPONENT_HASH, label.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, null);
    assertRepositoryData(auditDTO, repository);
    assertComponentLabelData(auditDTO);
  }

  @Test
  void testDeleteComponentLabel_RepoContainerLevel() throws Exception {
    ctx.tempEntity().newComponentLabel(RepositoryContainer.REPOSITORY_CONTAINER_ID, label.getId(), COMPONENT_HASH);
    restRequest(RepositoryContainer.SINGLETON, COMPONENT_HASH, label.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, null);
    assertRepositoryContainerData(auditDTO);
    assertComponentLabelData(auditDTO);
  }

  @Test
  void testDeleteComponentLabel_Unauthorized() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    ctx.tempEntity().newComponentLabel(application.getId(), label.getId(), COMPONENT_HASH);
    restRequest(application, COMPONENT_HASH, label.getId()).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_COMPONENT_LABEL, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  void testGetComponentLabels_Application() throws Exception {
    final Application application = ctx.tempEntity().newApplicationWithParent();
    restRequest(application, COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  void testGetComponentLabels_Organization() throws Exception {
    final Organization organization = ctx.tempEntity().newOrganization();
    restRequest(organization, COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  void testGetComponentLabels_Repository() throws Exception {
    final Repository repository = ctx.tempEntity().newRepository();
    restRequest(repository, COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertRepositoryData(auditDTO, repository);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  void testGetComponentLabels_RepositoryContainer() throws Exception {
    restRequest(RepositoryContainer.SINGLETON, COMPONENT_HASH).get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, null);
    assertRepositoryContainerData(auditDTO);
    assertCustomData(auditDTO, "componentHash", COMPONENT_HASH);
  }

  @Test
  void testGetComponentLabels_Unauthorized() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();

    restRequest(application, COMPONENT_HASH).with(unauthorizedUser()).get();
    AuditDTO auditDTO = assertAuditLog(AuditEvent.VIEW_COMPONENT_INFORMATION, "unauthorized");
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
