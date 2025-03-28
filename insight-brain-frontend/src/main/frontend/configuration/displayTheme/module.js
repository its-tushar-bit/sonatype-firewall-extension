/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ngReduxModule from 'ng-redux';

import { actions } from './displayThemeSlice';
import registerDisplayThemeHandler from './displayThemeHandler';

/**
 * Register handler that changes actual rendered theme in response to stored theme changes, and connect redux and
 * localstorage
 */
function moduleInit($ngRedux) {
  registerDisplayThemeHandler($ngRedux);
  $ngRedux.dispatch(actions.initialize());
}

moduleInit.$inject = ['$ngRedux'];

export default angular.module('displayThemeModule', [ngReduxModule]).run(moduleInit);
