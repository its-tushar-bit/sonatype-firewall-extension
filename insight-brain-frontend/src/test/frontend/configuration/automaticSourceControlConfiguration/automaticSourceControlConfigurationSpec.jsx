/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxButton, NxStatefulForm } from '@sonatype/react-shared-components';
import AutomaticSourceControlConfiguration from '../../../../main/frontend/configuration/automaticSourceControlConfiguration/AutomaticSourceControlConfiguration';
import * as enzymeUtils from '../../enzymeUtils';

describe('AutomaticSourceControlConfiguration', function () {
  let mockUpdate, mockToggleIsEnabled, minimalProps, mockReset, getShallowComponent, mockLoad;

  beforeEach(function () {
    mockUpdate = jasmine.createSpy('update');
    mockLoad = jasmine.createSpy('load');
    mockReset = jasmine.createSpy('resetForm');
    mockToggleIsEnabled = jasmine.createSpy('toggleEnabled');
    minimalProps = {
      update: mockUpdate,
      load: mockLoad,
      resetForm: mockReset,
      toggleEnabled: mockToggleIsEnabled,
      loading: false,
      isDirty: false,
      enabled: false,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(AutomaticSourceControlConfiguration, minimalProps);
  });

  it('renders a component with the nx-page-main class', function () {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  describe('on initial load', function () {
    it('calls load', function () {
      const getMountedComponent = enzymeUtils.getMountedComponent(AutomaticSourceControlConfiguration, minimalProps);
      const mountedComponent = getMountedComponent();

      expect(mockLoad).toHaveBeenCalled();
      mountedComponent.unmount();
    });

    it('shows error if loadError occurred', function () {
      const loadError = 'some error happend';
      const shallowComponent = getShallowComponent({ loadError });
      const nxForm = shallowComponent.find(NxStatefulForm);

      expect(nxForm).toHaveProp('loadError', loadError);
    });
  });

  describe('on cancel', function () {
    it('contains the cancel bottom', function () {
      const shallowComponent = getShallowComponent({ isDirty: true });
      const cancelButton = (
        <NxButton type="button" id="automatic-source-control-cancel" disabled={false} onClick={mockReset}>
          Cancel
        </NxButton>
      );
      const form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('additionalFooterBtns', cancelButton);
    });

    it('contains disabled the cancel bottom when the form is not dirty', function () {
      const shallowComponent = getShallowComponent({ isDirty: false });
      const cancelButton = (
        <NxButton type="button" id="automatic-source-control-cancel" disabled={true} onClick={mockReset}>
          Cancel
        </NxButton>
      );
      const form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('additionalFooterBtns', cancelButton);
    });
  });

  describe('on form submit', function () {
    it('calls update when the form is submitted if it is dirty', function () {
      const shallowComponent = getShallowComponent({ isDirty: true });
      const form = shallowComponent.find(NxStatefulForm);
      form.simulate('submit');
      expect(mockUpdate).toHaveBeenCalledTimes(1);
    });

    it('has validation error when the form is dirty', function () {
      const shallowComponent = getShallowComponent({ isDirty: true });
      const form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('validationErrors', null);
    });

    it('has not validation error when the form is not dirty', function () {
      const shallowComponent = getShallowComponent({ isDirty: false });
      const form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('validationErrors', 'There are no changes to update');
    });
  });

  describe('doLoad', function () {
    it('has proper handler', function () {
      const shallowComponent = getShallowComponent();
      const form = shallowComponent.find(NxStatefulForm);

      expect(form).toHaveProp('doLoad', mockLoad);
    });
  });

  describe('on toggle change', function () {
    it('calls toggleIsEnabled when toggle value is changed', function () {
      const shallowComponent = getShallowComponent();
      const toggle = shallowComponent.find('.nx-toggle--no-gap');
      toggle.simulate('change');
      expect(mockToggleIsEnabled).toHaveBeenCalled();
    });

    it('has the toggle value connected to toggle prop', function () {
      let shallowComponent = getShallowComponent({ enabled: false });
      expect(shallowComponent.find('.nx-toggle--no-gap')).toHaveProp('isChecked', false);
      shallowComponent = getShallowComponent({ enabled: true });
      expect(shallowComponent.find('.nx-toggle--no-gap')).toHaveProp('isChecked', true);
    });
  });

  describe('on authorization error', function () {
    it('sets default authErrorMessage value if loadError is not provided', function () {
      const loadError = 'Some load error happend';
      const shallowComponent = getShallowComponent({ loadError });
      const nxForm = shallowComponent.find(NxStatefulForm);
      expect(nxForm).toHaveProp('loadError', loadError);
    });
  });
});
