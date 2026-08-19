/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState } from 'MainRoot/componentDetails/ViolationsTableTile/policyViolationsSlice';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

describe('componentDetailspolicyViolationsSlice', () => {
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

  describe('componentDetailsPolicyViolations/toggleComponentWaiversPopover action', () => {
    it('toggles the value of showComponentWaiversPopover and reloadComponentWaivers', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        showComponentWaiversPopover: false,
        reloadComponentWaivers: false,
      });
      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/toggleComponentWaiversPopover',
      });
      expect(newState.showComponentWaiversPopover).toBe(true);
      expect(newState.reloadComponentWaivers).toBe(true);
      expect(newState.other).toBe(stateConstantObject);

      const newState2 = reducer(newState, {
        type: 'componentDetailsPolicyViolations/toggleComponentWaiversPopover',
      });
      expect(newState2.showComponentWaiversPopover).toBe(false);
      expect(newState2.reloadComponentWaivers).toBe(false);
      expect(newState2.other).toBe(stateConstantObject);
    });

    it('keeps showComponentWaiversPopover and showComponentWaiversPopover in sync', () => {
      let state, newState;

      // intentionally starting state out of sync
      state = Object.freeze({
        other: stateConstantObject,
        showComponentWaiversPopover: false,
        reloadComponentWaivers: true,
      });
      newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/toggleComponentWaiversPopover',
      });
      expect(newState.showComponentWaiversPopover).toBe(true);
      expect(newState.reloadComponentWaivers).toBe(true);

      // intentionally starting state out of sync
      state = Object.freeze({
        other: stateConstantObject,
        showComponentWaiversPopover: true,
        reloadComponentWaivers: false,
      });
      newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/toggleComponentWaiversPopover',
      });
      expect(newState.showComponentWaiversPopover).toBe(false);
      expect(newState.reloadComponentWaivers).toBe(false);
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

    it('sets the hasPermissionToAddWaivers to the permissionResult property in the payload', () => {
      let state = Object.freeze({
        other: stateConstantObject,
        hasPermissionToAddWaivers: false,
      });

      let payload = {
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
        permissionResult: true,
        hash: 'componentHash',
      };

      let newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/load/fulfilled',
        payload,
      });
      expect(newState.hasPermissionToAddWaivers).toBe(true);
      expect(newState.other).toBe(stateConstantObject);

      state = Object.freeze({
        other: stateConstantObject,
        hasPermissionToAddWaivers: true,
      });

      payload = {
        ...payload,
        permissionResult: false,
      };
      newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/load/fulfilled',
        payload,
      });
      expect(newState.hasPermissionToAddWaivers).toBe(false);
      expect(newState.other).toBe(stateConstantObject);
    });

    it('sets innerSourceTransitiveWaiver to the innerSourceTransitiveWaiver property in the payload', () => {
      let state = Object.freeze({
        other: stateConstantObject,
        innerSourceTransitiveWaiver: false,
      });

      let payload = {
        innerSourceTransitiveWaiver: true,
      };

      let newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/load/fulfilled',
        payload,
      });
      expect(newState.innerSourceTransitiveWaiver).toBe(true);
      expect(newState.other).toBe(stateConstantObject);

      state = Object.freeze({
        other: stateConstantObject,
        innerSourceTransitiveWaiver: true,
      });

      payload = {
        innerSourceTransitiveWaiver: false,
      };
      newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/load/fulfilled',
        payload,
      });
      expect(newState.innerSourceTransitiveWaiver).toBe(false);
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
        waiversResult: { waiversByOwner: [], expiredWaiversByOwner: [] },
        hash: 'componentHash',
      };

      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/load/fulfilled',
        payload,
      });

      expect(newState.violations).toEqual([
        { policyViolationId: 'violation1ForComponentHash', applicableWaivers: [], expiredWaivers: [] },
        { policyViolationId: 'violation2ForComponentHash', applicableWaivers: [], expiredWaivers: [] },
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
              ownerId: 'org1',
              ownerName: 'org1Name',
              ownerType: 'organization',
              waivers: [{ id: 'waiverForOrg1' }, { id: 'waiverForOrg2' }],
            },
          ],
          expiredWaiversByOwner: [],
        },
        hash: 'componentHash',
      };

      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/load/fulfilled',
        payload,
      });

      expect(newState.waivers).toEqual([
        {
          id: 'waiverForApp1',
          policyWaiverId: 'waiverForApp1',
          scopeOwnerId: 'app1',
          scopeOwnerType: 'application',
          scopeOwnerName: 'app1Name',
        },
        {
          id: 'waiverForApp2',
          policyWaiverId: 'waiverForApp2',
          scopeOwnerType: 'application',
          scopeOwnerId: 'app1',
          scopeOwnerName: 'app1Name',
        },
        {
          id: 'waiverForOrg1',
          policyWaiverId: 'waiverForOrg1',
          scopeOwnerId: 'org1',
          scopeOwnerType: 'organization',
          scopeOwnerName: 'org1Name',
        },
        {
          id: 'waiverForOrg2',
          policyWaiverId: 'waiverForOrg2',
          scopeOwnerId: 'org1',
          scopeOwnerType: 'organization',
          scopeOwnerName: 'org1Name',
        },
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
                {
                  policyViolationId: 'violation2ForComponentHash',
                  policyId: 'policyIdForViolation2',
                  constraintFactsJson: '{factsSerializedAsJson1}',
                },
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
          expiredWaiversByOwner: [
            {
              owner: 'org1',
              ownerName: 'org1Name',
              ownerType: 'organization',
              waivers: [
                {
                  id: 'waiverForOrg3',
                  policyId: 'policyIdForViolation2',
                  constraintFactsJson: '{factsSerializedAsJson1}',
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
          expiredWaivers: [],
        },
        {
          policyViolationId: 'violation2ForComponentHash',
          applicableWaivers: [],
          constraintFactsJson: '{factsSerializedAsJson1}',
          policyId: 'policyIdForViolation2',
          expiredWaivers: [
            {
              id: 'waiverForOrg3',
              policyId: 'policyIdForViolation2',
              constraintFactsJson: '{factsSerializedAsJson1}',
              policyWaiverId: 'waiverForOrg3',
              scopeOwnerId: undefined,
              scopeOwnerType: 'organization',
              scopeOwnerName: 'org1Name',
            },
          ],
        },
      ]);
    });
    it('enhances old report violations with their applicable waivers information', () => {
      const state = Object.freeze({
        other: stateConstantObject,
      });

      const payload = {
        violationsResult: {
          aaData: [
            {
              hash: 'componentHash',
              activeViolations: [
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
          expiredWaiversByOwner: [
            {
              ownerId: 'org1Id',
              owner: 'org1',
              ownerName: 'org1Name',
              ownerType: 'organization',
              waivers: [
                {
                  id: 'waiverForOrg3',
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
          expiredWaivers: [
            {
              id: 'waiverForOrg3',
              policyId: 'policyIdForViolation',
              constraintFactsJson: '{factsSerializedAsJson}',
              policyWaiverId: 'waiverForOrg3',
              scopeOwnerId: 'org1Id',
              scopeOwnerType: 'organization',
              scopeOwnerName: 'org1Name',
            },
          ],
        },
        {
          policyViolationId: 'violation2ForComponentHash',
          applicableWaivers: [],
          expiredWaivers: [],
        },
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

  describe('toggleShowViolationsDetailPopover', () => {
    it('toggles the showViolationsDetailPopover between true to false', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        showViolationsDetailPopover: true,
      });

      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/toggleShowViolationsDetailPopover',
      });
      expect(newState.showViolationsDetailPopover).toBe(false);
      expect(newState.other).toBe(stateConstantObject);
    });

    it('toggles the showViolationsDetailPopover between false to true', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        showViolationsDetailPopover: false,
      });

      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/toggleShowViolationsDetailPopover',
      });
      expect(newState.showViolationsDetailPopover).toBe(true);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('setSelectedPolicyViolationId', () => {
    it('sets the selectedPolicyViolationId to the received payload', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        selectedPolicyViolationId: null,
      });

      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/setSelectedPolicyViolationId',
        payload: 'oneViolationToRuleThemAll',
      });
      expect(newState.selectedPolicyViolationId).toBe('oneViolationToRuleThemAll');
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('setViolationType', () => {
    it('sets the violationType to the received payload', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        violationType: null,
      });

      const newState = reducer(state, {
        type: 'componentDetailsPolicyViolations/setViolationType',
        payload: 'test',
      });
      expect(newState.violationType).toBe('test');
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('SELECT_COMPONENT', () => {
    it('resets current state to initialState', () => {
      const state = Object.freeze({
        violations: {},
        waivers: [],
        loading: true,
        loadError: 'error',
        showComponentWaiversPopover: true,
        reloadComponentWaivers: true,
        showViolationsDetailPopover: true,
        hasPermissionToAddWaivers: true,
        innerSourceTransitiveWaiver: true,
        selectedPolicyViolationId: '42',
        violationType: 'TYPE',
      });

      const newState = reducer(state, { type: SELECT_COMPONENT });
      expect(newState).toEqual(initialState);
    });
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('resets popover variables on UI_ROUTER_ON_FINISH', () => {
      const state = Object.freeze({
        showViolationsDetailPopover: true,
        violationsDetailRowClicked: true,
      });

      const newState = reducer(state, { type: UI_ROUTER_ON_FINISH });
      expect(newState).toEqual({
        showViolationsDetailPopover: false,
        violationsDetailRowClicked: false,
      });
    });
  });
});
