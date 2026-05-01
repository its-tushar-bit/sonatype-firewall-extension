/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfig;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.OwnerType.APPLICATION;
import static com.sonatype.insight.brain.model.OwnerType.ORGANIZATION;
import static org.assertj.core.api.Assertions.assertThat;

public class ScanHealthConfigDAOTest
    extends AbstractDbDAOTest
{
  private ScanHealthConfigDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createScanHealthConfigDAO();
  }

  @Test
  public void testFindByOwner_NotFound() {
    Optional<ScanHealthConfig> result = dao.findByOwner(APPLICATION.toString(), "nonexistent-id");
    assertThat(result).isEmpty();
  }

  @Test
  public void testSave_Insert() {
    ScanHealthConfig config = new ScanHealthConfig(
        application.getId(),
        APPLICATION.toString(),
        "{\"failOnZeroComponents\":true}");

    assertThat(config.getId()).isNull();

    dao.save(config);

    assertThat(config.getId()).isNotNull();

    ScanHealthConfig saved = dao.findByOwner(APPLICATION.toString(), application.getId()).orElse(null);
    assertThat(saved).isNotNull();
    assertThat(saved.getConfigurationJson()).isEqualTo("{\"failOnZeroComponents\":true}");
    assertThat(saved.getCreateTime()).isNotNull();
    assertThat(saved.getUpdateTime()).isNotNull();
  }

  @Test
  public void testSave_Update() {
    ScanHealthConfig config = new ScanHealthConfig(
        application.getId(),
        APPLICATION.toString(),
        "{\"failOnZeroComponents\":true}");

    dao.save(config);

    // Update the config
    config.setConfigurationJson("{\"failOnZeroComponents\":false}");
    dao.save(config);

    ScanHealthConfig updated = dao.findByOwner(APPLICATION.toString(), application.getId()).orElse(null);
    assertThat(updated).isNotNull();
    assertThat(updated.getConfigurationJson()).isEqualTo("{\"failOnZeroComponents\":false}");
    assertThat(updated.getId()).isEqualTo(config.getId());
  }

  @Test
  public void testSave_Organization() {
    ScanHealthConfig config = new ScanHealthConfig(
        organization.getId(),
        ORGANIZATION.toString(),
        "{\"failOnZeroComponents\":true}");

    dao.save(config);

    ScanHealthConfig saved = dao.findByOwner(ORGANIZATION.toString(), organization.getId()).orElse(null);
    assertThat(saved).isNotNull();
    assertThat(saved.getConfigurationJson()).isEqualTo("{\"failOnZeroComponents\":true}");
  }

  @Test
  public void testDelete() {
    ScanHealthConfig config = new ScanHealthConfig(
        application.getId(),
        APPLICATION.toString(),
        "{\"failOnZeroComponents\":true}");

    dao.save(config);

    assertThat(dao.findByOwner(APPLICATION.toString(), application.getId())).isPresent();

    dao.delete(APPLICATION.toString(), application.getId());

    assertThat(dao.findByOwner(APPLICATION.toString(), application.getId())).isEmpty();
  }

  @Test
  public void testDelete_NonExistent() {
    // Should not throw - delete on non-existent is a no-op
    dao.delete(APPLICATION.toString(), "nonexistent-id");
  }

  @Test
  public void testDeleteByOwnerId() {
    // Create configs for both application and organization
    ScanHealthConfig appConfig = new ScanHealthConfig(
        application.getId(),
        APPLICATION.toString(),
        "{\"failOnZeroComponents\":true}");
    dao.save(appConfig);

    ScanHealthConfig orgConfig = new ScanHealthConfig(
        organization.getId(),
        ORGANIZATION.toString(),
        "{\"failOnZeroComponents\":true}");
    dao.save(orgConfig);

    // Verify both exist
    assertThat(dao.findByOwner(APPLICATION.toString(), application.getId())).isPresent();
    assertThat(dao.findByOwner(ORGANIZATION.toString(), organization.getId())).isPresent();

    // Delete by application owner ID
    try (TransactionContext tx = dao.createTransactionContext()) {
      dao.deleteByOwnerId(tx, application.getId());
    }

    // Application config should be gone
    assertThat(dao.findByOwner(APPLICATION.toString(), application.getId())).isEmpty();
    // Organization config should still exist
    assertThat(dao.findByOwner(ORGANIZATION.toString(), organization.getId())).isPresent();
  }

  @Test
  public void testUniqueConstraint_SameOwnerTypeSameOwnerId() {
    ScanHealthConfig config1 = new ScanHealthConfig(
        application.getId(),
        APPLICATION.toString(),
        "{\"failOnZeroComponents\":true}");
    dao.save(config1);

    // Updating existing config should work
    ScanHealthConfig config2 = new ScanHealthConfig(
        application.getId(),
        APPLICATION.toString(),
        "{\"failOnZeroComponents\":false}");
    dao.save(config2);

    // Should still only have one config for this owner
    Optional<ScanHealthConfig> result = dao.findByOwner(APPLICATION.toString(), application.getId());
    assertThat(result).isPresent();
    assertThat(result.get().getConfigurationJson()).isEqualTo("{\"failOnZeroComponents\":false}");
  }
}
