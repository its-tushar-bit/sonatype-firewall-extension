/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

import reduce from '../../../../main/frontend/configuration/mail/mailConfigSlice';

const { initialState, userInput } = nxTextInputStateHelpers;

describe('mailConfigSlice reducer', function () {
  let otherObject;

  beforeEach(function () {
    otherObject = { value: 'test value' };
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('mailConfig/save/pending action', function () {
    it('sets submitMaskState to false', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null,
      });

      const newState = reduce(state, {
        type: 'mailConfig/save/pending',
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('mailConfig/save/fulfilled action', function () {
    it('sets submitMaskState to true', function () {
      const state = Object.freeze({
        other: otherObject,
        formState: {},
        submitMaskState: false,
      });

      const newState = reduce(state, {
        type: 'mailConfig/save/fulfilled',
        payload: {
          hostname: 'test.host',
          port: 42,
          systemEmail: 'foo@bar.com',
        },
      });

      expect(newState.submitMaskState).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('mailConfig/save/rejected action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: true,
        formState: {},
      });

      const newState = reduce(state, {
        type: 'mailConfig/save/rejected',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('mailConfig/delete/pending action', function () {
    it('sets submitMaskState to false', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null,
      });

      const newState = reduce(state, {
        type: 'mailConfig/delete/pending',
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('mailConfig/delete/fulfilled action', function () {
    it('sets submitMaskState to true and enables input validation errors', function () {
      const state = Object.freeze({
        submitMaskState: false,
        formState: {
          hostname: initialState(''),
          port: initialState(''),
          systemEmail: initialState(''),
        },
      });

      const newState = reduce(state, {
        type: 'mailConfig/delete/fulfilled',
      });

      expect(newState.submitMaskState).toBe(true);
      expect(newState.formState.hostname.validationErrors).toBeTruthy();
      expect(newState.formState.port.validationErrors).toBeTruthy();
      expect(newState.formState.systemEmail.validationErrors).toBeTruthy();
    });
  });

  describe('mailConfig/delete/rejected action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: true,
      });

      const newState = reduce(state, {
        type: 'mailConfig/delete/rejected',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('mailConfig/sendTestEmail/pending action', function () {
    it('sets submitMaskState to false', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: null,
      });

      const newState = reduce(state, {
        type: 'mailConfig/sendTestEmail/pending',
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('mailConfig/sendTestEmail/fulfilled action', function () {
    it('sets submitMaskState to true', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: false,
      });

      const newState = reduce(state, {
        type: 'mailConfig/sendTestEmail/fulfilled',
      });

      expect(newState.submitMaskState).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('mailConfig/sendTestEmail/rejected action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: false,
      });

      const newState = reduce(state, {
        type: 'mailConfig/sendTestEmail/rejected',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('mailConfig/submitMaskTimerDone action', function () {
    it('sets submitMaskState to null', function () {
      const state = Object.freeze({
        other: otherObject,
        submitMaskState: true,
      });

      const newState = reduce(state, {
        type: 'mailConfig/submitMaskTimerDone',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('mailConfig/resetForm action', function () {
    it('returns the initial state if theres no serverData', function () {
      const state = Object.freeze({
        other: otherObject,
      });

      const newState = reduce(state, {
        type: 'mailConfig/resetForm',
      });

      expect(newState.isDirty).toBe(false);
      expect(newState.isValid).toBe(false);
      expect(newState.hasAllRequiredData).toBe(false);
      expect(newState.loading).toBe(false);
      expect(newState.submitMaskState).toBe(null);
      expect(newState.submitMaskMessage).toBe(null);
      expect(newState.loadError).toBe(null);
      expect(newState.saveError).toBe(null);
      expect(newState.deleteError).toBe(null);
      expect(newState.testEmailError).toBe(null);
      expect(newState.showDeleteModal).toBe(false);
      expect(newState.mustReenterPassword).toBe(false);
      expect(newState.testEmailSent).toBe(false);
      expect(newState.serverData).toBe(null);
      expect(newState.formState.hostname.value).toBe('');
      expect(newState.formState.port.value).toBe('');
      expect(newState.formState.username.value).toBe('');
      expect(newState.formState.password.value).toBe('');
      expect(newState.formState.sslEnabled).toBe(false);
      expect(newState.formState.startTlsEnabled).toBe(false);
      expect(newState.formState.systemEmail.value).toBe('');
      expect(newState.formState.testEmail.value).toBe('');
      expect(newState.formState.hostname.validationErrors).toBeTruthy();
      expect(newState.formState.port.validationErrors).toBeTruthy();
      expect(newState.formState.systemEmail.validationErrors).toBeTruthy();
    });

    it('resets the formState based on the serverData', function () {
      const state = Object.freeze({
        serverData: {
          hostname: 'host',
          port: 1234,
          username: 'user',
          sslEnabled: true,
          startTlsEnabled: true,
          systemEmail: 'foo@system.com',
        },
        formState: {
          hostname: userInput(() => 'bad', 'asdf'),
          port: userInput(() => 'bad', 'asdf'),
          username: userInput(() => 'bad', 'asdf'),
          password: userInput(() => 'bad', 'asdf'),
          sslEnabled: false,
          startTlsEnabled: false,
          systemEmail: userInput(() => 'bad', 'asdf'),
          testEmail: userInput(() => 'bad', 'asdf'),
        },
      });

      const newState = reduce(state, {
        type: 'mailConfig/resetForm',
      });

      expect(newState.serverData).toBe(state.serverData);
      expect(newState.formState.hostname.value).toBe('host');
      expect(newState.formState.port.value).toBe('1234');
      expect(newState.formState.username.value).toBe('user');
      expect(newState.formState.password.value).toBe('\0\0\0\0\0'); // password gets set to the fake password value
      expect(newState.formState.sslEnabled).toBe(true);
      expect(newState.formState.startTlsEnabled).toBe(true);
      expect(newState.formState.systemEmail.value).toBe('foo@system.com');
      expect(newState.formState.testEmail.value).toBe('asdf'); // testEmail does not get reset
    });

    it('resets the various flags', function () {
      const state = Object.freeze({
        serverData: {
          hostname: '',
          port: 1234,
          username: 'user',
          password: 'admin123',
          sslEnabled: true,
          startTlsEnabled: true,
          systemEmail: 'foo@system.com',
          testEmail: 'bob@gmail.com',
        },
        formState: {
          hostname: userInput(() => 'bad', 'asdf'),
          port: userInput(() => 'bad', 'asdf'),
          username: userInput(() => 'bad', 'asdf'),
          password: userInput(() => 'bad', 'asdf'),
          sslEnabled: false,
          startTlsEnabled: false,
          systemEmail: userInput(() => 'bad', 'asdf'),
          testEmail: userInput(() => 'bad', 'asdf'),
        },
      });

      const newState = reduce(state, {
        type: 'mailConfig/resetForm',
      });

      expect(newState.isDirty).toBe(false);
      expect(newState.hasAllRequiredData).toBe(false);
      expect(newState.loading).toBe(false);
      expect(newState.submitMaskState).toBe(null);
      expect(newState.submitMaskMessage).toBe(null);
      expect(newState.loadError).toBe(null);
      expect(newState.saveError).toBe(null);
      expect(newState.deleteError).toBe(null);
      expect(newState.testEmailError).toBe(null);
      expect(newState.mustReenterPassword).toBe(false);
      expect(newState.testEmailSent).toBe(false);
    });
  });

  describe('mailConfig/setHostname', function () {
    describe('when payload is empty string', function () {
      it('sets the hostname value and a validation error', function () {
        const state = Object.freeze({
          other: otherObject,
          formState: {
            hostname: initialState(''),
            port: initialState(''),
            username: initialState(''),
            password: initialState(''),
            systemEmail: initialState(''),
            testEmail: initialState(''),
          },
        });

        const newState = reduce(state, {
          type: 'mailConfig/setHostname',
          payload: '',
        });

        expect(newState.formState.hostname.value).toBe('');
        expect(newState.formState.hostname.trimmedValue).toBe('');
        expect(newState.formState.hostname.isPristine).toBe(false);
        expect(newState.formState.hostname.validationErrors).toBeTruthy();
        expect(newState.other).toBe(otherObject);
      });
    });

    describe('when payload is invalid hostname string', function () {
      it('sets the hostname value and a validation error', function () {
        const state = Object.freeze({
          other: otherObject,
          formState: {
            hostname: initialState(''),
            port: initialState(''),
            username: initialState(''),
            password: initialState(''),
            systemEmail: initialState(''),
            testEmail: initialState(''),
          },
        });

        const newState = reduce(state, {
          type: 'mailConfig/setHostname',
          payload: 'sonatype.com/host',
        });

        expect(newState.formState.hostname.value).toBe('sonatype.com/host');
        expect(newState.formState.hostname.trimmedValue).toBe('sonatype.com/host');
        expect(newState.formState.hostname.isPristine).toBe(false);
        expect(newState.formState.hostname.validationErrors).toBeTruthy();
        expect(newState.other).toBe(otherObject);
      });
    });

    describe('when payload is valid hostname string', function () {
      it('sets the hostname value and no validation error', function () {
        const state = Object.freeze({
          other: otherObject,
          formState: {
            hostname: initialState(''),
            port: initialState(''),
            username: initialState(''),
            password: initialState(''),
            systemEmail: initialState(''),
            testEmail: initialState(''),
          },
        });

        const newState = reduce(state, {
          type: 'mailConfig/setHostname',
          payload: 'asdf',
        });

        expect(newState.formState.hostname.value).toBe('asdf');
        expect(newState.formState.hostname.trimmedValue).toBe('asdf');
        expect(newState.formState.hostname.isPristine).toBe(false);
        expect(newState.formState.hostname.validationErrors.length).toBe(0);
        expect(newState.other).toBe(otherObject);
      });
    });
  });

  describe('mailConfig/setPort', function () {
    describe('when payload is empty string', function () {
      it('sets the hostname value and a validation error', function () {
        const state = Object.freeze({
          other: otherObject,
          formState: {
            hostname: initialState(''),
            port: initialState(''),
            username: initialState(''),
            password: initialState(''),
            systemEmail: initialState(''),
            testEmail: initialState(''),
          },
        });

        const newState = reduce(state, {
          type: 'mailConfig/setPort',
          payload: '',
        });

        expect(newState.formState.port.value).toBe('');
        expect(newState.formState.port.trimmedValue).toBe('');
        expect(newState.formState.port.isPristine).toBe(false);
        expect(newState.formState.port.validationErrors.length).toBeGreaterThan(0);
        expect(newState.other).toBe(otherObject);
      });
    });

    describe('when payload is non-numeric string', function () {
      it('sets the hostname value and a validation error', function () {
        const state = Object.freeze({
          other: otherObject,
          formState: {
            hostname: initialState(''),
            port: initialState(''),
            username: initialState(''),
            password: initialState(''),
            systemEmail: initialState(''),
            testEmail: initialState(''),
          },
        });

        const newState = reduce(state, {
          type: 'mailConfig/setPort',
          payload: 'asdf',
        });

        expect(newState.formState.port.value).toBe('asdf');
        expect(newState.formState.port.trimmedValue).toBe('asdf');
        expect(newState.formState.port.isPristine).toBe(false);
        expect(newState.formState.port.validationErrors.length).toBeGreaterThan(0);
        expect(newState.other).toBe(otherObject);
      });
    });

    describe('when payload is a numeric string', function () {
      it('sets the hostname value and no validation error', function () {
        const state = Object.freeze({
          other: otherObject,
          formState: {
            hostname: initialState(''),
            port: initialState(''),
            username: initialState(''),
            password: initialState(''),
            systemEmail: initialState(''),
            testEmail: initialState(''),
          },
        });

        const newState = reduce(state, {
          type: 'mailConfig/setPort',
          payload: '12345 ',
        });

        expect(newState.formState.port.value).toBe('12345 ');
        expect(newState.formState.port.trimmedValue).toBe('12345');
        expect(newState.formState.port.isPristine).toBe(false);
        expect(newState.formState.port.validationErrors.length).toBe(0);
        expect(newState.other).toBe(otherObject);
      });
    });
  });

  describe('mailConfig/setUsername', function () {
    it('sets the username value and no validation error', function () {
      const state = Object.freeze({
        other: otherObject,
        formState: {
          hostname: initialState(''),
          port: initialState(''),
          username: initialState(''),
          password: initialState(''),
          systemEmail: initialState(''),
          testEmail: initialState(''),
        },
      });

      const newState = reduce(state, {
        type: 'mailConfig/setUsername',
        payload: 'user ',
      });

      expect(newState.formState.username.value).toBe('user ');
      expect(newState.formState.username.trimmedValue).toBe('user');
      expect(newState.formState.username.isPristine).toBe(false);
      expect(newState.formState.username.validationErrors).toBeFalsy();
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('mailConfig/setPassword', function () {
    it('sets the password value and no validation error', function () {
      const state = Object.freeze({
        other: otherObject,
        formState: {
          hostname: initialState(''),
          port: initialState(''),
          username: initialState(''),
          password: initialState(''),
          systemEmail: initialState(''),
          testEmail: initialState(''),
        },
      });

      const newState = reduce(state, {
        type: 'mailConfig/setPassword',
        payload: 'user ',
      });

      expect(newState.formState.password.value).toBe('user ');
      expect(newState.formState.password.trimmedValue).toBe('user');
      expect(newState.formState.password.isPristine).toBe(false);
      expect(newState.formState.password.validationErrors).toBeFalsy();
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('mailConfig/setSslEnabled', function () {
    it('sets the sslEnabled value', function () {
      const state = Object.freeze({
        other: otherObject,
        formState: {
          sslEnabled: true,
          hostname: initialState(''),
          port: initialState(''),
          username: initialState(''),
          password: initialState(''),
          systemEmail: initialState(''),
          testEmail: initialState(''),
        },
      });

      const newState = reduce(state, {
        type: 'mailConfig/setSslEnabled',
        payload: false,
      });

      expect(newState.formState.sslEnabled).toBe(false);
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('mailConfig/setStartTlsEnabled', function () {
    it('sets the startTlsEnabled value', function () {
      const state = Object.freeze({
        other: otherObject,
        formState: {
          startTlsEnabled: true,
          hostname: initialState(''),
          port: initialState(''),
          username: initialState(''),
          password: initialState(''),
          systemEmail: initialState(''),
          testEmail: initialState(''),
        },
      });

      const newState = reduce(state, {
        type: 'mailConfig/setStartTlsEnabled',
        payload: false,
      });

      expect(newState.formState.startTlsEnabled).toBe(false);
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('mailConfig/setSystemEmail', function () {
    describe('when payload is empty string', function () {
      it('sets the sysetmEmail value and a validation error', function () {
        const state = Object.freeze({
          other: otherObject,
          formState: {
            hostname: initialState(''),
            port: initialState(''),
            username: initialState(''),
            password: initialState(''),
            systemEmail: initialState(''),
            testEmail: initialState(''),
          },
        });

        const newState = reduce(state, {
          type: 'mailConfig/setSystemEmail',
          payload: '',
        });

        expect(newState.formState.systemEmail.value).toBe('');
        expect(newState.formState.systemEmail.trimmedValue).toBe('');
        expect(newState.formState.systemEmail.isPristine).toBe(false);
        expect(newState.formState.systemEmail.validationErrors.length).toBeGreaterThan(0);
        expect(newState.other).toBe(otherObject);
      });
    });

    describe('when payload is non-empty string', function () {
      it('sets the systemEmail value and no validation error', function () {
        const state = Object.freeze({
          other: otherObject,
          formState: {
            hostname: initialState(''),
            port: initialState(''),
            username: initialState(''),
            password: initialState(''),
            systemEmail: initialState(''),
            testEmail: initialState(''),
          },
        });

        const newState = reduce(state, {
          type: 'mailConfig/setSystemEmail',
          payload: 'asdf@asdf.com ',
        });

        expect(newState.formState.systemEmail.value).toBe('asdf@asdf.com ');
        expect(newState.formState.systemEmail.trimmedValue).toBe('asdf@asdf.com');
        expect(newState.formState.systemEmail.isPristine).toBe(false);
        expect(newState.formState.systemEmail.validationErrors.length).toBe(0);
        expect(newState.other).toBe(otherObject);
      });

      it('sets validation error if invalid / simple string email', function () {
        const state = Object.freeze({
          other: otherObject,
          formState: {
            hostname: initialState(''),
            port: initialState(''),
            username: initialState(''),
            password: initialState(''),
            systemEmail: initialState(''),
            testEmail: initialState(''),
          },
        });

        const newState = reduce(state, {
          type: 'mailConfig/setSystemEmail',
          payload: 'simplestring  ',
        });

        expect(newState.formState.systemEmail.value).toBe('simplestring  ');
        expect(newState.formState.systemEmail.trimmedValue).toBe('simplestring');
        expect(newState.formState.systemEmail.isPristine).toBe(false);
        expect(newState.formState.systemEmail.validationErrors.length).toBeGreaterThan(0);
        expect(newState.formState.systemEmail.validationErrors[0]).toContain('Invalid system email');
        expect(newState.other).toBe(otherObject);
      });
    });
  });

  describe('mailConfig/setTestEmail', function () {
    it('sets the testEmail value and no validation error', function () {
      const state = Object.freeze({
        other: otherObject,
        formState: {
          hostname: initialState(''),
          port: initialState(''),
          username: initialState(''),
          password: initialState(''),
          systemEmail: initialState(''),
          testEmail: initialState(''),
        },
      });

      const newState = reduce(state, {
        type: 'mailConfig/setTestEmail',
        payload: 'asdf ',
      });

      expect(newState.formState.testEmail.value).toBe('asdf ');
      expect(newState.formState.testEmail.trimmedValue).toBe('asdf');
      expect(newState.formState.testEmail.isPristine).toBe(false);
      expect(newState.formState.testEmail.validationErrors).toBeFalsy();
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('mailConfig/setShowDeleteModal action', function () {
    it('sets setShowDeleteModal to null', function () {
      const state = Object.freeze({
        other: otherObject,
        showDeleteModal: true,
      });

      const newState = reduce(state, {
        type: 'mailConfig/setShowDeleteModal',
        payload: false,
      });

      expect(newState.showDeleteModal).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
});
