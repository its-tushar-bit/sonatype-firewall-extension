/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Provider as ReduxProvider } from 'react-redux';
import { UIRouterContext, UIView } from '@uirouter/react';
import store from 'MainRoot/reduxConfig/store';
import router from 'MainRoot/router/routerInstance';
import { NexusOneShellLayout } from 'MainRoot/nosc/shell/NexusOneShellLayout';

import '@radix-ui/themes/styles.css';
// Sonatype Nexus One brand colors, Radix palette (with brand blue/tomato
// scale overrides), and typography. Side-effect import — the package's
// index.js auto-imports the CSS files. This is the Nexus One SPA entry
// point; all components mounted under it inherit the loaded CSS. Components
// rendered outside the Nexus One SPA (e.g. `ClassicToggleButton`, mounted
// inside the Classic bundle) load the lib import themselves.
import '@sonatype/nexus-one-components';
import './nexus-one.css';

function ThemedApp() {
  return (
    <NexusOneShellLayout>
      <UIView />
    </NexusOneShellLayout>
  );
}

export default function App() {
  return (
    <ReduxProvider store={store}>
      <UIRouterContext.Provider value={router}>
        <ThemedApp />
      </UIRouterContext.Provider>
    </ReduxProvider>
  );
}
