/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LicenseThreatGroupValueTypeTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private Organization org;

  private Application app;

  @Before
  public void setUp() throws Exception {
    org = tempEntity.newOrganization("orgName");
    app = tempEntity.newApplication("appName", "appId", org.getId());
    tempEntity.newLicenseThreatGroup(app.getId());
  }

  @Test
  public void testGetAvailableValues_AppLevel() {
    LicenseThreatGroupValueType type = new LicenseThreatGroupValueType(app.getId());
    List<LicenseThreatGroup> ltgs = type.getAvailableValues();
    assertNotNull(ltgs);
    assertEquals(5, ltgs.size());
  }

  @Test
  public void testGetAvailableValues_OrgLevel() {
    LicenseThreatGroupValueType type = new LicenseThreatGroupValueType(org.getId());
    List<LicenseThreatGroup> ltgs = type.getAvailableValues();
    assertNotNull(ltgs);
    assertEquals(4, ltgs.size());
  }
}
