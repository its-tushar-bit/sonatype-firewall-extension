/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ProprietaryConfigMigratorTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private ProprietaryConfigMigrator migrator;

  private ProprietaryConfigDAO proprietaryConfigDAO;

  private InsightWork work;

  @Before
  public void setUp() throws Exception {
    InsightConfig insightConfig = new InsightConfig();
    File workDir = tempDir.newFolder();
    insightConfig.setSonatypeWork(workDir.getAbsolutePath());
    work = new InsightWork(insightConfig);
    work.getDataDir().mkdirs();
    proprietaryConfigDAO = new ProprietaryConfigDAO();
    migrator = new ProprietaryConfigMigrator(work, proprietaryConfigDAO);
  }

  @After
  public void tearDown() {
    // clean up any migrated proprietary config, since it doesn't get cleaned up automatically due to the root org.
    ProprietaryConfig proprietaryConfig = proprietaryConfigDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    if (proprietaryConfig != null) {
      proprietaryConfigDAO.delete(proprietaryConfig);
    }
  }

  @Test
  public void testMigrateWithExistingJsonFile() throws Exception {
    // setup
    com.sonatype.clm.dto.model.ProprietaryConfig obsoleteConfig = new com.sonatype.clm.dto.model.ProprietaryConfig();
    obsoleteConfig.setPackages(Collections.singletonList("com.test.package"));
    obsoleteConfig.setRegexes(Collections.singletonList("regex"));
    writeProprietaryConfigFile(obsoleteConfig);

    // execute
    migrator.migrate();

    // assert
    File markerFile = new File(work.getWorkDir(), ProprietaryConfigMigrator.MARKER_FILE_NAME);
    assertThat(markerFile).isFile();
    ProprietaryConfig migratedConfig = proprietaryConfigDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(migratedConfig).isNotNull();
    assertThat(migratedConfig.getPackages()).isEqualTo(obsoleteConfig.getPackages());
    assertThat(migratedConfig.getRegexes()).isEqualTo(obsoleteConfig.getRegexes());
    assertThat(migratedConfig.getOwnerId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testMigrateWithoutExistingJsonFile() throws Exception {
    // execute
    migrator.migrate();

    // assert
    File markerFile = new File(work.getWorkDir(), ProprietaryConfigMigrator.MARKER_FILE_NAME);
    assertThat(markerFile).isFile();
    com.sonatype.insight.brain.model.configuration.ProprietaryConfig migratedConfig = proprietaryConfigDAO
        .getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(migratedConfig).isNull();
  }

  @Test
  public void testMigrateWithEmptyJsonFile() throws Exception {
    // setup
    com.sonatype.clm.dto.model.ProprietaryConfig obsoleteConfig = new com.sonatype.clm.dto.model.ProprietaryConfig();
    obsoleteConfig.setPackages(Collections.emptyList());
    obsoleteConfig.setRegexes(Collections.emptyList());
    writeProprietaryConfigFile(obsoleteConfig);

    // execute
    migrator.migrate();

    // assert
    File markerFile = new File(work.getWorkDir(), ProprietaryConfigMigrator.MARKER_FILE_NAME);
    assertThat(markerFile).isFile();
    com.sonatype.insight.brain.model.configuration.ProprietaryConfig migratedConfig = proprietaryConfigDAO
        .getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(migratedConfig).isNull();
  }

  @Test
  public void testMigrateAlreadyMigrated() throws Exception {
    // setup
    File markerFile = new File(work.getWorkDir(), ProprietaryConfigMigrator.MARKER_FILE_NAME);
    markerFile.createNewFile();

    com.sonatype.clm.dto.model.ProprietaryConfig obsoleteConfig = new com.sonatype.clm.dto.model.ProprietaryConfig();
    obsoleteConfig.setPackages(Collections.singletonList("com.test.package"));
    obsoleteConfig.setRegexes(Collections.singletonList("regex"));
    writeProprietaryConfigFile(obsoleteConfig);

    // execute
    migrator.migrate();

    // assert
    assertThat(markerFile).isFile();
    com.sonatype.insight.brain.model.configuration.ProprietaryConfig migratedConfig = proprietaryConfigDAO
        .getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(migratedConfig).isNull();
  }

  private void writeProprietaryConfigFile(com.sonatype.clm.dto.model.ProprietaryConfig config) throws IOException {
    JsonUtils.fileStore(work.getDataDir()).commit(ProprietaryConfigMigrator.PROPRIETARY_CONFIG_FILENAME,
        JsonUtils.stamp("user", "ip", null, JsonUtils.asTree(config)));
  }
}
