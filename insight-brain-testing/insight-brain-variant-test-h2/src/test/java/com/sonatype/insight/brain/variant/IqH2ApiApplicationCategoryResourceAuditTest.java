/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.v2.ApiApplicationCategoryResource;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationCategoryDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.tag.TagService.fromDTO;
import static com.sonatype.insight.brain.tag.TagService.toDTO;

@IqH2Test
class IqH2ApiApplicationCategoryResourceAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private Organization organization;

  private User unauthorizedUser;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUser = ctx.tempEntity().newUser();
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
    return unauthorizedUser.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest()
        .path(ApiApplicationCategoryResource.RESOURCE_PATH,
            ApiApplicationCategoryResource.ORGANIZATION_PATH)
        .parameter(organization.getId());
  }

  @Test
  void testAddTag() throws Exception {
    ApiApplicationCategoryDTO dto = restRequest().body(toDTO(tag())).post().getBody(ApiApplicationCategoryDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION_CATEGORY, null);
    assertOrganizationData(auditDTO, organization);
    assertTagData(auditDTO, fromDTO(dto, dto.organizationId));
  }

  @Test
  void testAddTag_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).body(toDTO(tag())).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_APPLICATION_CATEGORY, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void testUpdateTag() throws Exception {
    ApiApplicationCategoryDTO dto =
        restRequest().body(toDTO(tag(saveTag().getId()))).put().getBody(ApiApplicationCategoryDTO.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_APPLICATION_CATEGORY, null);
    assertOrganizationData(auditDTO, organization);
    assertTagData(auditDTO, fromDTO(dto, dto.organizationId));
  }

  @Test
  void testUpdateTag_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).body(toDTO(tag(saveTag().getId()))).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_APPLICATION_CATEGORY, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void testDeleteTag() throws Exception {
    Tag tag = saveTag();

    restRequest().path(tag.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_APPLICATION_CATEGORY, null);
    assertOrganizationData(auditDTO, organization);
    assertTagData(auditDTO, tag);
  }

  @Test
  void testDeleteTag_Unauthorized() throws Exception {
    restRequest().with(unauthorizedUser()).path(saveTag().getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_APPLICATION_CATEGORY, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  private Tag tag() {
    return tag(null);
  }

  private Tag tag(String id) {
    Tag tag = new Tag(organization.getId(), "name1", "description1", Color.yellow);
    tag.setId(id);
    return tag;
  }

  private Tag saveTag() {
    return ctx.tempEntity().newTag(organization.getId(), "name2", "description2", Color.dark_blue);
  }

  private void assertTagData(AuditDTO auditDTO, Tag tag) {
    assertCustomData(auditDTO, "applicationCategoryId", tag.getId());
    assertCustomData(auditDTO, "applicationCategoryName", tag.getName());
    assertCustomData(auditDTO, "applicationCategoryDescription", tag.getDescription());
    assertCustomData(auditDTO, "applicationCategoryColor", tag.getColor().toValue());
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
