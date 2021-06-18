/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import unsavedChangesModalModule from '../../../main/frontend/unsavedChangesModal/module';

describe('UnsavedChangesModalService', function () {
  let $rootScope, unsavedChangesModalService;

  beforeEach(angular.mock.module(unsavedChangesModalModule.name));

  beforeEach(inject(function (_$rootScope_, _unsavedChangesModalService_) {
    unsavedChangesModalService = _unsavedChangesModalService_;
    $rootScope = _$rootScope_;
  }));

  afterEach(() => {
    let element = document.getElementById('unsaved-changes-modal-wrapper');
    while (element != null) {
      element.parentNode.removeChild(element);
      element = document.getElementById('unsaved-changes-modal-wrapper');
    }
  });

  it('opens the unsaved changes modal when you call open()', (done) => {
    unsavedChangesModalService.open();
    expect(document.querySelector('#unsaved-changes-modal-wrapper .nx-modal')).toBeTruthy();

    // hack to get afterEach to execute in a separate event loop invocation to prevent NxModal from throwing
    // errors when it gets removed from the document before its useEffect hook gets a chance to fire
    setTimeout(done, 0);
  });

  it('does not open multiple copies of the unsaved changes modal', (done) => {
    const promise1 = unsavedChangesModalService.open();
    const promise2 = unsavedChangesModalService.open();
    expect(promise1).toBe(promise2);
    expect(document.querySelectorAll('#unsaved-changes-modal-wrapper').length).toEqual(1);
    expect(document.getElementsByClassName('nx-modal').length).toEqual(1);

    // hack to get afterEach to execute in a separate event loop invocation to prevent NxModal from throwing
    // errors when it gets removed from the document before its useEffect hook gets a chance to fire
    setTimeout(done, 0);
  });

  it('resolves the promise and closes the modal when you click continue', () => {
    const continueSpy = jasmine.createSpy();
    const cancelSpy = jasmine.createSpy();
    unsavedChangesModalService.open().then(continueSpy).catch(cancelSpy);

    const unsavedChangesModal = document.getElementById('unsaved-changes-modal-wrapper');
    const continueButton = unsavedChangesModal.querySelector('#unsaved-changes-modal-continue-button');
    continueButton.click();

    $rootScope.$digest();
    expect(document.getElementById('unsaved-changes-modal-wrapper')).toBeFalsy();
    expect(document.getElementById('unsaved-changes-modal')).toBeFalsy();
    expect(continueSpy).toHaveBeenCalled();
    expect(cancelSpy).not.toHaveBeenCalled();
  });

  it('rejects the promise and closes the modal when you click cancel', () => {
    const continueSpy = jasmine.createSpy();
    const cancelSpy = jasmine.createSpy();
    unsavedChangesModalService.open().then(continueSpy).catch(cancelSpy);

    const unsavedChangesModal = document.getElementById('unsaved-changes-modal-wrapper');
    const cancelButton = unsavedChangesModal.querySelector('#unsaved-changes-modal-cancel-button');
    cancelButton.click();

    $rootScope.$digest();
    expect(document.getElementById('unsaved-changes-modal-wrapper')).toBeFalsy();
    expect(document.getElementById('unsaved-changes-modal')).toBeFalsy();
    expect(continueSpy).not.toHaveBeenCalled();
    expect(cancelSpy).toHaveBeenCalled();
  });
});
