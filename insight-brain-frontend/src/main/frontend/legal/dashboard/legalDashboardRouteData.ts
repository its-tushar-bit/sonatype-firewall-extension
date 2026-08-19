/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * UI-Router state `url`/`data` for the Legal Dashboard's two tab states. Classic
 * (`legal/route.js`) and the Nexus One embedded mount (`nexus-one/routes.tsx`) each
 * register their own copy of these states in their own bundle's router instance;
 * sharing the literals here keeps the two from drifting apart. `LeftNav.jsx`'s
 * `LEGAL_ACTIVE_HREFS` also reuses the URL constants so the rail-highlight paths
 * can't drift from the routes they're meant to match.
 */
export const LEGAL_DASHBOARD_TITLE = 'Legal Dashboard';

export const LEGAL_APPLICATIONS_DASHBOARD_URL = '/legal/applicationsDashboard';
export const LEGAL_COMPONENTS_DASHBOARD_URL = '/legal/componentsDashboard';

/**
 * URL templates for the two states LegalDashboardApplicationRow/LegalDashboardComponentRow
 * stateGo() to on row click. Registered in legalDeepLinkStates.ts with the same real Classic
 * components (LegalApplicationDetailsContainer / ComponentLegalOverviewContainer) Classic itself
 * uses, mounted in-shell via nexus-one/routes.tsx's mountLegalComponentOnce — not a hard-redirect
 * to Classic. Sharing the literals here just keeps the url templates themselves from drifting
 * between the two bundles, same as the dashboard URLs above.
 */
export const LEGAL_APPLICATION_DETAILS_URL = '/legal/application/{applicationPublicId}/stage/{stageTypeId}';
export const LEGAL_COMPONENT_OVERVIEW_URL = '/legal/component/{hash}';

export const LEGAL_APPLICATIONS_DASHBOARD_DATA = {
  title: LEGAL_DASHBOARD_TITLE,
  activeTab: 'applications',
  disableCreateAttributionReportBtn: false,
} as const;

export const LEGAL_COMPONENTS_DASHBOARD_DATA = {
  title: LEGAL_DASHBOARD_TITLE,
  activeTab: 'components',
  disableCreateAttributionReportBtn: true,
} as const;
