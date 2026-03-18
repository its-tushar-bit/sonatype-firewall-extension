/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.autowaivers;

import java.util.Date;
import java.util.List;

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
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import com.google.common.collect.Lists;
import com.google.inject.Binder;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionResource.BY_AUTO_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionResource.OWNERS_PATH;
import static com.sonatype.insight.brain.api.v2.autowaivers.ApiAutoPolicyWaiverExclusionResource.BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiAutoPolicyWaiverExclusionResourceTest
    extends AbstractResourceTest
{
  private AutoPolicyWaiverExclusionDAO autoPolicyWaiverExclusionDAO;

  private PolicyViolationDAO policyViolationDAO;

  protected static ReportService reportService = mock(ReportService.class);

  @Override
  public void configure(Binder binder) {
    binder.bind(ReportService.class).toInstance(reportService);
    super.configure(binder);
  }

  @Before
  public void setUp() {
    autoPolicyWaiverExclusionDAO = lookup(AutoPolicyWaiverExclusionDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
    licenseManager.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    Mockito.reset(reportService);
  }

  @After
  public void cleanup() {
    licenseManager.reset();
  }

  @Test
  public void testDeleteAutoPolicyWaiverExclusion() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    AutoPolicyWaiverExclusion exclusion =
        tempEntity.newAutoPolicyWaiverExclusion(application.getId(), autoPolicyWaiver.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId(), exclusion.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(autoPolicyWaiverExclusionDAO.getById(exclusion.getId())).isNull();
  }

  @Test
  public void testDeleteAutoPolicyWaiverExclusion_FeatureFlag() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    AutoPolicyWaiverExclusion exclusion =
        tempEntity.newAutoPolicyWaiverExclusion(application.getId(), autoPolicyWaiver.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId(), exclusion.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(autoPolicyWaiverExclusionDAO.getById(autoPolicyWaiver.getId())).isNull();

    // when feature flag is disabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

    response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId(), exclusion.getId())
        .delete();

    assertResponseStatus(403, response);
  }

  @Test
  public void testDeleteAutoPolicyWaiverExclusion_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    AutoPolicyWaiverExclusion exclusion =
        tempEntity.newAutoPolicyWaiverExclusion(application.getId(), autoPolicyWaiver.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_EXCLUSION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId(), exclusion.getId())
        .delete();

    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testAddAutoPolicyWaiverExclusion_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    Policy policy = tempEntity.newPolicy(application.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(application.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    ApiAutoPolicyWaiverExclusionRequestDTO exclusion = new ApiAutoPolicyWaiverExclusionRequestDTO();
    exclusion.ownerId = application.getId();
    exclusion.autoPolicyWaiverId = autoPolicyWaiver.getId();
    exclusion.applicationPublicId = application.getPublicId();
    exclusion.scanId = "scanId";
    exclusion.policyViolationId = violation.getId();
    exclusion.matchStrategy = ComponentMatcherStrategyForExclusion.EXACT_COMPONENT;

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        violation);

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(exclusion)
        .post();

    assertResponseStatus(200, response);
    AutoPolicyWaiverExclusion resultingExclusion =
        autoPolicyWaiverExclusionDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
            application.getId(), autoPolicyWaiver.getId(), violation.getHash());
    assertThat(resultingExclusion).isNotNull();
  }

  @Test
  public void testAddAutoPolicyWaiverExclusion_Organization() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        violation);

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    ApiAutoPolicyWaiverExclusionRequestDTO exclusion = new ApiAutoPolicyWaiverExclusionRequestDTO();
    exclusion.ownerId = app.getOrganizationId();
    exclusion.autoPolicyWaiverId = autoPolicyWaiver.getId();
    exclusion.applicationPublicId = app.getPublicId();
    exclusion.scanId = "scanId";
    exclusion.policyViolationId = violation.getId();
    exclusion.matchStrategy = ComponentMatcherStrategyForExclusion.EXACT_COMPONENT;

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, app.getOrganizationId())
        .body(exclusion)
        .post();

    assertResponseStatus(200, response);
    AutoPolicyWaiverExclusion resultingExclusion =
        autoPolicyWaiverExclusionDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
            app.getOrganizationId(), autoPolicyWaiver.getId(), violation.getHash());
    assertThat(resultingExclusion).isNotNull();
  }

  @Test
  public void testAddAutoPolicyWaiverExclusion_FeatureFlag() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    Policy policy = tempEntity.newPolicy(application.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(application.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    ApiAutoPolicyWaiverExclusionRequestDTO exclusion = new ApiAutoPolicyWaiverExclusionRequestDTO();
    exclusion.ownerId = application.getId();
    exclusion.autoPolicyWaiverId = autoPolicyWaiver.getId();
    exclusion.applicationPublicId = application.getPublicId();
    exclusion.scanId = "scanId";
    exclusion.policyViolationId = violation.getId();
    exclusion.matchStrategy = ComponentMatcherStrategyForExclusion.EXACT_COMPONENT;

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        violation);

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(exclusion)
        .post();

    assertResponseStatus(200, response);

    AutoPolicyWaiverExclusion resultingExclusion =
        autoPolicyWaiverExclusionDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
            application.getId(), autoPolicyWaiver.getId(), violation.getHash());
    assertThat(resultingExclusion).isNotNull();

    // when feature flag is disabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

    response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(exclusion)
        .post();

    assertResponseStatus(403, response);
  }

  @Test
  public void testAddAutoPolicyWaiverExclusion_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverExclusionResponseDTO exclusion = new ApiAutoPolicyWaiverExclusionResponseDTO();
    exclusion.ownerId = application.getId();
    exclusion.autoPolicyWaiverId = autoPolicyWaiver.getId();
    exclusion.hash = "hash";

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(exclusion)
        .post();

    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testAddAutoPolicyWaiverExclusion_IncompleteDto() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverExclusionResponseDTO exclusion = new ApiAutoPolicyWaiverExclusionResponseDTO();
    exclusion.ownerId = application.getId();
    exclusion.autoPolicyWaiverId = autoPolicyWaiver.getId();

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(exclusion)
        .post();

    assertResponseStatus(400, response);
  }

  @Test
  public void testGetAutoPolicyWaiverExclusions() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "fakeScanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

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
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());

    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverExclusion(app.getId(), waiver.getId());

    HttpResponse responseOne = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 1)
        .query("pageSize", 3)
        .get();

    assertResponseStatus(200, responseOne);

    List<ApiAutoPolicyWaiverExclusionResponseDTO> exclusions =
        responseOne.getBodyList(ApiAutoPolicyWaiverExclusionResponseDTO.class);
    assertThat(exclusions).hasSize(3).allSatisfy(exclusion -> {
      assertThat(exclusion.ownerId).isEqualTo(app.getId());
      assertThat(exclusion.autoPolicyWaiverId).isEqualTo(waiver.getId());
    });

    HttpResponse responseTwo = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 2)
        .query("pageSize", 3)
        .get();

    assertResponseStatus(200, responseTwo);

    List<ApiAutoPolicyWaiverExclusionResponseDTO> exclusionsTwo =
        responseTwo.getBodyList(ApiAutoPolicyWaiverExclusionResponseDTO.class);

    assertThat(exclusionsTwo).hasSize(2).allSatisfy(exclusion -> {
      assertThat(exclusion.ownerId).isEqualTo(app.getId());
      assertThat(exclusion.autoPolicyWaiverId).isEqualTo(waiver.getId());
    });
  }

  @Test
  public void testGetAutoPolicyWaiverExclusionsWithFeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 1)
        .query("pageSize", 10)
        .get();

    assertResponseStatus(403, response);
  }

  @Test
  public void testGetAutoPolicyWaiverExclusions_FeatureFlag() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 1)
        .query("pageSize", 10)
        .get();

    assertResponseStatus(200, response);

    // when feature flag is disabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);

    response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_EXCLUSION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 1)
        .query("pageSize", 10)
        .get();

    assertResponseStatus(403, response);
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

  private PolicyThreats createPolicyThreats(final List<PolicyThreats.Component> components) {
    final PolicyThreats policyThreats = new PolicyThreats();
    policyThreats.aaData.addAll(components);

    return policyThreats;
  }

  private PolicyThreats.Component createPolicyThreatsComponents(
      ComponentIdentifier componentIdentifier,
      PolicyViolation violation)
  {
    PolicyThreats.PolicyViolation policyViolation = new PolicyThreats.PolicyViolation();
    policyViolation.policyThreatLevel = violation.getThreatLevel();
    policyViolation.policyViolationId = violation.getId();
    policyViolation.policyName = violation.getPolicyName();
    policyViolation.policyId = violation.getPolicyId();
    policyViolation.actions = null;
    policyViolation.constraints = null;
    policyViolation.policyThreatCategory = null;
    policyViolation.reachabilityStatus = null;
    policyViolation.constraintFactsJson = violation.getConstraintFactsJson();

    final PolicyThreats.Component component = new PolicyThreats.Component();
    component.hash = violation.getHash();
    component.componentIdentifier = componentIdentifier;
    component.activeViolations.add(policyViolation);
    component.allViolations.add(policyViolation);
    return component;
  }
}
