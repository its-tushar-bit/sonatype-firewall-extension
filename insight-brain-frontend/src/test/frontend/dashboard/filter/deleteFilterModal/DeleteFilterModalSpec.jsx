/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxErrorAlert, NxFontAwesomeIcon, NxSubmitMask } from '@sonatype/react-shared-components';
import { faSync } from '@fortawesome/free-solid-svg-icons/index';

import * as enzymeUtils from '../../../enzymeUtils';
import DeleteFilterModal
  from '../../../../../main/frontend/dashboard/filter/deleteFilterModal/DeleteFilterModal';

describe('DeleteFilterModal', function() {
  let getShallowComponent, hideDeleteFilterModal, deleteFilter;

  beforeEach(function() {
    hideDeleteFilterModal = jasmine.createSpy('hideDeleteFilterModal');
    deleteFilter = jasmine.createSpy('deleteFilter');

    getShallowComponent = enzymeUtils.getShallowComponent(DeleteFilterModal, {
      filterToDelete: 'filter1',
      deleteFilterError: null,
      hideDeleteFilterModal,
      deleteFilter
    });
  });

  describe('when deleteFilterError is null', function() {

    it('renders warning message with the filter name as modal content', function() {
      const modalContent = getShallowComponent().find('.nx-modal-content');
      expect(modalContent.find('ForwardRef(NxWarningAlert)')).toHaveText(
          'You are about to delete "filter1" filter. This action can not be undone.');
    });

    it('renders footer without .nx-error class', function() {
      const footer = getShallowComponent().find('footer');
      expect(footer).not.toHaveClassName('nx-error');
    });

    it('renders footer with no NxErrorAlert', function() {
      const footer = getShallowComponent().find('footer');
      expect(footer.find(NxErrorAlert)).not.toExist();
    });

    it('renders submit button as primary with "Continue" text', function() {
      const submitButton = getShallowComponent().find(NxButton).at(0);
      expect(submitButton).toHaveProp('variant', 'primary');
      expect(submitButton).toHaveText('Continue');
    });
  });

  describe('when deleteFilterError is not null', function() {
    let component;

    beforeEach(function() {
      component = getShallowComponent({
        deleteFilterError: 'error123'
      });
    });

    it('does not render modal content', function() {
      const modalContent = component.find('.nx-modal-content');
      expect(modalContent).not.toExist();
    });

    it('renders footer with .nx-error class', function() {
      const footer = component.find('footer');
      expect(footer).toHaveClassName('nx-error');
    });

    it('renders footer with NxErrorAlert containing error message', function() {
      const footer = component.find('footer');
      expect(footer.childAt(0)).toContainReact(<NxErrorAlert>error123</NxErrorAlert>);
    });

    it('renders submit button as error variant with "Retry" text and faSync icon', function() {
      const submitButton = component.find(NxButton).at(0);
      expect(submitButton).toHaveProp('variant', 'error');
      expect(submitButton.childAt(0)).toContainReact(<NxFontAwesomeIcon icon={faSync}/>);
      expect(submitButton.childAt(1)).toHaveText('Retry');
    });
  });

  describe('NxSubmitMask', function() {
    it('is rendered when deleteFilterSaving is true', function() {
      const component = getShallowComponent({
        deleteFilterSaving: true,
        deleteFilterSuccess: false
      });

      expect(component.find('form').childAt(0)).toContainReact(
        <NxSubmitMask message="Removing…" success={false} />
      );
    });

    it('is rendered when deleteFilterSuccess is true', function() {
      const component = getShallowComponent({
        deleteFilterSaving: false,
        deleteFilterSuccess: true
      });

      expect(component.find('form').childAt(0)).toContainReact(
        <NxSubmitMask message="Removing…" success={true} />
      );
    });

    it('is not rendered when both deleteFilterSaving and deleteFilterSuccess are false', function() {
      const component = getShallowComponent({
        deleteFilterSaving: false,
        deleteFilterSuccess: false
      });

      expect(component.find(NxSubmitMask)).not.toExist();
    });
  });

  describe('onClose handler', function() {
    it('fires hideDeleteFilterModal action', function() {
      getShallowComponent().simulate('close');
      expect(hideDeleteFilterModal).toHaveBeenCalled();
    });
  });

  describe('onSubmit handler', function() {
    it('fires deleteFilter action with filterToDelete and calls preventDefault on the event', function() {
      const preventDefault = jasmine.createSpy('preventDefault');
      getShallowComponent().find('form').simulate('submit', {
        preventDefault
      });
      expect(preventDefault).toHaveBeenCalled();
      expect(deleteFilter).toHaveBeenCalledWith('filter1');
    });
  });

  describe('cancel button click handler', function() {
    it('fires hideDeleteFilterModal action and calls stopImmediatePropagation on the nativeEvent', function() {
      const stopImmediatePropagation = jasmine.createSpy('stopImmediatePropagation');
      const cancelButton = getShallowComponent().find(NxButton).at(1);
      cancelButton.simulate('click', {
        nativeEvent: {
          stopImmediatePropagation
        }
      });
      expect(stopImmediatePropagation).toHaveBeenCalled();
      expect(hideDeleteFilterModal).toHaveBeenCalled();
    });
  });
});
