/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.105
 */
public class ComponentCopyrightDAO
    extends AbstractOperationalSqlDAO<ComponentCopyright>
{
  @Override
  public ComponentCopyright getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ComponentCopyright entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<ComponentCopyright> getByOwnerId(TransactionContext tx, String ownerId) {
    String sQuery = "SELECT entity FROM ComponentCopyright entity" + //
        " WHERE entity.ownerId=?1";
    return getList(tx, sQuery, ownerId);
  }

  public List<ComponentCopyright> getByOwnerId(String ownerId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOwnerId(tx, ownerId);
    }
  }

  @Override
  public void delete(TransactionContext tx, ComponentCopyright componentCopyright) {
    // Cascade to copyright overrides
    CopyrightOverrideDAO copyrightOverrideDAO = new CopyrightOverrideDAO();
    for (CopyrightOverride copyrightOverride : copyrightOverrideDAO
        .getByComponentCopyrightId(tx, componentCopyright.getId())) {
      copyrightOverrideDAO.delete(tx, copyrightOverride);
    }
    super.delete(tx, componentCopyright);
  }
}
