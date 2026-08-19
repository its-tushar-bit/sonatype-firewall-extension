/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';
import {
  getBaseLicenseOverrideUrl,
  getDeleteLicenseOverrideUrl,
  getSbomComponentDetailsUrl,
} from 'MainRoot/util/CLMLocation';
import * as licenseDetectionTileSelectors from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSelectors';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

describe('licenseDetectionsTileSlice SBOM Manager context', () => {
  let store, state, mock;

  const internalAppId = 'internal-app-id';
  const componentIdentifier = {
    format: 'maven',
    coordinates: { groupId: 'com.example', artifactId: 'lib', version: '1.0' },
  };
  const ownerType = 'application';
  const ownerId = 'owner-id';
  const editLicenseForm = {
    status: 'OVERRIDDEN',
    comment: { value: 'some comment' },
    scope: { ownerType, ownerId },
    licenseIds: ['Apache-2.0'],
  };

  const sbomManagerRouterState = {
    currentState: { name: 'sbomManager.component.legal', url: '/legal', data: {} },
    currentParams: { applicationPublicId: 'app-id', sbomVersion: '1.0', componentHash: 'abc123' },
  };

  beforeAll(() => {
    mock = axiosMockAdapter();
  });

  beforeEach(() => {
    state = {
      router: sbomManagerRouterState,
      sbomComponentDetailsPage: {
        loading: false,
        loadError: null,
        internalAppId,
        componentDetails: {
          componentIdentifier,
          displayName: 'lib',
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  describe('saveEditLicensesForm', () => {
    const { saveEditLicensesForm } = actions;

    it('testSaveEditLicensesForm_SbomManagerContext_DispatchesStandardReload', async () => {
      jest.spyOn(licenseDetectionTileSelectors, 'selectEditLicensesForm').mockReturnValue(editLicenseForm);

      mock.onPost(getBaseLicenseOverrideUrl(ownerType, ownerId)).reply(200);
      mock
        .onGet(getSbomComponentDetailsUrl(internalAppId, '1.0', 'abc123'))
        .reply(200, { componentIdentifier, licenses: [] });

      await store.dispatch(saveEditLicensesForm());
      jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      jest.useRealTimers();
      await new Promise((resolve) => setTimeout(resolve, 0));

      const dispatchedActions = store.getActions();
      expect(dispatchedActions).toHaveActionType(
        'componentDetailsLicenseDetectionsTile/saveEditLicensesForm/fulfilled'
      );
      expect(dispatchedActions).toHaveActionType('componentDetailsLicenseDetectionsTile/resetSubmitMaskState');
      expect(dispatchedActions).toHaveActionType('sbomComponentDetailsPage/loadComponentDetails/pending');
      expect(dispatchedActions).toHaveActionType('sbomComponentDetailsPage/loadComponentLicenses/pending');
    });
  });

  describe('deleteLicenseOverride', () => {
    const { deleteLicenseOverride } = actions;
    const licenseOverrideId = 'license-override-id';

    it('testDeleteLicenseOverride_SbomManagerContext_WithLicenseOverride_DispatchesStandardReload', async () => {
      jest.spyOn(licenseDetectionTileSelectors, 'selectEditLicensesForm').mockReturnValue({
        ...editLicenseForm,
        scope: { ownerType, ownerId, licenseOverride: { id: licenseOverrideId } },
      });

      mock.onDelete(getDeleteLicenseOverrideUrl(ownerType, ownerId, licenseOverrideId)).reply(204);
      mock
        .onGet(getSbomComponentDetailsUrl(internalAppId, '1.0', 'abc123'))
        .reply(200, { componentIdentifier, licenses: [] });

      await store.dispatch(deleteLicenseOverride());
      jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      jest.useRealTimers();
      await new Promise((resolve) => setTimeout(resolve, 0));

      const dispatchedActions = store.getActions();
      expect(dispatchedActions).toHaveActionType(
        'componentDetailsLicenseDetectionsTile/deleteLicenseOverride/fulfilled'
      );
      expect(dispatchedActions).toHaveActionType('componentDetailsLicenseDetectionsTile/resetSubmitMaskState');
      expect(dispatchedActions).toHaveActionType('sbomComponentDetailsPage/loadComponentDetails/pending');
      expect(dispatchedActions).toHaveActionType('sbomComponentDetailsPage/loadComponentLicenses/pending');
    });
  });
});
