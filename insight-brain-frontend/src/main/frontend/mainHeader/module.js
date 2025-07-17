/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import utilityServicesModule from '../utility/services/utility.services.module';
import CLMLocationModule from '../util/CLMLocation';
import permissionServiceModule from '../utilAngular/PermissionService';
import telemetryServiceModule from '../services/telemetryService';
import userActions from '../user/userActions';
import userReducer from '../user/userReducer';
import reactComponentsModule from '../react/module.js';
import MenuBar from './MenuBar/MenuBar.jsx';
import mainHeader from './mainHeader';

export default angular
  .module('mainHeader', [
    'ui.router',
    CLMLocationModule.name,
    permissionServiceModule.name,
    utilityServicesModule.name,
    telemetryServiceModule.name,
    reactComponentsModule.name,
  ])
  .factory('userActions', userActions)
  .value('userReducer', userReducer)
  .component('mainHeader', mainHeader)
  .component(
    'menuBar',
    iqReact2Angular(
      MenuBar,
      [
        'majorMinorVersion',
        'permissions',
        'isWebhooksSupported',
        'isLabsDataInsightsEnabled',
        'isSourceControlSupported',
        'login',
        'isLoggedIn',
        'shouldShowLoginButton',
        'isCrowdIntegrationEnabled',
        'isWebhookConfigurationEnabled',
        'isProductLicenseConfigurationEnabled',
        'isLdapConfigurationEnabled',
        'isEmailConfigurationEnabled',
        'isProxyConfigurationEnabled',
        'isSystemNoticeConfigurationEnabled',
        'isSuccessMetricsConfigurationEnabled',
        'isAutomaticApplicationConfigurationEnabled',
        'isAutomaticScmConfigurationEnabled',
        'isAdvancedSearchConfigurationEnabled',
        'isShowNotificationMenuEnabled',
        'isBaseUrlConfigurationEnabled',
        'isSamlConfigurationEnabled',
        'isMonitoringSupported',
        'isSsoIdpManagedBySonatype',
        'isSingleTenant',
        'isSbomManagerOnlyLicense',
        'isStandaloneDeveloper',
        'isOrgsAndAppsEnabled',
        'isStandaloneFirewall',
        'isFirewallOnlyLicense',
        'isZscalerEnabled',
      ],
      ['$ngRedux', 'userActions', '$state']
    )
  );
