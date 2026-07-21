/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router';
import { Theme } from '@radix-ui/themes';
import { FeatureFlagProvider } from 'GuideRoot/feature-flags/FeatureFlagProvider';
import { FeatureGate } from 'GuideRoot/feature-flags/FeatureGate';
import { FEATURE_FLAGS } from 'GuideRoot/feature-flags/featureFlags';
import * as featureFlagsApi from 'GuideRoot/feature-flags/featureFlagsApi';

jest.mock('GuideRoot/feature-flags/featureFlagsApi');

function renderAtComponents(ui: React.ReactElement) {
  return render(
    <Theme>
      <FeatureFlagProvider>
        <MemoryRouter initialEntries={['/components']}>
          <Routes>
            <Route path="/" element={<div>Home Page</div>} />
            <Route path="/components" element={ui} />
          </Routes>
        </MemoryRouter>
      </FeatureFlagProvider>
    </Theme>
  );
}

describe('FeatureGate', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('renders children when the flag is enabled', async () => {
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue(['guide-ui']);

    renderAtComponents(
      <FeatureGate flag={FEATURE_FLAGS.GUIDE_UI}>
        <div>Components Content</div>
      </FeatureGate>
    );

    await waitFor(() => {
      expect(screen.getByText('Components Content')).toBeInTheDocument();
    });
  });

  it('redirects to / when the flag is disabled', async () => {
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue([]);

    renderAtComponents(
      <FeatureGate flag={FEATURE_FLAGS.GUIDE_UI}>
        <div>Components Content</div>
      </FeatureGate>
    );

    await waitFor(() => {
      expect(screen.getByText('Home Page')).toBeInTheDocument();
    });
    expect(screen.queryByText('Components Content')).not.toBeInTheDocument();
  });

  it('renders neither children nor redirect target while loading', () => {
    jest
      .spyOn(featureFlagsApi, 'fetchFeatureFlags')
      .mockReturnValue(new Promise(() => {}));

    renderAtComponents(
      <FeatureGate flag={FEATURE_FLAGS.GUIDE_UI}>
        <div>Components Content</div>
      </FeatureGate>
    );

    expect(screen.queryByText('Components Content')).not.toBeInTheDocument();
    expect(screen.queryByText('Home Page')).not.toBeInTheDocument();
  });

  it('redirects to / when the fetch fails (default-disabled)', async () => {
    jest
      .spyOn(featureFlagsApi, 'fetchFeatureFlags')
      .mockRejectedValue(new Error('500'));

    renderAtComponents(
      <FeatureGate flag={FEATURE_FLAGS.GUIDE_UI}>
        <div>Components Content</div>
      </FeatureGate>
    );

    await waitFor(() => {
      expect(screen.getByText('Home Page')).toBeInTheDocument();
    });
  });
});

function renderAtSearch(ui: React.ReactElement) {
  return render(
    <Theme>
      <FeatureFlagProvider>
        <MemoryRouter initialEntries={['/search']}>
          <Routes>
            <Route path="/" element={<div>Home Page</div>} />
            <Route path="/search" element={ui} />
          </Routes>
        </MemoryRouter>
      </FeatureFlagProvider>
    </Theme>
  );
}

describe('FeatureGate on /search route with AI_DEVELOPER flag', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('renders the search page when ai-developer is present', async () => {
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue(['ai-developer']);

    renderAtSearch(
      <FeatureGate flag={FEATURE_FLAGS.AI_DEVELOPER}>
        <div>Search Page</div>
      </FeatureGate>
    );

    await waitFor(() => {
      expect(screen.getByText('Search Page')).toBeInTheDocument();
    });
  });

  it('redirects to / when ai-developer is absent (only guide-ui present)', async () => {
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue(['guide-ui']);

    renderAtSearch(
      <FeatureGate flag={FEATURE_FLAGS.AI_DEVELOPER}>
        <div>Search Page</div>
      </FeatureGate>
    );

    await waitFor(() => {
      expect(screen.getByText('Home Page')).toBeInTheDocument();
    });
    expect(screen.queryByText('Search Page')).not.toBeInTheDocument();
  });

  it('redirects to / when no feature flags are present', async () => {
    jest.spyOn(featureFlagsApi, 'fetchFeatureFlags').mockResolvedValue([]);

    renderAtSearch(
      <FeatureGate flag={FEATURE_FLAGS.AI_DEVELOPER}>
        <div>Search Page</div>
      </FeatureGate>
    );

    await waitFor(() => {
      expect(screen.getByText('Home Page')).toBeInTheDocument();
    });
    expect(screen.queryByText('Search Page')).not.toBeInTheDocument();
  });
});
