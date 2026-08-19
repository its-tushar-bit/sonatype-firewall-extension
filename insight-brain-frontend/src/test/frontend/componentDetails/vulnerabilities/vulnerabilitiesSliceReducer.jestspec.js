/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState } from 'MainRoot/componentDetails/VulnerabilitiesTableTile/vulnerabilitiesSlice';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('componentDetailsVulnerabilitiesSlice', () => {
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

  describe('componentDetailsVulnerabilities/loadVulnerabilityDetails/pending action', () => {
    it('sets the loading flag to true in both vulnerabilityDetails and vulnerabilitySecurityOverride', () => {
      const state = Object.freeze({
        vulnerabilityDetails: {
          loading: false,
        },
        vulnerabilitySecurityOverride: {
          loading: false,
        },
      });

      const { vulnerabilityDetails, vulnerabilitySecurityOverride } = reducer(state, {
        type: 'componentDetailsVulnerabilities/loadVulnerabilityDetails/pending',
      });

      expect(vulnerabilityDetails.loading).toBe(true);
      expect(vulnerabilitySecurityOverride.loading).toBe(true);
    });
  });

  describe('componentDetailsVulnerabilities/loadVulnerabilityDetails/fulfilled action', () => {
    it(
      'sets loading flag to false, unsets the error and fills in the details and override information, ' +
        'translating the status to a server accepted value',
      () => {
        const commentsInputState = initUserInput('');

        const state = Object.freeze({
          vulnerabilityDetails: {
            details: null,
            loading: true,
            error: 'error',
          },
          vulnerabilities: {
            data: [
              {
                refId: '1',
                severity: 8,
                status: 'status 1',
              },
              {
                refId: '2',
                severity: 9.2,
                status: 'Not Applicable',
              },
            ],
          },
          selectedRefId: '2',
          vulnerabilitySecurityOverride: {
            status: '',
            comments: { ...commentsInputState },
            loading: true,
            loadError: '',
            saveError: 'save error',
          },
        });

        const payload = {
          identifier: 'CVE-2014-3625',
          description: 'Directory traversal vulnerability',
          categories: ['data', 'operational'],
          hasEditIqPermission: true,
        };

        const { vulnerabilityDetails, vulnerabilitySecurityOverride, hasEditIqPermission } = reducer(state, {
          type: 'componentDetailsVulnerabilities/loadVulnerabilityDetails/fulfilled',
          payload,
        });

        expect(vulnerabilityDetails.details).toEqual({
          identifier: 'CVE-2014-3625',
          description: 'Directory traversal vulnerability',
          categories: ['data', 'operational'],
          hasEditIqPermission: true,
        });
        expect(vulnerabilityDetails.loading).toBe(false);
        expect(vulnerabilityDetails.error).toBe(null);
        expect(hasEditIqPermission).toBe(true);

        expect(vulnerabilitySecurityOverride).toEqual({
          status: 'NOT_APPLICABLE',
          comments: { ...commentsInputState },
          loading: false,
          loadError: null,
          saveError: null,
        });
      }
    );

    it('toggles the showCommentField if translated status is `OPEN`', () => {
      const state = Object.freeze({
        vulnerabilityDetails: {
          details: null,
          loading: true,
          error: 'error',
        },
        vulnerabilities: {
          data: [
            {
              refId: '1',
              severity: 8,
              status: 'status 1',
            },
            {
              refId: '2',
              severity: 9.2,
              status: 'Open',
            },
          ],
        },
        selectedRefId: '2',
        vulnerabilitySecurityOverride: {
          showCommentField: true,
        },
      });

      const { vulnerabilitySecurityOverride } = reducer(state, {
        type: 'componentDetailsVulnerabilities/loadVulnerabilityDetails/fulfilled',
        payload: {},
      });

      expect(vulnerabilitySecurityOverride.showCommentField).toBe(false);
    });
  });

  describe('componentDetailsVulnerabilities/loadVulnerabilityDetails/rejected action', () => {
    it('sets the error to the payload and the loading flag to false in both vulnerabilityDetails and vulnerabilitySecurityOverride', () => {
      const state = Object.freeze({
        vulnerabilityDetails: {
          details: null,
          loading: true,
          error: null,
        },
        vulnerabilitySecurityOverride: {
          loading: true,
          loadError: null,
        },
      });

      const { vulnerabilityDetails, vulnerabilitySecurityOverride } = reducer(state, {
        type: 'componentDetailsVulnerabilities/loadVulnerabilityDetails/rejected',
        payload: 'load error',
      });

      expect(vulnerabilityDetails.loading).toBe(false);
      expect(vulnerabilityDetails.error).toBe('load error');

      expect(vulnerabilitySecurityOverride.loading).toBe(false);
      expect(vulnerabilitySecurityOverride.loadError).toBe('load error');
    });
  });

  describe('componentDetailsVulnerabilities/loadVulnerabilities/pending action', () => {
    it('sets the loading flag to true', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        vulnerabilities: {
          loading: false,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/pending',
      });

      expect(newState.vulnerabilities.loading).toBe(true);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('componentDetailsVulnerabilities/loadVulnerabilities/fulfilled action', () => {
    it('sets loading flag to false, unsets the error and fills in the data', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        vulnerabilities: {
          data: null,
          loading: true,
          error: 'some error',
        },
      });

      const payload = {
        data: {
          securityVulnerabilities: [
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
          ],
        },
      };

      const newState = reducer(state, {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/fulfilled',
        payload,
      });
      expect(newState.vulnerabilities.data).toEqual([
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
      ]);
      expect(newState.vulnerabilities.loading).toBe(false);
      expect(newState.vulnerabilities.error).toBe(null);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('componentDetailsVulnerabilities/loadVulnerabilities/rejected action', () => {
    it('sets the error to the payload and the loading flag to false', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        vulnerabilities: {
          data: null,
          loading: true,
          error: null,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsVulnerabilities/loadVulnerabilities/rejected',
        payload: 'loadError',
      });

      expect(newState.vulnerabilities.loading).toBe(false);
      expect(newState.vulnerabilities.error).toBe('loadError');
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('componentDetailsVulnerabilities/toggleVulnerabilityPopoverWithEffects action', () => {
    it('sets the referenceId to the payload, toggles the popover visibility and restarts the vulnerability override form values', () => {
      const state = Object.freeze({
        vulnerabilityDetails: {
          details: null,
          loading: true,
          error: 'error',
        },
        vulnerabilities: {
          data: [
            {
              refId: '1',
              severity: 8,
              status: 'status 1',
            },
          ],
        },
        selectedRefId: null,
        showVulnerabilityDetailPopover: false,
        vulnerabilitySecurityOverride: {
          status: 'status 2',
          saveError: 'save error',
          comments: { value: 'associated comment', trimmedValue: 'associated comment', isPristine: false },
          showCommentField: false,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsVulnerabilities/toggleVulnerabilityPopoverWithEffects',
        payload: '1',
      });

      expect(newState.showVulnerabilityDetailPopover).toBe(true);
      expect(newState.selectedRefId).toBe('1');
      expect(newState.vulnerabilitySecurityOverride.status).toEqual('');
      expect(newState.vulnerabilitySecurityOverride.saveError).toBe(null);
      expect(newState.vulnerabilitySecurityOverride.comments).toEqual({
        value: '',
        trimmedValue: '',
        isPristine: true,
        validationErrors: null,
      });
      expect(newState.vulnerabilitySecurityOverride.showCommentField).toBe(true);

      const newToggledState = reducer(newState, {
        type: 'componentDetailsVulnerabilities/toggleVulnerabilityPopoverWithEffects',
        payload: null,
      });
      expect(newToggledState.showVulnerabilityDetailPopover).toBe(false);
    });
  });

  describe('componentDetailsVulnerabilities/setVulnerabilityOverrideStatus', () => {
    it(
      'sets input state with the payload on the status field of the vulnerabilitySecurityOverride, ' +
        're initializing the comments value, as well as cleaning up any saveErrors',
      () => {
        const state = Object.freeze({
          vulnerabilitySecurityOverride: {
            status: 'status 2',
            comments: { value: 'associated comment', trimmedValue: 'associated comment', isPristine: false },
            saveError: 'there was an error saving the previous state',
            showCommentField: false,
          },
        });

        const newState = reducer(state, {
          type: 'componentDetailsVulnerabilities/setVulnerabilityOverrideStatus',
          payload: 'ACKNOWLEDGED',
        });

        expect(newState.vulnerabilitySecurityOverride.status).toEqual('ACKNOWLEDGED');
        expect(newState.vulnerabilitySecurityOverride.comments).toEqual({
          value: '',
          trimmedValue: '',
          isPristine: true,
          validationErrors: null,
        });
        expect(newState.vulnerabilitySecurityOverride.saveError).toBe(null);
        expect(newState.vulnerabilitySecurityOverride.showCommentField).toBe(true);
      }
    );
  });

  describe('componentDetailsVulnerabilities/setVulnerabilityOverrideComments', () => {
    it('sets input state with the payload on the comments field of the vulnerabilitySecurityOverride and sets pristine tp false', () => {
      const state = Object.freeze({
        vulnerabilitySecurityOverride: {
          status: 'Open',
          comments: { value: 'associated comment', trimmedValue: 'associated comment', isPristine: true },
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsVulnerabilities/setVulnerabilityOverrideComments',
        payload: 'This is the new comment for the status',
      });

      expect(newState.vulnerabilitySecurityOverride.status).toEqual('Open');
      expect(newState.vulnerabilitySecurityOverride.comments).toEqual({
        value: 'This is the new comment for the status',
        trimmedValue: 'This is the new comment for the status',
        isPristine: false,
        validationErrors: null,
      });
    });

    it('prevents the input of a comment longer than 1000 characters', () => {
      const state = Object.freeze({
        vulnerabilitySecurityOverride: {
          status: 'Open',
          comments: { value: 'associated comment', trimmedValue: 'associated comment', isPristine: true },
        },
      });

      const preparedPayload = 'c'.repeat(1020);
      const newState = reducer(state, {
        type: 'componentDetailsVulnerabilities/setVulnerabilityOverrideComments',
        payload: preparedPayload,
      });

      expect(newState.vulnerabilitySecurityOverride.comments).toEqual({
        value: preparedPayload,
        trimmedValue: preparedPayload,
        isPristine: false,
        validationErrors: 'Please enter less than 1000 characters',
      });
    });
  });

  describe('saveVulnerabilityOverrideMaskDone', () => {
    it('sets the maskState of vulnerabilitySecurityOverride to null', () => {
      const state = Object.freeze({
        vulnerabilitySecurityOverride: {
          submitMaskState: false,
        },
      });

      const { vulnerabilitySecurityOverride } = reducer(state, {
        type: 'componentDetailsVulnerabilities/saveVulnerabilityOverrideMaskDone',
      });

      expect(vulnerabilitySecurityOverride.submitMaskState).toBe(null);
    });
  });

  describe('setVulnerabilityOverrideFormDisabled', () => {
    it('sets the isDisabled of vulnerabilitySecurityOverride to false', () => {
      const state = Object.freeze({
        vulnerabilitySecurityOverride: {
          isDisabled: true,
        },
      });

      const { vulnerabilitySecurityOverride } = reducer(state, {
        type: 'componentDetailsVulnerabilities/setVulnerabilityOverrideFormDisabled',
        payload: false,
      });

      expect(vulnerabilitySecurityOverride.isDisabled).toBe(false);
    });
  });

  describe('saveVulnerabilityOverride', () => {
    describe('componentDetailsVulnerabilities/saveVulnerabilityOverride/pending action', () => {
      it('sets the submitMaskState flag to false', () => {
        const state = Object.freeze({
          vulnerabilitySecurityOverride: {
            submitMaskState: null,
          },
        });

        const newState = reducer(state, {
          type: 'componentDetailsVulnerabilities/saveVulnerabilityOverride/pending',
        });

        expect(newState.vulnerabilitySecurityOverride.submitMaskState).toBe(false);
      });
    });

    describe('componentDetailsVulnerabilities/saveVulnerabilityOverride/fulfilled action', () => {
      it('sets submitMaskState flag to false, unsets the error and updates the vulnerability record with the new status and comments', () => {
        const state = Object.freeze({
          selectedRefId: '4',
          vulnerabilitySecurityOverride: {
            submitMaskState: false,
            saveError: 'save error',
            comments: { value: 'some previous value', trimmedValue: 'some previous value' },
          },
          vulnerabilities: {
            data: [{ refId: '4', status: 'Open' }],
          },
        });

        const payload = {
          status: 'NOT_APPLICABLE',
          comment: 'This comment comes from the server',
        };

        const newState = reducer(state, {
          type: 'componentDetailsVulnerabilities/saveVulnerabilityOverride/fulfilled',
          payload,
        });
        expect(newState.vulnerabilities.data).toEqual([{ refId: '4', status: 'Not Applicable' }]);
        expect(newState.vulnerabilitySecurityOverride.submitMaskState).toBe(true);
        expect(newState.vulnerabilitySecurityOverride.saveError).toBe(null);
        expect(newState.vulnerabilitySecurityOverride.comments).toEqual({
          value: 'This comment comes from the server',
          trimmedValue: 'This comment comes from the server',
          validationErrors: undefined,
          isPristine: true,
        });
      });

      it('keeps the current comments in the field representing the comments form field if no comment returns from the server and sets the status to Open', () => {
        const state = Object.freeze({
          selectedRefId: '4',
          vulnerabilitySecurityOverride: {
            submitMaskState: false,
            comments: { value: 'some previous value', trimmedValue: 'some previous value' },
          },
          vulnerabilities: {
            data: [{ refId: '4', status: 'Not Applicable' }],
          },
        });

        const newState = reducer(state, {
          type: 'componentDetailsVulnerabilities/saveVulnerabilityOverride/fulfilled',
          payload: '',
        });
        expect(newState.vulnerabilities.data).toEqual([{ refId: '4', status: 'Open' }]);
        expect(newState.vulnerabilitySecurityOverride.comments).toEqual({
          value: 'some previous value',
          trimmedValue: 'some previous value',
          isPristine: true,
        });
      });
    });

    describe('componentDetailsVulnerabilities/saveVulnerabilityOverride/rejected action', () => {
      it('sets the error to the payload and the submitMaskState flag to null', () => {
        const state = Object.freeze({
          vulnerabilitySecurityOverride: {
            submitMaskState: false,
            saveError: null,
          },
        });

        const newState = reducer(state, {
          type: 'componentDetailsVulnerabilities/saveVulnerabilityOverride/rejected',
          payload: 'Error during save operation',
        });

        expect(newState.vulnerabilitySecurityOverride.submitMaskState).toBe(null);
        expect(newState.vulnerabilitySecurityOverride.saveError).toBe('Error during save operation');
      });
    });
  });

  describe('SELECT_COMPONENT', () => {
    it('resets current state to initialState', () => {
      const state = Object.freeze({
        vulnerabilityDetails: {
          loading: true,
          error: 'error',
          details: {},
        },
        vulnerabilities: {
          data: {},
          loading: true,
          error: 'error',
        },
        showVulnerabilityDetailPopover: true,
        selectedRefId: '42',
        vulnerabilitySecurityOverride: {
          status: 'status',
          loading: true,
          loadError: 'error',
          submitMaskState: true,
          saveError: 'error',
        },
      });

      const newState = reducer(state, { type: SELECT_COMPONENT });

      expect(newState).toEqual(initialState);
    });
  });
});
