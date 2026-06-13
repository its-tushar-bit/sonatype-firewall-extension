/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { createRoot } from 'react-dom/client';
// @ts-expect-error - classybrew ships no type declarations
import ClassyBrew from 'classybrew/src/classybrew';
import { attachAxiosInterceptors } from 'MainRoot/utility/axiosConfig';
import { actions as displayThemeActions } from 'MainRoot/configuration/displayTheme/displayThemeSlice';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { load as loadProductLicense } from 'MainRoot/configuration/license/productLicenseActions';
import { fetchUser } from 'MainRoot/user/userSessionUtils';
import store from 'MainRoot/reduxConfig/store';
import router from 'MainRoot/router/routerInstance';
import App from './App';
import { ensureNexusOneShellAccess } from './ensureNexusOneShellAccess';
import './routes';

// The ported Classic dashboard tabs (Applications/Components) reach into the Classic dashboard data
// service, which instantiates `new window.classyBrew()` for risk heat-map color scaling. The Classic
// bundle sets this global in index.jsx; the Nexus One bundle must mirror it or those tabs throw
// "window.classyBrew is not a constructor".
(window as unknown as { classyBrew: unknown }).classyBrew = ClassyBrew;

document.addEventListener('DOMContentLoaded', async () => {
  if (!(await ensureNexusOneShellAccess())) {
    return;
  }

  attachAxiosInterceptors();
  store.dispatch(displayThemeActions.initialize());

  // Hydrate the Redux slices the shared shell (LeftNav, TopNav menus) depends
  // on. Classic does this in main.js; the Nexus One bundle has its own store,
  // so without these the session/feature/license selectors stay at their empty
  // defaults and the LeftNav rail renders no items. `ensureNexusOneShellAccess`
  // already confirmed auth + the preview flag above. These are fire-and-forget
  // (idempotent) — the shell reactively renders as each resolves.
  fetchUser(false);
  store.dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded());
  store.dispatch(loadProductLicense());

  router.start();
  const container = document.getElementById('nexus-one-root');
  if (container) {
    createRoot(container).render(<App />);
  }
});
