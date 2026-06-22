/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { act, render, screen, waitFor } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { LicenseProvider } from 'GuideRoot/license/LicenseProvider';
import { LicenseGate } from 'GuideRoot/license/LicenseGate';
import { setLicenseRevocationHandler } from 'GuideRoot/license/licenseRevocation';
import { apiFetch } from 'GuideRoot/api/apiFetch';
import type { LicensedSolution } from 'GuideRoot/layout/ProductSwitcher/productMetadata';

/**
 * End-to-end test of mid-session Guide license revocation: a Guide data call returning the
 * backend marker drives the HTTP client -> licenseRevocation notifier -> LicenseProvider.refresh()
 * -> LicenseGate chain so the learn-more page replaces the Guide app without a manual reload.
 *
 * Nothing is mocked except the global fetch, so the real apiFetch, licenseRevocation bridge,
 * LicenseProvider, and LicenseGate all run.
 */

const LIFECYCLE: LicensedSolution = { id: 'lifecycle', url: '/lifecycle' };
const GUIDE: LicensedSolution = { id: 'guide', url: '/guide' };

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((res) => {
    resolve = res;
  });
  return { promise, resolve };
}

function solutionsResponse(list: LicensedSolution[]): Response {
  return {
    ok: true,
    status: 200,
    statusText: 'OK',
    headers: new Headers(),
    json: async () => list,
  } as unknown as Response;
}

function guideLicenseRevokedResponse(): Response {
  return {
    ok: false,
    status: 403,
    statusText: 'Forbidden',
    headers: new Headers({ 'X-Sonatype-Guide-License': 'unavailable' }),
    json: async () => ({
      success: false,
      message: 'Guide API is not available with the current license.',
    }),
  } as unknown as Response;
}

// Gated content. Renders GUIDE CONTENT while mounted; flips to a visible error marker if its
// Guide data call rejects — so the test can prove that error UI never flashes during the refresh.
function Probe() {
  const [errored, setErrored] = useState(false);
  useEffect(() => {
    apiFetch('/api/v2/guide/components/detail').catch(() => setErrored(true));
  }, []);
  return (
    <div>
      <span>GUIDE CONTENT</span>
      {errored && <span>PROBE ERROR</span>}
    </div>
  );
}

const realFetch = global.fetch;
const mockFetch = jest.fn();

beforeAll(() => {
  global.fetch = mockFetch as unknown as typeof global.fetch;
});

afterAll(() => {
  global.fetch = realFetch;
});

afterEach(() => {
  setLicenseRevocationHandler(null);
  mockFetch.mockReset();
});

it('swaps in the learn-more page when a Guide data call reports the license was revoked', async () => {
  const guideCall = deferred<Response>();
  const secondSolutionsCall = deferred<Response>();
  let solutionsCalls = 0;

  mockFetch.mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes('/api/v2/solutions/licensed')) {
      solutionsCalls += 1;
      // Boot: guide is licensed. Refresh (2nd call): guide is gone.
      return solutionsCalls === 1
        ? Promise.resolve(solutionsResponse([LIFECYCLE, GUIDE]))
        : secondSolutionsCall.promise;
    }
    if (url.includes('/api/v2/guide/')) {
      return guideCall.promise;
    }
    return Promise.resolve(solutionsResponse([]));
  });

  render(
    <Theme>
      <LicenseProvider>
        <LicenseGate>
          <Probe />
        </LicenseGate>
      </LicenseProvider>
    </Theme>
  );

  // Guide is licensed at boot, so the gated app renders and issues its data call.
  await screen.findByText('GUIDE CONTENT');
  expect(screen.queryByText(/not currently enabled/i)).not.toBeInTheDocument();

  // The Guide data call comes back flagged as license-revoked.
  await act(async () => {
    guideCall.resolve(guideLicenseRevokedResponse());
    await guideCall.promise;
  });

  // While the licensed-solutions refetch is in flight the gated content is gone and NO error UI
  // flashed in its place (a stable loading state, per the acceptance criteria).
  await waitFor(() => expect(screen.queryByText('GUIDE CONTENT')).not.toBeInTheDocument());
  expect(screen.queryByText('PROBE ERROR')).not.toBeInTheDocument();
  expect(screen.queryByText(/not currently enabled/i)).not.toBeInTheDocument();

  // Refresh resolves with guide no longer licensed -> learn-more page renders in place.
  await act(async () => {
    secondSolutionsCall.resolve(solutionsResponse([LIFECYCLE]));
    await secondSolutionsCall.promise;
  });

  expect(await screen.findByText(/not currently enabled/i)).toBeInTheDocument();
  expect(screen.queryByText('PROBE ERROR')).not.toBeInTheDocument();
});

