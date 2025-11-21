/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import uiRouter from '@uirouter/angularjs';
import routerStateReducer from './routerStateReducer';
import routerListener from './routerListener';
import routerMiddleware, { setStateService } from './routerMiddleware';

function initializeRouterMiddleware($state) {
  setStateService($state);
}
initializeRouterMiddleware.$inject = ['$state'];

export default angular
  .module('reduxUiRouter', [uiRouter])
  .value('routerStateReducer', routerStateReducer) // add to angular so we can test it
  .value('routerListener', routerListener) // add to angular so we can test it
  .value('routerMiddleware', routerMiddleware) // add to angular so we can test it
  .run(routerListener)
  .run(initializeRouterMiddleware);
