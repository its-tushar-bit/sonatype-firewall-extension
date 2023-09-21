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
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class IdUtilsTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @Test
  public void testGetOwnerNotNull_Global() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> IdUtils.getOwnerNotNull(OwnerType.GLOBAL, null /* ownerId */))
        .withMessage(IdUtils.MSG_PREFIX_NO_OWNER_INSTANCE + OwnerType.GLOBAL);
  }

  private void assertOwnerEqualPrivateId(final Owner expected) {
    final Owner actual = IdUtils.getOwnerNotNull(expected.getType(), expected.getId());
    assertOwnerEqual(expected, actual);
  }

  private void assertOwnerEqual(final Owner expected, final Owner actual) {
    assertThat(actual.getType()).isEqualTo(expected.getType());
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getPublicId()).isEqualTo(expected.getPublicId());
    assertThat(actual.getParentOwnerId()).isEqualTo(expected.getParentOwnerId());
  }

  @Test
  public void testGetOwnerNotNull_PrivateId() {
    assertOwnerEqualPrivateId(RepositoryContainer.SINGLETON);
    assertOwnerEqualPrivateId(tempEntity.newRepository());
    assertOwnerEqualPrivateId(tempEntity.newOrganization());
    assertOwnerEqualPrivateId(tempEntity.newRepositoryManager());
  }

  @Test
  public void testGetOwnerNotNull_PublicId() {
    final Application owner = tempEntity.newApplication(tempEntity.newOrganization().getId());
    final Owner actual = IdUtils.getOwnerNotNull(owner.getType(), owner.getPublicId());
    assertOwnerEqual(owner, actual);
  }

  @Test
  public void testGetOwnerNotNull_ApplicationInternalId() {
    final Application owner = tempEntity.newApplication(tempEntity.newOrganization().getId());
    final Owner actual = IdUtils.getOwnerNotNull(owner.getType(), owner.getId());
    assertOwnerEqual(owner, actual);
  }

  @Test
  public void testGetInternalOwnerId_Global() {
    String id = IdUtils.getInternalOwnerId(OwnerType.GLOBAL, null /* ownerId */);
    assertThat(id).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testGetInternalOwnerId_RepositoryContainer() {
    String id = IdUtils.getInternalOwnerId(OwnerType.REPOSITORY_CONTAINER, null /* ownerId */);
    assertThat(id).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testGetInternalOwnerId_RepositoryManager() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    String id = IdUtils.getInternalOwnerId(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId());
    assertThat(id).isEqualTo(repositoryManager.getId());
  }

  @Test
  public void testGetInternalOwnerId_Repository() {
    Repository repository = tempEntity.newRepository();
    String id = IdUtils.getInternalOwnerId(OwnerType.REPOSITORY, repository.getId());
    assertThat(id).isEqualTo(repository.getId());
  }

  @Test
  public void testGetInternalOwnerId_ApplicationInternalId() {
    Application application = tempEntity.newApplicationWithParent();
    String internalOwnerId = IdUtils.getInternalOwnerId(application.getType(), application.getId());
    assertThat(internalOwnerId).isEqualTo(application.getId());
  }

  @Test
  public void testGetInternalOwnerId_ApplicationPublicId() {
    Application application = tempEntity.newApplicationWithParent();
    String internalOwnerId = IdUtils.getInternalOwnerId(application.getType(), application.getPublicId());
    assertThat(internalOwnerId).isEqualTo(application.getId());
  }

  @Test
  public void testGetInternalOwnerId_Application_NotFound() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> IdUtils.getInternalOwnerId(OwnerType.APPLICATION, "no-such-app-id"))
        .withMessage("Application with ID no-such-app-id does not exist.");
  }

  @Test
  public void testGetPublicOwnerId_Global() {
    String id = IdUtils.getPublicOwnerId(OwnerType.GLOBAL, null /* ownerId */);
    assertThat(id).isEqualTo(MembershipMapping.GLOBAL_CONTEXT_ID);
  }

  @Test
  public void testGetPublicOwnerId_RepositoryContainer() {
    String id = IdUtils.getPublicOwnerId(OwnerType.REPOSITORY_CONTAINER, null /* ownerId */);
    assertThat(id).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testGetPublicOwnerId_Repository() {
    Repository repository = tempEntity.newRepository();
    String id = IdUtils.getPublicOwnerId(OwnerType.REPOSITORY, repository.getId());
    assertThat(id).isEqualTo(repository.getPublicId());
  }

  @Test
  public void testGetPublicOwnerId_RepositoryManager() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    String id = IdUtils.getPublicOwnerId(OwnerType.REPOSITORY_MANAGER, repositoryManager.getId());
    assertThat(id).isEqualTo(repositoryManager.getPublicId());
  }

  @Test
  public void testGetPublicOwnerId_ApplicationInternalId() {
    Application application = tempEntity.newApplicationWithParent();
    String publicOwnerId = IdUtils.getPublicOwnerId(application.getType(), application.getId());
    assertThat(publicOwnerId).isEqualTo(application.getPublicId());
  }

  @Test
  public void testGetPublicOwnerId_ApplicationPublicId() {
    Application application = tempEntity.newApplicationWithParent();
    String publicOwnerId = IdUtils.getPublicOwnerId(application.getType(), application.getPublicId());
    assertThat(publicOwnerId).isEqualTo(application.getPublicId());
  }

  @Test
  public void testGetPublicOwnerId_Application_NotFound() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> IdUtils.getPublicOwnerId(OwnerType.APPLICATION, "no-such-app-public-id"))
        .withMessage("Application with ID no-such-app-public-id does not exist.");
  }
}
