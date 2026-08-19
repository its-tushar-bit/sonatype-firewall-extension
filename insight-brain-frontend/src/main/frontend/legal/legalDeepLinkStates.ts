/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import LegalDashboardContainer from './dashboard/LegalDashboardContainer';
import ComponentLegalOverviewContainer from './ComponentLegalOverviewContainer';
import LegalApplicationDetailsContainer from './application/LegalApplicationDetailsContainer';
import AttributionReportForm from './application/AttributionReportFormContainer';
import AttributionReportTemplateForm from './application/AttributionReportTemplateFormContainer';
import ComponentCopyrightDetails from './copyright/ComponentCopyrightDetails';
import CopyrightDetailsContentsContainer from './copyright/CopyrightDetailsContentsContainer';
import ComponentNoticeDetails from './files/notices/ComponentNoticeDetails';
import NoticeDetailsContentsContainer from './files/notices/NoticeDetailsContentsContainer';
import ComponentLicenseFilesDetails from './files/licenses/ComponentLicenseFilesDetails';
import LicenseFilesDetailsContentsContainer from './files/licenses/LicenseFilesDetailsContentsContainer';
import ComponentLicenseDetailsContainer from './license/ComponentLicenseDetailsContainer';
import {
  LEGAL_APPLICATION_DETAILS_URL,
  LEGAL_COMPONENT_OVERVIEW_URL,
  LEGAL_DASHBOARD_TITLE,
} from './dashboard/legalDashboardRouteData';

/**
 * Single source of truth for every `legal.*` state beyond the dashboard and its two tabs (those
 * live in legalDashboardRouteData.ts / nexus-one/routes.tsx directly, since they need
 * per-tab `data` and a shared mount reference the generic Coming Soon loop also touches).
 *
 * Classic's `legal/route.js` and the Nexus One embedded mount (`nexus-one/routes.tsx`) both
 * register every one of these under their own bundle's router instance, from this one array, so
 * the two can't drift apart the way earlier rounds of this ticket found (see the git history on
 * CLM-42162) — a state added here only once still needs registering in the OTHER file's forEach
 * call, but the name/url/component/data can never mismatch between the two.
 *
 * `LegalDeepLinkStateDef.abstract` states render a Classic component that itself contains a
 * nested `<UIView />` for its dotted children (confirmed for each below) — in the Nexus One
 * bundle those get wrapped in ClassicComponentMount once, at the parent; their children are
 * registered with their own component *unwrapped*, since they render inside the parent's own
 * UIView, not as a second top-level mount. Getting this backwards double-wraps the page in two
 * nested ClassicComponentMounts.
 */
export interface LegalDeepLinkStateDef {
  readonly name: string;
  readonly url: string;
  readonly component: React.ComponentType;
  readonly data?: Record<string, unknown>;
  readonly abstract?: boolean;
}

