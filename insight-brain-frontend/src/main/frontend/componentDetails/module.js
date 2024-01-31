/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import ComponentDetails from './ComponentDetails';

const componentDetailsModule = angular
  .module('componentDetails', ['ui.router'])
  .component('componentDetails', iqReact2Angular(ComponentDetails, [], ['$ngRedux', '$state']));

export default componentDetailsModule;
