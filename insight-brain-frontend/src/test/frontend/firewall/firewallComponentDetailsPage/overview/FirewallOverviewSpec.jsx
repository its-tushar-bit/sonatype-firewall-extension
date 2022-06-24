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

describe('FirewallOverview', () => {
  let originalSelectFirewallCDP = firewallSelectors.selectFirewallCDP,
    originalCurrentFirewallCDPComponentVersion = firewallSelectors.currentFirewallCDPComponentVersion,
    minState;

  beforeEach(function () {
    spyOn(FirewallOverviewComponentInformationTile, 'default').and.callFake(() => (
      <div>FirewallOverviewComponentInformationTile</div>
    ));
    spyOn(firewallSelectors, 'currentFirewallCDPComponentVersion').and.callFake(() => {
      return originalCurrentFirewallCDPComponentVersion(minState);
    });

    minState = {
      firewall: {
        cdp: {
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
    };
  });

  it('renders the RiskRemediation if component is known', () => {
    spyOn(firewallSelectors, 'selectFirewallCDP').and.callFake(() => {
      return originalSelectFirewallCDP(minState);
    });
    render(<FirewallOverview />);
    expect(screen.getByText('Risk Remediation')).toBeVisible();
  });

  it('does not render the RiskRemediation if component is unknown', () => {
    spyOn(firewallSelectors, 'selectFirewallCDP').and.callFake(() => {
      return originalSelectFirewallCDP({
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
      });
    });
    render(<FirewallOverview />);
    expect(screen.queryByText('Risk Remediation')).toBeNull();
  });
});
