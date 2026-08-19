/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RepositoryContainerDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryContainerDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRepositoryContainerDAO();
  }

  @Test
  public void testGetInstance() {
    RepositoryContainer repositoryContainer = dao.getInstance();
    assertThat(repositoryContainer).isNotNull();
    assertThat(repositoryContainer.getId()).isEqualTo(RepositoryContainer.REPOSITORY_CONTAINER_ID);
  }

  @Test
  public void testGetAndSetRelatedOrganizationId() {
    Organization organization = tempEntity.newOrganization();

    dao.setRelatedOrganizationIdNotNull(organization.getId());

    assertThat(dao.getRelatedOrganizationId()).isEqualTo(organization.getId());
  }

  @Test
  public void testSetRelatedOrganizationId_InvalidOrganization() {
    String invalidOrganizationId = "nonexistent-id";

    assertThatThrownBy(() -> dao.setRelatedOrganizationIdNotNull(invalidOrganizationId))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Organization not found");
  }

  @Test
  public void testInsert_UnsupportedOperation() {
    RepositoryContainer repositoryContainer = dao.getInstance();

    assertThatThrownBy(() -> dao.insert(null, repositoryContainer))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("RepositoryContainerDAO does not support insert");
  }

  @Test
  public void testUpdate_UnsupportedOperation() {
    RepositoryContainer repositoryContainer = dao.getInstance();

    assertThatThrownBy(() -> dao.update(null, repositoryContainer))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("RepositoryContainerDAO does not support update");
  }

  @Test
  public void testDelete_UnsupportedOperation() {
    RepositoryContainer repositoryContainer = dao.getInstance();

    assertThatThrownBy(() -> dao.delete(null, repositoryContainer))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("RepositoryContainerDAO does not support delete");
  }
}
