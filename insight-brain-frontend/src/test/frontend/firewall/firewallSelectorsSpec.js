/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectFirewall,
  selectFirewallCDP,
  selectFirewallCDPRouteParams,
  currentFirewallCDPComponentVersion,
} from 'MainRoot/firewall/firewallSelectors';

describe('firewallSelectors', () => {
  const minState = {
    firewall: {
      cdp: {
        componentDetails: {
          componentIdentifier: {
            coordinates: {
              version: 1.6,
            },
          },
        },
        componentDetailsError: '',
        isLoadingComponentDetails: false,
      },
    },
    router: {
      currentParams: {
        repositoryId: 'abc',
        componentIdentifier: 'abc123',
        componentHash: 'abc123456',
        matchState: 'exact',
        proprietary: 'false',
        identificationSource: 'sonatype',
        tabId: 'overview',
        notValidProperty: 'notValidProperty',
        pathname: 'pathname',
      },
    },
  };

  it('gets firewall state', () => {
    expect(selectFirewall(minState)).toEqual(minState.firewall);
  });

  it('gets firewall cdp state', () => {
    expect(selectFirewallCDP(minState)).toEqual(minState.firewall.cdp);
  });

  it('gets repository component properties from router.currentParams state', () => {
    expect(Object.keys(selectFirewallCDPRouteParams(minState))).toEqual([
      'repositoryId',
      'componentIdentifier',
      'componentHash',
      'matchState',
      'proprietary',
      'identificationSource',
      'tabId',
      'pathname',
    ]);
  });

  it('gets current repository component version from firewall.cdp state', () => {
    expect(currentFirewallCDPComponentVersion(minState)).toEqual(1.6);
  });
});
