/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  SearchResultItemDTO,
  isApplication,
  isOrganization,
  isComponent,
  isVulnerability,
  isPolicy,
  isPolicyViolation,
  isWaiver,
  isSbomMetadata,
} from 'MainRoot/nosc/search/searchTypes';
import { classicHref, violationSidebarHref } from 'MainRoot/nosc/applications/applicationDetailUtils';
import router from 'MainRoot/router/routerInstance';

/**
 * Maps a search result row to the URL the omnibar navigates to on click.
 *
 * Nexus One destinations are resolved through the UI-Router state registry
 * (`router.stateService.href`) so paths and params come from a single source of
 * truth rather than hand-built hash strings. Organizations, vulnerabilities, and
 * policies have no native Nexus One page yet, so they deep-link into Classic via
 * `bundleIndexUrl` (which is context-path / MTIQ-prefix aware).
 *
 * TODO(CLM-39549): replace the Classic deep-links (org / vuln / policy) and the
 * component / SBOM "home" fallbacks with native Nexus One detail routes as those
 * pages land, so global-search clicks keep users inside the Nexus One UI.
 */

export function clickHrefFor(result: SearchResultItemDTO): string {
  if (isApplication(result) && result.applicationPublicId) {
    return router.stateService.href('nexusOneApplicationsDetail.overview', {
      publicId: result.applicationPublicId,
    });
  }

  if (isOrganization(result) && result.organizationId) {
    return classicHref(
      `/management/view/organization/${encodeURIComponent(result.organizationId)}`,
    );
  }

  if (isVulnerability(result) && result.vulnerabilityId) {
    return classicHref(`/vulnerabilities/${encodeURIComponent(result.vulnerabilityId)}`);
  }

  if (isPolicy(result) && result.policyId) {
    return classicHref('/management/view/organization/ROOT_ORGANIZATION_ID');
  }

  // Policy violations and waivers both deep-link to the same Classic violation-detail sidebar.
  if ((isPolicyViolation(result) || isWaiver(result)) && result.policyViolationId) {
    return violationSidebarHref(result.policyViolationId);
  }

  // TODO(CLM-39549): no native Nexus One component / SBOM detail page exists yet;
  // land on the Nexus One home until those routes ship.
  if (isComponent(result) && result.componentHash) {
    return router.stateService.href('platformHome');
  }

  if (isSbomMetadata(result) && result.reportId) {
    return router.stateService.href('platformHome');
  }

  return router.stateService.href('platformHome');
}

/**
 * URL the user navigates to when they press Enter without selecting a row.
 * Goes to the Nexus One search route, carrying the current query as the `q`
 * state param so the destination can read it.
 */
export function enterSearchHref(query: string): string {
  const trimmed = query.trim();
  return router.stateService.href('nexusOneSearch', trimmed ? { q: trimmed } : {});
}
