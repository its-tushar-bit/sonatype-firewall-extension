/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db.dao;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.inject.Inject;
import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.TenantMetadata.TENANT_METADATA;

public class TenantMetadataDAO
    extends AbstractOperationalSqlDAO<TenantMetadata>
{
  @Override
  public Table<?> getJooqTable() {
    return TENANT_METADATA;
  }

  @Inject
  public TenantMetadataDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public TenantMetadata get() {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl().selectFrom(TENANT_METADATA).fetchOne());
    }
  }

  public void setEncryptionKeyName(String encryptionKeyName) {
    TenantMetadata tenantMetadata = get();
    tenantMetadata.setEncryptionKeyName(encryptionKeyName);
    update(tenantMetadata);
  }

  @Override
  public Class<TenantMetadata> getEntityClass() {
    return TenantMetadata.class;
  }
}
