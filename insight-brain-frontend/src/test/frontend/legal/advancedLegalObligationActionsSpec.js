/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getSaveComponentObligationAttributionUrl,
  getDeleteComponentObligationAttributionUrl,
  getComponentObligationAttributionUrl,
  getSaveComponentObligationUrl,
  getDeleteComponentObligationUrl,
  getComponentObligationUrl
} from '../../../main/frontend/util/CLMLocation';
import {
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE,
  saveAttribution,
  saveObligation,
  ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED,
  ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED,
  ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE,
  ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED
} from '../../../main/frontend/legal/advancedLegalObligationActions';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

describe('advancedLegalObligationActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  describe('saveAttribution', function () {
    let store;
    let initialState = {
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier',
            licenseLegalData: {
              obligations: [
                { name: 'name' }
              ],
              attributions: [{ id: 'id', obligationName: 'name', content: 'content', ownerId: 'ROOT_ORGANIZATION_ID' }]
            }
          }
        },
        availableScopes: {
          values: [
            { id: 'org', publicId: 'org', type: 'organization' },
            { id: 'ROOT_ORGANIZATION_ID', publicId: 'ROOT_ORGANIZATION_ID', type: 'organization' }
          ]
        }
      }
    };

    it('immediately dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveAttribution('name'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED action when the API succeeds with' +
        ' create/update', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'id',
        componentIdentifier: 'componentIdentifier',
        obligationName: 'name',
        content: 'content'
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentObligationAttributionUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve(
              { data: 'postData' })
        },
        get: {
          [getComponentObligationAttributionUrl('organization', 'org', 'componentIdentifier', 'name')]:
              Promise.resolve(
                  { data: [{ id: 'id', content: 'content', ownerId: 'ROOT_ORGANIZATION_ID', foo: 'bar' }] })
        }
      });

      store.dispatch(saveAttribution('name')).then(() => {
        setTimeout(() => {
          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation' +
              '/attribution',
              expectedPostBody);
          expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/obligation' +
              '/attribution?componentIdentifier=%22componentIdentifier%22&obligationName=name');
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED);
          expect(actions[1].payload).toEqual(
              { name: 'name', value: { id: 'id', content: 'content', ownerId: 'ROOT_ORGANIZATION_ID' } });
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE);
          expect(actions[2].payload).toEqual({ name: 'name' });
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED action when the save API fails with' +
        ' create/update', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'id',
        componentIdentifier: 'componentIdentifier',
        obligationName: 'name',
        content: 'content'
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentObligationAttributionUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.reject('error')
        }
      });

      store.dispatch(saveAttribution('name')).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation/attribution',
            expectedPostBody);
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

    it('dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED action when the get API fails with' +
        ' create/update', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'id',
        componentIdentifier: 'componentIdentifier',
        obligationName: 'name',
        content: 'content'
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentObligationAttributionUrl('organization', 'ROOT_ORGANIZATION_ID')]:
              Promise.resolve('postData')
        },
        get: {
          [getComponentObligationAttributionUrl('organization', 'org', 'componentIdentifier', 'name')]:
              Promise.reject('error')
        }
      });

      store.dispatch(saveAttribution('name')).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation/attribution',
            expectedPostBody);
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

    it('dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED action when the API succeeds with delete and there is ' +
        'no attribution at a higher scope',
    function(done) {
      let state = { ...initialState };
      state.advancedLegal.component.component.licenseLegalData.attributions[0].content = '';
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationAttributionUrl('id')]: Promise.resolve({})
        },
        get: {
          [getComponentObligationAttributionUrl('organization', 'org', 'componentIdentifier',
              'name')]: Promise.resolve({ data: [] })
        }
      });

      store.dispatch(saveAttribution('name')).then(() => {
        setTimeout(() => {
          const actions = store.getActions();
          expect(axios.delete).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/component/obligation/attribution/id');
          expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/' +
              'obligation/attribution?componentIdentifier=%22componentIdentifier%22&obligationName=name');
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED);
          expect(actions[1].payload).toEqual(
              { name: 'name', value: { id: null, content: '', ownerId: 'ROOT_ORGANIZATION_ID' } });
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE);
          expect(actions[2].payload).toEqual({ name: 'name' });
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED action when the API succeeds with delete and there is ' +
        'an attribution at a higher scope',
    function(done) {
      let state = { ...initialState };
      state.advancedLegal.component.component.licenseLegalData.attributions[0].content = '';
      state.advancedLegal.component.component.licenseLegalData.attributions[0].ownerId = 'org';
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationAttributionUrl('id')]: Promise.resolve({})
        },
        get: {
          [getComponentObligationAttributionUrl('organization', 'org', 'componentIdentifier',
              'name')]: Promise.resolve({ data: [{ id: 'id2', content: 'content2', ownerId: 'ROOT_ORGANIZATION_ID' }] })
        }
      });

      store.dispatch(saveAttribution('name')).then(() => {
        setTimeout(() => {
          const actions = store.getActions();
          expect(axios.delete).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/component/obligation/attribution/id');
          expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/' +
              'obligation/attribution?componentIdentifier=%22componentIdentifier%22&obligationName=name');
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED);
          expect(actions[1].payload).toEqual(
              { name: 'name', value: { id: 'id2', content: 'content2', ownerId: 'ROOT_ORGANIZATION_ID' } });
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE);
          expect(actions[2].payload).toEqual({ name: 'name' });
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED action when the API fails with delete', function(done) {
      let state = { ...initialState };
      state.advancedLegal.component.component.licenseLegalData.attributions[0].content = '';
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationAttributionUrl('id')]: Promise.reject('error')
        }
      });

      store.dispatch(saveAttribution('name')).then(() => {
        const actions = store.getActions();
        expect(axios.delete).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/component/obligation/attribution/id');
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED);
        expect(actions[1].payload).toEqual(
            { name: 'name', value: 'error' });
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
                { id: 'id', name: 'name', status: 'OPEN', comment: 'comment', ownerId: 'ROOT_ORGANIZATION_ID' }
              ]
            }
          }
        },
        availableScopes: {
          values: [
            { id: 'org', publicId: 'org', type: 'organization' },
            { id: 'ROOT_ORGANIZATION_ID', publicId: 'ROOT_ORGANIZATION_ID', type: 'organization' }
          ]
        }
      }
    };

    it('immediately dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED action', function () {
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(saveObligation('name'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED action when the API succeeds with' +
        ' create/update', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'id',
        componentIdentifier: 'componentIdentifier',
        name: 'name',
        comment: 'comment',
        status: 'OPEN'
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve(
              { data: 'postData' })
        },
        get: {
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'name')]:
              Promise.resolve({
                data: {
                  id: 'id',
                  comment: 'comment',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  status: 'OPEN',
                  foo: 'bar',
                  lastUpdatedByUsername: 'admin',
                  lastUpdatedAt: 1618873200000
                }
              })
        }
      });

      store.dispatch(saveObligation('name')).then(() => {
        setTimeout(() => {
          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation',
              expectedPostBody);
          expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/' +
              'obligation?componentIdentifier=%22componentIdentifier%22&obligationName=name');
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
              lastUpdatedAt: 1618873200000
            }
          });
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE);
          expect(actions[2].payload).toEqual({ name: 'name' });
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED action when the save API fails with' +
        ' create/update', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'id',
        componentIdentifier: 'componentIdentifier',
        name: 'name',
        comment: 'comment',
        status: 'OPEN'
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.reject('error')
        }
      });

      store.dispatch(saveObligation('name')).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation',
            expectedPostBody);
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

    it('dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED action when the get API fails with' +
        ' create/update', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        id: 'id',
        componentIdentifier: 'componentIdentifier',
        name: 'name',
        comment: 'comment',
        status: 'OPEN'
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentObligationUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve('postData')
        },
        get: {
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier', 'name')]:
              Promise.reject('error')
        }
      });

      store.dispatch(saveObligation('name')).then(() => {
        const actions = store.getActions();
        expect(axios.post).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation',
            expectedPostBody);
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

    it('dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED action when the API succeeds with delete and there is ' +
        'no obligation at a higher scope',
    function(done) {
      let state = { ...initialState };
      state.advancedLegal.component.component.licenseLegalData.obligations[0].status = 'OPEN';
      state.advancedLegal.component.component.licenseLegalData.obligations[0].comment = '';
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationUrl('id')]: Promise.resolve({})
        },
        get: {
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier',
              'name')]: Promise.resolve({ data: null })
        }
      });

      store.dispatch(saveObligation('name')).then(() => {
        setTimeout(() => {
          const actions = store.getActions();
          expect(axios.delete).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/component/obligation/id');
          expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/' +
              'obligation?componentIdentifier=%22componentIdentifier%22&obligationName=name');
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
          expect(actions[1].payload).toEqual(
              { name: 'name', value: { id: null, comment: '', ownerId: 'ROOT_ORGANIZATION_ID', status: 'OPEN' } });
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE);
          expect(actions[2].payload).toEqual({ name: 'name' });
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED action when the API succeeds with delete and there is ' +
        'an obligation at a higher scope',
    function(done) {
      let state = { ...initialState };
      state.advancedLegal.component.component.licenseLegalData.obligations[0].ownerId = 'org';
      state.advancedLegal.component.component.licenseLegalData.obligations[0].comment = '';
      state.advancedLegal.component.component.licenseLegalData.obligations[0].status = 'OPEN';
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationUrl('id')]: Promise.resolve({})
        },
        get: {
          [getComponentObligationUrl('organization', 'org', 'componentIdentifier',
              'name')]: Promise.resolve(
              {
                data: {
                  id: 'id2',
                  comment: 'comment2',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  status: 'FLAGGED',
                  lastUpdatedByUsername: 'admin',
                  lastUpdatedAt: 1618873200000
                }
              })
        }
      });

      store.dispatch(saveObligation('name')).then(() => {
        setTimeout(() => {
          const actions = store.getActions();
          expect(axios.delete).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/component/obligation/id');
          expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/org/component/' +
              'obligation?componentIdentifier=%22componentIdentifier%22&obligationName=name');
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUCCEEDED);
          expect(actions[1].payload).toEqual(
              {
                name: 'name',
                value: {
                  id: 'id2',
                  comment: 'comment2',
                  ownerId: 'ROOT_ORGANIZATION_ID',
                  status: 'FLAGGED',
                  lastUpdatedByUsername: 'admin',
                  lastUpdatedAt: 1618873200000
                }
              });
          expect(actions[2].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_SUBMIT_MASK_DONE);
          expect(actions[2].payload).toEqual({ name: 'name' });
          done();
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });

    it('dispatches a ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED action when the API fails with delete', function(done) {
      let state = { ...initialState };
      state.advancedLegal.component.component.licenseLegalData.obligations[0].status = 'OPEN';
      state.advancedLegal.component.component.licenseLegalData.obligations[0].comment = '';
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationUrl('id')]: Promise.reject('error')
        }
      });

      store.dispatch(saveObligation('name')).then(() => {
        const actions = store.getActions();
        expect(axios.delete).toHaveBeenCalledWith(
            '/api/experimental/licenseLegalMetadata/component/obligation/id');
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_FAILED);
        expect(actions[1].payload).toEqual(
            { name: 'name', value: 'error' });
        done();
      });

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_SAVE_OBLIGATION_REQUESTED);
      expect(actions[0].payload).toEqual({ name: 'name' });
    });
  });
});
