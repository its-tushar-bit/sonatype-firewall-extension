/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { omit } from 'ramda';

import { actions } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';
import {
  getComponentLicensesUrl,
  getLicenseOverrideUrl,
  getLicensesWithSyntheticFilterUrl,
} from 'MainRoot/util/CLMLocation';

describe('componentDetailsLicenseDetectionsTileActions', () => {
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
      componentDetailsLicenseDetectionsTile: {
        selectedRefId: '2',
      },
    };
    store = SpecUtil.mockReduxStore(state);
  });

  describe('load', () => {
    const { load } = actions,
      componentIdentifier = JSON.stringify({ format: 'format', coordinates: 'coordinates' }),
      ownerType = 'application',
      ownerId = 'appPublicId',
      licenseUrl = getLicensesWithSyntheticFilterUrl(),
      componentlicensUrl = getComponentLicensesUrl({
        clientType: 'ci',
        ownerType,
        ownerId,
        componentIdentifier,
        identificationSource: 'identificationSource',
        scanId: 'currentScanId',
      }),
      licensesOverrideUrl = getLicenseOverrideUrl(ownerType, ownerId, componentIdentifier),
      licenses = [
        {
          id: '0BSD',
          longDisplayName: 'BSD Zero Clause License',
          shortDisplayName: '0BSD',
        },
        {
          id: '10tec-Company-License-Agreement',
          longDisplayName: '10tec Company License Agreement',
          shortDisplayName: '10tec-Company-License-Agreement',
        },
      ],
      componentLicenses = {
        declaredlicenses: [
          { license: { licenseId: 'CDDL-1.1', licenseName: 'CDDL-1.1' }, threatLevel: 2 },
          {
            license: {
              licenseId: 'GPL-2.0-with-classpath-exception',
              licenseName: 'GPL-2.0-with-classpath-exception',
            },
            threatLevel: 9,
          },
        ],
        observedlicenses: [
          { license: { licenseId: 'CDDL-1.1', licenseName: 'CDDL-1.1' }, threatLevel: 2 },
          {
            license: {
              licenseId: 'GPL-2.0-with-classpath-exception',
              licenseName: 'GPL-2.0-with-classpath-exception',
            },
            threatLevel: 9,
          },
        ],
        effectiveLicenses: [
          { license: { licenseId: 'CDDL-1.1', licenseName: 'CDDL-1.1' }, threatLevel: 2 },
          {
            license: {
              licenseId: 'GPL-2.0-with-classpath-exception',
              licenseName: 'GPL-2.0-with-classpath-exception',
            },
            threatLevel: 9,
          },
        ],
        selectableLicenses: [
          { licenseId: 'CDDL-1.1', licenseName: 'CDDL-1.1' },
          { licenseId: 'GPL-2.0-with-classpath-exception', licenseName: 'GPL-2.0-with-classpath-exception' },
        ],
      },
      licensesOverride = {
        licenseOverridesByOwner: [
          {
            ownerId: 'wencelapp2.0',
            ownerName: 'wencel app 2.0',
            ownerType: 'application',
            licenseOverride: null,
          },
          {
            ownerId: '5b862dfe2c95486f8395eca90c06dcfe',
            ownerName: 'wencel org',
            ownerType: 'organization',
            licenseOverride: {
              id: '953498a769834c1b8e0a34e54624a8c1',
              ownerId: '5b862dfe2c95486f8395eca90c06dcfe',
              status: 'OVERRIDDEN',
              comment: '',
              licenseIds: ['10tec-Company-License-Agreement', 'Adobe-AFM'],
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'jersey-client',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'com.sun.jersey',
                  version: '1.19.3',
                },
              },
            },
          },
          {
            ownerId: 'ROOT_ORGANIZATION_ID',
            ownerName: 'Root Organization',
            ownerType: 'organization',
            licenseOverride: null,
          },
        ],
      };

    it('immediately dispatches a componentDetailsLicenseDetectionsTile/load/pending action and appropriate requests', () => {
      mockAxiosCalls({
        get: {
          [licenseUrl]: Promise.resolve({}),
          [componentlicensUrl]: Promise.resolve({}),
          [licensesOverrideUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(load());

      const actions = store.getActions();
      expect(actions).toHaveAction({
        type: 'componentDetailsLicenseDetectionsTile/load/pending',
      });
      expect(axios.get).toHaveBeenCalledTimes(3);
      expect(axios.get).toHaveBeenCalledWith('/rest/license?filterSynthetic=true');
      expect(axios.get).toHaveBeenCalledWith(
        '/rest/ci/componentDetails/application/appPublicId/licenses?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D&identificationSource=identificationSource&scanId=currentScanId'
      );
      expect(axios.get).toHaveBeenCalledWith(
        '/rest/licenseOverride/application/appPublicId?componentIdentifier={"format":"format","coordinates":"coordinates"}'
      );
    });

    it('dispatches a componentDetailsLicenseDetectionsTile/load/fulfilled action after successful requests', (done) => {
      mockAxiosCalls({
        get: {
          [licenseUrl]: Promise.resolve({
            data: licenses,
          }),
          [componentlicensUrl]: Promise.resolve({
            data: componentLicenses,
          }),
          [licensesOverrideUrl]: Promise.resolve({
            data: licensesOverride,
          }),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsLicenseDetectionsTile/load/pending',
      };
      const expectedFulfilledAction = {
        type: 'componentDetailsLicenseDetectionsTile/load/fulfilled',
        payload: {
          licenseOverride: licensesOverride.licenseOverridesByOwner,
          declaredlicenses: componentLicenses.declaredlicenses,
          effectiveLicenses: componentLicenses.effectiveLicenses,
          observedlicenses: componentLicenses.observedlicenses,
          selectableLicenses: componentLicenses.selectableLicenses,
          allLicenses: licenses,
        },
      };
      store.dispatch(load()).then(() => {
        // Remove metadata and custom error information from redux toolkit before comparisons
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFulfilledAction]);
        done();
      });
    });

    describe('dispatches a componentDetailsLicenseDetectionsTile/load/rejected action after an error occurs in any of the requests', () => {
      it('licenseUrl fails', (done) => {
        mockAxiosCalls({
          get: {
            [licenseUrl]: () => Promise.reject('errorMessage'),
            [componentlicensUrl]: Promise.resolve({}),
            [licensesOverrideUrl]: Promise.resolve({}),
          },
        });

        const expectedPendingAction = {
          type: 'componentDetailsLicenseDetectionsTile/load/pending',
        };
        const expectedFailedAction = {
          type: 'componentDetailsLicenseDetectionsTile/load/rejected',
          payload: 'errorMessage',
        };

        store.dispatch(load()).then(() => {
          // Remove metadata and custom error information from redux toolkit before comparisons
          const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
          expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFailedAction]);
          done();
        });
      });

      it('componentlicensUrl fails', (done) => {
        mockAxiosCalls({
          get: {
            [licenseUrl]: Promise.resolve({}),
            [componentlicensUrl]: () => Promise.reject('errorMessage'),
            [licensesOverrideUrl]: Promise.resolve({}),
          },
        });

        const expectedPendingAction = {
          type: 'componentDetailsLicenseDetectionsTile/load/pending',
        };
        const expectedFailedAction = {
          type: 'componentDetailsLicenseDetectionsTile/load/rejected',
          payload: 'errorMessage',
        };

        store.dispatch(load()).then(() => {
          // Remove metadata and custom error information from redux toolkit before comparisons
          const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
          expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFailedAction]);
          done();
        });
      });

      it('licensesOverrideUrl fails', (done) => {
        mockAxiosCalls({
          get: {
            [licenseUrl]: Promise.resolve({}),
            [componentlicensUrl]: Promise.resolve({}),
            [licensesOverrideUrl]: () => Promise.reject('errorMessage'),
          },
        });

        const expectedPendingAction = {
          type: 'componentDetailsLicenseDetectionsTile/load/pending',
        };
        const expectedFailedAction = {
          type: 'componentDetailsLicenseDetectionsTile/load/rejected',
          payload: 'errorMessage',
        };

        store.dispatch(load()).then(() => {
          // Remove metadata and custom error information from redux toolkit before comparisons
          const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
          expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFailedAction]);
          done();
        });
      });
    });
  });
});
