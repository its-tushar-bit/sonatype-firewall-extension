/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.IdeMatchedComponent;
import com.sonatype.clm.dto.model.ide.MatchedComponent;
import com.sonatype.clm.dto.model.ide.ScannedComponent;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.CLMEnforcementPoint;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class IdeResourceTest
    extends AbstractResourceTest
{

  private static final ComponentIdentifier MAVEN_COORDINATES = ComponentIdentifier.createMavenCoordinates("g1", "a1",
      "v1", null, "jar");

  private void addPolicy(String applicationPublicId, Policy policy) throws Exception {
    String appId = new ApplicationDAO().getByPublicIdNotNull(applicationPublicId).getId();
    policy.setOwnerId(appId);
    PolicyDAO policyDAO = new PolicyDAO();
    policyDAO.insert(policy);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(IdeResource.RESOURCE_PATH);
  }

  private HttpRequest simpleScanRequest(String appId, String hash) {
    return restRequest().path("scan", "simple", appId, hash);
  }

  private HttpRequest enhancedScanRequest(String appId, String hash) {
    return restRequest().path("scan", "enhanced", appId, hash);
  }

  private HttpRequest versionsRequest(String groupId, String artifactId) {
    return restRequest().path("component/versions").query("groupId", groupId).query("artifactId", artifactId);
  }

  private HttpRequest versionsRequest(ComponentIdentifier componentIdentifier) {
    return restRequest().path("component/versions").query("componentIdentifier", componentIdentifier);
  }

  private void mockHdsScanResponse(HttpRequest request, int status, String resource) {
    String hdsUrl = convertScanUrlToHdsUrl(request.getUrl());
    setHdsResponseForURI(hdsUrl, status, "/IdeResourceTest/" + resource);
  }

  private void mockHdsResponse(HttpRequest request, Object body, int status) {
    String hdsUrl = convertScanUrlToHdsUrl(request.getUrl());
    setHdsResponseForURI(hdsUrl, body, status);
  }

  private String convertScanUrlToHdsUrl(String brainUrl) {
    return brainUrl.replaceFirst("(.*/)(rest/ide/scan/[^/]+)(/[^/]+)(/.*)", "$2$4");
  }

  private String convertVersionsUrlToHdsUrl(String brainUrl) {
    return brainUrl.replaceFirst("(.*/)(rest/ide/component/versions)(.*)", "rest/ide/artifact/versions$3");
  }

  @Test
  public void testDoScan_Simple() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilityConditionType.ID, "present");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab");
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertEquals(IdentificationSource.SONATYPE.getId(), ideMatchedComponent.getIdentificationSource());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_Simple_ByComponentIdentifier() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilityConditionType.ID, "present");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar");
    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab").query("componentIdentifer",
        componentIdentifier);
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(componentIdentifier, ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertEquals(IdentificationSource.SONATYPE.getId(), ideMatchedComponent.getIdentificationSource());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_Enhanced() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    HttpRequest request = enhancedScanRequest(applicationPublicId, "abababababababababab");
    mockHdsScanResponse(request, 202, "EnhancedMatch_wait.json");
    HttpResponse response = request.body(new ScannedComponent()).post();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertNotNull(ideMatchedComponent.getWaitDelta());
    Assert.assertTrue(ideMatchedComponent.getWaitDelta() > 0);

    mockHdsScanResponse(request, 200, "EnhancedMatch_abababababababababab.json");
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertEquals(IdentificationSource.SONATYPE.getId(), ideMatchedComponent.getIdentificationSource());
    Assert.assertFalse(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_Enhanced_ByComponentIdentifier() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar");
    HttpRequest request = enhancedScanRequest(applicationPublicId, "abababababababababab").query("componentIdentifer",
        componentIdentifier);
    mockHdsScanResponse(request, 202, "EnhancedMatch_wait.json");
    HttpResponse response = request.body(new ScannedComponent()).post();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertNotNull(ideMatchedComponent.getWaitDelta());
    Assert.assertTrue(ideMatchedComponent.getWaitDelta() > 0);

    mockHdsScanResponse(request, 200, "EnhancedMatch_abababababababababab.json");
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(componentIdentifier, ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertEquals(IdentificationSource.SONATYPE.getId(), ideMatchedComponent.getIdentificationSource());
    Assert.assertFalse(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_OverriddenLicense() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(LicenseConditionType.ID, "is", "GPL-2.0");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab");
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    // Override the license and evaluate the policy again

    tempEntity.newLicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        null /* comment */);
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_LicenseStatus() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);

    Condition condition1 = new Condition(LicenseStatusConditionType.ID, "is",
        LicenseOverrideStatus.OVERRIDDEN.toString());
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab");
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    // Override the license and evaluate the policy again

    tempEntity.newLicenseOverride(application.getId(), MAVEN_COORDINATES, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        null /* comment */);
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_SecurityStatus() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilityStatusConditionType.ID, "is",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED.getId());
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    // There should be no policy alerts when none of the security vulnerabilities was overridden
    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab");
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    // Override the security vulnerabilities status for a security vulnerability that does not match and evaluate
    // the policy again. There should be no policy alerts.
    tempEntity.newSecurityVulnerabilityOverride(application.getId(), ideMatchedComponent.getHash(), "osvdb", "121212",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    // Override the security vulnerabilities status and evaluate the policy again. There should be one policy alert.
    tempEntity.newSecurityVulnerabilityOverride(application.getId(), ideMatchedComponent.getHash(), "osvdb", "36079",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_Age() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(AgeInDaysConditionType.ID, "older than", "365");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab");
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_unknown_simple() throws Exception {
    String hash = "000babababababababab";
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(MatchStateConditionType.ID, "is", "unknown");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    HttpRequest request = simpleScanRequest(applicationPublicId, hash);
    mockHdsScanResponse(request, 200, "SimpleMatch_000babababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertNull(ideMatchedComponent.getAlerts());
  }

  @Test
  public void testDoScan_unknown_simple_enhancedResponse() throws Exception {
    String hash = "000babababababababab";
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(MatchStateConditionType.ID, "is", "unknown");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    HttpRequest request = simpleScanRequest(applicationPublicId, hash);
    mockHdsScanResponse(request, 200, "SimpleMatch_000babababababababab_enhanced.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_simple_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = simpleScanRequest("unlicensedappId", "ulh").get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    HttpResponse response = simpleScanRequest("unlicensedappId", "ulh").get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_unknown_enhanced() throws Exception {
    String hash = "000babababababababab";
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(MatchStateConditionType.ID, "is", "unknown");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    HttpRequest request = enhancedScanRequest(applicationPublicId, hash);
    mockHdsScanResponse(request, 200, "SimpleMatch_000babababababababab_enhanced.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_enhanced_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = enhancedScanRequest("unlicensedappId", "ulh").get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_enhanced_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    HttpResponse response = enhancedScanRequest("unlicensedappId", "ulh").get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_Proprietary() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(ProprietaryConditionType.ID, "is true"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab").query("proprietary", true);
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());

    request = enhancedScanRequest(applicationPublicId, "abababababababababab").query("proprietary", true);
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());

    request = simpleScanRequest(applicationPublicId, "abababababababababab").query("proprietary", false);
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());
  }

  @Test
  public void testDoScan_ManuallyIdentifiedComponent() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    tempEntity.newApplicationWithParent(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    constraint1.addCondition(new Condition(AgeInDaysConditionType.ID, "younger than", "30"));
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "absent"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    String hash = "abababa1234babababab";
    String groupId = "g1";
    String artifactId = "a1";
    String version = "v1";
    Date createTime = new Date();
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(hash,
        ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version));
    hashComponentIdentifier.setCreateTime(createTime);
    HashComponentIdentifierDAO hashComponentIdentifierDAO = new HashComponentIdentifierDAO();
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);
    HttpRequest request = simpleScanRequest(applicationPublicId, hash).query("proprietary", false);
    MatchedComponent hdsResponse = new MatchedComponent();
    hdsResponse.setHash(hash);
    hdsResponse.addSecurityVulnerability(new SecurityVulnerability("12345", "osvdb", 5f));
    mockHdsResponse(request, hdsResponse, 200);
    HttpResponse response = request.get();
    hashComponentIdentifierDAO.delete(hashComponentIdentifier);
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent(groupId, artifactId, version, ideMatchedComponent);
    Assert.assertEquals(hash, ideMatchedComponent.getHash());
    Assert.assertEquals(MatchState.EXACT.getId(), ideMatchedComponent.getMatchState());
    Assert.assertEquals(IdentificationSource.MANUAL.getId(), ideMatchedComponent.getIdentificationSource());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_Label_DefinedAtAppLevel() throws Exception {
    testDoScan_Label(false, false);
  }

  @Test
  public void testDoScan_Label_DefinedAtOrgLevel_AppliedAtOrgLevel() throws Exception {
    testDoScan_Label(true, true);
  }

  @Test
  public void testDoScan_Label_DefinedAtOrgLevel_AppliedAtAppLevel() throws Exception {
    testDoScan_Label(true, false);
  }

  private void testDoScan_Label(boolean orgLabel, boolean orgComponentLabel) throws Exception {
    String hash = "abababababababababab";
    String applicationPublicId = "IdeResourceTest_AppId";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);
    Label label = tempEntity.newLabel(orgLabel ? app.getOrganizationId() : app.getId(), "red");
    tempEntity.newComponentLabel(orgComponentLabel ? app.getOrganizationId() : app.getId(), label.getId(), hash);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.setAction(BuildStageType.ID, FailActionType.ID);
    addPolicy(applicationPublicId, policy1);

    HttpRequest request = simpleScanRequest(applicationPublicId, hash);
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    Assert.assertEquals(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"),
        ideMatchedComponent.getComponentIdentifier());
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    Assert.assertThat(ideMatchedComponent.getHash(), is(hash));
    Assert.assertThat(ideMatchedComponent.getMatchState(), is("exact"));
    Assert.assertThat(ideMatchedComponent.getIdentificationSource(), is(IdentificationSource.SONATYPE.getId()));
    Assert.assertThat(ideMatchedComponent.isSimpleMatch(), is(true));
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertThat(policyAlerts, is(notNullValue()));
    Assert.assertThat(policyAlerts.size(), is(1));
  }

  @Test
  public void testGetComponentVersions() throws Exception {
    HttpRequest request = versionsRequest("gid", "aid");
    setHdsResponseForURI(convertVersionsUrlToHdsUrl(request.getUrl()), "[\"1.1\", \"2.0\"]", 200);
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    String[] versions = response.getBody(String[].class);
    Assert.assertEquals(Arrays.asList("1.1", "2.0"), Arrays.asList(versions));
  }

  @Test
  public void testGetComponentVersions_ByComponentIdentifier() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("gid", "aid", null);
    HttpRequest request = versionsRequest(componentIdentifier);
    setHdsResponseForURI(convertVersionsUrlToHdsUrl(request.getUrl()), "[\"1.1\", \"2.0\"]", 200);
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    String[] versions = response.getBody(String[].class);
    Assert.assertEquals(Arrays.asList("1.1", "2.0"), Arrays.asList(versions));
  }

  @Test
  public void testGetComponentVersions_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = versionsRequest("ulg", "ula").get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentVersions_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    HttpResponse response = versionsRequest("ulg", "ula").get();
    assertResponseStatus(402, response);
  }

  @SuppressWarnings("deprecation")
  private void assertGavInIdeMatchedComponent(String groupId,
                                              String artifactId,
                                              String version,
                                              IdeMatchedComponent ideMatchedComponent)
  {
    assertThat(ideMatchedComponent.getGroupId(), is(groupId));
    assertThat(ideMatchedComponent.getArtifactId(), is(artifactId));
    assertThat(ideMatchedComponent.getVersion(), is(version));
  }
}
