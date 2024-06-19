/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess.development.prioritization;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class DevelopmentPrioritizationComponentInfoDAO
    extends AbstractOperationalSqlDAO<DevelopmentPrioritizationComponentInfo>
{
  private static final Logger log = LoggerFactory.getLogger(DevelopmentPrioritizationComponentInfoDAO.class);

  private static final int PG_QUERY_PARAMS_MAX_SIZE = 65535;

  @Inject
  public DevelopmentPrioritizationComponentInfoDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<DevelopmentPrioritizationComponentInfo> getAllByScanId(final String scanId) {
    final String sQuery =
        "SELECT entity FROM DevelopmentPrioritizationComponentInfo entity WHERE entity.scanId=?1";
    return getList(sQuery, scanId);
  }

  public DevelopmentPrioritizationComponentInfo getByScanIdAndComponentHash(
      final String scanId, final String componentHash)
  {
    final String sQuery =
        "SELECT entity FROM DevelopmentPrioritizationComponentInfo " +
            "entity WHERE entity.scanId=?1 AND entity.componentHash=?2";
    return get(sQuery, scanId, componentHash);
  }

  // Batch insert to avoid multiple round trips to the DB when we want to insert multiple rows at the same time
  // https://github.com/sonatype/insight-brain/pull/11563#discussion_r1628538103
  public void insertBatch(
      TransactionContext tx,
      Collection<DevelopmentPrioritizationComponentInfo> developmentPrioritizationComponentInfoCollection)
  {
    if (CollectionUtils.isEmpty(developmentPrioritizationComponentInfoCollection)) {
      log.info("No rows to insert in the bath. Batch skipped.");
      return;
    }
    int paramsPerEntity = 8;
    if (developmentPrioritizationComponentInfoCollection.size() > PG_QUERY_PARAMS_MAX_SIZE / paramsPerEntity) {
      log.error("Too many ({}) rows to insert in the batch. Skipped to avoid crashing",
          developmentPrioritizationComponentInfoCollection.size());
      return;
    }
    javax.persistence.Query query = buildBatchQuery(tx, developmentPrioritizationComponentInfoCollection);
    query.executeUpdate();
  }

  public void deleteAllByScanId(final TransactionContext tx, final String scanId) {
    final String sQuery = "DELETE FROM DevelopmentPrioritizationComponentInfo entity WHERE entity.scanId=?1";
    createQuery(sQuery, scanId).executeUpdate(tx);
  }

  private javax.persistence.Query buildBatchQuery(
      final TransactionContext tx,
      final Collection<DevelopmentPrioritizationComponentInfo> developmentPrioritizationComponentInfoCollection)
  {
    String qs = "INSERT INTO " + getDatabaseSchema() + ".development_prioritization_component_info" +
        " (development_prioritization_component_info_id, scan_id, development_prioritization_id, component_hash," +
        " remediation_type, remediation_version, created_at, updated_at)" +
        " VALUES (?, ?, ?, ?, ?, ?, ?, ?)" + StringUtils.repeat(
            ", (?, ?, ?, ?, ?, ?, ?, ?)", developmentPrioritizationComponentInfoCollection.size() - 1);

    javax.persistence.Query query = tx.createNativeQuery(qs);
    int pos = 0;
    for (DevelopmentPrioritizationComponentInfo componentInfo : developmentPrioritizationComponentInfoCollection) {
      if (StringUtils.isBlank(componentInfo.getId())) {
        componentInfo.setId(UUID.randomUUID().toString().replace("-", ""));
      }
      query.setParameter(++pos, componentInfo.getId())
          .setParameter(++pos, componentInfo.getScanId())
          .setParameter(++pos, componentInfo.getDevelopmentPrioritizationId())
          .setParameter(++pos, componentInfo.getComponentHash())
          .setParameter(++pos, componentInfo.getRemediationType().toString())
          .setParameter(++pos, componentInfo.getRemediationVersion())
          .setParameter(++pos, componentInfo.getCreatedAt())
          .setParameter(++pos, componentInfo.getUpdatedAt());
    }
    return query;
  }
}
