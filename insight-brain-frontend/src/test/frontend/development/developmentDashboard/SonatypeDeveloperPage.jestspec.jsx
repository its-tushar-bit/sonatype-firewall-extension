/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import SonatypeDeveloperPage from 'MainRoot/development/developmentDashboard/SonatypeDeveloperPage';
import { SECTIONS } from 'MainRoot/development/developmentDashboard/sections';

import * as ProductFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { DEVELOPER_FEATURE_DISABLED_MESSAGE } from 'MainRoot/development/developmentDashboard/LicenseLockScreen';

describe('SonatypeDeveloperPage', () => {
  let renderComponent;
  let selectIsDeveloperDashboardEnabled;

  const defaultPreloadedState = {
    router: {
      currentState: {
        name: `integrations.${SECTIONS.OVERVIEW}`,
      },
    },
  };

  beforeEach(() => {
    selectIsDeveloperDashboardEnabled = jest
      .spyOn(ProductFeaturesSelectors, 'selectIsDeveloperDashboardEnabled')
      .mockReturnValue(true);

    renderComponent = (preloadedState) =>
      render(<SonatypeDeveloperPage />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders an alert', () => {
    renderComponent();
    const infoIcon = screen.getByRole('img', { name: /info/i });
    const alertContainer = infoIcon.parentElement;
    expect(infoIcon).toBeInTheDocument();
    expect(alertContainer).toHaveTextContent(
      'Sonatype Development is available for free in the Product Preview Program (PPP). Innovate with us by submitting your feedback to sonatype-developer@sonatype.com.'
    );
  });

  it('renders a heading "Sonatype Developer"', () => {
    renderComponent();
    expect(screen.getByRole('heading', { name: 'Sonatype Development' })).toBeInTheDocument();
  });

  it('renders an alert in place of content given the feature is not enabled for the license', async () => {
    selectIsDeveloperDashboardEnabled.mockReturnValue(false);

    renderComponent();

    const alert = screen.getByRole('alert');
    expect(alert).toBeInTheDocument();
    expect(alert).toHaveTextContent(DEVELOPER_FEATURE_DISABLED_MESSAGE);

    expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
  });

  describe('tabs', () => {
    it('renders a tablist', () => {
      renderComponent();

      const tablist = screen.getByRole('tablist');
      expect(tablist).toBeInTheDocument();
    });

    it('renders 5 tabs', () => {
      renderComponent();

      const tablist = screen.getByRole('tablist');
      const tabs = within(tablist).getAllByRole('tab');
      expect(tabs).toHaveLength(5);
    });

    it('renders the tabs in correct order', () => {
      renderComponent();

      const tabNamesInOrder = [
        'Overview',
        'CI/CD Integrations',
        'SCM Integrations',
        'Issue Tracking Integrations',
        'IDE Integrations',
      ];
      const tablist = screen.getByRole('tablist');
      const tabs = within(tablist).getAllByRole('tab');

      tabs.forEach((tab, id) => {
        expect(tab).toHaveTextContent(tabNamesInOrder[id]);
      });
    });

    it('overview tab is selected by default', () => {
      renderComponent();
      const overviewTab = screen.getByRole('tab', { name: 'Overview Overview' });

      expect(overviewTab).toHaveAttribute('aria-selected', 'true');
    });

    describe('relevant tabs are highlighted based on router state', () => {
      it('CI/CD Integrations tab', () => {
        const cicdState = {
          router: {
            currentState: {
              name: `integrations.${SECTIONS.CICD}`,
            },
          },
        };
        renderComponent(cicdState);
        const cicdTab = screen.getByRole('tab', { name: 'CI/CD Integrations CI/CD Integrations' });

        expect(cicdTab).toHaveAttribute('aria-selected', 'true');
      });

      it('SCM Integrations tab', () => {
        const scmState = {
          router: {
            currentState: {
              name: `integrations.${SECTIONS.SCM}`,
            },
          },
        };
        renderComponent(scmState);
        const scmTab = screen.getByRole('tab', { name: 'SCM Integrations SCM Integrations' });

        expect(scmTab).toHaveAttribute('aria-selected', 'true');
      });

      it('Issue Tracking Integrations tab', () => {
        const issueTrackingState = {
          router: {
            currentState: {
              name: `integrations.${SECTIONS.ISSUE_TRACKING}`,
            },
          },
        };
        renderComponent(issueTrackingState);
        const issueTrackingTab = screen.getByRole('tab', {
          name: 'Issue Tracking Integrations Issue Tracking Integrations',
        });

        expect(issueTrackingTab).toHaveAttribute('aria-selected', 'true');
      });

      it('IDE Integrations tab', () => {
        const ideState = {
          router: {
            currentState: {
              name: `integrations.${SECTIONS.IDE}`,
            },
          },
        };
        renderComponent(ideState);
        const ideTab = screen.getByRole('tab', { name: 'IDE Integrations IDE Integrations' });

        expect(ideTab).toHaveAttribute('aria-selected', 'true');
      });
    });
  });
});
