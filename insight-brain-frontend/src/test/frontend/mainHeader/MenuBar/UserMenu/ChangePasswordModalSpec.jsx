/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import {
  ChangePasswordModal,
  CONFIRMATION_MISMATCH_ERROR_MESSAGE,
  MISSING_FIELDS_ERROR_MESSAGE,
} from '../../../../../main/frontend/mainHeader/MenuBar/UserMenu/ChangePasswordModal';
import { NxForm, NxTextInput } from '@sonatype/react-shared-components';

describe('ChangePasswordModal', function () {
  let minProps;
  let getShallowComponent;

  function findInputById(component, id) {
    return component.find(NxTextInput).filterWhere((component) => component.prop('id') === id);
  }

  function typeInInput(component, idOrInput, value) {
    const input = typeof idOrInput === 'string' ? findInputById(component, idOrInput) : idOrInput;
    input.prop('onChange')(value);
    component.update();
  }

  beforeEach(() => {
    minProps = {
      onClose: jasmine.createSpy('onClose'),
      onChangePassword: jasmine.createSpy('onChangePassword'),
      changePasswordError: undefined,
      changePasswordStatus: 'idle',
    };
    getShallowComponent = enzymeUtils.getShallowComponent(ChangePasswordModal, minProps);
  });

  it('calls onChangePassword when for is filled out and submitted', () => {
    const onChangePassword = jasmine.createSpy('onChangePassword');
    const component = getShallowComponent({ onChangePassword });
    typeInInput(component, 'original-password', 'myOldPassword');
    typeInInput(component, 'new-password', 'myNewPassword');
    typeInInput(component, 'confirm-password', 'myNewPassword');

    component.find(NxForm).invoke('onSubmit')();
    expect(onChangePassword).toHaveBeenCalledWith({
      oldPassword: 'myOldPassword',
      newPassword: 'myNewPassword',
    });
  });

  // So when your form is pristine, and you type into "New Password", there should be no error message.
  // But when you start typing into "Confirm New Password", the "must match" error should appear only for "Confirm New Password" input.
  it('shows a validation error message, only under confirm-password, when confirm does not match the new-password', () => {
    const component = getShallowComponent();

    typeInInput(component, 'original-password', 'myOldPassword');
    const newPassword = findInputById(component, 'new-password');
    const confirmPassword = findInputById(component, 'confirm-password');

    typeInInput(component, newPassword, 'imatch');
    typeInInput(component, confirmPassword, 'idontmatch');

    expect(findInputById(component, 'new-password').prop('validationErrors')).toEqual(null);
    expect(findInputById(component, 'confirm-password').prop('validationErrors')).toEqual(
      CONFIRMATION_MISMATCH_ERROR_MESSAGE
    );
  });

  it('shows validation ui as soon as the new password and confirm password match', () => {
    const component = getShallowComponent();

    typeInInput(component, 'new-password', 'match');
    typeInInput(component, 'confirm-password', 'matc');

    expect(findInputById(component, 'confirm-password').prop('validationErrors')).toEqual(
      CONFIRMATION_MISMATCH_ERROR_MESSAGE
    );
    typeInInput(component, 'confirm-password', 'match');

    expect(findInputById(component, 'new-password').prop('validationErrors')).toEqual(null);
    expect(findInputById(component, 'confirm-password').prop('validationErrors')).toEqual(null);
  });

  it('shows validation errors when the new password and confirm go from match to unmatched again', () => {
    const component = getShallowComponent();

    typeInInput(component, 'new-password', 'match');
    typeInInput(component, 'confirm-password', 'match');

    expect(findInputById(component, 'new-password').prop('validationErrors')).toEqual(null);
    expect(findInputById(component, 'confirm-password').prop('validationErrors')).toEqual(null);

    typeInInput(component, 'new-password', 'nomatch');

    expect(findInputById(component, 'new-password').prop('validationErrors')).toEqual(null);
    expect(findInputById(component, 'confirm-password').prop('validationErrors')).toEqual(
      CONFIRMATION_MISMATCH_ERROR_MESSAGE
    );
  });

  it('does not allow the form to be submitted if there are any fields empty', () => {
    const component = getShallowComponent();
    expect(component.find(NxForm).prop('validationErrors')).toEqual([MISSING_FIELDS_ERROR_MESSAGE]);

    typeInInput(component, 'original-password', 'mypassword');
    typeInInput(component, 'new-password', 'match');
    typeInInput(component, 'confirm-password', 'match');

    expect(component.find(NxForm).prop('validationErrors')).toEqual([]);

    typeInInput(component, 'original-password', '');
    expect(component.find(NxForm).prop('validationErrors')).toEqual([MISSING_FIELDS_ERROR_MESSAGE]);
  });

  it('does not allow the form to be submitted if the new password and confirmation do not match (while pris', () => {
    const component = getShallowComponent();
    expect(component.find(NxForm).prop('validationErrors')).toEqual([MISSING_FIELDS_ERROR_MESSAGE]);

    typeInInput(component, 'original-password', 'mypassword');
    typeInInput(component, 'new-password', 'match');
    typeInInput(component, 'confirm-password', 'no-match');

    expect(component.find(NxForm).prop('validationErrors')[0]).toEqual(CONFIRMATION_MISMATCH_ERROR_MESSAGE);
  });

  it('displays a non-empty error message for current password if you enter something and then delete it', () => {
    const component = getShallowComponent();

    expect(findInputById(component, 'original-password').prop('isPristine')).toEqual(true);

    typeInInput(component, 'original-password', 'originalpw');
    expect(findInputById(component, 'original-password').prop('isPristine')).toEqual(false);
    expect(findInputById(component, 'original-password').prop('validationErrors')).toEqual(null);

    typeInInput(component, 'original-password', '');
    component.update();
    expect(findInputById(component, 'original-password').prop('validationErrors')).toEqual('Must be non-empty');
  });
});
