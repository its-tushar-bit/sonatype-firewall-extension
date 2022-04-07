/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import withStoreProvider from '../reactAdapter/StoreProvider';
import withRouterStateProvider from '../reactAdapter/RouterStateProvider';
import utilityServicesModule from '../utility/services/utility.services.module';
import pendoModule from '../pendo/module';
import angularCommonModule from '../util/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';
import permissionServiceModule from '../util/PermissionService';
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
    angularCommonModule.name,
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
    react2angular(
      withStoreProvider(withRouterStateProvider(MenuBar)),
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
      ],
      ['$ngRedux', 'userActions', '$state']
    )
  );
