/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  loadComponentAndNoticeDetails,
  NOTICE_DETAILS_SELECTED_NOTICE,
  refreshNoticeFilesDetails,
} from '../../../../../main/frontend/legal/files/notices/componentNoticeDetailsActions';
import axios from 'axios';
import {
  getLicenseLegalComponentByComponentIdentifierUrl,
  getLicenseLegalComponentUrl,
  getOwnerHierarchyLegalReviewerUrl,
} from 'MainRoot/util/CLMLocation';
import { pathSet } from 'MainRoot/util/jsUtil';

import 'TestRoot/SpecUtil';

describe('ComponentNoticeDetailsAction', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store;
  let initialState = {
    advancedLegal: {
      component: {
        component: {
          componentIdentifier: 'componentIdentifier',
          hash: 'componentHash',
          licenseLegalData: {
            noticeFiles: [
              {
                relPath: '/test/NOTICE',
                content: 'you must include notice for this fake notice file',
              },
              {
                relPath: '/test/sub/notice.txt',
                content: 'Apache Royale bla bla bla',
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
        noticeIndex: '1',
      },
    },
    componentNoticeDetails: {
      noticeIndex: 1,
    },
  };

  describe('load notice details', function () {
    it('immediately dispatches a NOTICE_DETAILS_SELECTED_NOTICE action. Component already in state by hash', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadComponentAndNoticeDetails('organization', 'org', 'componentHash', 1));
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(NOTICE_DETAILS_SELECTED_NOTICE);
    });

    it('immediately dispatches a NOTICE_DETAILS_SELECTED_NOTICE action. Component already in state by component identifier', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadComponentAndNoticeDetails(undefined, undefined, undefined, 1, 'componentIdentifier'));
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(NOTICE_DETAILS_SELECTED_NOTICE);
    });

    it('fetches notice file details by hash when not loaded', function (done) {
      store = SpecUtil.mockReduxStore(pathSet(['advancedLegal', 'component', 'component'], undefined, initialState));

      const ownerHierarchyUrl = getOwnerHierarchyLegalReviewerUrl('organization', 'org');
      const licenseLegalComponentUrl = getLicenseLegalComponentUrl('organization', 'org', 'componentHash');

      mockAxiosCalls({
        get: {
          [ownerHierarchyUrl]: Promise.resolve({ data: 'getData' }),
          [licenseLegalComponentUrl]: Promise.resolve({ data: 'getData2' }),
        },
      });

      store.dispatch(loadComponentAndNoticeDetails('organization', 'org', 'componentHash', 1)).then(() => {
        expect(axios.get).toHaveBeenCalledWith(ownerHierarchyUrl);
        expect(axios.get).toHaveBeenCalledWith(licenseLegalComponentUrl);
        done();
      });
    });

    it('fetches license file details by component identifier when not loaded', function (done) {
      let state = pathSet(['advancedLegal', 'component', 'component'], undefined, initialState);
      state = pathSet(['router', 'currentParams', 'hash'], undefined, state);
      store = SpecUtil.mockReduxStore(state);

      const licenseLegalComponentByComponentIdentifierUrl = getLicenseLegalComponentByComponentIdentifierUrl(
        'componentIdentifier',
        'organization',
        'org'
      );

      mockAxiosCalls({
        get: {
          [licenseLegalComponentByComponentIdentifierUrl]: Promise.resolve({
            data: 'getData2',
          }),
        },
      });

      store
        .dispatch(loadComponentAndNoticeDetails('organization', 'org', undefined, 1, 'componentIdentifier'))
        .then(() => {
          expect(axios.get).toHaveBeenCalledWith(licenseLegalComponentByComponentIdentifierUrl);
          done();
        });
    });
  });

  describe('refresh notice file details', function () {
    it('immediately dispatches a NOTICE_DETAILS_SELECTED_NOTICE action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(refreshNoticeFilesDetails());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(NOTICE_DETAILS_SELECTED_NOTICE);
    });
  });
});
