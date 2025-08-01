/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import changeDefaultAdminPasswordNotice from './changeDefaultAdminPasswordNotice';
import telemetryServiceModule from '../services/telemetryService';
import CLMLocationModule from '../util/CLMLocation';
import userActions from '../user/userActions';
import userReducer from '../user/userReducer';

export default angular
  .module('changeDefaultAdminPasswordNoticeModule', [telemetryServiceModule.name, CLMLocationModule.name])
  .component('changeDefaultAdminPasswordNotice', changeDefaultAdminPasswordNotice)
  .factory('userActions', userActions)
  .value('userReducer', userReducer);
