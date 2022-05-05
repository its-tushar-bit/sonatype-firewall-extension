/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectFirewall, selectFirewallCDP, selectFirewallCDPRouteParams } from 'MainRoot/firewall/firewallSelectors';

describe('firewallSelectors', () => {
  const minState = {
    firewall: {
      cdp: {
        componentDetails: {},
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
        scanId: '123',
        tabId: 'overview',
        notValidProperty: 'notValidProperty',
      },
    },
  };

  it('gets firewall state', () => {
    expect(selectFirewall(minState)).toEqual(minState.firewall);
  });

  it('gets firewall cdp state', () => {
    expect(selectFirewallCDP(minState)).toEqual(minState.firewall.cdp);
  });

  it('gets repository component proprties from router.currentParams state', () => {
    expect(Object.keys(selectFirewallCDPRouteParams(minState))).toEqual([
      'repositoryId',
      'componentIdentifier',
      'componentHash',
      'matchState',
      'proprietary',
      'identificationSource',
      'scanId',
      'tabId',
    ]);
  });
});
