/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import withLoginModalService from 'MainRoot/reactAdapter/LoginModalServiceProvider';
import LoginModalService from './LoginModalService';
import LoginModal from './LoginModal';
import utilityServicesModule from 'MainRoot/utility/services/utility.services.module';

export default angular
  .module('loginModalModule', ['reduxConfig', utilityServicesModule.name])
  .service('LoginModalService', LoginModalService)
  .component(
    'loginModal',
    iqReact2Angular(withLoginModalService(LoginModal), [], ['LoginModalService', '$ngRedux', '$state'])
  );
