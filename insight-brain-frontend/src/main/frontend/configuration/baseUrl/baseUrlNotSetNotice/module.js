/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import BaseUrlNotSetNotice from './BaseUrlNotSetNotice';
import currentUserService from 'MainRoot/user/CurrentUserService';

const baseUrlNotSetNoticeModule = angular
  .module('baseUrlNotSetNoticeModule', [])
  .factory('CurrentUser', currentUserService)
  .component(
    'baseUrlNotSetNotice',
    iqReact2Angular(BaseUrlNotSetNotice, ['login', 'isLoggedIn'], ['$ngRedux', '$state'])
  )
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('baseUrlNotSetNotice', { component: 'baseUrlNotSetNotice' });
    },
  ]);

export default baseUrlNotSetNoticeModule;
