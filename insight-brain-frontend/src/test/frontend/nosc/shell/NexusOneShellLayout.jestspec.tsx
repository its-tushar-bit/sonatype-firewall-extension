/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-env jest */
import React from 'react';
import { Provider } from 'react-redux';
import { render, screen, waitFor } from '@testing-library/react';
import { axiosMockAdapter, configureStore } from 'TestRoot/SpecUtil';
import reducers from 'MainRoot/reduxConfig/reducers';
import { NexusOneShellLayout } from 'MainRoot/nosc/shell/NexusOneShellLayout';
import * as appErrorSelectors from 'MainRoot/session/appErrorSelectors';

jest.mock('MainRoot/nosc/theme/useNoscTheme', () => ({
  useNoscTheme: () => ({ effectiveTheme: 'light' }),
}));
jest.mock('MainRoot/nosc/shell/TopNav', () => ({
  TopNav: () => <nav data-testid="top-nav" />,
}));
jest.mock(
  'MainRoot/nosc/shell/LeftNav',
  () =>
    function MockLeftNav() {
      return <nav data-testid="left-nav" />;
    }
);
jest.mock(
  'MainRoot/modals/unsavedChangesModal/UnsavedChangesModal',
  () =>
    function MockUnsavedChangesModal() {
      return null;
    }
);
jest.mock('@radix-ui/themes', () => ({
  ...jest.requireActual('@radix-ui/themes'),
  // eslint-disable-next-line react/prop-types
  Theme: function MockTheme({ children }) {
    return <div>{children}</div>;
  },
}));

describe('NexusOneShellLayout', () => {
  const axiosMock = axiosMockAdapter();

  afterEach(() => {
    axiosMock.reset();
    jest.clearAllMocks();
    jest.restoreAllMocks();
  });

  afterAll(() => {
    axiosMock.restore();
  });

  it('does not preload dashboard metrics or application facets when the shell mounts', async () => {
    const store = configureStore({ reducer: reducers });

    render(
      <Provider store={store}>
        <NexusOneShellLayout>
          <main>Shell content</main>
        </NexusOneShellLayout>
      </Provider>
    );

    await waitFor(() =>
      expect(
        axiosMock.history.post.filter(({ url }) =>
          /\/rest\/dashboard\/metrics|\/rest\/dashboard\/applications\/list/.test(url ?? '')
        )
      ).toHaveLength(0)
    );
    expect(screen.getByTestId('top-nav')).toBeInTheDocument();
    expect(screen.getByTestId('left-nav')).toBeInTheDocument();
    expect(screen.getByRole('main')).toHaveTextContent('Shell content');
  });

  it('suppresses the notice strip when a global app error is present', () => {
    jest.spyOn(appErrorSelectors, 'selectError').mockReturnValue({ message: 'Critical error' });

    // System Notice and Default Admin Password Notice would otherwise render with this state —
    // the point of this test is that hasAppError suppresses them anyway.
    const preloadedState = {
      systemNoticeConfiguration: {
        serverData: { enabled: true, message: 'System maintenance scheduled' },
      },
      userSession: {
        data: { username: 'admin' },
        shouldDisplayPasswordWarning: true,
        loading: false,
        error: null,
      },
    };

    const store = configureStore({ reducer: reducers, preloadedState });

    render(
      <Provider store={store}>
        <NexusOneShellLayout>
          <main>Shell content</main>
        </NexusOneShellLayout>
      </Provider>
    );

    expect(screen.getByRole('main')).toHaveTextContent('Shell content');
    expect(screen.queryByText('System maintenance scheduled')).not.toBeInTheDocument();
    expect(screen.queryByText(/Change Administrator Password/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('region', { name: 'System notices' })).not.toBeInTheDocument();
  });
});
