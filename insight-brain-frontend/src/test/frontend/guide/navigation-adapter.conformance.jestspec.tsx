/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { runConformanceTests } from '@guide/ui-core/testing';

// Create spies for router methods (prefix with 'mock' to allow access in jest.mock)
const mockPush = jest.fn();
const mockReplace = jest.fn();

// Mock react-router
jest.mock('react-router', () => {
  // This mock simulates how React Router's navigate works, but translates
  // the adapter's options to match what the conformance tests expect
  const mockNavigate = (url: string, options?: { replace?: boolean; preventScrollReset?: boolean }) => {
    // Translate React Router options back to the format expected by conformance tests
    const translatedOptions = options ? { scroll: !options.preventScrollReset } : undefined;

    if (options?.replace) {
      mockReplace(url, translatedOptions);
    } else {
      // Only pass second argument if there are options (conformance tests expect 1 arg for Form submits)
      if (translatedOptions) {
        mockPush(url, translatedOptions);
      } else {
        mockPush(url);
      }
    }
  };

  return {
    useNavigate: () => mockNavigate,
    useLocation: () => ({ pathname: '/', search: '', hash: '', state: null }),
    useSearchParams: () => [new URLSearchParams(), jest.fn()],
    Link: ({ to, children, ...props }: { to: string; children: React.ReactNode }) => (
      <a href={to} {...props}>{children}</a>
    ),
  };
});

// Import after mock is set up
const { useReactRouterAdapter } = jest.requireActual('GuideRoot/reactRouterAdapter');

// Reset mocks before each test
beforeEach(() => {
  mockPush.mockClear();
  mockReplace.mockClear();
});

runConformanceTests('React Router NavigationAdapter', useReactRouterAdapter, {
  routerSpy: {
    push: mockPush,
    replace: mockReplace,
  },
});
