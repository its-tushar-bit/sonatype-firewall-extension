/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MembershipMapping;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IdUtilsTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Test
  public void testGetInternalOwnerId_Global() {
    String id = IdUtils.getInternalOwnerId(OwnerType.GLOBAL, null /* ownerId */);
    assertThat(id, is(MembershipMapping.GLOBAL_CONTEXT_ID));
  }

  @Test
  public void testGetInternalOwnerId_RepositoryContainer() {
    String id = IdUtils.getInternalOwnerId(OwnerType.REPOSITORY_CONTAINER, null /* ownerId */);
    assertThat(id, is(RepositoryContainer.REPOSITORY_CONTAINER_ID));
  }

  @Test
  public void testGetInternalOwnerId_Repository() {
    Repository repository = tempEntity.newRepository();
    String id = IdUtils.getInternalOwnerId(OwnerType.REPOSITORY, repository.getId());
    assertThat(id, is(repository.getId()));
  }
}
