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
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

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

  protected RepositoryManager repositoryManager;

  @Before
  @BeforeEach
  public void setup() {
    String uniquePrefix = getClass().getSimpleName() + "_" + TemporaryEntity.uuid();
    organization = tempEntity.newOrganization(uniquePrefix);
    application = tempEntity.newApplication(uniquePrefix + "-AppName", uniquePrefix + "-AppPublicId",
        organization.getId());
    repositoryManager = tempEntity.newRepositoryManager();
    repository = tempEntity.newRepository(repositoryManager);

  }

  // JUnit 5 (Jupiter): drive the @Rule DatamartUpdaterState.before() (inert under Vintage). setup() is
  // dual-annotated (@Before + @BeforeEach) above so Jupiter invokes it once and a subclass override replaces it;
  // do NOT call setup() explicitly here (an overriding @BeforeEach setup would run twice -> double inserts).
  @BeforeEach
  public void jupiterInitDatamart() {
    datamartUpdaterState.before();
  }

  @AfterEach
  public void jupiterCleanupDbDAO() {
    datamartUpdaterState.after();
  }
}
