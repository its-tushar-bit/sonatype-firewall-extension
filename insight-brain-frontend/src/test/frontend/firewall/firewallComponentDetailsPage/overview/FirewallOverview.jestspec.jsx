/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from '../../../SpecUtil';

import FirewallOverview from 'MainRoot/firewall/firewallComponentDetailsPage/overview/FirewallOverview';

import * as FirewallOverviewComponentInformationTile from 'MainRoot/firewall/firewallComponentDetailsPage/overview/componentInformationTile/FirewallOverviewComponentInformationTile';
import * as firewallSelectors from 'MainRoot/firewall/firewallSelectors';

import 'TestRoot/SpecUtil';

describe('FirewallOverview', () => {
  let originalSelectFirewallComponentDetailsPage = firewallSelectors.selectFirewallComponentDetailsPage,
    originalSelectFirewallComponentDetailsPageRouteParams =
      firewallSelectors.selectFirewallComponentDetailsPageRouteParams,
    minState;

  beforeEach(function () {
    jest
      .spyOn(FirewallOverviewComponentInformationTile, 'default')
      .mockImplementation(() => <div>FirewallOverviewComponentInformationTile</div>);
    jest.spyOn(firewallSelectors, 'selectFirewallComponentDetailsPageRouteParams').mockImplementation(() => {
      return originalSelectFirewallComponentDetailsPageRouteParams(minState);
    });

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
          componentDetailsError: null,
        },
      },
      router: {
        currentParams: {
          repositoryId: 'abc',
          componentIdentifier: JSON.stringify({
            format: 'maven',
            coordinates: {
              artifactId: 'ant',
              classifier: '',
              extension: 'jar',
              groupId: 'ant',
              version: '1.6',
            },
          }),
          componentHash: 'abc123456',
          matchState: 'exact',
          tabId: 'overview',
          notValidProperty: 'notValidProperty',
          pathname: 'pathname',
        },
      },
    };
  });

  it('renders the Version Explorer tile if component is known', () => {
    jest.spyOn(firewallSelectors, 'selectFirewallComponentDetailsPage').mockImplementation(() => {
      return originalSelectFirewallComponentDetailsPage(minState);
    });
    render(<FirewallOverview />);
    expect(screen.getByText('Version Explorer')).toBeVisible();
  });

  it('does not render the RiskRemediation if component is unknown', () => {
    jest.spyOn(firewallSelectors, 'selectFirewallComponentDetailsPage').mockImplementation(() => {
      return originalSelectFirewallComponentDetailsPage({
        ...minState,
        firewall: {
          ...minState.firewall,
          componentDetailsPage: {
            ...minState.firewall.componentDetailsPage,
            componentDetails: {
              ...minState.firewall.componentDetailsPage.componentDetails,
              matchState: 'unknown',
            },
          },
        },
        router: {
          currentParams: {
            repositoryId: 'abc',
            componentIdentifier: JSON.stringify({
              format: 'maven',
              coordinates: {
                artifactId: 'ant',
                classifier: '',
                extension: 'jar',
                groupId: 'ant',
                version: '1.6',
              },
            }),
            componentHash: 'hash1',
            matchState: 'exact',
            identificationSource: 'sonatype',
            tabId: 'overview',
            notValidProperty: 'notValidProperty',
            pathname: 'pathname',
          },
        },
      });
    });
    render(<FirewallOverview />);
    expect(screen.queryByText('Risk Remediation')).toBeNull();
  });
});
