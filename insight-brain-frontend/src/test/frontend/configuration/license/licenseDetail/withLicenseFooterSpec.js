/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxStatefulForm, NxModal } from '@sonatype/react-shared-components';
import WithLicenseFooter from '../../../../../main/frontend/configuration/license/footers/WithLicenseFooter';
import * as enzymeUtils from '../../../enzymeUtils';

describe('WithLicenseFooter', () => {
  let getShallowComponent, minimalProps, mockFileChangeHandler, mockUninstallLicense;

  beforeEach(() => {
    mockFileChangeHandler = jasmine.createSpy('fileChangeHandler');
    mockUninstallLicense = jasmine.createSpy('uninstallLicense').and.returnValue(Promise.resolve({ type: null }));
    minimalProps = {
      fileChangeHandler: mockFileChangeHandler,
      uninstallLicense: mockUninstallLicense,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(WithLicenseFooter, minimalProps);
  });

  it('renders a component with a nx-btn-bar', () => {
    const shallowComponent = getShallowComponent();
    const footer = shallowComponent.find('.nx-btn-bar');
    expect(footer).toExist();
  });

  it('calls fileChangeHandler on input value change', () => {
    const shallowComponent = getShallowComponent();
    const hiddenInput = shallowComponent.find('input');
    hiddenInput.simulate('change');
    expect(mockFileChangeHandler).toHaveBeenCalledTimes(1);
  });

  describe('uninstall confirmation modal', () => {
    let modal, form;

    beforeEach(() => {
      const shallowComponent = getShallowComponent();
      const uninstallLicenseBtn = shallowComponent.find('#uninstall-license');
      uninstallLicenseBtn.simulate('click');
      modal = shallowComponent.find(NxModal);
      form = shallowComponent.find(NxStatefulForm);
    });

    it('opens the confirmation modal', () => {
      expect(modal).toExist();
    });

    it('calls uninstallLicenseHandler after click on uninstall btn in confirmation modal', () => {
      form.simulate('submit');
      expect(mockUninstallLicense).toHaveBeenCalledTimes(1);
    });

    it('has uninstallMaskState setted as false', () => {
      const shallowComponent = getShallowComponent({ uninstallMaskState: false });
      const uninstallLicenseBtn = shallowComponent.find('#uninstall-license');
      uninstallLicenseBtn.simulate('click');
      modal = shallowComponent.find(NxModal);
      form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('submitMaskState', false);
    });

    it('has uninstallError prop', () => {
      const uninstallError = 'some error happened';
      const shallowComponent = getShallowComponent({ uninstallError });
      const uninstallLicenseBtn = shallowComponent.find('#uninstall-license');
      uninstallLicenseBtn.simulate('click');
      modal = shallowComponent.find(NxModal);
      form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('submitError', uninstallError);
    });
  });
});
