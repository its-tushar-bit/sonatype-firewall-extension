/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LicenseThreatGroupValueTypeTest
{
  private Organization org;

  private Application app;

  @Before
  public void setUp() throws Exception {
    org = new Organization("orgName");
    new OrganizationDAO().insert(org);
    app = new Application();
    app.setName("appName");
    app.setPublicId("appId");
    app.setOrganizationId(org.getId());
    new ApplicationDAO().insert(app);
    LicenseThreatGroup ltg = new LicenseThreatGroup(app.getId(), "ltgName", 5);
    new LicenseThreatGroupDAO().insert(ltg);
  }

  @After
  public void tearDown() throws Exception {
    ApplicationDAO appDAO = new ApplicationDAO();
    for (Application app : appDAO.getAll()) {
      appDAO.delete(app);
    }
    OrganizationDAO orgDAO = new OrganizationDAO();
    for (Organization org : orgDAO.getAll()) {
      orgDAO.delete(org);
    }
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
