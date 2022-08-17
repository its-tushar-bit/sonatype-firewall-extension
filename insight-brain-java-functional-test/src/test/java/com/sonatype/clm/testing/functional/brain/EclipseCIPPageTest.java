/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.clm.dto.model.component.ComponentDetailsList;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.VersionsCIP;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.EclipseCIPPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LicenseConditionType;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.util.UrlEncoded;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class EclipseCIPPageTest
    extends AbstractFunctionalTest
{
  private static final String SELECT_COMPONENT = "Select a component to view details.";

  private static final String JUNIT_DETAILS_FILE = "/canned-hds-responses/componentDetailsJunit.json";

  private static final String JUNIT_DETAILS_LIST_FILE = "/canned-hds-responses/componentDetailsListJunit.json";

  private static final String CATALINA_HOST_MANAGER_DETAILS_FILE =
      "/canned-hds-responses/componentDetailsCatalinaHostManager.json";

  private static final String CATALINA_HOST_MANAGER_DETAILS_LIST_FILE =
      "/canned-hds-responses/componentDetailsListCatalinaHostManager.json";

  private static final String ENTITY_FRAMEWORK_DETAILS_FILE =
      "/canned-hds-responses/componentDetailsEntityFramework.json";

  private static final String ENTITY_FRAMEWORK_DETAILS_LIST_FILE =
      "/canned-hds-responses/componentDetailsListEntityFramework.json";

  private static final String PREZI_DETAILS_FILE = "/canned-hds-responses/componentDetailsPrezi.json";

  private static final String PREZI_DETAILS_LIST_FILE = "/canned-hds-responses/componentDetailsListPrezi.json";

  private static final String LICENSES_FILE = "/canned-hds-responses/licenses.json";

  private static final String[] versionGraphLabels =
      {"Popularity", "Policy Threat", "Details", "Security", "License", "Quality", "Other"};

  private static Policy violatedPolicy = null;

  private static Application app;

  private static ComponentDetails JUNIT;

  private static ComponentDetails CATALINA_HOST_MANAGER;

  private static ComponentDetails ENTITY_FRAMEWORK;

  private static ComponentDetails PREZI_DIST;

  private final EclipseCIPPage page = new EclipseCIPPage();

  @BeforeClass
  public static void startup() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  private static <T> T parseJsonFile(String jsonFilename, Class<? extends T> type) throws IOException {
    URL resourceUrl = EclipseCIPPageTest.class.getResource(jsonFilename);
    return new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(resourceUrl, type);
  }

  private static String toJson(Object o) {
    try {
      return new ObjectMapper().writeValueAsString(o);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static String getComponentIdentifierParam(ComponentIdentifier identifier) {
    return UrlEncoded.encodeString(toJson(identifier));
  }

  @Before
  public void start() throws IOException {
    Organization org = tempEntity.newOrganization("EclipseCIPSpec");
    app = tempEntity.newApplication("EclipseCIPSpec", org.getId());

    JUNIT = mockComponentDetails(JUNIT_DETAILS_FILE);
    mockComponentDetailsList(JUNIT_DETAILS_LIST_FILE, JUNIT);
    CATALINA_HOST_MANAGER = mockComponentDetails(CATALINA_HOST_MANAGER_DETAILS_FILE);
    mockComponentDetailsList(CATALINA_HOST_MANAGER_DETAILS_LIST_FILE, CATALINA_HOST_MANAGER);
    ENTITY_FRAMEWORK = mockComponentDetails(ENTITY_FRAMEWORK_DETAILS_FILE);
    mockComponentDetailsList(ENTITY_FRAMEWORK_DETAILS_LIST_FILE, ENTITY_FRAMEWORK);
    PREZI_DIST = mockComponentDetails(PREZI_DETAILS_FILE);
    mockComponentDetailsList(PREZI_DETAILS_LIST_FILE, PREZI_DIST);

    mockComponentDependencies();

    // validation of a license category Policy will trigger this request to populate a cache of licenses
    testCLMServer.getHdsServer().respondWith(getClass().getResource(LICENSES_FILE)).atUri("rest/license");
  }

  @Test
  public void testInitialState() {
    refreshOrOpen(DashboardPage.url());
    //The initial page can be loaded without authentication
    logout();

    //We load the CIP
    refreshOrOpen(EclipseCIPPage.url());

    //We get the default message
    page.versionsCIPBase().shouldNotBe(visible);
    page.defaultText().shouldBe(visible).shouldHave(text(SELECT_COMPONENT));
    startup();
  }

  @Test
  public void testInitialCIP() {
    //Initially the CIP is not shown
    refreshOrOpen(EclipseCIPPage.url());

    //The CIP is not loaded
    page.versionsCIPBase().shouldNotBe(visible);
    page.defaultText().shouldBe(visible).shouldHave(text(SELECT_COMPONENT));
  }

  @Test
  public void testAuthentication() {
    refreshOrOpen(DashboardPage.url());
    logout();
    refreshOrOpen(EclipseCIPPage.url());

    //Simulating user selection of a GAV with javascript
    executeJavaScript(generateMavenCoordinates(JUNIT));

    //an error message is shown
    FormMask.seeAndWaitForDismissal();
    page.errorText().shouldBe(visible).shouldHave(text("Missing Credentials"));
    startup();
  }

  @Test
  public void testSelectComponent() {
    refreshOrOpen(EclipseCIPPage.url());

    //the CIP loads
    executeJavaScript(generateMavenCoordinates(JUNIT));
    FormMask.seeAndWaitForDismissal();
    validateMavenComponent(JUNIT);
    VersionsCIP.highestSecurityThreat().shouldHave(text("NA"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("NA"));
    page.websiteHref().lastChild().shouldHave(attribute("href", JUNIT.getWebsite()));

    //a "View Details" button is present and enabled
    VersionsCIP.viewDetailsButton().shouldBe(visible);
    VersionsCIP.viewDetailsButton().shouldBe(enabled);

    //a "Migrate" button is present and disabled
    VersionsCIP.migrateButton().shouldBe(visible);
    VersionsCIP.migrateButton().shouldHave(cssClass("disabled"));

    //the version graph is present and has a fixed height
    verifyVersionGraph();

    //the select text is no longer shown
    page.defaultText().shouldNotBe(visible);
  }

  @Test
  public void testLegacySelectComponent() {
    refreshOrOpen(EclipseCIPPage.url());

    //the CIP loads
    executeJavaScript(generateMavenGav(JUNIT));
    FormMask.seeAndWaitForDismissal();
    validateMavenComponent(JUNIT);
    VersionsCIP.highestSecurityThreat().shouldHave(text("NA"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("NA"));
    page.websiteHref().lastChild().shouldHave(attribute("href", JUNIT.getWebsite()));

    //a "View Details" button is present and enabled
    VersionsCIP.viewDetailsButton().shouldBe(visible);
    VersionsCIP.viewDetailsButton().shouldBe(enabled);

    //a "Migrate" button is present and disabled
    VersionsCIP.migrateButton().shouldBe(visible);
    VersionsCIP.migrateButton().shouldHave(cssClass("disabled"));

    //the version graph is present and has a fixed height
    verifyVersionGraph();

    //the select text is no longer shown
    page.defaultText().shouldNotBe(visible);
  }

  @Test
  public void testClearGav() {
    refreshOrOpen(EclipseCIPPage.url());
    executeJavaScript(generateMavenCoordinates(JUNIT));
    FormMask.seeAndWaitForDismissal();

    //We simulate the client clearing the GAV information
    executeJavaScript("window.Insight.clearGav()");

    //We are back to being asked to select a component
    page.defaultText().shouldBe(visible).shouldHave(text(SELECT_COMPONENT));
  }

  @Test
  public void testPolicyChange() {
    //A new policy is added that our viewed component violates
    violatedPolicy = createLicensePolicy(app.getId(), "EclipseCIPSpec", "CPL-1.0");

    refreshOrOpen(EclipseCIPPage.url());

    //We set the Component
    executeJavaScript(generateMavenCoordinates(JUNIT));
    FormMask.seeAndWaitForDismissal();

    //The changes should be reflected in the component details
    VersionsCIP.highestPolicyThreat().shouldHave(text(String.valueOf(violatedPolicy.getThreatLevel())));
  }

  @Test
  public void testLegacyPolicyChange() {
    //A new policy is added that our viewed component violates
    violatedPolicy = createLicensePolicy(app.getId(), "EclipseCIPSpec", "CPL-1.0");

    refreshOrOpen(EclipseCIPPage.url());

    //We set the Component
    executeJavaScript(generateMavenGav(JUNIT));
    FormMask.seeAndWaitForDismissal();

    //The changes should be reflected in the component details
    VersionsCIP.highestPolicyThreat().shouldHave(text(String.valueOf(violatedPolicy.getThreatLevel())));
  }

  @Test
  public void testSecurityVulnerabilities() {
    refreshOrOpen(EclipseCIPPage.url());

    //We load a component with known security vulnerabilities
    executeJavaScript(generateMavenCoordinates(CATALINA_HOST_MANAGER));

    //Details of the vulnerabilities are shown
    validateMavenComponent(CATALINA_HOST_MANAGER);
    VersionsCIP.highestSecurityThreat().shouldHave(text("4.3 within 4 security issues"));

    //No website information is provided for this GAV
    page.websiteHref().shouldNotBe(visible);
  }

  @Test
  public void testLegacySecurityVulnerabilities() {
    refreshOrOpen(EclipseCIPPage.url());

    //We load a component with known security vulnerabilities
    executeJavaScript(generateMavenGav(CATALINA_HOST_MANAGER));

    //Details of the vulnerabilities are shown
    validateMavenComponent(CATALINA_HOST_MANAGER);
    VersionsCIP.highestSecurityThreat().shouldHave(text("4.3 within 4 security issues"));

    //No website information is provided for this GAV
    page.websiteHref().shouldNotBe(visible);
  }

  @Test
  public void testClassifierAndExtensionForMavenComponent() {
    refreshOrOpen(EclipseCIPPage.url());

    //We load a component with both classifier and extension
    executeJavaScript(generateMavenCoordinates(PREZI_DIST));

    //the CIP loads and all GAVEC coordinate information is shown
    validateMavenComponent(PREZI_DIST);
    VersionsCIP.extension().shouldHave(text(PREZI_DIST.getComponentIdentifier().getCoordinates().get("extension")));
    VersionsCIP.classifier().shouldHave(text(PREZI_DIST.getComponentIdentifier().getCoordinates().get("classifier")));
  }

  @Test
  public void testSelectNugetComponent() {
    refreshOrOpen(EclipseCIPPage.url());

    //We load a NuGet component
    executeJavaScript(generateNugetCoordinates(ENTITY_FRAMEWORK));

    //the CIP loads and shows the expected fields
    validateNuGetComponent(ENTITY_FRAMEWORK);
    VersionsCIP.highestSecurityThreat().shouldHave(text("NA"));
    VersionsCIP.highestPolicyThreat().shouldHave(text("NA"));
  }

  private void mockComponentDependencies() {
    testCLMServer.getHdsServer()
        .respondWith(new ComponentDependenciesDTO(new HashMap<>(), new HashMap<>()))
        .atUri("/rest/component/dependencies");
  }

  private ComponentDetails mockComponentDetails(String jsonFilename) throws IOException {
    ComponentDetails hdsComponentResponse = parseJsonFile(jsonFilename, ComponentDetails.class);
    hdsComponentResponse.setCatalogDate(Instant.now().minus(Duration.ofDays(366)).toEpochMilli());
    // ensure that catalog data is consistent
    testCLMServer.getHdsServer()
        .respondWith(hdsComponentResponse)
        .atUri(createComponentDetailURL(hdsComponentResponse.getComponentIdentifier()));
    return hdsComponentResponse;
  }

  private void mockComponentDetailsList(String jsonFilename, ComponentDetails component) throws IOException {
    ComponentDetailsList hdsComponentListResponse = parseJsonFile(jsonFilename, ComponentDetailsList.class);
    testCLMServer.getHdsServer()
        .respondWith(hdsComponentListResponse)
        .atUri(createComponentDetailListURL(component.getComponentIdentifier()));
  }

  private String createComponentDetailURL(ComponentIdentifier componentIdentifier) {
    return "rest/" + getToolName() + "/componentDetails?componentIdentifier=" +
        getComponentIdentifierParam(componentIdentifier);
  }

  private String createComponentDetailListURL(ComponentIdentifier componentIdentifier) {
    return "rest/" + getToolName() + "/componentDetails/list?componentIdentifier=" +
        getComponentIdentifierParam(componentIdentifier);
  }

  private void verifyVersionGraph() {
    page.versionsCIPBase().shouldBe(visible);
    VersionsCIP.versionGraph().shouldBe(visible);
    VersionsCIP.versionGraphLabels().shouldHave(texts(versionGraphLabels));
    VersionsCIP.versionGraph().shouldHave(attribute("height","153"));
  }

  private void validateMavenComponent(ComponentDetails component) {
    VersionsCIP.groupId().lastChild()
        .shouldHave(text(component.getComponentIdentifier().getCoordinates().get("groupId")));
    VersionsCIP.artifactId().lastChild()
        .shouldHave(text(component.getComponentIdentifier().getCoordinates().get("artifactId")));

    validateComponentCommon(component);
  }

  private void validateNuGetComponent(ComponentDetails component) {
    VersionsCIP.artifactTable().shouldBe(visible);
    page.nugetComponentID().lastChild()
        .shouldHave(text(component.getComponentIdentifier().getCoordinates().get("packageId")));
    validateComponentCommon(component);
  }

  private void validateComponentCommon(ComponentDetails component) {
    VersionsCIP.version().lastChild()
        .shouldHave(text(component.getComponentIdentifier().getCoordinates().get("version")));
    VersionsCIP.declaredLicenses()
        .shouldHave(texts(component.getDeclaredLicenses().iterator().next().getLicenseName()));
    VersionsCIP.observedLicenses()
        .shouldHave(texts(component.getObservedLicenses().iterator().next().getLicenseName()));
    VersionsCIP.effectiveLicenses().shouldHave(texts(generateEffectiveLicenses(component)));
    VersionsCIP.matchState().shouldHave(text("exact"));
    VersionsCIP.identificationSource().shouldHave(text("Sonatype"));
    VersionsCIP.catalogDate().shouldHave(text("1 year ago"));
  }

  private Policy createLicensePolicy(String appId, String name, String licenseId) {
    Policy policy = new Policy(licenseId, name);
    policy.setThreatLevel(10);
    policy.setOwnerId(appId);
    Constraint constraint = new Constraint(appId, name, null);
    constraint.addCondition(new Condition(LicenseConditionType.ID, "is", licenseId));
    policy.addConstraint(constraint);
    return tempEntity.newPolicy(policy);
  }

  private String generateMavenCoordinates(ComponentDetails component) {
    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    String format = componentIdentifier.getFormat();
    String groupId = componentIdentifier.getCoordinates().get("groupId");
    String artifactId = componentIdentifier.getCoordinates().get("artifactId");
    String version = componentIdentifier.getCoordinates().get("version");

    String extension = componentIdentifier.getCoordinates().get("extension");
    extension = (extension == null) ? "" : ", \"extension\":\"" + extension + "\"";
    String classifier = componentIdentifier.getCoordinates().get("classifier");
    classifier = (classifier == null) ? "" : ", \"classifier\":\"" + classifier  + "\"";

    return "window.Insight.setCoordinates(\"" + format + "\", {\"groupId\":\"" + groupId + "\", \"artifactId\":\"" +
        artifactId + "\", \"version\":" +
        "\"" + version + "\"" + extension + classifier + "}, {\"appId\":\"EclipseCIPSpec\"});";

  }

  private String generateNugetCoordinates(ComponentDetails component) {
    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    String format = componentIdentifier.getFormat();
    String packageId = component.getComponentIdentifier().getCoordinates().get("packageId");
    String version = componentIdentifier.getCoordinates().get("version");

    return "window.Insight.setCoordinates(\"" + format + "\", {\"packageId\":\"" + packageId + "\", \"version\":" +
        "\"" + version + "\"}, {\"appId\":\"EclipseCIPSpec\"});";

  }

  private String generateMavenGav(ComponentDetails component) {
    ComponentIdentifier componentIdentifier = component.getComponentIdentifier();
    String groupId = componentIdentifier.getCoordinates().get("groupId");
    String artifactId = componentIdentifier.getCoordinates().get("artifactId");
    String version = componentIdentifier.getCoordinates().get("version");

    return "window.Insight.setGav({\"groupId\": \"" + groupId + "\", \"artifactId\": \"" + artifactId
        + "\", \"version\": \"" + version + "\", \"appId\": \"EclipseCIPSpec\"});";

  }

  private List<String> generateEffectiveLicenses(ComponentDetails component) {
    List<String> effectiveLicenses = new ArrayList<>();

    for (License license : component.getEffectiveLicenses()) {
      if (effectiveLicenses.size() == component.getEffectiveLicenses().size() - 1) {
        effectiveLicenses.add(license.getLicenseName());
      }
      else {
        effectiveLicenses.add(license.getLicenseName() + ",");
      }
    }

    return effectiveLicenses;
  }

  private String getToolName() {
    return "ide";
  }
}
