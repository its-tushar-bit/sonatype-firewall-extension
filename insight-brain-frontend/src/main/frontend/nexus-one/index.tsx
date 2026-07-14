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
import initDisplayTheme from 'MainRoot/configuration/displayTheme/initDisplayTheme';
import { loadConfiguration as loadSuccessMetricsConfig } from 'MainRoot/configuration/successMetricsConfiguration/successMetricsConfigurationActions';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { actions as mainHeaderActions } from 'MainRoot/mainHeader/mainHeaderSlice';
import { load as loadProductLicense } from 'MainRoot/configuration/license/productLicenseActions';
import { fetchUser } from 'MainRoot/user/userSessionUtils';
import store from 'MainRoot/reduxConfig/store';
import router from 'MainRoot/router/routerInstance';
import { initializeRouterListener } from 'MainRoot/reduxUiRouter/routerListener';
import { setStateService } from 'MainRoot/reduxUiRouter/routerMiddleware';
import { installDirtyGuard } from 'MainRoot/nosc/shell/installDirtyGuard';
import App from './App';
import { ensureNexusOneShellAccess } from './ensureNexusOneShellAccess';
import './routes';

// The ported Classic dashboard tabs (Applications/Components) reach into the Classic dashboard data
// service, which instantiates `new window.classyBrew()` for risk heat-map color scaling. The Classic
// bundle sets this global in index.jsx; the Nexus One bundle must mirror it or those tabs throw
// "window.classyBrew is not a constructor".
((window as unknown) as { classyBrew: unknown }).classyBrew = ClassyBrew;

document.addEventListener('DOMContentLoaded', async () => {
  if (!(await ensureNexusOneShellAccess())) {
    return;
  }

  attachAxiosInterceptors();
  initDisplayTheme();

  // Hydrate the Redux slices the shared shell (LeftNav, TopNav menus) depends
  // on. Classic does this in main.js; the Nexus One bundle has its own store,
  // so without these the session/feature/license selectors stay at their empty
  // defaults and the LeftNav rail renders no items. `ensureNexusOneShellAccess`
  // already confirmed auth + the preview flag above. These are fire-and-forget
  // (idempotent) — the shell reactively renders as each resolves.
  fetchUser(false);
  store.dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded());
  store.dispatch(loadProductLicense());
  // LeftNav gates Success Metrics on successMetricsConfiguration.serverData.enabled.
  // Classic loads this in NavigationContainer; the nexus-one bundle must too.
  store.dispatch(loadSuccessMetricsConfig());
  // The TopNav System Preferences menu gates its items on mainHeader
  // permissions (CONFIGURE_SYSTEM, VIEW_ROLES, etc.). Classic loads these in
  // MainHeader.jsx; the Nexus One bundle must dispatch it too or the gear menu
  // renders "No preferences available".
  store.dispatch(mainHeaderActions.loadPermissions());

  initializeRouterListener(router.transitionService);
  setStateService(router.stateService);
  // Cleanup fn intentionally discarded: the guard lives for the bundle's
  // lifetime, and full page unload tears both hook registrations down. The
  // return is kept on the helper so tests can drive teardown.
  installDirtyGuard(router.transitionService, store);

  router.start();
  const container = document.getElementById('nexus-one-root');
  if (container) {
    createRoot(container).render(<App />);
  }
});
