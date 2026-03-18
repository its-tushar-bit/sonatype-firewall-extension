/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess.development.prioritization;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.SourceStageType;
import com.sonatype.insight.brain.model.policy.stages.StageReleaseStageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.prioritization.DevelopmentPrioritizationComponentInfo;
import com.sonatype.insight.dataaccess.TransactionContext;

import com.google.common.collect.Lists;
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

  // The theoretical limit for postgres would be 65535/8 = 8191. We set our soft limit up to half of that hard limit.
  static final int BATCH_INSERT_SIZE_LIMIT = 4000;

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
      final String scanId,
      final String componentHash)
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
      List<DevelopmentPrioritizationComponentInfo> developmentPrioritizationComponentInfoCollection)
  {
    if (CollectionUtils.isEmpty(developmentPrioritizationComponentInfoCollection)) {
      log.info("No rows to insert in the bath. Batch skipped.");
      return;
    }
    List<List<DevelopmentPrioritizationComponentInfo>> sizeSafeBatches =
        Lists.partition(developmentPrioritizationComponentInfoCollection, BATCH_INSERT_SIZE_LIMIT);

    if (sizeSafeBatches.size() > 1) {
      log.info("Persisting {} batches of DevelopmentPrioritizationComponentInfo data.",
          sizeSafeBatches.size());
    }

    sizeSafeBatches.forEach(sizeSafeBatch -> {
      jakarta.persistence.Query query = buildBatchQuery(tx, sizeSafeBatch);
      query.executeUpdate();
    });
  }

  public void deleteAllByScanId(final TransactionContext tx, final String scanId) {
    final String sQuery = "DELETE FROM DevelopmentPrioritizationComponentInfo entity WHERE entity.scanId=?1";
    createQuery(sQuery, scanId).executeUpdate(tx);
  }

  private jakarta.persistence.Query buildBatchQuery(
      final TransactionContext tx,
      final Collection<DevelopmentPrioritizationComponentInfo> developmentPrioritizationComponentInfoCollection)
  {
    String qs = "INSERT INTO " + getDatabaseSchema() + ".development_prioritization_component_info" +
        " (development_prioritization_component_info_id, scan_id, development_prioritization_id, component_hash," +
        " remediation_type, remediation_version, created_at, updated_at, source_status, build_status," +
        " stage_release_status, release_status)" + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" + StringUtils.repeat(
            ", (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", developmentPrioritizationComponentInfoCollection.size() - 1);

    jakarta.persistence.Query query = tx.createNativeQuery(qs);
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
          .setParameter(++pos, componentInfo.getUpdatedAt())
          .setParameter(++pos, componentInfo.getSourceStatus())
          .setParameter(++pos, componentInfo.getBuildStatus())
          .setParameter(++pos, componentInfo.getStageReleaseStatus())
          .setParameter(++pos, componentInfo.getReleaseStatus());
    }
    return query;
  }

  public Map<StageType, String> getStageStatusesByScanIdAndComponentHash(
      final String scanId,
      final String componentHash)
  {
    final DevelopmentPrioritizationComponentInfo componentInfo = getByScanIdAndComponentHash(scanId, componentHash);
    if (Objects.isNull(componentInfo)) {
      return Collections.emptyMap();
    }

    final Map<StageType, String> stageStatuses = new HashMap<>();
    getSupportedStageTypes().forEach(
        stageType -> stageStatuses.put(stageType, getStageStatusByStageType(stageType.getId(), componentInfo)));
    return stageStatuses;
  }

  private List<StageType> getSupportedStageTypes() {
    final Collection<StageType> allStageTypes = StageTypes.getAll();
    final List<StageType> supportedStageTypes =
        Arrays.asList(StageTypes.SOURCE, StageTypes.BUILD, StageTypes.STAGE_RELEASE, StageTypes.RELEASE);
    return allStageTypes.stream()
        .filter(supportedStageTypes::contains)
        .collect(Collectors.toList());
  }

  private String getStageStatusByStageType(
      final String stageTypeId,
      final DevelopmentPrioritizationComponentInfo componentInfo)
  {
    switch (stageTypeId) {
      case SourceStageType.ID:
        return componentInfo.getSourceStatus();
      case BuildStageType.ID:
        return componentInfo.getBuildStatus();
      case StageReleaseStageType.ID:
        return componentInfo.getStageReleaseStatus();
      case ReleaseStageType.ID:
        return componentInfo.getReleaseStatus();
      default:
        throw new IllegalStateException("Unsupported stage: " + stageTypeId);
    }
  }
}
