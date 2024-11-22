/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectFirewall,
  selectFirewallComponentDetailsPage,
  selectFirewallComponentDetailsPageRouteParams,
  currentFirewallComponentDetailsPageComponentVersion,
} from 'MainRoot/firewall/firewallSelectors';

describe('firewallSelectors', () => {
  const minState = {
    firewall: {
      componentDetailsPage: {
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
        tabId: 'overview',
        notValidProperty: 'notValidProperty',
        pathname: 'pathname',
      },
    },
  };

  it('gets firewall state', () => {
    expect(selectFirewall(minState)).toEqual(minState.firewall);
  });

  it('gets firewall componentDetailsPage state', () => {
    expect(selectFirewallComponentDetailsPage(minState)).toEqual(minState.firewall.componentDetailsPage);
  });

  it('gets repository component properties from router.currentParams state', () => {
    expect(Object.keys(selectFirewallComponentDetailsPageRouteParams(minState))).toEqual([
      'repositoryId',
      'componentIdentifier',
      'componentHash',
      'matchState',
      'tabId',
      'pathname',
      'componentDisplayName',
    ]);
  });

  it('gets current repository component version from firewall.componentDetailsPage state', () => {
    expect(currentFirewallComponentDetailsPageComponentVersion(minState)).toEqual(1.6);
  });
});
