/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { UIView } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import ComponentLegalOverviewContainer from '../ComponentLegalOverviewContainer';
import LegalApplicationDetailsContainer from '../application/LegalApplicationDetailsContainer';
import AttributionReportForm from '../application/AttributionReportFormContainer';
import AttributionReportTemplateForm from '../application/AttributionReportTemplateFormContainer';
import ComponentCopyrightDetails from '../copyright/ComponentCopyrightDetails';
import CopyrightDetailsContentsContainer from '../copyright/CopyrightDetailsContentsContainer';
import NoticeDetailsContentsContainer from '../files/notices/NoticeDetailsContentsContainer';
import LegalDashboardContainer from '../dashboard/LegalDashboardContainer';
import ComponentLicenseDetailsContainer from '../license/ComponentLicenseDetailsContainer';
import ComponentNoticeDetails from '../files/notices/ComponentNoticeDetails';
import ComponentLicenseFilesDetails from '../files/licenses/ComponentLicenseFilesDetails';
import LicenseFilesDetailsContentsContainer from '../files/licenses/LicenseFilesDetailsContentsContainer';

const parentRoute = 'sbomManager.legal';

// Abstract parent state for SBOM Manager legal routes
router.stateRegistry.register({
  name: parentRoute,
  abstract: true,
  component: UIView,
});

