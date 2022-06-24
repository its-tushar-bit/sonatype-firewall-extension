/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from '../../../../SpecUtil';

import FirewallOverviewComponentInformation from 'MainRoot/firewall/firewallComponentDetailsPage/overview/componentInformationTile/FirewallOverviewComponentInformation';
import * as firewallSelectors from 'MainRoot/firewall/firewallSelectors';

describe('FirewallOverviewComponentInformation', () => {
  let minState;
  let originalSelectFirewallCDP = firewallSelectors.selectFirewallCDP;

  beforeEach(function () {
    minState = {
      firewall: {
        cdp: {
          isLoadingComponentDetails: false,
          componentDetails: {
            matchState: 'exact',
            identificationSource: 'Sonatype',
            website: 'mywebsite.com',
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6',
              },
            },
            componentCategories: [
              {
                componentCategoryId: 10,
                path: 'Build Tools',
              },
            ],
            displayName: {
              parts: [
                {
                  field: 'Group',
                  value: 'ant',
                },
                {
                  value: ' : ',
                },
                {
                  field: 'Artifact',
                  value: 'ant',
                },
                {
                  value: ' : ',
                },
                {
                  field: 'Version',
                  value: '1.6',
                },
              ],
              name: 'ant',
            },
          },
        },
      },
    };
  });

  it('renders a FirewallOverviewComponentInformation with "View Coordinates" button enabled of known components', async () => {
    spyOn(firewallSelectors, 'selectFirewallCDP').and.returnValue(originalSelectFirewallCDP(minState));
    render(<FirewallOverviewComponentInformation />);
    await waitFor(() => screen.getByText('Component Information'));
    expect(screen.getByText('Match State')).toBeVisible();
    expect(screen.getByText('Exact')).toBeVisible();
    expect(screen.getByText('Identification Source')).toBeVisible();
    expect(screen.getByText('Sonatype')).toBeVisible();
    expect(screen.getByText('Website')).toBeVisible();
    expect(screen.getByText('Visit Project Website')).toBeVisible();
    expect(screen.getByText('Category')).toBeVisible();
    expect(screen.getByText('Build Tools')).toBeVisible();
    expect(screen.getByText('View Coordinates')).toBeVisible();
  });

  it('renders a FirewallOverviewComponentInformation with "View Coordinates" button not present for unkwnon components', async () => {
    spyOn(firewallSelectors, 'selectFirewallCDP').and.returnValue(
      originalSelectFirewallCDP({
        ...minState,
        firewall: {
          ...minState.firewall,
          cdp: {
            ...minState.firewall.cdp,
            componentDetails: {
              ...minState.firewall.cdp.componentDetails,
              matchState: 'unknown',
            },
          },
        },
      })
    );

    render(<FirewallOverviewComponentInformation />);
    await waitFor(() => screen.getByText('Component Information'));
    expect(screen.getByText('Match State')).toBeVisible();
    expect(screen.getByText('Unknown')).toBeVisible();
    expect(screen.getByText('Identification Source')).toBeVisible();
    expect(screen.getByText('Sonatype')).toBeVisible();
    expect(screen.getByText('Website')).toBeVisible();
    expect(screen.getByText('Visit Project Website')).toBeVisible();
    expect(screen.getByText('Category')).toBeVisible();
    expect(screen.getByText('Build Tools')).toBeVisible();
    expect(screen.queryByText('View Coordinates')).toBeNull();
  });
});
