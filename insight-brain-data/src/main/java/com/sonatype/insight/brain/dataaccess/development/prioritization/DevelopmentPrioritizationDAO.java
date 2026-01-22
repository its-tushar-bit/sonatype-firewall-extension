/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess.development.prioritization;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritization;
import com.sonatype.insight.dataaccess.TransactionContext;

@Named
@Singleton
public class DevelopmentPrioritizationDAO
    extends AbstractOperationalSqlDAO<DevelopmentPrioritization>
{
  private final DevelopmentPrioritizationComponentInfoDAO developmentPrioritizationComponentInfoDAO;

  @Inject
  public DevelopmentPrioritizationDAO(
      final OperationalDataStore operationalDataStore,
      final DevelopmentPrioritizationComponentInfoDAO developmentPrioritizationComponentInfoDAO)
  {
    super(operationalDataStore);
    this.developmentPrioritizationComponentInfoDAO = developmentPrioritizationComponentInfoDAO;
  }

  public DevelopmentPrioritization getByScanId(final String scanId) {
    try (final TransactionContext tx = createTransactionContext()) {
      return getByScanId(tx, scanId);
    }
  }

  public DevelopmentPrioritization getByScanId(final TransactionContext tx, final String scanId) {
    final String sQuery =
        "SELECT entity FROM DevelopmentPrioritization entity WHERE entity.scanId=?1";
    return get(tx, sQuery, scanId);
  }

  public void deleteByScanIdCascade(final TransactionContext tx, final String scanId) {
    // This method would cascade-delete all children DevelopmentPrioritizationComponentInfo entities
    developmentPrioritizationComponentInfoDAO.deleteAllByScanId(tx, scanId);

    final String sQuery = "DELETE FROM DevelopmentPrioritization entity WHERE entity.scanId=?1";
    createQuery(sQuery, scanId).executeUpdate(tx);
  }

  @Override
  public void delete(final TransactionContext tx, final DevelopmentPrioritization entity) {
    /*
     * Do not use this method for deleting DevelopmentPrioritization entities
     * since it does not cascade to DevelopmentPrioritizationComponentInfo.
     *
     * Instead, use the deleteByScanIdCascade method to delete both
     * DevelopmentPrioritization and associated DevelopmentPrioritizationComponentInfo.
     */
    super.delete(tx, entity);
  }

  @Override
  public void delete(final DevelopmentPrioritization entity) {
    /*
     * Do not use this method for deleting DevelopmentPrioritization entities
     * since it does not cascade to DevelopmentPrioritizationComponentInfo.
     *
     * Instead, use the deleteByScanIdCascade method to delete both
     * DevelopmentPrioritization and associated DevelopmentPrioritizationComponentInfo.
     */
    super.delete(entity);
  }
}
