/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';

describe('componentDetailsLicenseDetectionsTile reducer', () => {
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

  describe('componentDetailsLicenseDetectionsTile/toggleShowEditLicensesPopover', () => {
    it('returns toggles showEditLicensesPopover', function () {
      const state = Object.freeze({ showEditLicensesPopover: false });
      const action = {
        type: 'componentDetailsLicenseDetectionsTile/toggleShowEditLicensesPopover',
      };

      const { showEditLicensesPopover } = reducer(state, action);

      expect(showEditLicensesPopover).toBe(true);
    });
  });

  describe('componentDetailsLicenseDetectionsTile/load', () => {
    it('returns componentDetailsLicenseDetectionsTile/load/pending', function () {
      const otherState = { foo: 'bar' };
      const state = Object.freeze({ loading: true, ...otherState });
      const action = {
        type: 'componentDetailsLicenseDetectionsTile/load/pending',
      };

      const { loading, ...expectedOtherState } = reducer(state, action);

      expect(loading).toBe(true);
      expect(expectedOtherState).toEqual(otherState);
    });

    it('returns componentDetailsLicenseDetectionsTile/load/fulfilled', function () {
      const otherState = { foo: 'bar' };
      const state = Object.freeze({
        licenseOverride: null,
        declaredlicenses: null,
        effectiveLicenses: null,
        observedlicenses: null,
        selectableLicenses: null,
        allLicenses: null,
        loading: true,
        loadError: 'error',
        editLicensesForm: {
          scope: null,
          comment: {
            value: '',
            isPristine: true,
          },
          status: null,
          isDirty: false,
          submitError: null,
          submitMaskState: null,
          fieldsPristineState: null,
        },
        otherState,
      });
      const firstLicenseOverride = { license1: { id: 'id1' } };
      const payload = {
        licenseOverride: [firstLicenseOverride, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
        declaredlicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
        effectiveLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
        observedlicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
        selectableLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
        allLicenses: [{ license1: { id: 'id1' } }, { license2: { id: 'id2' } }, { license3: { id: 'id3' } }],
      };
      const action = {
        payload,
        type: 'componentDetailsLicenseDetectionsTile/load/fulfilled',
      };

      const expectedState = reducer(state, action);

      expect(expectedState.loading).toBe(false);
      expect(expectedState.loadError).toBeNull();
      expect(expectedState.licenseOverride).toEqual(payload.licenseOverride);
      expect(expectedState.declaredlicenses).toEqual(payload.declaredlicenses);
      expect(expectedState.effectiveLicenses).toEqual(payload.effectiveLicenses);
      expect(expectedState.observedlicenses).toEqual(payload.observedlicenses);
      expect(expectedState.selectableLicenses).toEqual(payload.selectableLicenses);
      expect(expectedState.allLicenses).toEqual(payload.allLicenses);
      expect(expectedState.editLicensesForm.isDirty).toBeFalse();
      expect(expectedState.editLicensesForm.scope).toBe(firstLicenseOverride);
      expect(expectedState.editLicensesForm.status).toBe(null);
      expect(expectedState.editLicensesForm.comment.value).toBe('');
      expect(expectedState.editLicensesForm.fieldsPristineState).toEqual({
        comment: '',
        scope: firstLicenseOverride,
        status: null,
      });
      expect(expectedState.otherState).toEqual(otherState);
    });

    it('returns componentDetailsLicenseDetectionsTile/load/rejected', function () {
      const otherState = { foo: 'bar' };
      const state = Object.freeze({ loading: false, loadError: null, ...otherState });
      const action = {
        payload: 'error',
        type: 'componentDetailsLicenseDetectionsTile/load/rejected',
      };

      const { loading, loadError, ...expectedOtherState } = reducer(state, action);

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('componentDetailsLicenseDetectionsTile/resetSubmitMaskState action', () => {
    it('resets submitMaskState', () => {
      const state = Object.freeze({
        editLicensesForm: {
          submitMaskState: false,
        },
      });

      const { editLicensesForm } = reducer(state, {
        type: 'componentDetailsLicenseDetectionsTile/resetSubmitMaskState',
      });

      expect(editLicensesForm.submitMaskState).toBe(null);
    });
  });

  describe('componentDetailsLicenseDetectionsTile/resetEditLicensesFormFields action', () => {
    const mockState = {
      someField: 'some value',
      licenseOverride: [],
      editLicensesForm: {
        isDirty: true,
        scope: 'some scope',
        status: 'some status',
        comment: {
          value: 'some value',
          isPristine: false,
        },
        submitError: 'someError',
      },
    };

    it('resets form field states', () => {
      const state = Object.freeze(mockState);

      const newState = reducer(state, { type: 'componentDetailsLicenseDetectionsTile/resetEditLicensesFormFields' });

      expect(newState.editLicensesForm.isDirty).toBe(false);
      expect(newState.editLicensesForm.submitError).toBe(null);
      expect(newState.editLicensesForm.scope).toBe(null);
      expect(newState.editLicensesForm.status).toBe(null);
      expect(newState.editLicensesForm.comment.value).toBe('');
      expect(newState.editLicensesForm.comment.isPristine).toBe(true);
    });

    const firstOverrideScope = {
        ownerId: 'owf',
        ownerName: 'OWF',
        ownerType: 'application',
        licenseOverride: null,
      },
      thirdOverrideScope = {
        ownerId: 'ROOT_ORGANIZATION_ID',
        ownerName: 'Root Organization',
        ownerType: 'organization',
        licenseOverride: {
          id: '82823b22b17d4925a358763058b82184',
          ownerId: 'ROOT_ORGANIZATION_ID',
          status: 'ACKNOWLEDGED',
          comment: '',
          licenseIds: [],
          componentIdentifier: {
            format: 'a-name',
            coordinates: {
              name: 'bson',
              qualifier: '',
              version: '0.0.4',
            },
          },
        },
      },
      licenseOverride = [
        firstOverrideScope,
        {
          ownerId: 'asdf',
          ownerName: 'asdf',
          ownerType: 'organization',
          licenseOverride: null,
        },
        thirdOverrideScope,
      ];

    it('resets form field states to first scope with licenseOverride', () => {
      const state = Object.freeze({
        ...mockState,
        licenseOverride,
      });

      const newState = reducer(state, { type: 'componentDetailsLicenseDetectionsTile/resetEditLicensesFormFields' });

      expect(newState.editLicensesForm.scope).toBe(thirdOverrideScope);
      expect(newState.editLicensesForm.status).toBe('ACKNOWLEDGED');
      expect(newState.editLicensesForm.comment.value).toBe('');
      expect(newState.editLicensesForm.comment.isPristine).toBe(true);
    });

    it('resets form field states to first scope if no scope contains licenseOverride', () => {
      const state = Object.freeze({
        ...mockState,
        licenseOverride: licenseOverride.slice(0, -1),
      });

      const newState = reducer(state, { type: 'componentDetailsLicenseDetectionsTile/resetEditLicensesFormFields' });

      expect(newState.editLicensesForm.scope).toBe(firstOverrideScope);
      expect(newState.editLicensesForm.status).toBe(null);
      expect(newState.editLicensesForm.comment.value).toBe('');
      expect(newState.editLicensesForm.comment.isPristine).toBe(true);
    });
  });

  describe('componentDetailsLicenseDetectionsTile/setLicenseFormStatus action', () => {
    it('sets status and isDirty', () => {
      const state = Object.freeze({
        editLicensesForm: {
          status: null,
          comment: { value: '' },
          isDirty: false,
          fieldsPristineState: {
            status: null,
            comment: { value: '' },
          },
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsLicenseDetectionsTile/setLicenseFormStatus',
        payload: 'new status',
      });

      expect(newState.editLicensesForm.status).toBe('new status');
      expect(newState.editLicensesForm.isDirty).toBe(true);
    });
  });

  describe('componentDetailsLicenseDetectionsTile/setLicenseFormScope action', () => {
    const scope = {
      ownerId: 'owf',
      ownerName: 'OWF',
      ownerType: 'application',
      licenseOverride: null,
    };
    it('sets scope and isDirty', () => {
      const state = Object.freeze({
        editLicensesForm: {
          scope: null,
          comment: { value: '' },
          isDirty: false,
          fieldsPristineState: {
            scope: null,
            comment: { value: '' },
          },
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsLicenseDetectionsTile/setLicenseFormScope',
        payload: scope,
      });

      expect(newState.editLicensesForm.scope).toEqual(scope);
      expect(newState.editLicensesForm.isDirty).toBe(true);
    });
  });

  describe('componentDetailsLicenseDetectionsTile/setLicenseFormComment action', () => {
    it('sets comment and isDirty', () => {
      const state = Object.freeze({
        editLicensesForm: {
          comment: { value: '' },
          isDirty: false,
          fieldsPristineState: {
            comment: { value: '' },
          },
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsLicenseDetectionsTile/setLicenseFormComment',
        payload: 'new value',
      });

      expect(newState.editLicensesForm.comment.value).toBe('new value');
      expect(newState.editLicensesForm.isDirty).toBe(true);
    });
  });

  describe('componentDetailsLicenseDetectionsTile/saveEditLicensesForm', () => {
    it('returns componentDetailsLicenseDetectionsTile/saveEditLicensesForm/pending action', () => {
      const state = Object.freeze({
          editLicensesForm: {
            submitMaskState: true,
            submitError: 'some old error',
          },
        }),
        action = {
          type: 'componentDetailsLicenseDetectionsTile/saveEditLicensesForm/pending',
        };

      const { editLicensesForm } = reducer(state, action),
        { submitMaskState, submitError } = editLicensesForm;

      expect(submitMaskState).toBe(false);
      expect(submitError).toBe(null);
    });

    it('returns componentDetailsLicenseDetectionsTile/saveEditLicensesForm/fulfilled action', () => {
      const state = Object.freeze({
          editLicensesForm: {
            submitMaskState: false,
            submitError: 'some old error',
          },
        }),
        action = {
          type: 'componentDetailsLicenseDetectionsTile/saveEditLicensesForm/fulfilled',
        };

      const { editLicensesForm } = reducer(state, action),
        { submitMaskState, submitError } = editLicensesForm;

      expect(submitMaskState).toBe(true);
      expect(submitError).toBe(null);
    });

    it('returns componentDetailsLicenseDetectionsTile/saveEditLicensesForm/rejected action', () => {
      const state = Object.freeze({
          editLicensesForm: {
            submitMaskState: false,
            submitError: null,
          },
        }),
        payload = 'http error',
        action = {
          type: 'componentDetailsLicenseDetectionsTile/saveEditLicensesForm/rejected',
          payload,
        };

      const { editLicensesForm } = reducer(state, action),
        { submitMaskState, submitError } = editLicensesForm;

      expect(submitMaskState).toBe(null);
      expect(submitError).toEqual(payload);
    });
  });

  describe('componentDetailsLicenseDetectionsTile/deleteLicenseOverride', () => {
    it('returns componentDetailsLicenseDetectionsTile/deleteLicenseOverride/pending action', () => {
      const state = Object.freeze({
          editLicensesForm: {
            submitMaskState: true,
            submitError: 'some old error',
          },
        }),
        action = {
          type: 'componentDetailsLicenseDetectionsTile/deleteLicenseOverride/pending',
        };

      const { editLicensesForm } = reducer(state, action),
        { submitMaskState, submitError } = editLicensesForm;

      expect(submitMaskState).toBe(false);
      expect(submitError).toBe(null);
    });

    it('returns componentDetailsLicenseDetectionsTile/deleteLicenseOverride/fulfilled action', () => {
      const state = Object.freeze({
          editLicensesForm: {
            submitMaskState: false,
            submitError: 'some old error',
          },
        }),
        action = {
          type: 'componentDetailsLicenseDetectionsTile/deleteLicenseOverride/fulfilled',
        };

      const { editLicensesForm } = reducer(state, action),
        { submitMaskState, submitError } = editLicensesForm;

      expect(submitMaskState).toBe(true);
      expect(submitError).toBe(null);
    });

    it('returns componentDetailsLicenseDetectionsTile/deleteLicenseOverride/rejected action', () => {
      const state = Object.freeze({
          editLicensesForm: {
            submitMaskState: false,
            submitError: null,
          },
        }),
        payload = 'http error',
        action = {
          type: 'componentDetailsLicenseDetectionsTile/deleteLicenseOverride/rejected',
          payload,
        };

      const { editLicensesForm } = reducer(state, action),
        { submitMaskState, submitError } = editLicensesForm;

      expect(submitMaskState).toBe(null);
      expect(submitError).toEqual(payload);
    });
  });
});
