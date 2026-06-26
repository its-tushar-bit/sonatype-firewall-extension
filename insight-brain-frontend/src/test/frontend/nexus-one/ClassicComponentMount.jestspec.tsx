/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { render, screen } from '@testing-library/react';
import { ClassicComponentMount, mountClassicComponent } from 'MainRoot/nexus-one/ClassicComponentMount';

jest.mock('MainRoot/nosc/theme/useNoscTheme', () => ({
  useNoscTheme: () => ({ effectiveTheme: 'light', themeMode: 'light' }),
}));

jest.mock('MainRoot/nosc/shell/previewShellLayout', () => ({
  usePreviewShellOffsets: () => ({ top: 56, left: 72 }),
}));

function TestPage(): JSX.Element {
  return <div>Classic page content</div>;
}

describe('ClassicComponentMount', () => {
  it('renders children inside the classic mount wrapper', () => {
    render(
      <ClassicComponentMount>
        <TestPage />
      </ClassicComponentMount>,
    );
    expect(screen.getByTestId('nexus-one-classic-component-mount')).toBeInTheDocument();
    expect(screen.getByText('Classic page content')).toBeInTheDocument();
  });

  it('mountClassicComponent wraps a page for UI Router', () => {
    const Mounted = mountClassicComponent(TestPage);
    render(<Mounted />);
    expect(screen.getByTestId('nexus-one-classic-component-mount')).toBeInTheDocument();
    expect(screen.getByText('Classic page content')).toBeInTheDocument();
  });
});
