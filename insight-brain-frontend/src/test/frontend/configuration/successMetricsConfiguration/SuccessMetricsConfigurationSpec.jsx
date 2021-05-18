/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxForm, NxButton } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../enzymeUtils';

import SuccessMetricsConfiguration from '../../../../main/frontend/configuration/successMetricsConfiguration/SuccessMetricsConfiguration';

describe('SuccessMetricsConfiguration', function () {
  let mockUpdate, mockLoad, mockReset, mockToggleIsEnabled, getShallowComponent, minimalProps;

  beforeEach(function () {
    mockUpdate = jasmine.createSpy('update');
    mockLoad = jasmine.createSpy('load');
    mockReset = jasmine.createSpy('resetForm');
    mockToggleIsEnabled = jasmine.createSpy('toggleIsEnabled');

    minimalProps = {
      load: mockLoad,
      update: mockUpdate,
      resetForm: mockReset,
      toggleIsEnabled: mockToggleIsEnabled,
      enabled: false,
      loading: false,
      isDirty: false,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(SuccessMetricsConfiguration, minimalProps);
  });

  it('renders a component with the "nx-page-main" class', function () {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  describe('on initial load', function () {
    it('calls load', function () {
      const getMountedComponent = enzymeUtils.getMountedComponent(SuccessMetricsConfiguration, minimalProps);
      const mountedComponent = getMountedComponent();

      expect(mockLoad).toHaveBeenCalled();
      mountedComponent.unmount();
    });
  });

  describe('additionalFooterBtns prop', function () {
    it('contains non disabled cancel button with proper click handler if form is dirty', function () {
      const shallowComponent = getShallowComponent({ isDirty: true });
      const form = shallowComponent.find(NxForm);

      expect(form).toHaveProp(
        'additionalFooterBtns',
        <NxButton type="button" id="success-metrics-cancel" onClick={mockReset} disabled={false}>
          Cancel
        </NxButton>
      );
    });

    it('contains disabled cancel button if form is not dirty', function () {
      const shallowComponent = getShallowComponent({ isDirty: false });
      const form = shallowComponent.find(NxForm);

      expect(form).toHaveProp(
        'additionalFooterBtns',
        <NxButton type="button" id="success-metrics-cancel" onClick={mockReset} disabled={true}>
          Cancel
        </NxButton>
      );
    });
  });

  describe('validationErrors', function () {
    it('should be null if form was changed', function () {
      const shallowComponent = getShallowComponent({ isDirty: true });
      const form = shallowComponent.find(NxForm);

      expect(form).toHaveProp('validationErrors', null);
    });

    it('should contain tooltip validation message if form was not changed', function () {
      const shallowComponent = getShallowComponent({ isDirty: false });
      const form = shallowComponent.find(NxForm);

      expect(form).toHaveProp('validationErrors', 'There are no changes to update');
    });
  });

  describe('doLoad', function () {
    it('should have proper handler', function () {
      const shallowComponent = getShallowComponent();
      const form = shallowComponent.find(NxForm);

      expect(form).toHaveProp('doLoad', mockLoad);
    });
  });

  describe('on form submit', function () {
    it('calls update when the form is submitted if it"s dirty', function () {
      const shallowComponent = getShallowComponent({ isDirty: true });
      const form = shallowComponent.find(NxForm);

      form.simulate('submit');

      expect(mockUpdate).toHaveBeenCalled();
    });
  });

  describe('on toggle change', function () {
    it('calls toggleIsEnabled when toggle value is changed', function () {
      const shallowComponent = getShallowComponent();
      const toggle = shallowComponent.find('.nx-toggle--no-gap');

      toggle.simulate('change');

      expect(mockToggleIsEnabled).toHaveBeenCalled();
    });

    it('calls toggleIsEnabled when toggle value is changed', function () {
      let shallowComponent = getShallowComponent();

      expect(shallowComponent.find('.nx-toggle--no-gap')).toHaveProp('isChecked', false);

      shallowComponent = getShallowComponent({ enabled: true });

      expect(shallowComponent.find('.nx-toggle--no-gap')).toHaveProp('isChecked', true);
    });
  });
});
