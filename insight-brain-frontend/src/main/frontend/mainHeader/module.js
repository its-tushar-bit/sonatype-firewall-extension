/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';

import withStoreProvider from '../reactAdapter/StoreProvider';
import utilityServicesModule from '../utility/services/utility.services.module';
import angularCommonModule from '../util/AngularCommon';
import CLMLocationModule from '../util/CLMLocation';
import permissionServiceModule from '../util/PermissionService';
import productFeaturesModule from '../util/ProductFeatures';
import telemetryServiceModule from '../services/telemetryService';
import currentUserService from './userMenu/CurrentUserService';
import helpMenu from './helpMenu/helpMenu';
import userMenu from './userMenu/userMenu';
import notificationsMenu from './notificationsMenu/notificationsMenu';
import systemConfigurationMenu from './systemConfigurationMenu/systemConfigurationMenu';
import mainHeader from './mainHeader';
import userActions from '../user/userActions';
import userReducer from '../user/userReducer';
import userDetailsModal from './userMenu/userDetailsModal';
import reactComponentsModule from '../react/module.js';
import UserTokenModalContainer from './userMenu/userToken/UserTokenModalContainer';

export default angular
  .module('mainHeader', [
    'ui.router',
    'ui.validate',
    angularCommonModule.name,
    CLMLocationModule.name,
    productFeaturesModule.name,
    permissionServiceModule.name,
    'ngSanitize',
    utilityServicesModule.name,
    telemetryServiceModule.name,
    reactComponentsModule.name,
  ])
  .factory('CurrentUser', currentUserService)
  .factory('userActions', userActions)
  .value('userReducer', userReducer)
  .component('helpMenu', helpMenu)
  .component('userMenu', userMenu)
  .component('notificationsMenu', notificationsMenu)
  .component('systemConfigurationMenu', systemConfigurationMenu)
  .component('mainHeader', mainHeader)
  .component('userDetailsModal', userDetailsModal)
  .component(
    'userTokenModal',
    react2angular(withStoreProvider(UserTokenModalContainer), [], ['$ngRedux'])
  );