export const LEGAL_DEEP_LINK_STATES: readonly LegalDeepLinkStateDef[] = [
  // Classic's own nav-entry target (see IqSidebarNav.jsx's legalHref) — kept distinct from
  // legal.applicationsDashboard so $state.href('legal.dashboard') (e.g. Back buttons on the
  // deep-link pages below) resolves to a real url instead of throwing.
  {
    name: 'legal.dashboard',
    url: '/legal/dashboard',
    component: LegalDashboardContainer,
    data: { title: LEGAL_DASHBOARD_TITLE, activeTab: 'applications' },
  },

  // Application details — the row-click target from the dashboard's Applications tab.
  {
    name: 'legal.applicationDetails',
    url: LEGAL_APPLICATION_DETAILS_URL,
    component: LegalApplicationDetailsContainer,
    data: { title: 'Application Details' },
  },

  // Component overview — reused across every entry-point shape (by hash, by identifier, scoped
  // to an org/application/stage). Registering all of them with the same component means
  // navigating between them (e.g. a Back link) never remounts. legal.componentOverview itself is
  // the row-click target from the dashboard's Components tab.
  {
    name: 'legal.componentOverview',
    url: LEGAL_COMPONENT_OVERVIEW_URL,
    component: ComponentLegalOverviewContainer,
    data: { title: 'Component - Legal Overview' },
  },
  {
    name: 'legal.componentOverviewByComponentIdentifier',
    url: '/legal/component/componentIdentifier/{componentIdentifier}/repository/{repositoryId}',
    component: ComponentLegalOverviewContainer,
    data: { title: 'Component - Legal Overview' },
  },
  {
    name: 'legal.applicationComponentOverviewByComponentIdentifier',
    url:
      '/legal/component/componentIdentifier/{componentIdentifier}/application/{applicationPublicId}' +
      '/component/{hash}/scan/{scanId}/{tabId}?identificationSource',
    component: ComponentLegalOverviewContainer,
    data: { title: 'Component - Legal Overview' },
  },
  {
    name: 'legal.organizationComponentOverview',
    url: '/legal/organization/{organizationId}/component/{hash}',
    component: ComponentLegalOverviewContainer,
    data: { title: 'Component - Legal Overview' },
  },
  {
    name: 'legal.applicationComponentOverview',
    url: '/legal/application/{applicationPublicId}/component/{hash}',
    component: ComponentLegalOverviewContainer,
    data: { title: 'Component - Legal Overview' },
  },
  {
    name: 'legal.applicationStageTypeComponentOverview',
    url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/{hash}',
    component: ComponentLegalOverviewContainer,
    data: { title: 'Component - Legal Overview' },
  },

  // Attribution report generation (single app + multi-app; form + template variants).
  {
    name: 'legal.attributionReport',
    url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/attributionReport',
    component: AttributionReportForm,
    data: {
      title: 'Attribution Report',
      isDirty: ['attributionReports', 'attributionReports', 'isFormDirty'],
    },
  },
  {
    name: 'legal.attributionReportMultiApp',
    url: '/legal/application/attributionReport',
    component: AttributionReportForm,
    data: {
      title: 'Attribution Report (Multiple Applications)',
      isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
      isMultiApp: true,
    },
  },
  {
    name: 'legal.attributionReportTemplate',
    url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/attributionReportTemplate',
    component: AttributionReportTemplateForm,
    data: {
      title: 'Attribution Report Templates',
      isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
    },
  },
  {
    name: 'legal.attributionReportTemplateMultiApp',
    url: '/legal/application/attributionReportTemplate',
    component: AttributionReportTemplateForm,
    data: {
      title: 'Attribution Report Templates',
      isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
      isMultiApp: true,
    },
  },

  // Copyright details — abstract parent renders <UIView /> itself; see module doc comment.
  {
    name: 'legal.componentCopyrightDetails',
    url: '/legal/{ownerType}/{ownerId}/component/{hash}/copyrights',
    component: ComponentCopyrightDetails,
    abstract: true,
  },
  {
    name: 'legal.componentCopyrightDetails.copyrightDetails',
    url: '/{copyrightIndex}',
    component: CopyrightDetailsContentsContainer,
    data: { title: 'Copyright Details', viewportSized: true },
  },
  {
    name: 'legal.componentCopyrightDetailsByComponentIdentifier',
    url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/copyrights',
    component: ComponentCopyrightDetails,
    abstract: true,
  },
  {
    name: 'legal.componentCopyrightDetailsByComponentIdentifier.copyrightDetails',
    url: '/{copyrightIndex}',
    component: CopyrightDetailsContentsContainer,
    data: { title: 'Copyright Details', viewportSized: true },
  },
  {
    name: 'legal.stageTypeComponentCopyrightDetails',
    url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/copyrights',
    component: ComponentCopyrightDetails,
    abstract: true,
  },
  {
    name: 'legal.stageTypeComponentCopyrightDetails.copyrightDetails',
    url: '/{copyrightIndex}',
    component: CopyrightDetailsContentsContainer,
    data: { title: 'Copyright Details', viewportSized: true },
  },

  // Notice details — same abstract-parent-with-UIView shape as copyright details above.
  {
    name: 'legal.noticeFilesByComponentIdentifier',
    url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/notices',
    component: ComponentNoticeDetails,
    abstract: true,
  },
  {
    name: 'legal.noticeFilesByComponentIdentifier.noticeDetails',
    url: '/{noticeIndex}',
    component: NoticeDetailsContentsContainer,
    data: { title: 'Notice Details' },
  },
  {
    name: 'legal.componentNoticeDetails',
    url: '/legal/{ownerType}/{ownerId}/component/{hash}/notices',
    component: ComponentNoticeDetails,
    abstract: true,
  },
  {
    name: 'legal.componentNoticeDetails.noticeDetails',
    url: '/{noticeIndex}',
    component: NoticeDetailsContentsContainer,
    data: { title: 'Notice Details' },
  },
  {
    name: 'legal.stageTypeComponentNoticeDetails',
    url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/notices',
    component: ComponentNoticeDetails,
    abstract: true,
  },
  {
    name: 'legal.stageTypeComponentNoticeDetails.noticeDetails',
    url: '/{noticeIndex}',
    component: NoticeDetailsContentsContainer,
    data: { title: 'Notice Details' },
  },

  // License files details — same abstract-parent-with-UIView shape again.
  {
    name: 'legal.componentLicenseFilesDetails',
    url: '/legal/{ownerType}/{ownerId}/component/{hash}/licenseFiles',
    component: ComponentLicenseFilesDetails,
    abstract: true,
  },
  {
    name: 'legal.componentLicenseFilesDetails.licenseFilesDetails',
    url: '/{licenseIndex}',
    component: LicenseFilesDetailsContentsContainer,
    data: { title: 'License Files Details' },
  },
  {
    name: 'legal.componentLicenseFilesDetailsByComponentIdentifier',
    url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/licenseFiles',
    component: ComponentLicenseFilesDetails,
    abstract: true,
  },
  {
    name: 'legal.componentLicenseFilesDetailsByComponentIdentifier.licenseFilesDetails',
    url: '/{licenseIndex}',
    component: LicenseFilesDetailsContentsContainer,
    data: { title: 'License Files Details' },
  },
  {
    name: 'legal.stageTypeComponentLicenseFilesDetails',
    url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/licenseFiles',
    component: ComponentLicenseFilesDetails,
    abstract: true,
  },
  {
    name: 'legal.stageTypeComponentLicenseFilesDetails.licenseFilesDetails',
    url: '/{licenseIndex}',
    component: LicenseFilesDetailsContentsContainer,
    data: { title: 'License Files Details' },
  },

  // License details — flat pages (no nested UIView), reused across every entry-point shape.
  {
    name: 'legal.componentLicenseDetails',
    url: '/legal/{ownerType}/{ownerId}/component/{hash}/licenses/{licenseIndex}',
    component: ComponentLicenseDetailsContainer,
    data: { title: 'Component - License Details' },
  },
  {
    name: 'legal.componentLicenseDetailsByComponentIdentifier',
    url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/licenses/{licenseIndex}',
    component: ComponentLicenseDetailsContainer,
    data: { title: 'Component - License Details' },
  },
  {
    name: 'legal.componentLicenseDetailsByComponentIdentifierAndHashAndScanId',
    url:
      '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/component/{hash}/scan/{scanId}' +
      '/licenses/{licenseIndex}',
    component: ComponentLicenseDetailsContainer,
    data: { title: 'Component - License Details' },
  },
  {
    name: 'legal.stageTypeComponentLicenseDetails',
    url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/licenses/{licenseIndex}',
    component: ComponentLicenseDetailsContainer,
    data: { title: 'Component - License Details' },
  },
];
