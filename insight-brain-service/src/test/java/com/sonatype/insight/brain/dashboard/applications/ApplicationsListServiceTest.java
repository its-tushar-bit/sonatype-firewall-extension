/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.applications;

import java.util.Arrays;
import java.util.Set;

import com.sonatype.insight.brain.dashboard.DashboardIndexDimensionQueryBuilder;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.service.Configuration;

import org.apache.lucene.search.SortField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ApplicationsListServiceTest
{
  @Mock
  private Configuration configuration;

  @Mock
  private ApplicationsListViolationScopeResolver violationScopeResolver;

  @Test
  public void buildApplicationQuery_appFilterMovedToTermSets() {
    ApplicationsListRequestDTO request = new ApplicationsListRequestDTO();
    request.applicationIds = Set.of("app+id");
    ApplicationsListIndexQueryBuilder builder = new ApplicationsListIndexQueryBuilder(
        new DashboardIndexDimensionQueryBuilder(null, configuration),
        violationScopeResolver);
    String query = builder.buildApplicationQuery(request);
    // App filter no longer in query string — handled by term-set restrictions (CLM-44783).
    assertThat(query).doesNotContain("applicationId:");
    assertThat(builder.buildScopeRestrictions(request)).isNotEmpty();
  }

  @Test
  public void toSearchIndexPage_mapsZeroBasedClientPagesToIndexContract() {
    assertThat(ApplicationsListService.toSearchIndexPage(0)).isEqualTo(0);
    assertThat(ApplicationsListService.toSearchIndexPage(1)).isEqualTo(2);
    assertThat(ApplicationsListService.toSearchIndexPage(2)).isEqualTo(3);
  }

  @Test
  public void sessionSort_riskFirstUsesThreatThenEvaluationThenDocumentKey() {
    SortField[] fields = ApplicationsListService.sessionSort("-maxPolicyThreatLevel").getSort();

    assertThat(Arrays.stream(fields).map(SortField::getField).toList())
        .containsExactly(
            FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label,
            FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label,
            FieldIdentifier.DOCUMENT_KEY.label);
    assertThat(fields[0].getReverse()).isTrue();
    assertThat(fields[0].getMissingValue()).isEqualTo(Integer.MIN_VALUE);
    assertThat(fields[1].getReverse()).isTrue();
    assertThat(fields[1].getMissingValue()).isEqualTo(Long.MIN_VALUE);
    assertThat(fields[2].getReverse()).isFalse();
  }

  @Test
  public void sessionSort_ascendingRiskUsesAscendingMissingSentinels() {
    SortField[] fields = ApplicationsListService.sessionSort("maxPolicyThreatLevel").getSort();

    assertThat(Arrays.stream(fields).map(SortField::getField).toList())
        .containsExactly(
            FieldIdentifier.APPLICATION_MAX_POLICY_THREAT_LEVEL.label,
            FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label,
            FieldIdentifier.DOCUMENT_KEY.label);
    assertThat(fields[0].getReverse()).isFalse();
    assertThat(fields[0].getMissingValue()).isEqualTo(Integer.MAX_VALUE);
    assertThat(fields[1].getReverse()).isFalse();
    assertThat(fields[1].getMissingValue()).isEqualTo(Long.MAX_VALUE);
    assertThat(fields[2].getReverse()).isFalse();
  }

  @Test
  public void sessionSort_evaluationTimeUsesEvaluationThenDocumentKey() {
    SortField[] descending = ApplicationsListService.sessionSort("-lastEvaluationTime").getSort();
    assertThat(Arrays.stream(descending).map(SortField::getField).toList())
        .containsExactly(
            FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label,
            FieldIdentifier.DOCUMENT_KEY.label);
    assertThat(descending[0].getReverse()).isTrue();
    assertThat(descending[0].getMissingValue()).isEqualTo(Long.MIN_VALUE);

    SortField[] ascending = ApplicationsListService.sessionSort("lastEvaluationTime").getSort();
    assertThat(Arrays.stream(ascending).map(SortField::getField).toList())
        .containsExactly(
            FieldIdentifier.APPLICATION_LAST_EVALUATION_TIME_EPOCH_MS.label,
            FieldIdentifier.DOCUMENT_KEY.label);
    assertThat(ascending[0].getReverse()).isFalse();
    assertThat(ascending[0].getMissingValue()).isEqualTo(Long.MAX_VALUE);
  }

  @Test
  public void toFallbackComparatorOrderBy_mapsThreatTokensToEvaluationTimeNotTotalRisk() {
    assertThat(ApplicationsListService.toFallbackComparatorOrderBy("-maxPolicyThreatLevel"))
        .isEqualTo("-lastEvaluationTime");
    assertThat(ApplicationsListService.toFallbackComparatorOrderBy("maxPolicyThreatLevel"))
        .isEqualTo("lastEvaluationTime");
    assertThat(ApplicationsListService.toFallbackComparatorOrderBy("-lastEvaluationTime"))
        .isEqualTo("-lastEvaluationTime");
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
