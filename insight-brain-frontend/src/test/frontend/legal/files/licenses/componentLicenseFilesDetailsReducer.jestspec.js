/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../../../main/frontend/legal/files/licenses/componentLicenseFilesDetailsReducer';
import { LICENSE_DETAILS_SELECTED_LICENSE_FILE } from '../../../../../main/frontend/legal/files/licenses/componentLicenseFilesDetailsActions';

describe('ComponentLicenseFileDetailsReducer', function () {
  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);

      expect(newState.selectedLicense).toBeNull();
      expect(newState.licenseIndex).toBeNull();
      expect(newState.loadingLicenseDetails).toBeTruthy();
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = { foo: 'bar' };
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('License File Details action', function () {
    const originalState = {
      licenseIndex: null,
      selectedLicense: null,
      loadingLicenseDetails: true,
    };

    it('LICENSE_DETAILS_SELECTED_LICENSE_FILE sets selected license file', function () {
      const newState = reduce(originalState, {
        type: LICENSE_DETAILS_SELECTED_LICENSE_FILE,
        payload: {
          license: { relPath: '/license/', content: 'license' },
          licenseIndex: 1,
        },
      });
      expect(newState.loadingLicenseDetails).toBeFalsy();
      expect(newState.selectedLicense).toEqual({ relPath: '/license/', content: 'license' });
      expect(newState.licenseIndex).toBe(1);
    });
  });
});
