/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxErrorAlert,
  NxForm,
  NxModal,
  NxSuccessAlert,
  nxTextInputStateHelpers,
} from '@sonatype/react-shared-components';
import CheckLogin from '../../../../main/frontend/configuration/ldap/checkLogin/CheckLogin';
import * as enzymeUtils from '../../../frontend/enzymeUtils';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('CheckLogin', () => {
  let mockCloseModal,
    mockSetInputField,
    mockCheckLogin,
    getShallowComponent,
    mockResetCheckLoginModal,
    minimalProps,
    modalContainer;

  beforeEach(() => {
    mockCloseModal = jasmine.createSpy('closeModal');
    mockSetInputField = jasmine.createSpy('setInputField');
    mockCheckLogin = jasmine.createSpy('checkLogin');
    mockResetCheckLoginModal = jasmine.createSpy('resetCheckLoginModal');

    minimalProps = {
      closeModal: mockCloseModal,
      setInputField: mockSetInputField,
      checkLogin: mockCheckLogin,
      username: initUserInput(''),
      password: initUserInput(''),
      ldapId: 'LDAP-ID',
      resetCheckLoginModal: mockResetCheckLoginModal,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(CheckLogin, minimalProps);
    modalContainer = document.createElement('div');
    document.body.appendChild(modalContainer);
  });

  afterEach(() => {
    if (modalContainer) {
      document.body.removeChild(modalContainer);
      modalContainer = null;
    }
  });

  it('renders a NxModal', () => expect(getShallowComponent().find(NxModal)).toExist());

  it('calls checkLogin on submit', () => {
    const shallowComponent = getShallowComponent();
    const form = shallowComponent.find(NxForm);
    form.simulate('submit');
    expect(mockCheckLogin).toHaveBeenCalledWith('LDAP-ID');
  });

  it('displays the NxSuccessAlert when checkLoginSuccess is set to true', () => {
    const shallowComponent = getShallowComponent({ checkLoginSuccess: true });
    const successAlert = shallowComponent.find(NxSuccessAlert);
    expect(successAlert).toExist();
  });

  it('displays the NxErrorAlert when checkLoginError is filled', () => {
    const shallowComponent = getShallowComponent({ checkLoginError: 'some error' });
    const errorAlert = shallowComponent.find(NxErrorAlert);
    expect(errorAlert).toExist();
  });

  it('rests the view before unmount component', () => {
    const mountedComponent = enzymeUtils.getMountedComponent(CheckLogin, minimalProps, { attachTo: modalContainer })();
    mountedComponent.unmount();
    expect(mockResetCheckLoginModal).toHaveBeenCalledTimes(1);
  });
});
