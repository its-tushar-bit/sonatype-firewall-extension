/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import withStoreProvider from 'MainRoot/reactAdapter/StoreProvider';
import withRouterStateProvider from 'MainRoot/reactAdapter/RouterStateProvider';
import withLoginModalService from 'MainRoot/reactAdapter/LoginModalServiceProvider';
import LoginModalService from './LoginModalService';
import LoginModal from './LoginModal';

export default angular
  .module('loginModalModule', ['reduxConfig'])
  .service('LoginModalService', LoginModalService)
  .component(
    'loginModal',
    react2angular(
      withStoreProvider(withRouterStateProvider(withLoginModalService(LoginModal))),
      [],
      ['LoginModalService', '$ngRedux', '$state']
    )
  );
