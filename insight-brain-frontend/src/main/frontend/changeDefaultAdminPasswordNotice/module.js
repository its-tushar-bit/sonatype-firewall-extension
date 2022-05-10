/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import changeDefaultAdminPasswordNotice from './changeDefaultAdminPasswordNotice';
import CLMLocationModule from '../util/CLMLocation';
import telemetryServiceModule from '../services/telemetryService';
import permissionServiceModule from '../utilAngular/PermissionService';
import userActions from '../user/userActions';
import userReducer from '../user/userReducer';
import currentUserService from '../user/CurrentUserService';
import pendoModule from '../pendo/module';

export default angular
  .module('changeDefaultAdminPasswordNoticeModule', [
    CLMLocationModule.name,
    telemetryServiceModule.name,
    permissionServiceModule.name,
    pendoModule.name,
  ])
  .component('changeDefaultAdminPasswordNotice', changeDefaultAdminPasswordNotice)
  .factory('CurrentUser', currentUserService)
  .factory('userActions', userActions)
  .value('userReducer', userReducer);
