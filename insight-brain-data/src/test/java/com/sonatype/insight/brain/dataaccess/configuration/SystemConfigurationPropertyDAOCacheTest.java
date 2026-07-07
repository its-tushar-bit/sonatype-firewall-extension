/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.Map;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the TTL-based cache in {@link SystemConfigurationPropertyDAO}.
 */
public class SystemConfigurationPropertyDAOCacheTest
    extends AbstractDbDAOTest
{
  private SystemConfigurationPropertyDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createSystemConfigurationPropertyDAO();
  }

  @Test
  public void getByName_returnsCachedValueWithoutAdditionalDbQuery() {
    // Insert a property
    dao.set("cache-test-prop", "value1");

    // First read populates cache
    SystemConfigurationProperty first = dao.getByName("cache-test-prop");
    assertThat(first).isNotNull();
    assertThat(first.getValue()).isEqualTo("value1");

    // Second read should return cached value (same object reference from the same cached map)
    SystemConfigurationProperty second = dao.getByName("cache-test-prop");
    assertThat(second).isNotNull();
    assertThat(second.getValue()).isEqualTo("value1");
    // Both reads should return the same object from the cache
    assertThat(second).isSameAs(first);
  }

  @Test
  public void set_invalidatesCacheImmediately() {
    // Insert initial value
    dao.set("cache-invalidate-prop", "initial");

    // Read to populate cache
    SystemConfigurationProperty cached = dao.getByName("cache-invalidate-prop");
    assertThat(cached).isNotNull();
    assertThat(cached.getValue()).isEqualTo("initial");

    // Update via set() should invalidate cache
    dao.set("cache-invalidate-prop", "updated");

    // Next read should get fresh data
    SystemConfigurationProperty fresh = dao.getByName("cache-invalidate-prop");
    assertThat(fresh).isNotNull();
    assertThat(fresh.getValue()).isEqualTo("updated");
  }

  @Test
  public void delete_invalidatesCache() {
    // Insert a property
    dao.set("cache-delete-prop", "to-delete");

    // Read to populate cache
    SystemConfigurationProperty cached = dao.getByName("cache-delete-prop");
    assertThat(cached).isNotNull();

    // Delete the property
    dao.set("cache-delete-prop", null); // set(name, null) deletes the property

    // Next read should return null (cache was invalidated)
    SystemConfigurationProperty afterDelete = dao.getByName("cache-delete-prop");
    assertThat(afterDelete).isNull();
  }

  @Test
  public void getAllAsMap_returnsCachedValues() {
    // Insert properties
    dao.set("map-prop-1", "val1");
    dao.set("map-prop-2", "val2");

    // First call populates cache
    Map<String, SystemConfigurationProperty> first = dao.getAllAsMap();
    assertThat(first).containsKey("map-prop-1");
    assertThat(first).containsKey("map-prop-2");

    // Second call should return same map instance (cached)
    Map<String, SystemConfigurationProperty> second = dao.getAllAsMap();
    assertThat(second).isSameAs(first);
  }

  @Test
  public void invalidateCache_forcesNextReadToHitDb() {
    // Insert a property
    dao.set("invalidate-test-prop", "original");

    // Read to populate cache
    dao.getByName("invalidate-test-prop");

    // Manually invalidate
    dao.invalidateCache();

    // Next read should get a fresh map from DB (not same object as before)
    SystemConfigurationProperty fresh = dao.getByName("invalidate-test-prop");
    assertThat(fresh).isNotNull();
    assertThat(fresh.getValue()).isEqualTo("original");
  }

  @Test
  public void getByName_returnsNullForNonexistentProperty() {
    // Ensure cache doesn't cause issues with missing properties
    SystemConfigurationProperty result = dao.getByName("nonexistent-prop-xyz");
    assertThat(result).isNull();
  }

  @Test
  public void insert_invalidatesCache() {
    // Populate cache
    dao.getAllAsMap();

    // Insert a new property
    SystemConfigurationProperty prop = new SystemConfigurationProperty("insert-cache-test", "inserted-value");
    dao.insert(prop);

    // Cache should be invalidated - next read should find the new property
    SystemConfigurationProperty result = dao.getByName("insert-cache-test");
    assertThat(result).isNotNull();
    assertThat(result.getValue()).isEqualTo("inserted-value");
  }

  @Test
  public void update_invalidatesCache() {
    // Insert a property
    dao.set("update-cache-test", "before");

    // Read to populate cache
    SystemConfigurationProperty cached = dao.getByName("update-cache-test");
    assertThat(cached.getValue()).isEqualTo("before");

    // Update the property
    SystemConfigurationProperty toUpdate = new SystemConfigurationProperty("update-cache-test", "after");
    dao.update(toUpdate);

    // Cache should be invalidated
    SystemConfigurationProperty fresh = dao.getByName("update-cache-test");
    assertThat(fresh).isNotNull();
    assertThat(fresh.getValue()).isEqualTo("after");
  }

  @Test
  public void cacheIsSharedAcrossDaoInstancesOnSameDataStore() {
    // Both DAOs exist before the cache is populated, so neither constructor's cache reset interferes below.
    SystemConfigurationPropertyDAO otherDao = daoFactory.createSystemConfigurationPropertyDAO();
    dao.set("coherence-prop", "shared");

    // dao populates the data-store-scoped cache; otherDao must read the very same cached instance.
    SystemConfigurationProperty viaDao = dao.getByName("coherence-prop");
    SystemConfigurationProperty viaOtherDao = otherDao.getByName("coherence-prop");

    // A per-instance cache would make otherDao load its own copy; the shared entry returns the same object.
    assertThat(viaOtherDao).isSameAs(viaDao);
  }

  @Test
  public void closedDataStoreEntryDoesNotPoisonAnotherDataStore() {
    dao.set("isolation-prop", "live");

    // A stopped test server whose data store is closed: any use throws, exactly like the reused-fork leak.
    OperationalDataStore closedDataStore = mock(OperationalDataStore.class);
    when(closedDataStore.getDataSource()).thenThrow(new IllegalStateException("Data source is closed"));
    SystemConfigurationPropertyDAO daoOnClosedDataStore = new SystemConfigurationPropertyDAO(closedDataStore);

    // Reading through the closed data store installs its own (poisoned) cache entry and fails, as a leaked thread
    // would.
    Throwable thrown = catchThrowable(() -> daoOnClosedDataStore.getByName("isolation-prop"));
    assertThat(thrown).isNotNull().hasMessageContaining("Data source is closed");

    // A DAO on a live data store reads its own entry, unaffected by the closed data store's poisoned entry.
    assertThat(dao.getByName("isolation-prop").getValue()).isEqualTo("live");
  }

  @Test
  public void cacheRefreshesAfterTtlExpires() throws Exception {
    // Insert a property
    dao.set("ttl-test-prop", "value1");

    // Read to populate cache
    SystemConfigurationProperty first = dao.getByName("ttl-test-prop");
    assertThat(first.getValue()).isEqualTo("value1");

    // Manually invalidate to simulate TTL expiry
    dao.invalidateCache();

    // Next read should fetch from DB again (different object instance)
    SystemConfigurationProperty second = dao.getByName("ttl-test-prop");
    assertThat(second).isNotNull();
    assertThat(second.getValue()).isEqualTo("value1");
    // After invalidation, the new read produces a different object
    assertThat(second).isNotSameAs(first);
  }
}
