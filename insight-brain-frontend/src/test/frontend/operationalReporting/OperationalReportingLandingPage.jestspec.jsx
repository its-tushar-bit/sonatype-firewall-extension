/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, waitFor } from '@testing-library/dom';
import { axiosMockAdapter, render } from 'TestRoot/SpecUtil';
import OperationalReportingLandingPage from 'MainRoot/operationalReporting/OperationalReportingLandingPage';
import { getProductFeaturesUrl } from 'MainRoot/util/CLMLocation';

describe('OperationalReportingLandingPage', () => {
  let axiosMock;

  const defaultPreloadedState = {
    productFeatures: {
      loading: false,
      loadError: null,
      productFeatures: {
        'integrated-enterprise-reporting': true,
      },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, [{ name: 'integrated-enterprise-reporting', enabled: true }]);
  });

  afterEach(() => {
    jest.restoreAllMocks();
    axiosMock.reset();
  });

  const renderComponent = (preloadedState) => {
    return render(<OperationalReportingLandingPage />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });
  };

  it('should render the page title and description', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: /Operational Reporting/i })).toBeInTheDocument();
    expect(
      screen.getByText(/Operational Reporting provides immediate, real-time insight into your activities/i)
    ).toBeInTheDocument();
  });

  it('should render Rapid Response Reports section', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: /Rapid Response Reports/i })).toBeInTheDocument();
  });

  it('should render React2Shell report card', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByRole('region', { name: 'React2Shell Impact' })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: /React2Shell Impact/i })).toBeInTheDocument();
    });
  });

  it('should render Contact Us section', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: /Contact Us/i })).toBeInTheDocument();
  });

  it('should render all three contact cards', () => {
    renderComponent();

    expect(screen.getByRole('heading', { name: /Schedule a Discussion/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Suggest an Improvement/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Receive Technical Support/i })).toBeInTheDocument();
  });

  it('should display loading state', () => {
    const loadingState = {
      ...defaultPreloadedState,
      productFeatures: {
        ...defaultPreloadedState.productFeatures,
        loading: true,
      },
    };

    renderComponent(loadingState);

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('should fetch product features on mount', async () => {
    const initialLoadingState = {
      ...defaultPreloadedState,
      productFeatures: {
        loading: false,
        loadError: null,
        productFeatures: {},
      },
    };

    renderComponent(initialLoadingState);

    await waitFor(
      () => {
        expect(axiosMock.history.get.length).toBeGreaterThan(0);
        const featuresCall = axiosMock.history.get.find((call) => call.url === getProductFeaturesUrl());
        expect(featuresCall).toBeDefined();
      },
      { timeout: 3000 }
    );
  });

  it('should render info tooltip for Rapid Response Reports', () => {
    renderComponent();

    const tooltipTrigger = screen.getByRole('heading', { name: /Rapid Response Reports/i }).parentElement;
    expect(tooltipTrigger.querySelector('.nx-icon')).toBeInTheDocument();
  });
});
