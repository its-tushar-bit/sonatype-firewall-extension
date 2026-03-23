/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.RepositoryContainer.REPOSITORY_CONTAINER;

@Named
@Singleton
public class RepositoryContainerDAO
    extends AbstractOperationalSqlDAO<RepositoryContainer>
{
  private final OrganizationDAO organizationDAO;

  @Inject
  public RepositoryContainerDAO(
      final OperationalDataStore operationalDataStore,
      final OrganizationDAO organizationDAO)
  {
    super(operationalDataStore);
    this.organizationDAO = organizationDAO;
  }

  public RepositoryContainer getInstance() {
    try (TransactionContext tx = createTransactionContext()) {
      return getInstance(tx);
    }
  }

  public RepositoryContainer getInstance(TransactionContext tx) {
    return super.toEntity(tx.dsl()
        .selectFrom(REPOSITORY_CONTAINER)
        .fetchOne());
  }

  public String getRelatedOrganizationId() {
    try (TransactionContext tx = createTransactionContext()) {
      return getRelatedOrganizationId(tx);
    }
  }

  public String getRelatedOrganizationId(TransactionContext tx) {
    return getInstance(tx).getRelatedOrganizationId();
  }

  public void setRelatedOrganizationIdNotNull(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      setRelatedOrganizationIdNotNull(tx, organizationId);
      tx.commit();
    }
  }

  public void setRelatedOrganizationIdNotNull(TransactionContext tx, String organizationId) {
    final Organization org = organizationDAO.getById(tx, organizationId);

    if (org == null) {
      throw new NotFoundException("Organization not found");
    }

    tx.dsl()
        .update(REPOSITORY_CONTAINER)
        .set(REPOSITORY_CONTAINER.RELATED_ORGANIZATION_ID, organizationId)
        .where(REPOSITORY_CONTAINER.REPOSITORY_CONTAINER_ID.eq(RepositoryContainer.REPOSITORY_CONTAINER_ID))
        .execute();
  }

  @Override
  public void insert(TransactionContext tx, RepositoryContainer entity) {
    throw new UnsupportedOperationException("RepositoryContainerDAO does not support insert");
  }

  @Override
  public void update(TransactionContext tx, RepositoryContainer entity) {
    throw new UnsupportedOperationException("RepositoryContainerDAO does not support update");
  }

  @Override
  public void delete(TransactionContext tx, RepositoryContainer entity) {
    throw new UnsupportedOperationException("RepositoryContainerDAO does not support delete");
  }

  @Override
  public Table<?> getJooqTable() {
    return REPOSITORY_CONTAINER;
  }

  @Override
  public Class<RepositoryContainer> getEntityClass() {
    return RepositoryContainer.class;
  }
}
