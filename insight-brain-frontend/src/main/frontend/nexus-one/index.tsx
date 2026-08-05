/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { createRoot } from 'react-dom/client';
// @ts-expect-error - classybrew ships no type declarations
import ClassyBrew from 'classybrew/src/classybrew';
// @ts-expect-error - jquery ships no type declarations
import $ from 'jquery';
// @ts-expect-error - protovis ships no type declarations
import pv from 'MainRoot/lib/protovis/protovis.min';
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

// Classic code reachable from this bundle reads these off `window` instead of importing them, so
// the Nexus One bundle has to install them too. Classic sets classyBrew and pv directly in
// index.jsx, and $/jQuery via the lib/jquery-loader import; this bundle assigns all four here.
// - classyBrew: the Classic dashboard data service behind the ported Applications/Components tabs
//   does `new window.classyBrew()` for risk heat-map color scaling.
// - $/jQuery and pv: undeclared peer dependencies of @sonatype/version-graph, which drives the
//   Version Graph on the Classic component details embedded in the report.
const classicGlobals = (window as unknown) as Record<string, unknown>;
classicGlobals.classyBrew = ClassyBrew;
classicGlobals.$ = $;
classicGlobals.jQuery = $;
classicGlobals.pv = pv;

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
  // The Settings hub (nosc/settings/SettingsPage.tsx) gates its Admin Console
  // items on mainHeader permissions (CONFIGURE_SYSTEM, VIEW_ROLES, etc.) via
  // useSettingsGatingContext. Classic loads these in MainHeader.jsx; the
  // Nexus One bundle must dispatch it too or every admin item stays hidden.
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
