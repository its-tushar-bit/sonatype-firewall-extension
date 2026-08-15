/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2ApiLifecycleResourceTest
{
  private IqTestContext ctx;

  private RepositoryManagerDAO repositoryManagerDAO;

  private RepositoryDAO repositoryDAO;

  @BeforeEach
  void setUp() {
    repositoryManagerDAO = ctx.lookup(RepositoryManagerDAO.class);
    repositoryDAO = ctx.lookup(RepositoryDAO.class);
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
  }

  @AfterEach
  void tearDown() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Test
  void testGetRepositoryManagers_Empty() throws Exception {
    String response = ctx.restRequest()
        .path("/api/v2/lifecycle/repositoryManagers")
        .get()
        .getBodyText();

    assertThat(response).contains("\"repositoryManagers\":[]");
  }

  @Test
  void testGetRepositoryManagers_SingleConnected() throws Exception {
    RepositoryManager rm = ctx.tempEntity().newRepositoryManager("nxrm-instance-1");
    rm.setBaseUrl("http://localhost:8081");
    rm.setConfigured(true);
    repositoryManagerDAO.update(rm);

    Repository repo1 = ctx.tempEntity().newRepository(rm, "maven-releases", RepositoryType.hosted, "maven2");
    repo1.setMonitoringEnabled(true);
    repositoryDAO.update(repo1);

    Repository repo2 = ctx.tempEntity().newRepository(rm, "npm-hosted", RepositoryType.hosted, "npm");
    repo2.setMonitoringEnabled(true);
    repositoryDAO.update(repo2);

    String response = ctx.restRequest()
        .path("/api/v2/lifecycle/repositoryManagers")
        .get()
        .getBodyText();

    assertThat(response).contains("\"instanceId\":\"nxrm-instance-1\"");
    assertThat(response).contains("\"baseUrl\":\"http://localhost:8081\"");
    assertThat(response).contains("\"hostedRepositoryCount\":2");
    assertThat(response).contains("\"connectionStatus\":\"CONNECTED\"");
  }

  @Test
  void testGetRepositoryManagers_Disconnected() throws Exception {
    RepositoryManager rm = ctx.tempEntity().newRepositoryManager("nxrm-instance-2");
    rm.setBaseUrl("http://localhost:8082");
    repositoryManagerDAO.update(rm);

    String response = ctx.restRequest()
        .path("/api/v2/lifecycle/repositoryManagers")
        .get()
        .getBodyText();

    assertThat(response).contains("\"instanceId\":\"nxrm-instance-2\"");
    assertThat(response).contains("\"baseUrl\":\"http://localhost:8082\"");
    assertThat(response).contains("\"hostedRepositoryCount\":0");
    assertThat(response).contains("\"connectionStatus\":\"DISCONNECTED\"");
  }

  @Test
  void testGetRepositoryManagers_MultipleInstances() throws Exception {
    RepositoryManager rm1 = ctx.tempEntity().newRepositoryManager("nxrm-prod");
    rm1.setBaseUrl("http://nxrm-prod:8081");
    rm1.setConfigured(true);
    repositoryManagerDAO.update(rm1);

    Repository repo1 = ctx.tempEntity().newRepository(rm1, "maven-releases", RepositoryType.hosted, "maven2");
    repo1.setMonitoringEnabled(true);
    repositoryDAO.update(repo1);

    RepositoryManager rm2 = ctx.tempEntity().newRepositoryManager("nxrm-dev");
    rm2.setBaseUrl("http://nxrm-dev:8081");
    repositoryManagerDAO.update(rm2);

    String response = ctx.restRequest()
        .path("/api/v2/lifecycle/repositoryManagers")
        .get()
        .getBodyText();

    assertThat(response).contains("\"instanceId\":\"nxrm-prod\"");
    assertThat(response).contains("\"instanceId\":\"nxrm-dev\"");
    assertThat(response).contains("\"connectionStatus\":\"CONNECTED\"");
    assertThat(response).contains("\"connectionStatus\":\"DISCONNECTED\"");
  }

  @Test
  void testGetRepositoryManagers_OnlyProxyRepositories() throws Exception {
    RepositoryManager rm = ctx.tempEntity().newRepositoryManager("nxrm-proxy-only");
    rm.setBaseUrl("http://localhost:8081");
    rm.setConfigured(true);
    repositoryManagerDAO.update(rm);

    ctx.tempEntity().newRepository(rm, "maven-central", RepositoryType.proxy, "maven2");

    String response = ctx.restRequest()
        .path("/api/v2/lifecycle/repositoryManagers")
        .get()
        .getBodyText();

    assertThat(response).contains("\"instanceId\":\"nxrm-proxy-only\"");
    assertThat(response).contains("\"hostedRepositoryCount\":0");
    assertThat(response).contains("\"connectionStatus\":\"CONNECTED\"");
  }

  @Test
  void testGetRepositoryManagers_OnlyMonitoredHostedCounted() throws Exception {
    RepositoryManager rm = ctx.tempEntity().newRepositoryManager("nxrm-mixed");
    rm.setBaseUrl("http://localhost:8081");
    rm.setConfigured(true);
    repositoryManagerDAO.update(rm);

    Repository monitoredRepo = ctx.tempEntity().newRepository(rm, "maven-releases", RepositoryType.hosted, "maven2");
    monitoredRepo.setMonitoringEnabled(true);
    repositoryDAO.update(monitoredRepo);

    ctx.tempEntity().newRepository(rm, "npm-hosted", RepositoryType.hosted, "npm");

    ctx.tempEntity().newRepository(rm, "maven-central", RepositoryType.proxy, "maven2");

    String response = ctx.restRequest()
        .path("/api/v2/lifecycle/repositoryManagers")
        .get()
        .getBodyText();

    assertThat(response).contains("\"instanceId\":\"nxrm-mixed\"");
    assertThat(response).contains("\"hostedRepositoryCount\":1");
    assertThat(response).contains("\"connectionStatus\":\"CONNECTED\"");
  }
}
