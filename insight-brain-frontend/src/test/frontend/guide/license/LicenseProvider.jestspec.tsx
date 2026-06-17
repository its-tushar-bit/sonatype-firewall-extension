/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { LicenseProvider, useLicense } from 'GuideRoot/license/LicenseProvider';
import * as licenseApi from 'GuideRoot/license/licenseApi';

jest.mock('GuideRoot/license/licenseApi');

function LicenseConsumer() {
  const { solutions, isLoading, hasError, hasSolution } = useLicense();
  return (
    <div>
      <span data-testid="loading">{String(isLoading)}</span>
      <span data-testid="error">{String(hasError)}</span>
      <span data-testid="solutions">{solutions.map((s) => s.id).join(',')}</span>
      <span data-testid="has-guide">{String(hasSolution('guide'))}</span>
      <span data-testid="has-lifecycle">{String(hasSolution('lifecycle'))}</span>
    </div>
  );
}

describe('LicenseProvider', () => {
  afterEach(() => {
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
});
