/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import jakarta.inject.Inject;
import org.junit.Before;
import org.junit.Test;

public class ApiSearchServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiSearchServiceV2 searchService;

  @Before
  public void before() {
    setBaseUrl("http://localhost:8070");
  }

  @Test
  public void testSearchComponent() {
    String stage = Stage.ID_BUILD;
    String hash = "1249e25aebb15358bedd";
    String scanId = "search-test";
    tempEntity.newPolicyEvaluation(app.getId(), stage, scanId);
    tempEntity.newApplicationComponent(app.getId(), stage, hash, null);
    // No report is stored on disk for this app, so ReportService.getReportIfPresent returns null, exercising the
    // no-report path: the component still matches from the DB while dependency-data enrichment is skipped (CLM-41473).

    ApiSearchResultsDTOV2 results = searchService.searchComponent(stage, hash, null, null);
    assertThat(results).isNotNull();
    assertThat(results.results).isEmpty();

    grantReadPermission(app.getId());

    results = searchService.searchComponent(stage, hash, null, null);
    assertThat(results).isNotNull();
    assertThat(results.results).hasSize(1);
    assertThat(results.results.get(0).applicationId).isEqualTo(app.getPublicId());
    assertThat(results.results.get(0).dependencyData).isNotNull();
  }
}
