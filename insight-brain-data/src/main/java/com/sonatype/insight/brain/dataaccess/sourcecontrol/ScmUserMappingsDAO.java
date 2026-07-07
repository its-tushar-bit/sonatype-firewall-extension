/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.sourcecontrol.ScmUserMappings;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.OwnerAncestor.OWNER_ANCESTOR;
import static com.sonatype.insight.brain.jooq.generated.ods.tables.ScmUserMappings.SCM_USER_MAPPINGS;

@Named
@Singleton
public class ScmUserMappingsDAO
    extends AbstractOperationalSqlDAO<ScmUserMappings>
{
  @Inject
  public ScmUserMappingsDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public ScmUserMappings getByOwnerIdWithHierarchy(TransactionContext tx, String ownerId) {
    return tx.dsl()
        .select(SCM_USER_MAPPINGS.fields())
        .from(SCM_USER_MAPPINGS)
        .join(OWNER_ANCESTOR)
        .on(SCM_USER_MAPPINGS.ORGANIZATION_ID.eq(OWNER_ANCESTOR.ANCESTOR_ID))
        .where(OWNER_ANCESTOR.OWNER_ID.eq(ownerId))
        .and(OWNER_ANCESTOR.ANCESTOR_TYPE.eq(OwnerType.ORGANIZATION.name()))
        .orderBy(OWNER_ANCESTOR.ANCESTOR_DISTANCE)
        .limit(1)
        .fetchOneInto(ScmUserMappings.class);
  }

  public ScmUserMappings getByOwnerIdWithHierarchy(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerIdWithHierarchy(tx, ownerId);
    }
  }

  public ScmUserMappings getByOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOrganizationId(tx, organizationId);
    }
  }

  public ScmUserMappings getByOrganizationId(TransactionContext tx, String organizationId) {
    return toEntity(tx.dsl()
        .selectFrom(SCM_USER_MAPPINGS)
        .where(SCM_USER_MAPPINGS.ORGANIZATION_ID.eq(organizationId))
        .fetchOne());
  }

  public void deleteByOrganizationId(TransactionContext tx, String organizationId) {
    tx.dsl()
        .deleteFrom(SCM_USER_MAPPINGS)
        .where(SCM_USER_MAPPINGS.ORGANIZATION_ID.eq(organizationId))
        .execute();
  }

  public void deleteByOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByOrganizationId(tx, organizationId);
      tx.commit();
    }
  }

  public void addOrUpdate(ScmUserMappings scmUserMappings) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      ScmUserMappings existingUserMappings = getByOrganizationId(tx, scmUserMappings.getOrganizationId());
      if (existingUserMappings == null) {
        insert(tx, scmUserMappings);
      }
      else {
        scmUserMappings.setId(existingUserMappings.getId());
        update(tx, scmUserMappings);
      }
      tx.commit();
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return SCM_USER_MAPPINGS;
  }

  @Override
  public Class<ScmUserMappings> getEntityClass() {
    return ScmUserMappings.class;
  }
}
