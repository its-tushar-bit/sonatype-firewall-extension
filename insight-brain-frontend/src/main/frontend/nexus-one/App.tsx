/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import { Theme } from '@radix-ui/themes';
import { Provider as ReduxProvider, useSelector } from 'react-redux';
import { UIRouterContext, UIView } from '@uirouter/react';
import store from 'MainRoot/reduxConfig/store';
import { selectDisplayTheme } from 'MainRoot/configuration/displayTheme/displayThemeSelectors';
import router from 'MainRoot/router/routerInstance';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';

import '@radix-ui/themes/styles.css';
// Shared IQ theme (gray/slate scales, layout tokens). Brand blue/tomato also appear here
// until @sonatype/nexus-one-components (CLM-40381) replaces local CSS.
import 'MainRoot/nosc/theme/theme-variables.css';
// Nexus One brand/accent overrides — load last so edits in this file take effect.
// Master: apps/ux-standards/system/src/tokens/nexus-one-tokens.css
import 'MainRoot/nosc/theme/nexus-one-tokens.css';
import './nexus-one.css';

const DARK_MODE_QUERY = '(prefers-color-scheme: dark)';

type RadixAppearance = 'light' | 'dark';

/**
 * Reactively tracks whether the OS/browser prefers dark mode.
 * Subscribes to matchMedia changes so the UI updates live when the user
 * toggles their system theme — matching the approach used by next-themes in
 * the Guide codebase.
 */
function useSystemDarkMode(): boolean {
  const [isDark, setIsDark] = useState(
    () => typeof window !== 'undefined' && window.matchMedia?.(DARK_MODE_QUERY).matches === true,
  );

  useEffect(() => {
    const mql = window.matchMedia(DARK_MODE_QUERY);
    const handler = (e: MediaQueryListEvent) => setIsDark(e.matches);
    mql.addEventListener('change', handler);
    // Sync in case the value changed between the initial render and this effect
    setIsDark(mql.matches);
    return () => mql.removeEventListener('change', handler);
  }, []);

  return isDark;
}

function useRadixAppearance(): RadixAppearance {
  const theme = useSelector(selectDisplayTheme);
  const systemIsDark = useSystemDarkMode();
  if (theme === 'dark') return 'dark';
  if (theme === 'light') return 'light';
  // theme === 'system': reactively follow OS preference
  return systemIsDark ? 'dark' : 'light';
}

// Separate component because useRadixAppearance calls useSelector,
// which requires a ReduxProvider ancestor in the tree.
function ThemedApp() {
  const appearance = useRadixAppearance();
  return (
    <Theme appearance={appearance} accentColor={BRAND_ACCENT} grayColor="slate" radius="medium" scaling="100%">
      <UIView />
    </Theme>
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
