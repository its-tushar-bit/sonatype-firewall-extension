/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import ComponentLegalOverviewContainer from './ComponentLegalOverviewContainer';
import LegalApplicationDetailsContainer from './application/LegalApplicationDetailsContainer';
import withStoreProvider from '../reactAdapter/StoreProvider';
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

export default angular
  .module('legalModule', [])
  .component(
    'legalDashboard',
    react2angular(withStoreProvider(LegalDashboardContainer), ['isAuthorized'], ['$ngRedux'])
  )
  .component(
    'componentLegalOverview',
    react2angular(withStoreProvider(ComponentLegalOverviewContainer), [], ['$ngRedux', '$state'])
  )
  .component(
    'legalApplicationDetails',
    react2angular(withStoreProvider(LegalApplicationDetailsContainer), [], ['$ngRedux', '$state'])
  )
  .component('componentCopyrightDetails', componentCopyrightDetails)
  .component(
    'copyrightDetailsHeader',
    react2angular(withStoreProvider(CopyrightDetailsHeaderContainer), [], ['$ngRedux', '$state'])
  )
  .component('copyrightList', react2angular(withStoreProvider(CopyrightListContainer), [], ['$ngRedux', '$state']))
  .component(
    'copyrightDetailsContents',
    react2angular(withStoreProvider(CopyrightDetailsContentsContainer), [], ['$ngRedux', '$state'])
  )
  .component('componentNoticeDetails', componentNoticeDetails)
  .component(
    'noticeDetailsHeader',
    react2angular(withStoreProvider(NoticeDetailsHeaderContainer), [], ['$ngRedux', '$state'])
  )
  .component(
    'noticeDetailsList',
    react2angular(withStoreProvider(NoticeDetailsListContainer), [], ['$ngRedux', '$state'])
  )
  .component(
    'noticeDetailsContents',
    react2angular(withStoreProvider(NoticeDetailsContentsContainer), [], ['$ngRedux', '$state'])
  )
  .component(
    'componentLicenseDetails',
    react2angular(withStoreProvider(ComponentLicenseDetailsContainer), [], ['$ngRedux', '$state'])
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
      },
    })
    .state('legal.componentOverview', {
      url: '/legal/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
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
    .state('legal.componentLicenseDetails', {
      url: '/legal/{ownerType}/{ownerId}/component/{hash}/licenses/{licenseIndex}',
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
