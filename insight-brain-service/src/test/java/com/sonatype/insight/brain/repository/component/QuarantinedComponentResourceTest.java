/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

import javax.ws.rs.core.Response.Status;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightConfig.ExperimentalFeature;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class QuarantinedComponentResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetQuarantinedComponent() throws Exception {
    // setup
    getTestCLMServer().getCLMServer().getConfiguration().setExperimentalFeatures(ImmutableMap.of(
        ExperimentalFeature.ANONYMOUS_QUARANTINED_COMPONENT_VIEW.getFlag(), true));
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId());
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH).parameter(encodedToken).get();

    // then
    assertThat(response.getStatusCode()).isEqualTo(Status.OK.getStatusCode());
    QuarantinedComponentDto quarantinedComponentDto = response.getBody(QuarantinedComponentDto.class);
    assertThat(quarantinedComponentDto).isNotNull();
    assertThat(quarantinedComponentDto.success).isTrue();
    assertThat(quarantinedComponentDto.repositoryComponentId).isEqualTo(repositoryComponent.getId());
  }

  @Test
  public void testGetQuarantinedComponent_featureDisabled() throws Exception {
    // setup
    getTestCLMServer().getCLMServer().getConfiguration().setExperimentalFeatures(Collections.emptyMap());

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH).parameter("token").get();

    // then
    assertThat(response.getStatusCode()).isEqualTo(Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void testGetQuarantinedComponent_invalidToken() throws Exception {
    // setup
    getTestCLMServer().getCLMServer().getConfiguration().setExperimentalFeatures(ImmutableMap.of(
        ExperimentalFeature.ANONYMOUS_QUARANTINED_COMPONENT_VIEW.getFlag(), true));

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH).parameter("token").get();

    // then
    assertThat(response.getStatusCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void testGetQuarantinedComponent_expiredToken() throws Exception {
    // setup
    final Repository repository = tempEntity.newRepository("repo");
    final RepositoryComponent repositoryComponent = tempEntity.newRepositoryComponent(repository.getId());
    getTestCLMServer().getCLMServer().getConfiguration().setExperimentalFeatures(ImmutableMap.of(
        ExperimentalFeature.ANONYMOUS_QUARANTINED_COMPONENT_VIEW.getFlag(), true));
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repository.getId(), repositoryComponent.getId(),
            DateUtils.addDays(new Date(), -3));
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH).parameter(encodedToken).get();

    // then
    assertThat(response.getStatusCode()).isEqualTo(Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void testGetQuarantinedComponent_tokenDoesNotExist() throws Exception {
    // setup
    getTestCLMServer().getCLMServer().getConfiguration().setExperimentalFeatures(ImmutableMap.of(
        ExperimentalFeature.ANONYMOUS_QUARANTINED_COMPONENT_VIEW.getFlag(), true));
    final String encodedToken = Base64.getUrlEncoder().withoutPadding()
        .encodeToString("token".getBytes(StandardCharsets.UTF_8));

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_PATH).parameter(encodedToken).get();

    // then
    assertThat(response.getStatusCode()).isEqualTo(Status.NOT_FOUND.getStatusCode());
  }

  @Test
  public void testGetQuarantinedComponentOverview() throws Exception {
    // setup
    Date date = new Date();
    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    final String encodedToken = setupTestData(componentIdentifier, date);

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_OVERVIEW_PATH).parameter(encodedToken).get();

    // then
    assertThat(response.getStatusCode()).isEqualTo(Status.OK.getStatusCode());
    QuarantinedComponentOverviewDto quarantinedComponentOverviewDto =
        response.getBody(QuarantinedComponentOverviewDto.class);
    assertThat(quarantinedComponentOverviewDto).isNotNull();
    assertThat(quarantinedComponentOverviewDto.componentDisplayName).isEqualTo("g : a : v");
    assertThat(quarantinedComponentOverviewDto.isQuarantined).isEqualTo(true);
    assertThat(quarantinedComponentOverviewDto.quarantinedPolicyViolationsCount).isEqualTo(1);
    assertThat(quarantinedComponentOverviewDto.repositoryName).isEqualTo("repositoryPublicId");
    assertThat(quarantinedComponentOverviewDto.quarantinedDate).isEqualTo(date);
    assertThat(quarantinedComponentOverviewDto.cataloguedDate).isEqualTo(date);
  }

  @Test
  public void testGetQuarantinedComponentOverview_componentIdentifierDoesNotExist() throws Exception {
    // setup
    Date date = new Date();
    final String encodedToken = setupTestData(null, date);

    // when
    final HttpResponse response =
        restRequest().path(QuarantinedComponentResource.RESOURCE_PATH,
            QuarantinedComponentResource.QUARANTINED_COMPONENT_OVERVIEW_PATH).parameter(encodedToken).get();

    // then
    assertThat(response.getStatusCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
  }

  private String setupTestData(ComponentIdentifier componentIdentifier, Date date) {
    getTestCLMServer().getCLMServer().getConfiguration().setExperimentalFeatures(ImmutableMap.of(
        ExperimentalFeature.ANONYMOUS_QUARANTINED_COMPONENT_VIEW.getFlag(), true));
    final RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    final Repository repository = tempEntity.newRepository(repositoryManager, "repositoryPublicId");
    final RepositoryComponent repositoryComponent =
        tempEntity.newRepositoryComponent(repository.getId(), MatchState.EXACT, "path",
            "hash", componentIdentifier, date, date);
    tempEntity.newRepositoryPolicyViolation(repository.getId(), 6,
        repositoryComponent.getPathname(), false, "fail", "policyId", "policyName",
        repositoryComponent.getComponentIdentifier(), date);
    final QuarantinedComponentAccess quarantinedComponentAccess =
        tempEntity.newQuarantinedComponentAccess(repositoryComponent.getId());
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(quarantinedComponentAccess.getId().getBytes(StandardCharsets.UTF_8));
  }
}

