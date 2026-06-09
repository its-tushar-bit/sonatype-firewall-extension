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
  router.start();
  const container = document.getElementById('nexus-one-root');
  if (container) {
    createRoot(container).render(<App />);
  }
});
