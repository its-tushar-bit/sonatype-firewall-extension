/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.organization.IconUtils;
import com.sonatype.insight.brain.organization.OrganizationResource;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2OrganizationResourceAuditTest
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

  private HttpRequest organizationRequest() {
    return ctx.restRequest().path(OrganizationResource.RESOURCE_PATH);
  }

  @Test
  void testAddOrganization() throws Exception {
    Organization organization = new Organization("orgName");

    organization = organizationRequest().body(organization).post().getBody(Organization.class);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void testAddOrganization_Unauthorized() throws Exception {
    organizationRequest().body(new Organization()).with(unauthorizedUser()).post();

    assertAuditLog(AuditEvent.CREATE_ORGANIZATION, "unauthorized");
  }

  @Test
  void testUpdateOrganization() throws Exception {
    organization.setName("updatedName");

    organizationRequest().body(organization).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void testUpdateOrganization_Unauthorized() throws Exception {
    organizationRequest().body(organization).with(unauthorizedUser()).put();

    assertAuditLog(AuditEvent.UPDATE_ORGANIZATION, "unauthorized");
  }

  @Test
  void testDeleteOrganization_WithoutChildApplications() throws Exception {
    organizationRequest().path(OrganizationResource.DELETE_ORGANIZATION_PATH).parameter(organization.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);

    assertThat(assertAuditLogs(AuditEvent.DELETE_APPLICATION, 0, null)).isEmpty();
  }

  @Test
  void testDeleteOrganization_WithChildApplications() throws Exception {
    User contact = ctx.tempEntity().newUser();
    Application app1 = ctx.tempEntity()
        .newApplication("appName1", "appPublicId1", organization.getId(), contact.getUsername());
    Application app2 = ctx.tempEntity()
        .newApplication("appName2", "appPublicId2", organization.getId(), contact.getUsername());

    organizationRequest().path(OrganizationResource.DELETE_ORGANIZATION_PATH).parameter(organization.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);

    List<AuditDTO> auditDTOs = assertAuditLogs(AuditEvent.DELETE_APPLICATION, 2, null);
    for (Application app : Arrays.asList(app1, app2)) {
      AuditDTO appAuditDTO = findFirstByDataKeyValue(auditDTOs, "applicationId", app.getId());
      assertApplicationData(appAuditDTO, app);
      assertCustomData(appAuditDTO, "contactUsername", contact.getUsername());
      assertParentOrganizationData(appAuditDTO, organization);
    }
  }

  @Test
  void testDeleteOrganization_NLevel_WithoutChildApplications() throws Exception {
    List<Organization> testOrgs = ctx.tempEntity().newRelatedOrganizationsAsList(1, 7, 0);
    organizationRequest().path(OrganizationResource.DELETE_ORGANIZATION_PATH)
        .parameter(testOrgs.get(testOrgs.size() - 1).getId())
        .delete();

    List<AuditDTO> deletedOrgsAuditEvents = assertAuditLogs(AuditEvent.DELETE_ORGANIZATION, testOrgs.size(), null);
    for (Organization currentOrg : testOrgs) {
      AuditDTO orgAuditDTO = findFirstByDataKeyValue(deletedOrgsAuditEvents, "organizationId", currentOrg.getId());
      assertOrganizationData(orgAuditDTO, currentOrg);
      if (!Organization.ROOT_ORGANIZATION_ID.equals(currentOrg.getParentOrganizationId())) {
        Organization parentOrg =
            testOrgs.stream()
                .filter(org -> org.getId().equals(currentOrg.getParentOrganizationId()))
                .findFirst()
                .orElse(null);
        assertParentOrganizationData(orgAuditDTO, parentOrg);
      }
    }

    assertThat(assertAuditLogs(AuditEvent.DELETE_APPLICATION, 0, null)).isEmpty();
  }

  @Test
  void testDeleteOrganization_NLevel_WithChildApplications() throws Exception {
    List<Organization> testOrgs = ctx.tempEntity().newRelatedOrganizationsAsList(organization, 1, 6, 0);
    List<Application> testApps = new LinkedList<>();
    testOrgs.add(organization);

    testOrgs.forEach(currentOrg -> testApps.add(ctx.tempEntity().newApplicationWithParent(currentOrg)));

    organizationRequest().path(OrganizationResource.DELETE_ORGANIZATION_PATH)
        .parameter(organization.getId())
        .delete();

    List<AuditDTO> deletedOrgsAuditEvents = assertAuditLogs(AuditEvent.DELETE_ORGANIZATION, testOrgs.size(), null);
    List<AuditDTO> deletedAppsAuditEvents = assertAuditLogs(AuditEvent.DELETE_APPLICATION, testApps.size(), null);
    for (int i = 0; i < testOrgs.size(); i++) {
      Organization currentOrg = testOrgs.get(i);
      Application currentApp = testApps.get(i);

      AuditDTO orgAuditDTO = findFirstByDataKeyValue(deletedOrgsAuditEvents, "organizationId", currentOrg.getId());
      assertOrganizationData(orgAuditDTO, currentOrg);
      if (!Organization.ROOT_ORGANIZATION_ID.equals(currentOrg.getParentOrganizationId())) {
        Organization parentOrg =
            testOrgs.stream()
                .filter(org -> org.getId().equals(currentOrg.getParentOrganizationId()))
                .findFirst()
                .orElse(null);
        assertParentOrganizationData(orgAuditDTO, parentOrg);
      }

      AuditDTO appAuditDTO = findFirstByDataKeyValue(deletedAppsAuditEvents, "applicationId", currentApp.getId());
      assertApplicationData(appAuditDTO, currentApp);
      assertParentOrganizationData(appAuditDTO, currentOrg);
    }
  }

  @Test
  void testDeleteOrganization_Unauthorized() throws Exception {
    organizationRequest().path(OrganizationResource.DELETE_ORGANIZATION_PATH)
        .parameter(organization.getId())
        .with(unauthorizedUser())
        .delete();

    assertAuditLog(AuditEvent.DELETE_ORGANIZATION, "unauthorized");
  }

  @Test
  void testSetIcon_Robot() throws Exception {
    organizationRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organization.getId())
        .part("hasRobotSource", "true")
        .part("hashcode", "")
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ORGANIZATION_ICON, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "iconType", "robot");
  }

  @Test
  void testSetIcon_File() throws Exception {
    String iconFilename = "defaulticon_application.png";

    organizationRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organization.getId())
        .part("hasRobotSource", "false")
        .part("file", iconFilename, IconUtils.loadIconFromProductAssets("defaulticon_application.png"))
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ORGANIZATION_ICON, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "iconType", "file");
    assertCustomData(auditDTO, "iconFilename", iconFilename);
  }

  @Test
  void testSetIcon_Default() throws Exception {
    organizationRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organization.getId())
        .part("hasRobotSource", "false")
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ORGANIZATION_ICON, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "iconType", "default");
  }

  @Test
  void testSetIcon_Unauthorized() throws Exception {
    organizationRequest().path(OrganizationResource.ORGANIZATION_ICON_PATH)
        .parameter(organization.getId())
        .part("hasRobotSource", "false")
        .with(unauthorizedUser())
        .post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CONFIGURE_ORGANIZATION_ICON, "unauthorized");
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  void testMoveOrganizationErrorsExport() throws Exception {
    List<Organization> organizations = ctx.tempEntity().newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = ctx.tempEntity().newOrganization();

    organizationRequest()
        .path(OrganizationResource.MOVE_ORGANIZATION_ERRORS_EXPORT_PATH)
        .parameter(organizations.get(0).getId(), organization.getId())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_MOVE_ORGANIZATION_ERRORS_LIST, null);
    assertOrganizationData(auditDTO, organizations.get(0));
  }

  @Test
  void testMoveOrganizationErrorsExport_Unauthorized() throws Exception {
    List<Organization> organizations = ctx.tempEntity().newRelatedOrganizationsAsList(1, 2, 0);
    Organization organization = ctx.tempEntity().newOrganization();

    organizationRequest()
        .path(OrganizationResource.MOVE_ORGANIZATION_ERRORS_EXPORT_PATH)
        .parameter(organizations.get(0).getId(), organization.getId())
        .with(unauthorizedUser())
        .get();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EXPORT_MOVE_ORGANIZATION_ERRORS_LIST, "unauthorized");
    assertOrganizationData(auditDTO, organizations.get(0));
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
