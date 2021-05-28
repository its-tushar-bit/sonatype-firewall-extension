/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  LICENSE_DETAILS_SELECTED_LICENSE_FILE,
  loadComponentAndLicenseDetails,
  refreshLicenseFilesDetails,
} from '../../../../../main/frontend/legal/files/licenses/componentLicenseFilesDetailsActions';

describe('ComponentLicenseFileDetailsAction', function () {
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
        licenseIndex: '0',
      },
    },
    componentLicenseFileDetails: {
      selectedLicense: 'selectedLicense',
      loadingLicenseDetails: 'loadingLicenseDetails',
    },
  };

  describe('load license details', function () {
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
