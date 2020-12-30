/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;

import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.104
 */
public class ApplicationComponentLicenseDAO
    extends AbstractOperationalSqlDAO<ApplicationComponentLicense>
{
  @Override
  public ApplicationComponentLicense getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM ApplicationComponentLicense entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  @Override
  public void update(TransactionContext tx, ApplicationComponentLicense entity) {
    throw new UnsupportedOperationException("ApplicationComponentLicense does not support update operations");
  }

  public List<ApplicationComponentLicense> getByApplicationComponentId(
      TransactionContext tx,
      String applicationComponentId)
  {
    String sQuery = "SELECT entity FROM ApplicationComponentLicense entity" + //
        " WHERE entity.applicationComponentId=?1";
    return getList(tx, sQuery, applicationComponentId);
  }

  public List<ApplicationComponentLicense> getByApplicationComponentId(String applicationComponentId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByApplicationComponentId(tx, applicationComponentId);
    }
  }
}
