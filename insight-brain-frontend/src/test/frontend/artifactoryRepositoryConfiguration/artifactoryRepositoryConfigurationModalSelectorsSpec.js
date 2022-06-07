/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  getInitialState,
  getPayload,
} from 'TestRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalTestData';
import {
  FAKE_PASSWORD,
  MISSING_OR_INVALID_DATA_MESSAGE,
  MUST_REENTER_PASSWORD_MESSAGE,
  NO_CHANGES_MESSAGE,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSlice';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { getOriginalValues } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalUtil';

describe('artifactoryRepositoryConfigurationModalSelectors', function () {
  let spyGetOriginalValues,
    selectApplicationId,
    selectFormState,
    selectArtifactoryRepositoryConfigurationModalSlice,
    selectIsUpdate,
    selectOrganizationId,
    selectOriginalValues,
    selectOwnerTypeAndOwnerId,
    selectArtifactoryConnectionId,
    selectServerData,
    selectIsDirty,
    selectHasAllRequiredData,
    selectIsPasswordNeededAndNotEntered,
    selectValidationErrors;

  beforeEach(() => {
    spyGetOriginalValues = jasmine.createSpy('getOriginalValues').and.callFake((serverData) => {
      return getOriginalValues(serverData);
    });
    const module = require('inject-loader!../../../../src/main/frontend/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalSelectors')(
      {
        'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryConfigurationModalUtil': {
          FAKE_PASSWORD,
          initialState: getInitialState(),
          getOriginalValues: spyGetOriginalValues,
        },
      }
    );
    ({
      selectApplicationId,
      selectFormState,
      selectArtifactoryRepositoryConfigurationModalSlice,
      selectIsUpdate,
      selectOrganizationId,
      selectOriginalValues,
      selectOwnerTypeAndOwnerId,
      selectArtifactoryConnectionId,
      selectServerData,
      selectIsDirty,
      selectHasAllRequiredData,
      selectIsPasswordNeededAndNotEntered,
      selectValidationErrors,
    } = module);
  });

  describe('selectOrganizationId', () => {
    it('selects `organizationId`', () => {
      const state = { router: { currentParams: { organizationId: 'someOrganizationId' } } };
      expect(selectOrganizationId(state)).toBe('someOrganizationId');
    });
  });

  describe('selectApplicationId', () => {
    it('selects `applicationId`', () => {
      const state = { router: { currentParams: { applicationId: 'someApplicationId' } } };
      expect(selectApplicationId(state)).toBe('someApplicationId');
    });
  });

  describe('selectOwnerTypeAndOwnerId', () => {
    it('selects the correct ownerType and ownerId given an organizationId parameter', function () {
      const state = { router: { currentParams: { organizationId: 'someOrganizationId' } } };
      expect(selectOwnerTypeAndOwnerId(state)).toEqual({
        ownerType: 'organization',
        ownerId: 'someOrganizationId',
      });
    });

    it('selects the correct ownerType and ownerId given an applicationId parameter', function () {
      const state = { router: { currentParams: { applicationId: 'someApplicationId' } } };
      expect(selectOwnerTypeAndOwnerId(state)).toEqual({
        ownerType: 'application',
        ownerId: 'someApplicationId',
      });
    });

    it('selects undefined if there is no organizationId parameter or applicationId parameter', function () {
      const state = { router: { currentParams: {} } };
      expect(selectOwnerTypeAndOwnerId(state)).toEqual(undefined);
    });
  });

  describe('selectArtifactoryConnectionId', () => {
    it('selects `artifactoryConnectionId`', () => {
      const state = {
        artifactoryRepositoryConfigurationModal: { artifactoryConnectionId: 'someArtifactoryConnectionId' },
      };
      expect(selectArtifactoryConnectionId(state)).toBe('someArtifactoryConnectionId');
    });
  });

  describe('selectIsUpdate', () => {
    it('selects `isUpdate` as true if a artifactoryConnectionId exists', () => {
      const state = {
        router: { currentParams: {} },
        artifactoryRepositoryConfigurationModal: {
          artifactoryConnectionId: 'someArtifactoryConnectionId',
        },
      };
      expect(selectIsUpdate(state)).toBeTruthy();
    });

    it('selects `isUpdate` as false if a artifactoryConnectionId does not exist', () => {
      const state = { router: { currentParams: {} } };
      expect(selectIsUpdate(state)).toBeFalsy();
    });
  });

  describe('selectArtifactoryRepositoryConfigurationModalSlice', () => {
    it('selects `artifactoryRepositoryConfigurationModal`', () => {
      const state = { artifactoryRepositoryConfigurationModal: 'someArtifactoryRepositoryConfigurationModal' };
      expect(selectArtifactoryRepositoryConfigurationModalSlice(state)).toBe(
        'someArtifactoryRepositoryConfigurationModal'
      );
    });
  });

  describe('selectServerData', () => {
    it('selects `serverData`', () => {
      const state = { artifactoryRepositoryConfigurationModal: { serverData: 'someServerData' } };
      expect(selectServerData(state)).toBe('someServerData');
    });
  });

  describe('selectOriginalValues', () => {
    it('selects the result of `getOriginalValues`', () => {
      spyGetOriginalValues.and.returnValue('result');
      const state = { artifactoryRepositoryConfigurationModal: { serverData: 'someServerData' } };
      expect(selectOriginalValues(state)).toBe('result');
      expect(spyGetOriginalValues).toHaveBeenCalledWith('someServerData');
    });
  });

  describe('selectFormState', () => {
    it('selects `formState`', () => {
      const state = { artifactoryRepositoryConfigurationModal: { formState: 'someFormState' } };
      expect(selectFormState(state)).toBe('someFormState');
    });
  });

  describe('selectIsDirty', () => {
    let state;

    beforeEach(() => {
      state = {
        artifactoryRepositoryConfigurationModal: getInitialState(),
      };
    });

    it('returns false if nothing has changed', function () {
      expect(selectIsDirty(state)).toBeFalsy();
    });

    it('returns true if the `baseUrl` has changed', function () {
      state.artifactoryRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );

      expect(selectIsDirty(state)).toBeTruthy();
    });

    it('returns true if `isAnonymous` has changed', function () {
      state.artifactoryRepositoryConfigurationModal.formState.isAnonymous = false;

      expect(selectIsDirty(state)).toBeTruthy();
    });

    it('returns false if the `username` has changed and isAnonymous is true', function () {
      state.artifactoryRepositoryConfigurationModal.formState.usernameState = nxTextInputStateHelpers.initialState(
        'someUsername'
      );

      expect(selectIsDirty(state)).toBeFalsy();
    });

    it('returns false if the `password` has changed and isAnonymous is true', function () {
      state.artifactoryRepositoryConfigurationModal.formState.passwordState = nxTextInputStateHelpers.initialState(
        'somePassword'
      );

      expect(selectIsDirty(state)).toBeFalsy();
    });

    it('returns true if the `username` has changed and isAnonymous is false', function () {
      state.artifactoryRepositoryConfigurationModal.serverData = {
        ...getPayload(false),
        password: FAKE_PASSWORD,
      };
      state.artifactoryRepositoryConfigurationModal.formState.usernameState = nxTextInputStateHelpers.initialState(
        'someOtherUsername'
      );

      expect(selectIsDirty(state)).toBeTruthy();
    });

    it('returns true if the `password` has changed and isAnonymous is false', function () {
      state.artifactoryRepositoryConfigurationModal.serverData = {
        ...getPayload(false),
        password: FAKE_PASSWORD,
      };
      state.artifactoryRepositoryConfigurationModal.formState.passwordState = nxTextInputStateHelpers.initialState(
        'somePassword'
      );

      expect(selectIsDirty(state)).toBeTruthy();
    });
  });

  describe('selectHasAllRequiredData', () => {
    let state;

    beforeEach(() => {
      state = {
        artifactoryRepositoryConfigurationModal: getInitialState(),
      };
    });

    it('returns false if the `baseUrl` is not set', function () {
      expect(selectHasAllRequiredData(state)).toBeFalsy();
    });

    it('returns true if the `baseUrl` is set and `isAnonymous` is true', function () {
      state.artifactoryRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );

      expect(selectHasAllRequiredData(state)).toBeTruthy();
    });

    it('returns false if the `baseUrl` is set and `isAnonymous` is false', function () {
      state.artifactoryRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
      state.artifactoryRepositoryConfigurationModal.formState.isAnonymous = false;

      expect(selectHasAllRequiredData(state)).toBeFalsy();
    });

    it('returns false if the `baseUrl` and `username` are set and `isAnonymous` is false', function () {
      state.artifactoryRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
      state.artifactoryRepositoryConfigurationModal.formState.isAnonymous = false;
      state.artifactoryRepositoryConfigurationModal.formState.usernameState = nxTextInputStateHelpers.initialState(
        'someUsername'
      );

      expect(selectHasAllRequiredData(state)).toBeFalsy();
    });

    it('returns false if the `baseUrl` and `password` are set and `isAnonymous` is false', function () {
      state.artifactoryRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
      state.artifactoryRepositoryConfigurationModal.formState.isAnonymous = false;
      state.artifactoryRepositoryConfigurationModal.formState.passwordState = nxTextInputStateHelpers.initialState(
        'somePassword'
      );

      expect(selectHasAllRequiredData(state)).toBeFalsy();
    });

    it('returns true if the `baseUrl`, `username`, and `password` are set and `isAnonymous` is false', function () {
      state.artifactoryRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
      state.artifactoryRepositoryConfigurationModal.formState.isAnonymous = false;
      state.artifactoryRepositoryConfigurationModal.formState.usernameState = nxTextInputStateHelpers.initialState(
        'someUsername'
      );
      state.artifactoryRepositoryConfigurationModal.formState.passwordState = nxTextInputStateHelpers.initialState(
        'somePassword'
      );

      expect(selectHasAllRequiredData(state)).toBeTruthy();
    });
  });

  describe('selectIsPasswordNeededAndNotEntered', () => {
    let state;

    beforeEach(() => {
      state = {
        artifactoryRepositoryConfigurationModal: {
          formState: { isAnonymous: false, passwordState: nxTextInputStateHelpers.initialState(FAKE_PASSWORD) },
        },
      };
    });

    it('returns false if `isAnonymous` is true', function () {
      state.artifactoryRepositoryConfigurationModal.formState.isAnonymous = true;
      expect(selectIsPasswordNeededAndNotEntered(state)).toBeFalsy();
    });

    it('returns false if `passwordState` is not pristine', function () {
      state.artifactoryRepositoryConfigurationModal.formState.passwordState.isPristine = false;
      expect(selectIsPasswordNeededAndNotEntered(state)).toBeFalsy();
    });

    it('returns false if `passwordState` `trimmedValue` is not FAKE_PASSWORD', function () {
      state.artifactoryRepositoryConfigurationModal.formState.passwordState.trimmedValue = '';
      expect(selectIsPasswordNeededAndNotEntered(state)).toBeFalsy();
    });

    it('returns true for an update where the form requires credentials and the password is unchanged', function () {
      expect(selectIsPasswordNeededAndNotEntered(state)).toBeTruthy();
    });
  });

  describe('selectValidationErrors', () => {
    let state;

    beforeEach(() => {
      state = {
        artifactoryRepositoryConfigurationModal: getInitialState(),
      };
    });

    it('returns MISSING_OR_INVALID_DATA_MESSAGE if `hasAllRequiredData` is false', function () {
      expect(selectValidationErrors(state)).toBe(MISSING_OR_INVALID_DATA_MESSAGE);
    });

    it('returns NO_CHANGES_MESSAGE if `hasAllRequiredData` is true and `isDirty` is false', function () {
      state.artifactoryRepositoryConfigurationModal.formState.baseUrlState = nxTextInputStateHelpers.initialState(
        'someBaseUrl'
      );
      state.artifactoryRepositoryConfigurationModal.serverData = getPayload(true);
      expect(selectValidationErrors(state)).toBe(NO_CHANGES_MESSAGE);
    });

    it('returns MUST_REENTER_PASSWORD_MESSAGE if `hasAllRequiredData` is true, `isDirty` is true, and `isPasswordNeededAndNotEntered` is true', function () {
      state.artifactoryRepositoryConfigurationModal.formState = {
        ...state.artifactoryRepositoryConfigurationModal.formState,
        baseUrlState: nxTextInputStateHelpers.userInput(null, 'someOtherBaseUrl'),
        isAnonymous: false,
        usernameState: nxTextInputStateHelpers.initialState('someUsername'),
        passwordState: nxTextInputStateHelpers.initialState(FAKE_PASSWORD),
      };
      state.artifactoryRepositoryConfigurationModal.serverData = getPayload(false);
      expect(selectValidationErrors(state)).toBe(MUST_REENTER_PASSWORD_MESSAGE);
    });
  });
});
