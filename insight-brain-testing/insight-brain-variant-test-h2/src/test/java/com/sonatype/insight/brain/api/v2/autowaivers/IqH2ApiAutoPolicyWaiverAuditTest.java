/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import java.util.Date;
import jakarta.ws.rs.core.MediaType;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverDTO;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.service.AuditTestSupport;
import com.sonatype.insight.brain.variant.IqH2Test;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.test.LogOutput;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverResource.BY_AUTO_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverResource.OWNERS_PATH;
import static org.mockito.Mockito.when;

/**
 * Package-scoped: touches {@link ApiAutoPolicyWaiverResource}'s package-private {@code OWNERS_PATH}/
 * {@code BY_AUTO_POLICY_WAIVER_ID_PATH} constants, so the class stays in the original resource's package
 * (see convert-resource-test-to-variant skill, Step 3).
 */
@IqH2Test
public class IqH2ApiAutoPolicyWaiverAuditTest
    implements AuditTestSupport
{
  private IqTestContext ctx;

  private final TestLogOutput logOutput =
      new TestLogOutput(com.sonatype.insight.brain.audit.AuditRecorder.BASE_LOGGER_NAME);

  private com.sonatype.insight.brain.model.security.User unauthorizedUser;

  @BeforeEach
  void before() {
    logOutput.before();
    logOutput.clear();
    // beforeTest() resets the shared DeveloperEnablementService mock; the auto-policy-waiver resource is gated
    // by @ProductLicenseEnforcementPoint(DEVELOPER_DASHBOARD), which FeaturesService derives from this mock, so
    // re-stub it before setFeatures grants DEVELOPER_DASHBOARD (mirrors the AbstractResourceTest baseline).
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(true);
    unauthorizedUser = ctx.tempEntity().newUser();
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
  public PolicyDAO getPolicyDAO() {
    return ctx.lookup(PolicyDAO.class);
  }

  private java.util.function.Consumer<com.sonatype.insight.brain.HttpRequest> unauthorizedUser() {
    return httpRequest -> httpRequest.auth(unauthorizedUser);
  }

  @Override
  public String getUnauthorizedUsername() {
    return unauthorizedUser.getUsername();
  }

  @Test
  public void testAddApiAutoPolicyWaiver_Application() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    Application app = ctx.tempEntity().newApplicationWithParent();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = app.getId();
    dto.threatLevel = 2;
    dto.reachability = false;
    dto.pathForward = true;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    ApiAutoPolicyWaiverDTO responseDTO = response.getBody(ApiAutoPolicyWaiverDTO.class);

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER, null);
    assertApplicationData(auditDTO, app);
    assertCustomData(auditDTO, "autoPolicyWaiverId", responseDTO.autoPolicyWaiverId);
  }

  @Test
  public void testAddApiAutoPolicyWaiver_Organization() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    Organization organization = ctx.tempEntity().newOrganization();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = organization.getId();
    dto.threatLevel = 2;
    dto.reachability = false;
    dto.pathForward = true;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    ApiAutoPolicyWaiverDTO responseDTO = response.getBody(ApiAutoPolicyWaiverDTO.class);

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "autoPolicyWaiverId", responseDTO.autoPolicyWaiverId);
  }

  @Test
  public void testAddApiAutoPolicyWaiver_Unauthorized() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    Application app = ctx.tempEntity().newApplicationWithParent();

    ApiAutoPolicyWaiverDTO dto = new ApiAutoPolicyWaiverDTO();
    dto.ownerId = app.getId();
    dto.threatLevel = 2;
    dto.reachability = false;
    dto.pathForward = true;
    dto.creatorId = "creatorId";
    dto.creatorName = "creatorName";
    dto.createTime = new Date();

    ctx.restRequest()
        .with(unauthorizedUser())
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, app.getId())
        .body(dto, MediaType.APPLICATION_JSON)
        .post();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.CREATE_AUTO_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, app);
  }

  @Test
  public void testUpdateApiAutoPolicyWaiver_Application() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    autoPolicyWaiverDTO.autoPolicyWaiverId = autoPolicyWaiver.getId();
    autoPolicyWaiverDTO.threatLevel = 1;
    autoPolicyWaiverDTO.reachability = true;
    autoPolicyWaiverDTO.pathForward = false;

    ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .body(autoPolicyWaiverDTO, MediaType.APPLICATION_JSON)
        .put();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_AUTO_WAIVER, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "autoPolicyWaiverId", autoPolicyWaiver.getId());
  }

  @Test
  public void testUpdateApiAutoPolicyWaiver_Organization() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    Organization organization = ctx.tempEntity().newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(organization.getId());

    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    autoPolicyWaiverDTO.autoPolicyWaiverId = autoPolicyWaiver.getId();
    autoPolicyWaiverDTO.threatLevel = 1;
    autoPolicyWaiverDTO.reachability = true;
    autoPolicyWaiverDTO.pathForward = false;

    ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId())
        .body(autoPolicyWaiverDTO, MediaType.APPLICATION_JSON)
        .put();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_AUTO_WAIVER, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "autoPolicyWaiverId", autoPolicyWaiver.getId());
  }

  @Test
  public void testUpdateApiAutoPolicyWaiver_Unauthorized() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    ApiAutoPolicyWaiverDTO autoPolicyWaiverDTO = new ApiAutoPolicyWaiverDTO();
    autoPolicyWaiverDTO.autoPolicyWaiverId = autoPolicyWaiver.getId();
    autoPolicyWaiverDTO.threatLevel = 1;
    autoPolicyWaiverDTO.reachability = true;
    autoPolicyWaiverDTO.pathForward = false;

    ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .with(unauthorizedUser())
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .body(autoPolicyWaiverDTO, MediaType.APPLICATION_JSON)
        .put();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.UPDATE_AUTO_WAIVER, "unauthorized");
    assertApplicationData(auditDTO, application);
  }

  @Test
  public void testDeleteApiAutoPolicyWaiver_Application() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());

    ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER, null);
    assertApplicationData(auditDTO, application);
    assertCustomData(auditDTO, "autoPolicyWaiverId", autoPolicyWaiver.getId());
  }

  @Test
  public void testDeleteApiAutoPolicyWaiver_Organization() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    Organization organization = ctx.tempEntity().newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(organization.getId());

    ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId())
        .delete();

    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER, null);
    assertOrganizationData(auditDTO, organization);
    assertCustomData(auditDTO, "autoPolicyWaiverId", autoPolicyWaiver.getId());
  }

  @Test
  public void testDeleteApiAutoPolicyWaiver_Unauthorized() throws Exception {
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
    Organization organization = ctx.tempEntity().newOrganization();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(organization.getId());
    ctx.restRequest()
        .with(unauthorizedUser())
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.ORGANIZATION, organization.getId(), autoPolicyWaiver.getId())
        .delete();
    final AuditDTO auditDTO = assertAuditLog(AuditEvent.DELETE_AUTO_WAIVER, "unauthorized");
    assertOrganizationData(auditDTO, organization);
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
