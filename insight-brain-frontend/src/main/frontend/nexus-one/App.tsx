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
// Shared IQ theme (gray/slate scales, layout tokens). Brand blue/tomato also appear here
// until @sonatype/nexus-one-components (CLM-40381) replaces local CSS.
import 'MainRoot/nosc/theme/theme-variables.css';
// Nexus One brand/accent overrides — load last so edits in this file take effect.
// Master: apps/ux-standards/system/src/tokens/nexus-one-tokens.css
import 'MainRoot/nosc/theme/nexus-one-tokens.css';
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
