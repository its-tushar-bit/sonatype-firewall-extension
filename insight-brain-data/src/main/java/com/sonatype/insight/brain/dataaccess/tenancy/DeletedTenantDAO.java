/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.tenancy;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.tenancy.DeletedTenant;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAsGlobal;

public class DeletedTenantDAO
    extends AbstractOperationalSqlDAO<DeletedTenant>
{
  private static final Logger log = LoggerFactory.getLogger(DeletedTenantDAO.class);

  @Override
  public TransactionContext createTransactionContext() {
    return runAsGlobal(super::createTransactionContext);
  }

  @Override
  public void insert(TransactionContext tx, DeletedTenant entity) {
    if (GLOBAL_TENANT.tenantSlug.equals(entity.getId())) {
      throw new IllegalArgumentException("Scheduling the global tenant for deletion is not allowed");
    }

    // Note this log message is captured by a DataDog Monitor so should be edited with care
    log.warn("Tenant scheduled for deletion, {}", entity);

    super.insert(tx, entity);
  }

  @Override
  public DeletedTenant getById(TransactionContext tx, String tenantSlug) {
    String sQuery = "SELECT entity FROM DeletedTenant entity" + //
        " WHERE entity.tenantSlug=?1";

    return get(tx, sQuery, tenantSlug);
  }

  public DeletedTenant getTenantBySlug(String tenantSlug) {
    try (TransactionContext tx = createTransactionContext()) {
      return getById(tx, tenantSlug);
    }
  }

  public List<DeletedTenant> getAllTenantDeletionsOlderThanRetentionPeriod(long retentionPeriodInHours) {
    String query = "SELECT tenant FROM DeletedTenant tenant WHERE tenant.deleteRequestedTimestamp < ?1";

    long retentionInMillis = retentionPeriodInHours * 60 * 60 * 1000;

    return super.getList(query, System.currentTimeMillis() - retentionInMillis);
  }

  public List<DeletedTenant> getAllTenantDeletions() {
    String query = "SELECT tenant FROM DeletedTenant tenant";
    return super.getList(query);
  }

  public boolean isScheduledForDeletion(String tenantSlug) {
    String sQuery = "SELECT COUNT(tenant) FROM DeletedTenant tenant WHERE tenant.tenantSlug = ?1";
    int count = getSingle(Number.class, sQuery, tenantSlug).intValue();

    return count > 0;
  }
}
