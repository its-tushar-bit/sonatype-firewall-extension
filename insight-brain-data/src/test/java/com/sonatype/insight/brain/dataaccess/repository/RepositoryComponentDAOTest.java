/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;

import com.sonatype.clm.dto.model.component.AnalysisSource;
import com.sonatype.clm.dto.model.component.AnalysisType;
import com.sonatype.clm.dto.model.component.AnalyzerFeatures;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.LockedTransactionContext;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RepositoryComponentDAOTest
    extends AbstractDbDAOTest
{
  private RepositoryComponentDAO dao = new RepositoryComponentDAO();

  private Repository repositoryTwo;

  @Before
  public void before() {
    repositoryTwo = tempEntity.newRepository();
  }

  @Test
  public void testCRUD() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("groupId", "artifactId",
        "version");

    // Create
    Date createTime = new Date();
    RepositoryComponent repositoryComponent = new RepositoryComponent(repository.getId(), "path", createTime, "hash",
        componentIdentifier, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), createTime);
    String analyzerFeatures =
        JsonUtils.format(new AnalyzerFeatures(AnalysisSource.SDS, AnalysisType.COORDINATE, "client"));
    repositoryComponent.setAnalyzerFeaturesJson(analyzerFeatures);
    dao.insert(repositoryComponent);
    assertThat(repositoryComponent.getId()).isNotNull();

    // Get
    repositoryComponent = dao.getById(repositoryComponent.getId());
    assertThat(repositoryComponent).isNotNull();
    assertRepositoryComponent(repository.getId(), "path", createTime, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), createTime, repositoryComponent,
        analyzerFeatures);

    // Update
    Date updateTime = new Date();
    repositoryComponent.setLastEvaluationTime(updateTime);
    analyzerFeatures = JsonUtils.format(new AnalyzerFeatures(AnalysisSource.THIRD_PARTY, AnalysisType.HASH, "client"));
    repositoryComponent.setAnalyzerFeaturesJson(analyzerFeatures);
    dao.update(repositoryComponent);
    repositoryComponent = dao.getById(repositoryComponent.getId());
    assertThat(repositoryComponent).isNotNull();
    assertRepositoryComponent(repository.getId(), "path", createTime, "hash", componentIdentifier,
        MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), updateTime, repositoryComponent,
        analyzerFeatures);

    // Delete
    dao.delete(repositoryComponent);

    // Get
    repositoryComponent = dao.getById(repositoryComponent.getId());
    assertThat(repositoryComponent).isNull();
  }

  @Test
  public void testGetComponentCountByRepositoryId() {
    assertThat(dao.getComponentCountByRepositoryId(repository.getId())).isEqualTo(0);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));

    // Component in another repository
    tempEntity.newRepositoryComponent(repositoryTwo.getId());

    assertThat(dao.getComponentCountByRepositoryId(repository.getId())).isEqualTo(2);
  }

  @Test
  public void testGetKnownComponentCountByRepositoryId() {
    // Component in another repository
    tempEntity.newRepositoryComponent(repositoryTwo.getId());

    assertThat(dao.getKnownComponentCountByRepositoryId(repository.getId())).isEqualTo(0);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN,
        ComponentIdentifier.createMavenCoordinates("unknown", "component", "1"));
    assertThat(dao.getKnownComponentCountByRepositoryId(repository.getId())).isEqualTo(0);

    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    assertThat(dao.getKnownComponentCountByRepositoryId(repository.getId())).isEqualTo(1);
  }

  private void assertRepositoryComponent(String repositoryId,
                                         String pathname,
                                         Date time,
                                         String hash,
                                         ComponentIdentifier componentIdentifier,
                                         String matchStateId,
                                         String identificationSourceId,
                                         Date lastEvaluationTime,
                                         RepositoryComponent actual,
                                         String analyzerFeatures)
  {
    assertThat(actual.getRepositoryId()).isEqualTo(repositoryId);
    assertThat(actual.getPathname()).isEqualTo(pathname);
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getTime()).isEqualTo(time);
    assertThat(actual.getComponentIdentifier()).isEqualTo(componentIdentifier);
    assertThat(actual.getMatchStateId()).isEqualTo(matchStateId);
    assertThat(actual.getIdentificationSourceId()).isEqualTo(identificationSourceId);
    assertThat(actual.getLastEvaluationTime()).isEqualTo(lastEvaluationTime);
    assertThat(actual.getAnalyzerFeaturesJson()).isEqualTo(analyzerFeatures);
  }

  @Test
  public void testGetQuarantinedComponentCountByRepositoryId() {
    assertThat(dao.getQuarantinedComponentCountByRepositoryId(repository.getId())).isEqualTo(0);
    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined1", new Date(), null);
    assertThat(dao.getQuarantinedComponentCountByRepositoryId(repository.getId())).isEqualTo(1);
    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined2", new Date(), null);
    assertThat(dao.getQuarantinedComponentCountByRepositoryId(repository.getId())).isEqualTo(2);
    // unquarantined component, so shouldn't add to to total
    tempEntity.newRepositoryComponent(repository.getId(), "/quarantined3", new Date(), new Date());
    assertThat(dao.getQuarantinedComponentCountByRepositoryId(repository.getId())).isEqualTo(2);
    // not a quarantined item, shouldn't add to count
    tempEntity.newRepositoryComponent(repository.getId(), "/notquarantined", null, null);
    assertThat(dao.getQuarantinedComponentCountByRepositoryId(repository.getId())).isEqualTo(2);
  }

  @Test
  public void testGetOldestComponentEvaluationTimeByRepositoryId() {
    Date oldest = new Date();
    tempEntity.newRepositoryComponent(repository.getId(), new Date(oldest.getTime() + 1000));
    tempEntity.newRepositoryComponent(repository.getId(), oldest);
    tempEntity.newRepositoryComponent(repository.getId(), new Date(oldest.getTime() + 2000));

    assertThat(dao.getOldestComponentEvaluationTimeByRepositoryId(repository.getId())).isEqualTo(oldest);
  }

  @Test
  public void testGetCount() {
    // Component in another repository
    tempEntity.newRepositoryComponent(repositoryTwo.getId());
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT,
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));

    assertThat(dao.getCount()).isEqualTo(3);
  }

  @Test
  public void testDeleteByRepositoryId_H2() {
    assertThat(dao.isDatabaseEmbedded()).isTrue();

    RepositoryComponent repositoryComponent1 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    RepositoryComponent repositoryComponent2 =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
    LockedTransactionContext
        .createForRepositoryComponent(repository.getId(), repositoryComponent1.getPathname()).close();
    LockedTransactionContext
        .createForRepositoryComponent(repository.getId(), repositoryComponent2.getPathname()).close();
    assertThat(LockedTransactionContext.lockExists(LockedTransactionContext
        .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent1.getPathname()))).isTrue();
    assertThat(LockedTransactionContext.lockExists(LockedTransactionContext
        .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent2.getPathname()))).isTrue();

    dao.deleteByRepositoryId(null /* TransactionContext */, repository.getId());

    assertThat(LockedTransactionContext.lockExists(LockedTransactionContext
        .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent1.getPathname()))).isFalse();
    assertThat(LockedTransactionContext.lockExists(LockedTransactionContext
        .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent2.getPathname()))).isFalse();
    assertThat(dao.getByRepositoryId(repository.getId())).isEmpty();
  }

  @Test
  public void testDeleteByRepositoryId_Postgres() {
    DataSourceFactory.clear_ForTestsOnly();

    try (PostgresServer postgres = new PostgresServer()) {
      // Create a postgres ODS database
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);

      assertThat(dao.isDatabaseEmbedded()).isFalse();

      repository = tempEntity.newRepository();
      RepositoryComponent repositoryComponent1 =
          tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
      RepositoryComponent repositoryComponent2 =
          tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
      tempEntity.newRepositoryComponent(repository.getId(), MatchState.UNKNOWN, null);
      assertThat(dao.getByRepositoryId(repository.getId())).hasSize(3);
      LockedTransactionContext
          .createForRepositoryComponent(repository.getId(), repositoryComponent1.getPathname()).close();
      LockedTransactionContext
          .createForRepositoryComponent(repository.getId(), repositoryComponent2.getPathname()).close();
      assertThat(LockedTransactionContext.lockExists(LockedTransactionContext
          .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent1.getPathname()))).isTrue();
      assertThat(LockedTransactionContext.lockExists(LockedTransactionContext
          .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent2.getPathname()))).isTrue();

      try (TransactionContext tx = dao.createTransactionContext()) {
        tx.begin();
        dao.deleteByRepositoryId(tx, repository.getId());
        tx.commit();
      }

      assertThat(LockedTransactionContext.lockExists(LockedTransactionContext
          .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent1.getPathname()))).isFalse();
      assertThat(LockedTransactionContext.lockExists(LockedTransactionContext
          .getLockIdForRepositoryComponent(repository.getId(), repositoryComponent2.getPathname()))).isFalse();
      assertThat(dao.getByRepositoryId(repository.getId())).isEmpty();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }
}
