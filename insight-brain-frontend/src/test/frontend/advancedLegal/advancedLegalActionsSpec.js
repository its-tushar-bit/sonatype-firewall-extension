/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import {
  getApplicationsUrl,
  getLicenseLegalApplicationReportUrl, getLicenseLegalComponentUrl
} from '../../../main/frontend/util/CLMLocation';
import {
  ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED,
  ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FAILED,
  loadApplications,
  loadApplicationReport,
  loadComponent
} from '../../../main/frontend/advancedLegal/advancedLegalActions';

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
});
