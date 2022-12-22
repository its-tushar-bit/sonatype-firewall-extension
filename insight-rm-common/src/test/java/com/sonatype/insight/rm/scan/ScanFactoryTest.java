/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.scan;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.scan.model.Repository;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.ScanItemProvider;
import com.sonatype.insight.scan.model.ScanSummary;
import com.sonatype.insight.scan.model.io.ScanReader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanFactoryTest
{
  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  private ScanConfiguration newConfig() throws Exception {
    ScanConfiguration config = new ScanConfiguration();
    config.setWorkDir(tmpDir.newFolder());
    config.setRepository("staging-001", "maven2", "CLM 1.0");
    return config;
  }

  private File newScan(ScanConfiguration config) throws Exception {
    return new ScanFactory().forConfiguration(config);
  }

  private Scan parse(File scanFile) throws Exception {
    return new ScanReader(LoggerFactory.getLogger("ScanReader")).read(scanFile);
  }

  private void assertRepository(Repository repo) {
    assertThat(repo).isNotNull();
    assertThat(repo.getId()).isEqualTo("staging-001");
    assertThat(repo.getFormat()).isEqualTo("maven2");
    assertThat(repo.getName()).isEqualTo("CLM 1.0");
  }

  private void assertSummary(int archives, int files, ScanSummary summary) {
    assertThat(summary).isNotNull();
    assertThat(summary.getStartTime()).isNotNull();
    assertThat(summary.getEndTime()).isNotNull();
    assertThat(summary.getArchives()).isEqualTo(archives);
    assertThat(summary.getFiles()).isEqualTo(files);
  }

  private void assertIds(ScanItemProvider items, String... ids) {
    assertThat(items.getItems()).extracting(ScanItem::getId).containsExactlyInAnyOrder(ids);
  }

  private void assertPaths(ScanItemProvider items, String... paths) {
    assertThat(items.getItems()).extracting(ScanItem::getPath).containsOnly(paths);
  }

  @Test
  public void testScan_AllRelevantComponentsCoveredByBuildTool() throws Exception {
    ScanConfiguration config = newConfig();
    TestRepositoryItem.add(config, new File("src/test/repos/a"));
    File scanFile = newScan(config);
    assertThat(scanFile).isFile();
    Scan scan = parse(scanFile);
    assertRepository(scan.getRepository());
    assertIds(scan, "com.sonatype.clm.its:mod-a:jar:1.0");
    assertIds(scan.getItems().get(0), "org.codehaus.plexus:plexus-utils:jar:3.0",
        "org.apache.maven:maven-settings:jar:3.0", "com.sonatype.clm.its:mod-a:jar:1.0");
    assertSummary(3, 131, scan.getSummary());
  }

  @Test
  public void testScan_SomeModuleOutputsNotCoveredByCorrespondingBuildToolScan() throws Exception {
    ScanConfiguration config = newConfig();
    TestRepositoryItem.add(config, new File("src/test/repos/b"));
    File scanFile = newScan(config);
    assertThat(scanFile).isFile();
    Scan scan = parse(scanFile);
    assertRepository(scan.getRepository());
    assertIds(scan, "com.sonatype.clm.its:mod-a:jar:1.0", "com.sonatype.clm.its:mod-a:zip:dist:1.0");
    assertIds(scan.getItems().get(0), "org.codehaus.plexus:plexus-utils:jar:3.0",
        "org.apache.maven:maven-settings:jar:3.0", "com.sonatype.clm.its:mod-a:jar:1.0");
    assertPaths(scan.getItems().get(1), "mod-a-1.0.jar", "mod-a-1.0-javadoc.jar", "mod-a-1.0-sources.jar");
    // 131 items comes from the gzip scan file + 1 other unique archive = 132 (not 134 as one might expect)
    // There are 3 other archives total, but they all have the same hashes, so only 1 file is counted.
    assertSummary(7, 132, scan.getSummary());
  }

  @Test
  public void testScan_SomeModuleOutputsDeletedSinceBuildToolScan() throws Exception {
    ScanConfiguration config = newConfig();
    TestRepositoryItem.add(config, new File("src/test/repos/c"));
    File scanFile = newScan(config);
    assertThat(scanFile).isFile();
    Scan scan = parse(scanFile);
    assertRepository(scan.getRepository());
    assertIds(scan, "com.sonatype.clm.its:mod-a:jar:1.0");
    assertIds(scan.getItems().get(0), "org.codehaus.plexus:plexus-utils:jar:3.0",
        "org.apache.maven:maven-settings:jar:3.0", "com.sonatype.clm.its:mod-a:jar:cli:1.0");
    assertSummary(3, 131, scan.getSummary());
  }

  @Test
  public void testScan_AllModuleOutputsFromOriginalBuildToolScanGoneButNewOutputsStaged() throws Exception {
    ScanConfiguration config = newConfig();
    TestRepositoryItem.add(config, new File("src/test/repos/d"));
    File scanFile = newScan(config);
    assertThat(scanFile).isFile();
    Scan scan = parse(scanFile);
    assertRepository(scan.getRepository());
    assertIds(scan, "com.sonatype.clm.its:mod-a:jar:1.0", "com.sonatype.clm.its:mod-a:zip:dist:1.0");
    assertIds(scan.getItems().get(0), "org.codehaus.plexus:plexus-utils:jar:3.0",
        "org.apache.maven:maven-settings:jar:3.0");
    // 130 items comes from the gzip scan file + 1 other unique archive = 131 (not 133 as one might expect)
    // There are 3 other archives total, but they all have the same hashes, so only 1 file is counted.
    assertSummary(6, 131, scan.getSummary());
  }

  @Test
  public void testScan_AllModuleOutputsFromOriginalBuildToolScanGone() throws Exception {
    ScanConfiguration config = newConfig();
    TestRepositoryItem.add(config, new File("src/test/repos/e"));
    File scanFile = newScan(config);
    assertThat(scanFile).isFile();
    Scan scan = parse(scanFile);
    assertRepository(scan.getRepository());
    assertIds(scan);
    assertSummary(0, 0, scan.getSummary());
  }

  @Test
  public void testScan_MultipleBuildToolScans() throws Exception {
    ScanConfiguration config = newConfig();
    TestRepositoryItem.add(config, new File("src/test/repos/f"));
    File scanFile = newScan(config);
    assertThat(scanFile).isFile();
    Scan scan = parse(scanFile);
    assertRepository(scan.getRepository());
    assertIds(scan, "com.sonatype.clm.its:mod-a:jar:1.0", "com.sonatype.clm.its:mod-a:jar:1.1");
    assertIds(scan.getItems().get(0), "org.codehaus.plexus:plexus-utils:jar:3.0",
        "org.apache.maven:maven-settings:jar:3.0", "com.sonatype.clm.its:mod-a:jar:1.0");
    assertIds(scan.getItems().get(1), "org.codehaus.plexus:plexus-utils:jar:3.0",
        "org.apache.maven:maven-settings:jar:3.0", "com.sonatype.clm.its:mod-a:jar:1.1");
    assertSummary(6, 262, scan.getSummary());
  }

  @Test
  public void testScan_NoBuildToolScans() throws Exception {
    ScanConfiguration config = newConfig();
    TestRepositoryItem.add(config, new File("src/test/repos/g"));
    File scanFile = newScan(config);
    assertThat(scanFile).isFile();
    Scan scan = parse(scanFile);
    assertRepository(scan.getRepository());
    assertIds(scan, "com.sonatype.clm.its:mod-a:jar:1.0", "com.sonatype.clm.its:mod-a:jar:1.1");
    assertPaths(scan, "com/sonatype/clm/its/mod-a/1.0/mod-a-1.0.jar", "com/sonatype/clm/its/mod-a/1.1/mod-a-1.1.jar");
    assertPaths(scan.getItems().get(0), "META-INF/MANIFEST.MF");
    assertPaths(scan.getItems().get(1), "com/sonatype/insight/scan/model/ScanItem.class");
    ScanItem item = scan.getItems().get(1).getItems().get(0);
    assertThat(item.getSha1()).isNotNull();
    assertThat(item.getSha1JA001()).isNotNull();
    assertThat(item.getSha1JB001()).isNotNull();
    assertThat(item.getSha1JC001()).isNotNull();
    assertThat(item.getSha1JD001()).isNotNull();
    assertSummary(2, 2, scan.getSummary());
  }

  @Test
  public void testScan_Proprietary() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(Arrays.asList("com.sonatype", "org.sonatype"));
    assertRepoH(proprietaryConfig);
  }

  /**
   * Regex equivalent of testScan_Proprietary using package matching.
   * 
   * @since 1.11
   */
  @Test
  public void testScan_ProprietaryRegex() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setRegexes(Collections.singletonList(".*com.sonatype.*"));
    assertRepoH(proprietaryConfig);
  }

  private void assertRepoH(final ProprietaryConfig proprietaryConfig) throws Exception {
    ScanConfiguration config = newConfig();
    config.setProprietaryConfig(proprietaryConfig);
    TestRepositoryItem.add(config, new File("src/test/repos/h"));
    File scanFile = newScan(config);
    assertThat(scanFile).isFile();
    Scan scan = parse(scanFile);
    assertRepository(scan.getRepository());
    assertIds(scan, "com.sonatype.clm.its:mod-a:jar:1.0", "com.sonatype.clm.its:mod-a:jar:1.1");
    ScanItem projectItem = scan.getItems().get(0).getItems().get(0);
    assertThat(projectItem.getPath()).isEqualTo("target/mod-a-1.0.jar");
    assertPaths(projectItem, "META-INF/MANIFEST.MF", null);
    for (ScanItem item : projectItem.getItems()) {
      if (!"META-INF/MANIFEST.MF".equals(item.getPath())) {
        assertThat(item.getPath()).isNull();
        assertThat(item.getNoPathReason()).isEqualTo("proprietaryPackages");
      }
    }
    ScanItem repoItem = scan.getItems().get(1);
    assertThat(repoItem.getPath()).isEqualTo("com/sonatype/clm/its/mod-a/1.1/mod-a-1.1.jar");
    for (ScanItem item : repoItem.getItems()) {
      assertThat(item.getPath()).isNull();
      assertThat(item.getNoPathReason()).isEqualTo("proprietaryPackages");
    }
    assertSummary(2, 5, scan.getSummary());
  }
}
