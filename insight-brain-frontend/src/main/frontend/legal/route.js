/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { UIView } from '@uirouter/react';
import router from 'MainRoot/router/routerInstance';
import ComponentLegalOverviewContainer from './ComponentLegalOverviewContainer';
import LegalApplicationDetailsContainer from './application/LegalApplicationDetailsContainer';
import AttributionReportForm from './application/AttributionReportFormContainer';
import AttributionReportTemplateForm from './application/AttributionReportTemplateFormContainer';
import ComponentCopyrightDetails from './copyright/ComponentCopyrightDetails';
import CopyrightDetailsContentsContainer from './copyright/CopyrightDetailsContentsContainer';
import NoticeDetailsContentsContainer from './files/notices/NoticeDetailsContentsContainer';
import LegalDashboardContainer from './dashboard/LegalDashboardContainer';
import ComponentLicenseDetailsContainer from './license/ComponentLicenseDetailsContainer';
import ComponentNoticeDetails from './files/notices/ComponentNoticeDetails';
import ComponentLicenseFilesDetails from './files/licenses/ComponentLicenseFilesDetails';
import LicenseFilesDetailsContentsContainer from './files/licenses/LicenseFilesDetailsContentsContainer';

// Abstract parent state
router.stateRegistry.register({
  name: 'legal',
  abstract: true,
  component: UIView,
});

router.stateRegistry.register({
  name: 'legal.dashboard',
  url: '/legal/dashboard',
  component: LegalDashboardContainer,
  data: {
    title: 'Legal Dashboard',
    activeTab: 'applications',
  },
});

router.stateRegistry.register({
  name: 'legal.applicationsDashboard',
  url: '/legal/applicationsDashboard',
  component: LegalDashboardContainer,
  data: {
    title: 'Legal Dashboard',
    activeTab: 'applications',
    disableCreateAttributionReportBtn: false,
  },
});

router.stateRegistry.register({
  name: 'legal.componentsDashboard',
  url: '/legal/componentsDashboard',
  component: LegalDashboardContainer,
  data: {
    title: 'Legal Dashboard',
    activeTab: 'components',
    disableCreateAttributionReportBtn: true,
  },
});

