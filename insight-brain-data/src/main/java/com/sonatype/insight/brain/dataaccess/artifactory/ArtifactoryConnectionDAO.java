/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.artifactory;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.artifactory.ArtifactoryConnection;
import com.sonatype.insight.brain.security.RotatableSecrets;
import com.sonatype.insight.dataaccess.TransactionContext;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ArtifactoryConnection.ARTIFACTORY_CONNECTION;

@Named
@Singleton
public class ArtifactoryConnectionDAO
    extends AbstractOperationalSqlDAO<ArtifactoryConnection>
    implements RotatableSecrets
{
  @Inject
  public ArtifactoryConnectionDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public ArtifactoryConnection getByOwnerId(final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(ARTIFACTORY_CONNECTION)
          .where(ARTIFACTORY_CONNECTION.OWNER_ID.eq(ownerId))
          .fetchOne());
    }
  }

  public ArtifactoryConnection getByIdAndOwnerId(final String artifactoryConnectionId, final String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(ARTIFACTORY_CONNECTION)
          .where(ARTIFACTORY_CONNECTION.ARTIFACTORY_CONNECTION_ID.eq(artifactoryConnectionId))
          .and(ARTIFACTORY_CONNECTION.OWNER_ID.eq(ownerId))
          .fetchOne());
    }
  }

  @Override
  public List<ArtifactoryConnection> getAll(final TransactionContext tx) {
    return tx.dsl()
        .selectFrom(ARTIFACTORY_CONNECTION)
        .fetch(this::toEntity);
  }

  @Override
  public Table<?> getJooqTable() {
    return ARTIFACTORY_CONNECTION;
  }

  @Override
  public Class<ArtifactoryConnection> getEntityClass() {
    return ArtifactoryConnection.class;
  }
}
