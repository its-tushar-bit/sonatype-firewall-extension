/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.MatchedComponent;
import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.SecurityVulnerabilityOverrideDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssSeverityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCvssVectorDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomCweDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityCustomRemediationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCategory;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityCustomData;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssSeverity;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCvssVector;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomCwe;
import com.sonatype.insight.brain.model.vulnerability.VulnerabilityCustomRemediation;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.clm.dto.model.ComponentEndOfLifeStatus.END_OF_LIFE_TRUE;
import static com.sonatype.clm.dto.model.ComponentEndOfLifeStatus.END_OF_LIFE_UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

public class ComponentLoaderTest
    extends AbstractDataTest
{
  private static final String COMP_HASH = "12345678901234567890";

  private Application application;

  protected Organization organization;

  private MultiLicenseDAO multiLicenseDAO;

  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private OwnerDAO ownerDAO;

  private VulnerabilityCustomCvssSeverityDAO vulnerabilityCustomCvssSeverityDAO;

  private VulnerabilityCustomRemediationDAO vulnerabilityCustomRemediationDAO;

  private SecurityVulnerabilityOverrideDAO securityVulnerabilityOverrideDAO;

  private VulnerabilityCustomCweDAO vulnerabilityCustomCweDAO;

  private VulnerabilityCustomCvssVectorDAO vulnerabilityCustomCvssVectorDAO;

  private LabelDAO labelDAO;

  private ComponentLabelDAO componentLabelDAO;

  private LicenseOverrideDAO licenseOverrideDAO;

  private ComponentLoaderFactory componentLoaderFactory;

  private ComponentLoader componentLoader;

  @Before
  public void setup() {
    organization = tempEntity.newOrganization("AbstractDbDAOTest");
    application = tempEntity.newApplication("AbstractDbDAOTest-AppName", "AbstractDbDAOTest-AppPublicId",
        organization.getId());

    multiLicenseDAO = daoFactory.createMultiLicenseDAO();
    licenseThreatGroupDAO = daoFactory.createLicenseThreatGroupDAO();
    licenseThreatGroupLicenseDAO = daoFactory.createLicenseThreatGroupLicenseDAO();
    ownerDAO = daoFactory.createOwnerDAO();
    vulnerabilityCustomCvssSeverityDAO = daoFactory.createVulnerabilityCustomCvssSeverityDAO();
    vulnerabilityCustomRemediationDAO = daoFactory.createVulnerabilityCustomRemediationDAO();
    securityVulnerabilityOverrideDAO = daoFactory.createSecurityVulnerabilityOverrideDAO();
    vulnerabilityCustomCweDAO = daoFactory.createVulnerabilityCustomCweDAO();
    vulnerabilityCustomCvssVectorDAO = daoFactory.createVulnerabilityCustomCvssVectorDAO();
    labelDAO = daoFactory.createLabelDAO();
    componentLabelDAO = daoFactory.createComponentLabelDAO();
    licenseOverrideDAO = daoFactory.createLicenseOverrideDAO();

    componentLoaderFactory =
        new ComponentLoaderFactory(multiLicenseDAO, licenseThreatGroupDAO, licenseThreatGroupLicenseDAO,
            licenseOverrideDAO, securityVulnerabilityOverrideDAO, ownerDAO, componentLabelDAO,
            vulnerabilityCustomRemediationDAO, vulnerabilityCustomCweDAO, vulnerabilityCustomCvssVectorDAO,
            vulnerabilityCustomCvssSeverityDAO);

    componentLoader = componentLoaderFactory.createComponentLoader(application);

    // Create LTGs
    tempEntity.newLicenseThreatGroup(application.getId(), "My group 1", 1, "Apache-2.0");
    tempEntity.newLicenseThreatGroup(organization.getId(), "My group 2", 5, "GPL-2.0");
    tempEntity.newLicenseThreatGroup(organization.getParentOrganizationId(), "My group 3", 9, "GPL-3.0");
  }

  private SecurityVulnerability newSV(String refId,
                                      String source,
                                      Float severity,
                                      SecurityVulnerabilityOverrideStatus status)
  {
    SecurityVulnerability sv = new SecurityVulnerability(source, refId, severity);
    sv.setStatus(status);
    return sv;
  }

  private SecurityVulnerability newSVWithAlias(String refId,
                                      String source,
                                      Float severity,
                                      SecurityVulnerabilityOverrideStatus status, String... aliases)
  {
    SecurityVulnerability sv = newSV(refId, source, severity, status);
    for (String alias : aliases) {
      sv.addAlias(alias);
    }
    return sv;
  }

  private void assertSecurityVulnerabilities(List<SecurityVulnerability> actual, SecurityVulnerability... expected) {
    assertThat(actual).hasSameSizeAs(expected);
    for (int i = 0, n = expected.length; i < n; i++) {
      assertSecurityVulnerability(expected[i], actual.get(i));
    }
  }

  private void assertSecurityVulnerability(SecurityVulnerability expected, SecurityVulnerability actual) {
    assertThat(actual.getRefId()).isEqualTo(expected.getRefId());
    assertThat(actual.getSource()).isEqualTo(expected.getSource());
    assertThat(actual.getSeverity()).isEqualTo(expected.getSeverity());
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
    assertThat(actual.getVulnerabilityCategories()).isEqualTo(expected.getVulnerabilityCategories());
    assertThat(actual.getAliases()).isEqualTo(expected.getAliases());
  }

  private void assertLicenseThreatGroups(Set<LicenseThreatGroup> actual, String... expected) {
    assertThat(actual).extracting(LicenseThreatGroup::getName).containsExactlyInAnyOrder(expected);
  }

  @SuppressWarnings("deprecation")
  private void assertGav(MatchedComponent expectedMatchedComponent, Component actualComponent) {
    assertThat(actualComponent.getGroupId()).isEqualTo(expectedMatchedComponent.getGroupId());
    assertThat(actualComponent.getArtifactId()).isEqualTo(expectedMatchedComponent.getArtifactId());
    assertThat(actualComponent.getVersion()).isEqualTo(expectedMatchedComponent.getVersion());
  }

  @Test
  public void testGetComponent() {
    Label appLabel = new Label(application.getId(), "red");
    labelDAO.insert(appLabel);
    Label orgLabel = new Label(application.getOrganizationId(), "blue");
    labelDAO.insert(orgLabel);
    componentLabelDAO.insert(new ComponentLabel(application.getId(), appLabel.getId(), COMP_HASH));
    componentLabelDAO.insert(new ComponentLabel(application.getOrganizationId(), orgLabel.getId(), COMP_HASH));

    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3"));
    matchedComponent.setMatchState("similar");
    matchedComponent.setCatalogDate(System.currentTimeMillis());
    matchedComponent.setRelativePopularity(42);
    matchedComponent.addDeclaredLicenseId("Apache-2.0");
    matchedComponent.addObservedLicenseId("MIT");
    com.sonatype.clm.dto.model.component.HygieneRating hygieneRating =
        new com.sonatype.clm.dto.model.component.HygieneRating(1, "HygieneRating");
    matchedComponent.setHygieneRating(hygieneRating);
    com.sonatype.clm.dto.model.component.IntegrityRating integrityRating =
        new com.sonatype.clm.dto.model.component.IntegrityRating(1, "IntegrityRating");
    matchedComponent.setIntegrityRating(integrityRating);
    com.sonatype.clm.dto.model.component.ComponentCategory componentCategory =
        new com.sonatype.clm.dto.model.component.ComponentCategory(1, "ComponentCategory");
    matchedComponent.setComponentCategories(Collections.singletonList(componentCategory));
    com.sonatype.clm.dto.model.SecurityVulnerability sv =
        new com.sonatype.clm.dto.model.SecurityVulnerability("12345", "osvdb", 4f);
    sv.setVulnerabilityCategories(Collections.singletonList("cat1"));
    sv.setAliases(Collections.singletonList("alias1"));
    matchedComponent
        .addSecurityVulnerability(sv);
    matchedComponent.setEndOfLife(END_OF_LIFE_TRUE);

    Component component = componentLoader.getComponent(matchedComponent, true);

    assertThat(component).isNotNull();
    assertThat(component.getHash()).isEqualTo(matchedComponent.getHash());
    assertThat(component.getComponentIdentifier()).isEqualTo(matchedComponent.getComponentIdentifier());
    assertGav(matchedComponent, component);
    assertThat(component.getMatchState().getId()).isEqualTo(matchedComponent.getMatchState());
    assertThat(component.getCatalogDate()).isEqualTo(matchedComponent.getCatalogDate());
    assertThat(component.getRelativePopularity()).isEqualTo(matchedComponent.getRelativePopularity());
    assertThat(component.getDeclaredLicenseIds()).isEqualTo(matchedComponent.getDeclaredLicenseIds());
    assertThat(component.getObservedLicenseIds()).isEqualTo(matchedComponent.getObservedLicenseIds());
    assertThat(component.getDeclaredMultiLicenseIds()).isEqualTo(matchedComponent.getDeclaredLicenseIds());
    assertThat(component.getObservedMultiLicenseIds()).isEqualTo(matchedComponent.getObservedLicenseIds());

    assertThat(component.getLicenseOverrideIds()).isEmpty();
    assertLicenseThreatGroups(component.getLicenseThreatGroups(), "My group 1");
    assertSecurityVulnerabilities(component.getSecurityVulnerabilities(),
        newSVWithAlias("12345", "osvdb", 4f, SecurityVulnerabilityOverrideStatus.OPEN, "alias1"));

    SecurityVulnerability svWithAlias = component.getSecurityVulnerabilities().get(0);
    svWithAlias.addAlias("New Alias");
    assertSecurityVulnerability(svWithAlias,
        newSVWithAlias("12345", "osvdb", 4f, SecurityVulnerabilityOverrideStatus.OPEN, "alias1", "New Alias"));

    assertThat(component.getLabelIds()).containsExactlyInAnyOrder(appLabel.getId(), orgLabel.getId());
    assertThat(component.getHygieneRating().getId()).isEqualTo(String.valueOf(hygieneRating.getId()));
    assertThat(component.getIntegrityRating().getId()).isEqualTo(String.valueOf(integrityRating.getId()));
    assertThat(component.getComponentCategories()).hasSize(1);
    assertThat(component.getComponentCategories().get(0).getId())
        .isEqualTo(String.valueOf(componentCategory.getComponentCategoryId()));
    assertThat(component.getEndOfLife()).isEqualTo(END_OF_LIFE_TRUE);
  }

  @Test
  public void testGetComponent_LicenseOverride() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3"));
    Component component = componentLoader.getComponent(matchedComponent, true);
    assertThat(component).isNotNull();
    assertThat(component.getLicenseOverrideIds()).isEmpty();

    // Override at org level
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3", null,
        null);
    LicenseOverride orgLicenseOverride = new LicenseOverride(organization.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "GPL-3.0", "My comment");
    licenseOverrideDAO.insert(orgLicenseOverride);
    assertLicenseOverrideIsTheExpected(matchedComponent, "GPL-3.0");

    // Override at app level
    LicenseOverride appLicenseOverride = new LicenseOverride(application.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My comment");
    licenseOverrideDAO.insert(appLicenseOverride);
    assertLicenseOverrideIsTheExpected(matchedComponent, "GPL-2.0");
  }

  @Test
  public void testGetComponent_LegacyLicenseOverride() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent
        .setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3", "", "jar"));
    Component component = componentLoader.getComponent(matchedComponent, true);
    assertThat(component).isNotNull();
    assertThat(component.getLicenseOverrideIds()).isEmpty();

    // New-style override at org level
    ComponentIdentifier componentIdentifier = matchedComponent.getComponentIdentifier();
    LicenseOverride orgLicenseOverride = new LicenseOverride(organization.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "GPL-3.0", "My comment");
    licenseOverrideDAO.insert(orgLicenseOverride);
    assertLicenseOverrideIsTheExpected(matchedComponent, "GPL-3.0");

    // Legacy override at app level
    LicenseOverride appLicenseOverride =
        new LicenseOverride(application.getId(), ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3"),
            LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My comment");
    licenseOverrideDAO.insert(appLicenseOverride);
    assertLicenseOverrideIsTheExpected(matchedComponent, "GPL-2.0");
  }

  @Test
  public void testGetComponent_MultiLicenses_Declared() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3"));
    matchedComponent.addDeclaredLicenseId("Apache-2.0-GPL-2.0");
    matchedComponent.addDeclaredLicenseId("Apache-2.0-GPL-3.0");
    Component component = componentLoader.getComponent(matchedComponent, true);
    assertThat(component).isNotNull();
    assertThat(component.getDeclaredLicenseIds()).containsExactlyInAnyOrder("Apache-2.0", "GPL-2.0", "GPL-3.0");
    assertThat(component.getDeclaredMultiLicenseIds())
        .containsExactlyInAnyOrder("Apache-2.0-GPL-3.0", "Apache-2.0-GPL-2.0");
    assertLicenseThreatGroups(component.getLicenseThreatGroups(), "My group 1", "My group 2", "My group 3");
  }

  @Test
  public void testGetComponent_MultiLicenses_Observed() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3"));
    matchedComponent.addObservedLicenseId("Apache-2.0-GPL-2.0");
    matchedComponent.addObservedLicenseId("Apache-2.0-GPL-3.0");
    Component component = componentLoader.getComponent(matchedComponent, true);
    assertThat(component).isNotNull();
    assertThat(component.getObservedLicenseIds()).containsExactlyInAnyOrder("Apache-2.0", "GPL-2.0", "GPL-3.0");
    assertThat(component.getObservedMultiLicenseIds())
        .containsExactlyInAnyOrder("Apache-2.0-GPL-3.0", "Apache-2.0-GPL-2.0");
    assertLicenseThreatGroups(component.getLicenseThreatGroups(), "My group 1", "My group 2", "My group 3");
  }

  @Test
  public void testGetComponent_ObservedLicensesHidden() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createNpmCoordinates("p", "v"));
    matchedComponent.addObservedLicenseId("Apache-2.0-GPL-2.0");
    Component component = componentLoader.getComponent(matchedComponent, false);
    assertThat(component).isNotNull();
    assertThat(component.getObservedLicenseIds()).containsExactly(License.NOT_SUPPORTED_ID);
    assertThat(component.getObservedMultiLicenseIds()).containsExactly(License.NOT_SUPPORTED_ID);
    assertThat(component.isHiddenObservedLicenses()).isTrue();
  }

  @Test
  public void testGetComponent_ObservedLicensesHiddenNotSupportedFormat_AlpObservedLicenseDetectionEnabled() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", "q", "v"));
    matchedComponent.addObservedLicenseId("MIT");
    Component component =
        componentLoader.getComponent(matchedComponent, true /* isAlpObservedLicenseDetectionEnabled */);
    assertThat(component).isNotNull();
    assertThat(component.getObservedLicenseIds()).containsExactly("MIT");
    assertThat(component.getObservedMultiLicenseIds()).containsExactly("MIT");
    assertThat(component.isHiddenObservedLicenses()).isFalse();
  }

  @Test
  public void testGetComponent_ObservedLicensesHiddenNotSupportedFormat_AlpObservedLicenseDetectionDisabled() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", "q", "v"));
    matchedComponent.addObservedLicenseId("MIT");
    Component component =
        componentLoader.getComponent(matchedComponent, false /* isAlpObservedLicenseDetectionEnabled */);
    assertThat(component).isNotNull();
    assertThat(component.getObservedLicenseIds()).containsExactly("Not-Supported");
    assertThat(component.getObservedMultiLicenseIds()).containsExactly("Not-Supported");
    assertThat(component.isHiddenObservedLicenses()).isTrue();
  }

  @Test
  public void testGetComponent_ObservedLicensesHiddenEmptyLicenses() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createNpmCoordinates("p", "v"));
    Component component = componentLoader.getComponent(matchedComponent, false);
    assertThat(component).isNotNull();
    assertThat(component.getObservedLicenseIds()).isEmpty();
    assertThat(component.getObservedMultiLicenseIds()).isEmpty();
    assertThat(component.isHiddenObservedLicenses()).isFalse();
  }

  @Test
  public void testGetComponent_ObservedLicensesHiddenOnlyNotSupportedLicense() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createNpmCoordinates("p", "v"));
    matchedComponent.addObservedLicenseId(License.NOT_SUPPORTED_ID);
    Component component = componentLoader.getComponent(matchedComponent, false);
    assertThat(component).isNotNull();
    assertThat(component.getObservedLicenseIds()).containsExactly(License.NOT_SUPPORTED_ID);
    assertThat(component.getObservedMultiLicenseIds()).containsExactly(License.NOT_SUPPORTED_ID);
    assertThat(component.isHiddenObservedLicenses()).isFalse();
  }

  @Test
  public void testGetComponent_UnknownComponent_WithComponentIdentifier() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode bom = objectMapper.createObjectNode();
    ArrayNode aaData = objectMapper.createArrayNode();
    ObjectNode component = objectMapper.createObjectNode();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    component.set("componentIdentifier", objectMapper.valueToTree(componentIdentifier));
    component.put("matchState", MatchState.UNKNOWN.getId());
    ComponentDisplayName componentDisplayName = new ComponentDisplayName();
    componentDisplayName.add("Filename", "unknown.jar");
    componentDisplayName.setName("unknown.jar");
    component.set("displayName", objectMapper.valueToTree(componentDisplayName));
    component.put("proprietary", false);
    aaData.add(objectMapper.valueToTree(component));
    bom.set("aaData", aaData);

    List<Component> components =
        componentLoader.getAll(null, null, objectMapper.writeValueAsBytes(bom), null);

    assertThat(components).extracting(Component::getComponentIdentifier).containsExactly(componentIdentifier);
  }

  @Test
  public void testGetTypeToString_ComponentDisplayName() {
    ComponentDisplayName componentDisplayName = new ComponentDisplayName();
    componentDisplayName.add("field1", "value1");
    componentDisplayName.add(",");
    componentDisplayName.add("field2", "value2");
    JsonNode jsonNode = JsonUtils.asTree(componentDisplayName);

    assertThat(JsonUtils.getTypeToString(jsonNode, ComponentDisplayName.class)).isEqualTo("value1,value2");
  }

  @Test
  public void testGetAll_WithSecurityVulnerabilityCustom() throws Exception {
    String hash = "abc123";
    String refId = "CVE-123";
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode bom = objectMapper.createObjectNode();
    ArrayNode aaData = objectMapper.createArrayNode();

    ObjectNode component1 = objectMapper.createObjectNode();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    component1.set("componentIdentifier", objectMapper.valueToTree(componentIdentifier));
    component1.put("hash", hash);
    component1.put("matchState", MatchState.EXACT.getId());
    component1.set("displayName",
        objectMapper.valueToTree(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier)));
    component1.put("proprietary", false);
    component1.put("relativePopularity", 100.0);
    component1.put("createTime", System.currentTimeMillis());
    component1.put("endOfLife", END_OF_LIFE_TRUE.name());

    ObjectNode component2 = objectMapper.createObjectNode();
    ComponentIdentifier componentIdentifier2 = ComponentIdentifier
        .createMavenCoordinates("g2", "a2", "v2", "c2", "e2");
    component2.set("componentIdentifier", objectMapper.valueToTree(componentIdentifier2));
    component2.put("hash", hash + "-2");
    component2.put("matchState", MatchState.EXACT.getId());
    component2.set("displayName",
        objectMapper.valueToTree(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier2)));
    component2.put("proprietary", true);
    component2.put("relativePopularity", 70.0);
    component2.put("createTime", System.currentTimeMillis());

    aaData.add(objectMapper.valueToTree(component1));
    aaData.add(objectMapper.valueToTree(component2));
    bom.set("aaData", aaData);

    ObjectNode securityData = objectMapper.createObjectNode();
    ArrayNode aaDataSecurityData = objectMapper.createArrayNode();
    ObjectNode securityVulnerability = objectMapper.createObjectNode();
    securityVulnerability.put("hash", hash);
    securityVulnerability.put("source", "test");
    securityVulnerability.put("reference", refId);
    aaDataSecurityData.add(securityVulnerability);
    securityData.set("aaData", aaDataSecurityData);

    ObjectNode analysisNode = objectMapper.createObjectNode();
    analysisNode.put("detail", "analysis detail");
    analysisNode.put("justification", "code_not_reachable");
    analysisNode.put("response", "will_not_fix");
    analysisNode.put("state", "resolved");
    securityVulnerability.set("analysis", objectMapper.valueToTree(analysisNode));

    Tag tag = tempEntity.newTag(application.getOrganizationId());
    tempEntity.newApplicationTag(application.getId(), tag.getId());
    tempEntity.newVulnerabilityCustomData(application.getOrganizationId(), refId, tag, "custom-remediation", "123",
        "custom/vector", 4.4f);

    List<Component> components = componentLoader.getAll(null, false,
        objectMapper.writeValueAsBytes(securityData), objectMapper.writeValueAsBytes(bom), null);

    assertThat(components).hasSize(2);
    assertThat(components.get(0).getSecurityVulnerabilities()).hasSize(1);

    // should copy endOfLife from the bomRow
    assertThat(components.get(0).getEndOfLife()).isEqualTo(END_OF_LIFE_TRUE);

    // should be convert missing endOfLife to unknown
    assertThat(components.get(1).getEndOfLife()).isEqualTo(END_OF_LIFE_UNKNOWN);

    SecurityVulnerability securityVulnerabilityResult = components.get(0).getSecurityVulnerabilities().get(0);
    assertThat(securityVulnerabilityResult.getRefId()).isEqualTo(refId);

    SecurityVulnerabilityCustomData customData = securityVulnerabilityResult.getSecurityVulnerabilityCustomData();
    assertThat(customData).isNotNull();
    assertThat(customData.getRemediation()).isEqualTo("custom-remediation");
    assertThat(customData.getCweId()).isEqualTo("123");
    assertThat(customData.getCvssVector()).isEqualTo("custom/vector");
    assertThat(customData.getCvssSeverity()).isEqualTo(4.4f);

    ThirdPartyVulnerabilityExploitabilityExchange analysis = securityVulnerabilityResult.getAnalysis();
    assertThat(analysis.getDetail()).isEqualTo("analysis detail");
    assertThat(analysis.getJustification()).isEqualTo("code_not_reachable");
    assertThat(analysis.getResponse()).isEqualTo("will_not_fix");
    assertThat(analysis.getState()).isEqualTo("resolved");
  }

  @Test
  public void testProcessComponentsWithoutLicenses_LicenseOverride() throws Exception {
    String hash = "abc123";
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode bom = objectMapper.createObjectNode();
    ArrayNode aaData = objectMapper.createArrayNode();
    Component component = new Component();
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g", "a", "v", "c", "e");
    component.setComponentIdentifier(componentIdentifier);
    component.setHash(hash);
    component.setMatchState(MatchState.EXACT);
    component.setDisplayName(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).getName());
    component.setProprietary(false);
    component.setRelativePopularity(100);

    ComponentIdentifier componentIdentifier1 = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    Component component1 = new Component();
    component1.setComponentIdentifier(componentIdentifier1);
    component1.setHash(hash);
    component1.setMatchState(MatchState.EXACT);
    component1.setDisplayName(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).getName());
    component1.setProprietary(false);
    component1.setRelativePopularity(100);

    aaData.add(objectMapper.valueToTree(component));
    aaData.add(objectMapper.valueToTree(component1));
    bom.set("aaData", aaData);

    aaData.add(objectMapper.valueToTree(component));

    tempEntity.newLicenseOverride(application.getId(),
        componentIdentifier1,
        LicenseOverrideStatus.OVERRIDDEN,
        "BSD-2-Clause",
        "comment");

    Map<ComponentIdentifier, List<Component>> componentsByIdentifier = new HashMap<>();
    componentsByIdentifier.put(componentIdentifier, Collections.singletonList(component));
    componentsByIdentifier.put(componentIdentifier1, Collections.singletonList(component1));

    List<ComponentIdentifier> componentIdentifiersWithLicenses = new ArrayList<>();
    componentIdentifiersWithLicenses.add(componentIdentifier);

    componentLoader.processComponentsWithoutLicenses(
        componentsByIdentifier,
        componentIdentifiersWithLicenses,
        false);
    assertThat(component1.getLicenseOverrideStatus()).isEqualTo(LicenseOverrideStatus.OVERRIDDEN);
    assertThat(component1.getLicenseOverrideIds()).containsExactly("BSD-2-Clause");
    assertThat(component.getLicenseOverrideStatus()).isEqualTo(LicenseOverrideStatus.OPEN);
    assertThat(component.getLicenseOverrideIds()).isEmpty();
  }

  @Test
  public void testProcessComponentsWithoutLicenses_LicenseOverride_NullComponentIdentifier() throws Exception {
    String hash = "abc123";
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode bom = objectMapper.createObjectNode();
    ArrayNode aaData = objectMapper.createArrayNode();
    Component unknownComponent = new Component();
    unknownComponent.setHash(hash);
    unknownComponent.setMatchState(MatchState.UNKNOWN);
    unknownComponent.setProprietary(false);

    aaData.add(objectMapper.valueToTree(unknownComponent));
    bom.set("aaData", aaData);

    Map<ComponentIdentifier, List<Component>> componentsByIdentifier = new HashMap<>();
    componentsByIdentifier.put(null, Collections.singletonList(unknownComponent));

    List<ComponentIdentifier> componentIdentifiersWithLicenses = new ArrayList<>();

    componentLoader.processComponentsWithoutLicenses(
        componentsByIdentifier,
        componentIdentifiersWithLicenses,
        false);

    assertThat(unknownComponent.getLicenseOverrideStatus()).isEqualTo(LicenseOverrideStatus.OPEN);
    assertThat(unknownComponent.getLicenseOverrideIds()).isEmpty();
  }

  @Test
  public void testGetComponent_SecurityVulnerability() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent
        .setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3", "", "jar"));
    com.sonatype.clm.dto.model.SecurityVulnerability securityVulnerability =
        new com.sonatype.clm.dto.model.SecurityVulnerability("testRefId", "testSource", 7F);
    securityVulnerability.setCwe("testCwe");
    matchedComponent.setSecurityVulnerabilities(Collections.singletonList(securityVulnerability));

    Component component = componentLoader.getComponent(matchedComponent, true);

    assertThat(component.getSecurityVulnerabilities()).hasSize(1);
    SecurityVulnerability foundSecurityVulnerability = component.getSecurityVulnerabilities().get(0);
    assertThat(foundSecurityVulnerability.getRefId()).isEqualTo(securityVulnerability.getRefId());
    assertThat(foundSecurityVulnerability.getSource()).isEqualTo(securityVulnerability.getSource());
    assertThat(foundSecurityVulnerability.getSeverity()).isEqualTo(securityVulnerability.getSeverity());
    assertThat(foundSecurityVulnerability.getCwe()).isEqualTo(securityVulnerability.getCwe());
    assertThat(foundSecurityVulnerability.getStatus()).isEqualTo(SecurityVulnerabilityOverrideStatus.OPEN);
  }

  @Test
  public void testGetComponent_SecurityVulnerabilityOverride() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent
        .setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3", "", "jar"));
    com.sonatype.clm.dto.model.SecurityVulnerability securityVulnerability =
        new com.sonatype.clm.dto.model.SecurityVulnerability("testRefId", "testSource", 7F);
    securityVulnerability.setCwe("testCwe");
    matchedComponent.setSecurityVulnerabilities(Collections.singletonList(securityVulnerability));

    SecurityVulnerabilityOverride svOverride =
        new SecurityVulnerabilityOverride(application.getId(), COMP_HASH, securityVulnerability.getSource(),
            securityVulnerability.getRefId(), SecurityVulnerabilityOverrideStatus.CONFIRMED, "comment");
    securityVulnerabilityOverrideDAO.insert(svOverride);

    Component component = componentLoader.getComponent(matchedComponent, true);

    assertThat(component.getSecurityVulnerabilities()).hasSize(1);
    SecurityVulnerability foundSecurityVulnerability = component.getSecurityVulnerabilities().get(0);
    assertThat(foundSecurityVulnerability.getStatus()).isEqualTo(svOverride.getStatus());
  }

  @Test
  public void testGetComponent_SecurityVulnerabilityCustomCwe() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent
        .setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3", "", "jar"));
    com.sonatype.clm.dto.model.SecurityVulnerability securityVulnerability =
        new com.sonatype.clm.dto.model.SecurityVulnerability("testRefId", "testSource", 7F);
    securityVulnerability.setCwe("testCwe");
    matchedComponent.setSecurityVulnerabilities(Collections.singletonList(securityVulnerability));

    VulnerabilityCustomCwe vulnerabilityCustomCwe = new VulnerabilityCustomCwe();
    vulnerabilityCustomCwe.setOwnerId(application.getId());
    vulnerabilityCustomCwe.setRefId(securityVulnerability.getRefId());
    vulnerabilityCustomCwe.setComponentIdentifier(matchedComponent.getComponentIdentifier());
    vulnerabilityCustomCwe.setLastUpdatedAt(new Date());
    vulnerabilityCustomCwe.setLastUpdatedByUsername("testUser");
    vulnerabilityCustomCwe.setCwe("customCweId");
    vulnerabilityCustomCweDAO.insert(vulnerabilityCustomCwe);

    Component component = componentLoader.getComponent(matchedComponent, true);

    assertThat(component.getSecurityVulnerabilities()).hasSize(1);
    SecurityVulnerability foundSecurityVulnerability = component.getSecurityVulnerabilities().get(0);
    assertThat(foundSecurityVulnerability.getCwe()).isEqualTo(securityVulnerability.getCwe());
    assertThat(foundSecurityVulnerability.getSecurityVulnerabilityCustomData().getCweId())
        .isEqualTo(vulnerabilityCustomCwe.getCwe());
  }

  @Test
  public void testGetComponent_SecurityVulnerabilityCustomCVSSVectorString() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent
        .setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3", "", "jar"));
    com.sonatype.clm.dto.model.SecurityVulnerability securityVulnerability =
        new com.sonatype.clm.dto.model.SecurityVulnerability("testRefId", "testSource", 7F);
    matchedComponent.setSecurityVulnerabilities(Collections.singletonList(securityVulnerability));

    VulnerabilityCustomCvssVector customCvssVector = new VulnerabilityCustomCvssVector();
    customCvssVector.setOwnerId(application.getId());
    customCvssVector.setRefId(securityVulnerability.getRefId());
    customCvssVector.setComponentIdentifier(matchedComponent.getComponentIdentifier());
    customCvssVector.setLastUpdatedByUsername("testUser");
    customCvssVector.setLastUpdatedAt(new Date());
    customCvssVector.setVector("customVector");
    vulnerabilityCustomCvssVectorDAO.insert(customCvssVector);

    Component component = componentLoader.getComponent(matchedComponent, true);

    assertThat(component.getSecurityVulnerabilities()).hasSize(1);
    SecurityVulnerability foundSecurityVulnerability = component.getSecurityVulnerabilities().get(0);
    assertThat(foundSecurityVulnerability.getVector()).isNull();
    assertThat(foundSecurityVulnerability.getSecurityVulnerabilityCustomData().getCvssVector())
        .isEqualTo(customCvssVector.getVector());
  }

  @Test
  public void testGetComponent_SecurityVulnerabilityCustomSeverity() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent
        .setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3", "", "jar"));
    com.sonatype.clm.dto.model.SecurityVulnerability securityVulnerability =
        new com.sonatype.clm.dto.model.SecurityVulnerability("testRefId", "testSource", 7F);
    matchedComponent.setSecurityVulnerabilities(Collections.singletonList(securityVulnerability));

    VulnerabilityCustomCvssSeverity vulnerabilityCustomCvssSeverity = new VulnerabilityCustomCvssSeverity();
    vulnerabilityCustomCvssSeverity.setOwnerId(application.getId());
    vulnerabilityCustomCvssSeverity.setRefId(securityVulnerability.getRefId());
    vulnerabilityCustomCvssSeverity.setLastUpdatedByUsername("testUser");
    vulnerabilityCustomCvssSeverity.setSeverity(3F);
    vulnerabilityCustomCvssSeverityDAO.insert(vulnerabilityCustomCvssSeverity);

    Component component = componentLoader.getComponent(matchedComponent, true);

    assertThat(component.getSecurityVulnerabilities()).hasSize(1);
    SecurityVulnerability foundSecurityVulnerability = component.getSecurityVulnerabilities().get(0);
    assertThat(foundSecurityVulnerability.getSeverity()).isEqualTo(securityVulnerability.getSeverity());
    assertThat(foundSecurityVulnerability.getSecurityVulnerabilityCustomData().getCvssSeverity())
        .isEqualTo(vulnerabilityCustomCvssSeverity.getSeverity());
  }

  @Test
  public void testGetComponent_SecurityVulnerabilityCategory() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent
        .setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3", "", "jar"));
    com.sonatype.clm.dto.model.SecurityVulnerability securityVulnerability =
        new com.sonatype.clm.dto.model.SecurityVulnerability("testRefId", "testSource", 7F);
    securityVulnerability
        .setVulnerabilityCategories(Collections.singletonList(SecurityVulnerabilityCategory.CONFIGURATION.getId()));
    matchedComponent.setSecurityVulnerabilities(Collections.singletonList(securityVulnerability));

    Component component = componentLoader.getComponent(matchedComponent, true);

    assertThat(component.getSecurityVulnerabilities()).hasSize(1);
    SecurityVulnerability foundSecurityVulnerability = component.getSecurityVulnerabilities().get(0);
    assertThat(foundSecurityVulnerability.getVulnerabilityCategories())
        .containsExactly(SecurityVulnerabilityCategory.CONFIGURATION);
  }

  @Test
  public void testGetComponent_SecurityVulnerabilityAlias() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent
        .setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3", "", "jar"));
    com.sonatype.clm.dto.model.SecurityVulnerability securityVulnerability =
        new com.sonatype.clm.dto.model.SecurityVulnerability("testRefId", "testSource", 7F);
    securityVulnerability.setAliases(Collections.singletonList("testAlias"));
    matchedComponent.setSecurityVulnerabilities(Collections.singletonList(securityVulnerability));

    Component component = componentLoader.getComponent(matchedComponent, true);

    assertThat(component.getSecurityVulnerabilities()).hasSize(1);
    SecurityVulnerability foundSecurityVulnerability = component.getSecurityVulnerabilities().get(0);
    assertThat(foundSecurityVulnerability.getAliases()).containsExactly("testAlias");
  }

  @Test
  public void testGetComponent_SecurityVulnerabilityCustomRemediation() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent
        .setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3", "", "jar"));
    com.sonatype.clm.dto.model.SecurityVulnerability securityVulnerability =
        new com.sonatype.clm.dto.model.SecurityVulnerability("testRefId", "testSource", 7F);
    VulnerabilityCustomRemediation vulnerabilityCustomRemediation = new VulnerabilityCustomRemediation();
    vulnerabilityCustomRemediation.setRemediation("testRemediation");
    vulnerabilityCustomRemediation.setRefId(securityVulnerability.getRefId());
    vulnerabilityCustomRemediation.setOwnerId(application.getId());
    vulnerabilityCustomRemediation.setLastUpdatedByUsername("testUser");
    vulnerabilityCustomRemediationDAO.insert(vulnerabilityCustomRemediation);
    matchedComponent.setSecurityVulnerabilities(Collections.singletonList(securityVulnerability));

    Component component = componentLoader.getComponent(matchedComponent, true);

    assertThat(component.getSecurityVulnerabilities()).hasSize(1);
    SecurityVulnerability foundSecurityVulnerability = component.getSecurityVulnerabilities().get(0);
    assertThat(foundSecurityVulnerability.getSecurityVulnerabilityCustomData().getRemediation())
        .isEqualTo("testRemediation");
  }

  private void assertLicenseOverrideIsTheExpected(MatchedComponent matchedComponent, String licenseOverrideId) {
    // We need a new instance so the inserted LicenseOverrides are loaded. The LicenseOverride is only loaded
    // one time on first call to getComponent. This is aligned with the intentional design for the ComponentDAO
    Component component =
        componentLoaderFactory.createComponentLoader(application).getComponent(matchedComponent, true);
    assertThat(component).isNotNull();
    assertThat(component.getLicenseOverrideIds()).containsExactlyInAnyOrder(licenseOverrideId);
  }
}