router.stateRegistry.register({
  name: 'legal.componentOverview',
  url: '/legal/component/{hash}',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

router.stateRegistry.register({
  name: 'legal.componentOverviewByComponentIdentifier',
  url: '/legal/component/componentIdentifier/{componentIdentifier}/repository/{repositoryId}',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

router.stateRegistry.register({
  name: 'legal.applicationComponentOverviewByComponentIdentifier',
  url:
    '/legal/component/componentIdentifier/{componentIdentifier}/application/{applicationPublicId}' +
    '/component/{hash}/scan/{scanId}/{tabId}',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

router.stateRegistry.register({
  name: 'legal.noticeFilesByComponentIdentifier',
  url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/notices',
  component: ComponentNoticeDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: 'legal.noticeFilesByComponentIdentifier.noticeDetails',
  url: '/{noticeIndex}',
  component: NoticeDetailsContentsContainer,
  data: {
    title: 'Notice Details',
  },
});

router.stateRegistry.register({
  name: 'legal.organizationComponentOverview',
  url: '/legal/organization/{organizationId}/component/{hash}',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

router.stateRegistry.register({
  name: 'legal.applicationComponentOverview',
  url: '/legal/application/{applicationPublicId}/component/{hash}',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

router.stateRegistry.register({
  name: 'legal.applicationStageTypeComponentOverview',
  url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/{hash}',
  component: ComponentLegalOverviewContainer,
  data: {
    title: 'Component - Legal Overview',
  },
});

router.stateRegistry.register({
  name: 'legal.applicationDetails',
  url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}',
  component: LegalApplicationDetailsContainer,
  data: {
    title: 'Application Details',
  },
});

router.stateRegistry.register({
  name: 'legal.attributionReport',
  url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/attributionReport',
  component: AttributionReportForm,
  data: {
    title: 'Attribution Report',
    isDirty: ['attributionReports', 'attributionReports', 'isFormDirty'],
  },
});

router.stateRegistry.register({
  name: 'legal.attributionReportMultiApp',
  url: '/legal/application/attributionReport',
  component: AttributionReportForm,
  data: {
    title: 'Attribution Report (Multiple Applications)',
    isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
    isMultiApp: true,
  },
});

router.stateRegistry.register({
  name: 'legal.attributionReportTemplate',
  url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/attributionReportTemplate',
  component: AttributionReportTemplateForm,
  data: {
    title: 'Attribution Report Templates',
    isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
  },
});

router.stateRegistry.register({
  name: 'legal.attributionReportTemplateMultiApp',
  url: '/legal/application/attributionReportTemplate',
  component: AttributionReportTemplateForm,
  data: {
    title: 'Attribution Report Templates',
    isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
    isMultiApp: true,
  },
});

router.stateRegistry.register({
  name: 'legal.componentCopyrightDetails',
  url: '/legal/{ownerType}/{ownerId}/component/{hash}/copyrights',
  component: ComponentCopyrightDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: 'legal.componentCopyrightDetails.copyrightDetails',
  url: '/{copyrightIndex}',
  component: CopyrightDetailsContentsContainer,
  data: {
    title: 'Copyright Details',
    viewportSized: true,
  },
});

router.stateRegistry.register({
  name: 'legal.componentCopyrightDetailsByComponentIdentifier',
  url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/copyrights',
  component: ComponentCopyrightDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: 'legal.componentCopyrightDetailsByComponentIdentifier.copyrightDetails',
  url: '/{copyrightIndex}',
  component: CopyrightDetailsContentsContainer,
  data: {
    title: 'Copyright Details',
    viewportSized: true,
  },
});

router.stateRegistry.register({
  name: 'legal.stageTypeComponentCopyrightDetails',
  url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/copyrights',
  component: ComponentCopyrightDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: 'legal.stageTypeComponentCopyrightDetails.copyrightDetails',
  url: '/{copyrightIndex}',
  component: CopyrightDetailsContentsContainer,
  data: {
    title: 'Copyright Details',
    viewportSized: true,
  },
});

router.stateRegistry.register({
  name: 'legal.componentNoticeDetails',
  url: '/legal/{ownerType}/{ownerId}/component/{hash}/notices',
  component: ComponentNoticeDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: 'legal.componentNoticeDetails.noticeDetails',
  url: '/{noticeIndex}',
  component: NoticeDetailsContentsContainer,
  data: {
    title: 'Notice Details',
  },
});

router.stateRegistry.register({
  name: 'legal.stageTypeComponentNoticeDetails',
  url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/notices',
  component: ComponentNoticeDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: 'legal.stageTypeComponentNoticeDetails.noticeDetails',
  url: '/{noticeIndex}',
  component: NoticeDetailsContentsContainer,
  data: {
    title: 'Notice Details',
  },
});

router.stateRegistry.register({
  name: 'legal.componentLicenseFilesDetails',
  url: '/legal/{ownerType}/{ownerId}/component/{hash}/licenseFiles',
  component: ComponentLicenseFilesDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: 'legal.componentLicenseFilesDetails.licenseFilesDetails',
  url: '/{licenseIndex}',
  component: LicenseFilesDetailsContentsContainer,
  data: {
    title: 'License Files Details',
  },
});

router.stateRegistry.register({
  name: 'legal.componentLicenseFilesDetailsByComponentIdentifier',
  url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/licenseFiles',
  component: ComponentLicenseFilesDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: 'legal.componentLicenseFilesDetailsByComponentIdentifier.licenseFilesDetails',
  url: '/{licenseIndex}',
  component: LicenseFilesDetailsContentsContainer,
  data: {
    title: 'License Files Details',
  },
});

router.stateRegistry.register({
  name: 'legal.stageTypeComponentLicenseFilesDetails',
  url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/licenseFiles',
  component: ComponentLicenseFilesDetails,
  abstract: true,
});

router.stateRegistry.register({
  name: 'legal.stageTypeComponentLicenseFilesDetails.licenseFilesDetails',
  url: '/{licenseIndex}',
  component: LicenseFilesDetailsContentsContainer,
  data: {
    title: 'License Files Details',
  },
});

router.stateRegistry.register({
  name: 'legal.componentLicenseDetails',
  url: '/legal/{ownerType}/{ownerId}/component/{hash}/licenses/{licenseIndex}',
  component: ComponentLicenseDetailsContainer,
  data: {
    title: 'Component - License Details',
  },
});

router.stateRegistry.register({
  name: 'legal.componentLicenseDetailsByComponentIdentifier',
  url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/licenses/{licenseIndex}',
  component: ComponentLicenseDetailsContainer,
  data: {
    title: 'Component - License Details',
  },
});

router.stateRegistry.register({
  name: 'legal.componentLicenseDetailsByComponentIdentifierAndHashAndScanId',
  url:
    '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/component/{hash}/scan/{scanId}' +
    '/licenses/{licenseIndex}',
  component: ComponentLicenseDetailsContainer,
  data: {
    title: 'Component - License Details',
  },
});

router.stateRegistry.register({
  name: 'legal.stageTypeComponentLicenseDetails',
  url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/licenses/{licenseIndex}',
  component: ComponentLicenseDetailsContainer,
  data: {
    title: 'Component - License Details',
  },
});
