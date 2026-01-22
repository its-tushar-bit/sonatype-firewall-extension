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
  public SystemNoticeDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public SystemNotice get() {
    String sQuery = "SELECT entity FROM SystemNotice entity WHERE entity.id=?1";
    return get(sQuery, SYSTEM_NOTICE_ID);
  }

  @Override
  public void update(TransactionContext tx, SystemNotice systemNotice) {
    systemNotice.setId(SYSTEM_NOTICE_ID);
    super.update(tx, systemNotice);
  }

  @Override
  public void insert(TransactionContext tx, SystemNotice entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(TransactionContext tx, SystemNotice entity) {
    throw new UnsupportedOperationException();
  }
}
