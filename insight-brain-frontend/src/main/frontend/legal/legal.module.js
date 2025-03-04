/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import ComponentLegalOverviewContainer from './ComponentLegalOverviewContainer';
import LegalApplicationDetailsContainer from './application/LegalApplicationDetailsContainer';
import AttributionReportForm from './application/AttributionReportFormContainer';
import AttributionReportTemplateForm from './application/AttributionReportTemplateFormContainer';
import componentCopyrightDetails from './copyright/componentCopyrightDetails';
import CopyrightDetailsHeaderContainer from './copyright/CopyrightDetailsHeaderContainer';
import CopyrightListContainer from './copyright/CopyrightListContainer';
import CopyrightDetailsContentsContainer from './copyright/CopyrightDetailsContentsContainer';
import NoticeDetailsHeaderContainer from './files/notices/NoticeDetailsHeaderContainer';
import NoticeDetailsListContainer from './files/notices/NoticeDetailsListContainer';
import NoticeDetailsContentsContainer from './files/notices/NoticeDetailsContentsContainer';
import LegalDashboardContainer from './dashboard/LegalDashboardContainer';
import ComponentLicenseDetailsContainer from './license/ComponentLicenseDetailsContainer';
import componentNoticeDetails from './files/notices/componentNoticeDetails';
import componentLicenseFilesDetails from './files/licenses/componentLicenseFilesDetails';
import LicenseFilesDetailsHeaderContainer from './files/licenses/LicenseFilesDetailsHeaderContainer';
import LicenseFilesDetailsListContainer from './files/licenses/LicenseFilesDetailsListContainer';
import LicenseFilesDetailsContentsContainer from './files/licenses/LicenseFilesDetailsContentsContainer';

