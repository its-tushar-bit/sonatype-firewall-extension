/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { Theme } from '@radix-ui/themes';
import { BRAND_ACCENT } from 'MainRoot/nosc/theme';
import { useNoscTheme } from 'MainRoot/nosc/theme/useNoscTheme';
import { usePreviewShellOffsets } from 'MainRoot/nosc/shell/previewShellLayout';
import Footer from 'MainRoot/react/Footer/Footer';

import '@radix-ui/themes/styles.css';

/**
 * Wraps a Classic IQ React page so it renders inside the Nexus One
 * shell's content area, applying the same fixed-position + shell-offset
 * wrapper every native NOSC page uses (see `usePreviewShellOffsets`).
 *
 * {@code layout}:
 * - {@code 'scroll'} (default) — page content + Footer inside
 *   {@code .nx-global-footer-2-container.nx-viewport-sized}. Used by
 *   simple Classic embeds (Enterprise Reporting, Success Metrics, etc.).
 * - {@code 'page'} — children are direct children of {@code .nx-page}.
 *   Required for Orgs & Policies: {@code #iq-content.nx-page-content--full-width}
 *   uses {@code display: contents} so the owner sidebar and detail pane must
 *   participate in the {@code .nx-page} grid. Nesting them under another
 *   {@code .nx-global-footer-2-container} breaks that grid (sidebar becomes a
 *   short floating card). Callers using {@code 'page'} own Footer placement
 *   (typically inside {@code #iq-footer-container}, matching Classic App.jsx).
 */
export interface ClassicComponentMountProps {
  readonly children: React.ReactNode;
  readonly layout?: 'scroll' | 'page';
}

export function ClassicComponentMount({
  children,
  layout = 'scroll',
}: ClassicComponentMountProps): JSX.Element {
  const { effectiveTheme } = useNoscTheme();
  // zIndex excluded here and set via CSS instead (nexus-one.css) so its
  // `:has(.nx-modal-backdrop)` rule can override it without `!important`.
  const { zIndex: _unusedZIndex, ...restOffsets } = usePreviewShellOffsets();
  return (
    <Theme
      className="nosc-classic-mount nx-page"
      appearance={effectiveTheme}
      accentColor={BRAND_ACCENT}
      grayColor="slate"
      radius="medium"
      scaling="100%"
      data-testid="nexus-one-classic-component-mount"
      data-layout={layout}
      style={{
        position: 'fixed',
        ...restOffsets,
        right: 0,
        bottom: 0,
        width: 'auto',
        minWidth: 0,
        // Let top+bottom define the used height so `.nx-page { height: 100% }` /
        // the page grid's `1fr` content row resolve against the shell content box.
        height: 'auto',
        backgroundColor: 'var(--nx-color-site-background)',
      }}
    >
      {layout === 'page' ? (
        children
      ) : (
        <div className="nx-global-footer-2-container nx-viewport-sized">
          {children}
          <Footer clmServerVersion={CLM_SERVER_VERSION} />
        </div>
      )}
    </Theme>
  );
}

/**
 * Factory: wrap a Classic IQ page component in {@link ClassicComponentMount}
 * so it can be passed straight as a UI Router state's `component`.
 *
 * UI Router injects state props into route components; this wrapper renders
 * {@code <Component />} with no props. Safe for Classic pages that read params
 * from Redux router state; pages that need UI-Router {@code $stateParams}
 * injected as props must use a custom route component instead.
 */
export function mountClassicComponent(Component: React.ComponentType<Record<string, never>>): React.ComponentType {
  return function MountedClassicComponent() {
    return (
      <ClassicComponentMount>
        <Component />
      </ClassicComponentMount>
    );
  };
}
