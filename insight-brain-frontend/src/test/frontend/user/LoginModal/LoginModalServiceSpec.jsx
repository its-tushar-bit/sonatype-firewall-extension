/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import loginModalModule from 'MainRoot/user/LoginModal/module';
import { actions } from 'MainRoot/user/LoginModal/userLoginSlice';
import * as Locations from 'MainRoot/util/CLMLocation';
import { getEnableSsoOnly, getOAuth2Enabled, getSessionUrl } from 'MainRoot/util/CLMLocation';

describe('LoginModalService', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

  let LoginModalService, $ngRedux, $rootScope;
  let loadOAuth2EnabledWithValue, loadIsSsoOnlyEnabledWithValue;

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

    mockAxiosCalls({
      get: {
        [getOAuth2Enabled()]: new Promise((resolve) => {
          loadOAuth2EnabledWithValue = resolve;
        }),
        [getEnableSsoOnly()]: new Promise((resolve) => {
          loadIsSsoOnlyEnabledWithValue = resolve;
        }),
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

  it('authenticate opens the login modal', (done) => {
    loadOAuth2EnabledWithValue({ data: [] });
    loadIsSsoOnlyEnabledWithValue({ data: [] });

    authenticateOpensLoginModalAndAssertItIsOpen(false, done);
  });

  it('authenticate opens the login modal if isSsoOnlyEnabled is disabled', (done) => {
    // The OAUTH2_ENABLED feature is enabled
    loadOAuth2EnabledWithValue({ data: ['oauth2-enabled'] });
    // The isSsoOnlyEnabled feature is not enabled
    loadIsSsoOnlyEnabledWithValue({ data: [] });

    authenticateOpensLoginModalAndAssertItIsOpen(true, done);
  });

  it('authenticate calls redirectToIdP if isSsoOnlyEnabled and showSamlSso is enabled', (done) => {
    // The OAUTH2_ENABLED feature is not enabled
    loadOAuth2EnabledWithValue({ data: [] });
    // The isSsoOnlyEnabled feature is enabled
    loadIsSsoOnlyEnabledWithValue({ data: ['enable-sso-only'] });

    assertAuthenticateCallsRedirectToIdPAndRedirectsUser(true, '/saml/login', done);
  });

  it('authenticate calls redirectToIdP if isSsoOnlyEnabled is enabled and loadOAuth2Enabled', (done) => {
    // The OAUTH2_ENABLED feature is enabled
    loadOAuth2EnabledWithValue({ data: ['oauth2-enabled'] });
    // The isSsoOnlyEnabled feature is enabled
    loadIsSsoOnlyEnabledWithValue({ data: ['enable-sso-only'] });

    assertAuthenticateCallsRedirectToIdPAndRedirectsUser(false, '/oidc/login', done);
  });

  it('opens the login modal when open() is called', (done) => {
    openLoginModalAndAssertItIsOpen(false, done);
  });

  it('does open the login modal when open() is called with showSamlSso', (done) => {
    openLoginModalAndAssertItIsOpen(true, done);
  });

  it('does not open multiple copies of the modal', (done) => {
    // Open the modals
    const promise1 = LoginModalService.open(false);
    const promise2 = LoginModalService.open(true);
    expect(promise1).toBeDefined();
    expect(promise2).toBeDefined();

    // Adding timeout to wait until all the promises for the open modal are completed
    setTimeout(() => {
      expect($ngRedux.actions.length).toBe(3);
      expect($ngRedux.actions).toHaveActionTypesInOrder([
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

  it('redirects to SAML SSO URL', (done) => {
    assertUserIsRedirectedToSSOUrl('/saml/login', false, done);
  });

  it('redirects to OAuth2 SSO URL', (done) => {
    assertUserIsRedirectedToSSOUrl('/oidc/login', true, done);
  });

  it('redirects to SAML SSO URL when click on SSO button', (done) => {
    assertUserIsRedirectedToSSOUrlWhenSSOButtonIsClicked('/saml/login', [], done);
  });

  it('redirects to OAuth2 SSO URL when click on SSO button', (done) => {
    assertUserIsRedirectedToSSOUrlWhenSSOButtonIsClicked('/oidc/login', ['oauth2-enabled'], done);
  });

  function authenticateOpensLoginModalAndAssertItIsOpen(showSamlSso, done) {
    // Open the modal
    const authenticatePromise = LoginModalService.authenticate(showSamlSso);
    expect(authenticatePromise).toBeDefined();

    // Adding timeout to wait until all the promises for the open modal are completed
    setTimeout(() => {
      assertLoginModalIsOpen(showSamlSso);
      done();
    }, 0);
  }

  function assertLoginModalIsOpen(showSamlSso) {
    expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setIsLicensed($rootScope.licensed));
    expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setShowLoginModal(true));
    expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setShowSamlSso(showSamlSso));
  }

  function assertAuthenticateCallsRedirectToIdPAndRedirectsUser(showSamlSso, expectedUrl, done) {
    spyOn(Locations, 'assign').and.stub();

    // Open the modal
    const authenticatePromise = LoginModalService.authenticate(showSamlSso);
    expect(authenticatePromise).toBeDefined();

    // Adding timeout to wait until all the promises to redirect to the SSO is completed
    setTimeout(() => {
      expect(Locations.assign).toHaveBeenCalledWith(expectedUrl);
      done();
    }, 0);
  }

  function openLoginModalAndAssertItIsOpen(showSamlSso, done) {
    // Open the modal
    const modalPromise = LoginModalService.open(showSamlSso);
    expect(modalPromise).toBeDefined();

    // Adding timeout to wait until all the promises for the open modal are completed
    setTimeout(() => {
      expect($ngRedux.actions.length).toBe(3);
      expect($ngRedux.actions).toHaveActionTypesInOrder([
        'userLogin/setIsLicensed',
        'userLogin/setShowLoginModal',
        'userLogin/setShowSamlSso',
      ]);
      assertLoginModalIsOpen(showSamlSso);
      done();
    }, 0);
  }

  function assertUserIsRedirectedToSSOUrl(expectedUrl, isOAuth2Enabled, done) {
    spyOn(Locations, 'assign').and.stub();

    // Open the modal
    const modalPromise = LoginModalService.redirectToIdP(isOAuth2Enabled);
    expect(modalPromise).toBeDefined();

    // Adding timeout to wait until all the promises to redirect to the SSO is completed
    setTimeout(() => {
      expect(Locations.assign).toHaveBeenCalledWith(expectedUrl);

      done();
    }, 0);
  }

  function assertUserIsRedirectedToSSOUrlWhenSSOButtonIsClicked(expectedUrl, isOAuth2EnabledData, done) {
    spyOn(Locations, 'assign').and.stub();

    // Open the modal
    const modalPromise = LoginModalService.onClickSSO();
    expect(modalPromise).toBeDefined();

    // The OAUTH2_ENABLED feature is not enabled
    loadOAuth2EnabledWithValue({
      data: isOAuth2EnabledData,
    });

    // The action to get the OAUTH2_ENABLED flag is dispatched
    expect($ngRedux.actions.length).toBe(1);
    expect($ngRedux.actions).toHaveActionTypesInOrder(['productFeatures/loadIsOauth2Enabled/pending']);
    expect(Locations.assign).toHaveBeenCalledTimes(0);

    // Adding timeout to wait until all the promises for the open modal are completed
    setTimeout(() => {
      expect($ngRedux.actions.length).toBe(2);
      expect($ngRedux.actions).toHaveActionTypesInOrder(['productFeatures/loadIsOauth2Enabled/fulfilled']);
      expect(Locations.assign).toHaveBeenCalledWith(expectedUrl);

      done();
    }, 0);
  }
});
