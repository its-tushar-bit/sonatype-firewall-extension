/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.ide.MatchedComponent;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

import org.hamcrest.core.IsCollectionContaining;
import org.junit.Before;
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

  @Before
  public void before() {
    createDefaultApplication();
  }

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

  @Test
  public void testGetComponent() {
    Label appLabel = new Label(applicationId, "red", null);
    labelDAO.insert(appLabel);
    Label orgLabel = new Label(application.getOrganizationId(), "blue", null);
    labelDAO.insert(orgLabel);
    componentLabelDAO.insert(new ComponentLabel(applicationId, appLabel.getId(), COMP_HASH));
    componentLabelDAO.insert(new ComponentLabel(application.getOrganizationId(), orgLabel.getId(), COMP_HASH));

    MatchedComponent info = new MatchedComponent();
    info.setHash(COMP_HASH);
    info.setGroupId("gid");
    info.setArtifactId("aid");
    info.setVersion("1.2.3");
    info.setMatchState("similar");
    info.setCatalogDate(System.currentTimeMillis());
    info.setRelativePopularity(42);
    info.addDeclaredLicenseId("Apache-2.0");
    info.addObservedLicenseId("MIT");
    info.addSecurityVulnerability(new SecurityVulnerability("12345", "osvdb", 4f));
    Component comp = componentDAO.getComponent(applicationId, info, null, null);
    assertNotNull(comp);
    assertEquals(info.getHash(), comp.getHash());
    assertEquals(info.getGroupId(), comp.getGroupId());
    assertEquals(info.getArtifactId(), comp.getArtifactId());
    assertEquals(info.getVersion(), comp.getVersion());
    assertEquals(info.getMatchState(), comp.getMatchState().getId());
    assertEquals(info.getCatalogDate(), comp.getCatalogDate());
    assertEquals(info.getRelativePopularity(), new Integer(comp.getRelativePopularity()));
    assertEquals(info.getDeclaredLicenseIds(), comp.getDeclaredLicenseIds());
    assertEquals(info.getObservedLicenseIds(), comp.getObservedLicenseIds());

    assertNull(comp.getLicenseOverrideId());
    assertLicenseThreatGroups(comp.getLicenseThreatGroups(), "Liberal");
    assertSecurityVulnerabilities(comp.getSecurityVulnerabilities(),
        newSV("12345", "osvdb", 4f, SecurityVulnerabilityStatus.OPEN));

    assertEquals(2, comp.getLabelIds().size());
    assertThat(comp.getLabelIds(), IsCollectionContaining.hasItems(appLabel.getId(), orgLabel.getId()));
  }

  @Test
  public void testGetComponent_MultiLicenses_Declared() {
    MatchedComponent componentInfo = new MatchedComponent();
    componentInfo.setHash(COMP_HASH);
    componentInfo.setGroupId("gid");
    componentInfo.setArtifactId("aid");
    componentInfo.setVersion("1.2.3");
    componentInfo.addDeclaredLicenseId("Apache-2.0-GPL-2.0");
    Component component = componentDAO.getComponent(applicationId, componentInfo, null, null);
    assertNotNull(component);
    assertEquals(component.getDeclaredLicenseIds().toString(), 2, component.getDeclaredLicenseIds().size());
    assertTrue(component.getDeclaredLicenseIds().contains("Apache-2.0"));
    assertTrue(component.getDeclaredLicenseIds().contains("GPL-2.0"));
    assertLicenseThreatGroups(component.getLicenseThreatGroups(), "Liberal", "Copyleft");
  }

  @Test
  public void testGetComponent_MultiLicenses_Observed() {
    MatchedComponent componentInfo = new MatchedComponent();
    componentInfo.setHash(COMP_HASH);
    componentInfo.setGroupId("gid");
    componentInfo.setArtifactId("aid");
    componentInfo.setVersion("1.2.3");
    componentInfo.addObservedLicenseId("Apache-2.0-GPL-2.0");
    Component component = componentDAO.getComponent(applicationId, componentInfo, null, null);
    assertNotNull(component);
    assertEquals(component.getObservedLicenseIds().toString(), 2, component.getObservedLicenseIds().size());
    assertTrue(component.getObservedLicenseIds().contains("Apache-2.0"));
    assertTrue(component.getObservedLicenseIds().contains("GPL-2.0"));
    assertLicenseThreatGroups(component.getLicenseThreatGroups(), "Liberal", "Copyleft");
  }
}
