/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const importAll = (requireContext) => {
  requireContext.keys().forEach(requireContext);
};

import '../../main/frontend/lib/jquery-loader';
import 'angular';
import '@uirouter/angularjs';
import '@uirouter/angularjs/release/stateEvents';
import 'es6-collections';
import '../../main/frontend/utility/Polyfills';
import * as d3 from 'd3/index.js';
import Fuse from 'fuse.js';

import 'angular-mocks/ngMock';

// mocks and spec utils (Globals)
import './SpecUtil';
import './stores/resource.utils';
import './stores/access/access.mock.data';
import './stores/store.utils';
import './stores/policy/policy.mock.data';
import './stores/proprietary/proprietary.mock.data';
import './mock.data/sidebar.resource.mock.data';
import './assets/MockData';
import customMatchers from './customMatchers';

import '../../main/frontend/utilAngular/Globals';

importAll(require.context('.', true, /(?<!(jest))[sS]pec.jsx?$/));

// explicitly import all of our implementation code to ensure accurate code coverage numbers
// (i.e., make sure that even modules with no tests at all get counted)
import '../../main/frontend/index';

window.d3 = d3;
window.Fuse = Fuse;

beforeAll(function () {
  jasmine.addMatchers(customMatchers);
});
