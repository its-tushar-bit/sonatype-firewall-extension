/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { getUserLimits } from '../../../../main/frontend/configuration/gettingStarted/gettingStartedUtils';

describe('gettingStartedUtils', () => {
  describe('userLimits', function () {
    it('is set to array of Lifecycle and Firewall userLimits objects if license contains both', function () {
      const newProps = {
        firewallUsersToDisplay: 1000,
        licensedUsersToDisplay: 2000,
      };

      const userLimits = getUserLimits(newProps);

      expect(userLimits).toEqual([
        { name: 'Lifecycle', count: 2000 },
        { name: 'Firewall', count: 1000 },
      ]);
    });

    it('is set to array with single Lifecycle userLimits object if license contains only Lifecycle value', () => {
      const newProps = {
        firewallUsersToDisplay: null,
        licensedUsersToDisplay: 2000,
      };

      const userLimits = getUserLimits(newProps);

      expect(userLimits).toEqual([{ name: 'Lifecycle', count: 2000 }]);
    });

    it('is set to array with single Lifecycle userLimits object if Firewall value is null', function () {
      const newProps = {
        firewallUsersToDisplay: null,
        licensedUsersToDisplay: 2000,
      };

      const userLimits = getUserLimits(newProps);

      expect(userLimits).toEqual([{ name: 'Lifecycle', count: 2000 }]);
    });

    it('is set to array with single Firewall userLimits object if license contains only Firewall value', function () {
      const newProps = {
        firewallUsersToDisplay: 1000,
        licensedUsersToDisplay: null,
      };

      const userLimits = getUserLimits(newProps);

      expect(userLimits).toEqual([{ name: 'Firewall', count: 1000 }]);
    });

    it('is set to array with single Firewall userLimits object if Lifecycle value is null', function () {
      const newProps = {
        firewallUsersToDisplay: 1000,
        licensedUsersToDisplay: null,
      };

      const userLimits = getUserLimits(newProps);

      expect(userLimits).toEqual([{ name: 'Firewall', count: 1000 }]);
    });

    it('is set to empty array if license contains neither Lifecycle nor Firewall value', function () {
      const userLimits = getUserLimits({});

      expect(userLimits).toEqual([]);
    });
  });
});
