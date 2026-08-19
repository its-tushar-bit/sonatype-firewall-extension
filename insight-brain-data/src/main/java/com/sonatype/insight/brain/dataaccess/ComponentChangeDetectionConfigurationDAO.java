/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.ComponentChangeDetectionConfiguration;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.ComponentChangeDetectionConfiguration.COMPONENT_CHANGE_DETECTION_CONFIGURATION;

/**
 * @since 1.188.0
 */
@Named
@Singleton
public class ComponentChangeDetectionConfigurationDAO
    extends AbstractOperationalSqlDAO<ComponentChangeDetectionConfiguration>
{
  public static final String COMPONENT_CHANGE_DETECTION_VERSION = "1.0";

  private static final int MAX_PARAMETERS = 1000;

  private static final Logger log = LoggerFactory.getLogger(ComponentChangeDetectionConfigurationDAO.class);

  @Inject
  protected ComponentChangeDetectionConfigurationDAO(final OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
  }

  public List<ComponentChangeDetectionConfiguration> addComponents(
      final long maxComponents,
      final List<ComponentChangeDetectionConfiguration> components)
  {
    List<ComponentChangeDetectionConfiguration> removedComponents = new ArrayList<>();
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      insertComponents(tx, components);
      // Check if the table size exceeds maxComponents and remove the oldest components
      long count = getTotalComponentCount(tx);
      if (count > maxComponents) {
        int removeCount = (int) (count - maxComponents);
        removedComponents = removeOldestComponents(tx, removeCount);
      }
      tx.commit();
    }
    catch (Exception e) {
      log.error("error when adding components", e);
    }

    return removedComponents;
  }

  private void insertComponents(
      final TransactionContext tx,
      final List<ComponentChangeDetectionConfiguration> components)
  {
    // Validate and filter out components with existing purl values in the database
    // Remove duplicate purl values from the input list
    List<ComponentChangeDetectionConfiguration> refinedComponents = validateAndFilterComponents(tx, components);
    for (ComponentChangeDetectionConfiguration component : refinedComponents) {
      if (StringUtils.isBlank(component.getId())) {
        component.setId(UUID.randomUUID().toString().replace("-", ""));
      }
      tx.dsl()
          .insertInto(COMPONENT_CHANGE_DETECTION_CONFIGURATION)
          .set(COMPONENT_CHANGE_DETECTION_CONFIGURATION.COMPONENT_CHANGE_DETECTION_CONFIGURATION_ID, component.getId())
          .set(COMPONENT_CHANGE_DETECTION_CONFIGURATION.VERSION, COMPONENT_CHANGE_DETECTION_VERSION)
          .set(COMPONENT_CHANGE_DETECTION_CONFIGURATION.PURL, component.getPurl())
          .set(COMPONENT_CHANGE_DETECTION_CONFIGURATION.COMPONENT_HASH, component.getComponentHash())
          .set(COMPONENT_CHANGE_DETECTION_CONFIGURATION.ADDED_TIME, component.getAddedTime())
          .execute();
    }
  }

  private List<ComponentChangeDetectionConfiguration> validateAndFilterComponents(
      final TransactionContext tx,
      final List<ComponentChangeDetectionConfiguration> components)
  {
    List<ComponentChangeDetectionConfiguration> uniqueComponents = components.stream()
        .collect(Collectors.toMap(
            ComponentChangeDetectionConfiguration::getPurl,
            component -> component,
            (existing, replacement) -> existing, LinkedHashMap::new))
        .values()
        .stream()
        .toList();

    List<String> purls = uniqueComponents.stream()
        .map(ComponentChangeDetectionConfiguration::getPurl)
        .collect(Collectors.toList());

    List<String> existingPurls = new ArrayList<>();
    for (int i = 0; i < purls.size(); i += MAX_PARAMETERS) {
      List<String> subList = purls.subList(i, Math.min(i + MAX_PARAMETERS, purls.size()));
      List<String> batchResults = tx.dsl()
          .select(COMPONENT_CHANGE_DETECTION_CONFIGURATION.PURL)
          .from(COMPONENT_CHANGE_DETECTION_CONFIGURATION)
          .where(COMPONENT_CHANGE_DETECTION_CONFIGURATION.PURL.in(subList))
          .fetchInto(String.class);
      existingPurls.addAll(batchResults);
    }

    return uniqueComponents.stream()
        .filter(component -> !existingPurls.contains(component.getPurl()))
        .collect(Collectors.toList());
  }

  private long getTotalComponentCount(final TransactionContext tx) {
    return tx.dsl()
        .selectCount()
        .from(COMPONENT_CHANGE_DETECTION_CONFIGURATION)
        .fetchOne(0, Long.class);
  }

  private List<ComponentChangeDetectionConfiguration> removeOldestComponents(
      final TransactionContext tx,
      final int removeCount)
  {
    List<ComponentChangeDetectionConfiguration> removeComponents = tx.dsl()
        .selectFrom(COMPONENT_CHANGE_DETECTION_CONFIGURATION)
        .orderBy(COMPONENT_CHANGE_DETECTION_CONFIGURATION.ADDED_TIME)
        .limit(removeCount)
        .fetch()
        .stream()
        .map(super::toEntity)
        .collect(Collectors.toList());

    if (removeComponents.isEmpty()) {
      return removeComponents;
    }

    List<String> removeComponentPurls = removeComponents.stream()
        .map(ComponentChangeDetectionConfiguration::getPurl)
        .collect(Collectors.toList());

    for (int i = 0; i < removeComponentPurls.size(); i += MAX_PARAMETERS) {
      List<String> subList =
          removeComponentPurls.subList(i, Math.min(i + MAX_PARAMETERS, removeComponentPurls.size()));
      tx.dsl()
          .deleteFrom(COMPONENT_CHANGE_DETECTION_CONFIGURATION)
          .where(COMPONENT_CHANGE_DETECTION_CONFIGURATION.PURL.in(subList))
          .execute();
    }

    return removeComponents;
  }

  public void updateComparisonHashOfPurl(final String purl, final String comparisonHash) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .update(COMPONENT_CHANGE_DETECTION_CONFIGURATION)
          .set(COMPONENT_CHANGE_DETECTION_CONFIGURATION.COMPARISON_HASH, comparisonHash)
          .where(COMPONENT_CHANGE_DETECTION_CONFIGURATION.PURL.eq(purl))
          .execute();
      tx.commit();
    }
  }

  public void updateComparisonHashAndVersionOfPurl(
      final String purl,
      final String comparisonHash,
      final String version)
  {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      tx.dsl()
          .update(COMPONENT_CHANGE_DETECTION_CONFIGURATION)
          .set(COMPONENT_CHANGE_DETECTION_CONFIGURATION.COMPARISON_HASH, comparisonHash)
          .set(COMPONENT_CHANGE_DETECTION_CONFIGURATION.VERSION, version)
          .where(COMPONENT_CHANGE_DETECTION_CONFIGURATION.PURL.eq(purl))
          .execute();
      tx.commit();
    }
  }

  public List<ComponentChangeDetectionConfiguration> getComponents(final int page, final int pageSize) {
    if (page <= 0 || pageSize <= 0) {
      throw new IllegalArgumentException("Page and pageSize must be greater than 0");
    }
    int offset = (page - 1) * pageSize;
    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(COMPONENT_CHANGE_DETECTION_CONFIGURATION)
          .offset(offset)
          .limit(pageSize)
          .fetch()
          .stream()
          .map(super::toEntity)
          .collect(Collectors.toList());
    }
  }

  public List<ComponentChangeDetectionConfiguration> getComponentsInBatches(
      final int batchSize,
      final int continuationToken)
  {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("Batch size must be greater than 0");
    }

    if (continuationToken < 0) {
      throw new IllegalArgumentException("Invalid continuation token");
    }

    try (TransactionContext tx = createTransactionContext()) {
      return tx.dsl()
          .selectFrom(COMPONENT_CHANGE_DETECTION_CONFIGURATION)
          .offset(continuationToken)
          .limit(batchSize)
          .fetch()
          .stream()
          .map(this::toEntity)
          .collect(Collectors.toList());
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return COMPONENT_CHANGE_DETECTION_CONFIGURATION;
  }

  @Override
  public Class<ComponentChangeDetectionConfiguration> getEntityClass() {
    return ComponentChangeDetectionConfiguration.class;
  }
}
