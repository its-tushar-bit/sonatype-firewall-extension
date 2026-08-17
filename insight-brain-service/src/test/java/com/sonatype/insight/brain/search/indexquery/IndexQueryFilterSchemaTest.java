/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sonatype.insight.brain.search.indexquery.IndexQueryFilterSchema.FilterDef;
import com.sonatype.insight.brain.search.indexquery.IndexQueryFilterSchema.Kind;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLM-44713 slice 2b: id-keyed structured filters must compile to the SAME index field their matching
 * entity facet aggregates on ({@code IndexQueryService#FACET_FIELDS}). Otherwise
 * {@code IndexQueryService#computeFacets}'s own-clause removal (keyed by
 * {@code compiled.clausesByField().get(facet.indexField())}) never finds the filter's own clause, and
 * selecting a value collapses the facet's sibling buckets to just that selection.
 */
public class IndexQueryFilterSchemaTest
{
  /**
   * The invariant the per-key tests above check one at a time: every facet that aggregates on an index
   * field must have SOME filter key compiling to that same field, or a client sending bucket values back
   * cannot filter by them and own-clause removal cannot find the clause to drop.
   * <p>
   * Facets keyed on the field they aggregate (bounded vocabularies such as stages) satisfy this through
   * their own key; facets aggregating on an opaque id while displaying a name need a separate id-keyed
   * filter, which is what this catches when one is missing.
   */
  @Test
  public void everyFacetsAggregationFieldIsReachableByAFilterKey() {
    IndexQueryService.FACET_FIELDS.forEach((queryType, facets) -> {
      Map<String, FilterDef> schema = IndexQueryFilterSchema.forQueryType(queryType);
      for (IndexQueryService.Facet facet : facets) {
        if (facet.indexField() == null) {
          // Facets computed without aggregating an index field (e.g. derived numeric rails) have nothing
          // for a filter key to match.
          continue;
        }
        assertThat(schema.values())
            .as("%s facet '%s' aggregates on '%s', so some filter key must compile to that field "
                + "(otherwise its bucket values are unfilterable and selecting one collapses the rail)",
                queryType, facet.key(), facet.indexField())
            .anyMatch(def -> facet.indexField().equals(def.field()));
      }
    });
  }

  @Test
  public void waiver_organizationIds_compilesToTheSameFieldTheOrgFacetAggregatesOn() {
    // The WAIVER organizations facet aggregates on parentOrganizationId (see IndexQueryService
    // FACET_FIELDS). organizationIds must compile to that same field directly -- not organizationId,
    // which would rely on the createInitialQuery organizationId->parentOrganizationId rewrite that
    // runs AFTER clausesByField is computed, so it would never be found.
    Map<String, FilterDef> waiver = IndexQueryFilterSchema.forQueryType(IndexQueryType.WAIVER);
    assertThat(waiver.get("organizationIds")).isEqualTo(new FilterDef("parentOrganizationId", Kind.TERMS));
  }

  @Test
  public void application_applicationCategoryIds_compilesToTheSameFieldTheCategoryFacetAggregatesOn() {
    // The APPLICATION applicationCategories facet aggregates on applicationCategoryId.
    Map<String, FilterDef> application = IndexQueryFilterSchema.forQueryType(IndexQueryType.APPLICATION);
    assertThat(application.get("applicationCategoryIds"))
        .isEqualTo(new FilterDef("applicationCategoryId", Kind.TERMS));
  }

  @Test
  public void violation_applicationCategoryIds_compilesToTheSameFieldTheCategoryFacetAggregatesOn() {
    // The VIOLATION applicationCategories facet also aggregates on applicationCategoryId.
    Map<String, FilterDef> violation = IndexQueryFilterSchema.forQueryType(IndexQueryType.VIOLATION);
    assertThat(violation.get("applicationCategoryIds"))
        .isEqualTo(new FilterDef("applicationCategoryId", Kind.TERMS));
  }

  @Test
  public void waiver_applicationIds_alreadyMatchesTheAppFacetsAggregationField() {
    // The WAIVER applications facet aggregates on applicationId; applicationIds (and its deprecated
    // applicationId alias) must compile to the same field.
    Map<String, FilterDef> waiver = IndexQueryFilterSchema.forQueryType(IndexQueryType.WAIVER);
    assertThat(waiver.get("applicationIds")).isEqualTo(new FilterDef("applicationId", Kind.TERMS));
  }

  @Test
  public void waiver_policyIds_alreadyMatchesThePolicyFacetsAggregationField() {
    // The WAIVER policy facet aggregates on policyWaiverPolicyId; policyIds (and the deprecated
    // policies alias) must compile to the same field.
    Map<String, FilterDef> waiver = IndexQueryFilterSchema.forQueryType(IndexQueryType.WAIVER);
    assertThat(waiver.get("policyIds")).isEqualTo(new FilterDef("policyWaiverPolicyId", Kind.TERMS));
    assertThat(waiver.get("policies")).isEqualTo(new FilterDef("policyWaiverPolicyId", Kind.TERMS));
  }
}
