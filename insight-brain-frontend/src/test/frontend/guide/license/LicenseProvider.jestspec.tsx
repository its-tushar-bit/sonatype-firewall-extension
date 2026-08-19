/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { act, render, screen, waitFor } from '@testing-library/react';
import { LicenseProvider, useLicense } from 'GuideRoot/license/LicenseProvider';
import * as licenseApi from 'GuideRoot/license/licenseApi';
import { notifyLicenseRevoked, setLicenseRevocationHandler } from 'GuideRoot/license/licenseRevocation';
import type { LicensedSolution } from 'GuideRoot/layout/ProductSwitcher/productMetadata';

const LIFECYCLE: LicensedSolution = { id: 'lifecycle', url: '/lifecycle' };
const GUIDE: LicensedSolution = { id: 'guide', url: '/guide' };

jest.mock('GuideRoot/license/licenseApi');

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

function LicenseConsumer() {
  const { solutions, isLoading, hasError, hasSolution, refresh } = useLicense();
  return (
    <div>
      <span data-testid="loading">{String(isLoading)}</span>
      <span data-testid="error">{String(hasError)}</span>
      <span data-testid="solutions">{solutions.map((s) => s.id).join(',')}</span>
      <span data-testid="has-guide">{String(hasSolution('guide'))}</span>
      <span data-testid="has-lifecycle">{String(hasSolution('lifecycle'))}</span>
      <span data-testid="has-refresh">{String(typeof refresh === 'function')}</span>
    </div>
  );
}

