/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlWithFallbackDAO;
import com.sonatype.insight.brain.common.cache.ResettableExpiringMemoizingSupplier;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.jooq.Table;

import static com.sonatype.insight.brain.jooq.generated.ods.tables.SystemConfigurationProperty.SYSTEM_CONFIGURATION_PROPERTY;
import static com.sonatype.insight.brain.tenancy.TenantThreadLocal.runAsGlobal;

/**
 * @since 1.33
 */
@Named
@Singleton
public class SystemConfigurationPropertyDAO
    extends AbstractOperationalSqlWithFallbackDAO<SystemConfigurationProperty>
{
  // In a multi-node cluster, property changes may take up to this duration to propagate
  // if the cross-node Quartz invalidation job is delayed. This is acceptable since system
  // configuration properties are rarely updated (admin operations only).
  private static final Duration CACHE_TTL = Duration.ofSeconds(30);

  private static volatile TenantReference<ResettableExpiringMemoizingSupplier<Map<String, SystemConfigurationProperty>>> cacheRef =
      new TenantReference<>();

  /**
   * Creates a new DAO and invalidates the entire static cache.
   * <p>
   * The cache invalidation ensures that suppliers (which capture this instance's DataStore)
   * are recreated. This is critical in tests where multiple DAO instances are created with
   * different DataStores — stale suppliers would reference a closed DataStore and fail.
   * In production, this constructor fires only once at startup (@Singleton).
   * </p>
   */
  @Inject
  public SystemConfigurationPropertyDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
    invalidateEntireCache();
  }

  private Map<String, SystemConfigurationProperty> getCachedProperties() {
    var supplier = cacheRef.computeIfAbsent(
        _ -> new ResettableExpiringMemoizingSupplier<>(this::loadAllFromDb, CACHE_TTL));
    return supplier.get();
  }

  /**
   * Invalidates the cache, causing the next read to fetch from the database.
   * <p>
   * In single-tenant or global-tenant context, replaces the entire cache reference
   * (invalidating all tenants) because tenant caches include merged global properties.
   * In a per-tenant context, only the current tenant's cache is reset.
   * </p>
   */
  public void invalidateCache() {
    var tenantUtil = new TenantUtil();
    if (tenantUtil.isSingleTenant() || tenantUtil.isGlobalTenant()) {
      // Global change affects all tenants' merged views — replace the entire cache
      cacheRef = new TenantReference<>();
    }
    else {
      // null means no supplier yet for this tenant — next getCachedProperties() will create one
      var supplier = cacheRef.get();
      if (supplier != null) {
        supplier.reset();
      }
    }
  }

  /**
   * Discards the entire static cache unconditionally. Use in test teardown to prevent
   * stale suppliers (which capture a specific DAO instance's DataStore) from leaking
   * between test classes.
   */
  public static void invalidateEntireCache() {
    cacheRef = new TenantReference<>();
  }

  public SystemConfigurationProperty getByNameNotNull(String name) {
    SystemConfigurationProperty property = getCachedProperties().get(name);
    if (property == null) {
      throw new NotFoundException("A system configuration property '" + name + "' does not exist.");
    }
    return property;
  }

  public SystemConfigurationProperty getByName(String name) {
    return getCachedProperties().get(name);
  }

  public SystemConfigurationProperty getByName(TransactionContext tx, String name) {
    return getByNameInternal(tx, name, false);
  }

  /**
   * Fetches all system configuration properties as a map keyed by property name.
   * Returns cached values; the cache is populated by a single query and refreshed on TTL expiry.
   */
  public Map<String, SystemConfigurationProperty> getAllAsMap() {
    return getCachedProperties();
  }

  /**
   * Fetches all system configuration properties as a map keyed by property name.
   * Returns cached values; the cache is populated by a single query and refreshed on TTL expiry.
   */
  public Map<String, SystemConfigurationProperty> getAllAsMap(TransactionContext tx) {
    return loadAllFromDbWithTx(tx);
  }

  /**
   * Loads all system configuration properties from the database.
   * <p>
   * In MTIQ, this method first fetches from the current tenant's schema, then falls back
   * to the global tenant for any properties that are absent from the per-tenant schema.
   * This matches the fallback behavior of individual getByName() calls.
   * </p>
   */
  private Map<String, SystemConfigurationProperty> loadAllFromDbWithTx(TransactionContext tx) {
    var result = tx.dsl()
        .selectFrom(SYSTEM_CONFIGURATION_PROPERTY)
        .fetch()
        .stream()
        .collect(Collectors.toMap(
            r -> r.get(SYSTEM_CONFIGURATION_PROPERTY.NAME),
            r -> r.into(SystemConfigurationProperty.class),
            (existing, replacement) -> existing));

    // In MTIQ (not single-tenant), fall back to global tenant for missing properties
    var localTenantUtil = new TenantUtil();
    if (!localTenantUtil.isSingleTenant() && !localTenantUtil.isGlobalTenant()) {
      runAsGlobal(() -> {
        try (var globalTx = createTransactionContext()) {
          globalTx.dsl()
              .selectFrom(SYSTEM_CONFIGURATION_PROPERTY)
              .fetch()
              .forEach(r -> result.putIfAbsent(
                  r.get(SYSTEM_CONFIGURATION_PROPERTY.NAME),
                  r.into(SystemConfigurationProperty.class)));
        }
        return null;
      });
    }

    return Collections.unmodifiableMap(result);
  }

  private Map<String, SystemConfigurationProperty> loadAllFromDb() {
    try (var tx = createTransactionContext()) {
      // Fetch all properties from the current tenant's schema
      var result = tx.dsl()
          .selectFrom(SYSTEM_CONFIGURATION_PROPERTY)
          .fetch()
          .stream()
          .collect(Collectors.toMap(
              r -> r.get(SYSTEM_CONFIGURATION_PROPERTY.NAME),
              r -> r.into(SystemConfigurationProperty.class),
              (existing, replacement) -> existing)); // Keep first on collision

      // In MTIQ (not single-tenant), fall back to global tenant for missing properties
      var localTenantUtil = new TenantUtil();
      if (localTenantUtil.isSingleTenant() || localTenantUtil.isGlobalTenant()) {
        return Collections.unmodifiableMap(result);
      }

      // Merge properties from global tenant schema that are missing from the tenant's result
      runAsGlobal(() -> {
        try (var globalTx = createTransactionContext()) {
          globalTx.dsl()
              .selectFrom(SYSTEM_CONFIGURATION_PROPERTY)
              .fetch()
              .forEach(r -> result.putIfAbsent(
                  r.get(SYSTEM_CONFIGURATION_PROPERTY.NAME),
                  r.into(SystemConfigurationProperty.class)));
        }
        return null;
      });

      return Collections.unmodifiableMap(result);
    }
  }

  private SystemConfigurationProperty getByNameInternal(
      final TransactionContext tx,
      final String name,
      final boolean fetchForUpdate)
  {
    return getWithGlobalFallback(tx, t -> {
      var query = t.dsl()
          .selectFrom(SYSTEM_CONFIGURATION_PROPERTY)
          .where(SYSTEM_CONFIGURATION_PROPERTY.NAME.eq(name));
      return fetchForUpdate
          ? query.forUpdate().fetchOneInto(SystemConfigurationProperty.class)
          : query.fetchOneInto(SystemConfigurationProperty.class);
    }, fetchForUpdate);
  }

  public SystemConfigurationProperty getByNameNotNull(TransactionContext tx, String name) {
    return getByNameNotNull(tx, name, false);
  }

  private SystemConfigurationProperty getByNameNotNull(TransactionContext tx, String name, boolean fetchForUpdate) {
    SystemConfigurationProperty property = getByNameInternal(tx, name, fetchForUpdate);
    if (property == null) {
      throw new NotFoundException("A system configuration property '" + name + "' does not exist.");
    }
    return property;
  }

  @Override
  public void update(TransactionContext tx, SystemConfigurationProperty property) {
    SystemConfigurationProperty existingProperty = getByNameNotNull(tx, property.getName(), true);
    property.setId(existingProperty.getId());
    tx.dsl()
        .update(SYSTEM_CONFIGURATION_PROPERTY)
        .set(SYSTEM_CONFIGURATION_PROPERTY.NAME, property.getName())
        .set(SYSTEM_CONFIGURATION_PROPERTY.VALUE, property.getValue())
        .where(SYSTEM_CONFIGURATION_PROPERTY.SYSTEM_CONFIGURATION_PROPERTY_ID.eq(property.getId()))
        .execute();
    tx.afterCommit(this::invalidateCache);
  }

  @Override
  public int insert(TransactionContext tx, SystemConfigurationProperty entity) {
    if (entity.getId() == null) {
      entity.setId(UUID.randomUUID().toString());
    }
    int inserted = tx.dsl()
        .insertInto(SYSTEM_CONFIGURATION_PROPERTY)
        .set(SYSTEM_CONFIGURATION_PROPERTY.SYSTEM_CONFIGURATION_PROPERTY_ID, entity.getId())
        .set(SYSTEM_CONFIGURATION_PROPERTY.NAME, entity.getName())
        .set(SYSTEM_CONFIGURATION_PROPERTY.VALUE, entity.getValue())
        .execute();
    tx.afterCommit(this::invalidateCache);
    return inserted;
  }

  @Override
  public void delete(TransactionContext tx, SystemConfigurationProperty entity) {
    super.delete(tx, entity);
    tx.afterCommit(this::invalidateCache);
  }

  public String get(String name) {
    SystemConfigurationProperty property = getByName(name);
    return property != null ? property.getValue() : null;
  }

  public String get(TransactionContext tx, String name) {
    SystemConfigurationProperty property = getByName(tx, name);
    return property != null ? property.getValue() : null;
  }

  public void set(String name, String value) {
    try (TransactionContext tx = createTransactionContext()) {
      tx.begin();
      set(tx, name, value);
      tx.commit();
    }
  }

  public void set(TransactionContext tx, String name, String value) {
    SystemConfigurationProperty property = getByNameInternal(tx, name, true);
    if (value == null) {
      if (property != null) {
        delete(tx, property);
      }
    }
    else {
      if (property == null) {
        property = new SystemConfigurationProperty(name, value);
        insert(tx, property);
      }
      else {
        property.setValue(value);
        update(tx, property);
      }
    }
  }

  @Override
  public Table<?> getJooqTable() {
    return SYSTEM_CONFIGURATION_PROPERTY;
  }

  @Override
  public Class<SystemConfigurationProperty> getEntityClass() {
    return SystemConfigurationProperty.class;
  }
}
