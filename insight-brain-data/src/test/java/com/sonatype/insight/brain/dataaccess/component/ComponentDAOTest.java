/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.MatchedComponent;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;

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
    matchedComponent
        .addSecurityVulnerability(new com.sonatype.clm.dto.model.SecurityVulnerability("12345", "osvdb", 4f));
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

    assertThat(component.getLicenseOverrideIds()).isEmpty();
    assertLicenseThreatGroups(component.getLicenseThreatGroups(), "My group 1");
    assertSecurityVulnerabilities(component.getSecurityVulnerabilities(),
        newSV("12345", "osvdb", 4f, SecurityVulnerabilityOverrideStatus.OPEN));

    assertThat(component.getLabelIds()).containsExactlyInAnyOrder(appLabel.getId(), orgLabel.getId());
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
    assertLicenseThreatGroups(component.getLicenseThreatGroups(), "My group 1", "My group 2", "My group 3");
  }
}
