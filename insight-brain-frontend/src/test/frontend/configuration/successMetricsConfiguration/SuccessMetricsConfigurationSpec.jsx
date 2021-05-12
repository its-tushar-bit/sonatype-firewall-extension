/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxForm, NxLoadError, NxButton } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../enzymeUtils';

import SuccessMetricsConfiguration, {
  authErrorMessage,
} from '../../../../main/frontend/configuration/successMetricsConfiguration/SuccessMetricsConfiguration';

describe('SuccessMetricsConfiguration', function () {
  let mockUpdate,
    mockLoad,
    mockReset,
    mockToggleIsEnabled,
    getShallowComponent,
    getMountedComponent,
    mountedComponent,
    minimalProps;

  beforeEach(function () {
    mockUpdate = jasmine.createSpy('update');
    mockLoad = jasmine.createSpy('load');
    mockReset = jasmine.createSpy('resetForm');
    mockToggleIsEnabled = jasmine.createSpy('toggleIsEnabled');

    minimalProps = {
      isAuthorized: true,
      load: mockLoad,
      update: mockUpdate,
      resetForm: mockReset,
      toggleIsEnabled: mockToggleIsEnabled,
      enabled: false,
      loading: false,
      isDirty: false,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(SuccessMetricsConfiguration, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(SuccessMetricsConfiguration, minimalProps);
  });

  afterEach(function () {
    if (mountedComponent) {
      mountedComponent.unmount();
    }

    mountedComponent = null;
  });

  it('renders a component with the "nx-page-main" class', function () {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  describe('on initial load', function () {
    it('calls load', function () {
      mountedComponent = getMountedComponent();

      expect(mockLoad).toHaveBeenCalled();
    });

    it('shows error if loadError occurred', function () {
      const expectedError = 'Error 403';
      mountedComponent = getMountedComponent({ isAuthorized: true, loadError: { status: '403' } });
      const loadError = mountedComponent.find(NxLoadError);

      expect(loadError).toExist();
      expect(loadError).toHaveProp('error', expectedError);
    });
  });

  describe('on cancel', function () {
    it('calls resetForm', function () {
      mountedComponent = getMountedComponent({ isDirty: true });
      const cancel = mountedComponent.find('#success-metrics-cancel').first();

      cancel.simulate('click');

      expect(mockReset).toHaveBeenCalled();
    });
  });

  describe('on form submit', function () {
    it('calls update when the form is submitted if it"s dirty', function () {
      const shallowComponent = getShallowComponent({ isDirty: true });
      const form = shallowComponent.find(NxForm);

      form.simulate('submit');

      expect(mockUpdate).toHaveBeenCalled();
    });

    it('shows alert if update request failed', function () {
      const updateError = 'Some random error from failed update';
      mountedComponent = getMountedComponent({ updateError });
      const errorComponent = mountedComponent.find(NxLoadError);

      expect(errorComponent).toHaveProp('error', updateError);
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

  describe('on authorization error', function () {
    it('sets default authErrorMessage value if loadError isn"t provided', function () {
      mountedComponent = getMountedComponent({ isAuthorized: false });
      const loadError = mountedComponent.find(NxLoadError);

      expect(loadError).toExist();
      expect(loadError).toHaveProp('error', authErrorMessage);
    });

    it('calls load on retry click', function () {
      mountedComponent = getMountedComponent({ isAuthorized: false });
      const retryButton = mountedComponent.find(NxButton);

      retryButton.simulate('click');

      expect(mockLoad).toHaveBeenCalled();
    });
  });
});
