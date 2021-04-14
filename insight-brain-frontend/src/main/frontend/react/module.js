/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';

import IqOrgAppPicker from '../components/iqOrgAppPicker/IqOrgAppPicker';
import withRouterStateProvider from '../reactAdapter/RouterStateProvider';
import IqSidebarNav from './iqSidebarNav/IqSidebarNav';
import IqToggle from './IqToggle';

export default angular.module('reactComponents', ['ui.router'])
    .component('iqOrgAppPicker', react2angular(IqOrgAppPicker))
    .component('iqToggle', react2angular(IqToggle))
    .component('nxFontAwesomeIcon', react2angular(NxFontAwesomeIcon, [
      'icon', 'mask', 'className', 'color', 'spin', 'pulse', 'border', 'fixedWidth', 'inverse', 'listItem', 'flip',
      'size', 'pull', 'rotation', 'transform', 'symbol', 'style', 'tabIndex', 'title']))
    .component('iqSidebarNav',
        react2angular(withRouterStateProvider(IqSidebarNav), [
          'currentState',
          'productEdition',
          'releaseVersion',
          'isLoggedIn',
          'isLicensed',
          'isDashboardAvailable',
          'isReportsListAvailable',
          'isSuccessMetricsEnabled',
          'isAdvancedSearchEnabled',
          'isFirewallEnabled',
          'isLegalEnabled'
        ], ['$state']));

