/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/dashboard/results/componentRisk/componentRiskSlice';

describe('Componet Risk reducer', () => {
  describe('componentRiskDetails/loadDetails/fulfilled', () => {
    it('sets loadingDetails,component and componentName property', () => {
      const state = Object.freeze({
        component: { displayName: '' },
        componentName: '',
      });

      const payload = {
        parts: [
          {
            field: 'Name',
            value: 'testName',
          },
          {
            value: ' ',
          },
          {
            field: 'Version',
            value: 'testVersion',
          },
        ],
        name: 'org.webjars angularjs',
      };

      const { component, componentName } = reducer(state, {
        type: 'componentRiskDetails/loadDetails/fulfilled',
        payload,
      });

      expect(component).toEqual({ displayName: payload });
      expect(componentName).toEqual('testName testVersion');
    });
  });

  describe('componentRiskDetails/loadAppComponents/fulfilled', () => {
    it('sets totalRisk, loadingAppComponents and applicationComponents properties', () => {
      const payload = [
        {
          policyViolations: [
            {
              threatLevel: 1,
            },
            {
              threatLevel: 1,
            },
            {
              threatLevel: 1,
            },
          ],
        },
        {
          policyViolations: [
            {
              threatLevel: 2,
            },
            {
              threatLevel: 2,
            },
            {
              threatLevel: 2,
            },
          ],
        },
        {
          policyViolations: [
            {
              threatLevel: 3,
            },
            {
              threatLevel: 3,
            },
            {
              threatLevel: 3,
            },
          ],
        },
      ];
      const state = Object.freeze({
        applicationComponents: [],
        totalRisk: 0,
      });

      const { totalRisk, applicationComponents } = reducer(state, {
        type: 'componentRiskDetails/loadAppComponents/fulfilled',
        payload,
      });

      const componentsExpected = [
        { ...payload[0], risk: 3 },
        { ...payload[1], risk: 6 },
        { ...payload[2], risk: 9 },
      ];
      expect(totalRisk).toEqual(18);
      expect(applicationComponents).toEqual(componentsExpected);
    });
  });

  describe('componentRiskDetails/loadDetailsAndComponents/pending', () => {
    it('sets loading and clear loadError properties', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'componentRiskDetails/loadDetailsAndComponents/pending',
      });

      expect(loading).toEqual(true);
      expect(loadError).toEqual(null);
    });
  });

  describe('componentRiskDetails/loadDetailsAndComponents/fulfilled', () => {
    it('clear loading and loadError properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'componentRiskDetails/loadDetailsAndComponents/fulfilled',
      });

      expect(loading).toEqual(false);
      expect(loadError).toEqual('error');
    });
  });

  describe('componentRiskDetails/loadDetailsAndComponents/rejected', () => {
    it('clear loading and set loadError properties', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const payload = 'Error loadDetailsAndComponents';

      const { loading, loadError } = reducer(state, {
        type: 'componentRiskDetails/loadDetailsAndComponents/rejected',
        payload,
      });

      expect(loading).toEqual(false);
      expect(loadError).toEqual('Error loadDetailsAndComponents');
    });
  });
});
