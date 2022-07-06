/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { actions } from 'MainRoot/componentDetails/overview/overviewSlice';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import * as componentDetailsOverviewSelectors from 'MainRoot/componentDetails/overview/overviewSelectors';
import {
  getApplicableWaiversUrl,
  getApplicationReportsUrl,
  getComponentDetailsUrl,
  getInnerSourceComponentLatestVersionUrl,
  getVersionGraphUrl,
  getPolicyEvaluationTimestampUrl,
} from 'MainRoot/util/CLMLocation';

describe('componentDetailsOverviewActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    state = {};
    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadInnerSourceProducerData', () => {
    const { loadInnerSourceProducerData } = actions;
    const ownerApplicationId = 'mockOwnerApplicationId';

    it('immediately dispatches componentDetailsOverview/loadInnerSourceProducerData/pending action', () => {
      store.dispatch(loadInnerSourceProducerData());

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions).toHaveActionType('componentDetailsOverview/loadInnerSourceProducerData/pending');
    });

    it('should not send GET request if component is not innersource', () => {
      spyOn(applicationReportSelectors, 'selectSelectedComponent').and.returnValue({ innerSouce: false });
      mockAxiosCalls({
        get: () => {},
      });

      store.dispatch(loadInnerSourceProducerData());
      expect(axios.get).not.toHaveBeenCalled();
    });

    it('should not send GET request if component does not have ownerApplicationId', () => {
      spyOn(applicationReportSelectors, 'selectSelectedComponent').and.returnValue({ innerSource: true });
      mockAxiosCalls({
        get: () => {},
      });

      store.dispatch(loadInnerSourceProducerData());

      expect(axios.get).not.toHaveBeenCalled();
    });

    it('sends a GET ApplicationReportsUrl with the ownerApplicationId', () => {
      spyOn(applicationReportSelectors, 'selectSelectedComponent').and.returnValue({
        innerSource: true,
        innerSourceData: [{ ownerApplicationId }],
      });
      mockAxiosCalls({
        get: {
          [getApplicableWaiversUrl(ownerApplicationId)]: Promise.resolve(),
        },
      });

      store.dispatch(loadInnerSourceProducerData());

      const actions = store.getActions();
      expect(actions.length).toBe(2);
      expect(actions).toHaveActionType('componentDetailsOverview/loadInnerSourceProducerData/pending');
    });

    it('sets inssufficientPermission on 403 status', (done) => {
      spyOn(applicationReportSelectors, 'selectSelectedComponent').and.returnValue({
        innerSource: true,
        innerSourceData: [{ ownerApplicationId }],
      });
      mockAxiosCalls({
        get: {
          [getApplicationReportsUrl(ownerApplicationId)]: () =>
            Promise.reject({ response: { status: 403, data: 'error' } }),
        },
      });

      store.dispatch(loadInnerSourceProducerData()).then(() => {
        expect(store.getActions()).toHaveAction({
          type: 'componentDetailsOverview/setInsufficientPermission',
          payload: true,
        });

        done();
      });
    });

    it('should not set inssufficientPermission if error status is not 403', (done) => {
      spyOn(applicationReportSelectors, 'selectSelectedComponent').and.returnValue({
        innerSource: true,
        innerSourceData: [{ ownerApplicationId }],
      });
      mockAxiosCalls({
        get: {
          [getApplicationReportsUrl(ownerApplicationId)]: () => Promise.reject({ response: { data: 'error' } }),
        },
      });

      store.dispatch(loadInnerSourceProducerData()).then(() => {
        expect(store.getActions()).not.toHaveAction({
          type: 'componentDetailsOverview/setInsufficientPermission',
          payload: true,
        });
        expect(store.getActions()).toHaveAction({
          type: 'componentDetailsOverview/loadInnerSourceProducerData/rejected',
          payload: 'error',
        });

        done();
      });
    });

    it('reset insufficient permission, sets the producer"s report url and latest component version', (done) => {
      const getApplicableWaiversUrlPayload = { data: [{ latestReportHtmlUrl: 'mockReportUrl' }] };
      const componentIdentifier = { format: 'npm' };
      const getInnerSourceComponentLatestVersionUrlPayload = { data: '2.0' };

      const applicationReportsUrl = getApplicationReportsUrl(ownerApplicationId);
      const innerSourceComponentLatestVersionUrl = getInnerSourceComponentLatestVersionUrl(componentIdentifier);
      spyOn(applicationReportSelectors, 'selectSelectedComponent').and.returnValue({
        innerSource: true,
        innerSourceData: [{ ownerApplicationId }],
        componentIdentifier,
      });
      mockAxiosCalls({
        get: {
          [applicationReportsUrl]: Promise.resolve(getApplicableWaiversUrlPayload),
          [innerSourceComponentLatestVersionUrl]: Promise.resolve(getInnerSourceComponentLatestVersionUrlPayload),
        },
      });

      store.dispatch(loadInnerSourceProducerData()).then(() => {
        expect(store.getActions()).toHaveActionsInOrder([
          {
            type: 'componentDetailsOverview/setInnerSourceProducerReportUrl',
            payload: '/mockReportUrl',
          },
          { type: 'componentDetailsOverview/setInsufficientPermission', payload: false },
          { type: 'componentDetailsOverview/setLatestInnerSourceComponentVersion', payload: '2.0' },
        ]);
        expect(axios.get.calls.count()).toBe(2);
        expect(axios.get.calls.argsFor(0)[0]).toBe(applicationReportsUrl);
        expect(axios.get.calls.argsFor(1)[0]).toBe(innerSourceComponentLatestVersionUrl);

        done();
      });
    });
  });

  describe('openInnerSourceProducerReport', () => {
    const { openInnerSourceProducerReport } = actions;
    const selectedComponent = {
      componentIdentifier: {
        coordinates: {
          version: '2.0',
        },
      },
    };

    it('toggles the innerSource permissions modal', () => {
      spyOn(componentDetailsOverviewSelectors, 'selectInsufficientPermission').and.returnValue(true);

      store.dispatch(openInnerSourceProducerReport());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType('componentDetailsOverview/toggleInnerSourcePermissionsModal');
    });

    it('toggles the innerSource producer report modal', () => {
      spyOn(componentDetailsOverviewSelectors, 'selectInsufficientPermission').and.returnValue(false);
      spyOn(applicationReportSelectors, 'selectSelectedComponent').and.returnValue(selectedComponent);
      spyOn(componentDetailsOverviewSelectors, 'selectLatestInnerSourceComponentVersion').and.returnValue(
        'different version'
      );

      store.dispatch(actions.openInnerSourceProducerReport());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType('componentDetailsOverview/toggleInnerSourceProducerReportModal');
    });

    it('opens a new tab with the producer report url', () => {
      spyOn(applicationReportSelectors, 'selectSelectedComponent').and.returnValue(selectedComponent);
      spyOn(componentDetailsOverviewSelectors, 'selectLatestInnerSourceComponentVersion').and.returnValue('2.0');
      spyOn(componentDetailsOverviewSelectors, 'selectInnerSourceProducerUrl').and.returnValue('mockUrl');
      const windowOpenSpy = spyOn(window, 'open').and.stub();

      store.dispatch(actions.openInnerSourceProducerReport());

      expect(store.getActions().length).toBe(0);
      expect(windowOpenSpy).toHaveBeenCalledWith('mockUrl', '_blank');
    });
  });

  describe('loadSelectedVersionData', () => {
    beforeEach(() => {
      spyOn(componentDetailsOverviewSelectors, 'selectSelectedVersion').and.returnValue('2.3');
      spyOn(componentDetailsOverviewSelectors, 'selectCurrentVersion').and.returnValue('2.4');
    });

    it('does not trigger load request if newly selected version is equal to previously selected one', (done) => {
      store.dispatch(actions.loadSelectedVersionData('2.3')).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsOverview/loadSelectedVersionData/pending',
          'componentDetailsOverview/loadSelectedVersionData/fulfilled',
        ]);

        done();
      });
    });

    it('resets state for selected version if it equals to current', (done) => {
      store.dispatch(actions.loadSelectedVersionData('2.4')).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsOverview/loadSelectedVersionData/pending',
          'componentDetailsOverview/resetSelectedVersionData',
          'componentDetailsOverview/loadSelectedVersionData/fulfilled',
        ]);

        done();
      });
    });
  });

  describe('loadComponentDetailsByVerionsNumber', () => {
    beforeEach(() => {
      spyOn(componentDetailsOverviewSelectors, 'selectComponentDetailsSelectedRequestData').and.returnValue({});
    });

    it('loads selected version details', (done) => {
      const componentDetailsByVersion = {};
      mockAxiosCalls({
        get: {
          [getComponentDetailsUrl({})]: Promise.resolve({ data: componentDetailsByVersion }),
        },
      });

      store.dispatch(actions.loadComponentDetailsByVerionsNumber()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsOverview/loadComponentDetailsByVerionsNumber/pending',
          'componentDetailsOverview/loadComponentDetailsByVerionsNumber/fulfilled',
        ]);

        expect(actions[1].payload).toBe(componentDetailsByVersion);

        done();
      });
    });

    it('dispatches rejected action if selected version details request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getComponentDetailsUrl({})]: () => Promise.reject('failed to load component details with given version'),
        },
      });

      store.dispatch(actions.loadComponentDetailsByVerionsNumber()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsOverview/loadComponentDetailsByVerionsNumber/pending',
          'componentDetailsOverview/loadComponentDetailsByVerionsNumber/rejected',
        ]);

        expect(actions[1].payload).toBe('failed to load component details with given version');

        done();
      });
    });
  });

  describe('loadVersionExplorerDataWithCancelToken', () => {
    beforeEach(() => {
      spyOn(componentDetailsOverviewSelectors, 'selectVersionExplorerRequestData').and.returnValue({});
      spyOn(componentDetailsOverviewSelectors, 'selectComponentDetailsRequestData').and.returnValue({});
    });

    it('loads component details and versions data and dispatches fulfilled action with all the data', (done) => {
      const versionExplorerData = {};
      const componentDetails = {
        policyEvaluationTimestamps: {},
      };
      mockAxiosCalls({
        get: {
          [getVersionGraphUrl({})]: Promise.resolve({ data: versionExplorerData }),
          [getComponentDetailsUrl({})]: Promise.resolve({ data: componentDetails }),
          [getPolicyEvaluationTimestampUrl()]: Promise.resolve({ data: componentDetails.policyEvaluationTimestamps }),
        },
      });

      store.dispatch(actions.loadVersionExplorerDataWithCancelToken()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsOverview/loadVersionExplorerDataWithCancelToken/pending',
          'componentDetailsOverview/loadVersionExplorerDataWithCancelToken/fulfilled',
        ]);

        const fulfilledPayload = actions[1].payload;
        expect(fulfilledPayload.componentVersionsData).toBe(versionExplorerData);
        expect(fulfilledPayload.currentVersionDetails).toEqual(componentDetails);

        done();
      });
    });

    it('dispatches rejected action if Version Explorer data request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getVersionGraphUrl({})]: () => Promise.reject('failed to load version explorer data'),
          [getComponentDetailsUrl({})]: Promise.resolve({ data: {} }),
        },
      });

      store.dispatch(actions.loadVersionExplorerDataWithCancelToken()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsOverview/loadVersionExplorerDataWithCancelToken/pending',
          'componentDetailsOverview/loadVersionExplorerDataWithCancelToken/rejected',
        ]);

        expect(actions[1].payload).toBe('failed to load version explorer data');

        done();
      });
    });

    it('dispatches rejected action if Component Details request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getVersionGraphUrl({})]: Promise.resolve({ data: {} }),
          [getComponentDetailsUrl({})]: () => Promise.reject('failed to load component details'),
        },
      });

      store.dispatch(actions.loadVersionExplorerDataWithCancelToken()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsOverview/loadVersionExplorerDataWithCancelToken/pending',
          'componentDetailsOverview/loadVersionExplorerDataWithCancelToken/rejected',
        ]);

        expect(actions[1].payload).toBe('failed to load component details');

        done();
      });
    });
  });
});
