/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import ChangePasswordModal from '../../../../../main/frontend/mainHeader/MenuBar/UserMenu/ChangePasswordModal';
import { NxForm } from '@sonatype/react-shared-components';

describe('ChangePasswordModal', function () {
  let minProps;
  let getMountedComponent;

  beforeEach(() => {
    minProps = {
      onClose: jasmine.createSpy('onClose'),
      onChangePassword: jasmine.createSpy('onChangePassword'),
      changePasswordError: undefined,
      changePasswordStatus: 'idle',
    };
    getMountedComponent = enzymeUtils.getMountedComponent(ChangePasswordModal, minProps);
  });

  it('calls onChangePassword when for is filled and submitted', () => {
    const onChangePassword = jasmine.createSpy('onChangePassword');
    const component = getMountedComponent({ onChangePassword });
    component.find('input#original-password').simulate('change', { target: { value: 'myOldPassword' } });
    component.find('input#new-password').simulate('change', { target: { value: 'myNewPassword' } });
    component.find('input#confirm-password').simulate('change', { target: { value: 'myNewPassword' } });
    component.update();

    component.find(NxForm).invoke('onSubmit')();
    expect(onChangePassword).toHaveBeenCalled();
    component.unmount();
  });
});
