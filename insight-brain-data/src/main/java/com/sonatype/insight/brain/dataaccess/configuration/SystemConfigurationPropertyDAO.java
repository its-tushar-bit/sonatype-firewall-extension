/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
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

import com.google.common.cache.CacheBuilder;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;

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

  // Keyed by the DAO's OperationalDataStore so every DAO sharing a data store shares one cache (invalidation on
  // write stays coherent across instances), while a different data store gets a separate entry. This isolates
  // servers that reuse the JVM (test forks): a stopped server's lingering thread can only poison its own closed
  // data store's entry, never the entry a freshly started server reads. Weak keys let a discarded data store — and
  // its cached supplier, which captures that data store — be collected once nothing else references it.
  private static final ConcurrentMap<OperationalDataStore, TenantReference<ResettableExpiringMemoizingSupplier<Map<String, SystemConfigurationProperty>>>> cacheByDataStore =
      CacheBuilder.newBuilder()
          .weakKeys()
          .<OperationalDataStore, TenantReference<ResettableExpiringMemoizingSupplier<Map<String, SystemConfigurationProperty>>>>build()
          .asMap();

  @Inject
  public SystemConfigurationPropertyDAO(OperationalDataStore operationalDataStore) {
    super(operationalDataStore);
    // Start this data store with a clean cache. Scoped to this data store so it never disturbs another server's
    // entry. In production the @Singleton is built once; in tests each new DAO reloads its data store's properties.
    cacheByDataStore.remove(operationalDataStore);
  }

  private TenantReference<ResettableExpiringMemoizingSupplier<Map<String, SystemConfigurationProperty>>> cacheForDataStore() {
    return cacheByDataStore.computeIfAbsent(getDataStore(), _ -> new TenantReference<>());
  }

  private Map<String, SystemConfigurationProperty> getCachedProperties() {
    var supplier = cacheForDataStore().computeIfAbsent(
        _ -> new ResettableExpiringMemoizingSupplier<>(this::loadAllFromDb, CACHE_TTL));
    return supplier.get();
  }

  /**
   * Invalidates this data store's cache so subsequent reads reload from the database. Propagation is best-effort
   * within one {@link #CACHE_TTL}: a reader that already dereferenced the cache when this runs may still return the
   * prior value, the same eventual-consistency bound noted on the TTL above.
   * <p>
   * In single-tenant or global-tenant context, replaces the entire cache reference
   * (invalidating all tenants) because tenant caches include merged global properties.
   * In a per-tenant context, only the current tenant's cache is reset.
   * </p>
   */
  public void invalidateCache() {
    var tenantUtil = new TenantUtil();
    if (tenantUtil.isSingleTenant() || tenantUtil.isGlobalTenant()) {
      // Global change affects all tenants' merged views — replace this data store's entire cache
      cacheByDataStore.put(getDataStore(), new TenantReference<>());
    }
    else {
      // null means no supplier yet for this tenant — next getCachedProperties() will create one
      var supplier = cacheForDataStore().get();
      if (supplier != null) {
        supplier.reset();
      }
    }
  }

  /**
   * Discards every data store's cache unconditionally. Use in test teardown to drop cached values before the next
   * test reads them.
   */
  public static void invalidateEntireCache() {
    cacheByDataStore.clear();
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
  public int update(TransactionContext tx, SystemConfigurationProperty property) {
    SystemConfigurationProperty existingProperty = getByNameNotNull(tx, property.getName(), true);
    property.setId(existingProperty.getId());
    int updated = tx.dsl()
        .update(SYSTEM_CONFIGURATION_PROPERTY)
        .set(SYSTEM_CONFIGURATION_PROPERTY.NAME, property.getName())
        .set(SYSTEM_CONFIGURATION_PROPERTY.VALUE, property.getValue())
        .where(SYSTEM_CONFIGURATION_PROPERTY.SYSTEM_CONFIGURATION_PROPERTY_ID.eq(property.getId()))
        .execute();
    tx.afterCommit(this::invalidateCache);
    return updated;
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

  /**
   * Stores {@code value} only when the property has no value yet, and returns the value in force afterwards — the
   * existing one when there was one, otherwise the value just stored. The read bypasses the cache and honours the
   * MTIQ global fallback, so a value visible to readers is never replaced. The unique constraint on the property name
   * settles two callers racing on an absent property: the loser's insert fails, and it returns the winner's value
   * rather than the value it tried to store.
   *
   * @throws NullPointerException when {@code value} is null. Unlike {@link #set(String, String)}, which deletes the
   *           property when handed a null value, this method has no meaning for one.
   */
  public String setIfAbsent(String name, String value) {
    Objects.requireNonNull(value, "A null value cannot be stored under '" + name + "'.");
    String existing = getUncached(name);
    if (existing == null) {
      try (TransactionContext tx = createTransactionContext()) {
        tx.begin();
        insert(tx, new SystemConfigurationProperty(name, value));
        tx.commit();
        return value;
      }
      catch (DataAccessException e) {
        existing = getUncached(name);
        if (existing == null) {
          throw e;
        }
      }
    }
    // The value in force was stored by someone else, whose invalidation this node may not have seen, so drop the
    // cached copy here as well: a read that follows this call has to agree with the value it returned.
    invalidateCache();
    return existing;
  }

  private String getUncached(String name) {
    // No begin(): the read borrows a connection per statement instead of holding one until close
    try (TransactionContext tx = createTransactionContext()) {
      return get(tx, name);
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
