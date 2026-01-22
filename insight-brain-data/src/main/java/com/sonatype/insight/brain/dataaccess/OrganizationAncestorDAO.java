/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.OrganizationAncestor;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class OrganizationAncestorDAO
    extends AbstractOperationalSqlDAO<OrganizationAncestor>
{
  @Inject
  public OrganizationAncestorDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<OrganizationAncestor> getByOrganizationId(String orgId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOrganizationId(tx, orgId);
    }
  }

  public List<OrganizationAncestor> getByOrganizationId(TransactionContext tx, String orgId) {
    String sQuery = "SELECT entity FROM OrganizationAncestor entity WHERE entity.organizationId = ?1 " +
        "ORDER BY entity.ancestorDistance";

    return getList(tx, sQuery, orgId);
  }
}
