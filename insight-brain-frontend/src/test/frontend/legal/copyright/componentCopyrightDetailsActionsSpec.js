/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
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
  unloadCopyrightContexts,
} from '../../../../main/frontend/legal/copyright/componentCopyrightDetailsActions';
import {
  getCopyrightContextUrl,
  getCopyrightFileCountUrl,
  getCopyrightFilePathsUrl,
} from '../../../../main/frontend/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

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
            copyrights: [
              { originalContentHash: 'copyright_hash_1' },
              { originalContentHash: 'copyright_hash_2' },
            ],
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
        copyrightIndex: '0',
      },
    },
    componentCopyrightDetails: {
      copyrightIndex: 1,
    },
  };

  describe('load copyright details', function () {
    it('immediately dispatches a COPYRIGHT_DETAILS_REQUEST action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(
        loadComponentAndCopyrightDetails(
          'organization',
          'org',
          'componentHash',
          1
        )
      );

      const actions = store.getActions();
      console.log(actions);
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
        let copyrightFileCountUrl = getCopyrightFileCountUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier'
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
              data: ['path1/file1', 'path2/file2', 'path3/file3'],
            }),
          },
        });

        store
          .dispatch(
            loadComponentAndCopyrightDetails(
              'organization',
              'org',
              'componentHash',
              1
            )
          )
          .then(() => {
            setTimeout(() => {
              expect(axios.get).toHaveBeenCalledWith(copyrightFileCountUrl);
              expect(axios.get).toHaveBeenCalledWith(copyrightFilePathsUrl);

              const actions = store.getActions();
              expect(actions[1].type).toBe(COPYRIGHT_DETAILS_FULFILLED);
              expect(actions[1].payload).toEqual({
                copyrightIndex: 1,
                copyright: { originalContentHash: 'copyright_hash_2' },
                filePaths: ['path1/file1', 'path2/file2', 'path3/file3'],
                copyrightFileCounts: { path1: 5, path2: 10 },
              });
              done();
            }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
          });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(COPYRIGHT_DETAILS_REQUEST);
      }
    );

    function verifyCopyrightDetailsFailure(
      copyrightFileCountUrl,
      copyrightFilePathsUrl,
      done
    ) {
      store
        .dispatch(
          loadComponentAndCopyrightDetails(
            'organization',
            'org',
            'componentHash',
            1
          )
        )
        .then(() => {
          setTimeout(() => {
            expect(axios.get).toHaveBeenCalledWith(copyrightFileCountUrl);
            expect(axios.get).toHaveBeenCalledWith(copyrightFilePathsUrl);

            const actions = store.getActions();
            expect(actions[1].type).toBe(COPYRIGHT_DETAILS_FAILED);
            expect(actions[1].payload).toEqual({ value: 'Error' });
            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
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

        mockAxiosCalls({
          get: {
            [copyrightFileCountUrl]: Promise.reject({ value: 'Error' }),
            [copyrightFilePathsUrl]: Promise.resolve({
              data: ['path1/file1', 'path2/file2', 'path3/file3'],
            }),
          },
        });
        verifyCopyrightDetailsFailure(
          copyrightFileCountUrl,
          copyrightFilePathsUrl,
          done
        );
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
        let copyrightFileCountUrl = getCopyrightFileCountUrl(
          'organization',
          'org',
          'componentHash',
          'componentIdentifier'
        );

        mockAxiosCalls({
          get: {
            [copyrightFileCountUrl]: Promise.resolve({
              data: {
                path1: 5,
                path2: 10,
              },
            }),
            [copyrightFilePathsUrl]: Promise.reject({ value: 'Error' }),
          },
        });

        verifyCopyrightDetailsFailure(
          copyrightFileCountUrl,
          copyrightFilePathsUrl,
          done
        );
      }
    );
  });

  describe('load copyright contexts', function () {
    it('immediately dispatches a COPYRIGHT_CONTEXT_REQUEST action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(loadCopyrightContexts('path/file.js'));

      const actions = store.getActions();
      console.log(actions);
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(COPYRIGHT_CONTEXT_REQUEST);
      expect(actions[0].payload).toEqual({ selectedFilePath: 'path/file.js' });
    });

    it(
      'immediately dispatches a COPYRIGHT_CONTEXT_REQUEST action and then COPYRIGHT_CONTEXT_FULFILLED action ' +
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
              data: ['context'],
            }),
          },
        });

        store.dispatch(loadCopyrightContexts('path/file.js')).then(() => {
          setTimeout(() => {
            expect(axios.get).toHaveBeenCalledWith(copyrightContextUrl);

            const actions = store.getActions();
            expect(actions[1].type).toBe(COPYRIGHT_CONTEXT_FULFILLED);
            expect(actions[1].payload).toEqual({
              copyrightContexts: ['context'],
            });
            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(COPYRIGHT_CONTEXT_REQUEST);
      }
    );

    it(
      'immediately dispatches a COPYRIGHT_CONTEXT_REQUEST action and then COPYRIGHT_CONTEXT_FULFILLED action ' +
        'when copyright context unloads',
      function () {
        store = SpecUtil.mockReduxStore(initialState);

        store.dispatch(unloadCopyrightContexts());
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe(COPYRIGHT_CONTEXT_REQUEST);
        expect(actions[0].payload).toEqual({ selectedFilePath: null });
        expect(actions[1].type).toBe(COPYRIGHT_CONTEXT_FULFILLED);
        expect(actions[1].payload).toEqual({ copyrightContexts: [] });
      }
    );

    it(
      'immediately dispatches a COPYRIGHT_CONTEXT_REQUEST action and then COPYRIGHT_CONTEXT_FAILED action ' +
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
            [copyrightContextUrl]: Promise.reject({
              value: 'Error',
            }),
          },
        });

        store.dispatch(loadCopyrightContexts('path/file.js')).then(() => {
          setTimeout(() => {
            expect(axios.get).toHaveBeenCalledWith(copyrightContextUrl);

            const actions = store.getActions();
            expect(actions[1].type).toBe(COPYRIGHT_CONTEXT_FAILED);
            expect(actions[1].payload).toEqual({ value: 'Error' });
            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(COPYRIGHT_CONTEXT_REQUEST);
      }
    );
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
              data: ['path1/file1', 'path2/file2', 'path3/file3'],
            }),
          },
        });

        store.dispatch(loadFilePathsOnPageUpdate(1)).then(() => {
          setTimeout(() => {
            expect(axios.get).toHaveBeenCalledWith(copyrightFilePathsUrl);

            const actions = store.getActions();
            expect(actions.length).toBe(2);
            expect(actions[1].type).toBe(COPYRIGHT_FILE_PATHS_FULFILLED);
            expect(actions[1].payload).toEqual({
              filePaths: ['path1/file1', 'path2/file2', 'path3/file3'],
            });
            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
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
            [copyrightFilePathsUrl]: Promise.reject({
              value: 'Error',
            }),
          },
        });

        store.dispatch(loadFilePathsOnPageUpdate(1)).then(() => {
          setTimeout(() => {
            expect(axios.get).toHaveBeenCalledWith(copyrightFilePathsUrl);

            const actions = store.getActions();
            expect(actions[1].type).toBe(COPYRIGHT_FILE_PATHS_FAILED);
            expect(actions[1].payload).toEqual({ value: 'Error' });
            done();
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(COPYRIGHT_FILE_PATHS_REQUEST);
      }
    );
  });
});
