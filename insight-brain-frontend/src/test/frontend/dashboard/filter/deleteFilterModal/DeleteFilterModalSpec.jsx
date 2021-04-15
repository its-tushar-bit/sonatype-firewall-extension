/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxErrorAlert,
  NxSubmitMask,
  NxLoadError,
  NxModal,
} from '@sonatype/react-shared-components';

import * as enzymeUtils from '../../../enzymeUtils';
import DeleteFilterModal from '../../../../../main/frontend/dashboard/filter/deleteFilterModal/DeleteFilterModal';

describe('DeleteFilterModal', function () {
  let getShallowComponent, hideDeleteFilterModal, deleteFilter;

  beforeEach(function () {
    hideDeleteFilterModal = jasmine.createSpy('hideDeleteFilterModal');
    deleteFilter = jasmine.createSpy('deleteFilter');

    getShallowComponent = enzymeUtils.getShallowComponent(DeleteFilterModal, {
      filterToDelete: 'filter1',
      deleteFilterError: null,
      hideDeleteFilterModal,
      deleteFilter,
    });
  });

  it('renders NxModal when filterToDelete is not null', function () {
    const shallowRender = getShallowComponent();
    console.log(shallowRender.debug());
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
      const submitButton = getShallowComponent().find(
        '#delete-filter-modal-continue-button'
      );
      expect(submitButton).toHaveProp('variant', 'primary');
      expect(submitButton).toHaveText('Continue');
    });
  });

  describe('when deleteFilterError is not null', function () {
    let component;

    beforeEach(function () {
      component = getShallowComponent({
        deleteFilterError: 'error123',
      });
    });

    it('renders a NxLoadError in the footer which can retry the delete', function () {
      const loadError = component.find('.nx-footer').find(NxLoadError);

      expect(loadError).toExist();
      expect(loadError).toHaveProp('error', 'error123');

      expect(deleteFilter).not.toHaveBeenCalled();
      loadError.prop('retryHandler')();
      expect(deleteFilter).toHaveBeenCalled();
    });

    it('does not render the Continue button', function () {
      expect(
        component.find('#delete-filter-modal-continue-button')
      ).not.toExist();
    });
  });

  describe('NxSubmitMask', function () {
    it('is rendered when deleteFilterSaving is true', function () {
      const component = getShallowComponent({
        deleteFilterSaving: true,
        deleteFilterSuccess: false,
      });

      expect(component.find('form').childAt(0)).toContainReact(
        <NxSubmitMask message="Removing…" success={false} />
      );
    });

    it('is rendered when deleteFilterSuccess is true', function () {
      const component = getShallowComponent({
        deleteFilterSaving: false,
        deleteFilterSuccess: true,
      });

      expect(component.find('form').childAt(0)).toContainReact(
        <NxSubmitMask message="Removing…" success={true} />
      );
    });

    it('is not rendered when both deleteFilterSaving and deleteFilterSuccess are false', function () {
      const component = getShallowComponent({
        deleteFilterSaving: false,
        deleteFilterSuccess: false,
      });

      expect(component.find(NxSubmitMask)).not.toExist();
    });
  });

  describe('onSubmit handler', function () {
    it('fires deleteFilter action with filterToDelete and calls preventDefault on the event', function () {
      const preventDefault = jasmine.createSpy('preventDefault');
      getShallowComponent().find('form').simulate('submit', {
        preventDefault,
      });
      expect(preventDefault).toHaveBeenCalled();
      expect(deleteFilter).toHaveBeenCalledWith('filter1');
    });
  });

  describe('cancel button click handler', function () {
    it('fires hideDeleteFilterModal action', function () {
      const cancelButton = getShallowComponent().find(
        '#delete-filter-modal-cancel-button'
      );
      cancelButton.simulate('click');
      expect(hideDeleteFilterModal).toHaveBeenCalled();
    });
  });
});
