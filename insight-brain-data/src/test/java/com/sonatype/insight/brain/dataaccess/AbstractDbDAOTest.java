/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;

import org.junit.Before;
import org.junit.Rule;

/**
 * This base class is created with the only purpose of test DAOs, and it should only be used for that. This base test
 * class will ensure a DB is provisioned * and ready to be used in your tests.
 */
public abstract class AbstractDbDAOTest
    extends AbstractDataTest
{
  @Rule
  public DatamartUpdaterState datamartUpdaterState = new DatamartUpdaterState();

  protected Application application;

  protected Organization organization;

  protected Repository repository;

  @Before
  public void setup() {
    organization = tempEntity.newOrganization("AbstractDbDAOTest");
    application = tempEntity.newApplication("AbstractDbDAOTest-AppName", "AbstractDbDAOTest-AppPublicId",
        organization.getId());
    repository = tempEntity.newRepository();
  }
}
