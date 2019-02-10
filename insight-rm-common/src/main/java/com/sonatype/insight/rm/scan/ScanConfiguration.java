/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.rm.scan;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sonatype.clm.dto.model.ProprietaryConfig;

public class ScanConfiguration
{
  private File workDir;

  private Properties scanOptions;

  private String repositoryId;

  private String repositoryName;

  private String repositoryFormat;

  private ProprietaryConfig proprietaryConfig;

  private final List<RepositoryItem> componentItems;

  private final List<RepositoryItem> scanItems;

  public ScanConfiguration() {
    workDir = getDefaultWorkDir();
    scanOptions = new Properties();
    componentItems = new ArrayList<>(128);
    scanItems = new ArrayList<>();
  }

  private static File getDefaultWorkDir() {
    return new File(System.getProperty("java.io.tmpdir", "")).getAbsoluteFile();
  }

  public File getWorkDir() {
    return workDir;
  }

  public ScanConfiguration setWorkDir(final File workDir) {
    this.workDir = (workDir != null) ? workDir : getDefaultWorkDir();
    return this;
  }

  public Properties getScanOptions() {
    return scanOptions;
  }

  public ScanConfiguration setScanOptions(final Properties scanOptions) {
    this.scanOptions.clear();
    if (scanOptions != null) {
      this.scanOptions.putAll(scanOptions);
    }
    return this;
  }

  public ScanConfiguration setScanOption(final String key, final String value) {
    if (value == null) {
      scanOptions.remove(key);
    }
    else {
      scanOptions.setProperty(key, value);
    }
    return this;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public String getRepositoryName() {
    return repositoryName;
  }

  public String getRepositoryFormat() {
    return repositoryFormat;
  }

  public ScanConfiguration setRepository(String id, String format, String name) {
    repositoryId = id;
    repositoryFormat = format;
    repositoryName = name;
    return this;
  }

  public ScanConfiguration setProprietaryConfig(ProprietaryConfig proprietaryConfig) {
    this.proprietaryConfig = proprietaryConfig;
    return this;
  }

  public ProprietaryConfig getProprietaryConfig() {
    return proprietaryConfig;
  }

  public void addItem(final RepositoryItem item) {
    if (item != null) {
      if (isScanItem(item)) {
        scanItems.add(item);
      }
      else if (isComponentItem(item)) {
        componentItems.add(item);
      }
    }
  }

  private boolean isScanItem(final RepositoryItem item) {
    final String path = item.getPath();
    if (path.endsWith("-sonatype-clm-scan.xml.gz") || path.endsWith("-nexus-iq-scan.xml.gz")) {
      return true;
    }
    return false;
  }

  private boolean isComponentItem(final RepositoryItem item) {
    String path = item.getPath();
    if (path.endsWith(".pom") || path.endsWith(".asc") || path.endsWith(".sha1") || path.endsWith(".md5")) {
      return false;
    }
    if (path.endsWith("-sources.jar") || path.endsWith("-javadoc.jar") || path.endsWith("-tests.jar")) {
      return false;
    }
    return true;
  }

  List<RepositoryItem> getComponentItems() {
    return componentItems;
  }

  List<RepositoryItem> getScanItems() {
    return scanItems;
  }
}
