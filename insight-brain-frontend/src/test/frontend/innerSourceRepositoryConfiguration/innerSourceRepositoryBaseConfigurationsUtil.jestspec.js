/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  getOriginalValues,
  toFormState,
  toServerData,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsUtil';

describe('innerSourceRepositoryBaseConfigurationsUtil', function () {
  describe('getOriginalValues', () => {
    it('returns values from the passed `repositoryConnectionStatus` if it exists', function () {
      const repositoryConnectionStatus = {
        enabled: false,
        allowOverride: false,
      };
      expect(getOriginalValues(repositoryConnectionStatus)).toEqual({
        enabled: false,
        allowOverride: false,
      });
    });

    it('returns initial state values if `repositoryConnectionStatus` does not exist', function () {
      expect(getOriginalValues(undefined)).toEqual({
        enabled: null,
        allowOverride: true,
      });
    });
  });

  describe('toFormState', () => {
    it('converts `repositoryConnectionStatus` to a `formState`', function () {
      const repositoryConnectionStatus = {
        enabled: true,
        allowOverride: false,
      };
      expect(toFormState(repositoryConnectionStatus)).toEqual({
        enabled: true,
        allowOverride: false,
      });
    });

    it('converts `repositoryConnectionStatus` to a `formState` if not set for root organization', function () {
      const repositoryConnectionStatus = {
        enabled: null,
        allowOverride: true,
        inheritedFromOrganizationId: null,
      };
      expect(toFormState(repositoryConnectionStatus)).toEqual({
        enabled: false,
        allowOverride: true,
      });
    });
  });

  describe('toServerData', () => {
    it('converts `formState` to a payload', function () {
      const formState = {
        enabled: true,
        allowOverride: true,
      };
      expect(toServerData(formState)).toEqual({
        enabled: formState.enabled,
        allowOverride: formState.allowOverride,
      });
    });
  });
});
