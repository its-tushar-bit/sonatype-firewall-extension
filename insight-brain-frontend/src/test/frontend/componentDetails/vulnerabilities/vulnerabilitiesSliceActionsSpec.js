/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { omit } from 'ramda';

import { actions } from '../../../../main/frontend/componentDetails/VulnerabilitiesTableTile/vulnerabilitiesSlice';
import { getVulnerabilitiesUrl } from '../../../../main/frontend/util/CLMLocation';

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
});
