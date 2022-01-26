/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.legal;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.legal.SourceLinkOverride;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.133
 */
public class SourceLinkOverrideDAO
    extends AbstractOperationalSqlDAO<SourceLinkOverride>
{
  @Override
  public SourceLinkOverride getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM SourceLinkOverride entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  public List<SourceLinkOverride> getByComponentSourceLinkId(TransactionContext tx, String componentSourceLinkId) {
    String sQuery = "SELECT entity FROM SourceLinkOverride entity" + //
        " WHERE entity.componentSourceLinkId=?1";
    return getList(tx, sQuery, componentSourceLinkId);
  }

  @Override
  public void update(TransactionContext tx, SourceLinkOverride sourceLinkOverride) {
    if (getById(tx, sourceLinkOverride.getId()) == null) {
      throw new BadRequestException(
          "Cannot update source link override with id " + sourceLinkOverride.getId() + " because it does not exist.");
    }
    super.update(tx, sourceLinkOverride);
  }
}
