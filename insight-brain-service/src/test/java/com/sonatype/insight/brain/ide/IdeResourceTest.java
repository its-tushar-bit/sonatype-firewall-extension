/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.ide.IdeMatchedComponent;
import com.sonatype.clm.dto.model.ide.MatchedComponent;
import com.sonatype.clm.dto.model.ide.ScannedComponent;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
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
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.CLMEnforcementPoint;
import com.sonatype.insight.test.RestAccess;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class IdeResourceTest
    extends AbstractResourceTest
{
  private void addPolicy(String applicationPublicId, Policy policy) throws Exception {
    String appId = new ApplicationDAO().getByPublicIdNotNull(applicationPublicId).getId();
    PolicyDAO policyDAO = new PolicyDAO(brain.getWorkDir());
    policyDAO.insert(appId, policy);
  }

  @Test
  public void testDoScan_Simple() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    createApplication(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilityConditionType.ID, "present");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    String serviceUrl = getScanSimpleUrl(applicationPublicId, "abababababababababab");
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json");
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(),
        IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
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
    createApplication(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    String serviceUrl = getScanEnhancedUrl(applicationPublicId, "abababababababababab");
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 202, "/IdeResourceTest/EnhancedMatch_wait.json");
    Response response = AuthedRestAccess.post(serviceUrl, JsonHelpers.asJson(new ScannedComponent()));
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(),
        IdeMatchedComponent.class);
    Assert.assertNotNull(ideMatchedComponent.getWaitDelta());
    Assert.assertTrue(ideMatchedComponent.getWaitDelta() > 0);

    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/EnhancedMatch_abababababababababab.json");
    response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(), IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
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
    Application application = createApplication(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(LicenseConditionType.ID, "is", "GPL-2.0");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    String serviceUrl = getScanSimpleUrl(applicationPublicId, "abababababababababab");
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json");
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(),
        IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    // Override the license and evaluate the policy again

    LicenseOverride licenseOverride = new LicenseOverride(application.getId(), "g1", "a1", "v1",
        LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", null /* comment */);
    new LicenseOverrideDAO().insert(licenseOverride);
    response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(), IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
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
    Application application = createApplication(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);

    Condition condition1 = new Condition(LicenseStatusConditionType.ID, "is",
        LicenseOverrideStatus.OVERRIDDEN.toString());
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    String serviceUrl = getScanSimpleUrl(applicationPublicId, "abababababababababab");
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json");
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(),
        IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    // Override the license and evaluate the policy again

    LicenseOverride licenseOverride = new LicenseOverride(application.getId(), "g1", "a1", "v1",
        LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", null /* comment */);
    new LicenseOverrideDAO().insert(licenseOverride);
    response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(), IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
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
    Application application = createApplication(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(SecurityVulnerabilityStatusConditionType.ID, "is",
        SecurityVulnerabilityStatus.ACKNOWLEDGED.getId());
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    // There should be no policy alerts when none of the security vulnerabilities was overridden
    String serviceUrl = getScanSimpleUrl(applicationPublicId, "abababababababababab");
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json");
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(),
        IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    // Override the security vulnerabilities status for a security vulnerability that does not match and evaluate
    // the policy again. There should be no policy alerts.
    setSecurityAuditLog(application.getId(), "/IdeResourceTest/SecurityOverride_abababababababababab_NotMatch.json");
    response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(), IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(0, policyAlerts.size());

    // Override the security vulnerabilities status and evaluate the policy again. There should be one policy alert.
    setSecurityAuditLog(application.getId(), "/IdeResourceTest/SecurityOverride_abababababababababab.json");
    response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(), IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
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
    createApplication(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(AgeInDaysConditionType.ID, "older than", "365");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    String serviceUrl = getScanSimpleUrl(applicationPublicId, "abababababababababab");
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json");
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(),
        IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
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
    createApplication(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(MatchStateConditionType.ID, "is", "unknown");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    String serviceUrl = getScanSimpleUrl(applicationPublicId, hash);
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/SimpleMatch_000babababababababab.json");
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(),
        IdeMatchedComponent.class);
    Assert.assertNull(ideMatchedComponent.getAlerts());
  }

  @Test
  public void testDoScan_unknown_simple_enhancedResponse() throws Exception {
    String hash = "000babababababababab";
    String applicationPublicId = "IdeResourceTest_AppId";
    createApplication(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(MatchStateConditionType.ID, "is", "unknown");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    String serviceUrl = getScanSimpleUrl(applicationPublicId, hash);
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/SimpleMatch_000babababababababab_enhanced.json");
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(),
        IdeMatchedComponent.class);
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_simple_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getScanSimpleUrl("unlicensedappId", "ulh"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    Response response = AuthedRestAccess.get(getScanSimpleUrl("unlicensedappId", "ulh"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_unknown_enhanced() throws Exception {
    String hash = "000babababababababab";
    String applicationPublicId = "IdeResourceTest_AppId";
    createApplication(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    Condition condition1 = new Condition(MatchStateConditionType.ID, "is", "unknown");
    constraint1.addCondition(condition1);
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    String serviceUrl = getScanEnhancedUrl(applicationPublicId, hash);
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/SimpleMatch_000babababababababab_enhanced.json");
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(),
        IdeMatchedComponent.class);
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());
  }

  @Test
  public void testDoScan_enhanced_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getScanEnhancedUrl("unlicensedappId", "ulh"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_enhanced_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    Response response = AuthedRestAccess.get(getScanEnhancedUrl("unlicensedappId", "ulh"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_Proprietary() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    createApplication(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(ProprietaryConditionType.ID, "is true"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    addPolicy(applicationPublicId, policy1);

    String serviceUrl = getScanUrl("simple", applicationPublicId, "abababababababababab", null, null, null, null,
        "true");
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json");
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(),
        IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());

    serviceUrl = getScanUrl("enhanced", applicationPublicId, "abababababababababab", null, null, null, null, "true");
    saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json");
    response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(), IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
    Assert.assertEquals("abababababababababab", ideMatchedComponent.getHash());
    Assert.assertEquals("exact", ideMatchedComponent.getMatchState());
    Assert.assertTrue(ideMatchedComponent.isSimpleMatch());
    policyAlerts = ideMatchedComponent.getAlerts();
    Assert.assertNotNull(policyAlerts);
    Assert.assertEquals(1, policyAlerts.size());

    serviceUrl = getScanUrl("simple", applicationPublicId, "abababababababababab", null, null, null, null, "false");
    saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json");
    response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(), IdeMatchedComponent.class);
    Assert.assertEquals("g1", ideMatchedComponent.getGroupId());
    Assert.assertEquals("a1", ideMatchedComponent.getArtifactId());
    Assert.assertEquals("v1", ideMatchedComponent.getVersion());
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
    createApplication(applicationPublicId);

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(MatchStateConditionType.ID, "is", "exact"));
    constraint1.addCondition(new Condition(AgeInDaysConditionType.ID, "younger than", "30"));
    constraint1.addCondition(new Condition(SecurityVulnerabilityConditionType.ID, "absent"));
    Policy policy1 = new Policy("PolicyId1", "Policy1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    policy1.addAction(BuildStageType.ID, new Action(FailActionType.ID));
    addPolicy(applicationPublicId, policy1);

    String hash = "abababa1234babababab";
    String groupId = "g1";
    String artifactId = "a1";
    String version = "v1";
    Date createTime = new Date();
    HashGAV hashGAV = new HashGAV(hash, groupId, artifactId, version, null /* extension */, null /* classifier */);
    hashGAV.setCreateTime(createTime);
    HashGAVDAO hashGAVDAO = new HashGAVDAO();
    hashGAVDAO.insert(hashGAV);
    String serviceUrl = getScanUrl("simple", applicationPublicId, hash, null, null, null, null, "false" /* proprietary */);
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    MatchedComponent saasResponse = new MatchedComponent();
    saasResponse.setHash(hash);
    saasResponse.addSecurityVulnerability(new SecurityVulnerability("12345", "osvdb", 5f));
    setSaasResponseForURI(saasUrl, JsonHelpers.asJson(saasResponse), 200);
    Response response = AuthedRestAccess.get(serviceUrl);
    hashGAVDAO.delete(hashGAV);
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(),
        IdeMatchedComponent.class);
    Assert.assertEquals(groupId, ideMatchedComponent.getGroupId());
    Assert.assertEquals(artifactId, ideMatchedComponent.getArtifactId());
    Assert.assertEquals(version, ideMatchedComponent.getVersion());
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
    Application app = createApplication(applicationPublicId);
    Label label = new Label(orgLabel ? app.getOrganizationId() : app.getId(), "red", null);
    new LabelDAO().insert(label);
    new ComponentLabelDAO().insert(new ComponentLabel(orgComponentLabel ? app.getOrganizationId() : app.getId(), label
        .getId(), hash));

    Constraint constraint1 = new Constraint("C1", "Constraint 1", LogicalOperator.AND);
    constraint1.addCondition(new Condition(LabelConditionType.ID, "is", label.getId()));
    Policy policy1 = new Policy("PolicyId1", "Policy Name 1");
    policy1.setThreatLevel(8);
    policy1.addConstraint(constraint1);
    Action failAction = new Action(FailActionType.ID);
    policy1.addAction(BuildStageType.ID, failAction);
    addPolicy(applicationPublicId, policy1);

    String serviceUrl = getScanSimpleUrl(applicationPublicId, hash);
    String saasUrl = convertToSaasUrl(serviceUrl, applicationPublicId);
    setSaasResponseForURI(saasUrl, 200, "/IdeResourceTest/SimpleMatch_abababababababababab.json");
    Response response = AuthedRestAccess.get(serviceUrl);
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = JsonHelpers.fromJson(response.getResponseBody(),
        IdeMatchedComponent.class);
    Assert.assertThat(ideMatchedComponent.getGroupId(), is("g1"));
    Assert.assertThat(ideMatchedComponent.getArtifactId(), is("a1"));
    Assert.assertThat(ideMatchedComponent.getVersion(), is("v1"));
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
    setSaasResponseForURI("rest/ide/artifact/versions?groupId=gid&artifactId=aid", "[\"1.1\", \"2.0\"]", 200);
    Response response = AuthedRestAccess.get(getComponentVersionsUrl("gid", "aid"));
    assertResponseStatus(200, response);
    String[] versions = JsonHelpers.fromJson(response.getResponseBody(), String[].class);
    Assert.assertEquals(Arrays.asList("1.1", "2.0"), Arrays.asList(versions));
  }

  @Test
  public void testGetComponentVersions_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = AuthedRestAccess.get(getComponentVersionsUrl("ulg", "ula"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentVersions_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    Response response = AuthedRestAccess.get(getComponentVersionsUrl("ulg", "ula"));
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetAsset() throws Exception {
    setSaasResponseForURI("ide/sub/dir/some%20space.html?x=y&a=b", "OK", 200);
    Response response = RestAccess.get(getServiceURL() + "/asset/sub/dir/some%20space.html?x=y&a=b");
    assertResponseStatus(200, response);
    Assert.assertEquals("OK", response.getResponseBody());
  }

  @Test
  public void testGetAsset_Unlicensed() throws Exception {
    uninstallLicense();
    Response response = RestAccess.get(getServiceURL() + "/asset/sub/dir/some%20space.html?x=y&a=b");
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetAsset_EnforcementPointUnlicensed() throws Exception {
    // note this enforcement point should not apply to this request
    setEnforcementPoints(CLMEnforcementPoint.StageRelease);

    Response response = RestAccess.get(getServiceURL() + "/asset/sub/dir/some%20space.html?x=y&a=b");
    assertResponseStatus(402, response);
  }

  private String convertToSaasUrl(String brainUrl, String applicationId) {
    return brainUrl.substring(getRestBaseUrl().length()).replace("/" + applicationId, "");
  }

  private String getServiceURL() {
    return getRestBaseUrl() + IdeResource.SERVICE_PATH;
  }

  private String getScanSimpleUrl(String applicationPublicId, String hash) {
    return getScanUrl("simple", applicationPublicId, hash, null, null, null, null, null);
  }

  private String getScanEnhancedUrl(String applicationPublicId, String hash) {
    return getScanUrl("enhanced", applicationPublicId, hash, null, null, null, null, null);
  }

  private String getScanUrl(String mode, String applicationPublicId, String hash, String filename, String groupId,
      String artifactId, String version, String proprietary)
  {
    return getServiceURL()
        + "/scan/"
        + mode
        + "/"
        + applicationPublicId
        + "/"
        + hash
        + getQueryParams("filename", filename, "groupId", groupId, "artifactId", artifactId, "version", version,
            "proprietary", proprietary);
  }

  private String getComponentVersionsUrl(String groupId, String artifactId) {
    return getServiceURL() + "/component/versions/?groupId=" + groupId + "&artifactId=" + artifactId;
  }

  private String getQueryParams(String... params) {
    if (params.length % 2 != 0) {
      throw new IllegalArgumentException("query parameter mismatch");
    }

    StringBuilder buffer = new StringBuilder(256);
    for (int i = 0; i < params.length - 1; i += 2) {
      String param = params[i];
      String value = params[i + 1];
      if (value != null && !value.isEmpty()) {
        buffer.append((buffer.length() > 0) ? '&' : '?');
        buffer.append(param).append('=').append(value);
      }
    }
    return buffer.toString();
  }
}
