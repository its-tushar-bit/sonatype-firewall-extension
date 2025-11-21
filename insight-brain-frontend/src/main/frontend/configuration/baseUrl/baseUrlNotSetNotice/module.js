/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import BaseUrlNotSetNotice from './BaseUrlNotSetNotice';

const baseUrlNotSetNoticeModule = angular
  .module('baseUrlNotSetNoticeModule', [])
  .component('baseUrlNotSetNotice', iqReact2Angular(BaseUrlNotSetNotice, ['login', 'isLoggedIn'], ['$state']))
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('baseUrlNotSetNotice', { component: 'baseUrlNotSetNotice' });
    },
  ]);

export default baseUrlNotSetNoticeModule;
