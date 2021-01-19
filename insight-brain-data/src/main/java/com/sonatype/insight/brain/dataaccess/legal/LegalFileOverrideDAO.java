/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.105
 */
public class LegalFileOverrideDAO
    extends AbstractOperationalSqlDAO<LegalFileOverride>
{
  @Override
  public LegalFileOverride getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM LegalFileOverride entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<LegalFileOverride> getByComponentLegalFileId(TransactionContext tx, String componentLegalFileId) {
    String sQuery = "SELECT entity FROM LegalFileOverride entity" + //
        " WHERE entity.componentLegalFileId=?1";
    return getList(tx, sQuery, componentLegalFileId);
  }

  public List<LegalFileOverride> getByComponentLegalFileId(String componentLegalFileId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByComponentLegalFileId(tx, componentLegalFileId);
    }
  }
}
