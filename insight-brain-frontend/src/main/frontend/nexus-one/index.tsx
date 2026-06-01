/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { createRoot } from 'react-dom/client';
import { attachAxiosInterceptors } from 'MainRoot/utility/axiosConfig';
import { actions as displayThemeActions } from 'MainRoot/configuration/displayTheme/displayThemeSlice';
import store from 'MainRoot/reduxConfig/store';
import router from 'MainRoot/router/routerInstance';
import App from './App';
import { ensureNexusOneShellAccess } from './ensureNexusOneShellAccess';
import './routes';

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
