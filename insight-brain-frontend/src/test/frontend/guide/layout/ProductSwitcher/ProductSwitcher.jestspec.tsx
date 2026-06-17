/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { act } from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from 'TestRoot/guide/test-utils';
import { ProductSwitcher } from 'GuideRoot/layout/ProductSwitcher/ProductSwitcher';
import { LicenseProvider } from 'GuideRoot/license/LicenseProvider';

// Flush all pending microtasks (mocked auth session, hook fetches) inside act
// before driving user-event interactions. Without this, state updates from
// AuthProvider's mocked session check can resolve mid-click and cause React 19
// to wrap the act warnings as an AggregateError.
async function settle() {
  await act(async () => {
    await Promise.resolve();
  });
}

const originalFetch = global.fetch;
const originalLocation = window.location;
const originalOpen = window.open;

// jsdom does not implement these APIs that Radix DropdownMenu (and the ScrollArea
// it embeds) use during open/close flows. Polyfill them at module load time so
// userEvent pointer interactions don't blow up inside Radix.
const elProto = Element.prototype as unknown as Record<string, unknown>;
if (!('hasPointerCapture' in elProto)) elProto.hasPointerCapture = () => false;
if (!('setPointerCapture' in elProto)) elProto.setPointerCapture = () => {};
if (!('releasePointerCapture' in elProto)) elProto.releasePointerCapture = () => {};
if (!('scrollIntoView' in elProto)) elProto.scrollIntoView = () => {};
if (typeof (globalThis as Record<string, unknown>).ResizeObserver === 'undefined') {
  (globalThis as Record<string, unknown>).ResizeObserver = class {
    observe() {}
    unobserve() {}
    disconnect() {}
  };
}

function mockLicensedSolutions(body: unknown, ok = true, status = 200) {
  global.fetch = jest.fn((input: RequestInfo | URL) => {
    const url = typeof input === 'string' ? input : input.toString();
    if (url.includes('/api/v2/solutions/licensed')) {
      return Promise.resolve({
        ok,
        status,
        headers: new Headers(),
        json: () => Promise.resolve(body),
      } as unknown as Response);
    }
    return Promise.resolve({ ok: true, status: 200, headers: new Headers() } as Response);
  }) as unknown as typeof fetch;
}

function stubLocation() {
  // Replace window.location with a writable object so the AuthProvider's
  // unauthenticated redirect (window.location.assign('/')) doesn't crash.
  delete (window as unknown as { location?: Location }).location;
  (window as unknown as { location: { href: string; assign: (url: string) => void } }).location = {
    href: '',
    assign: jest.fn(),
  };
}

function stubWindowOpen() {
  const mock = jest.fn();
  (window as unknown as { open: typeof window.open }).open = mock as unknown as typeof window.open;
  return mock;
}

// ProductSwitcher's useLicensedProducts reads the licensed solutions from LicenseProvider, which
// performs the single /api/v2/solutions/licensed fetch. Render it inside a LicenseProvider so the
// per-test global.fetch mock drives the switcher's data.
function renderSwitcher() {
  return render(
    <LicenseProvider>
      <ProductSwitcher />
    </LicenseProvider>
  );
}

