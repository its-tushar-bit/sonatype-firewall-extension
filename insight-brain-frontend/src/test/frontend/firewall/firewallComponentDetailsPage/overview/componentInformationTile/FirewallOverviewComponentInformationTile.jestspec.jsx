/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '../../../../SpecUtil';

import * as FirewallOverviewComponentInformationTile from 'MainRoot/firewall/firewallComponentDetailsPage/overview/componentInformationTile/FirewallOverviewComponentInformationTile';
import * as firewallSelectors from 'MainRoot/firewall/firewallSelectors';
import * as firewallActions from 'MainRoot/firewall/firewallActions';

import 'TestRoot/SpecUtil';

describe('FirewallOverviewComponentInformationTile', () => {
  let originalSelectFirewallComponentDetailsPage = firewallSelectors.selectFirewallComponentDetailsPage,
    minState;

  beforeEach(function () {
    minState = {
      firewall: {
        componentDetailsPage: {
          isLoadingComponentDetails: false,
          componentDetails: {
            matchState: 'exact',
            pathnames: ['componentPath'],
            identificationSource: 'Sonatype',
            website: 'mywebsite.com',
            loading: false,
            loadError: null,
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

    jest.spyOn(firewallActions, 'loadComponentDetails');
  });

  it('renders a FirewallOverviewComponentInformation', async () => {
    jest
      .spyOn(firewallSelectors, 'selectFirewallComponentDetailsPage')
      .mockReturnValue(originalSelectFirewallComponentDetailsPage(minState));
    render(<FirewallOverviewComponentInformationTile.default />);

    expect(screen.getByText('Component Information')).toBeVisible();
  });

  it('renders a spiner when contents are not ready', async () => {
    jest.spyOn(firewallSelectors, 'selectFirewallComponentDetailsPage').mockReturnValue(
      originalSelectFirewallComponentDetailsPage({
        ...minState,
        firewall: {
          ...minState.firewall,
          componentDetailsPage: {
            ...minState.firewall.componentDetailsPage,
            isLoadingComponentDetails: true,
          },
        },
      })
    );
    render(<FirewallOverviewComponentInformationTile.default />);

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders an error message when there was a load error', async () => {
    jest.spyOn(firewallSelectors, 'selectFirewallComponentDetailsPage').mockReturnValue(
      originalSelectFirewallComponentDetailsPage({
        ...minState,
        firewall: {
          ...minState.firewall,
          componentDetailsPage: {
            ...minState.firewall.componentDetailsPage,
            componentDetailsError: 'Custom Error',
          },
        },
      })
    );
    render(<FirewallOverviewComponentInformationTile.default />);

    expect(screen.getByText(/Custom Error/)).toBeVisible();
  });
});
