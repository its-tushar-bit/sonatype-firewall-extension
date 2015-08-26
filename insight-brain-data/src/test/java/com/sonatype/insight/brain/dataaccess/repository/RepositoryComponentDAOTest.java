/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class RepositoryComponentDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryComponentDAO dao = new RepositoryComponentDAO();

  private Repository repository;

  @Before
  public void before() {
    repository = tempEntity.newRepository();
  }

  @Test
  public void testCRUD() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("groupId", "artifactId",
        "version");

    // Create
    Date createTime = new Date();
    RepositoryComponent repositoryComponent = new RepositoryComponent(repository.getId(), "path", createTime, "hash",
        componentIdentifier, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), createTime, true /* canBeQuarantined */);
    dao.insert(repositoryComponent);
    assertThat(repositoryComponent.getId(), notNullValue());

    // Get
    repositoryComponent = dao.getById(repositoryComponent.getId());
    assertThat(repositoryComponent, notNullValue());
    assertRepositoryComponent(repository.getId(), "path", createTime, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), createTime, true /* canBeQuarantined */,
        repositoryComponent);

    // Update
    Date updateTime = new Date();
    repositoryComponent.setLastEvaluationTime(updateTime);
    dao.update(repositoryComponent);
    repositoryComponent = dao.getById(repositoryComponent.getId());
    assertThat(repositoryComponent, notNullValue());
    assertRepositoryComponent(repository.getId(), "path", createTime, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), updateTime, true /* canBeQuarantined */,
        repositoryComponent);

    // Delete
    dao.delete(repositoryComponent);

    // Get
    repositoryComponent = dao.getById(repositoryComponent.getId());
    assertThat(repositoryComponent, nullValue());
  }

  private void assertRepositoryComponent(String repositoryId, String pathname, Date time, String hash,
      ComponentIdentifier componentIdentifier, String matchStateId, String identificationSourceId,
      Date lastEvaluationTime, boolean canBeQuarantined, RepositoryComponent actual)
  {
    assertThat(actual.getRepositoryId(), is(repositoryId));
    assertThat(actual.getPathname(), is(pathname));
    assertThat(actual.getHash(), is(hash));
    assertThat(actual.getTime(), is(time));
    assertThat(actual.getComponentIdentifier(), is(componentIdentifier));
    assertThat(actual.getMatchStateId(), is(matchStateId));
    assertThat(actual.getIdentificationSourceId(), is(identificationSourceId));
    assertThat(actual.getLastEvaluationTime(), is(lastEvaluationTime));
    assertThat(actual.isCanBeQuarantined(), is(canBeQuarantined));
  }
}
