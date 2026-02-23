/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit } from 'ramda';

import { actions } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';
import {
  getBaseLicenseOverrideUrl,
  getComponentMultiLicensesUrl,
  getDeleteLicenseOverrideUrl,
  getLicenseOverrideUrl,
  getLicensesWithSyntheticFilterUrl,
} from 'MainRoot/util/CLMLocation';
import * as licenseDetectionTileSelectors from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSelectors';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

describe('componentDetailsLicenseDetectionsTileActions', () => {
  let store, state, mock;
  beforeAll(() => {
    mock = axiosMockAdapter();
  });

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
          allEntries: [
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
      componentMultiLicensesUrl = getComponentMultiLicensesUrl({
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

    beforeEach(() => {
      mock.onGet(licenseUrl).reply(200, {});
      mock.onGet(componentMultiLicensesUrl).reply(200, {});
      mock.onGet(licensesOverrideUrl).reply(200, {});
    });

    it('immediately dispatches a componentDetailsLicenseDetectionsTile/load/pending action and appropriate requests', (done) => {
      store.dispatch(load()).then(() => {
        expect(mock.history.get.length).toBe(3);
        expect(mock.history.get[0].url).toBe('/rest/license?filterSynthetic=true');
        expect(mock.history.get[1].url).toBe(
          '/rest/ci/componentDetails/application/appPublicId/multiLicenses?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D&identificationSource=identificationSource&scanId=currentScanId'
        );
        expect(mock.history.get[2].url).toBe(
          '/api/v2/licenseOverrides/application/appPublicId?componentIdentifier=%7B%22format%22%3A%22format%22%2C%22coordinates%22%3A%22coordinates%22%7D'
        );
        done();
      });

      const actions = store.getActions();
      expect(actions).toHaveAction({
        type: 'componentDetailsLicenseDetectionsTile/load/pending',
      });
    });

    it('dispatches a componentDetailsLicenseDetectionsTile/load/fulfilled action after successful requests', (done) => {
      mock.onGet(licenseUrl).reply(200, licenses);
      mock.onGet(componentMultiLicensesUrl).reply(200, componentLicenses);
      mock.onGet(licensesOverrideUrl).reply(200, licensesOverride);

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
          declaredLicenses: componentLicenses.declaredLicenses,
          effectiveLicenses: componentLicenses.effectiveLicenses,
          observedLicenses: componentLicenses.observedLicenses,
          selectableLicenses: componentLicenses.selectableLicenses,
          allLicenses: transformedAllLicenses,
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
      // Note in all three tests below we don't need to mock all three requests,
      // we override only the request in question and the rest remain with default mocking behavior
      it('licenseUrl fails', (done) => {
        // To mock response with the error message in the body use function returning rejected promise
        mock.onGet(licenseUrl).reply(() => Promise.reject('errorMessage'));

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

      it('componentMultiLicensesUrl fails', (done) => {
        mock.onGet(componentMultiLicensesUrl).reply(() => Promise.reject('errorMessage'));

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
        mock.onGet(licensesOverrideUrl).reply(() => Promise.reject('errorMessage'));

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
      jest.spyOn(applicationReportSelectors, 'selectSelectedComponent').mockReturnValue(selectedComponent);
      state = {
        router: {
          currentParams: {
            publicId: 'appPublicId',
            scanId: 'currentScanId',
            hash: 'currentComponentHash',
          },
          currentState: {
            name: 'applicationReport.componentDetails.legal',
            url: '/legal',
            data: {},
          },
        },
      };
      store = SpecUtil.mockReduxStore(state);
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('sends a post request to the BaseLicenseOverrideUrl with a payload using default licenseIds', (done) => {
      jest.spyOn(licenseDetectionTileSelectors, 'selectEditLicensesForm').mockReturnValue(editLicenseForm);

      // example of matching request data with asymmetric matcher
      mock
        .onPost(getBaseLicenseOverrideUrl(ownerType, ownerId), {
          asymmetricMatch: function (actual) {
            return actual.licenseIds.length === 0;
          },
        })
        .reply(200);

      store.dispatch(saveEditLicensesForm()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(mock.history.post.length).toBe(1);
        expect(mock.history.post[0].url).toBe('/api/v2/licenseOverrides/ownerType/ownerId');
        expect(mock.history.post[0].data).toEqual(
          JSON.stringify({
            id: null,
            licenseIds: [],
            componentIdentifier: Object({ format: 'some format' }),
            status: 'status',
            comment: 'some comment',
            ownerId: 'ownerId',
          })
        );

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
        jest.spyOn(licenseDetectionTileSelectors, 'selectEditLicensesForm').mockReturnValue({
          ...editLicenseForm,
          status: testStatus,
        });

        mock.onPost(getBaseLicenseOverrideUrl(ownerType, ownerId)).reply(200);

        store.dispatch(saveEditLicensesForm()).then(() => {
          jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          expect(mock.history.post.length).toBe(1);
          expect(mock.history.post[0].url).toBe('/api/v2/licenseOverrides/ownerType/ownerId');
          expect(mock.history.post[0].data).toEqual(
            JSON.stringify({
              id: null,
              licenseIds: ['apache 2.0'],
              componentIdentifier: Object({ format: 'some format' }),
              status: testStatus,
              comment: 'some comment',
              ownerId: 'ownerId',
            })
          );

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
      state = {
        router: {
          currentParams: {
            publicId: 'appPublicId',
            scanId: 'currentScanId',
            hash: 'currentComponentHash',
          },
          currentState: {
            name: 'applicationReport.componentDetails.legal',
            url: '/legal',
            data: {},
          },
        },
      };
      store = SpecUtil.mockReduxStore(state);
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('does not send a delete request to the DeleteLicenseOverrideUrl', (done) => {
      jest.spyOn(licenseDetectionTileSelectors, 'selectEditLicensesForm').mockReturnValue(editLicenseForm);

      mock.onDelete(getDeleteLicenseOverrideUrl(ownerType, ownerId, licenseOverrideId)).reply(200);

      store.dispatch(deleteLicenseOverride()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(mock.history.delete.length).toBe(0);

        const actions = store.getActions();
        expect(actions.length).toBe(5);
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/deleteLicenseOverride/fulfilled');
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/resetSubmitMaskState');
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/load/pending');

        done();
      });
    });

    it('sends a delete request to the DeleteLicenseOverrideUrl when licenseOverride exist', (done) => {
      jest.spyOn(licenseDetectionTileSelectors, 'selectEditLicensesForm').mockReturnValue({
        ...editLicenseForm,
        scope: { ...editLicenseForm.scope, licenseOverride: { id: licenseOverrideId } },
      });

      mock.onDelete(getDeleteLicenseOverrideUrl(ownerType, ownerId, licenseOverrideId)).reply(200);

      store.dispatch(deleteLicenseOverride()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(mock.history.delete.length).toBe(1);
        expect(mock.history.delete[0].url).toBe('/api/v2/licenseOverrides/ownerType/ownerId/licenseOverrideId');

        const actions = store.getActions();
        expect(actions.length).toBe(5);
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/deleteLicenseOverride/fulfilled');
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/resetSubmitMaskState');
        expect(actions).toHaveActionType('componentDetailsLicenseDetectionsTile/load/pending');

        done();
      });
    });
  });
});
