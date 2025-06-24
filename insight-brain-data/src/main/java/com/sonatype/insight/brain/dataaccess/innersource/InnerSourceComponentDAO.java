/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.innersource;

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.98
 * @deprecated use {@link InnerSourceApplicationDAO} instead
 */
@Named
@Singleton
@Deprecated
public class InnerSourceComponentDAO
    extends AbstractOperationalSqlDAO<InnerSourceComponent>
{
  @Inject
  public InnerSourceComponentDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public static final String SELECT_ENTITY_FROM_INNER_SOURCE_COMPONENT =
      "SELECT entity FROM InnerSourceComponent entity";

  public List<InnerSourceComponent> getByApplicationId(TransactionContext tx, String appId) {
    String sQuery = SELECT_ENTITY_FROM_INNER_SOURCE_COMPONENT + //
        " WHERE entity.applicationId=?1";
    return getList(tx, sQuery, appId);
  }

  public List<InnerSourceComponent> getByApplicationId(String appId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationId(tx, appId);
    }
  }
}
