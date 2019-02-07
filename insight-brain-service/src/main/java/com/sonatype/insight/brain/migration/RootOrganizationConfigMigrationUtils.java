/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.FileUtils;

/**
 * @since 1.18
 */
@Named
@Singleton
public class RootOrganizationConfigMigrationUtils
{
  private static final String MARKER_FILE_NAME = "rootorganizationconfig-migrated";

  private static final String MIGRATION_CONFIG_FILE_NAME = "rootorganizationconfig-migration";

  private final InsightWork insightWork;

  @Inject
  public RootOrganizationConfigMigrationUtils(InsightWork insightWork) {
    this.insightWork = insightWork;
  }

  public boolean isMigrated() {
    return getMarkerFile().isFile();
  }

  public boolean isMigrationScheduled() {
    return getMigrationConfigFile().isFile();
  }

  private File getMigrationConfigFile() {
    return new File(insightWork.getWorkDir(), MIGRATION_CONFIG_FILE_NAME);
  }

  private File getMarkerFile() {
    return new File(insightWork.getWorkDir(), MARKER_FILE_NAME);
  }

  public void setSourceOrganizationId(String orgId) throws IOException {
    File migrationConfigFile = getMigrationConfigFile();
    migrationConfigFile.getParentFile().mkdirs();
    FileUtils.fileWrite(migrationConfigFile, orgId);
  }

  public String getSourceOrganizationId() {
    if (isMigrationScheduled()) {
      try {
        return FileUtils.fileRead(getMigrationConfigFile());
      }
      catch (IOException e) {
        throw new UncheckedIOException("Cannot load the source organization ID from file: "
            + getMigrationConfigFile().getAbsolutePath(), e);
      }
    }

    return null;
  }

  public void setMigrated() throws IOException {
    getMigrationConfigFile().delete();

    File markerFile = getMarkerFile();
    markerFile.getParentFile().mkdirs();
    markerFile.createNewFile();
  }

  public void clean() {
    getMarkerFile().delete();
    getMigrationConfigFile().delete();
  }
}
