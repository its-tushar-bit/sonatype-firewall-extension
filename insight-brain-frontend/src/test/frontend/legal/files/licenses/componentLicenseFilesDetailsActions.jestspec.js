/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import {
  getLicenseLegalComponentByComponentIdentifierUrl,
  getLicenseLegalComponentUrl,
  getOwnerHierarchyLegalReviewerUrl,
} from 'MainRoot/util/CLMLocation';
import { pathSet } from 'MainRoot/util/jsUtil';
import {
  LICENSE_DETAILS_SELECTED_LICENSE_FILE,
  loadComponentAndLicenseDetails,
  refreshLicenseFilesDetails,
} from '../../../../../main/frontend/legal/files/licenses/componentLicenseFilesDetailsActions';

import 'TestRoot/SpecUtil';

describe('ComponentLicenseFileDetailsAction', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store;
  let initialState = {
    advancedLegal: {
      component: {
        component: {
          componentIdentifier: 'componentIdentifier',
          hash: 'componentHash',
          licenseLegalData: {
            licenseFiles: [
              {
                relPath: '/test/LICENSE',
                content: 'This is MIT License',
              },
              {
                relPath: '/test/sub/license.txt',
                content: 'This is still MIT Licenses',
              },
            ],
          },
        },
      },
      availableScopes: {
        values: [
          { id: 'org', publicId: 'org', type: 'organization' },
          {
            id: 'ROOT_ORGANIZATION_ID',
            publicId: 'ROOT_ORGANIZATION_ID',
            type: 'organization',
          },
        ],
      },
    },
    router: {
      currentParams: {
        ownerType: 'organization',
        ownerId: 'org',
        hash: 'componentHash',
        componentIdentifier: 'componentIdentifier',
        licenseIndex: '0',
      },
    },
    componentLicenseFileDetails: {
      selectedLicense: 'selectedLicense',
      loadingLicenseDetails: 'loadingLicenseDetails',
    },
  };

  describe('load license details', function () {
    it('fetches license file details by hash when not loaded', function (done) {
      store = SpecUtil.mockReduxStore(pathSet(['advancedLegal', 'component', 'component'], undefined, initialState));

      const ownerHierarchyUrl = getOwnerHierarchyLegalReviewerUrl('organization', 'org');
      const licenseLegalComponent = getLicenseLegalComponentUrl('organization', 'org', 'componentHash');

      mockAxiosCalls({
        get: {
          [ownerHierarchyUrl]: Promise.resolve({ data: 'getData' }),
          [licenseLegalComponent]: Promise.resolve({ data: 'getData2' }),
        },
      });

      store.dispatch(loadComponentAndLicenseDetails('organization', 'org', 'componentHash', 1)).then(() => {
        expect(axios.get).toHaveBeenCalledWith(ownerHierarchyUrl);
        expect(axios.get).toHaveBeenCalledWith(licenseLegalComponent);
        done();
      });
    });

    it('fetches license file details by component identifier when not loaded', function (done) {
      let state = pathSet(['advancedLegal', 'component', 'component'], undefined, initialState);
      state = pathSet(['router', 'currentParams', 'hash'], undefined, state);
      store = SpecUtil.mockReduxStore(state);

      const ownerHierarchyUrl = getOwnerHierarchyLegalReviewerUrl('organization', 'org');
      const licenseLegalCompByCompIdentifier = getLicenseLegalComponentByComponentIdentifierUrl(
        'componentIdentifier',
        'organization',
        'org'
      );

      mockAxiosCalls({
        get: {
          [ownerHierarchyUrl]: Promise.resolve({ data: 'getData' }),
          [licenseLegalCompByCompIdentifier]: Promise.resolve({ data: 'getData2' }),
        },
      });

      store
        .dispatch(loadComponentAndLicenseDetails('organization', 'org', undefined, 1, 'componentIdentifier'))
        .then(() => {
          expect(axios.get).toHaveBeenCalledWith(ownerHierarchyUrl);
          expect(axios.get).toHaveBeenCalledWith(licenseLegalCompByCompIdentifier);
          done();
        });
    });

    it('immediately dispatches a LICENSE_DETAILS_SELECTED_LICENSE_FILE action. Component already in state', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadComponentAndLicenseDetails('organization', 'org', 'componentHash', 1));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(LICENSE_DETAILS_SELECTED_LICENSE_FILE);
    });
  });

  describe('refresh license file details', function () {
    it('immediately dispatches a LICENSE_DETAILS_SELECTED_LICENSE_FILE action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(refreshLicenseFilesDetails());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(LICENSE_DETAILS_SELECTED_LICENSE_FILE);
    });
  });
});
