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

    sizeSafeBatches.forEach(sizeSafeBatch -> executeBatch(tx, sizeSafeBatch));
  }

  private void executeBatch(TransactionContext tx, List<DevelopmentPrioritizationComponentInfo> batch) {
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
    batchInsert.execute();
  }

  @Override
  public void insert(TransactionContext tx, DevelopmentPrioritizationComponentInfo entity) {
    if (StringUtils.isBlank(entity.getId())) {
      entity.setId(UUID.randomUUID().toString().replace("-", ""));
    }
    super.insert(tx, entity);
  }

  @Override
  public void update(TransactionContext tx, DevelopmentPrioritizationComponentInfo entity) {
    // Special handling: set updatedAt to now if not provided
    if (entity.getUpdatedAt() == null) {
      entity.setUpdatedAt(new Date());
    }
    super.update(tx, entity);
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
