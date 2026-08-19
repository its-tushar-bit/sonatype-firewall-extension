/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectCreateProxyRepositoryError,
  selectCreatingProxyRepository,
  selectFirewallIqProxy,
  selectSaveError,
  selectSaving,
} from 'MainRoot/firewall/iqProxy/firewallIqProxySelectors';

describe('firewallIqProxySelectors', () => {
  const mockState = {
    firewallIqProxy: {
      saving: false,
      saveError: null,
    },
  };

  describe('selectFirewallIqProxy', () => {
    it('returns the firewallIqProxy slice', () => {
      expect(selectFirewallIqProxy(mockState)).toEqual(mockState.firewallIqProxy);
    });
  });

  describe('selectSaving', () => {
    it('returns false when not saving', () => {
      expect(selectSaving(mockState)).toBe(false);
    });

    it('returns true when saving', () => {
      expect(selectSaving({ firewallIqProxy: { ...mockState.firewallIqProxy, saving: true } })).toBe(true);
    });
  });

  describe('selectSaveError', () => {
    it('returns null when there is no error', () => {
      expect(selectSaveError(mockState)).toBeNull();
    });

    it('returns the error message when present', () => {
      const errorMsg = 'Something went wrong';
      expect(selectSaveError({ firewallIqProxy: { ...mockState.firewallIqProxy, saveError: errorMsg } })).toBe(
        errorMsg
      );
    });
  });

  describe('proxy-repository create selectors (FIRE-665)', () => {
    const proxyState = {
      firewallIqProxy: {
        creatingProxyRepository: true,
        createProxyRepositoryError: 'create-err',
      },
    };

    it('selectCreatingProxyRepository returns creating flag', () => {
      expect(selectCreatingProxyRepository(proxyState)).toBe(true);
    });

    it('selectCreateProxyRepositoryError returns error', () => {
      expect(selectCreateProxyRepositoryError(proxyState)).toBe('create-err');
    });
  });
});
