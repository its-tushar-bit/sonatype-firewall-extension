/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  getOriginalValues,
  toFormState,
  toServerData,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsUtil';

describe('artifactoryRepositoryBaseConfigurationsUtil', function () {
  describe('getOriginalValues', () => {
    it('returns values from the passed `artifactoryConnectionStatus` if it exists', function () {
      const artifactoryConnectionStatus = {
        enabled: false,
        allowOverride: false,
      };
      expect(getOriginalValues(artifactoryConnectionStatus)).toEqual({
        enabled: false,
        allowOverride: false,
      });
    });

    it('returns initial state values if `artifactoryConnectionStatus` does not exist', function () {
      expect(getOriginalValues(undefined)).toEqual({
        enabled: null,
        allowOverride: true,
      });
    });
  });

  describe('toFormState', () => {
    it('converts `artifactoryConnectionStatus` to a `formState`', function () {
      const artifactoryConnectionStatus = {
        enabled: true,
        allowOverride: false,
      };
      expect(toFormState(artifactoryConnectionStatus)).toEqual({
        enabled: true,
        allowOverride: false,
      });
    });

    it('converts `artifactoryConnectionStatus` to a `formState` if not set for root organization', function () {
      const artifactoryConnectionStatus = {
        enabled: null,
        allowOverride: true,
        inheritedFromOrganizationId: null,
      };
      expect(toFormState(artifactoryConnectionStatus)).toEqual({
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
