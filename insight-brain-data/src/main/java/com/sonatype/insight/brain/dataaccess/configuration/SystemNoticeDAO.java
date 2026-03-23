/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.SystemNotice;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SystemNotice.SYSTEM_NOTICE;

/**
 * @since 1.29.0
 */
@Named
@Singleton
public class SystemNoticeDAO
    extends AbstractOperationalSqlDAO<SystemNotice>
{
  private static final String SYSTEM_NOTICE_ID = "system-notice";

  @Inject
  public SystemNoticeDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public SystemNotice get() {
    return getById(SYSTEM_NOTICE_ID);
  }

  @Override
  public void update(final TransactionContext tx, final SystemNotice entity) {
    entity.setId(SYSTEM_NOTICE_ID);
    super.update(tx, entity);
  }

  @Override
  public void insert(final TransactionContext tx, final SystemNotice entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(final TransactionContext tx, final SystemNotice entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Table<?> getJooqTable() {
    return SYSTEM_NOTICE;
  }

  @Override
  public Class<SystemNotice> getEntityClass() {
    return SystemNotice.class;
  }
}
