/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.MatchedComponent;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentDAOTest
    extends AbstractDbDAOTest
{
  private static final String COMP_HASH = "12345678901234567890";

  private LabelDAO labelDAO = new LabelDAO();

  private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  @Before
  public void createLTGs() {
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
    Component component = new ComponentDAO(application).getComponent(matchedComponent);
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
  }

  @Test
  public void testGetComponent_LicenseOverride() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3"));
    Component component = new ComponentDAO(application).getComponent(matchedComponent);
    assertThat(component).isNotNull();
    assertThat(component.getLicenseOverrideIds()).isEmpty();

    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    // Override at org level
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3", null,
        null);
    LicenseOverride orgLicenseOverride = new LicenseOverride(organization.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "GPL-3.0", "My comment");
    licenseOverrideDAO.insert(orgLicenseOverride);
    component = new ComponentDAO(application).getComponent(matchedComponent);
    assertThat(component).isNotNull();
    assertThat(component.getLicenseOverrideIds()).containsExactlyInAnyOrder("GPL-3.0");

    // Override at app level
    LicenseOverride appLicenseOverride = new LicenseOverride(application.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My comment");
    licenseOverrideDAO.insert(appLicenseOverride);
    component = new ComponentDAO(application).getComponent(matchedComponent);
    assertThat(component).isNotNull();
    assertThat(component.getLicenseOverrideIds()).containsExactlyInAnyOrder("GPL-2.0");
  }

  @Test
  public void testGetComponent_LegacyLicenseOverride() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent
        .setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3", "", "jar"));
    Component component = new ComponentDAO(application).getComponent(matchedComponent);
    assertThat(component).isNotNull();
    assertThat(component.getLicenseOverrideIds()).isEmpty();

    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    // New-style override at org level
    ComponentIdentifier componentIdentifier = matchedComponent.getComponentIdentifier();
    LicenseOverride orgLicenseOverride = new LicenseOverride(organization.getId(), componentIdentifier,
        LicenseOverrideStatus.OVERRIDDEN, "GPL-3.0", "My comment");
    licenseOverrideDAO.insert(orgLicenseOverride);
    component = new ComponentDAO(application).getComponent(matchedComponent);
    assertThat(component).isNotNull();
    assertThat(component.getLicenseOverrideIds()).containsExactlyInAnyOrder("GPL-3.0");

    // Legacy override at app level
    LicenseOverride appLicenseOverride =
        new LicenseOverride(application.getId(), ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3"),
            LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My comment");
    licenseOverrideDAO.insert(appLicenseOverride);
    component = new ComponentDAO(application).getComponent(matchedComponent);
    assertThat(component).isNotNull();
    assertThat(component.getLicenseOverrideIds()).containsExactlyInAnyOrder("GPL-2.0");
  }

  @Test
  public void testGetComponent_MultiLicenses_Declared() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3"));
    matchedComponent.addDeclaredLicenseId("Apache-2.0-GPL-2.0");
    matchedComponent.addDeclaredLicenseId("Apache-2.0-GPL-3.0");
    Component component = new ComponentDAO(application).getComponent(matchedComponent);
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
    Component component = new ComponentDAO(application).getComponent(matchedComponent);
    assertThat(component).isNotNull();
    assertThat(component.getObservedLicenseIds()).containsExactlyInAnyOrder("Apache-2.0", "GPL-2.0", "GPL-3.0");
    assertThat(component.getObservedMultiLicenseIds())
        .containsExactlyInAnyOrder("Apache-2.0-GPL-3.0", "Apache-2.0-GPL-2.0");
    assertLicenseThreatGroups(component.getLicenseThreatGroups(), "My group 1", "My group 2", "My group 3");
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
        new ComponentDAO(application).getAll(null, null, objectMapper.writeValueAsBytes(bom), null);

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
}
