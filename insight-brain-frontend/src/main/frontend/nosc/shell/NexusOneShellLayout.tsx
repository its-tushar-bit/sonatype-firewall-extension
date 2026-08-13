/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Theme } from '@radix-ui/themes';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { actions as displayThemeActions } from 'MainRoot/configuration/displayTheme/displayThemeSlice';
import UnsavedChangesModal from 'MainRoot/modals/unsavedChangesModal/UnsavedChangesModal';
import NoscToastHost from 'MainRoot/nosc/toast/NoscToastHost';
import { selectError } from 'MainRoot/session/appErrorSelectors';
import {
  SHELL_THEME_Z_INDEX,
  TOP_NAV_WRAPPER_Z_INDEX,
  useNoticeStripHeight,
} from 'MainRoot/nosc/shell/previewShellLayout';
import { NoticeStrip } from './NoticeStrip';
import { DefaultAdminPasswordNotice } from './notices/DefaultAdminPasswordNotice';
import { SystemNotice } from './notices/SystemNotice';
import { BaseUrlNotSetNotice } from './notices/BaseUrlNotSetNotice';
import { MtiqAnnouncementBanner } from './notices/MtiqAnnouncementBanner';
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
  const noticeStripHeightPx = useNoticeStripHeight();
  // Parity with Classic's App.jsx: suppress notices while a global app error is showing (session/appErrorSlice
  // is registered in the shared store this bundle imports directly).
  const hasAppError = useSelector(selectError);

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
      style={{ zIndex: SHELL_THEME_Z_INDEX, position: 'relative', minHeight: '100vh' }}
    >
      <NoticeStrip showLandmark={!hasAppError}>
        {!hasAppError && (
          <>
            <SystemNotice />
            <DefaultAdminPasswordNotice />
            <BaseUrlNotSetNotice />
            <MtiqAnnouncementBanner />
          </>
        )}
      </NoticeStrip>
      <div
        style={{
          position: 'fixed',
          top: noticeStripHeightPx,
          left: 0,
          right: 0,
          zIndex: TOP_NAV_WRAPPER_Z_INDEX,
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
