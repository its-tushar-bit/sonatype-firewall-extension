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

  let LoginModalService, $ngRedux, $window;
  let loadIsSsoOnlyEnabledWithValue;

  beforeEach(
    angular.mock.module(loginModalModule.name, ($provide) => {
      // provide redux so we can test action calls
      SpecUtil.mockNgRedux($provide);
      $provide.value('$window', {
        location: {
          hash: '',
        },
      });
    })
  );

  beforeEach(inject(function (_LoginModalService_, _$ngRedux_, _$window_) {
    LoginModalService = _LoginModalService_;
    $ngRedux = _$ngRedux_;
    $window = _$window_;

    // Mock Redux state with license information
    $ngRedux.getState.and.returnValue({
      productLicense: {
        installed: true,
        license: {
          products: [],
        },
      },
      userLogin: {
        loginModalState: {
          isUnauthenticatedPagesEnabled: undefined,
          isQuarantinedComponentViewAnonymousAccessEnabled: undefined,
        },
      },
    });

    mockAxiosCalls({
      get: {
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

  it('authenticate opens the login modal with no SSO', (done) => {
    loadIsSsoOnlyEnabledWithValue({ data: [] });

    authenticateOpensLoginModalAndAssertItIsOpen(null, null, null, done);
  });

  it('authenticate opens the login modal if SAML is available but isSsoOnlyEnabled is disabled', (done) => {
    // The isSsoOnlyEnabled feature is not enabled
    loadIsSsoOnlyEnabledWithValue({ data: [] });

    authenticateOpensLoginModalAndAssertItIsOpen('SAML', true, '/saml/login', done);
  });

  it('authenticate opens the login modal if OIDC is available but isSsoOnlyEnabled is disabled', (done) => {
    // The isSsoOnlyEnabled feature is not enabled
    loadIsSsoOnlyEnabledWithValue({ data: [] });

    authenticateOpensLoginModalAndAssertItIsOpen('OIDC', true, '/oidc/login', done);
  });

  it('authenticate calls redirectToIdP if isSsoOnlyEnabled and SAML is available', (done) => {
    // The isSsoOnlyEnabled feature is enabled
    loadIsSsoOnlyEnabledWithValue({ data: ['enable-sso-only'] });

    assertAuthenticateCallsRedirectToIdPAndRedirectsUser('SAML', '/saml/login', done);
  });

  it('authenticate calls redirectToIdP if isSsoOnlyEnabled is enabled and OIDC is available', (done) => {
    // The isSsoOnlyEnabled feature is enabled
    loadIsSsoOnlyEnabledWithValue({ data: ['enable-sso-only'] });

    assertAuthenticateCallsRedirectToIdPAndRedirectsUser('OIDC', '/oidc/login', done);
  });

  describe('when current route is "backupLogin"', () => {
    beforeEach(() => {
      $window.location.hash = '#/backupLogin';
    });

    it('authenticate opens the login modal if isSsoOnlyEnabled and SAML is available', (done) => {
      // The isSsoOnlyEnabled feature is enabled
      loadIsSsoOnlyEnabledWithValue({ data: ['enable-sso-only'] });

      authenticateOpensLoginModalAndAssertItIsOpen('SAML', true, '/saml/login', done);
    });

    it('authenticate opens the login modal if isSsoOnlyEnabled and OIDC is available', (done) => {
      // The isSsoOnlyEnabled feature is enabled
      loadIsSsoOnlyEnabledWithValue({ data: ['enable-sso-only'] });

      authenticateOpensLoginModalAndAssertItIsOpen('OIDC', true, '/oidc/login', done);
    });
  });

  it('opens the login modal when open() is called without SSO', (done) => {
    openLoginModalAndAssertItIsOpen(false, null, done);
  });

  it('does open the login modal when open() is called with SSO button visible', (done) => {
    openLoginModalAndAssertItIsOpen(true, '/saml/login', done);
  });

  it('does open the login modal when open() is called with OIDC', (done) => {
    openLoginModalAndAssertItIsOpen(true, '/oidc/login', done);
  });

  it('does not open multiple copies of the modal', (done) => {
    // Open the modals
    const promise1 = LoginModalService.open(false, null);
    const promise2 = LoginModalService.open(true, '/oidc/login');
    expect(promise1).toBeDefined();
    expect(promise2).toBeDefined();

    // Adding timeout to wait until all the promises for the open modal are completed
    setTimeout(() => {
      expect($ngRedux.actions.length).toBe(7);
      expect($ngRedux.actions).toHaveActionTypesInOrder([
        'userLogin/setIsLicensed',
        'userLogin/setProducts',
        'userLogin/setUnauthenticatedPagesEnabled',
        'userLogin/setQuarantinedComponentViewAnonymousAccessEnabled',
        'userLogin/setShowLoginModal',
        'userLogin/setShowSso',
        'userLogin/setSsoLoginUrl',
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
    assertUserIsRedirectedToSSOUrl('/saml/login', '/saml/login', done);
  });

  it('redirects to OAuth2 SSO URL', (done) => {
    assertUserIsRedirectedToSSOUrl('/oidc/login', '/oidc/login', done);
  });

  it('redirects to SAML SSO URL when click on SSO button', (done) => {
    assertUserIsRedirectedToSSOUrlWhenSSOButtonIsClicked('/saml/login', '/saml/login', done);
  });

  it('redirects to OAuth2 SSO URL when click on SSO button', (done) => {
    assertUserIsRedirectedToSSOUrlWhenSSOButtonIsClicked('/oidc/login', '/oidc/login', done);
  });

  it('redirects to SAML SSO URL with encoded hash parameter', (done) => {
    $window.location.hash = '#/applications/app123/report/scanId456';
    const expectedEncodedHash = encodeURIComponent('#/applications/app123/report/scanId456');
    assertUserIsRedirectedToSSOUrl(`/saml/login?hash=${expectedEncodedHash}`, '/saml/login', done);
  });

  it('redirects to OIDC SSO URL with encoded hash parameter', (done) => {
    $window.location.hash = '#/applications/app123/report/scanId456';
    const expectedEncodedHash = encodeURIComponent('#/applications/app123/report/scanId456');
    assertUserIsRedirectedToSSOUrl(`/oidc/login?hash=${expectedEncodedHash}`, '/oidc/login', done);
  });

  function authenticateOpensLoginModalAndAssertItIsOpen(wwwAuthenticateHeader, expectedShowSso, ssoLoginUrl, done) {
    // Open the modal - backend provides ssoLoginUrl
    const authenticatePromise = LoginModalService.authenticate(wwwAuthenticateHeader, ssoLoginUrl);
    expect(authenticatePromise).toBeDefined();

    // Adding timeout to wait until all the promises for the open modal are completed
    setTimeout(() => {
      assertLoginModalIsOpen(expectedShowSso, ssoLoginUrl);
      done();
    }, 0);
  }

  function assertLoginModalIsOpen(showSsoButton, ssoLoginUrl) {
    // Service reads license from Redux state (mocked as installed: true)
    expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setIsLicensed(true));
    expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setShowLoginModal(true));
    expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setShowSso(showSsoButton));
    if (ssoLoginUrl !== undefined) {
      expect($ngRedux.dispatch).toHaveBeenCalledWith(actions.setSsoLoginUrl(ssoLoginUrl));
    }
  }

  function assertAuthenticateCallsRedirectToIdPAndRedirectsUser(wwwAuthenticateHeader, expectedUrl, done) {
    spyOn(Locations, 'assign').and.stub();

    // Call authenticate with WWW-Authenticate header and ssoLoginUrl from backend
    const authenticatePromise = LoginModalService.authenticate(wwwAuthenticateHeader, expectedUrl);
    expect(authenticatePromise).toBeDefined();

    // Adding timeout to wait until all the promises to redirect to the SSO is completed
    setTimeout(() => {
      expect(Locations.assign).toHaveBeenCalledWith(expectedUrl);
      done();
    }, 0);
  }

  function openLoginModalAndAssertItIsOpen(showSsoButton, ssoLoginUrl, done) {
    // Open the modal
    const modalPromise = LoginModalService.open(showSsoButton, ssoLoginUrl);
    expect(modalPromise).toBeDefined();

    // Adding timeout to wait until all the promises for the open modal are completed
    setTimeout(() => {
      expect($ngRedux.actions.length).toBe(7);
      expect($ngRedux.actions).toHaveActionTypesInOrder([
        'userLogin/setIsLicensed',
        'userLogin/setProducts',
        'userLogin/setUnauthenticatedPagesEnabled',
        'userLogin/setQuarantinedComponentViewAnonymousAccessEnabled',
        'userLogin/setShowLoginModal',
        'userLogin/setShowSso',
        'userLogin/setSsoLoginUrl',
      ]);
      assertLoginModalIsOpen(showSsoButton, ssoLoginUrl);
      done();
    }, 0);
  }

  function assertUserIsRedirectedToSSOUrl(expectedUrl, ssoLoginUrl, done) {
    spyOn(Locations, 'assign').and.stub();

    // Call redirectToIdP with ssoLoginUrl from backend
    const modalPromise = LoginModalService.redirectToIdP(ssoLoginUrl);
    expect(modalPromise).toBeDefined();

    // Adding timeout to wait until all the promises to redirect to the SSO is completed
    setTimeout(() => {
      expect(Locations.assign).toHaveBeenCalledWith(expectedUrl);

      done();
    }, 0);
  }

  function assertUserIsRedirectedToSSOUrlWhenSSOButtonIsClicked(expectedUrl, ssoLoginUrl, done) {
    spyOn(Locations, 'assign').and.stub();

    // Set up Redux state to have ssoLoginUrl value from backend
    $ngRedux.getState = () => ({
      userLogin: {
        loginModalState: {
          ssoLoginUrl: ssoLoginUrl,
        },
      },
    });

    // Click SSO button
    const modalPromise = LoginModalService.onClickSSO();
    expect(modalPromise).toBeDefined();

    // Adding timeout to wait until all the promises for the redirect are completed
    setTimeout(() => {
      expect(Locations.assign).toHaveBeenCalledWith(expectedUrl);

      done();
    }, 0);
  }
});