describe('ProductSwitcher', () => {
  beforeEach(() => {
    stubLocation();
  });

  afterEach(() => {
    global.fetch = originalFetch;
    (window as unknown as { location: Location }).location = originalLocation;
    (window as unknown as { open: typeof window.open }).open = originalOpen;
    jest.restoreAllMocks();
  });

  it('renders the trigger immediately on mount', async () => {
    mockLicensedSolutions([{ id: 'lifecycle', url: '/lifecycle' }]);
    renderSwitcher();
    expect(screen.getByRole('button', { name: /sonatype solutions/i })).toBeInTheDocument();
  });

  it('shows a loading indicator inside the menu while the fetch is pending', async () => {
    let resolveFetch: ((value: Response) => void) | undefined;
    global.fetch = jest.fn((input: RequestInfo | URL) => {
      const url = typeof input === 'string' ? input : input.toString();
      if (url.includes('/api/v2/solutions/licensed')) {
        return new Promise<Response>((resolve) => {
          resolveFetch = resolve;
        });
      }
      return Promise.resolve({ ok: true, status: 200, headers: new Headers() } as Response);
    }) as unknown as typeof fetch;

    const user = userEvent.setup();
    renderSwitcher();
    await settle();

    await user.click(screen.getByRole('button', { name: /sonatype solutions/i }));
    expect(await screen.findByText(/loading/i)).toBeInTheDocument();

    // Clean up the pending promise so the hook can settle.
    resolveFetch?.({
      ok: true,
      status: 200,
      headers: new Headers(),
      json: () => Promise.resolve([]),
    } as unknown as Response);
  });

  it('lists licensed products alphabetically and opens in a new tab on click', async () => {
    mockLicensedSolutions([
      { id: 'sbom', url: '/sbom' },
      { id: 'lifecycle', url: '/lifecycle' },
    ]);
    const openMock = stubWindowOpen();

    const user = userEvent.setup();
    renderSwitcher();
    await settle();

    await user.click(screen.getByRole('button', { name: /sonatype solutions/i }));

    // Find the licensed products by their stripped display names.
    const lifecycleItem = await screen.findByRole('menuitem', { name: /^Lifecycle$/ });
    const sbomItem = await screen.findByRole('menuitem', { name: /^SBOM Manager$/ });
    expect(lifecycleItem).toBeInTheDocument();
    expect(sbomItem).toBeInTheDocument();

    await user.click(lifecycleItem);
    expect(openMock).toHaveBeenCalledWith('/lifecycle', '_blank', 'noopener,noreferrer');
  });

  it('renders multi-instance products as a submenu and opens in a new tab', async () => {
    mockLicensedSolutions([
      { id: 'nexusRepositoryManager', url: '/east' },
      { id: 'nexusRepositoryManager', url: '/west' },
    ]);
    const openMock = stubWindowOpen();

    const user = userEvent.setup();
    renderSwitcher();
    await settle();

    await user.click(screen.getByRole('button', { name: /sonatype solutions/i }));

    const subTrigger = await screen.findByRole('menuitem', { name: /^Nexus Repository$/ });
    subTrigger.focus();
    await user.keyboard('{ArrowRight}');

    // Wait for the submenu item (labelled by its URL) to render before pressing Enter.
    await screen.findByRole('menuitem', { name: '/east' });
    await user.keyboard('{Enter}');
    expect(openMock).toHaveBeenCalledWith('/east', '_blank', 'noopener,noreferrer');
  });

  it('shows only the Explore section when no products are licensed', async () => {
    mockLicensedSolutions([]);

    const user = userEvent.setup();
    renderSwitcher();
    await settle();

    await user.click(screen.getByRole('button', { name: /sonatype solutions/i }));

    expect(await screen.findByText('Explore')).toBeInTheDocument();
    expect(screen.queryByText('My Sonatype Solutions')).not.toBeInTheDocument();
  });

  it('renders an Explore section with the products that are not licensed', async () => {
    mockLicensedSolutions([{ id: 'lifecycle', url: '/lifecycle' }]);
    const openMock = stubWindowOpen();

    const user = userEvent.setup();
    renderSwitcher();
    await settle();

    await user.click(screen.getByRole('button', { name: /sonatype solutions/i }));

    // Lifecycle is licensed; it should appear under My Sonatype Solutions but not Explore.
    expect(await screen.findByText('My Sonatype Solutions')).toBeInTheDocument();
    expect(screen.getByText('Explore')).toBeInTheDocument();

    // Click an Explore item (e.g. SBOM Manager) and verify it opens its marketing URL.
    const sbomExploreItem = screen.getByRole('menuitem', { name: /^SBOM Manager$/ });
    await user.click(sbomExploreItem);
    expect(openMock).toHaveBeenCalledTimes(1);
    const [url, target, features] = openMock.mock.calls[0];
    expect(url).toMatch(/sonatype-sbom-manager/);
    expect(target).toBe('_blank');
    expect(features).toBe('noopener,noreferrer');
  });

  it('renders nothing when the API errors', async () => {
    mockLicensedSolutions({ message: 'oops' }, false, 500);
    renderSwitcher();

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /sonatype solutions/i })).not.toBeInTheDocument();
    });
  });
});
