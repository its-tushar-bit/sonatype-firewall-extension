/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

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
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.AgeInDaysConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseStatusConditionType;
import com.sonatype.insight.brain.model.policy.conditions.MatchStateConditionType;
import com.sonatype.insight.brain.model.policy.conditions.ProprietaryConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilitySeverityConditionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityStatusConditionType;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IdeResourceTest
    extends AbstractResourceTest
{
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
    hdsRespondWithResource("/IdeResourceTest/" + resource).andStatus(status).atUri(hdsUrl);
  }

  private void mockHdsResponse(HttpRequest request, Object body, int status) {
    String hdsUrl = convertScanUrlToHdsUrl(request.getUrl());
    hdsRespondWith(body).andStatus(status).atUri(hdsUrl);
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
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab");
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  @Test
  public void testDoScan_Simple_ByComponentIdentifier() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar");
    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab").query("componentIdentifer",
        componentIdentifier);
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  @Test
  public void testDoScan_Enhanced() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(MatchStateConditionType.ID, "is", "exact");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    HttpRequest request = enhancedScanRequest(applicationPublicId, "abababababababababab");
    mockHdsScanResponse(request, 202, "EnhancedMatch_wait.json");
    HttpResponse response = request.body(new ScannedComponent()).post();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getWaitDelta()).isPositive();

    mockHdsScanResponse(request, 200, "EnhancedMatch_abababababababababab.json");
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertThat(ideMatchedComponent.isSimpleMatch()).isFalse();
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  @Test
  public void testDoScan_Enhanced_ByComponentIdentifier() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(MatchStateConditionType.ID, "is", "exact");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar");
    HttpRequest request = enhancedScanRequest(applicationPublicId, "abababababababababab").query("componentIdentifer",
        componentIdentifier);
    mockHdsScanResponse(request, 202, "EnhancedMatch_wait.json");
    HttpResponse response = request.body(new ScannedComponent()).post();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getWaitDelta()).isPositive();

    mockHdsScanResponse(request, 200, "EnhancedMatch_abababababababababab.json");
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertThat(ideMatchedComponent.isSimpleMatch()).isFalse();
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  @Test
  public void testDoScan_OverriddenLicense() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(LicenseConditionType.ID, "is", "GPL-2.0");
    tempEntity.newPolicy(application, 8, LogicalOperator.AND, condition);

    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab");
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).isEmpty();

    // Override the license and evaluate the policy again
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar");
    tempEntity.newLicenseOverride(application.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        null /* comment */);
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  @Test
  public void testDoScan_LicenseStatus() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition =
        new Condition(LicenseStatusConditionType.ID, "is", LicenseOverrideStatus.OVERRIDDEN.toString());
    tempEntity.newPolicy(application, 8, LogicalOperator.AND, condition);

    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab");
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).isEmpty();

    // Override the license and evaluate the policy again
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar");
    tempEntity.newLicenseOverride(application.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
        null /* comment */);
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  @Test
  public void testDoScan_SecurityStatus() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application application = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(SecurityVulnerabilityStatusConditionType.ID, "is",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED.getId());
    tempEntity.newPolicy(application, 8, LogicalOperator.AND, condition);

    // There should be no policy alerts when none of the security vulnerabilities was overridden
    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab");
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).isEmpty();

    // Override the security vulnerabilities status for a security vulnerability that does not match and evaluate
    // the policy again. There should be no policy alerts.
    tempEntity.newSecurityVulnerabilityOverride(application.getId(), ideMatchedComponent.getHash(), "osvdb", "121212",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).isEmpty();

    // Override the security vulnerabilities status and evaluate the policy again. There should be one policy alert.
    tempEntity.newSecurityVulnerabilityOverride(application.getId(), ideMatchedComponent.getHash(), "osvdb", "36079",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED);
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  @Test
  public void testDoScan_Age() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(AgeInDaysConditionType.ID, "older than", "365");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab");
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  @Test
  public void testDoScan_unknown_simple() throws Exception {
    String hash = "000babababababababab";
    String applicationPublicId = "IdeResourceTest_AppId";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(MatchStateConditionType.ID, "is", "unknown");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    HttpRequest request = simpleScanRequest(applicationPublicId, hash);
    mockHdsScanResponse(request, 200, "SimpleMatch_000babababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getAlerts()).isNull();
  }

  @Test
  public void testDoScan_unknown_simple_enhancedResponse() throws Exception {
    String hash = "000babababababababab";
    String applicationPublicId = "IdeResourceTest_AppId";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(MatchStateConditionType.ID, "is", "unknown");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    HttpRequest request = simpleScanRequest(applicationPublicId, hash);
    mockHdsScanResponse(request, 200, "SimpleMatch_000babababababababab_enhanced.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  @Test
  public void testDoScan_simple_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = simpleScanRequest("unlicensedappId", "ulh").get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_FeatureUnlicensed() throws Exception {
    setMissingFeature(LicensedFeature.IDE_INTEGRATION);

    HttpResponse response = simpleScanRequest("unlicensedappId", "ulh").get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_unknown_enhanced() throws Exception {
    String hash = "000babababababababab";
    String applicationPublicId = "IdeResourceTest_AppId";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(MatchStateConditionType.ID, "is", "unknown");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    HttpRequest request = enhancedScanRequest(applicationPublicId, hash);
    mockHdsScanResponse(request, 200, "SimpleMatch_000babababababababab_enhanced.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  @Test
  public void testDoScan_enhanced_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = enhancedScanRequest("unlicensedappId", "ulh").get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_enhanced_FeatureUnlicensed() throws Exception {
    setMissingFeature(LicensedFeature.IDE_INTEGRATION);

    HttpResponse response = enhancedScanRequest("unlicensedappId", "ulh").get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_Proprietary() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(ProprietaryConditionType.ID, "is true");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    HttpRequest request = simpleScanRequest(applicationPublicId, "abababababababababab").query("proprietary", true);
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);

    request = enhancedScanRequest(applicationPublicId, "abababababababababab").query("proprietary", true);
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);

    request = simpleScanRequest(applicationPublicId, "abababababababababab").query("proprietary", false);
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    response = request.get();
    assertResponseStatus(200, response);
    ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("abababababababababab");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).isEmpty();
  }

  @Test
  public void testDoScan_ManuallyIdentifiedComponent() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    Application app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition1 = new Condition(MatchStateConditionType.ID, "is", "exact");
    Condition condition2 = new Condition(AgeInDaysConditionType.ID, "younger than", "30");
    Policy policy1 = tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition1, condition2);

    // This policy verifies that the SVs are wiped out from manually identified components. If they are not wiped out,
    // then there will be a policy violation for this policy and the assert on the policy violations at the end of the
    // test will fail.
    Condition condition3 = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition3);

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
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version));
    assertGavInIdeMatchedComponent(groupId, artifactId, version, ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo(hash);
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(ideMatchedComponent.getIdentificationSource()).isEqualTo(IdentificationSource.MANUAL.getId());
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);
    assertThat(policyAlerts.get(0).getTrigger().getPolicyName()).isEqualTo(policy1.getName());
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

    Condition condition = new Condition(LabelConditionType.ID, "is", label.getId());
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    HttpRequest request = simpleScanRequest(applicationPublicId, hash);
    mockHdsScanResponse(request, 200, "SimpleMatch_abababababababababab.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    IdeMatchedComponent ideMatchedComponent = response.getBody(IdeMatchedComponent.class);
    assertThat(ideMatchedComponent.getComponentIdentifier())
        .isEqualTo(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar"));
    assertGavInIdeMatchedComponent("g1", "a1", "v1", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo(hash);
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(1);
  }

  @Test
  public void testGetComponentVersions() throws Exception {
    HttpRequest request = versionsRequest("gid", "aid");
    hdsRespondWith("[\"1.1\", \"2.0\"]").atUri(convertVersionsUrlToHdsUrl(request.getUrl()));
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    String[] versions = response.getBody(String[].class);
    assertThat(versions).containsExactly("1.1", "2.0");
  }

  @Test
  public void testGetComponentVersions_ByComponentIdentifier() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("gid", "aid", null);
    HttpRequest request = versionsRequest(componentIdentifier);
    hdsRespondWith("[\"1.1\", \"2.0\"]").atUri(convertVersionsUrlToHdsUrl(request.getUrl()));
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    String[] versions = response.getBody(String[].class);
    assertThat(versions).containsExactly("1.1", "2.0");
  }

  @Test
  public void testGetComponentVersions_Unlicensed() throws Exception {
    uninstallLicense();
    HttpResponse response = versionsRequest("ulg", "ula").get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testGetComponentVersions_FeatureUnlicensed() throws Exception {
    setMissingFeature(LicensedFeature.IDE_INTEGRATION);

    HttpResponse response = versionsRequest("ulg", "ula").get();
    assertResponseStatus(402, response);
  }

  @SuppressWarnings("deprecation")
  private void assertGavInIdeMatchedComponent(String groupId,
                                              String artifactId,
                                              String version,
                                              IdeMatchedComponent ideMatchedComponent)
  {
    assertThat(ideMatchedComponent.getGroupId()).isEqualTo(groupId);
    assertThat(ideMatchedComponent.getArtifactId()).isEqualTo(artifactId);
    assertThat(ideMatchedComponent.getVersion()).isEqualTo(version);
  }
}
