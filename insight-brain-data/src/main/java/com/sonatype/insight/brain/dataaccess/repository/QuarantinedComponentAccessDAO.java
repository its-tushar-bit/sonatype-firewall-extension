/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.repository;

import java.util.Date;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class QuarantinedComponentAccessDAO
    extends AbstractOperationalSqlDAO<QuarantinedComponentAccess>
{
  private static final int DELETE_BATCH_SIZE = 100;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public QuarantinedComponentAccessDAO(
      final OperationalDataStore operationalDataStore,
      final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO)
  {
    super(operationalDataStore);
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  public int deleteAllBeforeDate(final Date cutoffDate) {
    String sQuery = "SELECT entity.id FROM QuarantinedComponentAccess entity" +
        " WHERE entity.generateTime < ?1";
    int deletedRows = 0;
    List<String> ids =
        new Query<String>(sQuery, cutoffDate).setMaxResults(DELETE_BATCH_SIZE).getList();

    while (!ids.isEmpty()) {
      deletedRows +=
          createQuery("DELETE FROM QuarantinedComponentAccess entity WHERE entity.id IN (?1)", ids).executeUpdate();
      ids = new Query<String>(sQuery, cutoffDate).setMaxResults(DELETE_BATCH_SIZE).getList();
    }
    return deletedRows;
  }

  public void deleteByRepositoryComponentId(final TransactionContext tx, final String repositoryComponentId) {
    String sQuery = "DELETE FROM QuarantinedComponentAccess entity WHERE entity.repositoryComponentId=?1";
    createQuery(sQuery, repositoryComponentId).executeUpdate(tx);
  }

  public void deleteByRepositoryId(final TransactionContext tx, final String repositoryId) {
    String sQuery = "DELETE FROM QuarantinedComponentAccess entity WHERE entity.repositoryId=?1";
    createQuery(sQuery, repositoryId).executeUpdate(tx);
  }

  public void setAnonymousAccess(boolean enabled) {
    systemConfigurationPropertyDAO.update(new SystemConfigurationProperty(
        SystemConfigurationProperty.QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS, String.valueOf(enabled)));
  }

  public boolean isAnonymousAccessEnabled() {
    return Boolean.parseBoolean(systemConfigurationPropertyDAO
        .getByName(SystemConfigurationProperty.QUARANTINED_COMPONENT_VIEW_ANONYMOUS_ACCESS).getValue());
  }
}
