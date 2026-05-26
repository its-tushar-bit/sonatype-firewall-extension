/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ApiSearchServiceV2AuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private ApiSearchServiceV2 searchService;

  @Mock
  private ReportService reportServiceMock;

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
    when(reportServiceMock.getReport(app.getId(), scanId)).thenThrow(NotFoundException.class);

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