describe('LicenseProvider', () => {
  afterEach(() => {
    setLicenseRevocationHandler(null);
    jest.restoreAllMocks();
  });

  it('starts in loading state then exposes fetched solutions', async () => {
    jest.spyOn(licenseApi, 'fetchLicensedSolutions').mockResolvedValue([
      { id: 'lifecycle', url: '/lifecycle' },
      { id: 'guide', url: '/guide' },
    ]);

    render(
      <LicenseProvider>
        <LicenseConsumer />
      </LicenseProvider>
    );

    expect(screen.getByTestId('loading')).toHaveTextContent('true');

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(screen.getByTestId('solutions')).toHaveTextContent('lifecycle,guide');
    expect(screen.getByTestId('has-guide')).toHaveTextContent('true');
    expect(screen.getByTestId('has-lifecycle')).toHaveTextContent('true');
    expect(screen.getByTestId('error')).toHaveTextContent('false');
  });

  it('hasSolution returns false when the solution is not licensed', async () => {
    jest.spyOn(licenseApi, 'fetchLicensedSolutions').mockResolvedValue([
      { id: 'lifecycle', url: '/lifecycle' },
    ]);

    render(
      <LicenseProvider>
        <LicenseConsumer />
      </LicenseProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(screen.getByTestId('has-guide')).toHaveTextContent('false');
    expect(screen.getByTestId('has-lifecycle')).toHaveTextContent('true');
  });

  it('exposes empty solutions when fetch fails', async () => {
    jest.spyOn(licenseApi, 'fetchLicensedSolutions').mockRejectedValue(new Error('402 Payment Required'));

    render(
      <LicenseProvider>
        <LicenseConsumer />
      </LicenseProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(screen.getByTestId('solutions')).toHaveTextContent('');
    expect(screen.getByTestId('has-guide')).toHaveTextContent('false');
    expect(screen.getByTestId('error')).toHaveTextContent('true');
  });

  it('throws when useLicense is used outside LicenseProvider', () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => render(<LicenseConsumer />)).toThrow(
      'useLicense must be used within a LicenseProvider'
    );

    consoleSpy.mockRestore();
  });

  describe('reactive refresh on license revocation', () => {
    it('exposes a refresh function on the context', async () => {
      jest.spyOn(licenseApi, 'fetchLicensedSolutions').mockResolvedValue([GUIDE]);

      render(
        <LicenseProvider>
          <LicenseConsumer />
        </LicenseProvider>
      );

      await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));
      expect(screen.getByTestId('has-refresh')).toHaveTextContent('true');
    });

    it('refetches licensed solutions on revocation and drops guide', async () => {
      jest
        .spyOn(licenseApi, 'fetchLicensedSolutions')
        .mockResolvedValueOnce([LIFECYCLE, GUIDE])
        .mockResolvedValueOnce([LIFECYCLE]);

      render(
        <LicenseProvider>
          <LicenseConsumer />
        </LicenseProvider>
      );
      await waitFor(() => expect(screen.getByTestId('has-guide')).toHaveTextContent('true'));

      act(() => {
        notifyLicenseRevoked();
      });

      await waitFor(() => expect(screen.getByTestId('has-guide')).toHaveTextContent('false'));
      expect(screen.getByTestId('solutions')).toHaveTextContent('lifecycle');
      expect(licenseApi.fetchLicensedSolutions).toHaveBeenCalledTimes(2);
    });

    it('collapses concurrent revocation notifications into a single refetch (single-flight)', async () => {
      const refresh = deferred<LicensedSolution[]>();
      const spy = jest
        .spyOn(licenseApi, 'fetchLicensedSolutions')
        .mockResolvedValueOnce([LIFECYCLE, GUIDE])
        .mockReturnValueOnce(refresh.promise);

      render(
        <LicenseProvider>
          <LicenseConsumer />
        </LicenseProvider>
      );
      await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));

      act(() => {
        notifyLicenseRevoked();
        notifyLicenseRevoked();
        notifyLicenseRevoked();
      });

      // One fetch for the initial mount, exactly one more for the refresh despite three triggers.
      expect(spy).toHaveBeenCalledTimes(2);

      await act(async () => {
        refresh.resolve([LIFECYCLE]);
        await refresh.promise;
      });
      await waitFor(() => expect(screen.getByTestId('has-guide')).toHaveTextContent('false'));
    });

    it('shows the loading state while the revocation refetch is in flight', async () => {
      const refresh = deferred<LicensedSolution[]>();
      jest
        .spyOn(licenseApi, 'fetchLicensedSolutions')
        .mockResolvedValueOnce([LIFECYCLE, GUIDE])
        .mockReturnValueOnce(refresh.promise);

      render(
        <LicenseProvider>
          <LicenseConsumer />
        </LicenseProvider>
      );
      await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));

      act(() => {
        notifyLicenseRevoked();
      });
      expect(screen.getByTestId('loading')).toHaveTextContent('true');

      await act(async () => {
        refresh.resolve([LIFECYCLE]);
        await refresh.promise;
      });
      await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));
    });

    it('does not let a slow initial fetch re-add guide after a revocation refresh dropped it', async () => {
      // Race: the mount fetch is still in flight when a revocation arrives. refresh() drops guide,
      // then the slow mount fetch resolves still reporting guide — it must not restore it.
      const mountFetch = deferred<LicensedSolution[]>();
      jest
        .spyOn(licenseApi, 'fetchLicensedSolutions')
        .mockReturnValueOnce(mountFetch.promise) // initial mount fetch (slow, still pending)
        .mockResolvedValueOnce([LIFECYCLE]); // revocation refresh

      render(
        <LicenseProvider>
          <LicenseConsumer />
        </LicenseProvider>
      );

      // Revocation arrives and its refresh completes while the mount fetch is still pending.
      await act(async () => {
        notifyLicenseRevoked();
      });
      await waitFor(() => expect(screen.getByTestId('has-guide')).toHaveTextContent('false'));

      // The slow mount fetch now resolves, still listing guide — it must not re-add it.
      await act(async () => {
        mountFetch.resolve([LIFECYCLE, GUIDE]);
        await mountFetch.promise;
      });

      expect(screen.getByTestId('has-guide')).toHaveTextContent('false');
      expect(screen.getByTestId('solutions')).toHaveTextContent('lifecycle');
    });

    it('drops guide even when the revocation refetch still reports it licensed', async () => {
      // The Guide data API gates on GUIDE_SEARCH while /solutions/licensed gates on GUIDE, so a
      // refetch triggered by losing GUIDE_SEARCH can still return guide. The marker is
      // authoritative, so guide must be dropped regardless — otherwise the gate would re-show the
      // Guide UI and the next data call would 403 again (loop).
      jest
        .spyOn(licenseApi, 'fetchLicensedSolutions')
        .mockResolvedValueOnce([LIFECYCLE, GUIDE])
        .mockResolvedValueOnce([LIFECYCLE, GUIDE]);

      render(
        <LicenseProvider>
          <LicenseConsumer />
        </LicenseProvider>
      );
      await waitFor(() => expect(screen.getByTestId('has-guide')).toHaveTextContent('true'));

      await act(async () => {
        notifyLicenseRevoked();
      });

      await waitFor(() => expect(screen.getByTestId('has-guide')).toHaveTextContent('false'));
      expect(screen.getByTestId('solutions')).toHaveTextContent('lifecycle');
      expect(screen.getByTestId('error')).toHaveTextContent('false');
    });

    it('drops guide (so the learn-more page shows) when the revocation refetch fails', async () => {
      jest
        .spyOn(licenseApi, 'fetchLicensedSolutions')
        .mockResolvedValueOnce([LIFECYCLE, GUIDE])
        .mockRejectedValueOnce(new Error('503 Service Unavailable'));

      render(
        <LicenseProvider>
          <LicenseConsumer />
        </LicenseProvider>
      );
      await waitFor(() => expect(screen.getByTestId('has-guide')).toHaveTextContent('true'));

      await act(async () => {
        notifyLicenseRevoked();
      });
      await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'));

      // The revocation marker is authoritative: a failed refetch drops guide (LicenseGate then
      // renders the learn-more page) rather than leaving the user on a Guide UI the backend 403s —
      // which would also loop via remount/re-fire. Other solutions are unaffected and retained.
      expect(screen.getByTestId('has-guide')).toHaveTextContent('false');
      expect(screen.getByTestId('solutions')).toHaveTextContent('lifecycle');
      expect(screen.getByTestId('error')).toHaveTextContent('false');
    });
  });
});
