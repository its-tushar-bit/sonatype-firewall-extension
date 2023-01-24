/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxButton,
  NxErrorAlert,
  NxLoadError,
  NxModal,
  NxStatefulForm,
  NxSubmitMask,
} from '@sonatype/react-shared-components';

import * as enzymeUtils from 'TestRoot/enzymeUtils';
import DeleteFilterModal from 'MainRoot/dashboard/filter/deleteFilterModal/DeleteFilterModal';

describe('DeleteFilterModal', function () {
  let getShallowComponent, mountPoint, getMountedComponentWithAutoClean, mountedComponent;

  const hideDeleteFilterModal = jasmine.createSpy('hideDeleteFilterModal'),
    deleteFilter = jasmine.createSpy('deleteFilter');

  const minimalProps = {
    filterToDelete: 'filter1',
    deleteFilterError: null,
    hideDeleteFilterModal,
    deleteFilter,
  };

  beforeEach(function () {
    mountPoint = document.createElement('div');
    document.body.appendChild(mountPoint);
    getShallowComponent = enzymeUtils.getShallowComponent(DeleteFilterModal, minimalProps);

    const getMountedComponent = enzymeUtils.getMountedComponent(DeleteFilterModal, minimalProps, {
      attachTo: mountPoint,
    });
    getMountedComponentWithAutoClean = (additionalProps) => {
      mountedComponent = getMountedComponent(additionalProps);
      return mountedComponent;
    };
  });

  afterEach(() => {
    if (mountPoint) {
      document.body.removeChild(mountPoint);
      mountPoint = null;
    }
    mountedComponent?.unmount();
    mountedComponent = null;
  });

  it('renders NxModal when filterToDelete is not null', function () {
    const shallowRender = getShallowComponent();
    expect(shallowRender).toMatchSelector(NxModal);
  });

  it('renders nothing filterToDelete is null', function () {
    const shallowRender = getShallowComponent({
      filterToDelete: null,
    });

    expect(shallowRender).toBeEmptyRender();
  });

  describe('when deleteFilterError is null', function () {
    it('renders warning message with the filter name as modal content', function () {
      const modalContent = getShallowComponent().find('.nx-modal-content');

      expect(modalContent.find('ForwardRef(NxWarningAlert)')).toHaveText(
        'You are about to delete "filter1" filter. This action can not be undone.'
      );
    });

    it('renders footer without .nx-error class', function () {
      const footer = getShallowComponent().find('footer');

      expect(footer).not.toHaveClassName('nx-error');
    });

    it('renders footer with no NxErrorAlert', function () {
      const footer = getShallowComponent().find('footer');

      expect(footer.find(NxErrorAlert)).not.toExist();
    });

    it('renders submit button as primary with "Continue" text', function () {
      const component = getMountedComponentWithAutoClean(),
        submitButton = component.find(NxButton).last();

      expect(submitButton).toHaveProp('variant', 'primary');
      expect(submitButton).toHaveText('Continue');
    });
  });

  describe('when deleteFilterError is not null', function () {
    let component;

    beforeEach(function () {
      component = getMountedComponentWithAutoClean({
        deleteFilterError: 'error123',
      });
    });

    it('renders a NxLoadError in the footer with retry button', function () {
      const loadError = component.find('.nx-footer').find(NxLoadError),
        retryBtn = loadError.find(NxButton).first();

      expect(loadError).toExist();
      expect(loadError).toHaveProp('error', 'error123');
      expect(retryBtn).toExist();
    });

    it('does not render the Continue button', function () {
      expect(component.find('#delete-filter-modal-continue-button')).not.toExist();
    });
  });

  describe('NxSubmitMask', function () {
    it('shows submitting mask when deleteFilterMaskState is false', function () {
      const component = getMountedComponentWithAutoClean({ deleteFilterMaskState: false }),
        mask = component.find(NxSubmitMask);

      expect(mask).toHaveText('Removing…');
    });

    it('shows success mask when deleteFilterMaskState is true', function () {
      const component = getMountedComponentWithAutoClean({ deleteFilterMaskState: true }),
        mask = component.find(NxSubmitMask);

      expect(mask).toHaveText('Success!');
    });

    it('is not rendered when deleteFilterMaskState is null', function () {
      const component = getMountedComponentWithAutoClean({ deleteFilterMaskState: null }),
        mask = component.find(NxSubmitMask);

      expect(mask).not.toExist();
    });
  });

  describe('onSubmit handler', function () {
    it('fires deleteFilter action with filterToDelete and calls preventDefault on the event', function () {
      const preventDefault = jasmine.createSpy('preventDefault');
      getShallowComponent().find(NxStatefulForm).simulate('submit', {
        preventDefault,
      });

      expect(preventDefault).toHaveBeenCalled();
      expect(deleteFilter).toHaveBeenCalledWith('filter1');
    });
  });

  describe('cancel button click handler', function () {
    it('fires hideDeleteFilterModal action', function () {
      const cancelButton = getMountedComponentWithAutoClean().find(NxButton).first();
      cancelButton.simulate('click');

      expect(hideDeleteFilterModal).toHaveBeenCalled();
    });
  });
});
