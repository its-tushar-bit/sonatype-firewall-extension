/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.ComponentChangeDetectionConfiguration;
import com.sonatype.insight.dataaccess.TransactionContext;

import jakarta.persistence.NoResultException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private void insertComponents(TransactionContext tx, List<ComponentChangeDetectionConfiguration> components) {
    // Validate and filter out components with existing purl values in the database
    // Remove duplicate purl values from the input list
    List<ComponentChangeDetectionConfiguration> refinedComponents = validateAndFilterComponents(tx, components);
    for (ComponentChangeDetectionConfiguration component : refinedComponents) {
      if (StringUtils.isBlank(component.getId())) {
        component.setId(UUID.randomUUID().toString().replace("-", ""));
      }
      tx.createNativeQuery(
          "INSERT INTO " + getDatabaseSchema() + ".component_change_detection_configuration" +
              " (component_change_detection_configuration_id, version, purl, component_hash, added_time)" +
              " VALUES (?1, ?2, ?3, ?4, CURRENT_TIMESTAMP)")
          .setParameter(1, component.getId())
          .setParameter(2, COMPONENT_CHANGE_DETECTION_VERSION)
          .setParameter(3, component.getPurl())
          .setParameter(4, component.getComponentHash())
          .executeUpdate();
    }
  }

  private List<ComponentChangeDetectionConfiguration> validateAndFilterComponents(
      TransactionContext tx,
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
      jakarta.persistence.Query query = tx.createNativeQuery(
          "SELECT purl FROM " + getDatabaseSchema() + ".component_change_detection_configuration WHERE purl IN " +
              buildPositionalParameters(subList, 1));
      addPositionalParameters(query, subList, 1);
      existingPurls.addAll(query.getResultList());
    }

    return uniqueComponents.stream()
        .filter(component -> !existingPurls.contains(component.getPurl()))
        .collect(Collectors.toList());
  }

  private long getTotalComponentCount(TransactionContext tx) {
    String sQuery = "SELECT COUNT(*) FROM " + getDatabaseSchema() + ".component_change_detection_configuration";
    return (long) tx.createNativeQuery(sQuery).getSingleResult();
  }

  @SuppressWarnings("unchecked")
  private List<ComponentChangeDetectionConfiguration> removeOldestComponents(TransactionContext tx, int removeCount) {
    List<ComponentChangeDetectionConfiguration> removeComponents = new ArrayList<>();
    try {
      jakarta.persistence.Query selectQuery =
          tx.createNativeQuery("SELECT * FROM " + getDatabaseSchema() +
              ".component_change_detection_configuration ORDER BY added_time LIMIT ?1 FOR UPDATE")
              .setParameter(1, removeCount);

      removeComponents = ((Stream<Object[]>) selectQuery.getResultStream()).map(
          array -> new ComponentChangeDetectionConfiguration((String) array[1], (String) array[2], (String) array[3],
              (String) array[4], new Date(((Timestamp) array[5]).getTime())))
          .collect(Collectors.toList());

      List<String> removeComponentPurls = removeComponents.stream()
          .map(ComponentChangeDetectionConfiguration::getPurl)
          .collect(Collectors.toList());

      for (int i = 0; i < removeComponentPurls.size(); i += MAX_PARAMETERS) {
        List<String> subList =
            removeComponentPurls.subList(i, Math.min(i + MAX_PARAMETERS, removeComponentPurls.size()));
        jakarta.persistence.Query deleteQuery = tx.createNativeQuery(
            "DELETE FROM " + getDatabaseSchema() + ".component_change_detection_configuration" +
                " WHERE purl IN " + buildPositionalParameters(subList, 1));
        addPositionalParameters(deleteQuery, subList, 1);
        deleteQuery.executeUpdate();
      }
    }
    catch (NoResultException ignored) {
      // ignore
    }
    return removeComponents;
  }

  public void updateComparisonHashOfPurl(String purl, String comparisonHash) {
    String sQuery = "UPDATE ComponentChangeDetectionConfiguration entity" + //
        " SET entity.comparisonHash=?1" + //
        " WHERE entity.purl=?2";
    createQuery(sQuery, comparisonHash, purl).executeUpdate();
  }

  public void updateComparisonHashAndVersionOfPurl(
      final String purl,
      final String comparisonHash,
      final String version)
  {
    String sQuery = "UPDATE ComponentChangeDetectionConfiguration entity" + //
        " SET entity.comparisonHash=?1, entity.version=?2 WHERE entity.purl=?3";
    createQuery(sQuery, comparisonHash, version, purl).executeUpdate();
  }

  @SuppressWarnings("unchecked")
  public List<ComponentChangeDetectionConfiguration> getComponents(final int page, final int pageSize) {
    if (page <= 0 || pageSize <= 0) {
      throw new IllegalArgumentException("Page and pageSize must be greater than 0");
    }
    int offset = (page - 1) * pageSize;
    try (TransactionContext tx = createTransactionContext()) {
      String sQuery = "SELECT entity FROM ComponentChangeDetectionConfiguration entity";
      return tx.createQuery(sQuery).setFirstResult(offset).setMaxResults(pageSize).getResultList();
    }
  }

  @SuppressWarnings("unchecked")
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
      String sQuery = "SELECT entity FROM ComponentChangeDetectionConfiguration entity";
      return tx.createQuery(sQuery).setFirstResult(continuationToken).setMaxResults(batchSize).getResultList();
    }
  }
}
