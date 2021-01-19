/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.105
 */
public class ComponentObligationAttributionDAO
    extends AbstractOperationalSqlDAO<ComponentObligationAttribution>
{
  @Override
  public ComponentObligationAttribution getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ComponentObligationAttribution entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<ComponentObligationAttribution> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM ComponentObligationAttribution entity" + //
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }

  public List<ComponentObligationAttribution> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }
}
