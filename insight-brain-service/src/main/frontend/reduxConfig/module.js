/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import ngReduxModule from 'ng-redux';
import reduxUiRouterModule from '../reduxUiRouter/module';
import {createLogger} from 'redux-logger';
import thunk from 'redux-thunk';
import reducers from './reducers';

const middleware = [thunk, 'routerMiddleware'];

// don't use redux-logger in PROD or in Browser with no console.log.apply (IE9)
if (window.angularDebug && window.console.log.apply) {
  const logger = createLogger({level: 'info', collapsed: true, diff: true});
  middleware.push(logger);
}

function config($ngReduxProvider) {
  $ngReduxProvider.createStoreWith(reducers, middleware);
}
config.$inject = ['$ngReduxProvider'];

export default angular.module('reduxConfig', [ngReduxModule, reduxUiRouterModule.name])
    .config(config);
