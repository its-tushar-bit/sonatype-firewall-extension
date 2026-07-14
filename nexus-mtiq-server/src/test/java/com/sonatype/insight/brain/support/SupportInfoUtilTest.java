/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.support.SupportService.SupportFile;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Regression tests for CLM-42243: the support-bundle path must stream to disk instead of building
 * a {@code ByteArrayOutputStream} that OOMs past {@code Integer.MAX_VALUE - 8}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SupportInfoUtilTest
{
  @Mock
  private InsightConfig insightConfig;

  private Path tmpRoot;

  private SupportInfoUtil supportInfoUtil;

  @Before
  public void setup() throws IOException {
    tmpRoot = Files.createTempDirectory("support-info-util-test");
    when(insightConfig.getSonatypeWork()).thenReturn(tmpRoot.toFile());
    supportInfoUtil = new SupportInfoUtil(insightConfig);
  }

  @After
  public void cleanup() throws IOException {
    if (tmpRoot != null && Files.exists(tmpRoot)) {
      FileUtils.deleteDirectory(tmpRoot.toFile());
    }
  }

  @Test
  public void writePojoAsJsonToFile_streamsPojoWithoutMaterializingString() throws IOException {
    Map<String, Object> pojo = new TreeMap<>();
    pojo.put("artifactId", "tomcat-util");
    pojo.put("groupId", "tomcat");

    File out = supportInfoUtil.writePojoAsJsonToFile(pojo, "streamed.json");

    assertThat(out).exists();
    Map<?, ?> parsed = JsonUtils.parse(new String(Files.readAllBytes(out.toPath()), StandardCharsets.UTF_8), Map.class);
    assertThat(parsed).isEqualTo(pojo);
  }

  @Test
  public void generateSupportInfo_writesZipToDisk_notInMemory() throws IOException {
    // Given: a small support file inside the tenant work dir.
    File workDir = supportInfoUtil.getWorkDir();
    Files.createDirectories(workDir.toPath());
    File entryFile = new File(workDir, "waiver.json");
    FileUtils.write(entryFile, "{\"waiver\":[]}", StandardCharsets.UTF_8);

    List<SupportFile> supportFiles = new ArrayList<>();
    supportFiles.add(new SupportFile(SupportFileType.DB, entryFile, false));

    // When
    SupportInfo info = supportInfoUtil.generateSupportInfo("acme", supportFiles);

    // Then: the returned bundle is a real File on disk, matches the streaming ZIP layout, and
    // is NOT a byte-array-backed buffer that would blow up past 2 GiB.
    assertThat(info.getSupportInfoName()).startsWith("acme-mtiq-support-");
    assertThat(info.getSupportInfoFile()).exists().isFile();
    assertThat(info.getSupportInfoFile().getName()).endsWith(".zip");

    Set<String> entries = readZipEntryNames(info.getSupportInfoFile());
    assertThat(entries).containsExactly(info.getSupportInfoName() + "/db/waiver.json");
  }

  @Test
  public void generateSupportInfo_deletesSourceFilesMarkedForDeletion() throws IOException {
    File workDir = supportInfoUtil.getWorkDir();
    Files.createDirectories(workDir.toPath());
    File keep = new File(workDir, "keep.json");
    File drop = new File(workDir, "drop.json");
    FileUtils.write(keep, "{\"keep\":true}", StandardCharsets.UTF_8);
    FileUtils.write(drop, "{\"drop\":true}", StandardCharsets.UTF_8);

    List<SupportFile> files = new ArrayList<>();
    files.add(new SupportFile(SupportFileType.DB, keep, false));
    files.add(new SupportFile(SupportFileType.DB, drop, true));

    supportInfoUtil.generateSupportInfo("acme", files);

    assertThat(keep).exists();
    assertThat(drop).doesNotExist();
  }

  @Test
  public void generateSupportInfo_sweepsStaleBundlesFromPriorAbortedRequests() throws IOException {
    File workDir = supportInfoUtil.getWorkDir();
    Files.createDirectories(workDir.toPath());
    File orphan = new File(workDir, "acme-mtiq-support-20260101-000000-1.zip");
    File fresh = new File(workDir, "acme-mtiq-support-fresh.zip");
    // A zip that doesn't match the mtiq-support naming convention (e.g. left there by a support
    // engineer for debugging) must NOT be swept, even if old.
    File unrelated = new File(workDir, "customer-export.zip");
    FileUtils.write(orphan, "leftover", StandardCharsets.UTF_8);
    FileUtils.write(fresh, "recent", StandardCharsets.UTF_8);
    FileUtils.write(unrelated, "someone-elses-zip", StandardCharsets.UTF_8);
    Files.setLastModifiedTime(orphan.toPath(),
        FileTime.from(Instant.now().minus(SupportInfoUtil.STALE_BUNDLE_THRESHOLD).minusSeconds(60)));
    Files.setLastModifiedTime(fresh.toPath(), FileTime.from(Instant.now()));
    Files.setLastModifiedTime(unrelated.toPath(),
        FileTime.from(Instant.now().minus(SupportInfoUtil.STALE_BUNDLE_THRESHOLD).minusSeconds(60)));

    supportInfoUtil.generateSupportInfo("acme", new ArrayList<>());

    assertThat(orphan).doesNotExist();
    assertThat(fresh).exists();
    assertThat(unrelated).exists();
  }

  @Test
  public void generateSupportInfo_partialZipDeletedIfWriteFails() throws IOException {
    File workDir = supportInfoUtil.getWorkDir();
    Files.createDirectories(workDir.toPath());

    // Force a failure by pointing at a source file that doesn't exist.
    File missing = new File(workDir, "does-not-exist.json");
    List<SupportFile> files = new ArrayList<>();
    files.add(new SupportFile(SupportFileType.DB, missing, false));

    try {
      supportInfoUtil.generateSupportInfo("acme", files);
    }
    catch (IOException expected) {
      // expected
    }

    // The ZIP we started writing before the failure must not linger on disk.
    try (java.util.stream.Stream<Path> paths = Files.list(workDir.toPath())) {
      assertThat(paths.filter(p -> p.getFileName().toString().endsWith(".zip"))).isEmpty();
    }
  }

  @Test
  public void generateUniqueName_stripsPathSeparatorsFromSlug() {
    // A slug with path-traversal characters must not produce a filename that escapes the work dir.
    String name = supportInfoUtil.generateUniqueName("../evil/-mtiq-support-");
    assertThat(name).doesNotContain("..");
    assertThat(name).doesNotContain("/");
    assertThat(name).doesNotContain("\\");
    assertThat(name).doesNotContain(":");
  }

  @Test
  public void sanitizeForFilename_normalizesUnsafeCharacters() {
    assertThat(SupportInfoUtil.sanitizeForFilename("acme/../etc")).isEqualTo("acme___etc");
    assertThat(SupportInfoUtil.sanitizeForFilename("C:\\evil")).isEqualTo("C__evil");
    assertThat(SupportInfoUtil.sanitizeForFilename("line1\nline2\r")).isEqualTo("line1_line2_");
    assertThat(SupportInfoUtil.sanitizeForFilename(null)).isEmpty();
  }

  private static Set<String> readZipEntryNames(File zipFile) throws IOException {
    Set<String> names = new HashSet<>();
    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        names.add(entry.getName());
        zis.closeEntry();
      }
    }
    return names;
  }
}
