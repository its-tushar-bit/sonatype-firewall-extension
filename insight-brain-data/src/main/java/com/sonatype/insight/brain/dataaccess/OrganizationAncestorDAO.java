/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.OrganizationAncestor;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.OrganizationAncestor.ORGANIZATION_ANCESTOR;

@Named
@Singleton
public class OrganizationAncestorDAO
    extends AbstractOperationalSqlDAO<OrganizationAncestor>
{
  @Inject
  public OrganizationAncestorDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<OrganizationAncestor> getByOrganizationId(String orgId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOrganizationId(tx, orgId);
    }
  }

  public List<OrganizationAncestor> getByOrganizationId(TransactionContext tx, String orgId) {
    return tx.dsl()
        .selectFrom(ORGANIZATION_ANCESTOR)
        .where(ORGANIZATION_ANCESTOR.ORGANIZATION_ID.eq(orgId))
        .orderBy(ORGANIZATION_ANCESTOR.ANCESTOR_DISTANCE)
        .fetch()
        .stream()
        .map(this::toEntity)
        .collect(Collectors.toList());
  }

  public List<OrganizationAncestor> getByAncestorId(TransactionContext tx, String ancestorId) {
    return tx.dsl()
        .selectFrom(ORGANIZATION_ANCESTOR)
        .where(ORGANIZATION_ANCESTOR.ANCESTOR_ID.eq(ancestorId))
        .fetch()
        .stream()
        .map(this::toEntity)
        .collect(Collectors.toList());
  }

  public void deleteByAncestorId(TransactionContext tx, String ancestorId) {
    tx.dsl()
        .deleteFrom(ORGANIZATION_ANCESTOR)
        .where(ORGANIZATION_ANCESTOR.ANCESTOR_ID.eq(ancestorId))
        .execute();
  }

  @Override
  public Table<?> getJooqTable() {
    return ORGANIZATION_ANCESTOR;
  }

  @Override
  public Class<OrganizationAncestor> getEntityClass() {
    return OrganizationAncestor.class;
  }
}
