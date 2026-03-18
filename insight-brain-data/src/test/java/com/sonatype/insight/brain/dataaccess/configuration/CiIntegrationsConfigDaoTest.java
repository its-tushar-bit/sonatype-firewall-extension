/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.CiIntegrationsConfig;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CiIntegrationsConfigDaoTest
    extends AbstractDbDAOTest
{
  private CiIntegrationsConfigDao dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createCiIntegrationsConfigDao();
  }

  @Test
  public void testInsertAndFind() {
    Organization owner = tempEntity.newOrganization();
    String configJson = "{\"parameterPriority\":\"CI\",\"enableDebugLogging\":false,\"failBuildOnNetworkError\":true}";

    CiIntegrationsConfig config = new CiIntegrationsConfig(owner.getId(), "ORGANIZATION", configJson);

    dao.save(config);
    assertThat(config.getId()).isNotNull();

    // Find by owner
    Optional<CiIntegrationsConfig> found = dao.findByOwner("ORGANIZATION", owner.getId());
    assertThat(found).isPresent().hasValueSatisfying(c -> {
      assertThat(c.getId()).isNotNull();
      assertThat(c.getOwnerId()).isEqualTo(owner.getId());
      assertThat(c.getOwnerType()).isEqualTo("ORGANIZATION");
      assertThat(c.getConfigurationJson()).isEqualTo(configJson);
      assertThat(c.getCreateTime()).isNotNull();
      assertThat(c.getUpdateTime()).isNotNull();
    });
  }

  @Test
  public void testUpdate() {
    Application app = tempEntity.newApplicationWithParent();
    String originalJson = "{\"parameterPriority\":\"CI\",\"failBuildOnNetworkError\":true,"
        + "\"scanPatterns\":[\"*.jar\"]}";
    String updatedJson = "{\"parameterPriority\":\"API\",\"failBuildOnNetworkError\":false,"
        + "\"scanPatterns\":[\"*.war\",\"*.ear\"]}";

    CiIntegrationsConfig config = new CiIntegrationsConfig(app.getId(), "APPLICATION", originalJson);
    dao.save(config);

    Date createTime = config.getCreateTime();
    String configId = config.getId();

    // Update
    config.setConfigurationJson(updatedJson);
    dao.save(config);

    // Verify update
    Optional<CiIntegrationsConfig> updated = dao.findByOwner("APPLICATION", app.getId());
    assertThat(updated).isPresent().hasValueSatisfying(c -> {
      assertThat(c.getId()).isEqualTo(configId);
      assertThat(c.getConfigurationJson()).isEqualTo(updatedJson);
      assertThat(c.getCreateTime()).isEqualTo(createTime);
      assertThat(c.getUpdateTime()).isAfterOrEqualTo(createTime);
    });
  }

  @Test
  public void testDelete() {
    Organization owner = tempEntity.newOrganization();
    CiIntegrationsConfig config = new CiIntegrationsConfig(
        owner.getId(),
        "ORGANIZATION",
        "{\"parameterPriority\":\"CI\",\"failBuildOnScanningErrors\":true}");

    dao.save(config);
    assertThat(dao.findByOwner("ORGANIZATION", owner.getId())).isNotEmpty();

    // Delete
    dao.delete("ORGANIZATION", owner.getId());

    // Verify deletion
    assertThat(dao.findByOwner("ORGANIZATION", owner.getId())).isEmpty();
  }

  @Test
  public void testFindByOwnerList() {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization();
    Application app = tempEntity.newApplicationWithParent();

    // Create configs for different owners
    CiIntegrationsConfig config1 = new CiIntegrationsConfig(
        org1.getId(),
        "ORGANIZATION",
        "{\"parameterPriority\":\"CI\",\"scanPatterns\":[\"*.jar\",\"*.war\"]}");
    dao.save(config1);

    CiIntegrationsConfig config2 = new CiIntegrationsConfig(
        org2.getId(),
        "ORGANIZATION",
        "{\"parameterPriority\":\"API\",\"moduleExcludes\":[\"test/**\",\"**/node_modules/**\"]}");
    dao.save(config2);

    CiIntegrationsConfig config3 = new CiIntegrationsConfig(
        app.getId(),
        "APPLICATION",
        "{\"parameterPriority\":\"CI\",\"advancedProperties\":[\"key1=value1\",\"key2=value2\"]}");
    dao.save(config3);

    // Search by hierarchy
    List<CiIntegrationsConfig> configs = dao.findByOwnerList(List.of(org1.getId(), app.getId()));

    assertThat(configs).hasSize(2);
    assertThat(configs)
        .extracting(CiIntegrationsConfig::getOwnerId)
        .containsExactlyInAnyOrder(org1.getId(), app.getId());
  }

  @Test
  public void testFindByOwnerListEmpty() {
    List<CiIntegrationsConfig> configs = dao.findByOwnerList(List.of());
    assertThat(configs).isEmpty();

    configs = dao.findByOwnerList(null);
    assertThat(configs).isEmpty();
  }

  @Test
  public void testFindByOwnerNotFound() {
    Optional<CiIntegrationsConfig> found = dao.findByOwner("ORGANIZATION", "non-existent-id");
    assertThat(found).isEmpty();
  }

  @Test
  public void testJsonStorage() {
    Organization owner = tempEntity.newOrganization();
    String complexJson = "{"
        + "\"parameterPriority\":\"CI\","
        + "\"scanPatterns\":[\"*.jar\",\"*.war\"],"
        + "\"enableDebugLogging\":false,"
        + "\"proxy\":{"
        + "\"host\":\"https://proxy.example.com:8080\""
        + "},"
        + "\"download\":{"
        + "\"iqCliVersion\":\"2.1.0-01\","
        + "\"iqCliUrl\":\"https://download.sonatype.com/clm/nexus-iq-cli-2.1.0-01.jar\""
        + "},"
        + "\"reachability\":{"
        + "\"javaAnalysis\":{"
        + "\"enabled\":true,"
        + "\"namespaces\":[\"com.example\",\"org.example\"]"
        + "},"
        + "\"javaScriptAnalysis\":{"
        + "\"enabled\":false,"
        + "\"projectRoot\":\".\","
        + "\"jsSources\":[\"src/**/*.js\"],"
        + "},"
        + "\"failOnError\":false"
        + "}"
        + "}";

    CiIntegrationsConfig config = new CiIntegrationsConfig(owner.getId(), "ORGANIZATION", complexJson);
    dao.save(config);

    // Retrieve and verify JSON is stored correctly
    Optional<CiIntegrationsConfig> found = dao.findByOwner("ORGANIZATION", owner.getId());
    assertThat(found).isPresent()
        .hasValueSatisfying(c -> assertThat(c.getConfigurationJson()).isEqualTo(complexJson));
  }
}
