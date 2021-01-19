/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.105
 */
public class CopyrightOverrideDAO
    extends AbstractOperationalSqlDAO<CopyrightOverride>
{
  @Override
  public CopyrightOverride getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM CopyrightOverride entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<CopyrightOverride> getByComponentCopyrightId(TransactionContext tx, String componentCopyrightId) {
    String sQuery = "SELECT entity FROM CopyrightOverride entity" + //
        " WHERE entity.componentCopyrightId=?1";
    return getList(tx, sQuery, componentCopyrightId);
  }

  public List<CopyrightOverride> getByComponentCopyrightId(String componentCopyrightId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByComponentCopyrightId(tx, componentCopyrightId);
    }
  }
}
