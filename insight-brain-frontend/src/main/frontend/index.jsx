/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import 'components-font-awesome/css/font-awesome.min.css';
import './lib/glyphicon/glyphicons.css';
import './lib/bootstrap.scss';
import './lib/glyphicon/halflings.css';
import 'plottable/plottable.css';
import './glyphicons-sonatype.css';
import './sonatype-icons.css';
import './scss/scss.scss';

import './lib/jquery-loader';
import 'es6-collections';
import { polyfill } from 'es6-promise';

// d3 has a commonjs impl and a es6 impl.  The commonjs impl doesn't work with rollup for
// unknown reasons and the es6 one isn't detected correctly, despite the jsnext config which
// should enable it.  As a workaround we point to the specific file that has the es6 imports
// See https://stackoverflow.com/questions/39909200/d3-4-0-import-statement-gives-moduleexports-wrapper
import * as d3 from 'd3/index.js';
import pv from './lib/protovis/protovis.min';

import Fuse from 'fuse.js';
import ClassyBrew from 'classybrew/src/classybrew';

import './utility/Polyfills';

window.Fuse = Fuse;
window.d3 = d3;
window.classyBrew = ClassyBrew;
window.pv = pv;
polyfill();

import React from 'react';
import ReactDOM from 'react-dom';
import router from './router/routerInstance';
import App from './App';
import { initializeRouterListener } from './reduxUiRouter/routerListener';
import { setStateService } from './reduxUiRouter/routerMiddleware';
import main from './main';
import handleOnEnterPermissions from './routeProductLicenseValidator/RouteProductLicenseValidator';
import { setUrlService } from './pendo/mainBundlePendoService';
import { initDocumentTitle } from './documentTitle';
import { initFavicon } from './favicon';

// Import all route definitions (each route file self-registers on import)
import './allRoutes';

document.addEventListener('DOMContentLoaded', () => {
  // Initialize pendo service with router's URL service
  setUrlService(router.urlService);

  // Initialize Redux integration
  setStateService(router.stateService);
  initializeRouterListener(router.transitionService);

  // Ensure URL is synced after successful transitions
  // This handles cases where the built-in URL sync might not fire
  router.transitionService.onSuccess({}, () => {
    // Force URL sync to ensure browser URL matches the current state
    router.urlService.sync();
  });

  // Product license validation - check if routes are permitted based on license type
  router.transitionService.onEnter({}, (transition, state) =>
    handleOnEnterPermissions(transition.router.stateService.target, state)
  );

  // Initialize application logic (framework-agnostic!)
  main(router.stateService, router.transitionService);

  // Initialize document title and favicon
  initDocumentTitle();
  initFavicon();

  // Render React app - UIRouter component will call router.start() automatically
  const container = document.getElementById('react-root');
  if (container) {
    ReactDOM.render(<App />, container);
  }
});
