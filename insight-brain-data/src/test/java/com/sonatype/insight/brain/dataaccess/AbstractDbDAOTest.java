/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractDbDAOTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  protected Application application;

  protected String applicationId;

  protected Organization organization;

  @Before
  public void setup() {
    organization = tempEntity.newOrganization("AbstractDbDAOTest");
    application = tempEntity.newApplication("AbstractDbDAOTest-AppName", "AbstractDbDAOTest-AppPublicId",
        organization.getId());
    applicationId = application.getId();
  }
}
