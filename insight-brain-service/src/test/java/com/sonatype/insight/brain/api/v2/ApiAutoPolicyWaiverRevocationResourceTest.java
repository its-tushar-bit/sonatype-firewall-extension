/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationRequestDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationResponseDTO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverRevocationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation;
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
import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationResource.BY_AUTO_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationResource.OWNERS_PATH;
import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationResource.BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApiAutoPolicyWaiverRevocationResourceTest
    extends AbstractResourceTest
{
  private AutoPolicyWaiverRevocationDAO autoPolicyWaiverRevocationDAO;

  private PolicyViolationDAO policyViolationDAO;
  
  protected static ReportService reportService = mock(ReportService.class);
  
  @Override
  public void configure(Binder binder) {
    binder.bind(ReportService.class).toInstance(reportService);
    super.configure(binder);
  }
  
  @Before
  public void setUp() {
    autoPolicyWaiverRevocationDAO = lookup(AutoPolicyWaiverRevocationDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
    licenseManager.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
    Mockito.reset(reportService);
  }
  
  @After
  public void cleanup() {
    licenseManager.reset();
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    AutoPolicyWaiverRevocation revocation =
        tempEntity.newAutoPolicyWaiverRevocation(application.getId(), autoPolicyWaiver.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId(), revocation.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(autoPolicyWaiverRevocationDAO.getById(revocation.getId())).isNull();
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_FeatureFlag() throws Exception {
    //when feature flag is disabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    AutoPolicyWaiverRevocation revocation =
        tempEntity.newAutoPolicyWaiverRevocation(application.getId(), autoPolicyWaiver.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId(), revocation.getId())
        .delete();

    assertResponseStatus(400, response);

    //when feature flag is enabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);

    response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId(), revocation.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(autoPolicyWaiverRevocationDAO.getById(autoPolicyWaiver.getId())).isNull();
  }

  @Test
  public void testDeleteAutoPolicyWaiverRevocation_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    AutoPolicyWaiverRevocation revocation =
        tempEntity.newAutoPolicyWaiverRevocation(application.getId(), autoPolicyWaiver.getId());

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), autoPolicyWaiver.getId(), revocation.getId())
        .delete();

    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    Policy policy = tempEntity.newPolicy(application.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(application.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    ApiAutoPolicyWaiverRevocationRequestDTO revocation = new ApiAutoPolicyWaiverRevocationRequestDTO();
    revocation.ownerId = application.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.applicationPublicId = application.getPublicId();
    revocation.scanId = "scanId";
    revocation.policyViolationId = violation.getId();
    revocation.matchStrategy = ComponentMatcherStrategyForRevocation.EXACT_COMPONENT;

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        violation
    );

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));
    
    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();
    
    assertResponseStatus(200, response);
    AutoPolicyWaiverRevocation resultingRevocation =
        autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
            application.getId(), autoPolicyWaiver.getId(), violation.getHash());
    assertThat(resultingRevocation).isNotNull();
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Organization() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    
    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        violation
    );

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));

    ApiAutoPolicyWaiverRevocationRequestDTO revocation = new ApiAutoPolicyWaiverRevocationRequestDTO();
    revocation.ownerId = app.getOrganizationId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.applicationPublicId = app.getPublicId();
    revocation.scanId = "scanId";
    revocation.policyViolationId = violation.getId();
    revocation.matchStrategy = ComponentMatcherStrategyForRevocation.EXACT_COMPONENT;
    
    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, app.getOrganizationId())
        .body(revocation)
        .post();

    assertResponseStatus(200, response);
    AutoPolicyWaiverRevocation resultingRevocation =
        autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
            app.getOrganizationId(), autoPolicyWaiver.getId(), violation.getHash());
    assertThat(resultingRevocation).isNotNull();
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_FeatureFlag() throws Exception {
    //when feature flag is disabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    Policy policy = tempEntity.newPolicy(application.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(application.getId(), "stageId", "scanId", new Date());
    PolicyViolation violation = tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    ApiAutoPolicyWaiverRevocationRequestDTO revocation = new ApiAutoPolicyWaiverRevocationRequestDTO();
    revocation.ownerId = application.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.applicationPublicId = application.getPublicId();
    revocation.scanId = "scanId";
    revocation.policyViolationId = violation.getId();
    revocation.matchStrategy = ComponentMatcherStrategyForRevocation.EXACT_COMPONENT;

    final PolicyThreats.Component component1Threats = createPolicyThreatsComponents(
        identifier,
        violation
    );

    when(reportService.getPolicyThreats(anyString(), anyString())).thenReturn(
        createPolicyThreats(Lists.newArrayList(component1Threats)));
    
    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();

    assertResponseStatus(400, response);

    //when feature flag is enabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);

    response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();

    assertResponseStatus(200, response);
    AutoPolicyWaiverRevocation resultingRevocation =
        autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
            application.getId(), autoPolicyWaiver.getId(), violation.getHash());
    assertThat(resultingRevocation).isNotNull();
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverRevocationResponseDTO revocation = new ApiAutoPolicyWaiverRevocationResponseDTO();
    revocation.ownerId = application.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.hash = "hash";

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();

    assertResponseStatus(HttpStatus.PAYMENT_REQUIRED_402, response);
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_IncompleteDto() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverRevocationResponseDTO revocation = new ApiAutoPolicyWaiverRevocationResponseDTO();
    revocation.ownerId = application.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();

    assertResponseStatus(400, response);
  }

  @Test
  public void testGetAutoPolicyWaiverRevocations() throws Exception {
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

    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());

    HttpResponse responseOne = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 1)
        .query("pageSize", 3)
        .get();

    assertResponseStatus(200, responseOne);

    List<ApiAutoPolicyWaiverRevocationResponseDTO> revocations =
        responseOne.getBodyList(ApiAutoPolicyWaiverRevocationResponseDTO.class);
    assertThat(revocations).hasSize(3).allSatisfy(revocation -> {
      assertThat(revocation.ownerId).isEqualTo(app.getId());
      assertThat(revocation.autoPolicyWaiverId).isEqualTo(waiver.getId());
    });

    HttpResponse responseTwo = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 2)
        .query("pageSize", 3)
        .get();

    assertResponseStatus(200, responseTwo);

    List<ApiAutoPolicyWaiverRevocationResponseDTO> revocationsTwo =
        responseTwo.getBodyList(ApiAutoPolicyWaiverRevocationResponseDTO.class);

    assertThat(revocationsTwo).hasSize(2).allSatisfy(revocation -> {
      assertThat(revocation.ownerId).isEqualTo(app.getId());
      assertThat(revocation.autoPolicyWaiverId).isEqualTo(waiver.getId());
    });
  }

  @Test
  public void testGetAutoPolicyWaiverRevocationsWithFeatureDisabled() throws Exception {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 1)
        .query("pageSize", 10)
        .get();

    assertResponseStatus(400, response);
  }

  @Test
  public void testGetAutoPolicyWaiverRevocations_FeatureFlag() throws Exception {
    //when feature flag is disabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 1)
        .query("pageSize", 10)
        .get();

    assertResponseStatus(400, response);

    //when feature flag is enabled
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);

    response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + BY_AUTO_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, app.getId(), waiver.getId())
        .query("page", 1)
        .query("pageSize", 10)
        .get();

    assertResponseStatus(200, response);
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
      PolicyViolation violation
  )
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
