/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import org.apache.commons.lang3.StringUtils;

import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

/**
 * Maps an IQ-index {@link SearchResultItemDTO} into the discriminated-union {@link IndexQueryRow}
 * for the requested {@link IndexQueryType}.
 */
public final class IndexQueryRowMapper
{
  private IndexQueryRowMapper() {
  }

  /** Returns {@code null} when the row lacks the identifying field for its entity type. */
  public static IndexQueryRow toRow(final IndexQueryType queryType, final SearchResultItemDTO dto) {
    return switch (queryType) {
      case APPLICATION -> application(dto);
      case VIOLATION -> violation(dto);
      case POLICY -> policy(dto);
    };
  }

  private static IndexQueryRow application(final SearchResultItemDTO d) {
    final String id = d.applicationPublicId != null ? d.applicationPublicId : d.applicationId;
    if (id == null) {
      return null;
    }
    return base(IndexQueryType.APPLICATION, id)
        .title(d.applicationName)
        .subtitle(d.organizationName)
        .field("organizationName", d.organizationName)
        .field("organizationId", d.organizationId)
        .field("applicationName", d.applicationName)
        .field("applicationPublicId", d.applicationPublicId)
        .field("applicationId", d.applicationId)
        .field("policyEvaluationStage", d.policyEvaluationStage)
        .field("maxPolicyThreatLevel", d.policyThreatLevel)
        .href(d.applicationPublicId != null ? "/assets/index.html#/applicationReport/" + d.applicationPublicId : null)
        .build();
  }

  private static IndexQueryRow violation(final SearchResultItemDTO d) {
    // POLICY_VIOLATION populates policy fields, LEGAL_VIOLATION populates license fields; expose whichever is present.
    final boolean legal = ItemType.LEGAL_VIOLATION.name().equalsIgnoreCase(d.itemType);
    // policyViolationId is the only unique per-violation id. componentName is a display name shared by
    // multiple violations on the same component, so falling back to it would collide row ids downstream;
    // drop the row instead (surfaced as an explicit dropped-row warning by the caller).
    final String id = StringUtils.isNotBlank(d.policyViolationId) ? d.policyViolationId : null;
    if (id == null) {
      return null;
    }
    final String policyName =
        d.policyViolationPolicyName != null ? d.policyViolationPolicyName : d.componentLicenseThreatGroupName;
    return base(IndexQueryType.VIOLATION, id)
        .title(policyName)
        .subtitle(d.applicationName)
        .field("threat", legal ? d.componentLicenseThreatLevel : d.policyViolationThreatLevel)
        .field("policyName", policyName)
        .field("policyType", d.policyViolationThreatCategory)
        .field("policyId", d.policyViolationPolicyId)
        .field("organizationName", d.organizationName)
        .field("organizationId", d.organizationId)
        .field("applicationName", d.applicationName)
        .field("applicationId", d.applicationId)
        .field("componentName", d.componentName)
        .field("waiverStatus", d.policyViolationWaiverStatus)
        .field("effectiveLicense", d.componentEffectiveLicenseName)
        .href(d.policyViolationId != null ? "/preview/policyViolation/" + d.policyViolationId : null)
        .build();
  }

  private static IndexQueryRow policy(final SearchResultItemDTO d) {
    final String id = d.policyId;
    if (id == null) {
      return null;
    }
    // A policy is owned by exactly one context, so at most one of organizationId/applicationId is set.
    return base(IndexQueryType.POLICY, id)
        .title(d.policyName)
        .subtitle(d.organizationName)
        .field("name", d.policyName)
        .field("ownerId", d.organizationId != null ? d.organizationId : d.applicationId)
        .field("ownerType",
            d.organizationId != null ? "ORGANIZATION" : (d.applicationId != null ? "APPLICATION" : null))
        .field("threatLevel", d.policyThreatLevel)
        .field("policyType", d.policyThreatCategory)
        .field("organizationName", d.organizationName)
        .build();
  }

  private static IndexQueryRow.Builder base(final IndexQueryType queryType, final String id) {
    return IndexQueryRow.builder()
        .entityType(queryType.name())
        .source(SearchSource.LOCAL.value())
        .id(id);
  }
}
