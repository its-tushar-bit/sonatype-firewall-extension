/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import {
  COPYRIGHT_OVERRIDE_FAILED,
  COPYRIGHT_OVERRIDE_SAVE_FULFILLED,
  COPYRIGHT_OVERRIDE_SAVE_REQUESTED,
  COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE,
  saveCopyrightOverride
} from '../../../../main/frontend/legal/copyright/copyrightOverrideFormActions';
import {SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS} from '@sonatype/react-shared-components';
import {
  getComponentCopyrightOverrideUrl,
  getSaveComponentCopyrightOverrideUrl
} from '../../../../main/frontend/util/CLMLocation';
import {pathSet} from '@sonatype/react-shared-components/util/jsUtil';

describe('copyrightOverrideFormAction', function() {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  describe('save copyright override', function() {
    let store;
    let initialState = {
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            licenseLegalData: {
              componentCopyrightId: 'componentCopyrightId'
            }
          }
        },
        availableScopes: {
          values: [
            {id: 'org', publicId: 'org', type: 'organization'},
            {id: 'ROOT_ORGANIZATION_ID', publicId: 'ROOT_ORGANIZATION_ID', type: 'organization'}
          ]
        }
      }
    };

    let copyrights = [
      {
        id: '1',
        content: 'Copyright 2043',
        originalContentHash: 'originalContentHash1',
        status: 'enabled'
      },
      {
        id: '',
        content: 'Copyright 2',
        originalContentHash: 'originalContentHash2',
        status: 'disabled'
      }
    ];

    it('immediately dispatches a COPYRIGHT_OVERRIDE_SAVE_REQUESTED action', function() {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveCopyrightOverride({
        copyrights: copyrights,
        scopeOwnerId: 'org'
      }));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    });

    it('immediately dispatches a COPYRIGHT_OVERRIDE_SAVE_REQUESTED action', function() {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveCopyrightOverride({
        copyrights: copyrights,
        scopeOwnerId: 'org'
      }));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    });

    it('dispatches COPYRIGHT_OVERRIDE_SAVE_FULFILLED & COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE when ' +
        'saveCopyrightOverride succeeds', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        'id': 'componentCopyrightId',
        'componentIdentifier': 'componentIdentifier',
        'copyrightOverrides': [
          {
            'id': '1',
            'content': 'Copyright 2043',
            'originalContentHash': 'originalContentHash1',
            'status': 'enabled'
          },
          {
            'id': '',
            'content': 'Copyright 2',
            'originalContentHash': 'originalContentHash2',
            'status': 'disabled'
          }
        ]
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentCopyrightOverrideUrl('organization', 'org')]: Promise.resolve(
              {
                data: {
                  data: 'data'
                }
              })
        }
      });
      store.dispatch(saveCopyrightOverride(
          {
            copyrights: copyrights,
            scopeOwnerId: 'org'
          })).then(() => {
        setTimeout(() => {
          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/copyright', expectedPostBody);
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(COPYRIGHT_OVERRIDE_SAVE_FULFILLED);
          expect(actions[1].payload).toEqual({data: 'data', componentCopyrightScopeOwnerId: 'org'});
          expect(actions[2].type).toBe(COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE);
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    });

    it('dispatches an COPYRIGHT_OVERRIDE_FAILED action when the API fails to save', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        'id': 'componentCopyrightId',
        'componentIdentifier': 'componentIdentifier',
        'copyrightOverrides': [
          {
            'id': '1',
            'content': 'Copyright 2043',
            'originalContentHash': 'originalContentHash1',
            'status': 'enabled'
          },
          {
            'id': '',
            'content': 'Copyright 2',
            'originalContentHash': 'originalContentHash2',
            'status': 'disabled'
          }
        ]
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentCopyrightOverrideUrl('organization', 'org')]: Promise.reject('error')
        }
      });

      store.dispatch(saveCopyrightOverride(
          {copyrights: copyrights, scopeOwnerId: 'org'})).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/org/component/copyright', expectedPostBody);
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(COPYRIGHT_OVERRIDE_FAILED);
        expect(actions[1].payload).toBe('error');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    });
  });

  describe('save copyright override at different scope', function() {
    let store;
    let initialState = {
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            licenseLegalData: {
              componentCopyrightId: 'componentCopyrightId',
              componentCopyrightScopeOwnerId: 'app'
            }
          }
        },
        availableScopes: {
          values: [
            {id: 'app', publicId: 'app', type: 'application'},
            {id: 'org', publicId: 'org', type: 'organization'},
            {id: 'ROOT_ORGANIZATION_ID', publicId: 'ROOT_ORGANIZATION_ID', type: 'organization'}
          ]
        }
      }
    };

    let copyrights = [
      {
        id: '1',
        content: 'Copyright 2043',
        originalContentHash: 'originalContentHash1',
        status: 'enabled'
      },
      {
        id: '2',
        content: 'Copyright 2',
        originalContentHash: 'originalContentHash2',
        status: 'disabled'
      }
    ];

    it('ComponentCopyright exists at appScope, change to OrgScope', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        'id': 'componentCopyrightId',
        'componentIdentifier': 'componentIdentifier',
        'copyrightOverrides': [
          {
            'id': '1',
            'content': 'Copyright 2043',
            'originalContentHash': 'originalContentHash1',
            'status': 'enabled'
          },
          {
            'id': '2',
            'content': 'Copyright 2',
            'originalContentHash': 'originalContentHash2',
            'status': 'disabled'
          }
        ]
      };
      assertExpectedHighScopeCalls('org', 'organization', expectedPostBody, done);
    });

    it('ComponentCopyright exists at appScope, change to root scope', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        'id': 'componentCopyrightId',
        'componentIdentifier': 'componentIdentifier',
        'copyrightOverrides': [
          {
            'id': '1',
            'content': 'Copyright 2043',
            'originalContentHash': 'originalContentHash1',
            'status': 'enabled'
          },
          {
            'id': '2',
            'content': 'Copyright 2',
            'originalContentHash': 'originalContentHash2',
            'status': 'disabled'
          }
        ]
      };

      assertExpectedHighScopeCalls('ROOT_ORGANIZATION_ID', 'organization', expectedPostBody, done);
    });

    it('ComponentCopyright exists at orgScope, change to root scope', function(done) {
      initialState = pathSet(
          ['advancedLegal', 'component', 'component', 'licenseLegalData', 'componentCopyrightScopeOwnerId'], 'org',
          initialState);
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        'id': 'componentCopyrightId',
        'componentIdentifier': 'componentIdentifier',
        'copyrightOverrides': [
          {
            'id': '1',
            'content': 'Copyright 2043',
            'originalContentHash': 'originalContentHash1',
            'status': 'enabled'
          },
          {
            'id': '2',
            'content': 'Copyright 2',
            'originalContentHash': 'originalContentHash2',
            'status': 'disabled'
          }
        ]
      };

      assertExpectedHighScopeCalls('ROOT_ORGANIZATION_ID', 'organization', expectedPostBody, done);
    });

    it('ComponentCopyright exists at orgScope, change to app scope', function(done) {
      initialState = pathSet(
          ['advancedLegal', 'component', 'component', 'licenseLegalData', 'componentCopyrightScopeOwnerId'], 'org',
          initialState);
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        'id': null,
        'componentIdentifier': 'componentIdentifier',
        'copyrightOverrides': [
          {
            'id': '1',
            'content': 'Copyright 2043',
            'originalContentHash': 'originalContentHash1',
            'status': 'enabled'
          },
          {
            'id': '2',
            'content': 'Copyright 2',
            'originalContentHash': 'originalContentHash2',
            'status': 'disabled'
          }
        ]
      };

      assertExpectedCalls('app', 'application', expectedPostBody, done);
    });

    function assertExpectedCalls(expectedScope, orgOrApp, expectedPostBody, done) {
      mockAxiosCalls({
        post: {
          [getSaveComponentCopyrightOverrideUrl(orgOrApp, expectedScope)]: Promise.resolve(
              {
                data: {
                  data: 'data'
                }
              })
        }
      });
      store.dispatch(saveCopyrightOverride(
          {
            copyrights,
            scopeOwnerId: expectedScope
          })).then(() => {
        setTimeout(() => {
          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/' + orgOrApp + '/' + expectedScope + '/component/copyright',
              expectedPostBody);
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(COPYRIGHT_OVERRIDE_SAVE_FULFILLED);
          expect(actions[1].payload).toEqual({data: 'data', componentCopyrightScopeOwnerId: expectedScope});
          expect(actions[2].type).toBe(COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE);
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    }

    function assertExpectedHighScopeCalls(persistedAtScope, orgOrApp, expectedPostBody, done) {
      mockAxiosCalls({
        post: {
          [getSaveComponentCopyrightOverrideUrl(orgOrApp, persistedAtScope)]: Promise.resolve(
              {
                data: {
                  data: 'dataPOST'
                }
              })
        },
        get: {
          [getComponentCopyrightOverrideUrl(orgOrApp, persistedAtScope, 'componentIdentifier')]: Promise.resolve(
              {
                data: {
                  componentCopyrightDTO: {data: 'dataGET'},
                  ownerId: 'realOwner'
                }
              })
        }
      });
      store.dispatch(saveCopyrightOverride(
          {
            copyrights,
            scopeOwnerId: persistedAtScope
          })).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/' + orgOrApp + '/' + persistedAtScope + '/component/copyright',
            expectedPostBody);
        expect(axios.get).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/' + orgOrApp + '/' + persistedAtScope +
            '/component/copyright?componentIdentifier="componentIdentifier"');
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(COPYRIGHT_OVERRIDE_SAVE_FULFILLED);
        expect(actions[1].payload).toEqual({data: 'dataGET', componentCopyrightScopeOwnerId: 'realOwner'});
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    }
  });

});
