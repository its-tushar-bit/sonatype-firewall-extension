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
import com.sonatype.insight.brain.api.v2.dto.ApiAutoPolicyWaiverRevocationDTO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverRevocationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.policy.TriggerReference.Type.SECURITY_VULNERABILITY_REFID;
import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationResource.BY_AUTO_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationResource.OWNERS_PATH;
import static com.sonatype.insight.brain.api.v2.ApiAutoPolicyWaiverRevocationResource.BY_AUTO_POLICY_WAIVER_REVOCATION_ID_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ApiAutoPolicyWaiverRevocationResourceTest
    extends AbstractResourceTest
{
  private AutoPolicyWaiverRevocationDAO autoPolicyWaiverRevocationDAO;

  private PolicyViolationDAO policyViolationDAO;

  @Before
  public void setUp() {
    autoPolicyWaiverRevocationDAO = lookup(AutoPolicyWaiverRevocationDAO.class);
    policyViolationDAO = lookup(PolicyViolationDAO.class);
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
    licenseManager.setFeatures(LicensedFeature.DEVELOPER_DASHBOARD);
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);
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
    tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
    revocation.ownerId = application.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.scanId = "scanId";
    revocation.hash = "hash";

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.APPLICATION, application.getId())
        .body(revocation)
        .post();

    assertResponseStatus(200, response);
    AutoPolicyWaiverRevocation resultingRevocation =
        autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
            application.getId(), autoPolicyWaiver.getId(), revocation.hash);
    assertThat(resultingRevocation).isNotNull();
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_Organization() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(app.getOrganizationId());
    Policy policy = tempEntity.newPolicy(app.getOrganizationId());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "jar");
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), "stageId", "scanId", new Date());
    tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");
    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
    revocation.ownerId = app.getOrganizationId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.scanId = "scanId";
    revocation.hash = "hash";

    HttpResponse response = restRequest()
        .path(PublicApiPaths.AUTO_POLICY_WAIVER_REVOCATION_PATH + "/" + OWNERS_PATH)
        .parameter(OwnerType.ORGANIZATION, app.getOrganizationId())
        .body(revocation)
        .post();

    assertResponseStatus(200, response);
    AutoPolicyWaiverRevocation resultingRevocation =
        autoPolicyWaiverRevocationDAO.getByOwnerIdAndAutoPolicyWaiverIdAndHash(
            app.getOrganizationId(), autoPolicyWaiver.getId(), revocation.hash);
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
    tempEntity.newPolicyViolation(eval, policy, identifier, "fake", "fake");

    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
    revocation.ownerId = application.getId();
    revocation.autoPolicyWaiverId = autoPolicyWaiver.getId();
    revocation.scanId = "scanId";
    revocation.hash = "hash";

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
            application.getId(), autoPolicyWaiver.getId(), revocation.hash);
    assertThat(resultingRevocation).isNotNull();
  }

  @Test
  public void testAddAutoPolicyWaiverRevocation_MissingDeveloperDashboardFeature() throws Exception {
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    setMissingFeature(LicensedFeature.DEVELOPER_DASHBOARD);

    Application application = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver autoPolicyWaiver = tempEntity.newAutoPolicyWaiver(application.getId());
    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
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
    ApiAutoPolicyWaiverRevocationDTO revocation = new ApiAutoPolicyWaiverRevocationDTO();
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

    List<ApiAutoPolicyWaiverRevocationDTO> revocations =
        responseOne.getBodyList(ApiAutoPolicyWaiverRevocationDTO.class);
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

    List<ApiAutoPolicyWaiverRevocationDTO> revocationsTwo =
        responseTwo.getBodyList(ApiAutoPolicyWaiverRevocationDTO.class);

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
}
