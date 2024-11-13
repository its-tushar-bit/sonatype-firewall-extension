/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import logoutWarningModalModule from 'MainRoot/utility/services/logoutWarningModal/module';
import * as notificationService from 'MainRoot/utility/services/notificationService';
import { getSessionUrl } from 'MainRoot/util/CLMLocation';

describe('logoutWarningModalService', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let $rootScope, logoutWarningModalService;

  beforeEach(
    angular.mock.module(logoutWarningModalModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_$rootScope_, _logoutWarningModalService_) {
    $rootScope = _$rootScope_;
    logoutWarningModalService = _logoutWarningModalService_;
    spyOn(notificationService, 'showNotification');
  }));

  afterEach(() => {
    let element = document.getElementById('logout-warning-container');
    while (element != null) {
      element.parentNode.removeChild(element);
      element = document.getElementById('logout-warning-container');
    }
  });

  it('opens the logout warning modal when you call open', (done) => {
    logoutWarningModalService.open(2);
    expect(document.querySelector('#logout-warning-container .nx-modal')).toBeTruthy();

    // hack to get afterEach to execute in a separate event loop invocation to prevent NxModal from throwing
    // errors when it gets removed from the document before its useEffect hook gets a chance to fire
    setTimeout(done, 0);
  });

  it('does not open multiple copies of the modal', (done) => {
    const promise1 = logoutWarningModalService.open(3);
    const promise2 = logoutWarningModalService.open(4);
    expect(promise1).toBe(promise2);
    expect(document.querySelectorAll('#logout-warning-container').length).toEqual(1);
    expect(document.getElementsByClassName('nx-modal').length).toEqual(1);

    // hack to get afterEach to execute in a separate event loop invocation to prevent NxModal from throwing
    // errors when it gets removed from the document before its useEffect hook gets a chance to fire
    setTimeout(done, 0);
  });

  it('uses the value passed to `open` to initialize the count in the modal', (done) => {
    logoutWarningModalService.open(120);
    const alertContent = document.querySelector('.nx-alert__content');
    expect(alertContent.textContent).toEqual('Due to inactivity you will be logged out in 120 seconds.');
    setTimeout(done, 0);
  });

  it('calls showNotification with the productName passed to the `open` method', (done) => {
    logoutWarningModalService.open(120, 'ProductName');
    expect(notificationService.showNotification).toHaveBeenCalledWith('Session Timeout Warning', {
      body: `Your ProductName session will expire in 2 minutes due to inactivity.`,
    });
    setTimeout(done, 0);
  });

  it('closes the modal and fires a request when you click continue', (done) => {
    const sessionUrl = getSessionUrl();
    mockAxiosCalls({
      get: {
        [sessionUrl]: Promise.resolve(),
      },
    });

    const promise = logoutWarningModalService.open(120);

    const modal = document.getElementById('logout-warning-modal');
    const btn = modal.querySelector('#logout-warning-modal-extend-btn');
    btn.click();

    $rootScope.$digest();
    expect(axios.get).toHaveBeenCalledWith(sessionUrl);
    expect(document.getElementById('logout-warning-container')).toBeFalsy();
    expect(document.getElementById('logout-warning-modal')).toBeFalsy();
    promise.then(() => {
      done();
    });
  });
});