it('refetches licensed solutions exactly once even when several Guide calls are revoked at once', async () => {
  let solutionsCalls = 0;

  mockFetch.mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes('/api/v2/solutions/licensed')) {
      solutionsCalls += 1;
      return solutionsCalls === 1
        ? Promise.resolve(solutionsResponse([LIFECYCLE, GUIDE]))
        : Promise.resolve(solutionsResponse([LIFECYCLE]));
    }
    if (url.includes('/api/v2/guide/')) {
      return Promise.resolve(guideLicenseRevokedResponse());
    }
    return Promise.resolve(solutionsResponse([]));
  });

  render(
    <Theme>
      <LicenseProvider>
        <LicenseGate>
          <Probe />
          <Probe />
          <Probe />
        </LicenseGate>
      </LicenseProvider>
    </Theme>
  );

  // Three concurrently-mounted probes each get a revoked Guide call; the learn-more page renders.
  expect(await screen.findByText(/not currently enabled/i)).toBeInTheDocument();

  // 1 boot fetch + exactly 1 refresh, despite three concurrent revocation triggers (single-flight).
  expect(solutionsCalls).toBe(2);
});

it('swaps in the learn-more page even when the licensed-solutions refetch still reports guide', async () => {
  // Regression for the GUIDE_SEARCH-vs-GUIDE divergence: the Guide data API 403s because
  // GUIDE_SEARCH was lost, but /solutions/licensed still lists guide because GUIDE is retained.
  // The authoritative revocation marker must still win — guide is dropped and the gate closes.
  const guideCall = deferred<Response>();
  let solutionsCalls = 0;

  mockFetch.mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes('/api/v2/solutions/licensed')) {
      solutionsCalls += 1;
      // Both the boot fetch and the refresh report guide as still licensed.
      return Promise.resolve(solutionsResponse([LIFECYCLE, GUIDE]));
    }
    if (url.includes('/api/v2/guide/')) {
      return guideCall.promise;
    }
    return Promise.resolve(solutionsResponse([]));
  });

  render(
    <Theme>
      <LicenseProvider>
        <LicenseGate>
          <Probe />
        </LicenseGate>
      </LicenseProvider>
    </Theme>
  );

  await screen.findByText('GUIDE CONTENT');

  // The Guide data call comes back flagged as license-revoked even though solutions still lists guide.
  await act(async () => {
    guideCall.resolve(guideLicenseRevokedResponse());
    await guideCall.promise;
  });

  // Guide is dropped despite the refetch still reporting it -> learn-more renders, no remount/re-fire.
  expect(await screen.findByText(/not currently enabled/i)).toBeInTheDocument();
  expect(screen.queryByText('GUIDE CONTENT')).not.toBeInTheDocument();
  expect(screen.queryByText('PROBE ERROR')).not.toBeInTheDocument();
  // 1 boot + exactly 1 refresh; the learn-more page does not re-mount the Probe.
  expect(solutionsCalls).toBe(2);
});

it('shows the learn-more page without looping when the licensed-solutions refetch fails', async () => {
  const guideCall = deferred<Response>();
  let solutionsCalls = 0;

  mockFetch.mockImplementation((input: RequestInfo | URL) => {
    const url = String(input);
    if (url.includes('/api/v2/solutions/licensed')) {
      solutionsCalls += 1;
      // Boot: guide is licensed. Refresh: the licensed-solutions endpoint is down.
      return solutionsCalls === 1
        ? Promise.resolve(solutionsResponse([LIFECYCLE, GUIDE]))
        : Promise.reject(new Error('503 Service Unavailable'));
    }
    if (url.includes('/api/v2/guide/')) {
      return guideCall.promise;
    }
    return Promise.resolve(solutionsResponse([]));
  });

  render(
    <Theme>
      <LicenseProvider>
        <LicenseGate>
          <Probe />
        </LicenseGate>
      </LicenseProvider>
    </Theme>
  );

  await screen.findByText('GUIDE CONTENT');

  // The Guide data call reports the license was revoked, but the licensed-solutions refetch fails.
  await act(async () => {
    guideCall.resolve(guideLicenseRevokedResponse());
    await guideCall.promise;
  });

  // The marker is authoritative: guide is dropped and the learn-more page renders despite the
  // failed refetch. The gated content (and its data call) is gone, so nothing re-mounts to re-fire
  // a Guide call — no refetch storm.
  expect(await screen.findByText(/not currently enabled/i)).toBeInTheDocument();
  expect(screen.queryByText('GUIDE CONTENT')).not.toBeInTheDocument();
  expect(screen.queryByText('PROBE ERROR')).not.toBeInTheDocument();
  // 1 boot + exactly 1 (failed) refresh; the learn-more page does not re-mount the Probe.
  expect(solutionsCalls).toBe(2);
});
