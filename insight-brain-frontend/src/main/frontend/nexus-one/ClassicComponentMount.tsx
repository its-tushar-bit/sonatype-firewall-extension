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

import '@radix-ui/themes/styles.css';

/**
 * Wraps a Classic IQ React page so it renders inside the Nexus One
 * shell's content area, applying the same fixed-position + shell-offset
 * wrapper every native NOSC page uses (see `usePreviewShellOffsets`).
 */
export interface ClassicComponentMountProps {
  readonly children: React.ReactNode;
}

export function ClassicComponentMount({ children }: ClassicComponentMountProps): JSX.Element {
  const { effectiveTheme } = useNoscTheme();
  const offsets = usePreviewShellOffsets();
  return (
    <Theme
      appearance={effectiveTheme}
      accentColor={BRAND_ACCENT}
      grayColor="slate"
      radius="medium"
      scaling="100%"
      data-testid="nexus-one-classic-component-mount"
      style={{
        position: 'fixed',
        ...offsets,
        right: 0,
        bottom: 0,
        overflowY: 'auto',
        backgroundColor: 'var(--nx-color-site-background)',
      }}
    >
      {children}
    </Theme>
  );
}

/**
 * Factory: wrap a Classic IQ page component in {@link ClassicComponentMount}
 * so it can be passed straight as a UI Router state's `component`.
 *
 * UI Router injects state props into route components; this wrapper renders
 * {@code <Component />} with no props. Use only for Classic pages that do not
 * read router params. CLM-41538 pages that need {@code $stateParams} must use
 * a custom route component instead of this helper.
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
