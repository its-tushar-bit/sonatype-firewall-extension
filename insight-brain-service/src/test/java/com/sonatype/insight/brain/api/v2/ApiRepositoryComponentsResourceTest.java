/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ApiRepositoryComponentsResourceTest
    extends AbstractResourceTest
{
  private RepositoryManager repositoryManager;

  private Repository repository;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
    repositoryManager = tempEntity.newRepositoryManager("test-nexus");
    repository = tempEntity.newRepository(repositoryManager, "maven-hosted");
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Test
  public void testGetComponents_EmptyRepository() throws Exception {
    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .get()
        .getBodyText();

    assertThat(response).contains("\"components\":[]");
    assertThat(response).contains("\"totalCount\":0");
    assertThat(response).contains("\"hasNextPage\":false");
  }

  @Test
  public void testGetComponents_ReturnsComponents() throws Exception {
    tempEntity.newRepositoryComponent(repository.getId(), "log4j-core-2.14.1.jar");
    tempEntity.newRepositoryComponent(repository.getId(), "commons-text-1.9.0.jar");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .get()
        .getBodyText();

    assertThat(response).contains("\"totalCount\":2");
    assertThat(response).contains("log4j-core-2.14.1.jar");
    assertThat(response).contains("commons-text-1.9.0.jar");
  }

  @Test
  public void testGetComponents_FilterByDisplayName() throws Exception {
    tempEntity.newRepositoryComponent(repository.getId(), "log4j-core-2.14.1.jar");
    tempEntity.newRepositoryComponent(repository.getId(), "commons-text-1.9.0.jar");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .query("filter", "log4j")
        .get()
        .getBodyText();

    assertThat(response).contains("log4j-core-2.14.1.jar");
    assertThat(response).doesNotContain("commons-text-1.9.0.jar");
  }

  @Test
  public void testGetComponents_Pagination() throws Exception {
    for (int i = 0; i < 30; i++) {
      tempEntity.newRepositoryComponent(repository.getId(), "component-" + i + ".jar");
    }

    String page1 = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .query("page", "1")
        .query("pageSize", "25")
        .get()
        .getBodyText();

    assertThat(page1).contains("\"hasNextPage\":true");
    assertThat(page1).contains("\"page\":1");
    assertThat(page1).contains("\"totalCount\":30");
  }

  @Test
  public void testGetComponents_PageSizeCappedAt100() throws Exception {
    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .query("pageSize", "1000000")
        .get()
        .getBodyText();

    assertThat(response).contains("\"pageSize\":100");
  }

  @Test
  public void testGetComponents_WrongManager_Returns404() throws Exception {
    int status = restRequest()
        .path("/api/v2/repositories/wrong-manager/" + repository.getId() + "/components")
        .get()
        .getStatusCode();

    assertThat(status).isEqualTo(404);
  }

  @Test
  public void testGetComponents_FeatureDisabled_Returns404() throws Exception {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);

    int status = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components")
        .get()
        .getStatusCode();

    assertThat(status).isEqualTo(404);
  }

  @Test
  public void testGetComponent_ReturnsComponent() throws Exception {
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "log4j-core-2.14.1.jar");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/" + component.getId())
        .get()
        .getBodyText();

    assertThat(response).contains("log4j-core-2.14.1.jar");
  }

  @Test
  public void testGetComponent_NotFound_Returns404() throws Exception {
    int status = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/nonexistent-id")
        .get()
        .getStatusCode();

    assertThat(status).isEqualTo(404);
  }

  @Test
  public void testGetComponent_WithViolation_ReturnsViolationCount() throws Exception {
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "log4j-core-2.14.1.jar");
    tempEntity.newRepositoryPolicyViolation(repository.getId(), "log4j-core-2.14.1.jar");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/" + component.getId())
        .get()
        .getBodyText();

    assertThat(response).contains("\"violationCount\":1");
  }

  @Test
  public void testGetViolations_ReturnsEmptyList() throws Exception {
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "log4j-core-2.14.1.jar");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/" + component.getId()
            + "/violations")
        .get()
        .getBodyText();

    assertThat(response).contains("\"violations\":[]");
    assertThat(response).contains("\"totalViolations\":0");
  }

  @Test
  public void testGetViolations_ReturnsViolations() throws Exception {
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "log4j-core-2.14.1.jar");
    tempEntity.newRepositoryPolicyViolation(repository.getId(), "log4j-core-2.14.1.jar");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/components/" + component.getId()
            + "/violations")
        .get()
        .getBodyText();

    assertThat(response).contains("\"totalViolations\":1");
    assertThat(response).contains("policyName");
    assertThat(response).contains("\"threatLevel\":5");
    assertThat(response).contains("\"waived\":false");
  }

  @Test
  public void testGetQueueStats_EmptyQueue() throws Exception {
    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/queue/stats")
        .get()
        .getBodyText();

    assertThat(response).contains("\"pending\":0");
    assertThat(response).contains("\"processing\":0");
    assertThat(response).contains("\"completed\":0");
    assertThat(response).contains("\"failed\":0");
    assertThat(response).contains("\"total\":0");
  }

  @Test
  public void testGetQueueStats_WithEntries() throws Exception {
    RepositoryComponent component = tempEntity.newRepositoryComponent(repository.getId(), "log4j-core-2.14.1.jar");
    tempEntity.newHostedComponentScanQueue(component.getId(), repository.getId(), "PENDING");
    tempEntity.newHostedComponentScanQueue(component.getId(), repository.getId(), "IN_PROGRESS");
    tempEntity.newHostedComponentScanQueue(component.getId(), repository.getId(), "COMPLETED");
    tempEntity.newHostedComponentScanQueue(component.getId(), repository.getId(), "FAILED");

    String response = restRequest()
        .path("/api/v2/repositories/test-nexus/" + repository.getId() + "/queue/stats")
        .get()
        .getBodyText();

    assertThat(response).contains("\"pending\":1");
    assertThat(response).contains("\"processing\":1");
    assertThat(response).contains("\"completed\":1");
    assertThat(response).contains("\"failed\":1");
    assertThat(response).contains("\"total\":4");
  }
}
