/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.ide;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import jakarta.mail.MessagingException;
import jakarta.mail.util.ByteArrayDataSource;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.IdeMatchedComponent;
import com.sonatype.clm.dto.model.ide.MatchedComponent;
import com.sonatype.clm.dto.model.ide.ScannedComponent;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.hds.HdsClientAnalytics;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.ScanTriggerType;
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
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.ClientUserAgentUtil;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.jaxrs.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.mock.hds.HttpResponseProcessor;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Before;
import org.junit.Test;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

@Category(SlowTest.class)
public class IdeResourceTest
    extends AbstractResourceTest
{
  private HashComponentIdentifierDAO hashComponentIdentifierDAO;

  private Configuration configurationService;

  private Application app = null;

  @Before
  public void setUp() {
    hashComponentIdentifierDAO = lookup(HashComponentIdentifierDAO.class);
    configurationService = lookup(Configuration.class);

    assertThat(configurationService.isALPObservedLicenseDetectionEnabled()).isTrue();
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

  private HttpRequest coordinatesScanRequest(String appId, ComponentIdentifier identifier) {
    return restRequest()
        .path(IdeResource.COORDINATES_SCAN_PATH)
        .parameter(appId)
        .query("componentIdentifier", identifier);
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
    if (brainUrl.contains("rest/ide/scan/coordinates")) {
      return brainUrl.replaceFirst("(.*/)(rest/ide/scan/coordinates)(/[^?]+)(.*)", "$2$4");
    }
    return brainUrl.replaceFirst("(.*/)(rest/ide/scan/[^/]+)(/[^/]+)(/.*)", "$2$4");
  }

  private String convertVersionsUrlToHdsUrl(String brainUrl) {
    return brainUrl.replaceFirst("(.*/)(rest/ide/component/versions)(.*)", "rest/ide/artifact/versions$3");
  }

  @Test
  public void testDoScan_Simple() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    app = tempEntity.newApplicationWithParent(applicationPublicId);

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
    app = tempEntity.newApplicationWithParent(applicationPublicId);

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
  public void testDoCoordinatesScan_ExactMatch() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "", "jar");
    HttpRequest request = coordinatesScanRequest(applicationPublicId, componentIdentifier);
    mockHdsScanResponse(request, 200, "Coordinates.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    List<IdeMatchedComponent> results =
        JsonUtils.parse(response.getBodyText(), new TypeReference<List<IdeMatchedComponent>>()
        {
        });
    assertThat(results).hasSize(1);

    IdeMatchedComponent ideMatchedComponent = results.get(0);
    assertThat(ideMatchedComponent.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertGavInIdeMatchedComponent("tomcat", "tomcat-util", "5.5.23", ideMatchedComponent);
    assertThat(ideMatchedComponent.getHash()).isEqualTo("1249e25aebb15358bedd");
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("exact");
    assertThat(ideMatchedComponent.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    List<PolicyAlert> policyAlerts = ideMatchedComponent.getAlerts();
    assertThat(policyAlerts).hasSize(4);
  }

  @Test
  public void testDoCoordinatesScan_UnknownMatch() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(MatchStateConditionType.ID, "is", "unknown");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "tom", "tom", "0.1", "", "jar");
    HttpRequest request = coordinatesScanRequest(applicationPublicId, componentIdentifier);
    mockHdsScanResponse(request, 200, "Coordinates_unknown.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    List<IdeMatchedComponent> results =
        JsonUtils.parse(response.getBodyText(), new TypeReference<List<IdeMatchedComponent>>()
        {
        });
    assertThat(results).hasSize(1);

    IdeMatchedComponent ideMatchedComponent = results.get(0);
    assertThat(ideMatchedComponent.getComponentIdentifier()).isNull();
    assertThat(ideMatchedComponent.getHash()).isNull();
    assertThat(ideMatchedComponent.getMatchState()).isEqualTo("unknown");
    assertThat(ideMatchedComponent.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertThat(ideMatchedComponent.isSimpleMatch()).isTrue();
    assertThat(ideMatchedComponent.getAlerts()).hasSize(1);
  }

  @Test
  public void testDoCoordinatesScan_MultipleComponents_attributesAlertsPerComponent() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "", "jar");
    HttpRequest request = coordinatesScanRequest(applicationPublicId, componentIdentifier);
    mockHdsScanResponse(request, 200, "Coordinates_multiple.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    List<IdeMatchedComponent> results =
        JsonUtils.parse(response.getBodyText(), new TypeReference<List<IdeMatchedComponent>>()
        {
        });
    assertThat(results).hasSize(4);

    Map<String, IdeMatchedComponent> resultsByHash =
        results.stream().collect(toMap(IdeMatchedComponent::getHash, Function.identity()));

    IdeMatchedComponent tomcat = resultsByHash.get("1249e25aebb15358bedd");
    assertThat(tomcat.getAlerts()).hasSize(4);
    assertThat(tomcat.getAlerts())
        .allSatisfy(alert -> assertThat(alert.getTrigger().getComponentFacts().get(0).getHash())
            .isEqualTo("1249e25aebb15358bedd"));

    IdeMatchedComponent commons = resultsByHash.get("cccccccccccccccccccc");
    assertThat(commons.getAlerts()).hasSize(1);
    assertThat(commons.getAlerts())
        .allSatisfy(alert -> assertThat(alert.getTrigger().getComponentFacts().get(0).getHash())
            .isEqualTo("cccccccccccccccccccc"));

    IdeMatchedComponent safe = resultsByHash.get("dddddddddddddddddddd");
    assertThat(safe.getAlerts()).isNotNull().isEmpty();

    // This component carries a severity-9.0 vulnerability but has a positive waitDelta, so it must be excluded from
    // the evaluation batch. No alerts here proves needsEvaluation-false components are skipped; keep the vuln in the
    // fixture so a regression that wrongly evaluates it would surface as an unexpected alert.
    IdeMatchedComponent pending = resultsByHash.get("ffffffffffffffffffff");
    assertThat(pending.getWaitDelta()).isPositive();
    assertThat(pending.getAlerts()).isNullOrEmpty();
  }

  @Test
  public void testDoCoordinatesScan_SameHashDifferentIdentifiers_attributesAlertsByIdentity() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "dup", "lib-a", "1.0", "", "jar");
    HttpRequest request = coordinatesScanRequest(applicationPublicId, componentIdentifier);
    mockHdsScanResponse(request, 200, "Coordinates_sameHash.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    List<IdeMatchedComponent> results =
        JsonUtils.parse(response.getBodyText(), new TypeReference<List<IdeMatchedComponent>>()
        {
        });
    assertThat(results).hasSize(2);

    ComponentIdentifier idA = ComponentIdentifier.createMavenCoordinates("dup", "lib-a", "1.0", "", "jar");
    ComponentIdentifier idB = ComponentIdentifier.createMavenCoordinates("dup", "lib-b", "1.0", "", "jar");

    IdeMatchedComponent libA =
        results.stream().filter(r -> idA.equals(r.getComponentIdentifier())).findFirst().orElseThrow();
    assertThat(libA.getAlerts()).hasSize(2);
    assertThat(libA.getAlerts())
        .allSatisfy(alert -> assertThat(alert.getTrigger().getComponentFacts().get(0).getComponentIdentifier())
            .isEqualTo(idA));

    IdeMatchedComponent libB =
        results.stream().filter(r -> idB.equals(r.getComponentIdentifier())).findFirst().orElseThrow();
    assertThat(libB.getAlerts()).hasSize(1);
    assertThat(libB.getAlerts())
        .allSatisfy(alert -> assertThat(alert.getTrigger().getComponentFacts().get(0).getComponentIdentifier())
            .isEqualTo(idB));
  }

  @Test
  public void testDoCoordinatesScan_ManuallyClaimedComponent_clearsVulnerabilitiesWithoutAffectingBatch() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    String claimedHash = "cccccccccccccccccccc";
    ComponentIdentifier claimedIdentifier = ComponentIdentifier.createMavenCoordinates("claimed", "claimed-lib", "9.9");
    HashComponentIdentifier hashComponentIdentifier = new HashComponentIdentifier(claimedHash, claimedIdentifier);
    hashComponentIdentifier.setCreateTime(new Date());
    hashComponentIdentifierDAO.insert(hashComponentIdentifier);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "tomcat", "tomcat-util", "5.5.23", "", "jar");
    HttpRequest request = coordinatesScanRequest(applicationPublicId, componentIdentifier);
    mockHdsScanResponse(request, 200, "Coordinates_multiple.json");
    HttpResponse response = request.get();
    hashComponentIdentifierDAO.delete(hashComponentIdentifier);
    assertResponseStatus(200, response);
    List<IdeMatchedComponent> results =
        JsonUtils.parse(response.getBodyText(), new TypeReference<List<IdeMatchedComponent>>()
        {
        });
    assertThat(results).hasSize(4);

    Map<String, IdeMatchedComponent> resultsByHash =
        results.stream().collect(toMap(IdeMatchedComponent::getHash, Function.identity()));

    IdeMatchedComponent claimed = resultsByHash.get(claimedHash);
    assertThat(claimed.getComponentIdentifier()).isEqualTo(claimedIdentifier);
    assertThat(claimed.getMatchState()).isEqualTo(MatchState.EXACT.getId());
    assertThat(claimed.getIdentificationSource()).isEqualTo(IdentificationSource.MANUAL.getId());
    assertThat(claimed.getAlerts()).isNotNull().isEmpty();

    IdeMatchedComponent tomcat = resultsByHash.get("1249e25aebb15358bedd");
    assertThat(tomcat.getIdentificationSource()).isEqualTo(IdentificationSource.SONATYPE.getId());
    assertThat(tomcat.getAlerts()).hasSize(4);
    assertThat(tomcat.getAlerts())
        .allSatisfy(alert -> assertThat(alert.getTrigger().getComponentFacts().get(0).getHash())
            .isEqualTo("1249e25aebb15358bedd"));
  }

  @Test
  public void testDoCoordinatesScan_EmptyComponentList_returnsEmptyResult() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "none", "none", "0", "", "jar");
    HttpRequest request = coordinatesScanRequest(applicationPublicId, componentIdentifier);
    mockHdsScanResponse(request, 200, "Coordinates_empty.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    List<IdeMatchedComponent> results =
        JsonUtils.parse(response.getBodyText(), new TypeReference<List<IdeMatchedComponent>>()
        {
        });

    assertThat(results).isEmpty();
  }

  @Test
  public void testDoCoordinatesScan_AllComponentsPending_skipsEvaluationAndReturnsNoAlerts() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(SecurityVulnerabilitySeverityConditionType.ID, ">=", "0");
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "pending", "pending-a", "1.0.0", "", "jar");
    HttpRequest request = coordinatesScanRequest(applicationPublicId, componentIdentifier);
    mockHdsScanResponse(request, 200, "Coordinates_allPending.json");
    HttpResponse response = request.get();
    assertResponseStatus(200, response);
    List<IdeMatchedComponent> results =
        JsonUtils.parse(response.getBodyText(), new TypeReference<List<IdeMatchedComponent>>()
        {
        });

    // Every component is pending, so candidates is empty and the batched evaluate call is skipped entirely. The
    // severity-9.0/8.0 vulns would alert if any were wrongly evaluated.
    assertThat(results).hasSize(2);
    assertThat(results).allSatisfy(result -> {
      assertThat(result.getWaitDelta()).isPositive();
      assertThat(result.getAlerts()).isNullOrEmpty();
    });
  }

  @Test
  public void testGroupAlertsByComponent_alertWithoutComponentFacts_isSkippedNotRouted() {
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g", "a", "1.0");
    PolicyFact triggerWithFact = new PolicyFact();
    triggerWithFact.setComponentFacts(List.of(new ComponentFact(identifier, "aaaaaaaaaaaaaaaaaaaa")));
    PolicyAlert routable = new PolicyAlert(triggerWithFact, List.of());

    PolicyAlert nullTrigger = new PolicyAlert(null, List.of());
    PolicyFact triggerNoFacts = new PolicyFact();
    triggerNoFacts.setComponentFacts(Collections.emptyList());
    PolicyAlert emptyFacts = new PolicyAlert(triggerNoFacts, List.of());

    var grouped = IdeResource.groupAlertsByComponent(List.of(routable, nullTrigger, emptyFacts));

    assertThat(grouped.values()).hasSize(1);
    assertThat(grouped.values().iterator().next()).containsExactly(routable);
  }

  @Test
  public void testDoCoordinatesScan_Unlicensed() throws Exception {
    uninstallLicense();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "tom", "tom", "0.1", "", "jar");
    HttpResponse response = coordinatesScanRequest("unlicensedAppId", componentIdentifier).get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoCoordinatesScan_FeatureUnlicensed() throws Exception {
    setMissingFeature(LicensedFeature.IDE_INTEGRATION);
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "tom", "tom", "0.1", "", "jar");
    HttpResponse response = coordinatesScanRequest("unlicensedAppId", componentIdentifier).get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testDoScan_Enhanced() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    app = tempEntity.newApplicationWithParent(applicationPublicId);

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
    app = tempEntity.newApplicationWithParent(applicationPublicId);

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
    app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(LicenseConditionType.ID, "is", "GPL-2.0");
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
    assertThat(policyAlerts).isEmpty();

    // Override the license and evaluate the policy again
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar");
    tempEntity.newLicenseOverride(app.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
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
    app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition =
        new Condition(LicenseStatusConditionType.ID, "is", LicenseOverrideStatus.OVERRIDDEN.toString());
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
    assertThat(policyAlerts).isEmpty();

    // Override the license and evaluate the policy again
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", null, "jar");
    tempEntity.newLicenseOverride(app.getId(), componentIdentifier, LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0",
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
    app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(SecurityVulnerabilityStatusConditionType.ID, "is",
        SecurityVulnerabilityOverrideStatus.ACKNOWLEDGED.getId());
    tempEntity.newPolicy(app, 8, LogicalOperator.AND, condition);

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
    tempEntity.newSecurityVulnerabilityOverride(app.getId(), ideMatchedComponent.getHash(), "osvdb", "121212",
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
    tempEntity.newSecurityVulnerabilityOverride(app.getId(), ideMatchedComponent.getHash(), "osvdb", "36079",
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
    app = tempEntity.newApplicationWithParent(applicationPublicId);

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
    app = tempEntity.newApplicationWithParent(applicationPublicId);

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
    app = tempEntity.newApplicationWithParent(applicationPublicId);

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
    app = tempEntity.newApplicationWithParent(applicationPublicId);

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
    app = tempEntity.newApplicationWithParent(applicationPublicId);

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
    app = tempEntity.newApplicationWithParent(applicationPublicId);

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
    app = tempEntity.newApplicationWithParent(applicationPublicId);
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
  public void testDoScan_HiddenObservedLicense() throws Exception {
    String applicationPublicId = "IdeResourceTest_AppId";
    app = tempEntity.newApplicationWithParent(applicationPublicId);

    Condition condition = new Condition(LicenseConditionType.ID, "is", License.NOT_SUPPORTED_ID);
    Policy policy = tempEntity.newPolicy(app, 10, LogicalOperator.AND, condition);

    ComponentIdentifier componentIdentifier = ComponentIdentifier.createNpmCoordinates("p", "v");
    HttpRequest request = simpleScanRequest(applicationPublicId, "hash").query("proprietary", false);

    MatchedComponent hdsResponse = new MatchedComponent();
    hdsResponse.setHash("hash");
    hdsResponse.setComponentIdentifier(componentIdentifier);
    hdsResponse.addDeclaredLicenseId("MIT");
    hdsResponse.addObservedLicenseId("Apache-2.0");

    mockHdsResponse(request, hdsResponse, 200);

    configurationService.setALPObservedLicenseDetectionEnabled(false);
    try {
      HttpResponse response = request.get();
      assertResponseStatus(200, response);
      IdeMatchedComponent result = response.getBody(IdeMatchedComponent.class);

      assertThat(result).isNotNull();
      assertThat(result.getAlerts()).extracting(PolicyAlert::getTrigger)
          .extracting(PolicyFact::getPolicyName)
          .containsExactly(policy.getName());
    }
    finally {
      configurationService.setALPObservedLicenseDetectionEnabled(true);
    }
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
  private void assertGavInIdeMatchedComponent(
      String groupId,
      String artifactId,
      String version,
      IdeMatchedComponent ideMatchedComponent)
  {
    assertThat(ideMatchedComponent.getGroupId()).isEqualTo(groupId);
    assertThat(ideMatchedComponent.getArtifactId()).isEqualTo(artifactId);
    assertThat(ideMatchedComponent.getVersion()).isEqualTo(version);
  }

  @Test
  public void testSendTelemetry() throws Exception {
    String appId = "IdeResourceTest_AppId";
    String instanceId = "my-unique-id";
    String userAgent = "Sonatype_CLM_CI_Jenkins/3.13 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)";
    Integer mavenComponents = 10;
    Integer npmComponents = 5;
    app = tempEntity.newApplicationWithParent(appId);

    // Send request
    Map<String, Object> telemetryAttributes = sendTelemetryRequestAndGetTelemetryAttributes(
        appId,
        instanceId,
        userAgent,
        mavenComponents,
        npmComponents);

    // Assert Result
    assertApplicationEvaluationComponentTelemetryData(telemetryAttributes, mavenComponents, npmComponents);
    assertUserAgentData(telemetryAttributes, userAgent);
    assertThat(telemetryAttributes).containsEntry("client_instance_id", instanceId);
  }

  @Test
  public void testSendTelemetry_NoInstanceId() throws Exception {
    String appId = "IdeResourceTest_AppId";
    String userAgent = "Sonatype_CLM_CI_Jenkins/3.13 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)";
    Integer mavenComponents = 10;
    Integer npmComponents = 5;
    app = tempEntity.newApplicationWithParent(appId);

    // Send request
    Map<String, Object> telemetryAttributes = sendTelemetryRequestAndGetTelemetryAttributes(
        appId,
        null,
        userAgent,
        mavenComponents,
        npmComponents);

    // Assert Result
    assertApplicationEvaluationComponentTelemetryData(telemetryAttributes, mavenComponents, npmComponents);
    assertUserAgentData(telemetryAttributes, userAgent);
    assertThat(telemetryAttributes.get("client_instance_id")).isNull();
  }

  @Test
  public void testSendTelemetry_NoUserAgent() throws Exception {
    String appId = "IdeResourceTest_AppId";
    String instanceId = "my-unique-id";
    Integer mavenComponents = 10;
    Integer npmComponents = 5;
    app = tempEntity.newApplicationWithParent(appId);

    // Send request
    Map<String, Object> telemetryAttributes = sendTelemetryRequestAndGetTelemetryAttributes(
        appId,
        instanceId,
        null,
        mavenComponents,
        npmComponents);

    // Assert Result
    assertApplicationEvaluationComponentTelemetryData(telemetryAttributes, mavenComponents, npmComponents);
    assertUserAgentDataIsNotSent(telemetryAttributes);
    assertThat(telemetryAttributes).containsEntry("client_instance_id", instanceId);
  }

  @Test
  public void testSendTelemetry_NoComponents() throws Exception {
    String appId = "IdeResourceTest_AppId";
    String instanceId = "my-unique-id";
    String userAgent = "Sonatype_CLM_CI_Jenkins/3.13 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)";
    Integer mavenComponents = null;
    Integer npmComponents = null;
    app = tempEntity.newApplicationWithParent(appId);

    // Send request
    Map<String, Object> telemetryAttributes = sendTelemetryRequestAndGetTelemetryAttributes(
        appId,
        instanceId,
        userAgent,
        mavenComponents,
        npmComponents);

    // Assert Result
    assertApplicationEvaluationComponentTelemetryData(telemetryAttributes, mavenComponents, npmComponents);
    assertUserAgentData(telemetryAttributes, userAgent);
    assertThat(telemetryAttributes).containsEntry("client_instance_id", instanceId);
  }

  @Test
  public void testSendTelemetry_NoInstanceId_NoUserAgent() throws Exception {
    String appId = "IdeResourceTest_AppId";
    Integer mavenComponents = 10;
    Integer npmComponents = 5;
    app = tempEntity.newApplicationWithParent(appId);

    // Send request
    Map<String, Object> telemetryAttributes = sendTelemetryRequestAndGetTelemetryAttributes(
        appId,
        null,
        null,
        mavenComponents,
        npmComponents);

    // Assert Result
    assertApplicationEvaluationComponentTelemetryData(telemetryAttributes, mavenComponents, npmComponents);
    assertUserAgentDataIsNotSent(telemetryAttributes);
    assertThat(telemetryAttributes.get("client_instance_id")).isNull();
  }

  @Test
  public void testSendTelemetry_NoInstanceId_NoUserAgent_NoComponents() throws Exception {
    String appId = "IdeResourceTest_AppId";
    Integer mavenComponents = null;
    Integer npmComponents = null;
    app = tempEntity.newApplicationWithParent(appId);

    // Send request
    Map<String, Object> telemetryAttributes = sendTelemetryRequestAndGetTelemetryAttributes(
        appId,
        null,
        null,
        mavenComponents,
        npmComponents);

    // Assert Result
    assertApplicationEvaluationComponentTelemetryData(telemetryAttributes, mavenComponents, npmComponents);
    assertUserAgentDataIsNotSent(telemetryAttributes);
    assertThat(telemetryAttributes.get("client_instance_id")).isNull();
  }

  @Test
  public void testSendTelemetryV2() throws Exception {
    String appId = "IdeResourceTest_AppId";
    String instanceId = "my-unique-id";
    String userAgent = "Sonatype_CLM_CI_Jenkins/3.13 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)";
    Integer mavenComponents = 15;
    Integer npmComponents = 10;
    String attribute = "ide_theme";
    String attributeValue = "dark";
    app = tempEntity.newApplicationWithParent(appId);

    // Send request
    Map<String, Object> telemetryAttributes = sendTelemetryRequestAndGetTelemetryAttributesV2(
        appId,
        instanceId,
        userAgent,
        mavenComponents,
        npmComponents,
        attribute,
        attributeValue);

    // Assert Result
    assertThat(telemetryAttributes).containsEntry("ide_theme", "dark");
    assertApplicationEvaluationComponentTelemetryData(telemetryAttributes, mavenComponents, npmComponents);
    assertUserAgentData(telemetryAttributes, userAgent);
    assertThat(telemetryAttributes).containsEntry("client_instance_id", instanceId);
  }

  @Test
  public void testSendTelemetryV2_NoAttribute() throws Exception {
    String appId = "IdeResourceTest_AppId";
    String instanceId = "my-unique-id";
    String userAgent = "Sonatype_CLM_CI_Jenkins/3.13 (Java 1.8.0_201; Linux 5.4.144; Jenkins 2.319.2)";
    Integer mavenComponents = 15;
    Integer npmComponents = 10;
    app = tempEntity.newApplicationWithParent(appId);

    // Send request
    Map<String, Object> telemetryAttributes = sendTelemetryRequestAndGetTelemetryAttributesV2(
        appId,
        instanceId,
        userAgent,
        mavenComponents,
        npmComponents,
        null,
        null);

    // Assert Result
    assertThat(telemetryAttributes).doesNotContainEntry("ide_theme", "dark");
    assertApplicationEvaluationComponentTelemetryData(telemetryAttributes, mavenComponents, npmComponents);
    assertUserAgentData(telemetryAttributes, userAgent);
    assertThat(telemetryAttributes).containsEntry("client_instance_id", instanceId);
  }

  private Map<String, Object> sendTelemetryRequestAndGetTelemetryAttributes(
      final String appId,
      final String instanceId,
      final String userAgent,
      final Integer mavenComponents,
      final Integer npmComponents) throws Exception
  {
    // Setup telemetry data collection
    final Map<ByteArrayDataSource, Integer> responses = getHdsTelemetryDataCollection();

    // Prepare request
    Map<String, Integer> componentCounts = getComponentCounts(mavenComponents, npmComponents);
    HttpRequest request = sendTelemetryRequest(componentCounts, appId, instanceId, userAgent);

    // Send request
    HttpResponse response = request.post();

    // Assert result and telemetry purpose
    assertResponseStatus(204, response);
    return assertTelemetryPurposeIsFound(responses, TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS);
  }

  private Map<String, Object> sendTelemetryRequestAndGetTelemetryAttributesV2(
      final String appId,
      final String instanceId,
      final String userAgent,
      final Integer mavenComponents,
      final Integer npmComponents,
      final String attribute,
      final String attributeValue) throws Exception
  {
    // Setup telemetry data collection
    final Map<ByteArrayDataSource, Integer> responses = getHdsTelemetryDataCollection();

    // Prepare request
    Map<String, Object> telemetryRequest =
        getTelemetryRequestV2(attribute, attributeValue, getComponentCounts(mavenComponents, npmComponents));
    HttpRequest request = sendTelemetryRequestV2(telemetryRequest, appId, instanceId, userAgent);

    // Send request
    HttpResponse response = request.post();

    // Assert result and telemetry purpose
    assertResponseStatus(204, response);
    return assertTelemetryPurposeIsFound(responses, TelemetryPurpose.APPLICATION_EVALUATION_COMPONENT_COUNTS);
  }

  private Map<ByteArrayDataSource, Integer> getHdsTelemetryDataCollection() throws Exception {
    final Map<ByteArrayDataSource, Integer> responses = Collections.synchronizedMap(new LinkedHashMap<>());
    startIqTestServer(config -> getHdsServer()
        .respondWith((HttpResponseProcessor) (request, response) -> {
          // Read the input stream into a byte array first to ensure it's fully consumed
          // before creating the ByteArrayDataSource (required for Jetty 12/Jakarta EE 11)
          byte[] requestBody = request.getInputStream().readAllBytes();
          responses.put(new ByteArrayDataSource(requestBody, "multipart/form-data"),
              response.getStatus());
        })
        .andStatus(204)
        .atUri(TelemetrySender.RESOURCE_PATH));
    return responses;
  }

  private Map<String, Integer> getComponentCounts(final Integer mavenComponents, final Integer npmComponents) {
    Map<String, Integer> componentCounts = new HashMap<>();
    if (mavenComponents != null) {
      componentCounts.put("maven", mavenComponents);
    }
    if (npmComponents != null) {
      componentCounts.put("npm", npmComponents);
    }
    return componentCounts;
  }

  private Map<String, Object> getTelemetryRequestV2(
      String attribute,
      String attributeValue,
      Map<String, Integer> componentCounts)
  {
    Map<String, Object> telemetryRequest = new HashMap<>();
    Optional.ofNullable(attribute).ifPresent(attr -> telemetryRequest.put(attr, attributeValue));
    telemetryRequest.put("component_counts", componentCounts);
    return telemetryRequest;
  }

  private HttpRequest sendTelemetryRequest(
      final Map<String, Integer> componentCounts,
      final String appId,
      final String instanceId,
      final String userAgent)
  {
    Map<String, String> headers = new HashMap<>();
    headers.put(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, userAgent);
    headers.put(HdsClient.CLIENT_INSTANCE_ID_HEADER, instanceId);
    return restRequest().path("telemetry", appId).headers(headers).body(componentCounts);
  }

  private HttpRequest sendTelemetryRequestV2(
      final Map<String, Object> telemetryRequest,
      final String appId,
      final String instanceId,
      final String userAgent)
  {
    Map<String, String> headers = new HashMap<>();
    headers.put(HdsClient.CLM_CLIENT_USER_AGENT_HEADER, userAgent);
    headers.put(HdsClient.CLIENT_INSTANCE_ID_HEADER, instanceId);
    return restRequest().path("v2/telemetry", appId).headers(headers).body(telemetryRequest);
  }

  private Map<String, Object> assertTelemetryPurposeIsFound(
      final Map<ByteArrayDataSource, Integer> responses,
      final TelemetryPurpose purpose) throws MessagingException, IOException
  {
    // Wait for the specific telemetry purpose to be found (telemetry is sent asynchronously)
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
      Map<TelemetryPurpose, List<TelemetryItem>> telemetry = getTelemetryItemsByPurpose(responses);
      assertThat(telemetry.get(purpose)).isNotNull();
      assertThat(telemetry.get(purpose)).isNotEmpty();
    });

    // Getting the telemetry data
    Map<TelemetryPurpose, List<TelemetryItem>> collectedTelemetry = getTelemetryItemsByPurpose(responses);
    List<TelemetryItem> telemetryItems = collectedTelemetry.get(purpose);

    return telemetryItems.get(0).getTelemetryData().get(0).getAttributes();
  }

  private void assertApplicationEvaluationComponentTelemetryData(
      final Map<String, Object> telemetryAttributes,
      Integer mavenComponents,
      Integer npmComponents)
  {
    assertThat(telemetryAttributes).containsEntry("application_id", HdsClientAnalytics.obfuscate(app.getId()));
    assertThat(telemetryAttributes).containsEntry("real_application_id", app.getId());
    assertThat(telemetryAttributes).containsEntry("stage_id", Stage.ID_DEVELOP);
    assertThat(telemetryAttributes).containsEntry("scan_trigger_type", ScanTriggerType.IDE.getId());

    if (mavenComponents != null) {
      assertThat(telemetryAttributes).containsEntry("number_of_maven_components", mavenComponents.toString());
    }
    else {
      mavenComponents = 0;
      assertThat(telemetryAttributes.get("number_of_maven_components")).isNull();
    }

    if (npmComponents != null) {
      assertThat(telemetryAttributes).containsEntry("number_of_npm_components", npmComponents.toString());
    }
    else {
      npmComponents = 0;
      assertThat(telemetryAttributes.get("number_of_npm_components")).isNull();
    }

    int total = mavenComponents + npmComponents;
    assertThat(telemetryAttributes).containsEntry("number_of_components", Integer.toString(total));
  }

  private void assertUserAgentData(Map<String, Object> telemetryAttributes, String userAgent) {
    ClientUserAgentUtil.UserAgent userAgentData = ClientUserAgentUtil.parse(userAgent);
    assertThat(telemetryAttributes).containsEntry("client_id", userAgentData.client);
    assertThat(telemetryAttributes).containsEntry("client_version", userAgentData.clientVersion);
    assertThat(telemetryAttributes).containsEntry("client_runtime", userAgentData.runtime);
    assertThat(telemetryAttributes).containsEntry("client_runtime_version", userAgentData.runtimeVersion);
    assertThat(telemetryAttributes).containsEntry("client_os_name", userAgentData.os);
    assertThat(telemetryAttributes).containsEntry("client_os_version", userAgentData.osVersion);
    assertThat(telemetryAttributes).containsEntry("client_other", userAgentData.other);
  }

  private void assertUserAgentDataIsNotSent(Map<String, Object> telemetryAttributes) {
    assertThat(telemetryAttributes.get("client_id")).isNull();
    assertThat(telemetryAttributes.get("client_version")).isNull();
    assertThat(telemetryAttributes.get("client_runtime")).isNull();
    assertThat(telemetryAttributes.get("client_runtime_version")).isNull();
    assertThat(telemetryAttributes.get("client_os_name")).isNull();
    assertThat(telemetryAttributes.get("client_os_version")).isNull();
    assertThat(telemetryAttributes.get("client_other")).isNull();
  }

  private Map<TelemetryPurpose, List<TelemetryItem>> getTelemetryItemsByPurpose(
      final Map<ByteArrayDataSource, Integer> responses) throws MessagingException, IOException
  {
    await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> assertThat(responses).isNotEmpty());
    return getTelemetryItems(responses).stream()
        .collect(groupingBy(telemetryItem -> telemetryItem.getTelemetryPurposes().get(0)));
  }
}
