/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionResource.BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionResource.BY_AUTO_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionResource.OWNERS_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.autowaivers.ApiAutoPolicyWaiverExclusionResponseDTO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverExclusionDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverExclusion.ComponentMatcherStrategyForExclusion;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.brain.variant.IqPostgresTest;
import com.sonatype.insight.brain.variant.IqTestContext;
import com.sonatype.insight.license.model.LicensedFeature;

import java.util.Date;
import java.util.List;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@IqPostgresTest
class IqPostgresApiAutoPolicyWaiverExclusionResourceTest
{
  private static final String REPORT_RESOURCE = "/ApiAutoPolicyWaiverExclusionResourceTest/report";

  // Injected by IqPostgresServerExtension: the extension owns the shared, reused server.
  private IqTestContext ctx;

  private AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO;

  private PolicyViolationDAO policyViolationDAO;

  @BeforeEach
  void setUp() throws Exception {
    autoPolicyWaiverExclusionDAO = ctx.lookup(AutoPolicyWaiverExclusionDAO.class);
    policyViolationDAO = ctx.lookup(PolicyViolationDAO.class);
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(true);
    ctx.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.AUTO_WAIVER_MANAGEMENT);
  }

  @Test
  void testDeleteAutoPolicyWaiverExclusion() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());
    AutoPolicyWaiverExclusion exclusion =
        ctx.tempEntity().newAutoPolicyWaiverExclusion(application.getId(), autoPolicyWaiver.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId(), exclusion.getId())
        .delete();

    ctx.assertResponseStatus(204, response);
    assertThat(autoPolicyWaiverExclusionDAO.getById(exclusion.getId())).isNull();
  }

  @Test
  void testDeleteAutoPolicyWaiverExclusion_FeatureFlag() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());
    AutoPolicyWaiverExclusion exclusion =
        ctx.tempEntity().newAutoPolicyWaiverExclusion(application.getId(), autoPolicyWaiver.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId(), exclusion.getId())
        .delete();

    ctx.assertResponseStatus(204, response);
    assertThat(autoPolicyWaiverExclusionDAO.getById(autoPolicyWaiver.getId())).isNull();

    try {
      // when feature flag is disabled
      SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

      response = ctx.restRequest()
          .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
          .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId(), exclusion.getId())
          .delete();

      ctx.assertResponseStatus(403, response);
    }
    finally {
      SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
    }
  }

  @Test
  void testDeleteAutoPolicyWaiverExclusion_MissingDeveloperDashboardFeature() throws Exception {
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(false);
    ctx.setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());
    AutoPolicyWaiverExclusion exclusion =
        ctx.tempEntity().newAutoPolicyWaiverExclusion(application.getId(), autoPolicyWaiver.getId());

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId(), exclusion.getId())
        .delete();

    ctx.assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  void testAddAutoPolicyWaiverExclusion_Application() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());
    Policy policy = ctx.tempEntity().newPolicy(application.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = ctx.tempEntity().newPolicyEvaluation(application.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = ctx.tempEntity().newPolicyViolation(eval, policy, identifier, "fake", "fake");

    ApiAutoPolicyWaiverExclusionRequestDTO exclusion = new ApiAutoPolicyWaiverExclusionRequestDTO();
    exclusion.ownerId = application.getId();
    exclusion.autoPolicyWaiverId = autoPolicyWaiver.getId();
    exclusion.applicationPublicId = application.getPublicId();
    exclusion.scanId = "scanId";
    exclusion.policyViolationId = violation.getId();
    exclusion.matchStrategy = ComponentMatcherStrategyForExclusion.EXACT_COMPONENT;

    createPolicyThreatReport(application.getId(), exclusion.scanId, violation);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(exclusion)
        .post();

    ctx.assertResponseStatus(200, response);
    AutoPolicyWaiverExclusion resultingExclusion =
        autoPolicyWaiverExclusionDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
            application.getId(), autoPolicyWaiver.getId(), violation.getHash());
    assertThat(resultingExclusion).isNotNull();
  }

  @Test
  void testAddAutoPolicyWaiverExclusion_Organization() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(app.getOrganizationId());
    Policy policy = ctx.tempEntity().newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = ctx.tempEntity().newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = ctx.tempEntity().newPolicyViolation(eval, policy, identifier, "fake", "fake");

    ApiAutoPolicyWaiverExclusionRequestDTO exclusion = new ApiAutoPolicyWaiverExclusionRequestDTO();
    exclusion.ownerId = app.getOrganizationId();
    exclusion.autoPolicyWaiverId = autoPolicyWaiver.getId();
    exclusion.applicationPublicId = app.getPublicId();
    exclusion.scanId = "scanId";
    exclusion.policyViolationId = violation.getId();
    exclusion.matchStrategy = ComponentMatcherStrategyForExclusion.EXACT_COMPONENT;

    createPolicyThreatReport(app.getId(), exclusion.scanId, violation);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, app.getOrganizationId())
        .body(exclusion)
        .post();

    ctx.assertResponseStatus(200, response);
    AutoPolicyWaiverExclusion resultingExclusion =
        autoPolicyWaiverExclusionDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
            app.getOrganizationId(), autoPolicyWaiver.getId(), violation.getHash());
    assertThat(resultingExclusion).isNotNull();
  }

  @Test
  void testAddAutoPolicyWaiverExclusion_FeatureFlag() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());
    Policy policy = ctx.tempEntity().newPolicy(application.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = ctx.tempEntity().newPolicyEvaluation(application.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = ctx.tempEntity().newPolicyViolation(eval, policy, identifier, "fake", "fake");

    ApiAutoPolicyWaiverExclusionRequestDTO exclusion = new ApiAutoPolicyWaiverExclusionRequestDTO();
    exclusion.ownerId = application.getId();
    exclusion.autoPolicyWaiverId = autoPolicyWaiver.getId();
    exclusion.applicationPublicId = application.getPublicId();
    exclusion.scanId = "scanId";
    exclusion.policyViolationId = violation.getId();
    exclusion.matchStrategy = ComponentMatcherStrategyForExclusion.EXACT_COMPONENT;

    createPolicyThreatReport(application.getId(), exclusion.scanId, violation);

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(exclusion)
        .post();

    ctx.assertResponseStatus(200, response);

    AutoPolicyWaiverExclusion resultingExclusion =
        autoPolicyWaiverExclusionDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
            application.getId(), autoPolicyWaiver.getId(), violation.getHash());
    assertThat(resultingExclusion).isNotNull();

    try {
      // when feature flag is disabled
      SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

      response = ctx.restRequest()
          .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
          .parameter(OwnerType.APPLICATION, application.getId())
          .body(exclusion)
          .post();

      ctx.assertResponseStatus(403, response);
    }
    finally {
      SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
    }
  }

  @Test
  void testAddAutoPolicyWaiverExclusion_MissingDeveloperDashboardFeature() throws Exception {
    when(ctx.lookup(DeveloperEnablementService.class).shouldEnableDeveloperProduct()).thenReturn(false);
    ctx.setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverExclusionRequestDTO exclusion = new ApiAutoPolicyWaiverExclusionRequestDTO();
    exclusion.ownerId = application.getId();
    exclusion.autoPolicyWaiverId = autoPolicyWaiver.getId();

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(exclusion)
        .post();

    ctx.assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  void testAddAutoPolicyWaiverExclusion_IncompleteDto() throws Exception {
    Application application = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = ctx.tempEntity().newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverExclusionRequestDTO exclusion = new ApiAutoPolicyWaiverExclusionRequestDTO();
    exclusion.ownerId = application.getId();
    exclusion.autoPolicyWaiverId = autoPolicyWaiver.getId();

    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(exclusion)
        .post();

    ctx.assertResponseStatus(400, response);
  }

  @Test
  void testGetAutoPolicyWaiverExclusions() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    Policy policy = ctx.tempEntity().newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = ctx.tempEntity().newPolicyEvaluation(app.getId(), "stageId", "fakeScanId", new Date());
    PolicyViolation violation = ctx.tempEntity().newPolicyViolation(eval, policy, identifier, "fake", "fake");

    final ConstraintFact constraintFact1 = createConstraintFact("1", "Constraint 1",
        createSecuritySeverityConditionFact("CVE-111-111"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact2 = createConstraintFact("2", "Constraint 2",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact3 = createConstraintFact("3", "Constraint 3",
        createSecurityStatusConditionFact("CVE-222-333"),
        createLicenseConditionFact("Another reason"));

    final ConstraintFact constraintFact4 = createConstraintFact("1", "Constraint 1",
        createLicenseConditionFact("A reason"),
        createLicenseConditionFact("Another reason"));

    final List<ConstraintFact> constraintFacts =
        List.of(constraintFact1, constraintFact2, constraintFact3, constraintFact4);

    violation.setConstraintFacts(constraintFacts);
    policyViolationDAO.update(violation);
    AutoPolicyWaiver waiver = ctx.tempEntity().newAutoPolicyWaiver(app.getId());

    ctx.tempEntity().newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    ctx.tempEntity().newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    ctx.tempEntity().newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    ctx.tempEntity().newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    ctx.tempEntity().newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());

    HttpResponse responseOne = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 1)
        .query("pageSize", 3)
        .get();

    ctx.assertResponseStatus(200, responseOne);

    List<ApiAutoPolicyWaiverExclusionResponseDTO> exclusions =
        responseOne.getBodyList(ApiAutoPolicyWaiverExclusionResponseDTO.class);
    assertThat(exclusions).hasSize(3).allSatisfy(exclusion -> {
      assertThat(exclusion.ownerId).isEqualTo(app.getId());
      assertThat(exclusion.autoPolicyWaiverId).isEqualTo(waiver.getId());
    });

    HttpResponse responseTwo = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 2)
        .query("pageSize", 3)
        .get();

    ctx.assertResponseStatus(200, responseTwo);

    List<ApiAutoPolicyWaiverExclusionResponseDTO> exclusionsTwo =
        responseTwo.getBodyList(ApiAutoPolicyWaiverExclusionResponseDTO.class);

    assertThat(exclusionsTwo).hasSize(2).allSatisfy(exclusion -> {
      assertThat(exclusion.ownerId).isEqualTo(app.getId());
      assertThat(exclusion.autoPolicyWaiverId).isEqualTo(waiver.getId());
    });
  }

  @Test
  void testGetAutoPolicyWaiverExclusionsWithFeatureDisabled() throws Exception {
    try {
      SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
      Application app = ctx.tempEntity().newApplicationWithParent();
      AutoPolicyWaiver waiver = ctx.tempEntity().newAutoPolicyWaiver(app.getId());
      HttpResponse response = ctx.restRequest()
          .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
          .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
          .query("page", 1)
          .query("pageSize", 10)
          .get();

      ctx.assertResponseStatus(403, response);
    }
    finally {
      SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
    }
  }

  @Test
  void testGetAutoPolicyWaiverExclusions_FeatureFlag() throws Exception {
    Application app = ctx.tempEntity().newApplicationWithParent();
    AutoPolicyWaiver waiver = ctx.tempEntity().newAutoPolicyWaiver(app.getId());
    HttpResponse response = ctx.restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 1)
        .query("pageSize", 10)
        .get();

    ctx.assertResponseStatus(200, response);

    try {
      // when feature flag is disabled
      SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

      response = ctx.restRequest()
          .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
          .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
          .query("page", 1)
          .query("pageSize", 10)
          .get();

      ctx.assertResponseStatus(403, response);
    }
    finally {
      SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
    }
  }

  private ConditionFact createSecurityStatusConditionFact(final String cve) {
    return new ConditionFact(SecurityVulnerabilityStatusConditionType.ID, 0, "Security Status Summary",
        "Security Reason Summary", new TriggerReference(SECURITY_VULNERABILITY_REFID, cve));
  }

  private ConditionFact createSecuritySeverityConditionFact(final String cve) {
    return new ConditionFact(SecurityVulnerabilitySeverityConditionType.ID, 1, "Security Severity Summary",
        "Security Severity Reason", new TriggerReference(SECURITY_VULNERABILITY_REFID, cve));
  }

  private ConditionFact createLicenseConditionFact(final String reason) {
    return new ConditionFact(LicenseConditionType.ID, 1, "License Summary", reason, null);
  }

  private ConstraintFact createConstraintFact(
      final String constraintId,
      final String constraintName,
      final ConditionFact... conditionFacts)
  {
    return new ConstraintFact(constraintId, constraintName, null, conditionFacts);
  }

  private void createPolicyThreatReport(
      final String applicationId,
      final String scanId,
      final PolicyViolation violation) throws Exception
  {
    final InsightWork insightWork = ctx.lookup(InsightWork.class);
    ReportTestUtils.createReportFile(
        applicationId,
        scanId,
        ReportTestUtils.zipReportDir(REPORT_RESOURCE, ctx.tempFolder()),
        insightWork);
    ReportHelper.createPolicyThreats(insightWork, applicationId, scanId, List.of(violation));
  }
}
