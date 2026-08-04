/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch } from 'react-redux';
import { Theme } from '@radix-ui/themes';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { actions as displayThemeActions } from 'MainRoot/configuration/displayTheme/displayThemeSlice';
import UnsavedChangesModal from 'MainRoot/modals/unsavedChangesModal/UnsavedChangesModal';
import NoscToastHost from 'MainRoot/nosc/toast/NoscToastHost';
import { TopNav } from './TopNav';
// @ts-expect-error — LeftNav is intentionally .jsx
import LeftNav from './LeftNav';

import '@radix-ui/themes/styles.css';

/**
 * Nexus One shell chrome (TopNav + LeftNav) for the dedicated
 * {@code /assets/nexus-one/index.html} bundle. Classic IQ never mounts this
 * layout — users reach Nexus One via {@link bundleIndexUrl} from ClassicToggleButton.
 */
export function NexusOneShellLayout({ children }: { readonly children: React.ReactNode }): JSX.Element {
  const { effectiveTheme } = useNoscTheme();
  const dispatch = useDispatch();

  // Sync resolved light/dark into Redux so displayThemeHandler toggles nx-html--* on
  // <html> for mounted Classic RSC content (including system mode + OS dark preference).
  useEffect(() => {
    dispatch(displayThemeActions.setDisplayThemeState(effectiveTheme));
  }, [effectiveTheme, dispatch]);

  return (
    <Theme
      appearance={effectiveTheme}
      accentColor={BRAND_ACCENT}
      grayColor="slate"
      radius="medium"
      scaling="100%"
      hasBackground={false}
      style={{ zIndex: 100, position: 'relative', minHeight: '100vh' }}
    >
      <div
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          zIndex: 1000,
        }}
      >
        <TopNav />
      </div>
      <LeftNav />
      {children}
      <NoscToastHost />
      <UnsavedChangesModal />
    </Theme>
  );
}
