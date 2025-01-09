/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';

import IqOrgAppPicker from '../components/iqOrgAppPicker/IqOrgAppPicker';
import IqSidebarNav from './iqSidebarNav/IqSidebarNav';
import IqToggle from './IqToggle';

export default angular
  .module('reactComponents', ['ui.router'])
  .component('iqOrgAppPicker', iqReact2Angular(IqOrgAppPicker))
  .component('iqToggle', iqReact2Angular(IqToggle))
  .component(
    'nxFontAwesomeIcon',
    iqReact2Angular(NxFontAwesomeIcon, [
      'icon',
      'mask',
      'className',
      'color',
      'spin',
      'pulse',
      'border',
      'fixedWidth',
      'inverse',
      'listItem',
      'flip',
      'size',
      'pull',
      'rotation',
      'transform',
      'symbol',
      'style',
      'tabIndex',
      'title',
    ])
  )
  .component(
    'iqSidebarNav',
    iqReact2Angular(
      IqSidebarNav,
      [
        'currentState',
        'productEdition',
        'releaseVersion',
        'isLoggedIn',
        'isLicensed',
        'isDashboardAvailable',
        'isDashboardWaiversAvailable',
        'isReportsListAvailable',
        'isSuccessMetricsEnabled',
        'isAdvancedSearchEnabled',
        'isFirewallEnabled',
        'isLegalEnabled',
        'isApiPageEnabled',
        'isShowVersionEnabled',
        'isDeveloperDashboardEnabled',
        'isOrgsAndAppsEnabled',
        'isSbomManagerEnabled',
        'isIntegratedEnterpriseReportingSupported',
        'isSbomManager',
        'isProductFeaturesLoading',
        'isSbomManagerOnlyLicense',
        'isProductsLoading',
        'isStandaloneDeveloper',
        'isStandaloneFirewall',
        'isFirewallOnlyLicense',
      ],
      ['$state']
    )
  );
