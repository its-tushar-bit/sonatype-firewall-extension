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
import ComponentLicenseDetailsContainer from './license/ComponentLicenseDetailsContainer';

export default angular
  .module('legalModule', [])
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
  .component(
    'componentLicenseDetails',
    react2angular(withStoreProvider(ComponentLicenseDetailsContainer), [], ['$ngRedux', '$state'])
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('componentLegalOverview', {
      url: '/legal/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state('organizationComponentLegalOverview', {
      url: '/legal/organization/{organizationId}/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state('applicationComponentLegalOverview', {
      url: '/legal/application/{applicationPublicId}/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state('applicationStageTypeComponentLegalOverview', {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}/component/{hash}',
      component: 'componentLegalOverview',
      data: {
        title: 'Component - Legal Overview',
      },
    })
    .state('legalApplicationDetails', {
      url: '/legal/application/{applicationPublicId}/stage/{stageTypeId}',
      component: 'legalApplicationDetails',
      data: {
        title: 'Application Details',
      },
    })
    .state('componentCopyrightDetails', {
      url: '/legal/{ownerType}/{ownerId}/component/{hash}/copyrights',
      component: 'componentCopyrightDetails',
      abstract: true,
    })
    .state('componentCopyrightDetails.copyrightDetails', {
      url: '/{copyrightIndex}',
      component: 'copyrightDetailsContents',
      data: {
        title: 'Copyright Details',
      },
    })
    .state('componentLicenseDetails', {
      url: '/legal/{ownerType}/{ownerId}/component/{hash}/licenses/{licenseIndex}',
      component: 'componentLicenseDetails',
      data: {
        title: 'Component - License Details',
      },
    });
}

routes.$inject = ['$stateProvider'];
