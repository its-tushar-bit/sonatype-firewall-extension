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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RepositoryContainerDAOTest
        extends AbstractDbDAOTest
{
  private RepositoryContainerDAO dao;

  @Before
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

    dao.setRelatedOrganizationId(organization.getId());

    assertThat(dao.getRelatedOrganizationId()).isEqualTo(organization.getId());
  }

  @Test
  public void testSetRelatedOrganizationId_InvalidOrganization() {
    String invalidOrganizationId = "nonexistent-id";

    assertThatThrownBy(() -> dao.setRelatedOrganizationId(invalidOrganizationId))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Organization not found");
  }
}
