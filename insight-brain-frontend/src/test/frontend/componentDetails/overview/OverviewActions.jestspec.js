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

import 'TestRoot/SpecUtil';

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
      jest.spyOn(applicationReportSelectors, 'selectSelectedComponent').mockReturnValue({ innerSouce: false });
      mockAxiosCalls({
        get: () => {},
      });

      store.dispatch(loadInnerSourceProducerData());
      expect(axios.get).not.toHaveBeenCalled();
    });

    it('should not send GET request if component does not have ownerApplicationId', () => {
      jest.spyOn(applicationReportSelectors, 'selectSelectedComponent').mockReturnValue({ innerSource: true });
      mockAxiosCalls({
        get: () => {},
      });

      store.dispatch(loadInnerSourceProducerData());

      expect(axios.get).not.toHaveBeenCalled();
    });

    it('sends a GET ApplicationReportsUrl with the ownerApplicationId', () => {
      jest.spyOn(applicationReportSelectors, 'selectSelectedComponent').mockReturnValue({
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
      jest.spyOn(applicationReportSelectors, 'selectSelectedComponent').mockReturnValue({
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
      jest.spyOn(applicationReportSelectors, 'selectSelectedComponent').mockReturnValue({
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
      jest.spyOn(applicationReportSelectors, 'selectSelectedComponent').mockReturnValue({
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
        expect(axios.get.mock.calls.length).toBe(2);
        expect(axios.get.mock.calls[0][0]).toBe(applicationReportsUrl);
        expect(axios.get.mock.calls[1][0]).toBe(innerSourceComponentLatestVersionUrl);

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
      jest.spyOn(componentDetailsOverviewSelectors, 'selectInsufficientPermission').mockReturnValue(true);

      store.dispatch(openInnerSourceProducerReport());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType('componentDetailsOverview/toggleInnerSourcePermissionsModal');
    });

    it('toggles the innerSource producer report modal', () => {
      jest.spyOn(componentDetailsOverviewSelectors, 'selectInsufficientPermission').mockReturnValue(false);
      jest.spyOn(applicationReportSelectors, 'selectSelectedComponent').mockReturnValue(selectedComponent);
      jest
        .spyOn(componentDetailsOverviewSelectors, 'selectLatestInnerSourceComponentVersion')
        .mockReturnValue('different version');

      store.dispatch(actions.openInnerSourceProducerReport());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()).toHaveActionType('componentDetailsOverview/toggleInnerSourceProducerReportModal');
    });

    it('opens a new tab with the producer report url', () => {
      jest.spyOn(applicationReportSelectors, 'selectSelectedComponent').mockReturnValue(selectedComponent);
      jest.spyOn(componentDetailsOverviewSelectors, 'selectLatestInnerSourceComponentVersion').mockReturnValue('2.0');
      jest.spyOn(componentDetailsOverviewSelectors, 'selectInnerSourceProducerUrl').mockReturnValue('mockUrl');
      const windowOpenSpy = jest.spyOn(window, 'open').mockReturnValue(undefined);

      store.dispatch(actions.openInnerSourceProducerReport());

      expect(store.getActions().length).toBe(0);
      expect(windowOpenSpy).toHaveBeenCalledWith('mockUrl', '_blank');
    });
  });

  describe('loadSelectedVersionData', () => {
    beforeEach(() => {
      jest.spyOn(componentDetailsOverviewSelectors, 'selectSelectedVersion').mockReturnValue('2.3');
      jest.spyOn(componentDetailsOverviewSelectors, 'selectCurrentVersion').mockReturnValue('2.4');
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
      jest.spyOn(componentDetailsOverviewSelectors, 'selectComponentDetailsSelectedRequestData').mockReturnValue({});
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
      jest.spyOn(componentDetailsOverviewSelectors, 'selectVersionExplorerRequestData').mockReturnValue({});
      jest.spyOn(componentDetailsOverviewSelectors, 'selectComponentDetailsRequestData').mockReturnValue({});
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
