/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.scan;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.scan.model.Repository;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.ScanItemProvider;
import com.sonatype.insight.scan.model.ScanSummary;
import com.sonatype.insight.scan.model.io.DefaultScanReader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    return new DefaultScanReader(LoggerFactory.getLogger("ScanReader")).read(scanFile);
  }

  private void assertRepository(Repository repo) {
    assertNotNull(repo);
    assertEquals("staging-001", repo.getId());
    assertEquals("maven2", repo.getFormat());
    assertEquals("CLM 1.0", repo.getName());
  }

  private void assertSummary(int archives, int files, ScanSummary summary) {
    assertNotNull(summary);
    assertNotNull(summary.getStartTime());
    assertNotNull(summary.getEndTime());
    assertEquals(archives, summary.getArchives());
    assertEquals(files, summary.getFiles());
  }

  private void assertIds(ScanItemProvider items, String... ids) {
    Set<String> expected = new TreeSet<String>(Arrays.asList(ids));
    Set<String> actual = new TreeSet<String>();
    for (ScanItem item : items.getItems()) {
      actual.add(String.valueOf(item.getId()));
    }
    assertEquals(expected, actual);
  }

  private void assertPaths(ScanItemProvider items, String... paths) {
    Set<String> expected = new TreeSet<String>(Arrays.asList(paths));
    Set<String> actual = new TreeSet<String>();
    for (ScanItem item : items.getItems()) {
      actual.add(String.valueOf(item.getPath()));
    }
    assertEquals(expected, actual);
  }

  @Test
  public void testScan_AllRelevantComponentsCoveredByBuildTool() throws Exception {
    ScanConfiguration config = newConfig();
    TestRepositoryItem.add(config, new File("src/test/repos/a"));
    File scanFile = newScan(config);
    assertNotNull(scanFile);
    assertTrue(scanFile.getAbsolutePath(), scanFile.isFile());
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
    assertNotNull(scanFile);
    assertTrue(scanFile.getAbsolutePath(), scanFile.isFile());
    Scan scan = parse(scanFile);
    assertRepository(scan.getRepository());
    assertIds(scan, "com.sonatype.clm.its:mod-a:jar:1.0", "com.sonatype.clm.its:mod-a:zip:dist:1.0");
    assertIds(scan.getItems().get(0), "org.codehaus.plexus:plexus-utils:jar:3.0",
        "org.apache.maven:maven-settings:jar:3.0", "com.sonatype.clm.its:mod-a:jar:1.0");
    assertPaths(scan.getItems().get(1), "mod-a-1.0.jar", "mod-a-1.0-javadoc.jar", "mod-a-1.0-sources.jar");
    assertSummary(7, 134, scan.getSummary());
  }

  @Test
  public void testScan_SomeModuleOutputsDeletedSinceBuildToolScan() throws Exception {
    ScanConfiguration config = newConfig();
    TestRepositoryItem.add(config, new File("src/test/repos/c"));
    File scanFile = newScan(config);
    assertNotNull(scanFile);
    assertTrue(scanFile.getAbsolutePath(), scanFile.isFile());
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
    assertNotNull(scanFile);
    assertTrue(scanFile.getAbsolutePath(), scanFile.isFile());
    Scan scan = parse(scanFile);
    assertRepository(scan.getRepository());
    assertIds(scan, "com.sonatype.clm.its:mod-a:jar:1.0", "com.sonatype.clm.its:mod-a:zip:dist:1.0");
    assertIds(scan.getItems().get(0), "org.codehaus.plexus:plexus-utils:jar:3.0",
        "org.apache.maven:maven-settings:jar:3.0");
    assertSummary(6, 133, scan.getSummary());
  }

  @Test
  public void testScan_AllModuleOutputsFromOriginalBuildToolScanGone() throws Exception {
    ScanConfiguration config = newConfig();
    TestRepositoryItem.add(config, new File("src/test/repos/e"));
    File scanFile = newScan(config);
    assertNotNull(scanFile);
    assertTrue(scanFile.getAbsolutePath(), scanFile.isFile());
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
    assertNotNull(scanFile);
    assertTrue(scanFile.getAbsolutePath(), scanFile.isFile());
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
    assertNotNull(scanFile);
    assertTrue(scanFile.getAbsolutePath(), scanFile.isFile());
    Scan scan = parse(scanFile);
    assertRepository(scan.getRepository());
    assertIds(scan, "com.sonatype.clm.its:mod-a:jar:1.0", "com.sonatype.clm.its:mod-a:jar:1.1");
    assertPaths(scan, "com/sonatype/clm/its/mod-a/1.0/mod-a-1.0.jar", "com/sonatype/clm/its/mod-a/1.1/mod-a-1.1.jar");
    assertPaths(scan.getItems().get(0), "META-INF/MANIFEST.MF");
    assertPaths(scan.getItems().get(1), "com/sonatype/insight/scan/model/ScanItem.class");
    ScanItem item = scan.getItems().get(1).getItems().get(0);
    assertNotNull(item.getSha1());
    assertNotNull(item.getSha1JA001());
    assertNotNull(item.getSha1JB001());
    assertNotNull(item.getSha1JC001());
    assertNotNull(item.getSha1JD001());
    assertSummary(2, 2, scan.getSummary());
  }

  @Test
  public void testScan_Proprietary() throws Exception {
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig();
    proprietaryConfig.setPackages(Arrays.asList("com.sonatype", "org.sonatype"));
    ScanConfiguration config = newConfig();
    config.setProprietaryConfig(proprietaryConfig);
    TestRepositoryItem.add(config, new File("src/test/repos/h"));
    File scanFile = newScan(config);
    assertNotNull(scanFile);
    assertTrue(scanFile.getAbsolutePath(), scanFile.isFile());
    Scan scan = parse(scanFile);
    assertRepository(scan.getRepository());
    assertIds(scan, "com.sonatype.clm.its:mod-a:jar:1.0", "com.sonatype.clm.its:mod-a:jar:1.1");
    ScanItem projectItem = scan.getItems().get(0).getItems().get(0);
    assertEquals("target/mod-a-1.0.jar", projectItem.getPath());
    assertPaths(projectItem, "META-INF/MANIFEST.MF", "null");
    for (ScanItem item : projectItem.getItems()) {
      if (!"META-INF/MANIFEST.MF".equals(item.getPath())) {
        assertEquals(null, item.getPath());
        assertEquals("proprietaryPackages", item.getNoPathReason());
      }
    }
    ScanItem repoItem = scan.getItems().get(1);
    assertEquals("com/sonatype/clm/its/mod-a/1.1/mod-a-1.1.jar", repoItem.getPath());
    for (ScanItem item : repoItem.getItems()) {
      assertEquals(null, item.getPath());
      assertEquals("proprietaryPackages", item.getNoPathReason());
    }
    assertSummary(2, 5, scan.getSummary());
  }

}
