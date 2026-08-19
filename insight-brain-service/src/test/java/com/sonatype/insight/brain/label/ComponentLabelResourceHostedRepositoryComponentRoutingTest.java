/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test proving the JAX-RS regex on {@link ComponentLabelResource} actually routes
 * {@code hosted_repository_component} paths through the container to the resource method
 * — the regression risk the Mockito-based test cannot cover. A real HTTP request that lands on
 * a 200 or 404-from-DAO (not the JAX-RS "resource not found" 404) proves routing.
 */
public class ComponentLabelResourceHostedRepositoryComponentRoutingTest
    extends AbstractResourceTest
{
  @Before
  public void enableHostedRepositoryEvaluation() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(true);
  }

  @After
  public void disableHostedRepositoryEvaluation() {
    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);
  }

  @Test
  public void getComponentLabels_hrcPath_routesToHandlerAndReturns200() throws Exception {
    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    HttpResponse response = restRequest()
        .path("rest/label/component/{ownerType}/{ownerId}/{hash}")
        .parameter(OwnerType.HOSTED_REPOSITORY_COMPONENT.toString(), hrc.getId(), hrc.getHash())
        .get();

    // 200 with an empty labelsByOwner is the expected happy path when no labels are applied.
    // The critical assertion is that we don't get a JAX-RS 404 "Resource not found, please check
    // your request URL" — that's what a missing regex allowlist entry would produce.
    assertResponseStatus(200, response);
  }

  @Test
  public void getComponentLabels_hrcPath_hostedRepositoryEvaluationDisabled_returns401() throws Exception {
    Repository repository = tempEntity.newRepository();
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(repository);

    SystemConfigurationPropertyFeature.HOSTED_REPOSITORY_EVALUATION.setEnabled(false);

    HttpResponse response = restRequest()
        .path("rest/label/component/{ownerType}/{ownerId}/{hash}")
        .parameter(OwnerType.HOSTED_REPOSITORY_COMPONENT.toString(), hrc.getId(), hrc.getHash())
        .get();

    // HrcOwnerTypeFeatureGuard throws NotAuthorizedException → 401. Proves the runtime feature
    // gate that CLM-44276 Bhavat feedback added to this polymorphic-path resource is wired.
    assertResponseStatus(401, response);
  }
}
