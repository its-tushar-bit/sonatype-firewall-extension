/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import BulkWaiveTitle from 'MainRoot/waivers/BulkWaiveTitle';
import * as componentDetailsSelectors from 'MainRoot/componentDetails/componentDetailsSelectors';

describe('BulkWaiveTitle component', () => {
  let preloadedState;

  beforeEach(() => {
    preloadedState = getDefaultPreloadedState();
    jest.spyOn(componentDetailsSelectors, 'selectComponentName').mockReturnValue('com.example:default-component:1.0.0');
  });

  it('renders the page title "Bulk Waiver"', () => {
    renderComponent();

    const pageTitle = screen.getByRole('heading', { name: 'Bulk Waiver' });
    expect(pageTitle).toBeVisible();
  });

  describe('subtitle display for application report context', () => {
    it('displays application name and report title when not in component details context', () => {
      const stateWithoutHash = {
        ...preloadedState,
        router: {
          ...preloadedState.router,
          currentParams: {
            scanId: 'test-scan-id',
            // No hash param
          },
        },
        applicationReport: {
          ...preloadedState.applicationReport,
          metadata: {
            application: {
              name: 'Test Application',
            },
            reportTitle: 'v1.2.3',
          },
        },
      };

      renderComponent(stateWithoutHash);

      expect(screen.getByText('Test Application v1.2.3')).toBeVisible();
    });

    it('handles null metadata gracefully', () => {
      const stateWithNullMetadata = {
        ...preloadedState,
        applicationReport: {
          ...preloadedState.applicationReport,
          metadata: null,
        },
      };

      renderComponent(stateWithNullMetadata);

      // Should still render the page title
      expect(screen.getByRole('heading', { name: 'Bulk Waiver' })).toBeVisible();
    });
  });

  describe('subtitle display for component details context', () => {
    it('displays component name when in component details context (hash param present)', () => {
      const stateWithHash = {
        ...preloadedState,
        router: {
          ...preloadedState.router,
          currentParams: {
            scanId: 'test-scan-id',
            hash: 'component-hash-123',
          },
        },
      };

      renderComponent(stateWithHash);

      expect(screen.getByText('com.example:default-component:1.0.0')).toBeVisible();
    });

    it('displays empty subtitle when component name is null in component details context', () => {
      jest.spyOn(componentDetailsSelectors, 'selectComponentName').mockReturnValue(null);

      const stateWithHashButNoComponentName = {
        ...preloadedState,
        router: {
          ...preloadedState.router,
          currentParams: {
            scanId: 'test-scan-id',
            hash: 'component-hash-123',
          },
        },
      };

      renderComponent(stateWithHashButNoComponentName);

      // Should still render the page title
      expect(screen.getByRole('heading', { name: 'Bulk Waiver' })).toBeVisible();
      expect(screen.getByTestId('bulk-waiver-subtitle')).toHaveTextContent('');
    });
  });

  describe('context detection based on router params', () => {
    it('treats route as component details context when hash param is present', () => {
      const stateWithHash = {
        ...preloadedState,
        router: {
          ...preloadedState.router,
          currentParams: {
            scanId: 'test-scan-id',
            hash: 'some-hash',
          },
        },
        applicationReport: {
          ...preloadedState.applicationReport,
          metadata: {
            application: {
              name: 'Test App',
            },
            reportTitle: 'v1.0.0',
          },
        },
      };

      renderComponent(stateWithHash);

      // Should show component name, not application name + report title
      expect(screen.getByText('com.example:default-component:1.0.0')).toBeVisible();
      expect(screen.queryByText('Test App v1.0.0')).not.toBeInTheDocument();
    });

    it('treats route as application report context when hash param is absent', () => {
      const stateWithoutHash = {
        ...preloadedState,
        router: {
          ...preloadedState.router,
          currentParams: {
            scanId: 'test-scan-id',
          },
        },
        applicationReport: {
          ...preloadedState.applicationReport,
          metadata: {
            application: {
              name: 'Test App',
            },
            reportTitle: 'v1.0.0',
          },
        },
      };

      renderComponent(stateWithoutHash);

      // Should show application name + report title, not component name
      expect(screen.getByText('Test App v1.0.0')).toBeVisible();
      expect(screen.queryByText('com.example:default-component:1.0.0')).not.toBeInTheDocument();
    });

    it('treats route as application report context when hash param is empty string', () => {
      const stateWithEmptyHash = {
        ...preloadedState,
        router: {
          ...preloadedState.router,
          currentParams: {
            scanId: 'test-scan-id',
            hash: '',
          },
        },
        applicationReport: {
          ...preloadedState.applicationReport,
          metadata: {
            application: {
              name: 'Test App',
            },
            reportTitle: 'v1.0.0',
          },
        },
      };

      renderComponent(stateWithEmptyHash);

      // Empty string should be falsy, so should show application context
      expect(screen.getByText('Test App v1.0.0')).toBeVisible();
      expect(screen.queryByText('com.example:default-component:1.0.0')).not.toBeInTheDocument();
    });
  });

  describe('Pro Tier Gating', () => {
    it('shows Enterprise Feature tag when bulk-waivers feature is absent', () => {
      preloadedState = getProTierPreloadedState();
      renderComponent();
      expect(screen.getByText('Enterprise Feature')).toBeVisible();
    });

    it('still renders the page title', () => {
      preloadedState = getProTierPreloadedState();
      renderComponent();
      expect(screen.getByRole('heading', { name: /Bulk Waiver/ })).toBeVisible();
    });
  });

  function renderComponent(additionalState = {}) {
    const finalState = { ...preloadedState, ...additionalState };
    return render(<BulkWaiveTitle />, { preloadedState: finalState });
  }

  function getProTierPreloadedState() {
    return {
      productFeatures: {
        productFeatures: {},
      },
      productLicense: { license: { products: ['Sonatype Lifecycle Pro'] } },
      applicationReport: {
        metadata: {
          application: {
            name: 'Default Application',
          },
          reportTitle: 'v1.0.0',
        },
      },
      router: {
        currentParams: {
          scanId: 'default-scan-id',
        },
      },
    };
  }

  function getDefaultPreloadedState() {
    return {
      productFeatures: {
        productFeatures: { 'bulk-waivers': true },
      },
      applicationReport: {
        metadata: {
          application: {
            name: 'Default Application',
          },
          reportTitle: 'v1.0.0',
        },
      },
      router: {
        currentParams: {
          scanId: 'default-scan-id',
        },
      },
    };
  }
});
