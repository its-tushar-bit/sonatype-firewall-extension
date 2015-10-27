/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.repository.Repository;

import org.junit.Before;

public class CIComponentInfoResourceRepositoryTest
    extends AbstractComponentInfoResourceTest
{
  private Repository repository;

  @Before
  public void setUp() {
    repository = tempEntity.newRepository();
  }

  @Override
  protected String getResourcePath() {
    return CIComponentInfoResource.RESOURCE_PATH;
  }

  protected Owner getOwner() {
    return repository;
  }

  @Override
  protected String getOwnerId() {
    return repository.getId();
  }
}
