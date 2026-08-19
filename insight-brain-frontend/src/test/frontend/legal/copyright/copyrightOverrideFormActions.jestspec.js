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
  saveCopyrightOverride,
} from '../../../../main/frontend/legal/copyright/copyrightOverrideFormActions';
import {
  ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED,
  ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED,
  ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE,
} from '../../../../main/frontend/legal/obligation/advancedLegalObligationActions.js';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  getComponentCopyrightOverrideUrl,
  getSaveComponentCopyrightOverrideUrl,
  getComponentObligationUrl,
  getSaveComponentObligationUrl,
  getCopyrightFilePathsUrl,
  getCopyrightFileCountUrl,
} from '../../../../main/frontend/util/CLMLocation';
import { pathSet } from '@sonatype/react-shared-components/util/jsUtil';
import {
  COPYRIGHT_DETAILS_FULFILLED,
  COPYRIGHT_DETAILS_REQUEST,
} from '../../../../main/frontend/legal/copyright/componentCopyrightDetailsActions';

import 'TestRoot/SpecUtil';

describe('copyrightOverrideFormAction', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  describe('save copyright override', function () {
    let store;
    let initialState = {
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            hash: 'componentHash',
            licenseLegalData: {
              componentCopyrightId: 'componentCopyrightId',
              obligations: [
                {
                  id: 'd387da0b87a9428fbc352f437c8294cf',
                  name: 'Inclusion of Copyright',
                  status: 'FLAGGED',
                  comment: 'comment',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  lastUpdatedByUsername: 'admin',
                  lastUpdatedAt: 1618873200000,
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
    };

    let copyrights = [
      {
        id: '1',
        content: 'Copyright 2043',
        originalContentHash: 'originalContentHash1',
        status: 'enabled',
      },
      {
        id: '',
        content: 'Copyright 2',
        originalContentHash: 'originalContentHash2',
        status: 'disabled',
      },
    ];

    it('immediately dispatches a COPYRIGHT_OVERRIDE_SAVE_REQUESTED action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(
        saveCopyrightOverride({
          copyrights: copyrights,
          scopeOwnerId: 'org',
          isCopyrightsDirty: true,
          isObligationDirty: false,
        })
      );

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    });

    it('does not dispatch anything when not dirty', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(
        saveCopyrightOverride({
          copyrights: copyrights,
          scopeOwnerId: 'org',
          isCopyrightsDirty: false,
          isObligationDirty: false,
        })
      );

      const actions = store.getActions();
      expect(actions.length).toBe(0);
    });

    it(
      'dispatches COPYRIGHT_OVERRIDE_SAVE_FULFILLED & COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE when ' +
        'saveCopyrightOverride succeeds',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);
        const expectedPostBody = {
          id: 'componentCopyrightId',
          componentIdentifier: 'componentIdentifier',
          copyrightOverrides: [
            {
              id: '1',
              content: 'Copyright 2043',
              originalContentHash: 'originalContentHash1',
              status: 'enabled',
            },
            {
              id: '',
              content: 'Copyright 2',
              originalContentHash: 'originalContentHash2',
              status: 'disabled',
            },
          ],
        };
        mockAxiosCalls({
          post: {
            [getSaveComponentCopyrightOverrideUrl('organization', 'org')]: Promise.resolve({
              data: {
                data: 'dataPOST',
              },
            }),
            [getSaveComponentObligationUrl('organization', 'org')]: Promise.resolve({
              data: {
                data: 'dataPOST2',
              },
            }),
          },
          get: {
            [getComponentCopyrightOverrideUrl('organization', 'org', 'componentIdentifier')]: Promise.resolve({
              data: {
                componentCopyrightDTO: {
                  data: 'dataGET',
                  lastUpdatedByUsername: 'admin',
                  lastUpdatedAt: 1618873200000,
                },
                ownerId: 'realOwner',
              },
            }),
            [getComponentObligationUrl(
              'organization',
              'org',
              'componentIdentifier',
              'Inclusion of Copyright'
            )]: Promise.resolve({
              data: {
                id: 'id',
                comment: 'comment',
                status: 'OPEN',
                ownerId: 'realOwner',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
            }),
          },
        });
        jest.useFakeTimers();

        store
          .dispatch(
            saveCopyrightOverride({
              copyrights: copyrights,
              scopeOwnerId: 'org',
              existingObligation: {
                name: 'Inclusion of Copyright',
                status: 'FULFILLED',
              },
              isCopyrightsDirty: true,
              isObligationDirty: false,
            })
          )
          .then(() => {
            jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
            jest.useRealTimers();

            const actions = store.getActions();
            expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/copyright',
              expectedPostBody
            );
            expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org' +
                '/component/copyright?componentIdentifier=%22componentIdentifier%22'
            );
            expect(actions.length).toBe(3);
            expect(actions[1].type).toBe(COPYRIGHT_OVERRIDE_SAVE_FULFILLED);
            expect(actions[1].payload).toEqual({
              data: 'dataGET',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
              componentCopyrightScopeOwnerId: 'realOwner',
              componentCopyrightLastUpdatedByUsername: 'admin',
              componentCopyrightLastUpdatedAt: 1618873200000,
            });
            expect(actions[2].type).toBe(COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE);
            done();
          });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
      }
    );

    it('does not save obligation when it is missing', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'componentCopyrightId',
        componentIdentifier: 'componentIdentifier',
        copyrightOverrides: [
          {
            id: '1',
            content: 'Copyright 2043',
            originalContentHash: 'originalContentHash1',
            status: 'enabled',
          },
          {
            id: '',
            content: 'Copyright 2',
            originalContentHash: 'originalContentHash2',
            status: 'disabled',
          },
        ],
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentCopyrightOverrideUrl('organization', 'org')]: Promise.resolve({
            data: {
              data: 'dataPOST',
            },
          }),
        },
        get: {
          [getComponentCopyrightOverrideUrl('organization', 'org', 'componentIdentifier')]: Promise.resolve({
            data: {
              componentCopyrightDTO: {
                data: 'dataGET',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
              ownerId: 'realOwner',
            },
          }),
        },
      });
      jest.useFakeTimers();

      store
        .dispatch(
          saveCopyrightOverride({
            copyrights: copyrights,
            scopeOwnerId: 'org',
            isCopyrightsDirty: true,
            isObligationDirty: false,
          })
        )
        .then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jest.useRealTimers();

          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/org/component/copyright',
            expectedPostBody
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/org' +
              '/component/copyright?componentIdentifier=%22componentIdentifier%22'
          );
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(COPYRIGHT_OVERRIDE_SAVE_FULFILLED);
          expect(actions[1].payload).toEqual({
            data: 'dataGET',
            lastUpdatedByUsername: 'admin',
            lastUpdatedAt: 1618873200000,
            componentCopyrightScopeOwnerId: 'realOwner',
            componentCopyrightLastUpdatedByUsername: 'admin',
            componentCopyrightLastUpdatedAt: 1618873200000,
          });
          expect(actions[2].type).toBe(COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE);
          done();
        });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    });

    it('dispatches an COPYRIGHT_OVERRIDE_FAILED action when the API fails to save', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'componentCopyrightId',
        componentIdentifier: 'componentIdentifier',
        copyrightOverrides: [
          {
            id: '1',
            content: 'Copyright 2043',
            originalContentHash: 'originalContentHash1',
            status: 'enabled',
          },
          {
            id: '',
            content: 'Copyright 2',
            originalContentHash: 'originalContentHash2',
            status: 'disabled',
          },
        ],
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentCopyrightOverrideUrl('organization', 'org')]: () => Promise.reject('error'),
        },
      });

      store
        .dispatch(
          saveCopyrightOverride({
            copyrights: copyrights,
            scopeOwnerId: 'org',
            isCopyrightsDirty: true,
            isObligationDirty: false,
          })
        )
        .then(() => {
          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/org/component/copyright',
            expectedPostBody
          );
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(COPYRIGHT_OVERRIDE_FAILED);
          expect(actions[1].payload).toBe('error');
          done();
        });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    });

    it('dispatches COPYRIGHT_DETAILS_REQUEST and COPYRIGHT_DETAILS_FULFILLED when copyright details view is active', function (done) {
      const testState = {
        ...initialState,
        router: {
          currentParams: { copyrightIndex: 1, ownerType: 'organization', ownerId: 'org' },
        },
      };
      testState.advancedLegal.component.component.licenseLegalData = {
        copyrights: [{ originalContentHash: 'copyright_hash_1' }, { originalContentHash: 'copyright_hash_2' }],
      };
      store = SpecUtil.mockReduxStore(testState);
      mockAxiosCalls({
        post: {
          [getSaveComponentCopyrightOverrideUrl('organization', 'org')]: Promise.resolve({
            data: {
              data: 'dataPOST',
            },
          }),
          [getSaveComponentObligationUrl('organization', 'org')]: Promise.resolve({
            data: {
              data: 'dataPOST2',
            },
          }),
        },
        get: {
          [getComponentCopyrightOverrideUrl('organization', 'org', 'componentIdentifier')]: Promise.resolve({
            data: {
              componentCopyrightDTO: {
                data: 'dataGET',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
              ownerId: 'realOwner',
            },
          }),
          [getComponentObligationUrl(
            'organization',
            'org',
            'componentIdentifier',
            'Inclusion of Copyright'
          )]: Promise.resolve({
            data: {
              id: 'id',
              comment: 'comment',
              status: 'OPEN',
              ownerId: 'realOwner',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          }),
          [getCopyrightFileCountUrl('organization', 'org', 'componentHash', 'componentIdentifier')]: Promise.resolve({
            data: {
              path1: 5,
              path2: 10,
            },
          }),
          [getCopyrightFilePathsUrl(
            'organization',
            'org',
            'componentHash',
            'componentIdentifier',
            'copyright_hash_2',
            0,
            15
          )]: Promise.resolve({
            data: ['path1/file1', 'path2/file2', 'path3/file3'],
          }),
        },
      });
      store
        .dispatch(
          saveCopyrightOverride({
            copyrights: copyrights,
            scopeOwnerId: 'org',
            existingObligation: {
              name: 'Inclusion of Copyright',
              status: 'FULFILLED',
            },
            isCopyrightsDirty: true,
            isObligationDirty: false,
          })
        )
        .then(() => {
          setTimeout(() => {
            const actions = store.getActions();
            expect(actions.length).toBe(5);
            expect(actions[1].type).toBe(COPYRIGHT_OVERRIDE_SAVE_FULFILLED);
            expect(actions[1].payload).toEqual({
              data: 'dataGET',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
              componentCopyrightScopeOwnerId: 'realOwner',
              componentCopyrightLastUpdatedByUsername: 'admin',
              componentCopyrightLastUpdatedAt: 1618873200000,
            });
            expect(actions[2].type).toBe(COPYRIGHT_DETAILS_REQUEST);
            expect(actions[2].payload).toEqual({
              copyrightIndex: 1,
              copyright: {
                originalContentHash: 'copyright_hash_2',
              },
              loadingCopyrightFileCounts: true,
              loadingFilePaths: true,
            });
            expect(actions[3].type).toBe(COPYRIGHT_DETAILS_FULFILLED);
            expect(actions[3].payload).toEqual({
              copyrightIndex: 1,
              copyright: {
                originalContentHash: 'copyright_hash_2',
              },
              filePaths: ['path1/file1', 'path2/file2', 'path3/file3'],
              copyrightFileCounts: {
                path1: 5,
                path2: 10,
              },
            });
            expect(actions[4].type).toBe(COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE);
            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    });
  });

  describe('save copyright override at different scope', function () {
    let store;
    let initialState = {
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            licenseLegalData: {
              componentCopyrightId: 'componentCopyrightId',
              componentCopyrightScopeOwnerId: 'app',
              obligations: [
                {
                  id: 'd387da0b87a9428fbc352f437c8294cf',
                  name: 'Inclusion of Copyright',
                  status: 'FLAGGED',
                  comment: 'comment',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  lastUpdatedByUsername: 'admin',
                  lastUpdatedAt: 1618873200000,
                },
              ],
            },
          },
        },
        availableScopes: {
          values: [
            { id: 'app', publicId: 'app', type: 'application' },
            { id: 'org', publicId: 'org', type: 'organization' },
            {
              id: 'ROOT_ORGANIZATION_ID',
              publicId: 'ROOT_ORGANIZATION_ID',
              type: 'organization',
            },
          ],
        },
      },
    };

    let copyrights = [
      {
        id: '1',
        content: 'Copyright 2043',
        originalContentHash: 'originalContentHash1',
        status: 'enabled',
      },
      {
        id: '2',
        content: 'Copyright 2',
        originalContentHash: 'originalContentHash2',
        status: 'disabled',
      },
    ];

    it('ComponentCopyright exists at appScope, change to OrgScope', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'componentCopyrightId',
        componentIdentifier: 'componentIdentifier',
        copyrightOverrides: [
          {
            id: '1',
            content: 'Copyright 2043',
            originalContentHash: 'originalContentHash1',
            status: 'enabled',
          },
          {
            id: '2',
            content: 'Copyright 2',
            originalContentHash: 'originalContentHash2',
            status: 'disabled',
          },
        ],
      };
      assertExpectedHighScopeCalls('org', 'organization', expectedPostBody, done);
    });

    it('ComponentCopyright exists at appScope, change to root scope', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'componentCopyrightId',
        componentIdentifier: 'componentIdentifier',
        copyrightOverrides: [
          {
            id: '1',
            content: 'Copyright 2043',
            originalContentHash: 'originalContentHash1',
            status: 'enabled',
          },
          {
            id: '2',
            content: 'Copyright 2',
            originalContentHash: 'originalContentHash2',
            status: 'disabled',
          },
        ],
      };

      assertExpectedHighScopeCalls('ROOT_ORGANIZATION_ID', 'organization', expectedPostBody, done);
    });

    it('ComponentCopyright exists at orgScope, change to root scope', function (done) {
      initialState = pathSet(
        ['advancedLegal', 'component', 'component', 'licenseLegalData', 'componentCopyrightScopeOwnerId'],
        'org',
        initialState
      );
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'componentCopyrightId',
        componentIdentifier: 'componentIdentifier',
        copyrightOverrides: [
          {
            id: '1',
            content: 'Copyright 2043',
            originalContentHash: 'originalContentHash1',
            status: 'enabled',
          },
          {
            id: '2',
            content: 'Copyright 2',
            originalContentHash: 'originalContentHash2',
            status: 'disabled',
          },
        ],
      };

      assertExpectedHighScopeCalls('ROOT_ORGANIZATION_ID', 'organization', expectedPostBody, done);
    });

    it('ComponentCopyright exists at orgScope, change to app scope', function (done) {
      initialState = pathSet(
        ['advancedLegal', 'component', 'component', 'licenseLegalData', 'componentCopyrightScopeOwnerId'],
        'org',
        initialState
      );
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: null,
        componentIdentifier: 'componentIdentifier',
        copyrightOverrides: [
          {
            id: null,
            content: 'Copyright 2043',
            originalContentHash: 'originalContentHash1',
            status: 'enabled',
          },
          {
            id: null,
            content: 'Copyright 2',
            originalContentHash: 'originalContentHash2',
            status: 'disabled',
          },
        ],
      };

      assertExpectedCalls('app', 'application', expectedPostBody, done);
    });

    function assertExpectedCalls(expectedScope, orgOrApp, expectedPostBody, done) {
      mockAxiosCalls({
        post: {
          [getSaveComponentCopyrightOverrideUrl(orgOrApp, expectedScope)]: Promise.resolve({
            data: {
              data: 'data',
            },
          }),
          [getSaveComponentObligationUrl(orgOrApp, expectedScope)]: Promise.resolve({
            data: {
              data: 'dataPOST2',
            },
          }),
        },
        get: {
          [getComponentCopyrightOverrideUrl('application', 'app', 'componentIdentifier')]: Promise.resolve({
            data: {
              componentCopyrightDTO: {
                data: 'dataGET',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
              ownerId: 'realOwner',
            },
          }),
          [getComponentObligationUrl(
            orgOrApp,
            expectedScope,
            'componentIdentifier',
            'Inclusion of Copyright'
          )]: Promise.resolve({
            data: {
              id: 'id',
              comment: 'comment',
              status: 'OPEN',
              ownerId: 'realOwner',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          }),
        },
      });
      jest.useFakeTimers();

      store
        .dispatch(
          saveCopyrightOverride({
            copyrights,
            scopeOwnerId: expectedScope,
            existingObligation: {
              name: 'Inclusion of Copyright',
              status: 'FULFILLED',
            },
            isCopyrightsDirty: true,
            isObligationDirty: false,
          })
        )
        .then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          jest.useRealTimers();

          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/' + orgOrApp + '/' + expectedScope + '/component/copyright',
            expectedPostBody
          );
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(COPYRIGHT_OVERRIDE_SAVE_FULFILLED);
          expect(actions[1].payload).toEqual({
            data: 'dataGET',
            lastUpdatedByUsername: 'admin',
            lastUpdatedAt: 1618873200000,
            componentCopyrightScopeOwnerId: 'realOwner',
            componentCopyrightLastUpdatedByUsername: 'admin',
            componentCopyrightLastUpdatedAt: 1618873200000,
          });
          expect(actions[2].type).toBe(COPYRIGHT_OVERRIDE_SUBMIT_MASK_DONE);

          done();
        });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    }

    function assertExpectedHighScopeCalls(persistedAtScope, orgOrApp, expectedPostBody, done) {
      mockAxiosCalls({
        post: {
          [getSaveComponentCopyrightOverrideUrl(orgOrApp, persistedAtScope)]: Promise.resolve({
            data: {
              data: 'dataPOST',
            },
          }),
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({
            data: {
              data: 'dataPOST2',
            },
          }),
        },
        get: {
          [getComponentCopyrightOverrideUrl('application', 'app', 'componentIdentifier')]: Promise.resolve({
            data: {
              componentCopyrightDTO: {
                data: 'dataGET',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
              ownerId: 'realOwner',
            },
          }),
          [getComponentObligationUrl(
            'application',
            'app',
            'componentIdentifier',
            'Inclusion of Copyright'
          )]: Promise.resolve({
            data: {
              id: 'id',
              comment: 'comment',
              status: 'OPEN',
              ownerId: 'realOwner',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          }),
        },
      });
      store
        .dispatch(
          saveCopyrightOverride({
            copyrights,
            scopeOwnerId: persistedAtScope,
            existingObligation: {
              name: 'Inclusion of Copyright',
              status: 'FULFILLED',
            },
            isCopyrightsDirty: true,
            isObligationDirty: true,
          })
        )
        .then(() => {
          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/' + orgOrApp + '/' + persistedAtScope + '/component/copyright',
            expectedPostBody
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/application/app' +
              '/component/copyright?componentIdentifier=%22componentIdentifier%22'
          );
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(COPYRIGHT_OVERRIDE_SAVE_FULFILLED);
          expect(actions[1].payload).toEqual({
            data: 'dataGET',
            lastUpdatedByUsername: 'admin',
            lastUpdatedAt: 1618873200000,
            componentCopyrightScopeOwnerId: 'realOwner',
            componentCopyrightLastUpdatedByUsername: 'admin',
            componentCopyrightLastUpdatedAt: 1618873200000,
          });
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
          done();
        });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    }
  });

  describe('save copyright override and obligation', function () {
    let store;
    const initialState = {
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            licenseLegalData: {
              componentCopyrightId: 'componentCopyrightId',
              componentCopyrightScopeOwnerId: 'ROOT_ORGANIZATION_ID',
              obligations: [
                {
                  id: 'd387da0b87a9428fbc352f437c8294cf',
                  name: 'Inclusion of Copyright',
                  status: 'FLAGGED',
                  comment: 'comment',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  lastUpdatedByUsername: 'admin',
                  lastUpdatedAt: 1618873200000,
                },
              ],
            },
          },
        },
        availableScopes: {
          values: [
            {
              id: 'ROOT_ORGANIZATION_ID',
              publicId: 'ROOT_ORGANIZATION_ID',
              type: 'organization',
            },
          ],
        },
      },
    };
    const copyrights = [
      {
        id: '1',
        content: 'Copyright 2043',
        originalContentHash: 'originalContentHash1',
        status: 'enabled',
      },
      {
        id: '2',
        content: 'Copyright 2',
        originalContentHash: 'originalContentHash2',
        status: 'disabled',
      },
    ];

    it('dispatches the expected actions on success', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'componentCopyrightId',
        componentIdentifier: 'componentIdentifier',
        copyrightOverrides: [
          {
            id: '1',
            content: 'Copyright 2043',
            originalContentHash: 'originalContentHash1',
            status: 'enabled',
          },
          {
            id: '2',
            content: 'Copyright 2',
            originalContentHash: 'originalContentHash2',
            status: 'disabled',
          },
        ],
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentCopyrightOverrideUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({
            data: {
              data: 'dataPOST1',
            },
          }),
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({
            data: {
              data: 'dataPOST2',
            },
          }),
        },
        get: {
          [getComponentCopyrightOverrideUrl(
            'organization',
            'ROOT_ORGANIZATION_ID',
            'componentIdentifier'
          )]: Promise.resolve({
            data: {
              componentCopyrightDTO: {
                data: 'dataGET1',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
              ownerId: 'ROOT_ORGANIZATION_ID',
            },
          }),
          [getComponentObligationUrl(
            'organization',
            'ROOT_ORGANIZATION_ID',
            'componentIdentifier',
            'Inclusion of Copyright'
          )]: Promise.resolve({
            data: {
              id: 'd387da0b87a9428fbc352f437c8294cf',
              comment: 'comment',
              ownerId: 'ROOT_ORGANIZATION_ID',
              status: 'FLAGGED',
              name: 'Inclusion of Copyright',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          }),
        },
      });
      store
        .dispatch(
          saveCopyrightOverride({
            copyrights,
            scopeOwnerId: 'ROOT_ORGANIZATION_ID',
            existingObligation: {
              name: 'Inclusion of Copyright',
              status: 'FLAGGED',
            },
            isCopyrightsDirty: true,
            isObligationDirty: true,
          })
        )
        .then(() => {
          setTimeout(() => {
            const actions = store.getActions();
            expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/copyright',
              expectedPostBody
            );
            expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID' +
                '/component/copyright?componentIdentifier=%22componentIdentifier%22'
            );
            expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation',
              {
                id: 'd387da0b87a9428fbc352f437c8294cf',
                componentIdentifier: 'componentIdentifier',
                name: 'Inclusion of Copyright',
                comment: 'comment',
                status: 'FLAGGED',
              }
            );
            expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID' +
                '/component/obligation?componentIdentifier=%22componentIdentifier%22' +
                '&obligationName=Inclusion%20of%20Copyright'
            );
            expect(actions.length).toBe(5);
            expect(actions[1].type).toBe(COPYRIGHT_OVERRIDE_SAVE_FULFILLED);
            expect(actions[1].payload).toEqual({
              data: 'dataGET1',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
              componentCopyrightScopeOwnerId: 'ROOT_ORGANIZATION_ID',
              componentCopyrightLastUpdatedByUsername: 'admin',
              componentCopyrightLastUpdatedAt: 1618873200000,
            });
            expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
            expect(actions[3].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
            expect(actions[3].payload).toEqual({
              name: 'Inclusion of Copyright',
              value: {
                id: 'd387da0b87a9428fbc352f437c8294cf',
                comment: 'comment',
                ownerId: 'ROOT_ORGANIZATION_ID',
                status: 'FLAGGED',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
            });
            expect(actions[4].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE);
            expect(actions[4].payload).toEqual({
              name: 'Inclusion of Copyright',
            });
            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS * 2);
        });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_OVERRIDE_SAVE_REQUESTED);
    });
  });
});
