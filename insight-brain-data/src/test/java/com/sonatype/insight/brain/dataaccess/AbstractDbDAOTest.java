/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractDbDAOTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  protected Application application;

  protected String applicationId;

  protected Organization organization;

  private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  @Before
  public void setup() {
    // Make sure the default LTGs are created on the root organization
    licenseThreatGroupDAO.createDefaultLicenseThreatGroups();
    organization = tempEntity.newOrganization("AbstractDbDAOTest");
    application = tempEntity.newApplication("AbstractDbDAOTest-AppName", "AbstractDbDAOTest-AppPublicId",
        organization.getId());
    applicationId = application.getId();
  }

  @After
  public void deleteDefaultLicenseThreatGroups() {
    // Delete the default LTGs from the root organization
    licenseThreatGroupDAO.deleteDefaultLicenseThreatGroups();
  }
}
