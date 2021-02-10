/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getApplicationsUrl,
  getLicenseLegalApplicationReportUrl,
  getLicenseLegalComponentUrl,
  getOwnerHierarchyUrl,
  getSaveComponentObligationAttributionUrl,
  getDeleteComponentObligationAttributionUrl,
  getComponentObligationAttributionUrl
} from '../../../main/frontend/util/CLMLocation';
import {
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FAILED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
  ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE,
  loadApplicationReport,
  loadApplications,
  loadAvailableScopes,
  loadComponent,
  saveAttribution
} from '../../../main/frontend/advancedLegal/advancedLegalActions';
import { pick } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

describe('advancedLegalActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  describe('loadApplications', function () {
    let store;

    beforeEach(function () {
      store = SpecUtil.mockReduxStore({});
    });

    it('immediately dispatches a ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED action', function () {
      store.dispatch(loadApplications());

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED action with applications', function (done) {
      const applications = [{
        publicId: 'a'
      },
      {
        publicId: 'b'
      }];
      mockAxiosCalls({
        get: {
          [getApplicationsUrl()]: Promise.resolve({ data: applications })
        }
      });

      store.dispatch(loadApplications()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED);
        expect(actions[1].payload).toBe(applications);
        done();
      });
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED action when API fails', function (done) {
      const errorTest = 'Error test';
      mockAxiosCalls({
        get: {
          [getApplicationsUrl()]: Promise.reject(errorTest)
        }
      });

      store.dispatch(loadApplications()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED);
        expect(actions[1].payload).toBe(errorTest);
        done();
      });
    });
  });

  describe('loadApplicationReport', function () {
    let store;

    beforeEach(function () {
      store = SpecUtil.mockReduxStore({});
    });

    it('immediately dispatches a ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED action', function () {
      store.dispatch(loadApplicationReport('appId'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED action with applications', function (done) {
      const applicationPublicId = 'appId';
      const applicationReport = {
        components: [{ displayName: 'groupId : artifactId : version' }],
        licenseLegalMetadata: [{ licenseId: 'License Test' }]
      };
      mockAxiosCalls({
        get: {
          [getLicenseLegalApplicationReportUrl(applicationPublicId)]: Promise.resolve({ data: applicationReport })
        }
      });

      store.dispatch(loadApplicationReport(applicationPublicId)).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED);
        expect(actions[1].payload).toBe(applicationReport);
        done();
      });
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED action when API fails', function (done) {
      const applicationPublicId = 'appId';
      const errorTest = 'Error test';
      mockAxiosCalls({
        get: {
          [getLicenseLegalApplicationReportUrl(applicationPublicId)]: Promise.reject(errorTest)
        }
      });

      store.dispatch(loadApplicationReport(applicationPublicId)).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED);
        expect(actions[1].payload).toBe(errorTest);
        done();
      });
    });
  });

  describe('loadComponent', function () {
    let store;

    beforeEach(function () {
      store = SpecUtil.mockReduxStore({});
    });

    it('immediately dispatches a ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED action', function () {
      store.dispatch(loadComponent('orgOrApp', 'ownerId', 'hash'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED action with applications', function (done) {
      const componentInfo = {
        foo: 'bar'
      };
      mockAxiosCalls({
        get: {
          [getLicenseLegalComponentUrl('orgOrApp', 'ownerId', 'hash')]: Promise.resolve({ data: componentInfo })
        }
      });

      store.dispatch(loadComponent('orgOrApp', 'ownerId', 'hash')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED);
        expect(actions[1].payload).toBe(componentInfo);
        done();
      });
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_COMPONENT_FAILED action when API fails', function (done) {
      const errorTest = 'Error test';
      mockAxiosCalls({
        get: {
          [getLicenseLegalComponentUrl('orgOrApp', 'ownerId', 'hash')]: Promise.reject(errorTest)
        }
      });

      store.dispatch(loadComponent('orgOrApp', 'ownerId', 'hash')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_COMPONENT_FAILED);
        expect(actions[1].payload).toBe(errorTest);
        done();
      });
    });
  });

  describe('loadAvailableScopes', function () {
    let store;

    beforeEach(function () {
      store = SpecUtil.mockReduxStore({});
    });

    it('immediately dispatches a ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED action', function () {
      store.dispatch(loadAvailableScopes('ownerId'));

      const actions = store.getActions();
      expect(actions.length).toBe(1);
      expect(actions[0].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED);
      expect(actions[0].payload).toBeUndefined();
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED action with the hierarchy', function (done) {
      const payload = {
        id: 'ROOT_ORGANIZATION_ID',
        publicId: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
        type: 'organization',
        children: null
      };
      mockAxiosCalls({
        get: {
          [getOwnerHierarchyUrl('ownerType', 'ownerId')]: Promise.resolve({ data: payload })
        }
      });

      store.dispatch(loadAvailableScopes('ownerType', 'ownerId')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED);
        expect(actions[1].payload).toEqual(
            { values: [{ ...pick(['type', 'id', 'publicId', 'name'], payload), label: 'Organization' }] });
        done();
      });
    });

    it('dispatches a ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED action when the API fails', function (done) {
      const errorTest = 'Error test';
      mockAxiosCalls({
        get: {
          [getOwnerHierarchyUrl('ownerType', 'ownerId')]: Promise.reject(errorTest)
        }
      });

      store.dispatch(loadAvailableScopes('ownerType', 'ownerId')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[1].type).toBe(ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED);
        expect(actions[1].payload).toBe(errorTest);
        done();
      });
    });
  });

  describe('saveAttribution', function () {
    let store;
    let initialState = {
      advancedLegal: {
        component: {
          component: {
            componentIdentifier: 'componentIdentifier'
          },
          obligations: [
            { name: 'name', attributions: [{ id: 'id', content: 'content', ownerId: 'ROOT_ORGANIZATION_ID' }] }
          ]
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
        'id': 'id',
        'componentIdentifier': 'componentIdentifier',
        'obligationName': 'name',
        'content': 'content'
      };
      mockAxiosCalls({
        post: {
          [getSaveComponentObligationAttributionUrl('organization', 'ROOT_ORGANIZATION_ID')]: Promise.resolve(
              { data: 'data' })
        }
      });

      store.dispatch(saveAttribution('name')).then(() => {
        setTimeout(() => {
          const actions = store.getActions();
          expect(axios.post).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/obligation' +
              '/attribution',
              expectedPostBody);
          expect(actions.length).toBe(3);
          expect(actions[1].type).toBe(ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED);
          expect(actions[1].payload).toEqual({ name: 'name', value: 'data' });
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

    it('dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED action when the API fails with' +
        ' create/update', function(done) {
      store = SpecUtil.mockReduxStore(initialState);
      const expectedPostBody = {
        'id': 'id',
        'componentIdentifier': 'componentIdentifier',
        'obligationName': 'name',
        'content': 'content'
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

    it('dispatches a ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED action when the API succeeds with delete and there is ' +
        'no attribution at a higher scope',
    function(done) {
      let state = { ...initialState };
      state.advancedLegal.component.obligations[0].attributions[0].content = '';
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        del: {
          [getDeleteComponentObligationAttributionUrl('id')]: Promise.resolve({})
        },
        get: {
          [getComponentObligationAttributionUrl('organization', 'ROOT_ORGANIZATION_ID', 'componentIdentifier',
              'name')]: Promise.resolve({ data: [] })
        }
      });

      store.dispatch(saveAttribution('name')).then(() => {
        setTimeout(() => {
          const actions = store.getActions();
          expect(axios.delete).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/component/obligation/attribution/id');
          expect(axios.get).toHaveBeenCalledWith(
              '/api/experimental/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component/' +
              'obligation/attribution?componentIdentifier="componentIdentifier"&obligationName=name');
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
      state.advancedLegal.component.obligations[0].attributions[0].content = '';
      state.advancedLegal.component.obligations[0].attributions[0].ownerId = 'org';
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
              'obligation/attribution?componentIdentifier="componentIdentifier"&obligationName=name');
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
      state.advancedLegal.component.obligations[0].attributions[0].content = '';
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
});
