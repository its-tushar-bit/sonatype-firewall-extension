/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import loginModalModule from 'MainRoot/user/LoginModal/module';
import { actions } from 'MainRoot/user/LoginModal/userLoginSlice';
import * as Locations from 'MainRoot/util/CLMLocation';
import { getEnableSsoOnly, getSessionUrl } from 'MainRoot/util/CLMLocation';

describe('LoginModalService', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  let LoginModalService, $ngRedux, $rootScope;
  let loadIsSsoOnlyEnabledPromise, loadIsSsoOnlyEnabledWithValue;

  beforeEach(
    angular.mock.module(loginModalModule.name, ($provide) => {
      // provide redux so we can test action calls
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_LoginModalService_, _$ngRedux_, _$rootScope_) {
    LoginModalService = _LoginModalService_;
    $ngRedux = _$ngRedux_;
    $rootScope = _$rootScope_;

    $rootScope.licensed = true;

    loadIsSsoOnlyEnabledPromise = new Promise((resolve) => {
      loadIsSsoOnlyEnabledWithValue = resolve;
    });

    mockAxiosCalls({
      get: {
        [getEnableSsoOnly()]: loadIsSsoOnlyEnabledPromise,
      },
      post: {
        [getSessionUrl()]: Promise.resolve('success'),
      },
    });

    // This is needed for the actions submitUserLogin complete as expected
    window.Base64 = {
      encode: (args) => args,
    };
  }));

  it('opens the login modal when open() is called', (done) => {
    openLoginModalAndAssertItIsOpen(false, done);
  });

  it('does not open the login modal when open() is called with showSamlSso and isSsoOnlyEnabled', (done) => {
    spyOn(Locations, 'assign').and.stub();

    // Open the modal
    const modalPromise = LoginModalService.open(true);
    expect(modalPromise).toBeDefined();

    // When the isSsoOnlyEnabled flag is loaded
    loadIsSsoOnlyEnabledWithValue({
      data: ['enable-sso-only'],
    });

    // The action to get the isSsoOnlyEnabled flag is dispatched
    expect($ngRedux.actions.length).toBe(1);
    expect($ngRedux.actions).toHaveActionTypesInOrder(['productFeatures/loadIsSsoOnlyEnabled/pending']);
    expect(Locations.assign).toHaveBeenCalledTimes(0);

    // Adding timeout to wait until all the promises for the open modal are completed
    setTimeout(() => {
      expect($ngRedux.actions.length).toBe(2);
      expect($ngRedux.actions).toHaveActionTypesInOrder(['productFeatures/loadIsSsoOnlyEnabled/fulfilled']);
      expect(Locations.assign).toHaveBeenCalledTimes(1);
      expect($ngRedux.dispatch).not.toHaveBeenCalledWith(actions.setIsLicensed($rootScope.licensed));
      expect($ngRedux.dispatch).not.toHaveBeenCalledWith(actions.setShowLoginModal(true));
      expect($ngRedux.dispatch).not.toHaveBeenCalledWith(actions.setShowSamlSso(true));

      done();
    }, 0);
  });

  it('does open the login modal when open() is called with showSamlSso and not isSsoOnlyEnabled', (done) => {
    openLoginModalAndAssertItIsOpen(true, done);
  });

  it('does not open multiple copies of the modal', (done) => {
    // Open the modals
    const promise1 = LoginModalService.open(false);
    const promise2 = LoginModalService.open(true);
    expect(promise1).toBeDefined();
    expect(promise2).toBeDefined();

    // The isSsoOnlyEnabled feature is not enabled
    loadIsSsoOnlyEnabledWithValue({
      data: [],
    });

    // The action to load the isSsoOnlyEnabled is sent
    expect($ngRedux.actions.length).toBe(2);
    expect($ngRedux.actions).toHaveActionTypesInOrder([
      'productFeatures/loadIsSsoOnlyEnabled/pending',
      'productFeatures/loadIsSsoOnlyEnabled/pending',
    ]);

    // Adding timeout to wait until all the promises for the open modal are completed
    setTimeout(() => {
      expect($ngRedux.actions.length).toBe(7);
      expect($ngRedux.actions).toHaveActionTypesInOrder([
        'productFeatures/loadIsSsoOnlyEnabled/pending',
        'productFeatures/loadIsSsoOnlyEnabled/pending',
        'productFeatures/loadIsSsoOnlyEnabled/fulfilled',
        'productFeatures/loadIsSsoOnlyEnabled/fulfilled',
        'userLogin/setIsLicensed',
        'userLogin/setShowLoginModal',
        'userLogin/setShowSamlSso',
      ]);

      done();
    }, 0);
  });

  it('dismiss the modal when dismiss() is called', (done) => {
    LoginModalService.dismiss();
    expect($ngRedux.actions.length).toBe(1);
    expect($ngRedux.actions).toHaveActionTypesInOrder(['userLogin/resetLoginSubmitState']);
    expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.resetLoginSubmitState());

    done();
  });

  function openLoginModalAndAssertItIsOpen(showSamlSso, done) {
    // Open the modal
    const modalPromise = LoginModalService.open(showSamlSso);
    expect(modalPromise).toBeDefined();

    // The isSsoOnlyEnabled feature is not enabled
    loadIsSsoOnlyEnabledWithValue({
      data: [],
    });

    // The action to load the isSsoOnlyEnabled is sent
    expect($ngRedux.actions.length).toBe(1);
    expect($ngRedux.actions).toHaveActionTypesInOrder(['productFeatures/loadIsSsoOnlyEnabled/pending']);

    // Adding timeout to wait until all the promises for the open modal are completed
    setTimeout(() => {
      expect($ngRedux.actions.length).toBe(5);
      expect($ngRedux.actions).toHaveActionTypesInOrder([
        'productFeatures/loadIsSsoOnlyEnabled/pending',
        'productFeatures/loadIsSsoOnlyEnabled/fulfilled',
        'userLogin/setIsLicensed',
        'userLogin/setShowLoginModal',
        'userLogin/setShowSamlSso',
      ]);
      expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setIsLicensed($rootScope.licensed));
      expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setShowLoginModal(true));
      expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setShowSamlSso(showSamlSso));

      done();
    }, 0);
  }
});
