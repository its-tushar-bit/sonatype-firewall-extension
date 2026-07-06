/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.dataaccess.development.prioritization;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.util.CollectionUtils;
import org.jooq.BatchBindStep;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.DevelopmentPrioritizationComponentInfo.DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO;

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
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO)
          .where(DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.SCAN_ID.eq(scanId))
          .fetch(this::toEntity);
    }
  }

  public DevelopmentPrioritizationComponentInfo getByScanIdAndComponentHash(
      final String scanId,
      final String componentHash)
  {
    try (TransactionContext tx = createTransactionContext()) {
      return toEntity(tx.dsl()
          .selectFrom(DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO)
          .where(DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.SCAN_ID.eq(scanId))
          .and(DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.COMPONENT_HASH.eq(componentHash))
          .fetchOne());
    }
  }

  /**
   * Batch loads component info for multiple component hashes within a single scan context.
   * Returns a map keyed by componentHash for O(1) lookup during processing.
   * <p>
   * This method is useful for avoiding N+1 queries when processing multiple components
   * that all belong to the same scan.
   *
   * @param scanId the scan ID
   * @param componentHashes set of component hashes to fetch info for
   * @return map of component hash to component info, or empty map if componentHashes is null/empty
   */
  public Map<String, DevelopmentPrioritizationComponentInfo> getByScanIdAndComponentHashes(
      final String scanId,
      final Set<String> componentHashes)
  {
    if (CollectionUtils.isEmpty(componentHashes)) {
      return Collections.emptyMap();
    }

    try (TransactionContext tx = createTransactionContext()) {
      try (var stream = getStreamWithSqlInClause(componentHashes, partition -> tx.dsl()
          .selectFrom(DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO)
          .where(DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.SCAN_ID.eq(scanId))
          .and(DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.COMPONENT_HASH.in(partition))
          .fetchStream()
          .map(this::toEntity)))
      {
        return stream.collect(Collectors.toMap(
            DevelopmentPrioritizationComponentInfo::getComponentHash,
            info -> info,
            (a, b) -> a)); // Keep first if duplicates
      }
    }
  }

  // Batch insert to avoid multiple round trips to the DB when we want to insert multiple rows at the same time
  // https://github.com/sonatype/insight-brain/pull/11563#discussion_r1628538103
  @Override
  public int insertBatch(
      TransactionContext tx,
      List<DevelopmentPrioritizationComponentInfo> developmentPrioritizationComponentInfoCollection)
  {
    if (CollectionUtils.isEmpty(developmentPrioritizationComponentInfoCollection)) {
      log.info("No rows to insert in the bath. Batch skipped.");
      return 0;
    }
    List<List<DevelopmentPrioritizationComponentInfo>> sizeSafeBatches =
        Lists.partition(developmentPrioritizationComponentInfoCollection, BATCH_INSERT_SIZE_LIMIT);

    if (sizeSafeBatches.size() > 1) {
      log.info("Persisting {} batches of DevelopmentPrioritizationComponentInfo data.",
          sizeSafeBatches.size());
    }

    int inserted = 0;
    for (List<DevelopmentPrioritizationComponentInfo> sizeSafeBatch : sizeSafeBatches) {
      inserted += executeBatch(tx, sizeSafeBatch);
    }
    return inserted;
  }

  private int executeBatch(TransactionContext tx, List<DevelopmentPrioritizationComponentInfo> batch) {
    BatchBindStep batchInsert = tx.dsl()
        .batch(
            tx.dsl()
                .insertInto(DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO,
                    DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO_ID,
                    DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.SCAN_ID,
                    DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.DEVELOPMENT_PRIORITIZATION_ID,
                    DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.COMPONENT_HASH,
                    DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.REMEDIATION_TYPE,
                    DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.REMEDIATION_VERSION,
                    DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.CREATED_AT,
                    DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.UPDATED_AT,
                    DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.SOURCE_STATUS,
                    DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.BUILD_STATUS,
                    DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.STAGE_RELEASE_STATUS,
                    DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.RELEASE_STATUS)
                .values((String) null, null, null, null, null, null, null, null, null, null, null, null));

    for (DevelopmentPrioritizationComponentInfo componentInfo : batch) {
      if (StringUtils.isBlank(componentInfo.getId())) {
        componentInfo.setId(UUID.randomUUID().toString().replace("-", ""));
      }
      batchInsert.bind(
          componentInfo.getId(),
          componentInfo.getScanId(),
          componentInfo.getDevelopmentPrioritizationId(),
          componentInfo.getComponentHash(),
          componentInfo.getRemediationType() != null ? componentInfo.getRemediationType().toString() : null,
          componentInfo.getRemediationVersion(),
          componentInfo.getCreatedAt(),
          componentInfo.getUpdatedAt(),
          componentInfo.getSourceStatus(),
          componentInfo.getBuildStatus(),
          componentInfo.getStageReleaseStatus(),
          componentInfo.getReleaseStatus());
    }
    int inserted = 0;
    for (int count : batchInsert.execute()) {
      if (count > 0) {
        inserted += count;
      }
    }
    return inserted;
  }

  @Override
  public int insert(TransactionContext tx, DevelopmentPrioritizationComponentInfo entity) {
    if (StringUtils.isBlank(entity.getId())) {
      entity.setId(UUID.randomUUID().toString().replace("-", ""));
    }
    return super.insert(tx, entity);
  }

  @Override
  public int update(TransactionContext tx, DevelopmentPrioritizationComponentInfo entity) {
    // Special handling: set updatedAt to now if not provided
    if (entity.getUpdatedAt() == null) {
      entity.setUpdatedAt(new Date());
    }
    return super.update(tx, entity);
  }

  public void deleteAllByScanId(final TransactionContext tx, final String scanId) {
    tx.dsl()
        .deleteFrom(DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO)
        .where(DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO.SCAN_ID.eq(scanId))
        .execute();
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

  @Override
  public Table<?> getJooqTable() {
    return DEVELOPMENT_PRIORITIZATION_COMPONENT_INFO;
  }

  @Override
  public Class<DevelopmentPrioritizationComponentInfo> getEntityClass() {
    return DevelopmentPrioritizationComponentInfo.class;
  }
}
