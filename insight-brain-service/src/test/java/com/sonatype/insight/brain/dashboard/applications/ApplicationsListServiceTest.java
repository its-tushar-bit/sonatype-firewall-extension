/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.service.Configuration;

import org.apache.lucene.search.SortField;
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
  public void stableSessionSort_usesDocumentKeyDocValuesField() {
    SortField[] sortFields = ApplicationsListService.stableSessionSort().getSort();
    assertThat(Arrays.stream(sortFields).map(SortField::getField).collect(Collectors.toList()))
        .containsExactly(FieldIdentifier.DOCUMENT_KEY.label);
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

  @Test
  public void toSessionQueryString_rewritesOrgFieldsWithoutDoublePrefixing() {
    String rewritten = ApplicationsListService.toSessionQueryString(
        "itemType:APPLICATION AND organizationId:abc AND organizationName:Acme");
    assertThat(rewritten)
        .contains(FieldIdentifier.PARENT_ORGANIZATION_ID.label + ":abc")
        .contains(FieldIdentifier.PARENT_ORGANIZATION_NAME.label + ":Acme")
        .doesNotContain("parentparent")
        .doesNotContain("parentParent");
    assertThat(rewritten).doesNotContain(" AND " + FieldIdentifier.ORGANIZATION_ID.label + ":");
    assertThat(rewritten).doesNotContain(" AND " + FieldIdentifier.ORGANIZATION_NAME.label + ":");

    String alreadyParent = ApplicationsListService.toSessionQueryString(
        "itemType:APPLICATION AND parentOrganizationId:abc AND parentOrganizationName:Acme");
    // Must leave already-parent tokens intact (lookbehind), not only rely on label casing.
    assertThat(alreadyParent)
        .isEqualTo(
            "itemType:APPLICATION AND parentOrganizationId:abc AND parentOrganizationName:Acme"
                + " -itemType:NON_VULNERABLE_COMPONENT");
  }

  @Test
  public void toSessionQueryString_rewritesFieldTokensNotSearchValues() {
    // buildSearchClause embeds the term after the colon; a bare label replace would mutate it.
    String rewritten = ApplicationsListService.toSessionQueryString(
        "itemType:APPLICATION AND (organizationName:*organizationName* OR organizationId:*organizationId*)");
    assertThat(rewritten)
        .contains(FieldIdentifier.PARENT_ORGANIZATION_NAME.label + ":*organizationName*")
        .contains(FieldIdentifier.PARENT_ORGANIZATION_ID.label + ":*organizationId*")
        .doesNotContain(FieldIdentifier.PARENT_ORGANIZATION_NAME.label + ":*parentOrganizationName*")
        .doesNotContain(FieldIdentifier.PARENT_ORGANIZATION_ID.label + ":*parentOrganizationId*");
  }
}
