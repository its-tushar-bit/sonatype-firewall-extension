/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import navigationContainer from './navigationContainer';
import reactComponentsModule from '../react/module.js';

export default angular
  .module('navigationContainer', ['ui.router', reactComponentsModule.name])
  .component('navigationContainer', navigationContainer);
