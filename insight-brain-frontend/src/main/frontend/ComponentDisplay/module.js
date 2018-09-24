/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import AngularCommonModule from '../util/AngularCommon';
import componentName from './componentName';
import filenameDisplay from './filenameDisplay';
import linkedComponentDisplay from './linkedComponentDisplay';
import componentDisplay from './componentDisplay';
import periodDelimiter from './periodDelimiter';

export default angular.module('ComponentDisplay', [AngularCommonModule.name])
    .component('componentName', componentName)
    .component('componentDisplay', componentDisplay)
    .component('filenameDisplay', filenameDisplay)
    .component('linkedComponentDisplay', linkedComponentDisplay)
    .filter('periodDelimiter', periodDelimiter);
