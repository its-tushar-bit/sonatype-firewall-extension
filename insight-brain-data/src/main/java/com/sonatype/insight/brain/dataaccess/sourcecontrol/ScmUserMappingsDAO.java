/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.sourcecontrol.ScmUserMappings;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class ScmUserMappingsDAO
    extends AbstractOperationalSqlDAO<ScmUserMappings>
{
  @Inject
  public ScmUserMappingsDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public ScmUserMappings getByOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      return getByOrganizationId(tx, organizationId);
    }
  }

  public ScmUserMappings getByOrganizationId(TransactionContext tx, String organizationId) {
    return get(tx, "SELECT entity FROM ScmUserMappings entity WHERE entity.organizationId=?1", organizationId);
  }

  public void deleteByOrganizationId(TransactionContext tx, String organizationId) {
    String sQuery = "DELETE FROM ScmUserMappings entity WHERE entity.organizationId=?1";
    createQuery(sQuery, organizationId).executeUpdate(tx);
  }

  public void deleteByOrganizationId(String organizationId) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      deleteByOrganizationId(tx, organizationId);
      tx.commit();
    }
  }

  public void addOrUpdate(ScmUserMappings scmUserMappings) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      ScmUserMappings existingUserMappings = getByOrganizationId(tx, scmUserMappings.getOrganizationId());
      if (existingUserMappings == null) {
        insert(tx, scmUserMappings);
      }
      else {
        scmUserMappings.setId(existingUserMappings.getId());
        update(tx, scmUserMappings);
      }
      tx.commit();
    }
  }
}
