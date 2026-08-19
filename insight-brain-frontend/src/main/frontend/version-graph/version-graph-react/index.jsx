/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';

import $ from 'jquery';
import pv from 'MainRoot/lib/protovis/protovis.min';

import App from './components/App';
import store from './store';

import PendoService from 'MainRoot/pendo/PendoService';
import SanitizeUrlService from '../pendo/SanitizeUrlService';

// sets up window.Insight API
import './externalAPI';

// NOTE: @sonatype/version-graph has undeclared peer dependencies on global jquery and protovis
window.$ = $;
window.pv = pv;

// Initialize UI analytics
new PendoService(new SanitizeUrlService()).start();

// Render the app
createRoot(document.getElementById('ui-view')).render(
  <Provider store={store}>
    <App />
  </Provider>
);
