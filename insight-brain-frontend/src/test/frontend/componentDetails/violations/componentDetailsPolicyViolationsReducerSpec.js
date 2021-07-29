/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../../main/frontend/componentDetails/violations/PolicyViolationsRedux';

describe('componentDetailsPolicyViolationsReducer', () => {
  const stateConstantObject = { value: 'test value' };

  describe('unknown action', () => {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('componentDetailsPolicyViolations/setShowViolationsDetail action', () => {
    it('sets the showViolationsDetail flag a value', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        showViolationsDetail: false,
      });

      let newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/setShowViolationsDetail',
        payload: true,
      });

      expect(newState.showViolationsDetail).toBe(true);
      expect(newState.other).toBe(stateConstantObject);

      newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/setShowViolationsDetail',
        payload: false,
      });

      expect(newState.showViolationsDetail).toBe(false);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('componentDetailsPolicyViolations/load/pending action', () => {
    it('sets the loading flag to true', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        loading: false,
      });

      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/load/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('componentDetailsPolicyViolations/load/fulfilled action', () => {
    it('sets loading flag to false and unsets the loadError', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        loading: true,
        loadError: 'loadError',
      });

      const payload = {
        violationsResult: {
          aaData: [
            {
              hash: 'componentHash',
              allViolations: [
                { policyViolationId: 'violation1ForComponentHash' },
                { policyViolationId: 'violation2ForComponentHash' },
              ],
            },
          ],
        },
        hash: 'componentHash',
      };

      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/load/fulfilled',
        payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.other).toBe(stateConstantObject);
    });

    it('extracts the violations information only for the specific component hash in the payload', () => {
      const state = Object.freeze({
        other: stateConstantObject,
      });

      const payload = {
        violationsResult: {
          aaData: [
            { hash: 'this is not the hash you are looking for' },
            {
              hash: 'componentHash',
              allViolations: [
                { policyViolationId: 'violation1ForComponentHash' },
                { policyViolationId: 'violation2ForComponentHash' },
              ],
            },
            { hash: 'neither is this one' },
          ],
        },
        waiversResult: { waiversByOwner: [] },
        hash: 'componentHash',
      };

      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/load/fulfilled',
        payload,
      });

      expect(newState.violations).toEqual([
        { policyViolationId: 'violation1ForComponentHash', applicableWaivers: [] },
        { policyViolationId: 'violation2ForComponentHash', applicableWaivers: [] },
      ]);
    });

    it('flattens the waiver information into a single array adding the owner information per item', () => {
      const state = Object.freeze({
        other: stateConstantObject,
      });

      const payload = {
        violationsResult: {
          aaData: [{ hash: 'componentHash', allViolations: [] }],
        },
        waiversResult: {
          waiversByOwner: [
            {
              ownerId: 'app1',
              ownerName: 'app1Name',
              ownerType: 'application',
              waivers: [{ id: 'waiverForApp1' }, { id: 'waiverForApp2' }],
            },
            {
              owner: 'org1',
              ownerName: 'org1Name',
              ownerType: 'organization',
              waivers: [{ id: 'waiverForOrg1' }, { id: 'waiverForOrg2' }],
            },
          ],
        },
        hash: 'componentHash',
      };

      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/load/fulfilled',
        payload,
      });

      expect(newState.waivers).toEqual([
        { id: 'waiverForApp1', type: 'application', ownerName: 'app1Name' },
        { id: 'waiverForApp2', type: 'application', ownerName: 'app1Name' },
        { id: 'waiverForOrg1', type: 'organization', ownerName: 'org1Name' },
        { id: 'waiverForOrg2', type: 'organization', ownerName: 'org1Name' },
      ]);
    });

    it('enhances violations with their applicable waivers information', () => {
      const state = Object.freeze({
        other: stateConstantObject,
      });

      const payload = {
        violationsResult: {
          aaData: [
            {
              hash: 'componentHash',
              allViolations: [
                {
                  policyViolationId: 'violation1ForComponentHash',
                  policyId: 'policyIdForViolation',
                  constraintFactsJson: '{factsSerializedAsJson}',
                },
                { policyViolationId: 'violation2ForComponentHash' },
              ],
            },
          ],
        },
        waiversResult: {
          waiversByOwner: [
            {
              owner: 'org1',
              ownerName: 'org1Name',
              ownerType: 'organization',
              waivers: [
                { id: 'waiverForOrg1' },
                {
                  id: 'waiverForOrg2',
                  policyId: 'policyIdForViolation',
                  constraintFactsJson: '{factsSerializedAsJson}',
                },
              ],
            },
          ],
        },
        hash: 'componentHash',
      };

      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/load/fulfilled',
        payload,
      });

      expect(newState.violations).toEqual([
        {
          policyViolationId: 'violation1ForComponentHash',
          policyId: 'policyIdForViolation',
          constraintFactsJson: '{factsSerializedAsJson}',
          applicableWaivers: ['waiverForOrg2'],
        },
        { policyViolationId: 'violation2ForComponentHash', applicableWaivers: [] },
      ]);
    });
  });

  describe('componentDetailsPolicyViolations/load/rejected action', () => {
    it('sets the loadError to the payload and the loading flag to true', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        loading: true,
        loadError: null,
      });

      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/load/rejected',
        payload: 'loadError',
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe('loadError');
      expect(newState.other).toBe(stateConstantObject);
    });
  });
});
