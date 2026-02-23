/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angularDebug */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import store from './reduxConfig/store';
import { selectUsername } from 'MainRoot/user/userSessionSelectors';
import { selectError } from 'MainRoot/session/appErrorSelectors';
import { selectShowLoginModal } from 'MainRoot/user/LoginModal/userLoginSelectors';
import { selectRouterState, selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import legacyConfigurationModule from './LegacyConfigurationModule';
import './reduxConfig/store';
import ChangeDefaultAdminPasswordNotice from './changeDefaultAdminPasswordNotice/ChangeDefaultAdminPasswordNotice';
import SystemNoticeContainer from './systemNotice/SystemNoticeContainer';
import toastContainerModule from './toastContainer/module';
import modalContainerModule from './modalContainer/module';
import footerModule from './react/Footer/module';
import MainHeader from './mainHeader/MainHeader.jsx';
import NavigationContainer from './navigationContainer/NavigationContainer';

export default angular
  .module('managementApp', [
    legacyConfigurationModule.name,
    toastContainerModule.name,
    modalContainerModule.name,
    footerModule.name,
  ])
  .component('mainHeader', iqReact2Angular(MainHeader, [], []))
  .component('navigationContainer', iqReact2Angular(NavigationContainer, ['clmServerVersion'], []))
  .component('systemNotice', iqReact2Angular(SystemNoticeContainer, [], []))
  .component('changeDefaultAdminPasswordNotice', iqReact2Angular(ChangeDefaultAdminPasswordNotice, [], []))
  .config([
    '$compileProvider',
    function configureAngularTemplateCompilation($compileProvider) {
      /**
       * Allow for images to be sourced from blobs. This was removed from AngularJS with closed issue:
       * https://github.com/angular/angular.js/issues/3889
       */
      $compileProvider.imgSrcSanitizationWhitelist(/^\s*(https?|ftp|file|blob):|data:image\//);
      $compileProvider.debugInfoEnabled(angularDebug);
    },
  ])
  .run([
    '$rootScope',
    function syncRootScopeFromRedux($rootScope) {
      // There are still a few places, mainly in index.html, that require properties on the $rootScope. Sync them
      // from redux
      const unsubscribe = store.subscribe(() => {
        const state = store.getState();

        // Sync username
        const username = selectUsername(state);
        if (username) {
          $rootScope.username = username;
        } else {
          delete $rootScope.username;
        }

        // Sync error
        const error = selectError(state);
        if (error) {
          $rootScope.error = error;
        } else {
          delete $rootScope.error;
        }

        // Sync showLoginModal
        const showLoginModal = selectShowLoginModal(state);
        if (showLoginModal) {
          $rootScope.showLoginModal = showLoginModal;
        } else {
          delete $rootScope.showLoginModal;
        }

        // Sync router state for Angular template bindings in index.html
        // This provides $state.current, $state.params, etc. to Angular templates
        const currentState = selectRouterState(state);
        const currentParams = selectRouterCurrentParams(state);
        $rootScope.$state = {
          current: currentState,
          params: currentParams,
        };

        // Trigger Angular digest cycle to update the view
        // Use $evalAsync to safely trigger digest from outside Angular context
        $rootScope.$evalAsync();
      });

      // Initialize from Redux state
      const initialState = store.getState();

      const initialUsername = selectUsername(initialState);
      if (initialUsername) {
        $rootScope.username = initialUsername;
      }

      const initialError = selectError(initialState);
      if (initialError) {
        $rootScope.error = initialError;
      }

      const initialShowLoginModal = selectShowLoginModal(initialState);
      if (initialShowLoginModal) {
        $rootScope.showLoginModal = initialShowLoginModal;
      }

      // Initialize router state for Angular template bindings
      const initialRouterState = selectRouterState(initialState);
      const initialRouterParams = selectRouterCurrentParams(initialState);
      $rootScope.$state = {
        current: initialRouterState,
        params: initialRouterParams,
      };

      $rootScope.$on('$destroy', unsubscribe);
    },
  ]);
