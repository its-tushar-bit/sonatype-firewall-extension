/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import changeDefaultAdminPasswordNotice from './changeDefaultAdminPasswordNotice';
import userActions from '../user/userActions';
import userReducer from '../user/userReducer';

export default angular
  .module('changeDefaultAdminPasswordNoticeModule', [])
  .component('changeDefaultAdminPasswordNotice', changeDefaultAdminPasswordNotice)
  .factory('userActions', userActions)
  .value('userReducer', userReducer);
