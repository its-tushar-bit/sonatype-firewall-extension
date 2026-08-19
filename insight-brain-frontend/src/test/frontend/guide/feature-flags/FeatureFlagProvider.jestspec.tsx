/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import {
  FeatureFlagProvider,
  useFeatureFlags,
} from 'GuideRoot/feature-flags/FeatureFlagProvider';
import { FEATURE_FLAGS } from 'GuideRoot/feature-flags/featureFlags';
import * as featureFlagsApi from 'GuideRoot/feature-flags/featureFlagsApi';

jest.mock('GuideRoot/feature-flags/featureFlagsApi');

function FeatureFlagsConsumer() {
  const { isLoading, isFeatureEnabled } = useFeatureFlags();
  return (
    <div>
      <span data-testid="loading">{String(isLoading)}</span>
      <span data-testid="has-guide-ui">
        {String(isFeatureEnabled(FEATURE_FLAGS.GUIDE_UI))}
      </span>
    </div>
  );
}

describe('FeatureFlagProvider', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('starts in loading state then exposes fetched flags', async () => {
    jest
      .spyOn(featureFlagsApi, 'fetchFeatureFlags')
      .mockResolvedValue(['guide-ui', 'sbom-manager']);

    render(
      <FeatureFlagProvider>
        <FeatureFlagsConsumer />
      </FeatureFlagProvider>
    );

    expect(screen.getByTestId('loading')).toHaveTextContent('true');

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(screen.getByTestId('has-guide-ui')).toHaveTextContent('true');
  });

  it('isFeatureEnabled returns false when the flag is not in the list', async () => {
    jest
      .spyOn(featureFlagsApi, 'fetchFeatureFlags')
      .mockResolvedValue(['sbom-manager']);

    render(
      <FeatureFlagProvider>
        <FeatureFlagsConsumer />
      </FeatureFlagProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(screen.getByTestId('has-guide-ui')).toHaveTextContent('false');
  });

  it('defaults to empty flag list when fetch fails', async () => {
    jest
      .spyOn(featureFlagsApi, 'fetchFeatureFlags')
      .mockRejectedValue(new Error('500 Internal Server Error'));

    render(
      <FeatureFlagProvider>
        <FeatureFlagsConsumer />
      </FeatureFlagProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('loading')).toHaveTextContent('false');
    });
    expect(screen.getByTestId('has-guide-ui')).toHaveTextContent('false');
  });

  it('throws when useFeatureFlags is used outside FeatureFlagProvider', () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => render(<FeatureFlagsConsumer />)).toThrow(
      'useFeatureFlags must be used within a FeatureFlagProvider'
    );

    consoleSpy.mockRestore();
  });
});
