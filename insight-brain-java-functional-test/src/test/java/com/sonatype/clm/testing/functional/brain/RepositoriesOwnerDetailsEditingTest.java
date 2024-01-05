/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;

import org.junit.Before;

public class RepositoriesOwnerDetailsEditingTest
    extends AbstractOwnerDetailsEditingTest
{
  @Before
  public void init() {
    OwnerDAO ownerDAO = lookup(OwnerDAO.class);
    super.init(ownerDAO.getById(RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }
}
