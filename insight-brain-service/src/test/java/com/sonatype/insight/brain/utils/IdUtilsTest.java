/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.brain.model.security.MembershipMapping;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class IdUtilsTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Test
  public void testGetOwnerNotNull_Global() {
    try {
      IdUtils.getOwnerNotNull(OwnerType.GLOBAL, null /* ownerId */);
      fail("Should fail to get 'GLOBAL' owner object.");
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is(IdUtils.MSG_PREFIX_NO_OWNER_INSTANCE + OwnerType.GLOBAL));
    }
  }

  private void assertOwnerEqualPrivateId(final Owner expected) {
    final Owner actual = IdUtils.getOwnerNotNull(expected.getType(), expected.getId());
    assertOwnerEqual(expected, actual);
  }
  private void assertOwnerEqual(final Owner expected, final Owner actual) {
    assertThat(actual.getType(), is(expected.getType()));
    assertThat(actual.getId(), is(expected.getId()));
    assertThat(actual.getPublicId(), is(expected.getPublicId()));
    assertThat(actual.getParentOwnerId(), is(expected.getParentOwnerId()));
  }

  @Test
  public void testGetOwnerNotNull_PrivateId() {
    assertOwnerEqualPrivateId(RepositoryContainer.SINGLETON);
    assertOwnerEqualPrivateId(tempEntity.newRepository());
    assertOwnerEqualPrivateId(tempEntity.newOrganization());
  }

  @Test
  public void testGetOwnerNotNull_PublicId() {
    final Application owner = tempEntity.newApplication(tempEntity.newOrganization().getId());
    final Owner actual = IdUtils.getOwnerNotNull(owner.getType(), owner.getPublicId());
    assertOwnerEqual(owner, actual);
  }

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
