/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.Set;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.service.Configuration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationsListServiceTest
{
  @Mock
  private Configuration configuration;

  @Mock
  private ApplicationsListViolationScopeResolver violationScopeResolver;

  @Test
  public void buildApplicationQuery_escapesApplicationIdSpecialCharacters() {
    when(configuration.getMaxAdvancedSearchClauseCount()).thenReturn(100);

    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.applicationIds = Set.of("app+id");
    String query = new ApplicationsListIndexQueryBuilder(
        new DashboardIndexDimensionQueryBuilder(null, configuration),
        violationScopeResolver)
            .buildApplicationQuery(request);
    assertThat(query).isEqualTo("itemType:APPLICATION AND (applicationId:(app\\+id))");
  }

  @Test
  public void toSearchIndexPage_mapsZeroBasedClientPagesToIndexContract() {
    assertThat(ApplicationsListService.toSearchIndexPage(0)).isEqualTo(0);
    assertThat(ApplicationsListService.toSearchIndexPage(1)).isEqualTo(2);
    assertThat(ApplicationsListService.toSearchIndexPage(2)).isEqualTo(3);
  }

  @Test
  public void escapeLuceneTerm_neutralizesSpecialCharacters() {
    assertThat(ApplicationsListService.escapeLuceneTerm("foo+bar"))
        .isEqualTo("foo\\+bar");
    assertThat(ApplicationsListService.escapeLuceneTerm("a&&b"))
        .isEqualTo("a\\&\\&b");
    assertThat(ApplicationsListService.escapeLuceneTerm("foo/bar"))
        .isEqualTo("foo\\/bar");
  }
}
