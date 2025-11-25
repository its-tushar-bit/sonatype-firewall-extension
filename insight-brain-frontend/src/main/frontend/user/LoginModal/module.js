/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import LoginModal from './LoginModal';

export default angular
  .module('loginModalModule', [])
  .component('loginModal', iqReact2Angular(LoginModal, [], ['$state']));
