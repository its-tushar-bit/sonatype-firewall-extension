/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  getMinimalValidFormState,
  getPayload,
} from 'TestRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalTestData';
import {
  getOriginalValues,
  toFormState,
  toServerData,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalUtil';
import { FAKE_PASSWORD } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSlice';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

describe('artifactoryRepositoryConfigurationModalUtil', function () {
  describe('getOriginalValues', () => {
    it('returns the `serverData` with a username and password', function () {
      const serverData = getPayload(false);

      const originalValues = getOriginalValues(serverData);

      expect(originalValues).toEqual(serverData);
    });

    it('returns the `serverData` with a username and fake password', function () {
      const serverData = getPayload(false);
      delete serverData.password;

      const originalValues = getOriginalValues(serverData);

      expect(originalValues).toEqual({
        ...serverData,
        password: FAKE_PASSWORD,
      });
    });

    it('returns the `serverData` with an anonymous user', function () {
      const serverData = getPayload(true);

      const originalValues = getOriginalValues(serverData);

      expect(originalValues).toEqual({
        ...serverData,
        username: '',
        password: '',
      });
    });

    it('returns the initialState if serverData is not set', function () {
      const originalValues = getOriginalValues(null);

      expect(originalValues).toEqual({ baseUrl: '', isAnonymous: true, username: '', password: '' });
    });
  });

  describe('toServerData', () => {
    it('converts an anonymous `formState` to a payload', function () {
      const formState = getMinimalValidFormState();

      expect(toServerData(formState)).toEqual(getPayload(true));
    });

    it('converts a `formState` with credentials to a payload', function () {
      const formState = {
        baseUrlState: nxTextInputStateHelpers.initialState('someBaseUrl'),
        isAnonymous: false,
        usernameState: nxTextInputStateHelpers.initialState('someUsername'),
        passwordState: nxTextInputStateHelpers.initialState('somePassword'),
      };

      const expectedPayload = getPayload(false);
      delete expectedPayload.isAnonymous;
      expect(toServerData(formState)).toEqual(expectedPayload);
    });
  });

  describe('toFormState', () => {
    it('converts an anonymous `serverData` to a `formState`', function () {
      const serverData = getPayload(true);

      expect(toFormState(serverData)).toEqual(getMinimalValidFormState());
    });

    it('converts a `serverData` with credentials to a `formState`', function () {
      const serverData = getPayload(false);
      delete serverData.isAnonymous;

      expect(toFormState(serverData)).toEqual({
        baseUrlState: nxTextInputStateHelpers.initialState('someBaseUrl'),
        isAnonymous: false,
        usernameState: nxTextInputStateHelpers.initialState('someUsername'),
        passwordState: nxTextInputStateHelpers.initialState('somePassword'),
      });
    });

    it('converts a `serverData` with just a `username` to a `formState`', function () {
      const serverData = getPayload(false);
      delete serverData.isAnonymous;
      delete serverData.password;

      expect(toFormState(serverData)).toEqual({
        baseUrlState: nxTextInputStateHelpers.initialState('someBaseUrl'),
        isAnonymous: false,
        usernameState: nxTextInputStateHelpers.initialState('someUsername'),
        passwordState: nxTextInputStateHelpers.initialState(FAKE_PASSWORD),
      });
    });
  });
});
