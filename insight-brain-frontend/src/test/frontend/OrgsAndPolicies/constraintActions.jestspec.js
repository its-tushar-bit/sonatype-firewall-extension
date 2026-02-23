/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/OrgsAndPolicies/constraintSlice';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { getConditionTypeUrl, getConditionValueTypeUrl } from 'MainRoot/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('constraintActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(() => {
    state = {
      router: {
        currentParams: {
          applicationPublicId: 'ownerId',
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
  });

  describe('loadConstraint', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
        applicationPublicId: 'ownerId',
      });
    });

    it('load condition types and values successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getConditionTypeUrl()]: Promise.resolve({
            data: [
              {
                enabled: true,
                valueTypeId: 'AgeInDaysValueType',
                valueHint: 'Enter term',
                autoUnquarantineSupported: false,
                threatCategory: 'QUALITY',
                name: 'Age',
                id: 'AgeInDays',
                valueTypes: {},
              },
              {
                autoUnquarantineSupported: false,
                enabled: false,
                id: 'SecurityVulnerabilitySource',
                name: 'Vulnerability Source',
                supportedOperators: ['is', 'is not'],
                threatCategory: 'SECURITY',
                valueHint: null,
                valueTypeId: 'SecurityVulnerabilitySourceValueType',
              },
            ],
          }),
          [getConditionValueTypeUrl('application', 'ownerId')]: Promise.resolve({
            data: [
              {
                allowMultiple: false,
                availableValues: null,
                dataType: 'Integer',
                id: 'AgeInDaysValueType',
              },
            ],
          }),
        },
      });

      store.dispatch(actions.loadConstraint({ isNewPolicy: false, constraints: [] })).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);
        expect(axios.get).toHaveBeenCalledWith('/rest/policy/conditionType');
        expect(axios.get).toHaveBeenCalledWith('/rest/conditionValueType/application/ownerId');

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'constraint/loadConstraint/pending',
          'constraint/loadConstraint/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          conditionTypes: [
            {
              enabled: true,
              valueTypeId: 'AgeInDaysValueType',
              valueHint: 'Enter term',
              autoUnquarantineSupported: false,
              threatCategory: 'QUALITY',
              name: 'Age',
              id: 'AgeInDays',
              valueTypes: {},
              valueType: {
                allowMultiple: false,
                availableValues: null,
                dataType: 'Integer',
                id: 'AgeInDaysValueType',
              },
            },
          ],
          conditionTypesMap: {
            AgeInDays: {
              enabled: true,
              valueTypeId: 'AgeInDaysValueType',
              valueHint: 'Enter term',
              autoUnquarantineSupported: false,
              threatCategory: 'QUALITY',
              name: 'Age',
              id: 'AgeInDays',
              valueTypes: {},
              valueType: {
                allowMultiple: false,
                availableValues: null,
                dataType: 'Integer',
                id: 'AgeInDaysValueType',
              },
            },
            SecurityVulnerabilitySource: {
              autoUnquarantineSupported: false,
              enabled: false,
              id: 'SecurityVulnerabilitySource',
              name: 'Vulnerability Source',
              supportedOperators: ['is', 'is not'],
              threatCategory: 'SECURITY',
              valueHint: null,
              valueTypeId: 'SecurityVulnerabilitySourceValueType',
              valueType: undefined,
            },
          },
          editConstraintMap: {},
        });

        done();
      });
    });

    it('dispatches rejected action if load request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getConditionTypeUrl()]: () => Promise.reject('could not load condition types'),
          [getConditionValueTypeUrl('application', 'ownerId')]: Promise.resolve({
            data: [
              {
                allowMultiple: false,
                availableValues: null,
                dataType: 'Integer',
                id: 'AgeInDaysValueType',
              },
            ],
          }),
        },
      });

      store.dispatch(actions.loadConstraint({ isNewPolicy: false, constraints: [] })).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);
        expect(axios.get).toHaveBeenCalledWith('/rest/policy/conditionType');
        expect(axios.get).toHaveBeenCalledWith('/rest/conditionValueType/application/ownerId');

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'constraint/loadConstraint/pending',
          'constraint/loadConstraint/rejected',
        ]);
        expect(actions[1].payload).toBe('could not load condition types');

        done();
      });
    });
  });
});
