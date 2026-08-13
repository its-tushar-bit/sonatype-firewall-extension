/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.IOException;
import java.util.Collections;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.JsonFileStore;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class ProprietaryConfigMigratorTest
    extends AbstractComponentH2Test
{
  @Inject
  private ProprietaryConfigMigrator migrator;

  @Inject
  private ProprietaryConfigDAO proprietaryConfigDAO;

  @Inject
  private InsightWork work;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private ClusterLockManager clusterLockManager;

  @BeforeEach
  public void before() {
    migrationTrackerDAO.deleteById(ProprietaryConfigMigrator.MIGRATION_ID);
  }

  @Test
  public void testMigrateWithExistingJsonFile() throws Exception {
    // setup
    com.sonatype.clm.dto.model.ProprietaryConfig obsoleteConfig = new com.sonatype.clm.dto.model.ProprietaryConfig();
    obsoleteConfig.setPackages(Collections.singletonList("com.test.package"));
    obsoleteConfig.setRegexes(Collections.singletonList("regex"));
    writeProprietaryConfigFile(obsoleteConfig);
    assertThat(migrationTrackerDAO.getById(ProprietaryConfigMigrator.MIGRATION_ID)).isNull();

    // execute
    migrator.migrate();

    // assert
    assertThat(migrationTrackerDAO.getById(ProprietaryConfigMigrator.MIGRATION_ID)).isNotNull();
    ProprietaryConfig migratedConfig = proprietaryConfigDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(migratedConfig).isNotNull();
    assertThat(migratedConfig.getPackages()).isEqualTo(obsoleteConfig.getPackages());
    assertThat(migratedConfig.getRegexes()).isEqualTo(obsoleteConfig.getRegexes());
    assertThat(migratedConfig.getOwnerId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testMigrateWithoutExistingJsonFile() {
    assertThat(migrationTrackerDAO.getById(ProprietaryConfigMigrator.MIGRATION_ID)).isNull();

    // execute
    migrator.migrate();

    // assert
    assertThat(migrationTrackerDAO.getById(ProprietaryConfigMigrator.MIGRATION_ID)).isNotNull();
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
    assertThat(migrationTrackerDAO.getById(ProprietaryConfigMigrator.MIGRATION_ID)).isNull();

    // execute
    migrator.migrate();

    // assert
    assertThat(migrationTrackerDAO.getById(ProprietaryConfigMigrator.MIGRATION_ID)).isNotNull();
    com.sonatype.insight.brain.model.configuration.ProprietaryConfig migratedConfig = proprietaryConfigDAO
        .getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(migratedConfig).isNull();
  }

  @Test
  public void testMigrateAlreadyMigrated() throws Exception {
    // setup
    migrationTrackerDAO.insert(new MigrationTracker(ProprietaryConfigMigrator.MIGRATION_ID));
    com.sonatype.clm.dto.model.ProprietaryConfig obsoleteConfig = new com.sonatype.clm.dto.model.ProprietaryConfig();
    obsoleteConfig.setPackages(Collections.singletonList("com.test.package"));
    obsoleteConfig.setRegexes(Collections.singletonList("regex"));
    writeProprietaryConfigFile(obsoleteConfig);

    // execute
    migrator.migrate();

    // assert
    assertThat(migrationTrackerDAO.getById(ProprietaryConfigMigrator.MIGRATION_ID)).isNotNull();
    com.sonatype.insight.brain.model.configuration.ProprietaryConfig migratedConfig = proprietaryConfigDAO
        .getByOwnerId(Organization.ROOT_ORGANIZATION_ID);
    assertThat(migratedConfig).isNull();
  }

  private void writeProprietaryConfigFile(com.sonatype.clm.dto.model.ProprietaryConfig config) throws IOException {
    new JsonFileStore(work.getDataDir(), "test", clusterLockManager).commit(
        ProprietaryConfigMigrator.PROPRIETARY_CONFIG_FILENAME,
        JsonUtils.stamp("user", "ip", null, JsonUtils.asTree(config)));
  }
}
