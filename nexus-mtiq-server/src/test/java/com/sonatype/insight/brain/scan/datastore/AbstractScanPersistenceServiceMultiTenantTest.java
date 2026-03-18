/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Category(SlowTest.class)
public abstract class AbstractScanPersistenceServiceMultiTenantTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  protected static final String APPLICATION_ID = "app1";

  protected static final String SCAN_ID = "scan1";

  protected ScanPersistenceService service;

  protected ScanPersistenceServiceTestHelper helper;

  protected void setup(
      Configurator configurator,
      Supplier<ScanPersistenceServiceTestHelper> helperSupplier) throws Exception
  {
    startIqTestServer(configurator);

    this.service = lookup(ScanPersistenceService.class);
    this.helper = helperSupplier.get();
  }

  @Test
  public abstract void testCorrectImplClass();

  @Test
  @ManualIqServerInit
  public void testGetScan_exists() {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> helper.saveMockScan("scan1"));

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> helper.saveMockScan("scan2"));

    AtomicLong oldTenant1Time = new AtomicLong(0);
    AtomicLong oldTenant2Time = new AtomicLong(0);

    testAsTenant(tenant1, tenant -> {
      var entity = service.getScan(APPLICATION_ID, SCAN_ID);
      assertThat(entity.exists()).isTrue();
      oldTenant1Time.set(entity.getLastModifiedTime());
      assertThat(oldTenant1Time.get()).isGreaterThan(0);

      helper.assertScanContents(entity, helper.getSampleScanContent("scan1"));

      helper.waitForNewFileTime();

      String newContents = "scan1 overwritten xml content";
      try (var writer = entity.getWriter()) {
        writer.write(newContents);
      }
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getScan(APPLICATION_ID, "scan2");
      assertThat(entity.exists()).isTrue();
      oldTenant2Time.set(entity.getLastModifiedTime());
      assertThat(oldTenant2Time.get()).isGreaterThan(0);

      helper.assertScanContents(entity, helper.getSampleScanContent("scan2"));

      helper.waitForNewFileTime();

      String newContents = "scan2 overwritten xml content";
      try (var writer = entity.getWriter()) {
        writer.write(newContents);
      }
    });

    testAsTenant(tenant1, tenant -> {
      long currentTime = System.currentTimeMillis();
      var entity = service.getScan(APPLICATION_ID, SCAN_ID);
      assertThat(helper.readDirectScanFile(APPLICATION_ID, SCAN_ID)).isEqualTo("scan1 overwritten xml content");
      assertThat(entity.getLastModifiedTime()).isGreaterThan(oldTenant1Time.get());
      assertThat(entity.getLastModifiedTime()).isLessThanOrEqualTo(currentTime);

      helper.assertScanContents(entity, "scan1 overwritten xml content");
    });

    testAsTenant(tenant2, tenant -> {
      long currentTime = System.currentTimeMillis();
      var entity = service.getScan(APPLICATION_ID, "scan2");
      assertThat(helper.readDirectScanFile(APPLICATION_ID, "scan2")).isEqualTo("scan2 overwritten xml content");
      assertThat(entity.getLastModifiedTime()).isGreaterThan(oldTenant2Time.get());
      assertThat(entity.getLastModifiedTime()).isLessThanOrEqualTo(currentTime);

      helper.assertScanContents(entity, "scan2 overwritten xml content");
    });
  }

  @Test
  @ManualIqServerInit
  public void testGetScan_notExists() {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> helper.saveEmptyMockScan());

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> helper.saveEmptyMockScan());

    testAsTenant(tenant1, tenant -> {
      var entity = service.getScan(APPLICATION_ID, "nonexistent");
      assertThat(entity.exists()).isFalse();
      assertThatThrownBy(entity::getLastModifiedTime).isInstanceOf(IOException.class);
      assertThatThrownBy(entity::getInputStream).isInstanceOf(IOException.class);

      String newContents = "new scan content";
      try (var writer = entity.getWriter()) {
        writer.write(newContents);
      }

      assertThat(helper.readDirectScanFile(APPLICATION_ID, "nonexistent")).isEqualTo(newContents);
      long currentTime = System.currentTimeMillis();
      assertThat(entity.getLastModifiedTime()).isGreaterThan(0);
      assertThat(entity.getLastModifiedTime()).isLessThanOrEqualTo(currentTime);

      helper.assertScanContents(entity, newContents);
    });

    // new scan not visible in other tenant
    testAsTenant(tenant2, tenant -> {
      var entity = service.getScan(APPLICATION_ID, "nonexistent");
      assertThat(entity.exists()).isFalse();
      assertThatThrownBy(entity::getLastModifiedTime).isInstanceOf(IOException.class);
      assertThatThrownBy(entity::getInputStream).isInstanceOf(IOException.class);
    });
  }

  @Test
  @ManualIqServerInit
  public void testCreateAndMoveTempScan() {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> helper.saveEmptyMockScan());

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> helper.saveEmptyMockScan());

    testAsTenant(tenant1, tenant -> {
      var tempEntity = service.createTempScan(APPLICATION_ID);
      assertThat(tempEntity.exists()).isFalse();

      String scanContent = "temporary scan content 1";
      try (var writer = tempEntity.getWriter()) {
        writer.write(scanContent);
      }

      assertThat(tempEntity.exists()).isTrue();
      helper.assertScanContents(tempEntity, scanContent);

      // Move temp scan to permanent location
      service.moveTempScan(tempEntity, APPLICATION_ID, "moved-scan");

      assertThat(tempEntity.exists()).isFalse();
      var permanentEntity = service.getScan(APPLICATION_ID, "moved-scan");
      assertThat(permanentEntity.exists()).isTrue();
      helper.assertScanContents(permanentEntity, scanContent);
    });

    testAsTenant(tenant2, tenant -> {
      var tempEntity = service.createTempScan(APPLICATION_ID);
      assertThat(tempEntity.exists()).isFalse();

      String scanContent = "temporary scan content 2";
      try (var writer = tempEntity.getWriter()) {
        writer.write(scanContent);
      }

      assertThat(tempEntity.exists()).isTrue();
      helper.assertScanContents(tempEntity, scanContent);

      // Move temp scan to permanent location
      service.moveTempScan(tempEntity, APPLICATION_ID, "moved-scan");

      assertThat(tempEntity.exists()).isFalse();
      var permanentEntity = service.getScan(APPLICATION_ID, "moved-scan");
      assertThat(permanentEntity.exists()).isTrue();
      helper.assertScanContents(permanentEntity, scanContent);
    });

    // each tenant should only see their own moved scan
    testAsTenant(tenant1, tenant -> {
      var entity = service.getScan(APPLICATION_ID, "moved-scan");
      helper.assertScanContents(entity, "temporary scan content 1");
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getScan(APPLICATION_ID, "moved-scan");
      helper.assertScanContents(entity, "temporary scan content 2");
    });
  }

  @Test
  @ManualIqServerInit
  public void testDeleteScan() {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveMockScan("scan1");
      helper.saveMockScan("scan2");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveMockScan("scan1");
      helper.saveMockScan("scan2");
    });

    testAsTenant(tenant1, tenant -> {
      var entity = service.getScan(APPLICATION_ID, SCAN_ID);
      assertThat(entity.exists()).isTrue();

      boolean deleted = service.deleteScan(entity);
      assertThat(deleted).isTrue();
      assertThat(entity.exists()).isFalse();

      // Other scan should still exist
      var otherEntity = service.getScan(APPLICATION_ID, "scan2");
      assertThat(otherEntity.exists()).isTrue();
    });

    // Deletion should not affect other tenant
    testAsTenant(tenant2, tenant -> {
      var entity = service.getScan(APPLICATION_ID, SCAN_ID);
      assertThat(entity.exists()).isTrue();
      helper.assertScanContents(entity, helper.getSampleScanContent("scan1"));

      var otherEntity = service.getScan(APPLICATION_ID, "scan2");
      assertThat(otherEntity.exists()).isTrue();
      helper.assertScanContents(otherEntity, helper.getSampleScanContent("scan2"));
    });
  }

  @Test
  @ManualIqServerInit
  public void testDeleteScansFor() {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveMockScan("scan1");
      helper.saveMockScan("scan2");
      helper.saveMockScan("scan3");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveMockScan("scan1");
      helper.saveMockScan("scan2");
      helper.saveMockScan("scan3");
    });

    testAsTenant(tenant1, tenant -> {
      assertThat(service.getScan(APPLICATION_ID, "scan1").exists()).isTrue();
      assertThat(service.getScan(APPLICATION_ID, "scan2").exists()).isTrue();
      assertThat(service.getScan(APPLICATION_ID, "scan3").exists()).isTrue();

      service.deleteScansFor(APPLICATION_ID);

      assertThat(service.getScan(APPLICATION_ID, "scan1").exists()).isFalse();
      assertThat(service.getScan(APPLICATION_ID, "scan2").exists()).isFalse();
      assertThat(service.getScan(APPLICATION_ID, "scan3").exists()).isFalse();
    });

    // Deletion should not affect other tenant
    testAsTenant(tenant2, tenant -> {
      assertThat(service.getScan(APPLICATION_ID, "scan1").exists()).isTrue();
      assertThat(service.getScan(APPLICATION_ID, "scan2").exists()).isTrue();
      assertThat(service.getScan(APPLICATION_ID, "scan3").exists()).isTrue();

      helper.assertScanContents(service.getScan(APPLICATION_ID, "scan1"), helper.getSampleScanContent("scan1"));
      helper.assertScanContents(service.getScan(APPLICATION_ID, "scan2"), helper.getSampleScanContent("scan2"));
      helper.assertScanContents(service.getScan(APPLICATION_ID, "scan3"), helper.getSampleScanContent("scan3"));
    });
  }

  @Test
  @ManualIqServerInit
  public void testAllScanFilesFor() {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveMockScan("scan1");
      helper.saveMockScan("scan2");
      helper.saveMockScan("scan3");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveMockScan("scan1");
      helper.saveMockScan("scan4");
    });

    testAsTenant(tenant1, tenant -> {
      try (var stream = service.allScanFilesFor(APPLICATION_ID)) {
        var scanEntities = stream.toArray(ScanEntity[]::new);
        var scanNames = Arrays.stream(scanEntities)
            .map(ScanEntity::getName)
            .toArray(String[]::new);
        assertThat(scanNames).containsExactlyInAnyOrder("scan-scan1.xml.gz", "scan-scan2.xml.gz", "scan-scan3.xml.gz");
      }
    });

    testAsTenant(tenant2, tenant -> {
      try (var stream = service.allScanFilesFor(APPLICATION_ID)) {
        var scanEntities = stream.toArray(ScanEntity[]::new);
        var scanNames = Arrays.stream(scanEntities)
            .map(ScanEntity::getName)
            .toArray(String[]::new);
        assertThat(scanNames).containsExactlyInAnyOrder("scan-scan1.xml.gz", "scan-scan4.xml.gz");
      }
    });
  }

  @Test
  @ManualIqServerInit
  public void testCopyScanFile() {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> helper.saveMockScan("original"));

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> helper.saveMockScan("original"));

    testAsTenant(tenant1, tenant -> {
      var sourceEntity = service.getScan(APPLICATION_ID, "original");
      var targetEntity = service.getScan(APPLICATION_ID, "copy");

      assertThat(sourceEntity.exists()).isTrue();
      assertThat(targetEntity.exists()).isFalse();

      service.copyScanFile(sourceEntity, targetEntity);

      assertThat(sourceEntity.exists()).isTrue();
      assertThat(targetEntity.exists()).isTrue();

      helper.assertScanContents(sourceEntity, helper.getSampleScanContent("original"));
      helper.assertScanContents(targetEntity, helper.getSampleScanContent("original"));
    });

    testAsTenant(tenant2, tenant -> {
      var sourceEntity = service.getScan(APPLICATION_ID, "original");
      var targetEntity = service.getScan(APPLICATION_ID, "copy");

      assertThat(sourceEntity.exists()).isTrue();
      assertThat(targetEntity.exists()).isFalse();

      service.copyScanFile(sourceEntity, targetEntity);

      assertThat(sourceEntity.exists()).isTrue();
      assertThat(targetEntity.exists()).isTrue();

      helper.assertScanContents(sourceEntity, helper.getSampleScanContent("original"));
      helper.assertScanContents(targetEntity, helper.getSampleScanContent("original"));
    });

    // Verify isolation - each tenant should only see their own copied scan
    testAsTenant(tenant1, tenant -> {
      var originalEntity = service.getScan(APPLICATION_ID, "original");
      var copyEntity = service.getScan(APPLICATION_ID, "copy");

      helper.assertScanContents(originalEntity, helper.getSampleScanContent("original"));
      helper.assertScanContents(copyEntity, helper.getSampleScanContent("original"));
    });

    testAsTenant(tenant2, tenant -> {
      var originalEntity = service.getScan(APPLICATION_ID, "original");
      var copyEntity = service.getScan(APPLICATION_ID, "copy");

      helper.assertScanContents(originalEntity, helper.getSampleScanContent("original"));
      helper.assertScanContents(copyEntity, helper.getSampleScanContent("original"));
    });
  }

  @Test
  @ManualIqServerInit
  public void testGetScanByName() {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> helper.saveMockScan("custom-scan"));

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> helper.saveMockScan("custom-scan"));

    testAsTenant(tenant1, tenant -> {
      var entity = service.getScanByName(APPLICATION_ID, "scan-custom-scan.xml.gz");
      assertThat(entity.exists()).isTrue();
      helper.assertScanContents(entity, helper.getSampleScanContent("custom-scan"));
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getScanByName(APPLICATION_ID, "scan-custom-scan.xml.gz");
      assertThat(entity.exists()).isTrue();
      helper.assertScanContents(entity, helper.getSampleScanContent("custom-scan"));
    });
  }
}
