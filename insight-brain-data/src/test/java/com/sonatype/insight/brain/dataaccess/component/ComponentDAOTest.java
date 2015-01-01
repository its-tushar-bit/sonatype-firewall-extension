/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.ide.MatchedComponent;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ComponentDAOTest
    extends AbstractDbDAOTest
{

  private static final String COMP_HASH = "12345678901234567890";

  private ComponentDAO componentDAO = new ComponentDAO();

  private LabelDAO labelDAO = new LabelDAO();

  private ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();

  private com.sonatype.insight.brain.model.component.SecurityVulnerability newSV(String refId, String source,
      Float severity, SecurityVulnerabilityStatus status)
  {
    com.sonatype.insight.brain.model.component.SecurityVulnerability sv = new com.sonatype.insight.brain.model.component.SecurityVulnerability(
        source, refId, severity);
    sv.setStatus(status);
    return sv;
  }

  private void assertSecurityVulnerabilities(
      List<com.sonatype.insight.brain.model.component.SecurityVulnerability> actual,
      com.sonatype.insight.brain.model.component.SecurityVulnerability... expected)
  {
    assertEquals(expected.length, actual.size());
    for (int i = 0, n = expected.length; i < n; i++) {
      assertSecurityVulnerability(expected[i], actual.get(i));
    }
  }

  private void assertSecurityVulnerability(com.sonatype.insight.brain.model.component.SecurityVulnerability expected,
      com.sonatype.insight.brain.model.component.SecurityVulnerability actual)
  {
    assertEquals(expected.getRefId(), actual.getRefId());
    assertEquals(expected.getSource(), actual.getSource());
    assertEquals(expected.getSeverity(), actual.getSeverity());
    assertEquals(expected.getStatus(), actual.getStatus());
  }

  private void assertLicenseThreatGroups(Set<LicenseThreatGroup> actual, String... expected) {
    Set<String> actualNames = new TreeSet<String>();
    for (LicenseThreatGroup group : actual) {
      actualNames.add(group.getName());
    }
    assertEquals(new TreeSet<String>(Arrays.asList(expected)), actualNames);
  }

  @SuppressWarnings("deprecation")
  private void assertGav(MatchedComponent expectedMatchedComponent, Component actualComponent) {
    assertEquals(expectedMatchedComponent.getGroupId(), actualComponent.getGroupId());
    assertEquals(expectedMatchedComponent.getArtifactId(), actualComponent.getArtifactId());
    assertEquals(expectedMatchedComponent.getVersion(), actualComponent.getVersion());
  }

  @Test
  public void testGetComponent() {
    Label appLabel = new Label(applicationId, "red");
    labelDAO.insert(appLabel);
    Label orgLabel = new Label(application.getOrganizationId(), "blue");
    labelDAO.insert(orgLabel);
    componentLabelDAO.insert(new ComponentLabel(applicationId, appLabel.getId(), COMP_HASH));
    componentLabelDAO.insert(new ComponentLabel(application.getOrganizationId(), orgLabel.getId(), COMP_HASH));

    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3"));
    matchedComponent.setMatchState("similar");
    matchedComponent.setCatalogDate(System.currentTimeMillis());
    matchedComponent.setRelativePopularity(42);
    matchedComponent.addDeclaredLicenseId("Apache-2.0");
    matchedComponent.addObservedLicenseId("MIT");
    matchedComponent.addSecurityVulnerability(new SecurityVulnerability("12345", "osvdb", 4f));
    Component component = componentDAO.getComponent(application, matchedComponent, null);
    assertNotNull(component);
    assertEquals(matchedComponent.getHash(), component.getHash());
    assertEquals(matchedComponent.getComponentIdentifier(), component.getComponentIdentifier());
    assertGav(matchedComponent, component);
    assertEquals(matchedComponent.getMatchState(), component.getMatchState().getId());
    assertEquals(matchedComponent.getCatalogDate(), component.getCatalogDate());
    assertEquals(matchedComponent.getRelativePopularity(), new Integer(component.getRelativePopularity()));
    assertEquals(matchedComponent.getDeclaredLicenseIds(), component.getDeclaredLicenseIds());
    assertEquals(matchedComponent.getObservedLicenseIds(), component.getObservedLicenseIds());

    assertNull(component.getLicenseOverrideId());
    assertLicenseThreatGroups(component.getLicenseThreatGroups(), "Liberal");
    assertSecurityVulnerabilities(component.getSecurityVulnerabilities(),
        newSV("12345", "osvdb", 4f, SecurityVulnerabilityStatus.OPEN));

    assertEquals(2, component.getLabelIds().size());
    assertThat(component.getLabelIds(), IsCollectionContaining.hasItems(appLabel.getId(), orgLabel.getId()));
  }

  @Test
  public void testGetComponent_LicenseOverride() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3"));
    Component component = componentDAO.getComponent(application, matchedComponent, null);
    assertNotNull(component);
    assertNull(component.getLicenseOverrideId());

    LicenseOverrideDAO licenseOverrideDAO = new LicenseOverrideDAO();
    // Override at org level
    ComponentIdentifier componentIdentifier = ComponentIdentifier
      .createMavenCoordinates("gid", "aid", "1.2.3", null, null);
    LicenseOverride orgLicenseOverride = new LicenseOverride(organization.getId(), componentIdentifier,
      LicenseOverrideStatus.OVERRIDDEN, "GPL-3.0", "My comment");
    licenseOverrideDAO.insert(orgLicenseOverride);
    component = componentDAO.getComponent(application, matchedComponent, null);
    assertNotNull(component);
    assertEquals("GPL-3.0", component.getLicenseOverrideId());

    // Override at app level
    LicenseOverride appLicenseOverride = new LicenseOverride(application.getId(), componentIdentifier,
      LicenseOverrideStatus.OVERRIDDEN, "GPL-2.0", "My comment");
    licenseOverrideDAO.insert(appLicenseOverride);
    component = componentDAO.getComponent(application, matchedComponent, null);
    assertNotNull(component);
    assertEquals("GPL-2.0", component.getLicenseOverrideId());
  }

  @Test
  public void testGetComponent_MultiLicenses_Declared() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3"));
    matchedComponent.addDeclaredLicenseId("Apache-2.0-GPL-2.0");
    Component component = componentDAO.getComponent(application, matchedComponent, null);
    assertNotNull(component);
    assertEquals(component.getDeclaredLicenseIds().toString(), 2, component.getDeclaredLicenseIds().size());
    assertTrue(component.getDeclaredLicenseIds().contains("Apache-2.0"));
    assertTrue(component.getDeclaredLicenseIds().contains("GPL-2.0"));
    assertLicenseThreatGroups(component.getLicenseThreatGroups(), "Liberal", "Copyleft");
  }

  @Test
  public void testGetComponent_MultiLicenses_Observed() {
    MatchedComponent matchedComponent = new MatchedComponent();
    matchedComponent.setHash(COMP_HASH);
    matchedComponent.setComponentIdentifier(ComponentIdentifier.createMavenCoordinates("gid", "aid", "1.2.3"));
    matchedComponent.addObservedLicenseId("Apache-2.0-GPL-2.0");
    Component component = componentDAO.getComponent(application, matchedComponent, null);
    assertNotNull(component);
    assertEquals(component.getObservedLicenseIds().toString(), 2, component.getObservedLicenseIds().size());
    assertTrue(component.getObservedLicenseIds().contains("Apache-2.0"));
    assertTrue(component.getObservedLicenseIds().contains("GPL-2.0"));
    assertLicenseThreatGroups(component.getLicenseThreatGroups(), "Liberal", "Copyleft");
  }
}
