/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tenancy;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.DeletedTenant.DELETED_TENANT;
import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAsGlobal;

@Named
@Singleton
public class DeletedTenantDAO
    extends AbstractOperationalSqlDAO<DeletedTenant>
{
  private static final Logger log = LoggerFactory.getLogger(DeletedTenantDAO.class);

  @Inject
  public DeletedTenantDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  @Override
  public TransactionContext createTransactionContext() {
    return runAsGlobal(super::createTransactionContext);
  }

  @Override
  protected UpdatableRecord<?> fromEntity(final UpdatableRecord<?> record, final DeletedTenant entity) {
    super.fromEntity(record, entity);
    record.set(DELETED_TENANT.CREATED, entity.getCreated() != null
        ? entity.getCreated()
        : new Date());
    return record;
  }

  @Override
  public int insert(TransactionContext tx, DeletedTenant entity) {
    if (GLOBAL_TENANT.tenantSlug.equals(entity.getId())) {
      throw new IllegalArgumentException("Scheduling the global tenant for deletion is not allowed");
    }

    // Note this log message is captured by an Observe monitor so should be edited with care
    log.warn("Tenant scheduled for deletion, {}", entity);

    return super.insert(tx, entity);
  }

  public DeletedTenant getTenantBySlug(String tenantSlug) {
    try (TransactionContext tx = createTransactionContext()) {
      return getById(tx, tenantSlug);
    }
  }

  public List<DeletedTenant> getAllTenantDeletionsOlderThanRetentionPeriod(long retentionPeriodInHours) {
    long retentionInMillis = retentionPeriodInHours * 60 * 60 * 1000;
    Date cutoffDate = Date.from(Instant.ofEpochMilli(System.currentTimeMillis() - retentionInMillis));

    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(DELETED_TENANT)
          .where(DELETED_TENANT.CREATED.lt(cutoffDate))
          .and(DELETED_TENANT.DELETE_COMPLETED_DATE.isNull())
          .fetch()
          .map(this::toEntity);
    }
  }

  public List<DeletedTenant> getAllTenantDeletions() {
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(DELETED_TENANT)
          .where(DELETED_TENANT.DELETE_COMPLETED_DATE.isNull())
          .fetch()
          .map(this::toEntity);
    }
  }

  public boolean isScheduledForDeletion(String tenantSlug) {
    try (TransactionContext tx = createTransactionContext()) {
      int count = tx.dsl()
          .selectCount()
          .from(DELETED_TENANT)
          .where(DELETED_TENANT.TENANT_SLUG.eq(tenantSlug))
          .and(DELETED_TENANT.DELETE_COMPLETED_DATE.isNull())
          .fetchOne(0, int.class);

      return count > 0;
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return DELETED_TENANT;
  }

  @Override
  public Class<DeletedTenant> getEntityClass() {
    return DeletedTenant.class;
  }
}
