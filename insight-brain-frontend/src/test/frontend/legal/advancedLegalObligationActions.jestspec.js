/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getComponentObligationAttributionUrl,
  getComponentObligationUrl,
  getDeleteComponentObligationAttributionUrl,
  getDeleteComponentObligationsUrl,
  getSaveComponentObligationAttributionUrl,
  getSaveComponentObligationsUrl,
  getSaveComponentObligationUrl,
} from '../../../main/frontend/util/CLMLocation';
import {
  ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED,
  ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED,
  ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED,
  ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED,
  ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED,
  saveAllObligations,
  saveAttribution,
  saveObligation,
} from '../../../main/frontend/legal/obligation/advancedLegalObligationActions';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import {
  OBLIGATION_STATUS_FULFILLED,
  OBLIGATION_STATUS_OPEN,
} from '../../../main/frontend/legal/advancedLegalConstants';

import 'TestRoot/SpecUtil';

describe('advancedLegalObligationActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  beforeEach(() => jest.useFakeTimers());
  afterEach(() => jest.useRealTimers());

  describe('saveAttribution', function () {
    let store;
    let initialState = {
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            licenseLegalData: {
              obligations: [{ name: 'name' }],
              attributions: [
                {
                  id: 'id',
                  obligationName: 'name',
                  content: 'content',
                  ownerId: 'ROOT_ORGANIZATION_ID',
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

    it('immediately dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveAttribution({ obligationName: 'name', isAttributionDirty: true }));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });

    it('does not dispatch anything when not dirty', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveAttribution({ obligationName: 'name', isAttributionDirty: false }));
      const actions = store.getActions();
      expect(actions.length).toBe(0);
    });

    it(
      'dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED action when the API succeeds with' + ' create/update',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);
        const expectedPostBody = {
          id: 'id',
          componentIdentifier: 'componentIdentifier',
          obligationName: 'name',
          content: 'content',
        };
        mockAxiosCalls({
          post: {
            [getSaveComponentObligationAttributionUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({
              data: 'postData',
            }),
          },
          get: {
            [getComponentObligationAttributionUrl(
              'organization',
              'org',
              'componentIdentifier',
              'name'
            )]: Promise.resolve({
              data: [
                {
                  id: 'id',
                  content: 'content',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  foo: 'bar',
                },
              ],
            }),
          },
        });

        store
          .dispatch(
            saveAttribution({
              obligationName: 'name',
              isAttributionDirty: true,
            })
          )
          .then(() => {
            jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

            const actions = store.getActions();
            expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation' +
                '/attribution',
              expectedPostBody
            );
            expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/obligation' +
                '/attribution?componentIdentifier=%22componentIdentifier%22&obligationName=name'
            );
            expect(actions.length).toBe(3);
            expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED);
            expect(actions[1].payload).toEqual({
              name: 'name',
              value: {
                id: 'id',
                content: 'content',
                ownerId: 'ROOT_ORGANIZATION_ID',
              },
            });
            expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE);
            expect(actions[2].payload).toEqual({ name: 'name' });
            done();
          });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
        expect(actions[0].payload).toEqual({ name: 'name' });
      }
    );

    it(
      'dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED action when the save API fails with' + ' create/update',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);
        const expectedPostBody = {
          id: 'id',
          componentIdentifier: 'componentIdentifier',
          obligationName: 'name',
          content: 'content',
        };
        mockAxiosCalls({
          post: {
            [getSaveComponentObligationAttributionUrl('organization', 'ROOT_ORGANIZATION_ID')]: () =>
              Promise.reject('error'),
          },
        });

        store
          .dispatch(
            saveAttribution({
              obligationName: 'name',
              isAttributionDirty: true,
            })
          )
          .then(() => {
            const actions = store.getActions();
            expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation/attribution',
              expectedPostBody
            );
            expect(actions.length).toBe(2);
            expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED);
            expect(actions[1].payload).toEqual({
              name: 'name',
              value: 'error',
            });
            done();
          });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
        expect(actions[0].payload).toEqual({ name: 'name' });
      }
    );

    it(
      'dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED action when the get API fails with' + ' create/update',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);
        const expectedPostBody = {
          id: 'id',
          componentIdentifier: 'componentIdentifier',
          obligationName: 'name',
          content: 'content',
        };
        mockAxiosCalls({
          post: {
            [getSaveComponentObligationAttributionUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve(
              'postData'
            ),
          },
          get: {
            [getComponentObligationAttributionUrl('organization', 'org', 'componentIdentifier', 'name')]: () =>
              Promise.reject('error'),
          },
        });

        store
          .dispatch(
            saveAttribution({
              obligationName: 'name',
              isAttributionDirty: true,
            })
          )
          .then(() => {
            const actions = store.getActions();
            expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation/attribution',
              expectedPostBody
            );
            expect(actions.length).toBe(2);
            expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED);
            expect(actions[1].payload).toEqual({
              name: 'name',
              value: 'error',
            });
            done();
          });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
        expect(actions[0].payload).toEqual({ name: 'name' });
      }
    );

    it(
      'dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED action when the API succeeds with delete and there is ' +
        'no attribution at a higher scope',
      function (done) {
        let state = { ...initialState };
        state.advancedLegal.component.component.licenseLegalData.attributions[0].content = '';
        store = SpecUtil.mockReduxStore(state);
        mockAxiosCalls({
          del: {
            [getDeleteComponentObligationAttributionUrl('id')]: Promise.resolve({}),
          },
          get: {
            [getComponentObligationAttributionUrl(
              'organization',
              'org',
              'componentIdentifier',
              'name'
            )]: Promise.resolve({ data: [] }),
          },
        });

        store
          .dispatch(
            saveAttribution({
              obligationName: 'name',
              isAttributionDirty: true,
            })
          )
          .then(() => {
            jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

            const actions = store.getActions();
            expect(axios.delete).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/component/obligation/attribution/id'
            );
            expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/' +
                'obligation/attribution?componentIdentifier=%22componentIdentifier%22&obligationName=name'
            );
            expect(actions.length).toBe(3);
            expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED);
            expect(actions[1].payload).toEqual({
              name: 'name',
              value: {
                id: null,
                content: '',
                ownerId: 'ROOT_ORGANIZATION_ID',
              },
            });
            expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE);
            expect(actions[2].payload).toEqual({ name: 'name' });
            done();
          });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
        expect(actions[0].payload).toEqual({ name: 'name' });
      }
    );

    it(
      'dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED action when the API succeeds with delete and there is ' +
        'an attribution at a higher scope',
      function (done) {
        let state = { ...initialState };
        state.advancedLegal.component.component.licenseLegalData.attributions[0].content = '';
        state.advancedLegal.component.component.licenseLegalData.attributions[0].ownerId = 'org';
        store = SpecUtil.mockReduxStore(state);
        mockAxiosCalls({
          del: {
            [getDeleteComponentObligationAttributionUrl('id')]: Promise.resolve({}),
          },
          get: {
            [getComponentObligationAttributionUrl(
              'organization',
              'org',
              'componentIdentifier',
              'name'
            )]: Promise.resolve({
              data: [
                {
                  id: 'id2',
                  content: 'content2',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                },
              ],
            }),
          },
        });

        store
          .dispatch(
            saveAttribution({
              obligationName: 'name',
              isAttributionDirty: true,
            })
          )
          .then(() => {
            jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

            const actions = store.getActions();
            expect(axios.delete).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/component/obligation/attribution/id'
            );
            expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/' +
                'obligation/attribution?componentIdentifier=%22componentIdentifier%22&obligationName=name'
            );
            expect(actions.length).toBe(3);
            expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED);
            expect(actions[1].payload).toEqual({
              name: 'name',
              value: {
                id: 'id2',
                content: 'content2',
                ownerId: 'ROOT_ORGANIZATION_ID',
              },
            });
            expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE);
            expect(actions[2].payload).toEqual({ name: 'name' });
            done();
          });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
        expect(actions[0].payload).toEqual({ name: 'name' });
      }
    );

    it('dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED action when the API fails with delete', function (done) {
      let state = { ...initialState };
      state.advancedLegal.component.component.licenseLegalData.attributions[0].content = '';
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationAttributionUrl('id')]: () => Promise.reject('error'),
        },
      });

      store.dispatch(saveAttribution({ obligationName: 'name', isAttributionDirty: true })).then(() => {
        const actions = store.getActions();
        expect(axios.delete).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/component/obligation/attribution/id'
        );
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED);
        expect(actions[1].payload).toEqual({ name: 'name', value: 'error' });
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });
  });

  describe('saveObligation', function () {
    let store;
    let initialState = {
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            licenseLegalData: {
              obligations: [
                {
                  id: 'id',
                  name: 'name',
                  status: 'OPEN',
                  comment: 'comment',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                },
                {
                  id: 'id2',
                  name: 'test',
                  comment: '',
                  status: OBLIGATION_STATUS_OPEN,
                  ownerId: 'ROOT_ORGANIZATION_ID',
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

    it('throws an error when throwError is set to true', (done) => {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        post: {
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.reject('error'),
        },
        get: {
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'name')]: Promise.resolve({
            data: null,
          }),
        },
      });
      store.dispatch(saveObligation('name', true)).catch((error) => {
        expect(error.message).toEqual('error');
        done();
      });
    });

    it('throws an error when throwError is set to true and obligation comment is an empty string', (done) => {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationsUrl(['id2'])]: Promise.reject('error'),
        },
        get: {
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'test')]: Promise.resolve({
            data: null,
          }),
        },
      });
      store.dispatch(saveObligation('test', true)).catch((error) => {
        expect(error.message).toEqual('error');
        done();
      });
    });

    it('immediately dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveObligation('name'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });

    it(
      'dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED action when the API succeeds with' + ' create/update',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);
        const expectedPostBody = {
          id: 'id',
          componentIdentifier: 'componentIdentifier',
          name: 'name',
          comment: 'comment',
          status: 'OPEN',
        };
        mockAxiosCalls({
          post: {
            [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({
              data: 'postData',
            }),
          },
          get: {
            [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'name')]: Promise.resolve({
              data: {
                id: 'id',
                comment: 'comment',
                ownerId: 'ROOT_ORGANIZATION_ID',
                status: 'OPEN',
                foo: 'bar',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
            }),
          },
        });

        store.dispatch(saveObligation('name')).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation',
            expectedPostBody
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/org/component/' +
              'obligation?componentIdentifier=%22componentIdentifier%22&obligationName=name'
          );
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
          expect(actions[1].payload).toEqual({
            name: 'name',
            value: {
              id: 'id',
              comment: 'comment',
              ownerId: 'ROOT_ORGANIZATION_ID',
              status: 'OPEN',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          });
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE);
          expect(actions[2].payload).toEqual({ name: 'name' });
          done();
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
        expect(actions[0].payload).toEqual({ name: 'name' });
      }
    );

    it(
      'dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED action when the save API fails with' + ' create/update',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);
        const expectedPostBody = {
          id: 'id',
          componentIdentifier: 'componentIdentifier',
          name: 'name',
          comment: 'comment',
          status: 'OPEN',
        };
        mockAxiosCalls({
          post: {
            [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: () => Promise.reject('error'),
          },
        });

        store
          .dispatch(saveObligation('name'))
          .then(() => {
            const actions = store.getActions();
            expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation',
              expectedPostBody
            );
            expect(actions.length).toBe(2);
            expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED);
            expect(actions[1].payload).toEqual({ name: 'name', value: 'error' });
            done();
          })
          .catch(() => done());

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
        expect(actions[0].payload).toEqual({ name: 'name' });
      }
    );

    it(
      'dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED action when the get API fails with' + ' create/update',
      function (done) {
        store = SpecUtil.mockReduxStore(initialState);
        const expectedPostBody = {
          id: 'id',
          componentIdentifier: 'componentIdentifier',
          name: 'name',
          comment: 'comment',
          status: 'OPEN',
        };
        mockAxiosCalls({
          post: {
            [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve('postData'),
          },
          get: {
            [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'name')]: () =>
              Promise.reject('error'),
          },
        });

        store
          .dispatch(saveObligation('name'))
          .then(() => {
            const actions = store.getActions();
            expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation',
              expectedPostBody
            );
            expect(actions.length).toBe(2);
            expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED);
            expect(actions[1].payload).toEqual({ name: 'name', value: 'error' });
            done();
          })
          .catch(() => done());

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
        expect(actions[0].payload).toEqual({ name: 'name' });
      }
    );

    it(
      'dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED action when the API succeeds with delete and there is ' +
        'no obligation at a higher scope',
      function (done) {
        let state = { ...initialState };
        state.advancedLegal.component.component.licenseLegalData.obligations[0].status = 'OPEN';
        state.advancedLegal.component.component.licenseLegalData.obligations[0].comment = '';
        store = SpecUtil.mockReduxStore(state);
        mockAxiosCalls({
          del: {
            [getDeleteComponentObligationsUrl(['id'])]: Promise.resolve({}),
          },
          get: {
            [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'name')]: Promise.resolve({
              data: null,
            }),
          },
        });

        store.dispatch(saveObligation('name')).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          const actions = store.getActions();
          expect(axios.delete).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/component/obligation?componentObligationId=id'
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/org/component/' +
              'obligation?componentIdentifier=%22componentIdentifier%22&obligationName=name'
          );
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
          expect(actions[1].payload).toEqual({
            name: 'name',
            value: {
              id: null,
              comment: '',
              ownerId: 'ROOT_ORGANIZATION_ID',
              status: 'OPEN',
            },
          });
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE);
          expect(actions[2].payload).toEqual({ name: 'name' });
          done();
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
        expect(actions[0].payload).toEqual({ name: 'name' });
      }
    );

    it(
      'dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED action when the API succeeds with delete and there is ' +
        'an obligation at a higher scope',
      function (done) {
        let state = { ...initialState };
        state.advancedLegal.component.component.licenseLegalData.obligations[0].ownerId = 'org';
        state.advancedLegal.component.component.licenseLegalData.obligations[0].comment = '';
        state.advancedLegal.component.component.licenseLegalData.obligations[0].status = 'OPEN';
        store = SpecUtil.mockReduxStore(state);
        mockAxiosCalls({
          del: {
            [getDeleteComponentObligationsUrl(['id'])]: Promise.resolve({}),
          },
          get: {
            [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'name')]: Promise.resolve({
              data: {
                id: 'id2',
                comment: 'comment2',
                ownerId: 'ROOT_ORGANIZATION_ID',
                status: 'FLAGGED',
                lastUpdatedByUsername: 'admin',
                lastUpdatedAt: 1618873200000,
              },
            }),
          },
        });

        store.dispatch(saveObligation('name')).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          const actions = store.getActions();
          expect(axios.delete).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/component/obligation?componentObligationId=id'
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/org/component/' +
              'obligation?componentIdentifier=%22componentIdentifier%22&obligationName=name'
          );
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
          expect(actions[1].payload).toEqual({
            name: 'name',
            value: {
              id: 'id2',
              comment: 'comment2',
              ownerId: 'ROOT_ORGANIZATION_ID',
              status: 'FLAGGED',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          });
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE);
          expect(actions[2].payload).toEqual({ name: 'name' });
          done();
        });

        const actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
        expect(actions[0].payload).toEqual({ name: 'name' });
      }
    );

    it('dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED action when the API fails with delete', function (done) {
      let state = { ...initialState };
      state.advancedLegal.component.component.licenseLegalData.obligations[0].status = 'OPEN';
      state.advancedLegal.component.component.licenseLegalData.obligations[0].comment = '';
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationsUrl(['id'])]: () => Promise.reject('error'),
        },
      });

      store.dispatch(saveObligation('name')).then(() => {
        const actions = store.getActions();
        expect(axios.delete).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/component/obligation?componentObligationId=id'
        );
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED);
        expect(actions[1].payload).toEqual({ name: 'name', value: 'error' });
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });
  });

  describe('saveAllObligations', function () {
    let store;
    let initialState = {
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            licenseLegalData: {
              obligations: [
                {
                  id: 'id',
                  name: 'name',
                  status: 'OPEN',
                  comment: 'comment',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                },
                {
                  id: null,
                  name: 'otherName',
                  status: 'OPEN',
                  comment: 'comment',
                  ownerId: 'org',
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

    it('immediately dispatches a ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveAllObligations(OBLIGATION_STATUS_FULFILLED, 'comment', 'org'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED action when the API succeeds with post', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = [
        {
          id: 'id',
          componentIdentifier: 'componentIdentifier',
          name: 'name',
          comment: 'new comment',
          status: OBLIGATION_STATUS_FULFILLED,
        },
        {
          id: null,
          componentIdentifier: 'componentIdentifier',
          name: 'otherName',
          comment: 'new comment',
          status: OBLIGATION_STATUS_FULFILLED,
        },
      ];
      mockAxiosCalls({
        post: {
          [getSaveComponentObligationsUrl('organization', 'org')]: Promise.resolve({ data: 'postData' }),
        },
        get: {
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'name')]: Promise.resolve({
            data: {
              id: 'id',
              comment: 'new comment',
              ownerId: 'org',
              status: OBLIGATION_STATUS_FULFILLED,
              foo: 'bar',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          }),
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'otherName')]: Promise.resolve({
            data: {
              id: 'otherId',
              comment: 'new comment',
              ownerId: 'org',
              status: OBLIGATION_STATUS_FULFILLED,
              foo: 'bar',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          }),
        },
      });

      store.dispatch(saveAllObligations(OBLIGATION_STATUS_FULFILLED, 'new comment', 'org')).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/org/component/obligations',
          expectedPostBody
        );
        expect(axios.get).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/org/component/' +
            'obligation?componentIdentifier=%22componentIdentifier%22&obligationName=name'
        );
        expect(axios.get).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/org/component/' +
            'obligation?componentIdentifier=%22componentIdentifier%22&obligationName=otherName'
        );
        expect(actions.length).toBe(7);
        expect(actions[3].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED);

        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
        expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
        expect(actions[1].payload.name).toEqual('name');
        expect(actions[2].payload.name).toEqual('otherName');
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED action when the save API fails', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = [
        {
          id: 'id',
          componentIdentifier: 'componentIdentifier',
          name: 'name',
          comment: 'comment',
          status: OBLIGATION_STATUS_OPEN,
        },
        {
          id: null,
          componentIdentifier: 'componentIdentifier',
          name: 'otherName',
          comment: 'comment',
          status: OBLIGATION_STATUS_OPEN,
        },
      ];
      mockAxiosCalls({
        post: {
          [getSaveComponentObligationsUrl('organization', 'ROOT_ORGANIZATION_ID')]: () => Promise.reject('error'),
        },
      });

      store.dispatch(saveAllObligations(OBLIGATION_STATUS_OPEN, 'comment', 'ROOT_ORGANIZATION_ID')).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligations',
          expectedPostBody
        );
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED);
        expect(actions[1].payload).toEqual({ value: 'error' });
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED);
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED action when the get API fails with create/update', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = [
        {
          id: 'id',
          componentIdentifier: 'componentIdentifier',
          name: 'name',
          comment: 'comment',
          status: OBLIGATION_STATUS_OPEN,
        },
        {
          id: null,
          componentIdentifier: 'componentIdentifier',
          name: 'otherName',
          comment: 'comment',
          status: OBLIGATION_STATUS_OPEN,
        },
      ];
      mockAxiosCalls({
        post: {
          [getSaveComponentObligationsUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve('postData'),
        },
        get: {
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'name')]: () =>
            Promise.reject('error'),
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'otherName')]: Promise.resolve({
            data: {
              id: 'id',
              comment: 'comment',
              ownerId: 'org',
              status: OBLIGATION_STATUS_FULFILLED,
              foo: 'bar',
              lastUpdatedByUsername: 'admin',
              lastUpdatedAt: 1618873200000,
            },
          }),
        },
      });

      store.dispatch(saveAllObligations(OBLIGATION_STATUS_OPEN, 'comment', 'ROOT_ORGANIZATION_ID')).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligations',
          expectedPostBody
        );
        expect(actions.length).toBe(4);
        expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED);
        expect(actions[2].payload).toEqual({ name: 'name', value: 'error' });
        expect(actions[3].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED);
        done();
      });

      const actions = store.getActions();
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED);
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED action when the API fails with delete', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationsUrl(['id'])]: () => Promise.reject('error'),
        },
      });

      store.dispatch(saveAllObligations(OBLIGATION_STATUS_OPEN, '', 'ROOT_ORGANIZATION_ID')).then(() => {
        const actions = store.getActions();
        expect(axios.delete).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/component/obligation?componentObligationId=id'
        );
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_FAILED);
        expect(actions[1].payload).toEqual({ value: 'error' });
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED);
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED action when the API succeeds with delete single', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationsUrl(['id'])]: Promise.resolve({}),
        },
        get: {
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'name')]: Promise.resolve({
            data: null,
          }),
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'otherName')]: Promise.resolve({
            data: null,
          }),
        },
      });

      store.dispatch(saveAllObligations(OBLIGATION_STATUS_OPEN, '', 'ROOT_ORGANIZATION_ID')).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();
        expect(axios.delete).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/component/obligation?componentObligationId=id'
        );
        expect(axios.get).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/org/component/' +
            'obligation?componentIdentifier=%22componentIdentifier%22&obligationName=name'
        );
        expect(axios.get).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/org/component/' +
            'obligation?componentIdentifier=%22componentIdentifier%22&obligationName=otherName'
        );
        expect(actions.length).toBe(7);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
        expect(actions[1].payload).toEqual({
          name: 'name',
          value: {
            id: null,
            comment: '',
            ownerId: 'ROOT_ORGANIZATION_ID',
            status: 'OPEN',
          },
        });
        expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
        expect(actions[2].payload).toEqual({
          name: 'otherName',
          value: {
            id: null,
            comment: '',
            ownerId: 'ROOT_ORGANIZATION_ID',
            status: 'OPEN',
          },
        });
        expect(actions[3].type).toEqual(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED);
        expect(actions[6].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUBMIT_MASK_DONE);
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED);
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED action when the API succeeds with delete multiple', function (done) {
      let state = { ...initialState };
      state.advancedLegal.component.component.licenseLegalData = {
        obligations: [
          {
            id: 'id',
            name: 'name',
            status: OBLIGATION_STATUS_FULFILLED,
            comment: 'comment',
            ownerId: 'org',
          },
          {
            id: 'id2',
            name: 'otherName',
            status: OBLIGATION_STATUS_FULFILLED,
            comment: 'comment',
            ownerId: 'org',
          },
        ],
      };
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationsUrl(['id', 'id2'])]: Promise.resolve({}),
        },
        get: {
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'name')]: Promise.resolve({
            data: null,
          }),
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'otherName')]: Promise.resolve({
            data: null,
          }),
        },
      });

      store.dispatch(saveAllObligations(OBLIGATION_STATUS_OPEN, '', 'org')).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();
        expect(axios.delete).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/component/obligation?componentObligationId=id' +
            '&componentObligationId=id2'
        );
        expect(axios.get).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/org/component/' +
            'obligation?componentIdentifier=%22componentIdentifier%22&obligationName=name'
        );
        expect(axios.get).toHaveBeenCalledWith(
          '/api/experimental/licenseLegalMetadata/organization/org/component/' +
            'obligation?componentIdentifier=%22componentIdentifier%22&obligationName=otherName'
        );
        expect(actions.length).toBe(7);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
        expect(actions[1].payload).toEqual({
          name: 'name',
          value: {
            id: null,
            comment: '',
            ownerId: 'ROOT_ORGANIZATION_ID',
            status: 'OPEN',
          },
        });
        expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
        expect(actions[2].payload).toEqual({
          name: 'otherName',
          value: {
            id: null,
            comment: '',
            ownerId: 'ROOT_ORGANIZATION_ID',
            status: 'OPEN',
          },
        });
        expect(actions[3].type).toEqual(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED);
        expect(actions[6].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUBMIT_MASK_DONE);
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED);
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED action when the API succeeds with delete nothing', function (done) {
      let state = { ...initialState };
      state.advancedLegal.component.component.licenseLegalData = {
        obligations: [
          {
            id: null,
            name: 'name',
            status: 'FULFILLED',
            comment: 'comment',
            ownerId: 'org',
          },
          {
            id: null,
            name: 'otherName',
            status: 'FULFILLED',
            comment: 'comment',
            ownerId: 'org',
          },
        ],
      };
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        get: {
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'name')]: Promise.resolve({
            data: null,
          }),
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'otherName')]: Promise.resolve({
            data: null,
          }),
        },
      });

      store.dispatch(saveAllObligations(OBLIGATION_STATUS_OPEN, '', 'org')).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(actions[1].type).toEqual(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUCCEEDED);
        expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_SUBMIT_MASK_DONE);
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ALL_OBLIGATIONS_REQUESTED);
    });
  });

  describe('save obligation attribution and status', function () {
    let store;
    let initialState = {
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            licenseLegalData: {
              obligations: [
                {
                  name: 'Must State Changes',
                  id: 'd387da0b87a9428fbc352f437c8294cf',
                  status: 'FLAGGED',
                  comment: 'comment',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                },
              ],
              attributions: [
                {
                  id: 'id',
                  obligationName: 'Must State Changes',
                  content: 'content',
                  ownerId: 'ROOT_ORGANIZATION_ID',
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

    it('dispatches the expected actions on success', function (done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'id',
        componentIdentifier: 'componentIdentifier',
        obligationName: 'Must State Changes',
        content: 'content',
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentObligationAttributionUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({
            data: 'postData',
          }),
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve({
            data: {
              data: 'dataPOST2',
            },
          }),
        },
        get: {
          [getComponentObligationAttributionUrl(
            'organization',
            'org',
            'componentIdentifier',
            'Must State Changes'
          )]: Promise.resolve({
            data: [
              {
                id: 'id',
                content: 'content',
                ownerId: 'ROOT_ORGANIZATION_ID',
                foo: 'bar',
              },
            ],
          }),
          [getComponentObligationUrl(
            'organization',
            'org',
            'componentIdentifier',
            'Must State Changes'
          )]: Promise.resolve({
            data: {
              id: 'd387da0b87a9428fbc352f437c8294cf',
              comment: 'comment',
              ownerId: 'ROOT_ORGANIZATION_ID',
              status: 'FLAGGED',
              name: 'Must State Changes',
            },
          }),
        },
      });
      store
        .dispatch(
          saveAttribution({
            obligationName: 'Must State Changes',
            isAttributionDirty: true,
            isObligationDirty: true,
            existingObligation: {
              name: 'Must State Changes',
              status: 'FLAGGED',
            },
          })
        )
        .then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS * 2);

          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/' +
              'component/obligation/attribution',
            expectedPostBody
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/org/component/obligation/attribution?' +
              'componentIdentifier=%22componentIdentifier%22&obligationName=Must%20State%20Changes'
          );
          expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/' +
              'obligation/attribution',
            {
              id: 'id',
              componentIdentifier: 'componentIdentifier',
              obligationName: 'Must State Changes',
              content: 'content',
            }
          );
          expect(axios.get).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/org/component/obligation/attribution?' +
              'componentIdentifier=%22componentIdentifier%22&obligationName=Must%20State%20Changes'
          );
          expect(actions.length).toBe(5);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED);
          expect(actions[1].payload).toEqual({
            name: 'Must State Changes',
            value: {
              id: 'id',
              content: 'content',
              ownerId: 'ROOT_ORGANIZATION_ID',
            },
          });
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
          expect(actions[3].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
          expect(actions[3].payload).toEqual({
            name: 'Must State Changes',
            value: {
              id: 'd387da0b87a9428fbc352f437c8294cf',
              comment: 'comment',
              ownerId: 'ROOT_ORGANIZATION_ID',
              status: 'FLAGGED',
            },
          });
          expect(actions[4].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE);
          expect(actions[4].payload).toEqual({ name: 'Must State Changes' });
          done();
        });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
    });
  });
});
