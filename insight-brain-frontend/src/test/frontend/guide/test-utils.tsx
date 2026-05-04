/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import { MemoryRouter, type MemoryRouterProps } from 'react-router';
import { Theme } from '@radix-ui/themes';
import { NavigationProvider } from '@guide/ui-core';
import { useReactRouterAdapter } from 'GuideRoot/reactRouterAdapter';
import { AuthProvider } from 'GuideRoot/auth/AuthProvider';

jest.mock('GuideRoot/auth/loginApi', () => ({
  fetchSession: jest.fn().mockResolvedValue({
    authenticated: true,
    user: { username: 'test', displayName: 'Test', groups: [] },
    sessionTimeoutMs: 1800000,
    ssoConfig: null,
  }),
  submitLogin: jest.fn(),
}));

interface RouterOptions {
  initialEntries?: MemoryRouterProps['initialEntries'];
}

function AdapterBridge({ children }: { children: React.ReactNode }) {
  const adapter = useReactRouterAdapter();
  return (
    <NavigationProvider adapter={adapter}>
      {children}
    </NavigationProvider>
  );
}

function createWrapper(routerOptions: RouterOptions = {}) {
  return function AllTheProviders({ children }: { children: React.ReactNode }) {
    return (
      <MemoryRouter initialEntries={routerOptions.initialEntries ?? ['/']}>
        <Theme appearance="dark" accentColor="indigo" panelBackground="solid">
          <AuthProvider>
            <AdapterBridge>
              {children}
            </AdapterBridge>
          </AuthProvider>
        </Theme>
      </MemoryRouter>
    );
  };
}

interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  routerOptions?: RouterOptions;
}

const customRender = (
  ui: React.ReactElement,
  { routerOptions, ...options }: CustomRenderOptions = {}
) => render(ui, { wrapper: createWrapper(routerOptions), ...options });

export * from '@testing-library/react';
export { customRender as render };
