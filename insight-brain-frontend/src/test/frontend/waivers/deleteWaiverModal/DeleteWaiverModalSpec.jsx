/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxSubmitMask, NxButton, NxModal, NxLoadError } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../enzymeUtils';
import DeleteWaiverModal from '../../../../main/frontend/waivers/deleteWaiverModal/DeleteWaiverModal';

describe('DeleteWaiverModal', function () {
  let minimalProps, deleteWaiverSpy, hideDeleteWaiverModalSpy, getShallowComponent, getMountedComponent;

  beforeEach(function () {
    deleteWaiverSpy = jasmine.createSpy('deleteWaiver');
    hideDeleteWaiverModalSpy = jasmine.createSpy('hideDeleteWaiverModal');

    minimalProps = {
      waiverToDelete: {
        scopeOwnerId: 'App-name',
        scopeOwnerType: 'application',
        policyWaiverId: 'waiver-id',
      },
      deleteWaiver: deleteWaiverSpy,
      hideDeleteWaiverModal: hideDeleteWaiverModalSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(DeleteWaiverModal, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(DeleteWaiverModal, minimalProps);
  });

  it('renders an NxModal', function () {
    const component = getShallowComponent(),
      modal = component.find(NxModal);

    expect(modal).toExist();
    expect(modal).toHaveProp('id', 'delete-waiver-modal');
    expect(modal).toHaveProp('onClose');

    const modalTitle = component.find('.nx-modal-header');
    expect(modalTitle).toExist();
    expect(modalTitle).toIncludeText('Delete Waiver');

    const modalContent = component.find('.nx-modal-content');
    expect(modalContent).toExist();
    expect(modalContent).toHaveText('Are you sure you want to delete this waiver?');

    const modalFooter = component.find('.nx-footer');
    expect(modalFooter).toExist();

    const noButton = modalFooter.find('#delete-waiver-modal-cancel-button');
    expect(noButton).toMatchSelector(NxButton);
    expect(noButton).toHaveText('Cancel');
    expect(noButton).toHaveProp('onClick', hideDeleteWaiverModalSpy);

    const yesButton = modalFooter.find('#delete-waiver-modal-continue-button');
    expect(yesButton).toMatchSelector(NxButton);
    expect(yesButton).toHaveText('Delete Waiver');
    expect(yesButton.prop('onClick')).toEqual(jasmine.any(Function));
  });

  it('calls hideDeleteWaiverModal when No is clicked', function () {
    const component = getShallowComponent(),
      noButton = component.find('#delete-waiver-modal-cancel-button');

    noButton.simulate('click');
    expect(hideDeleteWaiverModalSpy).toHaveBeenCalled();
  });

  it('calls deleteWaiver with waiver data when Yes is clicked', function () {
    const component = getShallowComponent(),
      yesButton = component.find('#delete-waiver-modal-continue-button');

    yesButton.simulate('click');
    expect(deleteWaiverSpy).toHaveBeenCalledWith('application', 'App-name', 'waiver-id');
  });

  it('correctly handles the root org scenario when clicking Yes to delete the waiver', function () {
    const component = getShallowComponent({
      waiverToDelete: {
        scopeOwnerId: 'root',
        scopeOwnerType: 'root_organization',
        policyWaiverId: 'foo',
      },
    });
    const modal = component.find(NxModal);
    const yesButton = modal.find('#delete-waiver-modal-continue-button');
    yesButton.simulate('click');
    expect(deleteWaiverSpy).toHaveBeenCalledWith('organization', 'root', 'foo');
  });

  it('renders a submit Mask if the deletion is in progress', function () {
    const component = getMountedComponent({ deleteWaiverSaving: false }),
      submitMask = component.find(NxSubmitMask);

    expect(submitMask).toExist();
    expect(submitMask).toHaveText('Removing…');
  });

  it('renders a success mask if the deletion is succesful', function () {
    const component = getMountedComponent({ deleteWaiverSaving: true }),
      submitMask = component.find(NxSubmitMask);

    expect(submitMask).toExist();
    expect(submitMask).toHaveText('Success!');
  });

  it('renders an error if something went wrong', function () {
    const component = getMountedComponent({ deleteWaiverError: 'err!' }),
      err = component.find(NxLoadError);

    expect(err).toExist();
    expect(err).toHaveProp('error', 'err!');
    expect(err).toHaveProp('titleMessage', 'An error occurred deleting the waiver.');
    expect(err).toHaveProp('retryHandler');

    const yesButton = component.find('#delete-waiver-modal-continue-button');
    expect(yesButton).not.toExist();
  });
});
