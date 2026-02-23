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

// Setup global axios mocks BEFORE loading the application
// This prevents unhandled promise rejections from initialization code
import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';

const globalAxiosMock = new MockAdapter(axios);

// Mock all initialization API calls that happen when the app loads
// These endpoints are called during app initialization but aren't relevant to most tests
globalAxiosMock.onGet(/\/rest\/user-telemetry\/config/).reply(200, {});
globalAxiosMock.onGet(/\/rest\/user-telemetry\/javascript/).reply(200, '');
globalAxiosMock.onGet(/\/rest\/product\/features\/enableUnauthenticatedPages/).reply(200, false);
globalAxiosMock.onGet(/\/api\/v2\/firewall\/quarantinedComponentView\/configuration\/anonymousAccess\//).reply(200, {
  anonymousAccess: false,
});
globalAxiosMock.onGet(/\/rest\/user\/session/).reply(200, {});
globalAxiosMock.onGet(/\/rest\/user\/permissions\/global\/global/).reply(200, {});

// Pass through all other requests (tests will mock their specific endpoints)
globalAxiosMock.onAny().passThrough();

importAll(require.context('.', true, /(?<!(jest))[sS]pec.jsx?$/));

// explicitly import all of our implementation code to ensure accurate code coverage numbers
// (i.e., make sure that even modules with no tests at all get counted)
import '../../main/frontend/index';

window.d3 = d3;
window.Fuse = Fuse;

beforeAll(function () {
  jasmine.addMatchers(customMatchers);
});
