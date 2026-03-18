/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.testing.FunctionUtils.PredicateWithException;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceTestHelper.APPLICATION_ID;
import static com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceTestHelper.SCAN_ID;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsNewTenant;
import static com.sonatype.insight.brain.tenancy.TenantTestHelper.testAsTenant;
import static com.sonatype.insight.brain.testing.FunctionUtils.wrapException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Category(SlowTest.class)
public abstract class AbstractApplicationReportPersistenceServiceMultiTenantTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  protected ApplicationReportPersistenceService service;

  protected ApplicationReportPersistenceServiceTestHelper helper;

  /**
   * Should be called in @Before by subclasses. The helperSupplier should return a new instance of the helper class.
   */
  protected void setup(
      Configurator configurator,
      Supplier<ApplicationReportPersistenceServiceTestHelper> helperSupplier) throws Exception
  {
    startIqTestServer(configurator);

    this.service = lookup(ApplicationReportPersistenceService.class);
    this.helper = helperSupplier.get();
  }

  @Test
  public abstract void testCorrectImplClass();

  @Test
  @ManualIqServerInit
  public void testGetReportEntity_exists() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveMockReport("report1");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveMockReport("report2");
    });

    AtomicLong oldTenant1Time = new AtomicLong(0);
    AtomicLong oldTenant2Time = new AtomicLong(0);

    testAsTenant(tenant1, tenant -> {
      var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "bom.json");
      assertThat(entity.getName()).isEqualTo("bom.json");
      assertThat(entity.exists()).isTrue();
      oldTenant1Time.set(entity.getTime());
      assertThat(oldTenant1Time.get()).isGreaterThan(0);

      helper.assertEntityContents(entity, "report1 bom\n");

      helper.waitForNewFileTime();

      String newContents = "report1 overwritten bom";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "bom.json");
      assertThat(entity.getName()).isEqualTo("bom.json");
      assertThat(entity.exists()).isTrue();
      oldTenant2Time.set(entity.getTime());
      assertThat(oldTenant2Time.get()).isGreaterThan(0);

      helper.assertEntityContents(entity, "report2 bom\n");

      helper.waitForNewFileTime();

      String newContents = "report2 overwritten bom";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }
    });

    testAsTenant(tenant1, tenant -> {
      var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "bom.json");
      assertThat(helper.readFromLocalFiles("bom.json")).isEqualTo("report1 overwritten bom");
      assertThat(helper.readFromOriginalFiles("bom.json")).isEqualTo("report1 bom\n");
      assertThat(helper.readFromAdditionalFiles("bom.json")).isNull();
      assertThat(entity.getTime()).isGreaterThan(oldTenant1Time.get());
      assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

      helper.assertEntityContents(entity, "report1 overwritten bom");
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "bom.json");
      assertThat(helper.readFromLocalFiles("bom.json")).isEqualTo("report2 overwritten bom");
      assertThat(helper.readFromOriginalFiles("bom.json")).isEqualTo("report2 bom\n");
      assertThat(helper.readFromAdditionalFiles("bom.json")).isNull();
      assertThat(entity.getTime()).isGreaterThan(oldTenant2Time.get());
      assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

      helper.assertEntityContents(entity, "report2 overwritten bom");
    });
  }

  @Test
  @ManualIqServerInit
  public void testGetReportEntity_notExists() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
    });

    testAsTenant(tenant1, tenant -> {
      var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "foo");
      assertThat(entity.getName()).isEqualTo("foo");
      assertThat(entity.exists()).isFalse();
      assertThatThrownBy(entity::getTime).isInstanceOf(IOException.class);
      assertThatThrownBy(entity::getInputStream).isInstanceOf(IOException.class);

      String newContents = "foobar";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }

      assertThat(helper.readFromLocalFiles("foo")).isEqualTo(newContents);
      assertThat(helper.readFromOriginalFiles("foo")).isNull();
      assertThat(helper.readFromAdditionalFiles("foo")).isNull();
      assertThat(entity.getTime()).isGreaterThan(0);
      assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

      helper.assertEntityContents(entity, newContents);
    });

    // new entity not visible in other tenant
    testAsTenant(tenant2, tenant -> {
      var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "foo");
      assertThat(entity.getName()).isEqualTo("foo");
      assertThat(entity.exists()).isFalse();
      assertThatThrownBy(entity::getTime).isInstanceOf(IOException.class);
      assertThatThrownBy(entity::getInputStream).isInstanceOf(IOException.class);
    });
  }

  @Test
  @ManualIqServerInit
  public void testGetReportEntity_existsLocalOnly() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
      helper.writeLocalFile("foo.txt", "foo");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
    });

    AtomicLong oldTenant1Time = new AtomicLong(0);

    testAsTenant(tenant1, tenant -> {
      var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "foo.txt");
      assertThat(entity.getName()).isEqualTo("foo.txt");
      assertThat(entity.exists()).isTrue();
      oldTenant1Time.set(entity.getTime());
      assertThat(oldTenant1Time.get()).isGreaterThan(0);

      helper.assertEntityContents(entity, "foo");

      helper.waitForNewFileTime();

      String newContents = "bar";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "foo.txt");
      assertThat(entity.getName()).isEqualTo("foo.txt");
      assertThat(entity.exists()).isFalse();

      helper.waitForNewFileTime();

      String newContents = "baz";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }
    });

    testAsTenant(tenant1, tenant -> {
      var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "foo.txt");
      assertThat(helper.readFromLocalFiles("foo.txt")).isEqualTo("bar");
      assertThat(helper.readFromOriginalFiles("foo.txt")).isNull();
      assertThat(helper.readFromAdditionalFiles("foo.txt")).isNull();
      assertThat(entity.getTime()).isGreaterThan(oldTenant1Time.get());
      assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

      helper.assertEntityContents(entity, "bar");
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "foo.txt");
      assertThat(helper.readFromLocalFiles("foo.txt")).isEqualTo("baz");
      assertThat(helper.readFromOriginalFiles("foo.txt")).isNull();
      assertThat(helper.readFromAdditionalFiles("foo.txt")).isNull();

      helper.assertEntityContents(entity, "baz");
    });
  }

  @Test
  @ManualIqServerInit
  public void testGetReportEntity_existsAsAdditionalFile() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
      helper.writeAdditionalFile("foo.txt", "foo1");
      helper.writeAdditionalFile("bar.txt", "bar1");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
      helper.writeAdditionalFile("foo.txt", "foo2");
      helper.writeAdditionalFile("bar.txt", "bar2");
    });

    testAsTenant(tenant1, tenant -> {
      var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "foo.txt");
      assertThat(entity.getName()).isEqualTo("foo.txt");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(0);

      helper.assertEntityContents(entity, "foo1");

      String newContents = "something completely different";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }

      assertThat(helper.readFromLocalFiles("foo.txt")).isNull();
      assertThat(helper.readFromOriginalFiles("foo.txt")).isNull();
      assertThat(helper.readFromAdditionalFiles("foo.txt")).isEqualTo(newContents);

      helper.assertEntityContents(entity, newContents);

      helper.assertEntityContents(service.getReportEntity(APPLICATION_ID, SCAN_ID, "bar.txt"), "bar1");
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "foo.txt");
      assertThat(entity.getName()).isEqualTo("foo.txt");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(0);

      helper.assertEntityContents(entity, "foo2");

      String newContents = "something completely different 2";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }

      assertThat(helper.readFromLocalFiles("foo.txt")).isNull();
      assertThat(helper.readFromOriginalFiles("foo.txt")).isNull();
      assertThat(helper.readFromAdditionalFiles("foo.txt")).isEqualTo(newContents);

      helper.assertEntityContents(entity, newContents);

      helper.assertEntityContents(service.getReportEntity(APPLICATION_ID, SCAN_ID, "bar.txt"), "bar2");
    });
  }

  @Test
  @ManualIqServerInit
  public void testGetAllReportEntities() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveMockReport("report1");
      helper.writeAdditionalFile("foo.txt", "foo1");
      helper.writeAdditionalFile("bar.txt", "bar1");
      helper.writeLocalFile("new-file.txt", "new file contents 1");
      helper.writeLocalFile("bom.json", "overwritten bom file contents");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveMockReport("report2");
      helper.writeAdditionalFile("foo.txt", "foo2");
      helper.writeAdditionalFile("baz.txt", "baz2");
      helper.writeLocalFile("new-file.txt", "new file contents 2");
      helper.writeLocalFile("licenses.json", "overwritten license file contents");
    });

    testAsTenant(tenant1, tenant -> {
      try (var stream = service.getAllReportEntities(APPLICATION_ID, SCAN_ID)) {
        // Note: can't use assertThat(stream) because it closes the stream before the assertions are run
        var entities = stream.toArray(ReportEntity[]::new);

        assertThat(entities).allMatch(wrapException((PredicateWithException<ReportEntity>) BaseReportEntity::exists))
            .satisfiesExactlyInAnyOrder(
                wrapException(entity -> {
                  // overwritten bom.json should be present, not the original
                  assertThat(entity.getName()).isEqualTo("bom.json");
                  helper.assertEntityContents(entity, "overwritten bom file contents");
                }),
                wrapException(entity -> {
                  // original index.html should be present since it's not overwritten (note index.html is created by
                  // ReportHelper.saveMockReport, it's not in the src/test/resources/… dir)
                  assertThat(entity.getName()).isEqualTo("index.html");
                  helper.assertEntityContents(entity, "<html></html>");
                }),
                wrapException(entity -> {
                  // original licenses.json
                  assertThat(entity.getName()).isEqualTo("licenses.json");
                  helper.assertEntityContents(entity, "report1 licenses\n");
                }),
                wrapException(entity -> {
                  // new file from cache dir
                  assertThat(entity.getName()).isEqualTo("new-file.txt");
                  helper.assertEntityContents(entity, "new file contents 1");
                }),
                wrapException(entity -> {
                  // new file from additional files dir
                  assertThat(entity.getName()).isEqualTo("foo.txt");
                  helper.assertEntityContents(entity, "foo1");
                }),
                wrapException(entity -> {
                  // new file from additional files dir
                  assertThat(entity.getName()).isEqualTo("bar.txt");
                  helper.assertEntityContents(entity, "bar1");
                }));
      }
    });

    testAsTenant(tenant2, tenant -> {
      try (var stream = service.getAllReportEntities(APPLICATION_ID, SCAN_ID)) {
        // Note: can't use assertThat(stream) because it closes the stream before the assertions are run
        var entities = stream.toArray(ReportEntity[]::new);

        assertThat(entities).allMatch(wrapException((PredicateWithException<ReportEntity>) BaseReportEntity::exists))
            .satisfiesExactlyInAnyOrder(
                wrapException(entity -> {
                  // original bom.json
                  assertThat(entity.getName()).isEqualTo("bom.json");
                  helper.assertEntityContents(entity, "report2 bom\n");
                }),
                wrapException(entity -> {
                  // overwritten licenses.json
                  assertThat(entity.getName()).isEqualTo("licenses.json");
                  helper.assertEntityContents(entity, "overwritten license file contents");
                }),
                wrapException(entity -> {
                  // original index.html should be present since it's not overwritten (note index.html is created by
                  // ReportHelper.saveMockReport, it's not in the src/test/resources/… dir)
                  assertThat(entity.getName()).isEqualTo("index.html");
                  helper.assertEntityContents(entity, "<html></html>");
                }),
                wrapException(entity -> {
                  // new file from cache dir
                  assertThat(entity.getName()).isEqualTo("new-file.txt");
                  helper.assertEntityContents(entity, "new file contents 2");
                }),
                wrapException(entity -> {
                  // new file from additional files dir
                  assertThat(entity.getName()).isEqualTo("foo.txt");
                  helper.assertEntityContents(entity, "foo2");
                }),
                wrapException(entity -> {
                  // new file from additional files dir
                  assertThat(entity.getName()).isEqualTo("baz.txt");
                  helper.assertEntityContents(entity, "baz2");
                }));
      }
    });
  }

  @Test
  @ManualIqServerInit
  public void testSaveOriginalReport() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      try (var zipStream =
          getClass().getResourceAsStream("/ApplicationReportPersistenceServiceTest/report1.zip"))
      {
        service.saveOriginalReport(APPLICATION_ID, SCAN_ID, zipStream);
      }
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      try (var zipStream =
          getClass().getResourceAsStream("/ApplicationReportPersistenceServiceTest/report2.zip"))
      {
        service.saveOriginalReport(APPLICATION_ID, SCAN_ID, zipStream);
      }
    });

    testAsTenant(tenant1, tenant -> {
      assertThat(helper.readFromOriginalFiles("bom.json")).isEqualTo("report1.zip bom\n");
      assertThat(helper.readFromOriginalFiles("licenses.json")).isEqualTo("report1.zip licenses\n");
    });

    testAsTenant(tenant2, tenant -> {
      assertThat(helper.readFromOriginalFiles("bom.json")).isEqualTo("report2.zip bom\n");
      assertThat(helper.readFromOriginalFiles("licenses.json")).isEqualTo("report2.zip licenses\n");
    });
  }

  @Test
  @ManualIqServerInit
  public void testSaveReportFile_newFile_notInZip() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
    });

    Instant now = Instant.now();
    helper.waitForNewFileTime();

    testAsTenant(tenant1, tenant -> {
      service.saveReportFile(APPLICATION_ID, SCAN_ID, "new-file.txt",
          new ByteArrayInputStream("new file contents 1".getBytes(StandardCharsets.UTF_8)));
    });

    testAsTenant(tenant2, tenant -> {
      service.saveReportFile(APPLICATION_ID, SCAN_ID, "new-file.txt",
          new ByteArrayInputStream("new file contents 2".getBytes(StandardCharsets.UTF_8)));
    });

    testAsTenant(tenant1, tenant -> {
      assertThat(helper.readFromLocalFiles("new-file.txt")).isEqualTo("new file contents 1");
      assertThat(helper.readFromOriginalFiles("new-file.txt")).isNull();
      assertThat(helper.readFromAdditionalFiles("new-file.txt")).isNull();

      ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "new-file.txt");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
      helper.assertEntityContents(entity, "new file contents 1");
    });

    testAsTenant(tenant2, tenant -> {
      assertThat(helper.readFromLocalFiles("new-file.txt")).isEqualTo("new file contents 2");
      assertThat(helper.readFromOriginalFiles("new-file.txt")).isNull();
      assertThat(helper.readFromAdditionalFiles("new-file.txt")).isNull();

      ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "new-file.txt");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
      helper.assertEntityContents(entity, "new file contents 2");
    });
  }

  @Test
  @ManualIqServerInit
  public void testSaveReportFile_updateFile_notInZip() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
      helper.writeLocalFile("file.txt", "old file contents 1");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
      helper.writeLocalFile("file.txt", "old file contents 2");
    });

    Instant now = Instant.now();
    helper.waitForNewFileTime();

    testAsTenant(tenant1, tenant -> {
      service.saveReportFile(APPLICATION_ID, SCAN_ID, "file.txt",
          new ByteArrayInputStream("new file contents 1".getBytes(StandardCharsets.UTF_8)));
    });

    testAsTenant(tenant2, tenant -> {
      service.saveReportFile(APPLICATION_ID, SCAN_ID, "file.txt",
          new ByteArrayInputStream("new file contents 2".getBytes(StandardCharsets.UTF_8)));
    });

    testAsTenant(tenant1, tenant -> {
      assertThat(helper.readFromLocalFiles("file.txt")).isEqualTo("new file contents 1");
      assertThat(helper.readFromOriginalFiles("file.txt")).isNull();
      assertThat(helper.readFromAdditionalFiles("file.txt")).isNull();

      ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "file.txt");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
      helper.assertEntityContents(entity, "new file contents 1");
    });

    testAsTenant(tenant2, tenant -> {
      assertThat(helper.readFromLocalFiles("file.txt")).isEqualTo("new file contents 2");
      assertThat(helper.readFromOriginalFiles("file.txt")).isNull();
      assertThat(helper.readFromAdditionalFiles("file.txt")).isNull();

      ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "file.txt");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
      helper.assertEntityContents(entity, "new file contents 2");
    });
  }

  @Test
  @ManualIqServerInit
  public void testSaveReportFile_updateFile_inZip() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveMockReport("report1");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveMockReport("report2");
    });

    Instant now = Instant.now();
    helper.waitForNewFileTime();

    testAsTenant(tenant1, tenant -> {
      service.saveReportFile(APPLICATION_ID, SCAN_ID, "bom.json",
          new ByteArrayInputStream("new file contents 1".getBytes(StandardCharsets.UTF_8)));
    });

    testAsTenant(tenant2, tenant -> {
      service.saveReportFile(APPLICATION_ID, SCAN_ID, "bom.json",
          new ByteArrayInputStream("new file contents 2".getBytes(StandardCharsets.UTF_8)));
    });

    testAsTenant(tenant1, tenant -> {
      assertThat(helper.readFromLocalFiles("bom.json")).isEqualTo("new file contents 1");
      assertThat(helper.readFromOriginalFiles("bom.json")).isEqualTo("report1 bom\n");
      assertThat(helper.readFromAdditionalFiles("bom.json")).isNull();

      ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "bom.json");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
      helper.assertEntityContents(entity, "new file contents 1");
    });

    testAsTenant(tenant2, tenant -> {
      assertThat(helper.readFromLocalFiles("bom.json")).isEqualTo("new file contents 2");
      assertThat(helper.readFromOriginalFiles("bom.json")).isEqualTo("report2 bom\n");
      assertThat(helper.readFromAdditionalFiles("bom.json")).isNull();

      ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "bom.json");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
      helper.assertEntityContents(entity, "new file contents 2");
    });
  }

  @Test
  @ManualIqServerInit
  public void testSaveReportFile_updateFile_inZipAndLocalCache() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveMockReport("report1");
      helper.writeLocalFile("bom.json", "old local file contents 1");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveMockReport("report2");
      helper.writeLocalFile("bom.json", "old local file contents 2");
    });

    Instant now = Instant.now();
    helper.waitForNewFileTime();

    testAsTenant(tenant1, tenant -> {
      service.saveReportFile(APPLICATION_ID, SCAN_ID, "bom.json",
          new ByteArrayInputStream("new file contents 1".getBytes(StandardCharsets.UTF_8)));
    });

    testAsTenant(tenant2, tenant -> {
      service.saveReportFile(APPLICATION_ID, SCAN_ID, "bom.json",
          new ByteArrayInputStream("new file contents 2".getBytes(StandardCharsets.UTF_8)));
    });

    testAsTenant(tenant1, tenant -> {
      assertThat(helper.readFromLocalFiles("bom.json")).isEqualTo("new file contents 1");
      assertThat(helper.readFromOriginalFiles("bom.json")).isEqualTo("report1 bom\n");
      assertThat(helper.readFromAdditionalFiles("bom.json")).isNull();

      ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "bom.json");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
      helper.assertEntityContents(entity, "new file contents 1");
    });

    testAsTenant(tenant2, tenant -> {
      assertThat(helper.readFromLocalFiles("bom.json")).isEqualTo("new file contents 2");
      assertThat(helper.readFromOriginalFiles("bom.json")).isEqualTo("report2 bom\n");
      assertThat(helper.readFromAdditionalFiles("bom.json")).isNull();

      ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "bom.json");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
      helper.assertEntityContents(entity, "new file contents 2");
    });
  }

  @Test
  @ManualIqServerInit
  public void testSaveAdditionalReportFile_newFile() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
    });

    Instant now = Instant.now();
    helper.waitForNewFileTime();

    testAsTenant(tenant1, tenant -> {
      service.saveAdditionalReportFile(APPLICATION_ID, SCAN_ID, "new-file.txt",
          new ByteArrayInputStream("new file contents 1".getBytes(StandardCharsets.UTF_8)));
    });

    testAsTenant(tenant2, tenant -> {
      service.saveAdditionalReportFile(APPLICATION_ID, SCAN_ID, "new-file.txt",
          new ByteArrayInputStream("new file contents 2".getBytes(StandardCharsets.UTF_8)));
    });

    testAsTenant(tenant1, tenant -> {
      assertThat(helper.readFromLocalFiles("new-file.txt")).isNull();
      assertThat(helper.readFromOriginalFiles("new-file.txt")).isNull();
      assertThat(helper.readFromAdditionalFiles("new-file.txt")).isEqualTo("new file contents 1");

      ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "new-file.txt");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
      helper.assertEntityContents(entity, "new file contents 1");
    });

    testAsTenant(tenant2, tenant -> {
      assertThat(helper.readFromLocalFiles("new-file.txt")).isNull();
      assertThat(helper.readFromOriginalFiles("new-file.txt")).isNull();
      assertThat(helper.readFromAdditionalFiles("new-file.txt")).isEqualTo("new file contents 2");

      ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "new-file.txt");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
      helper.assertEntityContents(entity, "new file contents 2");
    });
  }

  @Test
  @ManualIqServerInit
  public void testSaveAdditionalReportFile_updateFile() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
      helper.writeAdditionalFile("file.txt", "old file contents 1");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
      helper.writeAdditionalFile("file.txt", "old file contents 2");
    });

    Instant now = Instant.now();
    helper.waitForNewFileTime();

    testAsTenant(tenant1, tenant -> {
      service.saveAdditionalReportFile(APPLICATION_ID, SCAN_ID, "txt",
          new ByteArrayInputStream("new file contents 1".getBytes(StandardCharsets.UTF_8)));
    });

    testAsTenant(tenant2, tenant -> {
      service.saveAdditionalReportFile(APPLICATION_ID, SCAN_ID, "txt",
          new ByteArrayInputStream("new file contents 2".getBytes(StandardCharsets.UTF_8)));
    });

    testAsTenant(tenant1, tenant -> {
      assertThat(helper.readFromLocalFiles("txt")).isNull();
      assertThat(helper.readFromOriginalFiles("txt")).isNull();
      assertThat(helper.readFromAdditionalFiles("txt")).isEqualTo("new file contents 1");

      ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "txt");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
      helper.assertEntityContents(entity, "new file contents 1");
    });

    testAsTenant(tenant2, tenant -> {
      assertThat(helper.readFromLocalFiles("txt")).isNull();
      assertThat(helper.readFromOriginalFiles("txt")).isNull();
      assertThat(helper.readFromAdditionalFiles("txt")).isEqualTo("new file contents 2");

      ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "txt");
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
      helper.assertEntityContents(entity, "new file contents 2");
    });
  }

  @Test
  @ManualIqServerInit
  public void testGetPdfEntity() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
      helper.writePdf("PDF1");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
      helper.writePdf("PDF2");
    });

    AtomicLong oldTenant1Time = new AtomicLong();
    AtomicLong oldTenant2Time = new AtomicLong();

    testAsTenant(tenant1, tenant -> {
      var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);
      oldTenant1Time.set(entity.getTime());
      assertThat(entity.exists()).isTrue();
      assertThat(entity.length()).isEqualTo(4);
      assertThat(entity.canCreate()).isFalse();
      assertThat(oldTenant1Time.get()).isGreaterThan(0);
      assertThat(oldTenant1Time.get()).isLessThanOrEqualTo(System.currentTimeMillis());

      helper.assertEntityContents(entity, "PDF1");

      helper.waitForNewFileTime();

      String newContents = "PDF1a";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);
      oldTenant2Time.set(entity.getTime());
      assertThat(entity.exists()).isTrue();
      assertThat(entity.length()).isEqualTo(4);
      assertThat(entity.canCreate()).isFalse();
      assertThat(oldTenant2Time.get()).isGreaterThan(0);
      assertThat(oldTenant2Time.get()).isLessThanOrEqualTo(System.currentTimeMillis());

      helper.assertEntityContents(entity, "PDF2");

      helper.waitForNewFileTime();

      String newContents = "PDF2a";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }
    });

    testAsTenant(tenant1, tenant -> {
      var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);
      assertThat(entity.exists()).isTrue();
      assertThat(entity.length()).isEqualTo(5);
      assertThat(entity.canCreate()).isFalse();
      assertThat(entity.getTime()).isGreaterThan(oldTenant1Time.get());
      assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

      helper.assertEntityContents(entity, "PDF1a");
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);
      assertThat(entity.exists()).isTrue();
      assertThat(entity.length()).isEqualTo(5);
      assertThat(entity.canCreate()).isFalse();
      assertThat(entity.getTime()).isGreaterThan(oldTenant2Time.get());
      assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

      helper.assertEntityContents(entity, "PDF2a");
    });
  }

  @Test
  @ManualIqServerInit
  public void testGetPdfEntity_notExists() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
      helper.writePdf("PDF2");
    });

    testAsTenant(tenant1, tenant -> {
      var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);
      assertThat(entity.exists()).isFalse();
      assertThat(entity.canCreate()).isTrue();
      assertThatThrownBy(entity::length).isInstanceOf(IOException.class);
      assertThatThrownBy(entity::getTime).isInstanceOf(IOException.class);
      assertThatThrownBy(entity::getInputStream).isInstanceOf(IOException.class);

      String newContents = "PDF1";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }

      assertThat(entity.exists()).isTrue();
      assertThat(entity.canCreate()).isFalse();
      assertThat(entity.getTime()).isGreaterThan(0);

      helper.assertEntityContents(entity, newContents);
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);
      helper.assertEntityContents(entity, "PDF2");
    });
  }

  @Test
  @ManualIqServerInit
  public void testGetPdfEntity_deleteIfExists() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
      helper.writePdf("PDF1");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
      helper.writePdf("PDF2");
    });

    testAsTenant(tenant1, tenant -> {
      var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);
      assertThat(entity.exists()).isTrue();

      entity.deleteIfExists();

      assertThat(entity.exists()).isFalse();
      assertThat(helper.readPdf()).isNull();
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);

      // unaffected by delete in entity 1
      assertThat(entity.exists()).isTrue();
      assertThat(helper.readPdf()).isEqualTo("PDF2");
    });
  }

  @Test
  @ManualIqServerInit
  public void testGetVulnerabilitySignaturesEntity() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
      helper.writeVulnerabilitySignatures("sig1");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
      helper.writeVulnerabilitySignatures("sig2");
    });

    AtomicLong oldTenant1Time = new AtomicLong();
    AtomicLong oldTenant2Time = new AtomicLong();

    testAsTenant(tenant1, tenant -> {
      var entity = service.getVulnerabilitySignaturesEntity(APPLICATION_ID, SCAN_ID);
      oldTenant1Time.set(entity.getTime());
      assertThat(entity.exists()).isTrue();
      assertThat(oldTenant1Time.get()).isGreaterThan(0);
      assertThat(oldTenant1Time.get()).isLessThanOrEqualTo(System.currentTimeMillis());

      helper.assertEntityContents(entity, "sig1");

      helper.waitForNewFileTime();

      String newContents = "sig1a";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getVulnerabilitySignaturesEntity(APPLICATION_ID, SCAN_ID);
      oldTenant2Time.set(entity.getTime());
      assertThat(entity.exists()).isTrue();
      assertThat(oldTenant2Time.get()).isGreaterThan(0);
      assertThat(oldTenant2Time.get()).isLessThanOrEqualTo(System.currentTimeMillis());

      helper.assertEntityContents(entity, "sig2");

      helper.waitForNewFileTime();

      String newContents = "sig2a";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }
    });

    testAsTenant(tenant1, tenant -> {
      var entity = service.getVulnerabilitySignaturesEntity(APPLICATION_ID, SCAN_ID);
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(oldTenant1Time.get());
      assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

      helper.assertEntityContents(entity, "sig1a");
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getVulnerabilitySignaturesEntity(APPLICATION_ID, SCAN_ID);
      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(oldTenant2Time.get());
      assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

      helper.assertEntityContents(entity, "sig2a");
    });
  }

  @Test
  @ManualIqServerInit
  public void testGetVulnerabilitySignaturesEntity_notExists() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
      helper.writeVulnerabilitySignatures("sig2");
    });

    testAsTenant(tenant1, tenant -> {
      var entity = service.getVulnerabilitySignaturesEntity(APPLICATION_ID, SCAN_ID);
      assertThat(entity.exists()).isFalse();
      assertThatThrownBy(entity::getTime).isInstanceOf(IOException.class);
      assertThatThrownBy(entity::getInputStream).isInstanceOf(IOException.class);

      String newContents = "sig1";
      try (var outputStream = entity.getOutputStream()) {
        outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
      }

      assertThat(entity.exists()).isTrue();
      assertThat(entity.getTime()).isGreaterThan(0);

      helper.assertEntityContents(entity, newContents);
    });

    testAsTenant(tenant2, tenant -> {
      var entity = service.getVulnerabilitySignaturesEntity(APPLICATION_ID, SCAN_ID);
      helper.assertEntityContents(entity, "sig2");
    });
  }

  @Test
  @ManualIqServerInit
  public void testReportExists() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isFalse();
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isFalse();
    });

    testAsTenant(tenant1, tenant -> {
      helper.saveEmptyMockReport();
      assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isTrue();
    });

    testAsTenant(tenant2, tenant -> {
      assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isFalse();
    });
  }

  @Test
  @ManualIqServerInit
  public void testDeleteReport() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
    });

    testAsTenant(tenant1, tenant -> {
      service.deleteReport(APPLICATION_ID, SCAN_ID);

      assertThat(helper.readFromOriginalFiles("index.html")).isNull();
      assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isFalse();
    });

    testAsTenant(tenant2, tenant -> {
      assertThat(helper.readFromOriginalFiles("index.html")).isEqualTo("<html></html>");
      assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isTrue();
    });
  }

  @Test
  @ManualIqServerInit
  public void testDeleteReports() throws Exception {
    Tenant tenant1 = testAsNewTenant("tenant1", tenant -> {
      helper.saveEmptyMockReport();
      helper.saveEmptyMockReport("scan2");
    });

    Tenant tenant2 = testAsNewTenant("tenant2", tenant -> {
      helper.saveEmptyMockReport();
      helper.saveEmptyMockReport("scan2");
    });

    testAsTenant(tenant1, tenant -> {
      service.deleteReports(APPLICATION_ID);

      assertThat(helper.readFromOriginalFiles("index.html")).isNull();
      assertThat(helper.readFromOriginalFiles("index.html")).isNull();
      assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isFalse();
      assertThat(service.reportExists(APPLICATION_ID, "scan2")).isFalse();
    });

    testAsTenant(tenant2, tenant -> {
      assertThat(helper.readFromOriginalFiles("index.html")).isEqualTo("<html></html>");
      assertThat(helper.readFromOriginalFiles(APPLICATION_ID, "scan2", "index.html")).isEqualTo("<html></html>");
      assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isTrue();
      assertThat(service.reportExists(APPLICATION_ID, "scan2")).isTrue();
    });
  }
}