export default angular
  .module('legalModule', [])
  .component('legalDashboard', iqReact2Angular(LegalDashboardContainer, ['isAuthorized'], ['$ngRedux', '$state']))
  .component('componentLegalOverview', iqReact2Angular(ComponentLegalOverviewContainer, [], ['$ngRedux', '$state']))
  .component('legalApplicationDetails', iqReact2Angular(LegalApplicationDetailsContainer, [], ['$ngRedux', '$state']))
  .component('attributionReportForm', iqReact2Angular(AttributionReportForm, [], ['$ngRedux', '$state']))
  .component(
    'attributionReportTemplateForm',
    iqReact2Angular(AttributionReportTemplateForm, [], ['$ngRedux', '$state'])
  )
  .component('componentCopyrightDetails', componentCopyrightDetails)
  .component('copyrightDetailsHeader', iqReact2Angular(CopyrightDetailsHeaderContainer, [], ['$ngRedux', '$state']))
  .component('copyrightList', iqReact2Angular(CopyrightListContainer, [], ['$ngRedux', '$state']))
  .component('copyrightDetailsContents', iqReact2Angular(CopyrightDetailsContentsContainer, [], ['$ngRedux', '$state']))
  .component('componentNoticeDetails', componentNoticeDetails)
  .component('noticeDetailsHeader', iqReact2Angular(NoticeDetailsHeaderContainer, [], ['$ngRedux', '$state']))
  .component('noticeDetailsList', iqReact2Angular(NoticeDetailsListContainer, [], ['$ngRedux', '$state']))
  .component('noticeDetailsContents', iqReact2Angular(NoticeDetailsContentsContainer, [], ['$ngRedux', '$state']))
  .component('componentLicenseDetails', iqReact2Angular(ComponentLicenseDetailsContainer, [], ['$ngRedux', '$state']))
  .component('componentLicenseFilesDetails', componentLicenseFilesDetails)
  .component(
    'licenseFilesDetailsHeader',
    iqReact2Angular(LicenseFilesDetailsHeaderContainer, [], ['$ngRedux', '$state'])
  )
  .component('licenseFilesDetailsList', iqReact2Angular(LicenseFilesDetailsListContainer, [], ['$ngRedux', '$state']))
  .component(
    'licenseFilesDetailsContents',
    iqReact2Angular(LicenseFilesDetailsContentsContainer, [], ['$ngRedux', '$state'])
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('legal', {
      abstract: true,
    })
    .state('legal.dashboard', {
      url: '/legal/dashboard',
      component: 'legalDashboard',
      data: {
        title: 'Legal Dashboard',
        activeTab: 'applications',
      },
    })
    .state('legal.applicationsDashboard', {
      url: '/legal/applicationsDashboard',
      component: 'legalDashboard',
      data: {
        title: 'Legal Dashboard',
        activeTab: 'applications',
        disableCreateAttributionReportBtn: false,
      },
    })
    .state('legal.componentsDashboard', {
      url: '/legal/componentsDashboard',
      component: 'legalDashboard',
      data: {
        title: 'Legal Dashboard',
        activeTab: 'components',
        disableCreateAttributionReportBtn: true,
      },
    })
    .state('legal.componentOverview', {
      url: '/legal/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state('legal.componentOverviewByComponentIdentifier', {
      url: '/legal/component/componentIdentifier/{componentIdentifier}/repository/{repositoryId}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state('legal.applicationComponentOverviewByComponentIdentifier', {
      url:
        '/legal/component/componentIdentifier/{componentIdentifier}/application/{applicationPublicId}' +
        '/component/{hash}/scan/{scanId}/{tabId}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state('legal.noticeFilesByComponentIdentifier', {
      url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/notices',
      component: 'componentNoticeDetails',
      abstract: true,
    })
    .state('legal.noticeFilesByComponentIdentifier.noticeDetails', {
      url: '/{noticeIndex}',
      component: 'noticeDetailsContents',
      data: {
        title: 'Notice Details',
      },
    })
    .state('legal.organizationComponentOverview', {
      url: '/legal/organization/{organizationId}/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state('legal.applicationComponentOverview', {
      url: '/legal/application/{applicationPublicId}/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state('legal.applicationStageTypeComponentOverview', {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state('legal.applicationDetails', {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}',
      component: 'legalApplicationDetails',
      data: {
        title: 'Application Details',
      },
    })
    .state('legal.attributionReport', {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/attributionReport',
      component: 'attributionReportForm',
      data: {
        title: 'Attribution Report',
        isDirty: ['attributionReports', 'attributionReports', 'isFormDirty'],
      },
    })
    .state('legal.attributionReportMultiApp', {
      url: '/legal/application/attributionReport',
      component: 'attributionReportForm',
      data: {
        title: 'Attribution Report (Multiple Applications)',
        isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
        isMultiApp: true,
      },
    })
    .state('legal.attributionReportTemplate', {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/attributionReportTemplate',
      component: 'attributionReportTemplateForm',
      data: {
        title: 'Attribution Report Templates',
        isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
      },
    })
    .state('legal.attributionReportTemplateMultiApp', {
      url: '/legal/application/attributionReportTemplate',
      component: 'attributionReportTemplateForm',
      data: {
        title: 'Attribution Report Templates',
        isDirty: ['attributionReports', 'attributionReportTemplates', 'isFormDirty'],
        isMultiApp: true,
      },
    })
    .state('legal.componentCopyrightDetails', {
      url: '/legal/{ownerType}/{ownerId}/component/{hash}/copyrights',
      component: 'componentCopyrightDetails',
      abstract: true,
    })
    .state('legal.componentCopyrightDetails.copyrightDetails', {
      url: '/{copyrightIndex}',
      component: 'copyrightDetailsContents',
      data: {
        title: 'Copyright Details',
      },
    })
    .state('legal.componentCopyrightDetailsByComponentIdentifier', {
      url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/copyrights',
      component: 'componentCopyrightDetails',
      abstract: true,
    })
    .state('legal.componentCopyrightDetailsByComponentIdentifier.copyrightDetails', {
      url: '/{copyrightIndex}',
      component: 'copyrightDetailsContents',
      data: {
        title: 'Copyright Details',
      },
    })
    .state('legal.stageTypeComponentCopyrightDetails', {
      url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/copyrights',
      component: 'componentCopyrightDetails',
      abstract: true,
    })
    .state('legal.stageTypeComponentCopyrightDetails.copyrightDetails', {
      url: '/{copyrightIndex}',
      component: 'copyrightDetailsContents',
      data: {
        title: 'Copyright Details',
      },
    })
    .state('legal.componentNoticeDetails', {
      url: '/legal/{ownerType}/{ownerId}/component/{hash}/notices',
      component: 'componentNoticeDetails',
      abstract: true,
    })
    .state('legal.componentNoticeDetails.noticeDetails', {
      url: '/{noticeIndex}',
      component: 'noticeDetailsContents',
      data: {
        title: 'Notice Details',
      },
    })
    .state('legal.stageTypeComponentNoticeDetails', {
      url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/notices',
      component: 'componentNoticeDetails',
      abstract: true,
    })
    .state('legal.stageTypeComponentNoticeDetails.noticeDetails', {
      url: '/{noticeIndex}',
      component: 'noticeDetailsContents',
      data: {
        title: 'Notice Details',
      },
    })
    .state('legal.componentLicenseFilesDetails', {
      url: '/legal/{ownerType}/{ownerId}/component/{hash}/licenseFiles',
      component: 'componentLicenseFilesDetails',
      abstract: true,
    })
    .state('legal.componentLicenseFilesDetails.licenseFilesDetails', {
      url: '/{licenseIndex}',
      component: 'licenseFilesDetailsContents',
      data: {
        title: 'License Files Details',
      },
    })
    .state('legal.componentLicenseFilesDetailsByComponentIdentifier', {
      url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/licenseFiles',
      component: 'componentLicenseFilesDetails',
      abstract: true,
    })
    .state('legal.componentLicenseFilesDetailsByComponentIdentifier.licenseFilesDetails', {
      url: '/{licenseIndex}',
      component: 'licenseFilesDetailsContents',
      data: {
        title: 'License Files Details',
      },
    })
    .state('legal.stageTypeComponentLicenseFilesDetails', {
      url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/licenseFiles',
      component: 'componentLicenseFilesDetails',
      abstract: true,
    })
    .state('legal.stageTypeComponentLicenseFilesDetails.licenseFilesDetails', {
      url: '/{licenseIndex}',
      component: 'licenseFilesDetailsContents',
      data: {
        title: 'License Files Details',
      },
    })
    .state('legal.componentLicenseDetails', {
      url: '/legal/{ownerType}/{ownerId}/component/{hash}/licenses/{licenseIndex}',
      component: 'componentLicenseDetails',
      data: {
        title: 'Component - License Details',
      },
    })
    .state('legal.componentLicenseDetailsByComponentIdentifier', {
      url: '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/licenses/{licenseIndex}',
      component: 'componentLicenseDetails',
      data: {
        title: 'Component - License Details',
      },
    })
    .state('legal.componentLicenseDetailsByComponentIdentifierAndHashAndScanId', {
      url:
        '/legal/{ownerType}/{ownerId}/componentIdentifier/{componentIdentifier}/component/{hash}/scan/{scanId}' +
        '/licenses/{licenseIndex}',
      component: 'componentLicenseDetails',
      data: {
        title: 'Component - License Details',
      },
    })
    .state('legal.stageTypeComponentLicenseDetails', {
      url: '/legal/{ownerType}/{ownerId}/stage/{stageTypeId}/component/{hash}/licenses/{licenseIndex}',
      component: 'componentLicenseDetails',
      data: {
        title: 'Component - License Details',
      },
    });
}

routes.$inject = ['$stateProvider'];
