/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqH2Test
public class ApiOrganizationResourceV2AuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private OrganizationDAO organizationDAO;

  private Organization parentOrg;

  private Organization childOrg;

  private Organization targetOrg;

  private User unauthorizedUserAccount;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  @BeforeEach
  public void before() {
    logOutput.before();
    logOutput.clear();
    unauthorizedUserAccount = ctx.tempEntity().newUser();

    organizationDAO = ctx.lookup(OrganizationDAO.class);

    parentOrg = ctx.tempEntity().newOrganization();
    childOrg = ctx.tempEntity().newOrganization(parentOrg);
    targetOrg = ctx.tempEntity().newOrganization();
  }

  @AfterEach
  public void after() {
    logOutput.tearDown();
  }

  @Override
  public LogOutput getLogOutput() {
    return logOutput;
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUserAccount.getUsername();
  }

  @Override
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUserAccount);
  }

  @Test
  public void testAddOrganization() throws Exception {
    ApiOrganizationDTO organizationDto = new ApiOrganizationDTO(null, "new-organization");
    organizationApiRequest().body(organizationDto).post();
    Organization organization = organizationDAO.getByName(organizationDto.name);

    AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testAddOrganization_Unauthorized() throws Exception {
    ApiOrganizationDTO organizationDto = new ApiOrganizationDTO(null, "new-organization");
    organizationApiRequest().with(unauthorizedUser()).body(organizationDto).post();

    assertAuditLog(AuditEvent.CREATE_ORGANIZATION, "unauthorized");
  }

  @Test
  public void testMoveOrganization() throws Exception {
    moveOrganizationApiRequest().parameter(childOrg.getId(), targetOrg.getId()).put();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_ORGANIZATION, null);
    assertOrganizationAndParentData(auditDTO, childOrg, targetOrg);
  }

  @Test
  public void testMoveOrganization_Unauthorized() throws Exception {
    moveOrganizationApiRequest().with(unauthorizedUser()).parameter(childOrg.getId(), targetOrg.getId()).put();

    assertAuditLog(AuditEvent.UPDATE_ORGANIZATION, "unauthorized");
  }

  @Test
  public void testDeleteOrganization() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();

    organizationApiRequest().path(organization.getId()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_ORGANIZATION, null);
    assertOrganizationData(auditDTO, organization);
  }

  @Test
  public void testDeleteOrganization_Unauthorized() throws Exception {
    Organization organization = ctx.tempEntity().newOrganization();

    organizationApiRequest().path(organization.getId()).with(unauthorizedUser()).delete();

    assertAuditLog(AuditEvent.DELETE_ORGANIZATION, "unauthorized");
  }

  private HttpRequest organizationApiRequest() {
    return ctx.restRequest().path(PublicApiPaths.ORG_RESOURCE_PATH);
  }

  private HttpRequest moveOrganizationApiRequest() {
    return organizationApiRequest().path(ApiOrganizationResourceV2.MOVE_ORGANIZATION_PATH);
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
