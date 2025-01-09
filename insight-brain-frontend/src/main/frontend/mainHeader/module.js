/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import utilityServicesModule from '../utility/services/utility.services.module';
import pendoModule from '../pendo/module';
import CLMLocationModule from '../util/CLMLocation';
import permissionServiceModule from '../utilAngular/PermissionService';
import telemetryServiceModule from '../services/telemetryService';
import currentUserService from '../user/CurrentUserService';
import userActions from '../user/userActions';
import userReducer from '../user/userReducer';
import reactComponentsModule from '../react/module.js';
import MenuBar from './MenuBar/MenuBar.jsx';
import mainHeader from './mainHeader';

export default angular
  .module('mainHeader', [
    'ui.router',
    'ui.validate',
    CLMLocationModule.name,
    permissionServiceModule.name,
    'ngSanitize',
    utilityServicesModule.name,
    telemetryServiceModule.name,
    reactComponentsModule.name,
    pendoModule.name,
  ])
  .factory('CurrentUser', currentUserService)
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
      ],
      ['$ngRedux', 'userActions', '$state']
    )
  );
