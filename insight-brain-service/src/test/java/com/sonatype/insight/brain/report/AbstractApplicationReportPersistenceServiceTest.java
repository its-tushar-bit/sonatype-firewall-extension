/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.CopyStorageService;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceTestHelper.APPLICATION_ID;
import static com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceTestHelper.SCAN_ID;
import static com.sonatype.insight.brain.testing.FunctionUtils.wrapException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

abstract class AbstractApplicationReportPersistenceServiceTest
    extends AbstractComponentTest
{
  private static final Set<String> BAD_NAMES = Set.of(
      "foo/../bar",
      "foo\\..\\bar",
      "..",
      ".",
      "../bar",
      "bar/..",
      "/",
      "/foo",
      "C:\\foo",
      "..\\foo",
      "foo//..",
      ".\\foo",
      "foo//.",
      "./foo",
      "foo/.",
      "\\",
      "\\foo",
      "",
      " foo",
      "foo ",
      "foo/ bar",
      "foo/bar ",
      "foo/bar /",
      "foo/bar /baz",
      "  "
  );

  private static final Set<String> GOOD_NAMES = Set.of(
      "bar..foo",
      "bar.foo",
      "bar/foo",
      "bar\\foo",
      "bar",
      "foo\\",
      "foo/",
      "foo bar",
      "foo bar/baz"
  );

  @Inject
  protected InsightConfig insightConfig;

  protected ApplicationReportPersistenceService service;

  protected ApplicationReportPersistenceServiceTestHelper helper;

  /**
   * Should be called in @Before by subclasses to specify the service and helper. Prior to this call, the appropriate
   * configs in InsightConfig should be set up to ensure that lookup(ApplicationReportPersistenceService.class) returns
   * the expected service implementation
   */
  protected void setup(ApplicationReportPersistenceServiceTestHelper helper) {
    this.service = lookup(ApplicationReportPersistenceService.class);
    this.helper = helper;
  }

  /**
   * Test that the bound service instance is of the expected type
   */
  @Test
  public abstract void testCorrectImplClass();

  @Test
  @Category(SlowTest.class)
  public void testGetReportEntity_exists() throws Exception {
    helper.saveEmptyMockReport();

    var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "index.html");
    assertThat(entity.getName()).isEqualTo("index.html");
    assertThat(entity.exists()).isTrue();
    var oldTime = entity.getTime();
    assertThat(oldTime).isGreaterThan(0);

    helper.assertEntityContents(entity, "<html></html>");

    helper.waitForNewFileTime();

    String newContents = "<html><title>test</title></html>";
    try (var outputStream = entity.getOutputStream()) {
      outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
    }

    assertThat(helper.readFromLocalFiles("index.html")).isEqualTo(newContents);
    assertThat(helper.readFromOriginalFiles("index.html")).isEqualTo("<html></html>");
    assertThat(helper.readFromAdditionalFiles("index.html")).isNull();
    assertThat(entity.getTime()).isGreaterThan(oldTime);
    assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

    helper.assertEntityContents(entity, newContents);
  }

  @Test
  @Category(SlowTest.class)
  public void testGetReportEntity_notExists() throws Exception {
    helper.saveEmptyMockReport();
    long startTime = System.currentTimeMillis();
    helper.waitForNewFileTime();

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
    assertThat(entity.getTime()).isGreaterThanOrEqualTo(startTime);
    assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());
    assertThat(entity.exists()).isTrue();

    helper.assertEntityContents(entity, newContents);
  }

  @Test
  @Category(SlowTest.class)
  public void testGetReportEntity_existsLocalOnly() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeLocalFile("foo.txt", "foo");

    helper.waitForNewFileTime();

    var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "foo.txt");
    assertThat(entity.getName()).isEqualTo("foo.txt");
    assertThat(entity.exists()).isTrue();
    var oldTime = entity.getTime();
    assertThat(oldTime).isLessThan(System.currentTimeMillis());

    helper.assertEntityContents(entity, "foo");

    helper.waitForNewFileTime();

    String newContents = "bar";
    try (var outputStream = entity.getOutputStream()) {
      outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
    }

    assertThat(helper.readFromLocalFiles("foo.txt")).isEqualTo(newContents);
    assertThat(helper.readFromOriginalFiles("foo.txt")).isNull();
    assertThat(helper.readFromAdditionalFiles("foo.txt")).isNull();
    assertThat(entity.getTime()).isGreaterThan(oldTime);
    assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

    helper.assertEntityContents(entity, newContents);
  }

  @Test
  public void testGetReportEntity_existsAsAdditionalFile() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("foo.txt", "foobar");

    var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "foo.txt");
    assertThat(entity.getName()).isEqualTo("foo.txt");
    assertThat(entity.exists()).isTrue();
    assertThat(entity.getTime()).isGreaterThan(0);

    helper.assertEntityContents(entity, "foobar");

    String newContents = "something completely different";
    try (var outputStream = entity.getOutputStream()) {
      outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
    }

    assertThat(helper.readFromLocalFiles("foo.txt")).isNull();
    assertThat(helper.readFromOriginalFiles("foo.txt")).isNull();
    assertThat(helper.readFromAdditionalFiles("foo.txt")).isEqualTo(newContents);

    helper.assertEntityContents(entity, newContents);
  }

  @Test
  public void testGetReportEntity_invalidNames() throws Exception {
    AtomicInteger count = new AtomicInteger(0);

    assertThat(BAD_NAMES).allSatisfy(name -> {
      String scanName = "scan" + count.getAndIncrement();
      helper.saveEmptyMockReport(scanName);

      assertThatThrownBy(
          () -> service.getReportEntity(APPLICATION_ID, scanName, name)
      ).isInstanceOf(IllegalArgumentException.class);
    });

    assertThat(GOOD_NAMES).allSatisfy(name -> {
      String scanName = "scan" + count.getAndIncrement();
      helper.saveEmptyMockReport(scanName);

      assertThatCode(
          () -> service.getReportEntity(APPLICATION_ID, scanName, name)
      ).doesNotThrowAnyException();
    });
  }

  @Test
  public void testGetAllReportEntities() throws Exception {
    helper.saveMockReport();
    helper.writeAdditionalFile("foo.txt", "foobar");
    helper.writeLocalFile("new-file.txt", "new file contents");
    helper.writeLocalFile("bom.json", "overwritten file contents");

    try (var stream = service.getAllReportEntities(APPLICATION_ID, SCAN_ID)) {
      // Note: can't use assertThat(stream) because it closes the stream before the assertions are run
      var entities = stream.toArray(ReportEntity[]::new);

      assertThat(entities).allMatch(wrapException(ReportEntity::exists)).satisfiesExactlyInAnyOrder(
          wrapException(entity -> {
            // overwritten bom.json should be present, not the original
            assertThat(entity.getName()).isEqualTo("bom.json");
            helper.assertEntityContents(entity, "overwritten file contents");
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
            helper.assertEntityContents(entity, "new file contents");
          }),
          wrapException(entity -> {
            // new file from additional files dir
            assertThat(entity.getName()).isEqualTo("foo.txt");
            helper.assertEntityContents(entity, "foobar");
          })
      );
    }
  }

  @Test
  public void testGetOriginalReportEntities() throws Exception {
    helper.saveMockReport();
    helper.writeAdditionalFile("foo.txt", "foobar");
    helper.writeLocalFile("new-file.txt", "new file contents");
    helper.writeLocalFile("bom.json", "overwritten file contents");

    try (var stream = service.getOriginalReportEntities(APPLICATION_ID, SCAN_ID)) {
      // Note: can't use assertThat(stream) because it closes the stream before the assertions are run
      var entities = stream.toArray(ReportEntity[]::new);

      assertThat(entities).allMatch(wrapException(ReportEntity::exists)).satisfiesExactlyInAnyOrder(
          wrapException(entity -> {
            // original bom.json should be present
            assertThat(entity.getName()).isEqualTo("bom.json");
            helper.assertEntityContents(entity, "{}\n");
          }),
          wrapException(entity -> {
            // original index.html should be present since it's not overwritten (note index.html is created by
            // ReportHelper.saveMockReport, it's not in the src/test/resources/… dir)
            assertThat(entity.getName()).isEqualTo("index.html");
            helper.assertEntityContents(entity, "<html></html>");
          })
      );
    }
  }

  @Test
  public void testSaveOriginalReport() throws Exception {
    try (var zipStream = getClass().getResourceAsStream("/ApplicationReportPersistenceServiceTest/report.zip")) {
      service.saveOriginalReport(APPLICATION_ID, SCAN_ID, zipStream);
    }

    assertThat(helper.readFromOriginalFiles("bom.json")).isEqualTo("{}\n");
    assertThat(helper.readFromLocalFiles("bom.json")).isNull();
    assertThat(helper.readFromAdditionalFiles("bom.json")).isNull();
  }

  @Test
  public void testSaveOriginalReport_alreadyExists() throws Exception {
    helper.saveEmptyMockReport();
    try (var zipStream = getClass().getResourceAsStream("/ApplicationReportPersistenceServiceTest/report.zip")) {
      assertThatThrownBy(() -> service.saveOriginalReport(APPLICATION_ID, SCAN_ID, zipStream))
          .isInstanceOf(IOException.class);
    }
  }

  @Test
  public void testSaveOriginalReport_cleansUpOnFailure() throws IOException {
    var service = mockForSaveOriginalReport_cleansUpOnFailure();

    try (var zipStream = getClass().getResourceAsStream("/ApplicationReportPersistenceServiceTest/report.zip")) {
      assertThatThrownBy(() -> service.saveOriginalReport(APPLICATION_ID, SCAN_ID, zipStream))
          .isInstanceOf(IOException.class);
    }

    assertThat(helper.readFromOriginalFiles("bom.json")).isNull();
    assertThat(helper.readFromOriginalFiles("index.html")).isNull();
  }

  @Test
  public void testMoveReport_targetReportIsOverwritten() throws Exception {
    final String reEvalScanId = "scan2";

    helper.saveMockReport();
    helper.saveEmptyMockReport(reEvalScanId);

    service.moveReport(APPLICATION_ID, reEvalScanId, SCAN_ID);

    assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isTrue();
    // New report content is added
    assertThat(helper.readFromOriginalFiles("index.html")).isEqualTo("<html></html>");
    // Old report content is gone
    assertThat(helper.readFromOriginalFiles("bom.json")).isNull();
    // Report with temp scan ID is removed.
    assertThat(service.reportExists(APPLICATION_ID, reEvalScanId)).isFalse();
  }

  /**
   * Subclasses should implement this with something that causes an exception during (but not at the start of) the
   * saving of the original report. For instance, of the files within the zip are saved separately in the service under
   * test, this might fail to save the second file (index.html) but not the first.
   *
   * @return the service instance to test, which might be different from this.service in order to facilitate dependency
   * mocking
   */
  protected abstract ApplicationReportPersistenceService mockForSaveOriginalReport_cleansUpOnFailure()
      throws IOException;

  @Test
  @Category(SlowTest.class)
  public void testSaveReportFile_newFile_notInZip() throws Exception {
    helper.saveEmptyMockReport();
    Instant now = Instant.now();
    helper.waitForNewFileTime();

    service.saveReportFile(APPLICATION_ID, SCAN_ID, "new-file.txt",
        new ByteArrayInputStream("new file contents".getBytes(StandardCharsets.UTF_8)));

    assertThat(helper.readFromLocalFiles("new-file.txt")).isEqualTo("new file contents");
    assertThat(helper.readFromOriginalFiles("new-file.txt")).isNull();
    assertThat(helper.readFromAdditionalFiles("new-file.txt")).isNull();

    ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "new-file.txt");
    assertThat(entity.exists()).isTrue();
    assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
    helper.assertEntityContents(entity, "new file contents");
  }

  @Test
  @Category(SlowTest.class)
  public void testSaveReportFile_updateFile_notInZip() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeLocalFile("file.txt", "old file contents");
    Instant now = Instant.now();
    helper.waitForNewFileTime();

    service.saveReportFile(APPLICATION_ID, SCAN_ID, "file.txt",
        new ByteArrayInputStream("new file contents".getBytes(StandardCharsets.UTF_8)));

    assertThat(helper.readFromLocalFiles("file.txt")).isEqualTo("new file contents");
    assertThat(helper.readFromOriginalFiles("file.txt")).isNull();
    assertThat(helper.readFromAdditionalFiles("file.txt")).isNull();

    ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "file.txt");
    assertThat(entity.exists()).isTrue();
    assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
    helper.assertEntityContents(entity, "new file contents");
  }

  @Test
  @Category(SlowTest.class)
  public void testSaveReportFile_updateFile_inZip() throws Exception {
    helper.saveMockReport();
    Instant now = Instant.now();
    helper.waitForNewFileTime();

    service.saveReportFile(APPLICATION_ID, SCAN_ID, "bom.json",
        new ByteArrayInputStream("new file contents".getBytes(StandardCharsets.UTF_8)));

    assertThat(helper.readFromLocalFiles("bom.json")).isEqualTo("new file contents");
    assertThat(helper.readFromOriginalFiles("bom.json")).isEqualTo("{}\n");
    assertThat(helper.readFromAdditionalFiles("bom.json")).isNull();

    ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "bom.json");
    assertThat(entity.exists()).isTrue();
    assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
    helper.assertEntityContents(entity, "new file contents");
  }

  @Test
  @Category(SlowTest.class)
  public void testSaveReportFile_updateFile_inZipAndLocalCache() throws Exception {
    helper.saveMockReport();
    helper.writeLocalFile("bom.json", "old local file contents");
    Instant now = Instant.now();
    helper.waitForNewFileTime();

    service.saveReportFile(APPLICATION_ID, SCAN_ID, "bom.json",
        new ByteArrayInputStream("new file contents".getBytes(StandardCharsets.UTF_8)));

    assertThat(helper.readFromLocalFiles("bom.json")).isEqualTo("new file contents");
    assertThat(helper.readFromOriginalFiles("bom.json")).isEqualTo("{}\n");
    assertThat(helper.readFromAdditionalFiles("bom.json")).isNull();

    ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "bom.json");
    assertThat(entity.exists()).isTrue();
    assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
    helper.assertEntityContents(entity, "new file contents");
  }

  @Test
  public void testSaveReportFile_invalidNames() throws Exception {
    AtomicInteger count = new AtomicInteger(0);

    assertThat(BAD_NAMES).allSatisfy(name -> {
      String scanName = "scan" + count.getAndIncrement();
      helper.saveEmptyMockReport(scanName);

      assertThatThrownBy(
          () -> service.saveReportFile(APPLICATION_ID, scanName, name, InputStream.nullInputStream())
      ).isInstanceOf(IllegalArgumentException.class);
    });

    assertThat(GOOD_NAMES).allSatisfy(name -> {
      String scanName = "scan" + count.getAndIncrement();
      helper.saveEmptyMockReport(scanName);

      assertThatCode(
          () -> service.saveReportFile(APPLICATION_ID, scanName, name, InputStream.nullInputStream())
      ).doesNotThrowAnyException();
    });
  }

  @Test
  @Category(SlowTest.class)
  public void testSaveAdditionalReportFile_newFile() throws Exception {
    helper.saveEmptyMockReport();
    Instant now = Instant.now();
    helper.waitForNewFileTime();

    service.saveAdditionalReportFile(APPLICATION_ID, SCAN_ID, "new-file.txt",
        new ByteArrayInputStream("new file contents".getBytes(StandardCharsets.UTF_8)));

    assertThat(helper.readFromLocalFiles("new-file.txt")).isNull();
    assertThat(helper.readFromOriginalFiles("new-file.txt")).isNull();
    assertThat(helper.readFromAdditionalFiles("new-file.txt")).isEqualTo("new file contents");

    ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "new-file.txt");
    assertThat(entity.exists()).isTrue();
    assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
    helper.assertEntityContents(entity, "new file contents");
  }

  @Test
  @Category(SlowTest.class)
  public void testSaveAdditionalReportFile_updateFile() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("file.txt", "old file contents");
    Instant now = Instant.now();
    helper.waitForNewFileTime();

    service.saveAdditionalReportFile(APPLICATION_ID, SCAN_ID, "txt",
        new ByteArrayInputStream("new file contents".getBytes(StandardCharsets.UTF_8)));

    assertThat(helper.readFromLocalFiles("txt")).isNull();
    assertThat(helper.readFromOriginalFiles("txt")).isNull();
    assertThat(helper.readFromAdditionalFiles("txt")).isEqualTo("new file contents");

    ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "txt");
    assertThat(entity.exists()).isTrue();
    assertThat(entity.getTime()).isGreaterThan(now.toEpochMilli());
    helper.assertEntityContents(entity, "new file contents");
  }

  @Test
  public void testSaveAdditionalReportFile_invalidNames() throws Exception {
    AtomicInteger count = new AtomicInteger(0);

    assertThat(BAD_NAMES).allSatisfy(name -> {
      String scanName = "scan" + count.getAndIncrement();
      helper.saveEmptyMockReport(scanName);

      assertThatThrownBy(
          () -> service.saveAdditionalReportFile(APPLICATION_ID, scanName, name, InputStream.nullInputStream())
      ).isInstanceOf(IllegalArgumentException.class);
    });

    assertThat(GOOD_NAMES).allSatisfy(name -> {
      String scanName = "scan" + count.getAndIncrement();
      helper.saveEmptyMockReport(scanName);

      assertThatCode(
          () -> service.saveAdditionalReportFile(APPLICATION_ID, scanName, name, InputStream.nullInputStream())
      ).doesNotThrowAnyException();
    });
  }

  @Test
  @Category(SlowTest.class)
  public void testGetPdfEntity() throws Exception {
    helper.saveEmptyMockReport();
    helper.writePdf("PDF");

    var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);
    var oldTime = entity.getTime();
    assertThat(entity.exists()).isTrue();
    assertThat(entity.length()).isEqualTo(3);
    assertThat(entity.canCreate()).isFalse();
    assertThat(oldTime).isGreaterThan(0);
    assertThat(oldTime).isLessThanOrEqualTo(System.currentTimeMillis());

    helper.assertEntityContents(entity, "PDF");

    helper.waitForNewFileTime();

    String newContents = "PDF2";
    try (var outputStream = entity.getOutputStream()) {
      outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
    }

    assertThat(entity.exists()).isTrue();
    assertThat(entity.length()).isEqualTo(4);
    assertThat(entity.canCreate()).isFalse();
    assertThat(entity.getTime()).isGreaterThan(oldTime);
    assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

    helper.assertEntityContents(entity, newContents);
  }

  @Test
  public void testGetPdfEntity_notExists() throws Exception {
    helper.saveEmptyMockReport();

    var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);
    assertThat(entity.exists()).isFalse();
    assertThat(entity.canCreate()).isTrue();
    assertThatThrownBy(entity::length).isInstanceOf(IOException.class);
    assertThatThrownBy(entity::getTime).isInstanceOf(IOException.class);
    assertThatThrownBy(entity::getInputStream).isInstanceOf(IOException.class);

    String newContents = "PDF2";
    try (var outputStream = entity.getOutputStream()) {
      outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
    }

    assertThat(entity.exists()).isTrue();
    assertThat(entity.canCreate()).isFalse();
    assertThat(entity.getTime()).isGreaterThan(0);

    helper.assertEntityContents(entity, newContents);
  }

  @Test
  @Category(SlowTest.class)
  public void testGetPdfEntity_currentlyEmpty() throws Exception {
    helper.saveEmptyMockReport();
    helper.writePdf("");

    var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);
    var oldTime = entity.getTime();
    assertThat(entity.exists()).isTrue();
    assertThat(entity.canCreate()).isTrue();
    assertThat(entity.length()).isEqualTo(0);
    assertThat(oldTime).isGreaterThan(0);

    helper.assertEntityContents(entity, "");

    helper.waitForNewFileTime();

    String newContents = "PDF2";
    try (var outputStream = entity.getOutputStream()) {
      outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
    }

    assertThat(entity.exists()).isTrue();
    assertThat(entity.canCreate()).isFalse();
    assertThat(entity.getTime()).isGreaterThan(oldTime);
    assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

    helper.assertEntityContents(entity, newContents);
  }

  @Test
  public void testGetPdfEntity_deleteIfExists() throws Exception {
    helper.saveEmptyMockReport();
    helper.writePdf("PDF");

    var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);
    assertThat(entity.exists()).isTrue();

    entity.deleteIfExists();

    assertThat(entity.exists()).isFalse();
    assertThat(helper.readPdf()).isNull();
  }

  @Test
  public void testGetPdfEntity_deleteIfExists_notExists() throws Exception {
    helper.saveEmptyMockReport();

    var entity = service.getPdfEntity(APPLICATION_ID, SCAN_ID);

    assertThatCode(() -> entity.deleteIfExists()).doesNotThrowAnyException();

    assertThat(entity.exists()).isFalse();
    assertThat(helper.readPdf()).isNull();
  }

  @Test
  @Category(SlowTest.class)
  public void testGetVulnerabilitySignaturesEntity() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeVulnerabilitySignatures("{}");

    var entity = service.getVulnerabilitySignaturesEntity(APPLICATION_ID, SCAN_ID);
    var oldTime = entity.getTime();
    assertThat(entity.exists()).isTrue();
    assertThat(oldTime).isGreaterThan(0);
    assertThat(oldTime).isLessThanOrEqualTo(System.currentTimeMillis());

    helper.assertEntityContents(entity, "{}");

    helper.waitForNewFileTime();

    String newContents = "[]";
    try (var outputStream = entity.getOutputStream()) {
      outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
    }

    assertThat(entity.exists()).isTrue();
    assertThat(entity.getTime()).isGreaterThan(oldTime);
    assertThat(entity.getTime()).isLessThanOrEqualTo(System.currentTimeMillis());

    helper.assertEntityContents(entity, newContents);
  }

  @Test
  public void testGetVulnerabilitySignaturesEntity_notExists() throws Exception {
    helper.saveEmptyMockReport();

    var entity = service.getVulnerabilitySignaturesEntity(APPLICATION_ID, SCAN_ID);
    assertThat(entity.exists()).isFalse();
    assertThatThrownBy(entity::getTime).isInstanceOf(IOException.class);
    assertThatThrownBy(entity::getInputStream).isInstanceOf(IOException.class);

    String newContents = "[]";
    try (var outputStream = entity.getOutputStream()) {
      outputStream.write(newContents.getBytes(StandardCharsets.UTF_8));
    }

    assertThat(entity.exists()).isTrue();
    assertThat(entity.getTime()).isGreaterThan(0);

    helper.assertEntityContents(entity, newContents);
  }

  @Test
  public void testGetReportLocation() {
    // The output of this API will differ by OS depending on the path separator character
    String suffix = FileSystems.getDefault().getSeparator().equals("\\") ?
         "\\report\\app1\\scan1" :
         "/report/app1/scan1";

    assertThat(service.getReportLocation(APPLICATION_ID, SCAN_ID)).isEqualTo(
        insightConfig.getClusterDirectory().toString() + suffix
    );
  }

  @Test
  public void testReportExists() throws Exception {
    assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isFalse();

    helper.saveEmptyMockReport();

    assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isTrue();
  }

  @Test
  public void testReportExists_NoFilesExists() throws Exception {
    assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isFalse();
  }

  @Test
  public void testReportExists_OnlyCopyMarkerExists() throws Exception {
    service.saveAdditionalReportFile(APPLICATION_ID, SCAN_ID, CopyStorageService.COPY_MARKER,
        new ByteArrayInputStream(new byte[] {0}));

    assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isFalse();
  }

  @Test
  public void testReportExists_OnlyReportFilesExist() throws Exception {
    helper.saveEmptyMockReport();

    assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isTrue();
  }

  @Test
  public void testReportExists_CopyMarkerAndReportFilesExist() throws Exception {
    helper.saveEmptyMockReport();
    service.saveAdditionalReportFile(APPLICATION_ID, SCAN_ID, CopyStorageService.COPY_MARKER,
        new ByteArrayInputStream(new byte[] {0}));

    assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isFalse();
  }

  @Test
  public void testReportExists_CopyMarkerDeleted() throws Exception {
    service.saveAdditionalReportFile(APPLICATION_ID, SCAN_ID, CopyStorageService.COPY_MARKER,
        new ByteArrayInputStream(new byte[] {0}));
    ReportEntity copyMarker = service.getReportEntity(APPLICATION_ID, SCAN_ID, CopyStorageService.COPY_MARKER);
    service.deleteReportEntity(copyMarker);

    assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isFalse();
  }

  @Test
  public void testDeleteReport() throws Exception {
    helper.saveEmptyMockReport();

    service.deleteReport(APPLICATION_ID, SCAN_ID);

    assertThat(helper.readFromOriginalFiles("index.html")).isNull();
    assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isFalse();
  }

  @Test
  public void testDeleteReports() throws Exception {
    helper.saveEmptyMockReport(SCAN_ID);
    helper.saveEmptyMockReport("scan2");

    service.deleteReports(APPLICATION_ID);

    assertThat(helper.readFromOriginalFiles("index.html")).isNull();
    assertThat(helper.readFromOriginalFiles(APPLICATION_ID, "scan2", "index.html")).isNull();
    assertThat(service.reportExists(APPLICATION_ID, SCAN_ID)).isFalse();
    assertThat(service.reportExists(APPLICATION_ID, "scan2")).isFalse();
  }

  @Test
  public void testGetReportEntity_Unknown_ChecksAdditional() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("some_unknown_file", "foobar");

    var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "some_unknown_file");
    assertThat(entity.exists()).isTrue();
    helper.assertEntityContents(entity, "foobar");

    assertThat(helper.readFromLocalFiles("some_unknown_file")).isNull();
    assertThat(helper.readFromOriginalFiles("some_unknown_file")).isNull();
    assertThat(helper.readFromAdditionalFiles("some_unknown_file")).isEqualTo("foobar");
  }

  @Test
  public void testGetReportEntity_KnownAndAdditional_ChecksAdditional() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("thirdparty-bom.json", "foobar");

    var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "thirdparty-bom.json");
    assertThat(entity.exists()).isTrue();
    helper.assertEntityContents(entity, "foobar");

    assertThat(helper.readFromLocalFiles("thirdparty-bom.json")).isNull();
    assertThat(helper.readFromOriginalFiles("thirdparty-bom.json")).isNull();
    assertThat(helper.readFromAdditionalFiles("thirdparty-bom.json")).isEqualTo("foobar");
  }

  @Test
  public void testGetReportEntity_KnownAndNotAdditional_DoesNotCheckAdditional() throws Exception {
    helper.saveEmptyMockReport();
    // Write an additional file that should not normally exist there and so shouldn't be read
    helper.writeAdditionalFile("bom.json", "foobar");

    var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "bom.json");
    assertThat(entity.exists()).isFalse();

    assertThat(helper.readFromLocalFiles("bom.json")).isNull();
    assertThat(helper.readFromOriginalFiles("bom.json")).isNull();
    assertThat(helper.readFromAdditionalFiles("bom.json")).isEqualTo("foobar");
  }

  @Test
  public void testDeleteReportEntity() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("some_unknown_file", "foobar");
    ReportEntity reportEntity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "some_unknown_file");
    assertThat(reportEntity.exists()).isTrue();

    service.deleteReportEntity(reportEntity);

    assertThat(reportEntity.exists()).isFalse();
  }

  @Test
  public void testGetMetadata() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("additional.txt", "additional");
    helper.writeLocalFile("local.txt", "local");
    helper.writePdf("pdf");
    helper.writeVulnerabilitySignatures("vulnerability signatures");

    assertThat(service.getReportEntity(APPLICATION_ID, SCAN_ID, "doesNotExist").getMetadata()).isNull();
    assertMetadataEqualsDirectCalls(service.getReportEntity(APPLICATION_ID, SCAN_ID, "index.html"));
    assertMetadataEqualsDirectCalls(service.getReportEntity(APPLICATION_ID, SCAN_ID, "additional.txt"));
    assertMetadataEqualsDirectCalls(service.getReportEntity(APPLICATION_ID, SCAN_ID, "local.txt"));
    assertMetadataEqualsDirectCalls(service.getPdfEntity(APPLICATION_ID, SCAN_ID));
    assertMetadataEqualsDirectCalls(service.getVulnerabilitySignaturesEntity(APPLICATION_ID, SCAN_ID));
    try (Stream<ReportEntity> reportEntitiesStream = service.getOriginalReportEntities(APPLICATION_ID, SCAN_ID)) {
      ReportEntity[] reportEntities = reportEntitiesStream.toArray(ReportEntity[]::new);
      for (ReportEntity reportEntity : reportEntities) {
        assertMetadataEqualsDirectCalls(reportEntity);
      }
    }
  }

  private void assertMetadataEqualsDirectCalls(final BaseReportEntity reportEntity) throws Exception {
    Metadata metadata = reportEntity.getMetadata(
        MetadataAttribute.LAST_MODIFIED_EPOCH_TIME,
        MetadataAttribute.SIZE_IN_BYTES
    );
    assertThat(metadata.lastModifiedEpochTime()).isEqualTo(reportEntity.getTime());
    assertThat(metadata.sizeInBytes()).isEqualTo(reportEntity.length());
  }
}
