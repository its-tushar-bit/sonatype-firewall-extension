/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { omit } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { actions } from 'MainRoot/componentDetails/VulnerabilitiesTableTile/vulnerabilitiesSlice';
import * as vulnerabilitiesSelectors from 'MainRoot/componentDetails/VulnerabilitiesTableTile/vulnerabilitiesSelectors';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';

import {
  getVulnerabilitiesUrl,
  getVulnerabilityJsonDetailUrl,
  getVulnerabilityOverrideUrl,
} from 'MainRoot/util/CLMLocation';

describe('vulnerabilitiesSliceActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          publicId: 'appPublicId',
          scanId: 'currentScanId',
          hash: 'currentComponentHash',
        },
      },
      applicationReport: {
        selectedReport: {
          displayedEntries: [
            {
              hash: 'currentComponentHash',
              matchState: 'exact',
              proprietary: false,
              identificationSource: 'identificationSource',
              stageId: 'internalAppId',
              derivedDependencyType: 'derivedDependencyType',
              componentIdentifier: {
                componentType: 'componentType',
                format: 'format',
                coordinates: 'coordinates',
              },
            },
          ],
        },
        metadata: {
          application: {
            id: 'internalAppId',
            stageId: 'internalAppId',
          },
        },
      },
      componentDetailsVulnerabilities: {
        selectedRefId: '2',
        vulnerabilities: {
          data: [{ refId: '2', source: 'cve' }],
        },
        vulnerabilitySecurityOverride: {
          status: 'ACKNOWLEDGED',
          comments: { trimmedValue: 'Vulnerability Acknowledged' },
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadVulnerabilities', () => {
    const { loadVulnerabilities } = actions;
    const url = getVulnerabilitiesUrl({
      clientType: 'ci',
      ownerType: 'application',
      ownerId: 'appPublicId',
      matchState: 'exact',
      componentIdentifier: JSON.stringify({ format: 'format', coordinates: 'coordinates' }),
      hash: 'currentComponentHash',
      identificationSource: 'identificationSource',
      scanId: 'currentScanId',
    });

    beforeEach(() => {
      spyOn(vulnerabilitiesSelectors, 'selectVulnerabilitiesRequestData').and.returnValue({
        clientType: 'ci',
        componentIdentifier: '{"format":"format","coordinates":"coordinates"}',
        hash: 'currentComponentHash',
        identificationSource: 'identificationSource',
        ownerId: 'appPublicId',
        ownerType: 'application',
        scanId: 'currentScanId',
      });
    });

    it('immediately dispatches a componentDetailsVulnerabilities/loadVulnerabilities/pending action and appropriate requests', () => {
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve({}),
        },
      });

      store.dispatch(loadVulnerabilities());

      const actions = store.getActions();
      expect(actions).toHaveAction({
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/pending',
      });
      expect(axios.get).toHaveBeenCalledTimes(1);
      expect(axios.get).toHaveBeenCalledWith(
        '/rest/ci/componentDetails/application/appPublicId/vulnerabilities?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D&hash=currentComponentHash&identificationSource=identificationSource&scanId=currentScanId'
      );
    });

    it('dispatches a componentDetailsVulnerabilities/loadVulnerabilities/fulfilled action after successful requests', (done) => {
      const securityVulnerabilities = [
        {
          refId: '1',
          severity: 8,
          status: 'status 1',
        },
        {
          refId: '2',
          severity: 9.2,
          status: 'status 2',
        },
      ];
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve({
            data: { securityVulnerabilities },
          }),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/pending',
      };
      const expectedFulfilledAction = {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/fulfilled',
        payload: {
          data: { securityVulnerabilities },
        },
      };
      store.dispatch(loadVulnerabilities()).then(() => {
        // Remove metadata and custom error information from redux toolkit before comparisons
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFulfilledAction]);
        done();
      });
    });

    it('dispatches a componentDetailsVulnerabilities/loadVulnerabilities/rejected action after an error occurs in the requests', (done) => {
      mockAxiosCalls({
        get: {
          [url]: () => Promise.reject('errorMessage'),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/pending',
      };
      const expectedFailedAction = {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/rejected',
        payload: 'errorMessage',
      };

      store.dispatch(loadVulnerabilities()).then(() => {
        // Remove metadata and custom error information from redux toolkit before comparisons
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFailedAction]);
        done();
      });
    });
  });

  describe('loadFirewallVulnerabilities', () => {
    const { loadFirewallVulnerabilities } = actions;
    const url = getVulnerabilitiesUrl({
      clientType: 'ci',
      ownerType: 'repository',
      ownerId: 'repositoryId',
      matchState: 'exact',
      componentIdentifier: JSON.stringify({ format: 'format', coordinates: 'coordinates' }),
      hash: 'currentComponentHash',
      identificationSource: 'identificationSource',
      scanId: 'currentScanId',
    });

    beforeEach(() => {
      spyOn(vulnerabilitiesSelectors, 'selectFirewallVulnerabilitiesRequestData').and.returnValue({
        clientType: 'ci',
        componentIdentifier: '{"format":"format","coordinates":"coordinates"}',
        hash: 'currentComponentHash',
        identificationSource: 'identificationSource',
        ownerId: 'repositoryId',
        ownerType: 'repository',
        scanId: 'currentScanId',
      });
    });

    it('immediately dispatches a componentDetailsVulnerabilities/loadFirewallVulnerabilities/pending action and appropriate requests', () => {
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve({}),
        },
      });

      store.dispatch(loadFirewallVulnerabilities());

      const actions = store.getActions();
      expect(actions).toHaveAction({
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/pending',
      });
      expect(axios.get).toHaveBeenCalledTimes(1);
      expect(axios.get).toHaveBeenCalledWith(
        '/rest/ci/componentDetails/repository/repositoryId/vulnerabilities?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D&hash=currentComponentHash&identificationSource=identificationSource&scanId=currentScanId'
      );
    });

    it('dispatches a componentDetailsVulnerabilities/loadVulnerabilities/fulfilled action after successful requests', (done) => {
      const securityVulnerabilities = [
        {
          refId: '1',
          severity: 8,
          status: 'status 1',
        },
        {
          refId: '2',
          severity: 9.2,
          status: 'status 2',
        },
      ];
      mockAxiosCalls({
        get: {
          [url]: Promise.resolve({
            data: { securityVulnerabilities },
          }),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/pending',
      };
      const expectedFulfilledAction = {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/fulfilled',
        payload: {
          data: { securityVulnerabilities },
        },
      };
      store.dispatch(loadFirewallVulnerabilities()).then(() => {
        // Remove metadata and custom error information from redux toolkit before comparisons
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFulfilledAction]);
        done();
      });
    });

    it('dispatches a componentDetailsVulnerabilities/loadVulnerabilities/rejected action after an error occurs in the requests', (done) => {
      mockAxiosCalls({
        get: {
          [url]: () => Promise.reject('errorMessage'),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/pending',
      };
      const expectedFailedAction = {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/rejected',
        payload: 'errorMessage',
      };

      store.dispatch(loadFirewallVulnerabilities()).then(() => {
        // Remove metadata and custom error information from redux toolkit before comparisons
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFailedAction]);
        done();
      });
    });
  });

  describe('loadVulnerabilityDetails', () => {
    const { loadVulnerabilityDetails } = actions;
    const expectedComponentIdentifier = { format: 'format', coordinates: { part1: 'part1', part2: 'part2' } };
    beforeEach(() => {
      spyOn(vulnerabilitiesSelectors, 'selectVulnerabityRefId').and.returnValue('2');
      spyOn(applicationReportSelectors, 'selectSelectedComponent').and.returnValue({
        componentIdentifier: expectedComponentIdentifier,
      });
    });

    it('dispatches a componentDetailsVulnerabilities/loadVulnerabilityDetails/fulfilled action after successful requests', (done) => {
      const vulnerabilityDetails = {
        identifier: 'CVE-2014-3625',
        description: 'Directory traversal vulnerability',
        categories: ['data', 'operational'],
      };

      mockAxiosCalls({
        get: {
          [getVulnerabilityJsonDetailUrl('2', expectedComponentIdentifier)]: Promise.resolve({
            data: {
              ...vulnerabilityDetails,
            },
          }),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsVulnerabilities/loadVulnerabilityDetails/pending',
      };
      const expectedFulfilledAction = {
        type: 'componentDetailsVulnerabilities/loadVulnerabilityDetails/fulfilled',
        payload: { ...vulnerabilityDetails },
      };

      store.dispatch(loadVulnerabilityDetails()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));

        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFulfilledAction]);
        done();
      });
    });

    it('dispatches componentDetailsVulnerabilities/loadVulnerabilityDetails/rejected action', (done) => {
      mockAxiosCalls({
        get: {
          [getVulnerabilityJsonDetailUrl('2', expectedComponentIdentifier)]: () => Promise.reject('some error'),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsVulnerabilities/loadVulnerabilityDetails/pending',
      };
      const expectedFailedAction = {
        type: 'componentDetailsVulnerabilities/loadVulnerabilityDetails/rejected',
        payload: 'some error',
      };

      store.dispatch(loadVulnerabilityDetails()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));

        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFailedAction]);
        done();
      });
    });
  });

  describe('saveVulnerabilityOverride', () => {
    const { saveVulnerabilityOverride } = actions;
    const url = getVulnerabilityOverrideUrl('application', 'appPublicId');

    it('immediately dispatches a componentDetailsVulnerabilities/saveVulnerabilityOverride/pending action and appropriate requests', () => {
      const expectedPutPayload = {
        status: 'ACKNOWLEDGED',
        comment: 'Vulnerability Acknowledged',
        referenceId: '2',
        hash: 'currentComponentHash',
        source: 'cve',
      };

      mockAxiosCalls({
        put: {
          [url]: Promise.resolve({}),
        },
      });

      store.dispatch(saveVulnerabilityOverride());

      const actions = store.getActions();
      expect(actions).toHaveAction({
        type: 'componentDetailsVulnerabilities/saveVulnerabilityOverride/pending',
      });
      expect(axios.put).toHaveBeenCalledTimes(1);
      expect(axios.put).toHaveBeenCalledWith(
        '/rest/securityVulnerabilityOverride/application/appPublicId',
        expectedPutPayload
      );
    });

    it('immediately dispatches a componentDetailsVulnerabilities/saveVulnerabilityOverride/pending action with repository component route params and appropriate requests', () => {
      const expectedPutPayload = {
        status: 'ACKNOWLEDGED',
        comment: 'Vulnerability Acknowledged',
        referenceId: '2',
        hash: 'currentComponentHash',
        source: 'cve',
      };

      const repositoryComponentsVulnerabilitiesUrl = getVulnerabilitiesUrl({
        clientType: 'ci',
        ownerType: 'repository',
        ownerId: 'repositoryId',
        matchState: 'exact',
        componentIdentifier: JSON.stringify({ format: 'format', coordinates: 'coordinates' }),
        hash: 'currentComponentHash',
        identificationSource: 'identificationSource',
        scanId: 'currentScanId',
      });

      store = SpecUtil.mockReduxStore({
        ...state,
        router: {
          ...state.router,
          currentParams: {
            ...state.router.currentParams,
            repositoryId: 'repositoryId',
            componentHash: 'currentComponentHash',
          },
        },
      });

      mockAxiosCalls({
        put: {
          [repositoryComponentsVulnerabilitiesUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(saveVulnerabilityOverride(true));

      const actions = store.getActions();
      expect(actions).toHaveAction({
        type: 'componentDetailsVulnerabilities/saveVulnerabilityOverride/pending',
      });
      expect(axios.put).toHaveBeenCalledTimes(1);
      expect(axios.put).toHaveBeenCalledWith(
        '/rest/securityVulnerabilityOverride/repository/repositoryId',
        expectedPutPayload
      );
    });

    it('dispatches a componentDetailsVulnerabilities/saveVulnerabilityOverride/fulfilled action after successful requests', (done) => {
      const vulnerabilityOverrideSaveResponseData = {
        referenceId: 'refId',
      };
      mockAxiosCalls({
        put: {
          [url]: Promise.resolve({
            data: { vulnerabilityOverrideSaveResponseData },
          }),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsVulnerabilities/saveVulnerabilityOverride/pending',
      };
      const expectedFulfilledAction = {
        type: 'componentDetailsVulnerabilities/saveVulnerabilityOverride/fulfilled',
        payload: {
          vulnerabilityOverrideSaveResponseData,
        },
      };
      store.dispatch(saveVulnerabilityOverride()).then(() => {
        // Remove metadata and custom error information from redux toolkit before comparisons
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFulfilledAction]);
        done();
      });
    });

    it('dispatches componentDetailsVulnerabilities/saveVulnerabilityOverrideMaskDone after timeout after a successful request', function (done) {
      jasmine.clock().install();
      mockAxiosCalls({
        put: {
          [url]: Promise.resolve({
            data: { referenceId: 'refId' },
          }),
        },
      });

      store.dispatch(saveVulnerabilityOverride()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        jasmine.clock().uninstall();

        const actions = store.getActions();
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsVulnerabilities/saveVulnerabilityOverride/pending',
          'componentDetailsVulnerabilities/saveVulnerabilityOverride/fulfilled',
          'componentDetailsVulnerabilities/saveVulnerabilityOverrideMaskDone',
        ]);

        done();
      });
    });

    it('dispatches a componentDetailsVulnerabilities/saveVulnerabilityOverride/rejected action after an error occurs in the requests', (done) => {
      mockAxiosCalls({
        put: {
          [url]: () => Promise.reject('errorMessage'),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsVulnerabilities/saveVulnerabilityOverride/pending',
      };
      const expectedFailedAction = {
        type: 'componentDetailsVulnerabilities/saveVulnerabilityOverride/rejected',
        payload: 'errorMessage',
      };

      store.dispatch(saveVulnerabilityOverride()).then(() => {
        // Remove metadata and custom error information from redux toolkit before comparisons
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFailedAction]);
        done();
      });
    });
  });
});
