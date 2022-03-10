/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import loginModalModule from 'MainRoot/user/LoginModal/module';
import { actions } from 'MainRoot/user/LoginModal/userLoginSlice';

describe('LoginModalService', function () {
  let LoginModalService, $ngRedux, $rootScope;

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

    $ngRedux.dispatch = jasmine.createSpy('dispatch');
  }));

  it('opens the login modal when open() is called', () => {
    LoginModalService.open(false);
    expect($ngRedux.dispatch).toHaveBeenCalledTimes(3);
    expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setIsLicensed($rootScope.licensed));
    expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setShowLoginModal(true));
    expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setShowSamlSso(false));
  });

  it('does not open multiple copies of the modal', () => {
    const promise1 = LoginModalService.open(false);
    const promise2 = LoginModalService.open(true);
    expect(promise1).toBe(promise2);
    expect($ngRedux.dispatch).toHaveBeenCalledTimes(3);
  });

  it('dismiss the modal when dismiss() is called', () => {
    LoginModalService.dismiss();
    expect($ngRedux.dispatch).toHaveBeenCalledTimes(1);
    expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.resetLoginSubmitState());
  });
});
