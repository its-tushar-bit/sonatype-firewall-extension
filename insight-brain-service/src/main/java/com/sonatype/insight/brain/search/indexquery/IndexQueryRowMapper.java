/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.search.global.SearchSource;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.indexquery.ApplicationStageSeverityBreakdown.Breakdown;
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
      case WAIVER -> waiver(dto);
    };
  }

  private static IndexQueryRow application(final SearchResultItemDTO d) {
    // publicId is the stable, human-readable id; applicationId is a fallback for pre-publicId docs.
    final String id = d.applicationPublicId != null ? d.applicationPublicId : d.applicationId;
    if (id == null) {
      return null;
    }
    final Breakdown breakdown = ApplicationStageSeverityBreakdown.parse(d.applicationStageSeverityCounts);
    return base(IndexQueryType.APPLICATION, id)
        .title(d.applicationName)
        .subtitle(d.organizationName)
        .field("organizationName", d.organizationName)
        .field("organizationId", d.organizationId)
        .field("applicationName", d.applicationName)
        .field("applicationPublicId", d.applicationPublicId)
        .field("applicationId", d.applicationId)
        .field("applicationCategories", d.applicationCategoryNames)
        .field("maxPolicyThreatLevel", d.policyThreatLevel)
        .field("lastEvaluationTimeEpochMs", d.applicationLastEvaluationTimeEpochMs)
        .field("stageSeverityBreakdown", breakdown == null ? null : breakdown.stages())
        .field("totalRisk", breakdown == null ? null : breakdown.totalRisk())
        // Stable, chrome-keeping deep link: the server-side ui/links redirect resolves the owning
        // app's management landing rather than a Classic bundle path or a stale /preview route.
        .href(applicationHref(d))
        .build();
  }

  private static String applicationHref(final SearchResultItemDTO d) {
    if (d.applicationId == null) {
      return null;
    }
    return "/" + UserInterfaceLinksHelper.getManagementPath("application", d.applicationId, false);
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
    final String componentVersion = componentVersion(d);
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
        .field("applicationCategories", d.applicationCategoryNames)
        .field("componentName", d.componentName)
        .field("componentVersion", componentVersion)
        .field("stage", d.policyEvaluationStage)
        .field("waiverStatus", d.policyViolationWaiverStatus)
        .field("state", IndexQueryWaiverStatus.toState(d.policyViolationWaiverStatus))
        .field("waiverType", IndexQueryWaiverStatus.toWaiverType(d.policyViolationWaiverStatus))
        .field("effectiveLicense", d.componentEffectiveLicenseName)
        // Stable, chrome-keeping deep link to the violation detail (ui/links redirect), not /preview.
        .href(d.policyViolationId != null
            ? "/" + UserInterfaceLinksHelper.getPolicyViolationDetailsUrl(d.policyViolationId)
            : null)
        .build();
  }

  private static String componentVersion(final SearchResultItemDTO d) {
    if (d.componentIdentifier == null || d.componentIdentifier.getCoordinates() == null) {
      return null;
    }
    return d.componentIdentifier.getCoordinates().get("version");
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

  private static IndexQueryRow waiver(final SearchResultItemDTO d) {
    final String id = d.policyWaiverId;
    if (id == null) {
      return null;
    }
    // Auto/manual is read from the indexed policyWaiverAuto discriminator. Older pre-reindex docs may
    // lack the field (null); treat those as manual until a full reindex populates it.
    final boolean auto = Boolean.TRUE.equals(d.policyWaiverAuto);
    // Manual waivers keep their real policy name. Auto-waivers carry no indexed policy name; synthesize
    // the display title here so the label is never indexed (not text-searchable, not matched by the
    // policy filter) and can change without a reindex. A manual waiver whose policy cannot be resolved
    // (orphaned policy, per buildPolicyWaiverDocs) also indexes a null policy name, so fall back to a
    // generic label there too rather than rendering a blank row title.
    final String title = waiverTitle(auto, d.policyWaiverPolicyName, d.policyWaiverThreatLevel);
    return base(IndexQueryType.WAIVER, id)
        .title(title)
        .subtitle(waiverScopeOwnerName(d))
        .field("policyName", d.policyWaiverPolicyName)
        .field("policyId", d.policyWaiverPolicyId)
        .field("reason", d.policyWaiverReason)
        .field("threatLevel", d.policyWaiverThreatLevel)
        .field("createdAt", d.policyWaiverCreatedAt)
        .field("expiresAt", d.policyWaiverExpiresAt)
        .field("scopeOwnerId", d.policyWaiverScopeOwnerId)
        .field("scopeOwnerType", d.policyWaiverScopeOwnerType)
        .field("waivedBy", d.policyWaiverWaivedBy)
        .field("auto", auto)
        .field("organizationName", d.organizationName)
        .href(waiverHref(d))
        .build();
  }

  /**
   * Human-meaningful subtitle: the scope owner's display name. {@code setOwner} on the indexer writes
   * applicationName for APPLICATION-scoped waivers and organizationName for ORGANIZATION-scoped ones,
   * so pick whichever the scope type indicates and fall back to the other when it is absent.
   */
  private static String waiverScopeOwnerName(final SearchResultItemDTO d) {
    final boolean appScoped = "APPLICATION".equalsIgnoreCase(d.policyWaiverScopeOwnerType);
    final String preferred = appScoped ? d.applicationName : d.organizationName;
    return preferred != null ? preferred : (appScoped ? d.organizationName : d.applicationName);
  }

  /**
   * Deep link to the waiver-detail page, mirroring VIOLATION's {@code /preview/policyViolation/{id}}
   * form. The route is {@code /preview/waivers/{ownerType}/{ownerId}/{waiverId}} with a lowercase
   * ownerType, but the index stores {@code OwnerType.name()} (uppercase), so lower it here. Requires
   * all three parts; returns {@code null} otherwise so the row carries no broken link.
   */
  private static String waiverHref(final SearchResultItemDTO d) {
    if (d.policyWaiverScopeOwnerType == null || d.policyWaiverScopeOwnerId == null || d.policyWaiverId == null) {
      return null;
    }
    final String ownerType = d.policyWaiverScopeOwnerType.toLowerCase(Locale.ROOT);
    return "/preview/waivers/" + ownerType + "/" + encodePathSegment(d.policyWaiverScopeOwnerId) + "/"
        + encodePathSegment(d.policyWaiverId);
  }

  // Ids are opaque and safe today; encode anyway as defense-in-depth so a value can never break out
  // of its path segment. URLEncoder targets query strings, so restore the space encoding to %20.
  private static String encodePathSegment(final String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private static String waiverTitle(final boolean auto, final String policyName, final Integer threatLevel) {
    if (StringUtils.isNotBlank(policyName)) {
      return policyName;
    }
    return auto ? syntheticAutoWaiverTitle(threatLevel) : "Waiver";
  }

  private static String syntheticAutoWaiverTitle(final Integer threatLevel) {
    return threatLevel != null ? "Auto-waiver (threat >= " + threatLevel + ")" : "Auto-waiver";
  }

  private static IndexQueryRow.Builder base(final IndexQueryType queryType, final String id) {
    return IndexQueryRow.builder()
        .entityType(queryType.name())
        .source(SearchSource.LOCAL.value())
        .id(id);
  }
}