router.stateRegistry.register({
  name: `${parentRoute}.dashboard`,
  url: '/legal/dashboard',
  component: LegalDashboardContainer,
  data: {
    title: 'Legal Dashboard',
    activeTab: 'applications',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.applicationsDashboard`,
  url: '/legal/applicationsDashboard',
  component: LegalDashboardContainer,
  data: {
    title: 'Legal Dashboard',
    activeTab: 'applications',
    disableCreateAttributionReportBtn: false,
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.componentsDashboard`,
  url: '/legal/componentsDashboard',
  component: LegalDashboardContainer,
  data: {
    title: 'Legal Dashboard',
    activeTab: 'components',
    disableCreateAttributionReportBtn: true,
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.componentOverview`,
  url: '/legal/component/{hash}',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.componentOverviewByComponentIdentifier`,
  url: '/legal/component/componentIdentifier/{componentIdentifier}/repository/{repositoryId}',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.applicationComponentOverviewByComponentIdentifier`,
  url:
    '/legal/component/componentIdentifier/{componentIdentifier}/application/{applicationPublicId}' +
    '/component/{hash}/scan/{scanId}/{tabId}',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.noticeFilesByComponentIdentifier`,
  url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/notices',
  component: ComponentNoticeDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: `${parentRoute}.noticeFilesByComponentIdentifier.noticeDetails`,
  url: '/{noticeIndex}',
  component: NoticeDetailsContentsContainer,
  data: {
    title: 'Notice Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.organizationComponentOverview`,
  url: '/legal/organization/{organizationId}/component/{hash}',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.applicationComponentOverview`,
  url: '/legal/application/{applicationPublicId}/component/{hash}',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.applicationStageTypeComponentOverview`,
  url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/{hash}',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.applicationDetails`,
  url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}',
  component: LegalApplicationDetailsContainer,
  data: {
    title: 'Application Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.attributionReport`,
  url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/attributionReport',
  component: AttributionReportForm,
  data: {
    title: 'Attribution Report',
    isDirty: ['attributionReports', 'attributionReports', 'isFormDirty'],
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.attributionReportMultiApp`,
  url: '/legal/application/attributionReport',
  component: AttributionReportForm,
  data: {
    title: 'Attribution Report (Multiple Applications)',
    isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
    isMultiApp: true,
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.attributionReportTemplate`,
  url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/attributionReportTemplate',
  component: AttributionReportTemplateForm,
  data: {
    title: 'Attribution Report Templates',
    isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.attributionReportTemplateMultiApp`,
  url: '/legal/application/attributionReportTemplate',
  component: AttributionReportTemplateForm,
  data: {
    title: 'Attribution Report Templates',
    isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
    isMultiApp: true,
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.componentCopyrightDetails`,
  url: '/legal/{ownerType}/{ownerId}/component/{hash}/copyrights',
  component: ComponentCopyrightDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: `${parentRoute}.componentCopyrightDetails.copyrightDetails`,
  url: '/{copyrightIndex}',
  component: CopyrightDetailsContentsContainer,
  data: {
    title: 'Copyright Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.componentCopyrightDetailsByComponentIdentifier`,
  url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/copyrights',
  component: ComponentCopyrightDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: `${parentRoute}.componentCopyrightDetailsByComponentIdentifier.copyrightDetails`,
  url: '/{copyrightIndex}',
  component: CopyrightDetailsContentsContainer,
  data: {
    title: 'Copyright Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.stageTypeComponentCopyrightDetails`,
  url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/copyrights',
  component: ComponentCopyrightDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: `${parentRoute}.stageTypeComponentCopyrightDetails.copyrightDetails`,
  url: '/{copyrightIndex}',
  component: CopyrightDetailsContentsContainer,
  data: {
    title: 'Copyright Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.componentNoticeDetails`,
  url: '/legal/{ownerType}/{ownerId}/component/{hash}/notices',
  component: ComponentNoticeDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: `${parentRoute}.componentNoticeDetails.noticeDetails`,
  url: '/{noticeIndex}',
  component: NoticeDetailsContentsContainer,
  data: {
    title: 'Notice Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.stageTypeComponentNoticeDetails`,
  url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/notices',
  component: ComponentNoticeDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: `${parentRoute}.stageTypeComponentNoticeDetails.noticeDetails`,
  url: '/{noticeIndex}',
  component: NoticeDetailsContentsContainer,
  data: {
    title: 'Notice Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.componentLicenseFilesDetails`,
  url: '/legal/{ownerType}/{ownerId}/component/{hash}/licenseFiles',
  component: ComponentLicenseFilesDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: `${parentRoute}.componentLicenseFilesDetails.licenseFilesDetails`,
  url: '/{licenseIndex}',
  component: LicenseFilesDetailsContentsContainer,
  data: {
    title: 'License Files Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.componentLicenseFilesDetailsByComponentIdentifier`,
  url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/licenseFiles',
  component: ComponentLicenseFilesDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: `${parentRoute}.componentLicenseFilesDetailsByComponentIdentifier.licenseFilesDetails`,
  url: '/{licenseIndex}',
  component: LicenseFilesDetailsContentsContainer,
  data: {
    title: 'License Files Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.stageTypeComponentLicenseFilesDetails`,
  url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/licenseFiles',
  component: ComponentLicenseFilesDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: `${parentRoute}.stageTypeComponentLicenseFilesDetails.licenseFilesDetails`,
  url: '/{licenseIndex}',
  component: LicenseFilesDetailsContentsContainer,
  data: {
    title: 'License Files Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.componentLicenseDetails`,
  url: '/legal/{ownerType}/{ownerId}/component/{hash}/licenses/{licenseIndex}',
  component: ComponentLicenseDetailsContainer,
  data: {
    title: 'Component - License Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.componentLicenseDetailsByComponentIdentifier`,
  url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/licenses/{licenseIndex}',
  component: ComponentLicenseDetailsContainer,
  data: {
    title: 'Component - License Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.componentLicenseDetailsByComponentIdentifierAndHashAndScanId`,
  url:
    '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/component/{hash}/scan/{scanId}' +
    '/licenses/{licenseIndex}',
  component: ComponentLicenseDetailsContainer,
  data: {
    title: 'Component - License Details',
  },
});

router.stateRegistry.register({
  name: `${parentRoute}.stageTypeComponentLicenseDetails`,
  url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/licenses/{licenseIndex}',
  component: ComponentLicenseDetailsContainer,
  data: {
    title: 'Component - License Details',
  },
});
