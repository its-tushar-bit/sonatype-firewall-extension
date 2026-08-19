/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, type RenderOptions } from '@testing-library/react';
import { MemoryRouter, Routes, Route, type MemoryRouterProps } from 'react-router';
import { Theme } from '@radix-ui/themes';
import { NavigationProvider } from '@guide/ui-core';
import { useReactRouterAdapter } from 'GuideRoot/reactRouterAdapter';
import { AuthProvider } from 'GuideRoot/auth/AuthProvider';
import { OwnerAdapterProvider } from 'GuideRoot/components/navigation/context-picker/OwnerAdapterProvider';
import { PolicyContextProvider } from 'GuideRoot/components/navigation/context-picker/PolicyContext';
import { MockOwnerAdapter } from 'GuideRoot/api/context-picker/MockOwnerAdapter';

// Radix Themes primitives (Tooltip / Badge sizing) call ResizeObserver, which jsdom lacks.
if (typeof (globalThis as Record<string, unknown>).ResizeObserver === 'undefined') {
  (globalThis as Record<string, unknown>).ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
}

jest.mock('GuideRoot/auth/loginApi', () => ({
  fetchSession: jest.fn().mockResolvedValue({
    authenticated: true,
    user: { username: 'test', displayName: 'Test', groups: [] },
    sessionTimeoutMs: 1800000,
  }),
}));

interface RouteConfig {
  path: string;
}

interface RouterOptions {
  initialEntries?: MemoryRouterProps['initialEntries'];
  /**
   * When provided, wraps children in a <Routes> block with the given path patterns so that
   * useParams() resolves correctly. Each entry's `element` is always the rendered children.
   *
   * Example: `[{ path: '/vulnerability/:vulnId' }, { path: '/vulnerability' }]`
   */
  routes?: RouteConfig[];
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
    const content = routerOptions.routes ? (
      <Routes>
        {routerOptions.routes.map(({ path }) => (
          <Route key={path} path={path} element={children} />
        ))}
      </Routes>
    ) : children;

    return (
      <MemoryRouter initialEntries={routerOptions.initialEntries ?? ['/']}>
        <Theme appearance="dark" accentColor="indigo" panelBackground="solid">
          <AuthProvider>
            <AdapterBridge>
              {/* Policy-context + owner-adapter are app-wide providers (see App.tsx), so any page
                  rendering the PolicyContextPicker gets them here without per-test wiring. */}
              <OwnerAdapterProvider adapter={new MockOwnerAdapter()}>
                <PolicyContextProvider>{content}</PolicyContextProvider>
              </OwnerAdapterProvider>
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
