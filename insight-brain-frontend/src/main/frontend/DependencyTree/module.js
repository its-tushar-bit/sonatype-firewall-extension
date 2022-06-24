/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import DependencyTreePage from './DependencyTreePage';

const dependencyTreeModule = angular
  .module('dependencyTree', ['ui.router'])
  .component('dependencyTree', iqReact2Angular(DependencyTreePage, [], ['$ngRedux', '$state']));

export default dependencyTreeModule;
