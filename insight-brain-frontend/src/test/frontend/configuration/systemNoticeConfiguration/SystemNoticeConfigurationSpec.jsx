/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxForm, NxButton } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../enzymeUtils';

import SystemNoticeConfiguration from '../../../../main/frontend/configuration/systemNoticeConfiguration/SystemNoticeConfiguration';

describe('SystemNoticeConfiguration', function () {
  let mockUpdate, mockLoad, mockReset, mockToggleIsEnabled, mockSetMessage, getShallowComponent, minimalProps;

  beforeEach(function () {
    mockUpdate = jasmine.createSpy('update');
    mockLoad = jasmine.createSpy('load');
    mockReset = jasmine.createSpy('resetForm');
    mockToggleIsEnabled = jasmine.createSpy('toggleIsEnabled');
    mockSetMessage = jasmine.createSpy('setMessage');

    minimalProps = {
      isAuthorized: true,
      load: mockLoad,
      update: mockUpdate,
      resetForm: mockReset,
      toggleIsEnabled: mockToggleIsEnabled,
      setMessage: mockSetMessage,
      enabled: false,
      message: {
        trimmedValue: '',
        value: '',
        isPristine: true,
      },
      loading: false,
      isDirty: false,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(SystemNoticeConfiguration, minimalProps);
  });

  it('renders a component with the "nx-page-main" class', function () {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  describe('on initial load', function () {
    it('calls load', function () {
      const getMountedComponent = enzymeUtils.getMountedComponent(SystemNoticeConfiguration, minimalProps);
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
        <NxButton type="button" id="system-notice-cancel" onClick={mockReset} disabled={false}>
          Cancel
        </NxButton>
      );
    });

    it('contains disabled cancel button if form is not dirty', function () {
      const shallowComponent = getShallowComponent({ isDirty: false });
      const form = shallowComponent.find(NxForm);

      expect(form).toHaveProp(
        'additionalFooterBtns',
        <NxButton type="button" id="system-notice-cancel" onClick={mockReset} disabled={true}>
          Cancel
        </NxButton>
      );
    });
  });

  describe('validationErrors', function () {
    it('should be null if form was changed', function () {
      const shallowComponent = getShallowComponent({
        isDirty: true,
        message: {
          value: 'text',
          trimmedValue: 'text',
          isPristine: false,
        },
        enabled: true,
      });
      const form = shallowComponent.find(NxForm);

      expect(form).toHaveProp('validationErrors', null);
    });

    it('should be null if form was changed', function () {
      const shallowComponent = getShallowComponent({
        isDirty: true,
        message: {
          value: '',
          trimmedValue: '',
          isPristine: false,
        },
        enabled: false,
      });
      const form = shallowComponent.find(NxForm);

      expect(form).toHaveProp('validationErrors', null);
    });

    it('should contain tooltip validation message if form was not changed', function () {
      const shallowComponent = getShallowComponent({ isDirty: false });
      const form = shallowComponent.find(NxForm);

      expect(form).toHaveProp('validationErrors', 'There are no changes to update');
    });

    it('should contain tooltip validation message if enabled and message is empty', function () {
      const shallowComponent = getShallowComponent({
        isDirty: true,
        message: {
          value: '',
          trimmedValue: '',
          isPristine: false,
        },
        enabled: true,
      });
      const form = shallowComponent.find(NxForm);

      expect(form).toHaveProp('validationErrors', 'Notice Text cannot be blank');
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
      const toggle = shallowComponent.find('#system-notice-display-toggle-checkbox');

      toggle.simulate('change');

      expect(mockToggleIsEnabled).toHaveBeenCalled();
    });

    it('has proper isChecked prop value', function () {
      let shallowComponent = getShallowComponent();

      expect(shallowComponent.find('#system-notice-display-toggle-checkbox')).toHaveProp('isChecked', false);

      shallowComponent = getShallowComponent({ enabled: true });

      expect(shallowComponent.find('#system-notice-display-toggle-checkbox')).toHaveProp('isChecked', true);
    });
  });

  describe('on text change', function () {
    it('calls setMessage when textarea value is changed', function () {
      const shallowComponent = getShallowComponent();
      const textArea = shallowComponent.find('#system-notice-text');

      textArea.simulate('change', 'notice changed');

      expect(mockSetMessage).toHaveBeenCalledWith('notice changed');
    });
  });
});
