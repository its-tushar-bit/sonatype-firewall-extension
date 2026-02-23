/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { pathSet } from 'MainRoot/util/jsUtil';
import {
  COPYRIGHT_CONTEXT_FAILED,
  COPYRIGHT_CONTEXT_FULFILLED,
  COPYRIGHT_CONTEXT_REQUEST,
  COPYRIGHT_DETAILS_FAILED,
  COPYRIGHT_DETAILS_FULFILLED,
  COPYRIGHT_DETAILS_REQUEST,
  COPYRIGHT_FILE_PATHS_FAILED,
  COPYRIGHT_FILE_PATHS_FULFILLED,
  COPYRIGHT_FILE_PATHS_REQUEST,
  loadComponentAndCopyrightDetails,
  loadCopyrightContexts,
  loadFilePathsOnPageUpdate,
  unloadCopyrightContext,
} from '../../../../main/frontend/legal/copyright/componentCopyrightDetailsActions';
import {
  getCopyrightContextUrl,
  getCopyrightFileCountUrl,
  getCopyrightFilePathsUrl,
  getLicenseLegalComponentByComponentIdentifierUrl,
  getLicenseLegalComponentUrl,
  getOwnerHierarchyLegalReviewerUrl,
} from '../../../../main/frontend/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('ComponentCopyrightDetailsAction', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store;
  let initialState = {
    advancedLegal: {
      component: {
        component: {
          componentIdentifier: 'componentIdentifier',
          hash: 'componentHash',
          licenseLegalData: {
            copyrights: [{ originalContentHash: 'copyright_hash_1' }, { originalContentHash: 'copyright_hash_2' }],
            componentCopyrightId: '9a240391fc4a4082a00468e3c5008476',
            componentCopyrightLastUpdatedAt: 1617116970393,
            componentCopyrightLastUpdatedByUsername: 'admin',
            componentCopyrightScopeOwnerId: '5466924469144007b748c25d6f269096',
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
        copyrightIndex: '0',
      },
    },
    componentCopyrightDetails: {
      copyrightIndex: 1,
      selectedFilePaths: [],
      copyrightContexts: [],
      filePaths: [
        {
          copyrightMatches: 1,
          filePath: 'path1/file1',
        },
      ],
    },
  };

  describe('load copyright details', function () {
    it('fetches component details by hash2 when not loaded', function (done) {
      store = SpecUtil.mockReduxStore(pathSet(['advancedLegal', 'component', 'component'], undefined, initialState));

      const ownerHierarchyUrl = getOwnerHierarchyLegalReviewerUrl('organization', 'org');
      const licenseLegalComponent = getLicenseLegalComponentUrl('organization', 'org', 'componentHash');

      mockAxiosCalls({
        get: {
          [ownerHierarchyUrl]: Promise.resolve({ data: 'getData' }),
          [licenseLegalComponent]: Promise.resolve({ data: 'getData2' }),
        },
      });

      store.dispatch(loadComponentAndCopyrightDetails('organization', 'org', 'componentHash', 1)).then(() => {
        expect(axios.get).toHaveBeenCalledWith(ownerHierarchyUrl);
        expect(axios.get).toHaveBeenCalledWith(licenseLegalComponent);
        done();
      });
    });

    it('fetches component details by component identifier when not loaded', function (done) {
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
          [licenseLegalCompByCompIdentifier]: Promise.resolve({
            data: 'getData2',
          }),
        },
      });

      store
        .dispatch(loadComponentAndCopyrightDetails('organization', 'org', undefined, 1, 'componentIdentifier'))
        .then(() => {
          expect(axios.get).toHaveBeenCalledWith(ownerHierarchyUrl);
          expect(axios.get).toHaveBeenCalledWith(licenseLegalCompByCompIdentifier);
          done();
        });
    });

    it('immediately dispatches a COPYRIGHT_DETAILS_REQUEST action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadComponentAndCopyrightDetails('organization', 'org', 'componentHash', 1));
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_DETAILS_REQUEST);
    });

    it(
      'immediately dispatches a COPYRIGHT_DETAILS_REQUEST & COPYRIGHT_DETAILS_FULFILLED actions ' +
        'when copyright details load',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);

        const copyrightFilePathsUrl = getCopyrightFilePathsUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier',
          'copyright_hash_2',
          0,
          15
        );
        const copyrightFileCountUrl = getCopyrightFileCountUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier'
        );
        const copyrightContextUrl = getCopyrightContextUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier',
          'copyright_hash_2',
          'path1/file1'
        );

        mockAxiosCalls({
          get: {
            [copyrightFileCountUrl]: Promise.resolve({
              data: {
                path1: 5,
                path2: 10,
              },
            }),
            [copyrightFilePathsUrl]: Promise.resolve({
              data: {
                filePaths: [
                  {
                    copyrightMatches: 1,
                    filePath: 'path1/file1',
                  },
                ],
                totalFileMatches: 1,
              },
            }),
            [copyrightContextUrl]: Promise.resolve({
              data: ['context 1', 'context 2'],
            }),
          },
        });

        store.dispatch(loadComponentAndCopyrightDetails('organization', 'org', 'componentHash', 1)).then(() => {
          expect(axios.get).toHaveBeenCalledWith(copyrightFileCountUrl);
          expect(axios.get).toHaveBeenCalledWith(copyrightFilePathsUrl);
          const actions = store.getActions();
          expect(actions[1].type).toBe(COPYRIGHT_DETAILS_FULFILLED);
          expect(actions[1].payload).toEqual({
            copyrightIndex: 1,
            copyright: { originalContentHash: 'copyright_hash_2' },
            filePaths: {
              filePaths: [
                {
                  copyrightMatches: 1,
                  filePath: 'path1/file1',
                },
              ],
              totalFileMatches: 1,
            },
            copyrightFileCounts: { path1: 5, path2: 10 },
          });
          expect(axios.get).toHaveBeenCalledWith(copyrightContextUrl);
          expect(actions[2].type).toBe(COPYRIGHT_CONTEXT_REQUEST);
          expect(actions[3].type).toBe(COPYRIGHT_CONTEXT_FULFILLED);
          expect(actions[3].payload).toEqual({
            filePath: 'path1/file1',
            copyrightContexts: ['context 1', 'context 2'],
          });
          done();
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(COPYRIGHT_DETAILS_REQUEST);
      }
    );

    function verifyCopyrightDetailsFailure(copyrightFileCountUrl, copyrightFilePathsUrl, done) {
      store.dispatch(loadComponentAndCopyrightDetails('organization', 'org', 'componentHash', 1)).then(() => {
        expect(axios.get).toHaveBeenCalledWith(copyrightFileCountUrl);
        expect(axios.get).toHaveBeenCalledWith(copyrightFilePathsUrl);

        const actions = store.getActions();
        expect(actions[1].type).toBe(COPYRIGHT_DETAILS_FAILED);
        expect(actions[1].payload).toEqual({ value: 'Error' });
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_DETAILS_REQUEST);
    }

    it(
      'immediately dispatches a COPYRIGHT_DETAILS_REQUEST action and then COPYRIGHT_DETAILS_FAILURE action ' +
        'when copyright file count fail to load',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);

        const copyrightFilePathsUrl = getCopyrightFilePathsUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier',
          'copyright_hash_2',
          0,
          15
        );
        const copyrightFileCountUrl = getCopyrightFileCountUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier'
        );
        const copyrightContextUrl = getCopyrightContextUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier',
          'copyright_hash_2',
          'path1/file1'
        );

        mockAxiosCalls({
          get: {
            [copyrightFileCountUrl]: () => Promise.reject({ value: 'Error' }),
            [copyrightFilePathsUrl]: Promise.resolve({
              data: {
                filePaths: [
                  {
                    copyrightMatches: 1,
                    filePath: 'path1/file1',
                  },
                ],
                totalFileMatches: 1,
              },
            }),
            [copyrightContextUrl]: Promise.resolve({
              data: ['context 1', 'context 2'],
            }),
          },
        });
        verifyCopyrightDetailsFailure(copyrightFileCountUrl, copyrightFilePathsUrl, done);
      }
    );

    it(
      'immediately dispatches a COPYRIGHT_DETAILS_REQUEST action and then COPYRIGHT_DETAILS_FAILURE action ' +
        'when copyright paths fail to load',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);

        const copyrightFilePathsUrl = getCopyrightFilePathsUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier',
          'copyright_hash_2',
          0,
          15
        );
        const copyrightFileCountUrl = getCopyrightFileCountUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier'
        );
        const copyrightContextUrl = getCopyrightContextUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier',
          'copyright_hash_2',
          'path1/file1'
        );

        mockAxiosCalls({
          get: {
            [copyrightFileCountUrl]: Promise.resolve({
              data: {
                path1: 5,
                path2: 10,
              },
            }),
            [copyrightFilePathsUrl]: () => Promise.reject({ value: 'Error' }),
            [copyrightContextUrl]: Promise.resolve({
              data: ['context 1', 'context 2'],
            }),
          },
        });

        verifyCopyrightDetailsFailure(copyrightFileCountUrl, copyrightFilePathsUrl, done);
      }
    );
  });

  describe('load copyright contexts', function () {
    it('immediately dispatches a COPYRIGHT_CONTEXT_REQUEST action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadCopyrightContexts('path/file.js'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_CONTEXT_REQUEST);
      expect(actions[0].payload).toEqual({ selectedFilePaths: ['path/file.js'] });
    });
    it(
      'immediately dispatches a COPYRIGHT_CONTEXT_REQUEST action and copyrightContext does not exist then COPYRIGHT_CONTEXT_FULFILLED action ' +
        'when copyright context loads',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);

        const copyrightContextUrl = getCopyrightContextUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier',
          'copyright_hash_2',
          'path/file.js'
        );
        mockAxiosCalls({
          get: {
            [copyrightContextUrl]: Promise.resolve({
              data: [
                {
                  contexts: ['context1', 'context2'],
                  filePath: 'path/file.js',
                },
              ],
            }),
          },
        });

        store.dispatch(loadCopyrightContexts('path/file.js')).then(() => {
          expect(axios.get).toHaveBeenCalledWith(copyrightContextUrl);
          const actions = store.getActions();
          expect(actions[1].type).toBe(COPYRIGHT_CONTEXT_FULFILLED);
          expect(actions[1].payload).toEqual({
            copyrightContexts: [
              {
                contexts: ['context1', 'context2'],
                filePath: 'path/file.js',
              },
            ],
            filePath: 'path/file.js',
          });
          done();
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(COPYRIGHT_CONTEXT_REQUEST);
      }
    );

    it(
      'immediately dispatches a COPYRIGHT_DETAILS_REQUEST action and then COPYRIGHT_CONTEXT_FAILED action ' +
        'when copyright contexts fail to load',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);

        const copyrightFilePathsUrl = getCopyrightFilePathsUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier',
          'copyright_hash_2',
          0,
          15
        );
        const copyrightFileCountUrl = getCopyrightFileCountUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier'
        );
        const copyrightContextUrl = getCopyrightContextUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier',
          'copyright_hash_2',
          'path1/file1'
        );

        mockAxiosCalls({
          get: {
            [copyrightFileCountUrl]: Promise.resolve({
              data: {
                path1: 5,
                path2: 10,
              },
            }),
            [copyrightFilePathsUrl]: Promise.resolve({
              data: {
                filePaths: [
                  {
                    copyrightMatches: 1,
                    filePath: 'path1/file1',
                  },
                  {
                    copyrightMatches: 1,
                    filePath: 'path1/file2',
                  },
                  {
                    copyrightMatches: 1,
                    filePath: 'path1/file3',
                  },
                ],
                totalFileMatches: 1,
              },
            }),
            [copyrightContextUrl]: () => Promise.reject({ value: 'Error' }),
          },
        });

        store.dispatch(loadComponentAndCopyrightDetails('organization', 'org', 'componentHash', 1)).then(() => {
          expect(axios.get).toHaveBeenCalledWith(copyrightFileCountUrl);
          expect(axios.get).toHaveBeenCalledWith(copyrightFilePathsUrl);
          const actions = store.getActions();
          expect(actions[1].type).toBe(COPYRIGHT_DETAILS_FULFILLED);
          expect(actions[1].payload).toEqual({
            copyrightIndex: 1,
            copyright: { originalContentHash: 'copyright_hash_2' },
            filePaths: {
              filePaths: [
                {
                  copyrightMatches: 1,
                  filePath: 'path1/file1',
                },
                {
                  copyrightMatches: 1,
                  filePath: 'path1/file2',
                },
                {
                  copyrightMatches: 1,
                  filePath: 'path1/file3',
                },
              ],
              totalFileMatches: 1,
            },
            copyrightFileCounts: { path1: 5, path2: 10 },
          });
          expect(axios.get).toHaveBeenCalledWith(copyrightContextUrl);
          expect(actions[3].type).toBe(COPYRIGHT_CONTEXT_FAILED);
          expect(actions[3].payload).toEqual({ value: 'Error' });
          done();
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(COPYRIGHT_DETAILS_REQUEST);
      }
    );
    it('immediately dispatches a COPYRIGHT_CONTEXT_REQUEST action when copyright context unloads', function () {
      store = SpecUtil.mockReduxStore(initialState);

      store.dispatch(unloadCopyrightContext());
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_CONTEXT_REQUEST);
      expect(actions[0].payload).toEqual({ selectedFilePaths: [] });
    });

    it(
      'immediately dispatches a COPYRIGHT_CONTEXT_REQUEST action and copyrightContext does not exist then COPYRIGHT_CONTEXT_FAILED action ' +
        'when loading copyright context fails',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);

        const copyrightContextUrl = getCopyrightContextUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier',
          'copyright_hash_2',
          'path/file.js'
        );
        mockAxiosCalls({
          get: {
            [copyrightContextUrl]: () =>
              Promise.reject({
                value: 'Error',
              }),
          },
        });

        store.dispatch(loadCopyrightContexts('path/file.js')).then(() => {
          expect(axios.get).toHaveBeenCalledWith(copyrightContextUrl);
          const actions = store.getActions();
          expect(actions[1].type).toBe(COPYRIGHT_CONTEXT_FAILED);
          expect(actions[1].payload).toEqual({ value: 'Error' });
          done();
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(COPYRIGHT_CONTEXT_REQUEST);
      }
    );

    it('COPYRIGHT_CONTEXT_REQUEST action is not dispatched when the copyrightContext does exist', function () {
      const customInitialState = { ...initialState };
      customInitialState.componentCopyrightDetails.copyrightContexts = [
        { filePath: 'path/file.js', contexts: ['context1'] },
      ];
      store = SpecUtil.mockReduxStore(customInitialState);
      store.dispatch(loadCopyrightContexts('path/file.js'));
      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_CONTEXT_REQUEST);
      expect(actions[0].payload).toEqual({ selectedFilePaths: ['path/file.js'] });
      jest.spyOn(axios, 'get');
      expect(axios.get).not.toHaveBeenCalledWith(getCopyrightContextUrl);
    });
  });

  describe('load copyright file paths', function () {
    it('immediately dispatches a COPYRIGHT_FILE_PATHS_REQUEST action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadFilePathsOnPageUpdate(1));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_FILE_PATHS_REQUEST);
      expect(actions[0].payload).toEqual({ filePathsPage: 1 });
    });

    it(
      'immediately dispatches a COPYRIGHT_FILE_PATHS_REQUEST action and then COPYRIGHT_FILE_PATHS_FULFILLED action ' +
        'when copyright context loads',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);

        const copyrightFilePathsUrl = getCopyrightFilePathsUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier',
          'copyright_hash_2',
          15,
          15
        );
        mockAxiosCalls({
          get: {
            [copyrightFilePathsUrl]: Promise.resolve({
              data: {
                filePaths: [
                  {
                    copyrightMatches: 1,
                    filePath: 'path1/file1',
                  },
                ],
                totalFileMatches: 1,
              },
            }),
          },
        });

        store.dispatch(loadFilePathsOnPageUpdate(1)).then(() => {
          expect(axios.get).toHaveBeenCalledWith(copyrightFilePathsUrl);

          const actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe(COPYRIGHT_FILE_PATHS_FULFILLED);
          expect(actions[1].payload).toEqual({
            filePaths: {
              filePaths: [
                {
                  copyrightMatches: 1,
                  filePath: 'path1/file1',
                },
              ],
              totalFileMatches: 1,
            },
          });
          done();
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(COPYRIGHT_FILE_PATHS_REQUEST);
      }
    );

    it(
      'immediately dispatches a COPYRIGHT_FILE_PATHS_REQUEST action and then COPYRIGHT_FILE_PATHS_FAILED action ' +
        'when loading file paths fails',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);

        const copyrightFilePathsUrl = getCopyrightFilePathsUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier',
          'copyright_hash_2',
          15,
          15
        );
        mockAxiosCalls({
          get: {
            [copyrightFilePathsUrl]: () =>
              Promise.reject({
                value: 'Error',
              }),
          },
        });

        store.dispatch(loadFilePathsOnPageUpdate(1)).then(() => {
          expect(axios.get).toHaveBeenCalledWith(copyrightFilePathsUrl);

          const actions = store.getActions();
          expect(actions[1].type).toBe(COPYRIGHT_FILE_PATHS_FAILED);
          expect(actions[1].payload).toEqual({ value: 'Error' });
          done();
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(COPYRIGHT_FILE_PATHS_REQUEST);
      }
    );
  });
});
