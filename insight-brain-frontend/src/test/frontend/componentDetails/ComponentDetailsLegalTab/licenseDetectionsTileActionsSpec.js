/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { omit } from 'ramda';

import {
  actions,
  fetchAdvanceLegalPackFeatures,
} from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';
import {
  getBaseLicenseOverrideUrl,
  getComponentLicensesUrl,
  getDeleteLicenseOverrideUrl,
  getLicenseLegalComponentUrl,
  getLicenseOverrideUrl,
  getLicensesWithSyntheticFilterUrl,
  getProductFeaturesUrl,
} from 'MainRoot/util/CLMLocation';
import * as licenseDetectionTileSelectors from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSelectors';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

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
      licenseLegalComponentUrl = getLicenseLegalComponentUrl(ownerType, ownerId, 'currentComponentHash'),
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
        declaredLicenses: [
          { license: { licenseId: 'CDDL-1.1', licenseName: 'CDDL-1.1' }, threatLevel: 2 },
          {
            license: {
              licenseId: 'GPL-2.0-with-classpath-exception',
              licenseName: 'GPL-2.0-with-classpath-exception',
            },
            threatLevel: 9,
          },
        ],
        observedLicenses: [
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
          [licenseLegalComponentUrl]: Promise.resolve({}),
        },
      });

      store.dispatch(load());

      const actions = store.getActions();
      expect(actions).toHaveAction({
        type: 'componentDetailsLicenseDetectionsTile/load/pending',
      });
      expect(axios.get).toHaveBeenCalledTimes(4);
      expect(axios.get).toHaveBeenCalledWith('/rest/license?filterSynthetic=true');
      expect(axios.get).toHaveBeenCalledWith(
        '/rest/ci/componentDetails/application/appPublicId/licenses?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D&identificationSource=identificationSource&scanId=currentScanId'
      );
      expect(axios.get).toHaveBeenCalledWith(
        '/rest/licenseOverride/application/appPublicId?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D'
      );
      expect(axios.get).toHaveBeenCalledWith(
        '/api/v2/licenseLegalMetadata/application/appPublicId/component?hash=currentComponentHash'
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
          [licenseLegalComponentUrl]: Promise.resolve({
            data: {
              component: {
                licenseLegalData: {
                  declaredLicenses: 'declaredLicenses',
                  effectiveLicenses: 'effectiveLicenses',
                  observedLicenses: 'observedLicenses',
                },
              },
              licenseLegalMetadata: 'licenseLegalMetadata',
            },
          }),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsLicenseDetectionsTile/load/pending',
      };

      const transformedAllLicenses = [
        {
          id: '0BSD',
          displayName: '0BSD',
        },
        {
          id: '10tec-Company-License-Agreement',
          displayName: '10tec-Company-License-Agreement',
        },
      ];

      const expectedFulfilledAction = {
        type: 'componentDetailsLicenseDetectionsTile/load/fulfilled',
        payload: {
          licenseOverride: licensesOverride.licenseOverridesByOwner,
          declaredLicenses: 'declaredLicenses',
          effectiveLicenses: 'effectiveLicenses',
          observedLicenses: 'observedLicenses',
          selectableLicenses: componentLicenses.selectableLicenses,
          allLicenses: transformedAllLicenses,
          licenseLegalMetadata: 'licenseLegalMetadata',
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

  describe('saveEditLicensesForm', () => {
    const { saveEditLicensesForm } = actions,
      ownerType = 'ownerType',
      ownerId = 'ownerId',
      editLicenseForm = {
        status: 'status',
        comment: { value: 'some comment' },
        scope: { ownerType, ownerId },
        licenseIds: ['apache 2.0'],
      },
      selectedComponent = {
        componentIdentifier: { format: 'some format' },
      };

    beforeEach(() => {
      spyOn(applicationReportSelectors, 'selectSelectedComponent').and.returnValue(selectedComponent);
      store = SpecUtil.mockReduxStore({});
      jasmine.clock().install();
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('sends a post request to the BaseLicenseOverrideUrl with a payload using default licenseIds', (done) => {
      spyOn(licenseDetectionTileSelectors, 'selectEditLicensesForm').and.returnValue(editLicenseForm);
      mockAxiosCalls({
        post: {
          [getBaseLicenseOverrideUrl(ownerType, ownerId)]: Promise.resolve(),
        },
      });

      store.dispatch(saveEditLicensesForm()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(axios.post).toHaveBeenCalledTimes(1);
        expect(axios.post).toHaveBeenCalledWith('/rest/licenseOverride/ownerType/ownerId', {
          id: null,
          licenseIds: [],
          componentIdentifier: Object({ format: 'some format' }),
          status: 'status',
          comment: 'some comment',
          ownerId: 'ownerId',
        });

        const actions = store.getActions();
        expect(actions.length).toBe(5);
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/saveEditLicensesForm/fulfilled');
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/resetSubmitMaskState');
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/load/pending');

        done();
      });
    });

    ['SELECTED', 'OVERRIDDEN'].forEach((testStatus) => {
      it(`sends a post request to the BaseLicenseOverrideUrl with a payload and form"s licenseIds if status is ${testStatus}`, (done) => {
        spyOn(licenseDetectionTileSelectors, 'selectEditLicensesForm').and.returnValue({
          ...editLicenseForm,
          status: testStatus,
        });
        mockAxiosCalls({
          post: {
            [getBaseLicenseOverrideUrl(ownerType, ownerId)]: Promise.resolve(),
          },
        });

        store.dispatch(saveEditLicensesForm()).then(() => {
          jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          expect(axios.post).toHaveBeenCalledTimes(1);
          expect(axios.post).toHaveBeenCalledWith('/rest/licenseOverride/ownerType/ownerId', {
            id: null,
            licenseIds: ['apache 2.0'],
            componentIdentifier: Object({ format: 'some format' }),
            status: testStatus,
            comment: 'some comment',
            ownerId: 'ownerId',
          });

          const actions = store.getActions();
          expect(actions.length).toBe(5);
          expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/saveEditLicensesForm/fulfilled');
          expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/resetSubmitMaskState');
          expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/load/pending');

          done();
        });
      });
    });
  });

  describe('deleteLicenseOverride', () => {
    const { deleteLicenseOverride } = actions,
      ownerType = 'ownerType',
      ownerId = 'ownerId',
      licenseOverrideId = 'licenseOverrideId',
      editLicenseForm = {
        status: 'status',
        comment: { value: 'some comment' },
        scope: { ownerType, ownerId },
      };

    beforeEach(() => {
      store = SpecUtil.mockReduxStore({});
      jasmine.clock().install();
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('does not send a delete request to the DeleteLicenseOverrideUrl', (done) => {
      spyOn(licenseDetectionTileSelectors, 'selectEditLicensesForm').and.returnValue(editLicenseForm);
      mockAxiosCalls({
        del: {
          [getDeleteLicenseOverrideUrl(ownerType, ownerId, licenseOverrideId)]: Promise.resolve(),
        },
      });

      store.dispatch(deleteLicenseOverride()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(axios.delete).toHaveBeenCalledTimes(0);

        const actions = store.getActions();
        expect(actions.length).toBe(5);
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/deleteLicenseOverride/fulfilled');
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/resetSubmitMaskState');
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/load/pending');

        done();
      });
    });

    it('sends a delete request to the DeleteLicenseOverrideUrl when licenseOverride exist', (done) => {
      spyOn(licenseDetectionTileSelectors, 'selectEditLicensesForm').and.returnValue({
        ...editLicenseForm,
        scope: { ...editLicenseForm.scope, licenseOverride: { id: licenseOverrideId } },
      });
      mockAxiosCalls({
        del: {
          [getDeleteLicenseOverrideUrl(ownerType, ownerId, licenseOverrideId)]: Promise.resolve(),
        },
      });

      store.dispatch(deleteLicenseOverride()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete).toHaveBeenCalledWith('/rest/licenseOverride/ownerType/ownerId/licenseOverrideId');

        const actions = store.getActions();
        expect(actions.length).toBe(5);
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/deleteLicenseOverride/fulfilled');
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/resetSubmitMaskState');
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/load/pending');

        done();
      });
    });
  });

  describe('fetchAdvanceLegalPackFeatures', () => {
    let store;
    beforeEach(() => {
      const editWebhookState = {
        reviewObligationsButtonIsVisible: false,
      };

      store = SpecUtil.mockReduxStore({ webhooks: editWebhookState });
    });

    it('retrieves from backend features and found that Advance Legal Pack feature is enabled', () => {
      mockAxiosCalls({
        get: {
          [getProductFeaturesUrl()]: () => Promise.resolve({ data: ['advanced-legal-pack'] }),
        },
      });

      store.dispatch(fetchAdvanceLegalPackFeatures()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(store.getActions()[0]).toEqual({
          type: 'componentDetailsLicenseDetectionsTile/fetchAdvanceLegalPackFeatures/fulfilled',
          payload: true,
        });
      });
    });

    it('retrieves from backend features and found that Advance Legal Pack feature is NOT enabled', () => {
      mockAxiosCalls({
        get: {
          [getProductFeaturesUrl()]: () => Promise.resolve({ data: [] }),
        },
      });

      store.dispatch(fetchAdvanceLegalPackFeatures()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(store.getActions()[0]).toEqual({
          type: 'componentDetailsLicenseDetectionsTile/fetchAdvanceLegalPackFeatures/fulfilled',
          payload: false,
        });
      });
    });
  });
});
