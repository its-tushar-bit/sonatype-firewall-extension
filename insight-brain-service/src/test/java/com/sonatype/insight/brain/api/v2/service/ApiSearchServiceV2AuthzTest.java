/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSearchServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiSearchServiceV2 searchService;

  @Override
  protected void customizeConfig(InsightConfig config) {
    config.setBaseUrl("http://localhost:8070");
  }

  @Test
  public void testSearchComponent() throws Exception {
    String stage = Stage.ID_BUILD;
    String hash = "1249e25aebb15358bedd";
    tempEntity.newPolicyEvaluation(app.getId(), stage, "search-test");
    tempEntity.newApplicationComponent(app.getId(), stage, hash, null);

    ApiSearchResultsDTOV2 results = searchService.searchComponent(stage, hash, null, null);
    assertThat(results).isNotNull();
    assertThat(results.results).isEmpty();

    grantReadPermission(app.getId());

    results = searchService.searchComponent(stage, hash, null, null);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(1);
    assertThat(results.results.get(0).applicationId).isEqualTo(app.getPublicId());
  }
}
