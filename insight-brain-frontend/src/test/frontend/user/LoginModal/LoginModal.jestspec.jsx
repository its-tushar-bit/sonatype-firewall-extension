/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, fireEvent, render, screen, waitFor } from 'TestRoot/SpecUtil';
import { actions } from 'MainRoot/user/LoginModal/userLoginSlice';

import LoginModal from 'MainRoot/user/LoginModal/LoginModal';
import * as userLoginSelectors from 'MainRoot/user/LoginModal/userLoginSelectors';
import * as routeSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import * as Locations from 'MainRoot/util/CLMLocation';

const { userInput, initialState } = nxTextInputStateHelpers;

describe('LoginModal', () => {
  const originalLoginStateSelector = userLoginSelectors.selectLoginModalState;

  let renderComponent,
    onSubmitSpy,
    onDismissSpy,
    onClickSSOSpy,
    setUsernameSpy,
    setPasswordSpy,
    useSelectorLoginStateSpy,
    useSelectorLoginSubmitStateSpy,
    mockRouteStateNameIncludesVulnerabilitySearch,
    mockRouteStateNameIncludesQuaratineComponent,
    loginState,
    loginSubmitState;

  beforeEach(() => {
    onSubmitSpy = jest.spyOn(actions, 'onSubmit').mockReturnValue(() => Promise.resolve());
    onDismissSpy = jest.spyOn(actions, 'dismiss').mockReturnValue(() => {});
    onClickSSOSpy = jest.spyOn(actions, 'onClickSSO').mockReturnValue(() => Promise.resolve());
    setUsernameSpy = jest.spyOn(actions, 'setUsername');
    setPasswordSpy = jest.spyOn(actions, 'setPassword');

    loginState = {
      username: initialState(''),
      password: initialState(''),
      isLicensed: true,
      showLoginModal: true,
      showSso: false,
      isFormValid: false,
      isUnauthenticatedPagesEnabled: true,
    };

    loginSubmitState = {
      loginSubmitError: null,
      loginSubmitMaskState: null,
    };

    mockRouteStateNameIncludesVulnerabilitySearch = {
      name: 'vulnerabilitySearch',
    };

    mockRouteStateNameIncludesQuaratineComponent = {
      name: 'firewall.quarantinedComponentReport',
    };

    useSelectorLoginSubmitStateSpy = jest.spyOn(userLoginSelectors, 'selectLoginModalSubmitState');
    useSelectorLoginStateSpy = jest.spyOn(userLoginSelectors, 'selectLoginModalState').mockImplementation((state) => {
      const originalSelection = originalLoginStateSelector(state);
      return { ...originalSelection, isLicensed: true, showLoginModal: true };
    });
    renderComponent = (additionalProps = {}) => render(<LoginModal {...additionalProps} />);
  });

  it('renders login modal with correct content', () => {
    renderComponent();

    expect(screen.getByRole('dialog')).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeVisible();
    expect(screen.getByRole('textbox', { name: 'Username' })).toBeVisible();
    expect(screen.getByLabelText('Password')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeVisible();
  });

  it('does NOT render Vulnerability Lookup link but does render a cancel button if license exists and user is on a page that does not require authentication', () => {
    jest.spyOn(routeSelectors, 'selectRouterState').mockReturnValue(mockRouteStateNameIncludesVulnerabilitySearch);
    useSelectorLoginStateSpy.mockReturnValue(loginState);
    renderComponent();

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toBeNull();
  });

  it('does not render Vulnerability Lookup link when isUnauthenticatedPagesEnabled false', () => {
    useSelectorLoginStateSpy.mockImplementation((state) => {
      const originalSelection = originalLoginStateSelector(state);
      return { ...originalSelection, isLicensed: true, showLoginModal: true, isUnauthenticatedPagesEnabled: false };
    });
    renderComponent();

    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toBeNull();
  });

  it('renders Vulnerability Lookup link in index but NOT cancel button when unauthenticated pages is enabled', () => {
    let hrefSpy = jest.fn().mockImplementation((args) => `href-${args}`);
    jest.spyOn(routeSelectors, 'selectRouterState').mockReturnValue({ name: 'index' });
    jest.spyOn(routerContext, 'useRouterState').mockReturnValue({
      href: hrefSpy,
    });
    useSelectorLoginStateSpy.mockReturnValue(loginState);
    renderComponent();

    expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toBeVisible();
  });

  it('gets the href for Vulnerability Link', () => {
    jest.spyOn(routeSelectors, 'selectRouterState').mockReturnValue({ name: 'index' });
    useSelectorLoginStateSpy.mockReturnValue(loginState);
    let hrefSpy = jest.fn().mockImplementation((args) => `href-${args}`);
    let includesSpy = jest.fn().mockReturnValue(false);
    jest.spyOn(routerContext, 'useRouterState').mockReturnValue({
      href: hrefSpy,
      includes: includesSpy,
    });

    renderComponent();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toHaveAttribute(
      'href',
      'href-vulnerabilitySearch'
    );
  });

  it('does not render Vulnerability Lookup link or cancel button if unlicensed, even if on a page that does not require authentication', () => {
    jest.spyOn(routeSelectors, 'selectRouterState').mockReturnValue(mockRouteStateNameIncludesVulnerabilitySearch);
    useSelectorLoginStateSpy.mockImplementation((state) => {
      const originalSelection = originalLoginStateSelector(state);
      return { ...originalSelection, isLicensed: false, showLoginModal: true };
    });
    renderComponent();

    expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toBeNull();
  });

  it('renders Vulnerability Lookup link if not SBOM Manager only license', () => {
    let hrefSpy = jest.fn().mockImplementation((args) => `href-${args}`);
    jest.spyOn(routerContext, 'useRouterState').mockReturnValue({
      href: hrefSpy,
    });
    jest.spyOn(routeSelectors, 'selectRouterState').mockReturnValue({ name: 'index' });
    useSelectorLoginStateSpy.mockImplementation((state) => {
      const originalSelection = originalLoginStateSelector(state);
      return {
        ...originalSelection,
        showLoginModal: true,
        isLicensed: true,
        isUnauthenticatedPagesEnabled: true,
        products: ['Sonatype SBOM Manager', 'Sonatype Lifecycle'],
      };
    });
    renderComponent();

    expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toBeVisible();
  });

  it('does not render Vulnerability Lookup link if SBOM Manager only license', () => {
    let hrefSpy = jest.fn().mockImplementation((args) => `href-${args}`);
    jest.spyOn(routerContext, 'useRouterState').mockReturnValue({
      href: hrefSpy,
    });
    jest.spyOn(routeSelectors, 'selectRouterState').mockReturnValue({ name: 'index' });
    useSelectorLoginStateSpy.mockImplementation((state) => {
      const originalSelection = originalLoginStateSelector(state);
      return {
        ...originalSelection,
        showLoginModal: true,
        isLicensed: true,
        isUnauthenticatedPagesEnabled: true,
        products: ['Sonatype SBOM Manager'],
      };
    });
    renderComponent();

    expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toBeNull();
  });

  it('renders Vulnerability Lookup link if Firewall only license', () => {
    let hrefSpy = jest.fn().mockImplementation((args) => `href-${args}`);
    jest.spyOn(routerContext, 'useRouterState').mockReturnValue({
      href: hrefSpy,
    });
    jest.spyOn(routeSelectors, 'selectRouterState').mockReturnValue({ name: 'index' });
    useSelectorLoginStateSpy.mockImplementation((state) => {
      const originalSelection = originalLoginStateSelector(state);
      return {
        ...originalSelection,
        showLoginModal: true,
        isLicensed: true,
        isUnauthenticatedPagesEnabled: true,
        products: ['Sonatype Repository Firewall', 'Sonatype Firewall for Artifactory'],
      };
    });
    renderComponent();

    expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toHaveAttribute(
      'href',
      'href-firewall.vulnerabilitySearch'
    );
  });

  it('renders a system notice alert when one is enabled', () => {
    jest.spyOn(userLoginSelectors, 'selectSystemNoticeServerData').mockReturnValue({
      enabled: true,
      message: 'test notice',
    });
    renderComponent();

    expect(screen.getByText('test notice')).toBeVisible();
  });

  it('does not render a system notice alert when not enabled', () => {
    jest.spyOn(userLoginSelectors, 'selectSystemNoticeServerData').mockReturnValue({
      enabled: false,
      message: 'test notice',
    });
    renderComponent();
    expect(screen.queryByText('test notice')).toBeNull();
  });

  it('still renders login modal when system notice is not configured', () => {
    jest.spyOn(userLoginSelectors, 'selectSystemNoticeServerData').mockReturnValue(undefined);
    renderComponent();

    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeVisible();
  });

  describe('login workflow', () => {
    let usernameInput, passwordInput;
    const userInputValidator = (val) => {
      return val.length ? null : 'Required field';
    };

    beforeEach(() => {
      (usernameInput = 'testUser'), (passwordInput = 'testPassword');
    });

    it('shows validation errors and styles after user input, when required fields are not completed', () => {
      renderComponent();
      fireEvent.change(screen.getByRole('textbox'), { target: { value: usernameInput } });
      fireEvent.change(screen.getByLabelText('Password'), { target: { value: passwordInput } });

      // Verify no errors are showing
      expect(screen.queryByText('Required field')).toBeNull();

      // Remove input
      fireEvent.change(screen.getByRole('textbox'), { target: { value: '' } });
      fireEvent.change(screen.getByLabelText('Password'), { target: { value: '' } });

      expect(setUsernameSpy).toHaveBeenCalledWith(userInput(userInputValidator, ''));
      expect(setPasswordSpy).toHaveBeenCalledWith(userInput(userInputValidator, ''));

      expect(screen.getAllByText('Required field').length).toBe(2);
    });

    it('dismisses the modal when cancel button is clicked and route is vulnerabilitySearch', () => {
      jest.spyOn(routeSelectors, 'selectRouterState').mockReturnValue(mockRouteStateNameIncludesVulnerabilitySearch);
      useSelectorLoginStateSpy.mockReturnValue({ ...loginState, showSso: true });
      renderComponent();

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      fireEvent.click(cancelButton);

      expect(onDismissSpy).toHaveBeenCalledTimes(1);
    });

    it('dismisses the modal when cancel button is clicked and route is quarantinedComponentReport', () => {
      jest.spyOn(routeSelectors, 'selectRouterState').mockReturnValue(mockRouteStateNameIncludesQuaratineComponent);
      useSelectorLoginStateSpy.mockReturnValue({ ...loginState, showSso: true });
      renderComponent();

      const cancelButton = screen.getByRole('button', { name: 'Cancel' });
      fireEvent.click(cancelButton);

      expect(onDismissSpy).toHaveBeenCalledTimes(1);
    });

    it('submits the login form when submit button is clicked', () => {
      renderComponent();

      fireEvent.change(screen.getByRole('textbox'), { target: { value: usernameInput } });
      fireEvent.change(screen.getByLabelText('Password'), { target: { value: passwordInput } });

      expect(setUsernameSpy).toHaveBeenCalledWith(userInput(userInputValidator, usernameInput));
      expect(setPasswordSpy).toHaveBeenCalledWith(userInput(userInputValidator, passwordInput));

      const submitButton = screen.getByRole('button', { name: 'Sign in' });
      fireEvent.click(submitButton);

      expect(onSubmitSpy).toHaveBeenCalledTimes(1);
    });

    describe('saml SSO Button tests', () => {
      it('renders login modal with "single sign-on (sso)" button if showSso is true', () => {
        useSelectorLoginStateSpy.mockReturnValue({ ...loginState, showSso: true });
        renderComponent();
        expect(screen.getByRole('heading', { name: 'Sign in' })).toBeVisible();
        expect(screen.getByText('Single Sign-On (SSO)')).toBeVisible();
      });

      it('renders login modal without the "single sign-on (sso)" button if showSso is false', () => {
        useSelectorLoginStateSpy.mockReturnValue({ ...loginState, showSso: false });
        renderComponent();
        expect(screen.getByRole('heading', { name: 'Sign in' })).toBeVisible();
        expect(() => screen.getByText('Single Sign-On (SSO)')).toThrowError();
      });

      it('redirects to sso login when "single sign-on (sso)" button is clicked', () => {
        useSelectorLoginStateSpy.mockReturnValue({ ...loginState, showSso: true });
        renderComponent();

        const ssoButton = screen.getByText('Single Sign-On (SSO)');
        fireEvent.click(ssoButton);

        expect(onClickSSOSpy).toHaveBeenCalled();
      });
    });

    it('renders pending submit mask upon firing login request', () => {
      useSelectorLoginSubmitStateSpy.mockReturnValue({ ...loginSubmitState, loginSubmitMaskState: false });
      renderComponent();

      expect(screen.getByRole('status')).toBeVisible();
      expect(screen.getByText('Submitting…')).toBeVisible();
    });

    it('renders success submit mask upon successful login request', () => {
      useSelectorLoginSubmitStateSpy.mockReturnValue({ ...loginSubmitState, loginSubmitMaskState: true });
      renderComponent();

      expect(screen.getByRole('status')).toBeVisible();
      expect(screen.getByText('Success!')).toBeVisible();
    });

    it('renders error alert with retry button if login error is thrown', () => {
      useSelectorLoginSubmitStateSpy.mockReturnValue({
        ...loginSubmitState,
        loginSubmitError: 'Invalid credentials. Please try again.',
      });
      renderComponent();

      expect(screen.getByText('Invalid credentials. Please try again.', { exact: false })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
    });
  });

  describe('authenticate action integration tests', () => {
    let axiosMock, assignSpy;

    beforeAll(() => {
      axiosMock = axiosMockAdapter();
    });

    beforeEach(() => {
      assignSpy = jest.spyOn(Locations, 'assign').mockImplementation(() => {});
    });

    afterEach(() => {
      assignSpy.mockRestore();
    });

    it('opens login modal when authenticate is called without SSO', async () => {
      axiosMock.onGet(Locations.getEnableSsoOnly()).reply(200, []);

      const { store } = render(<LoginModal />);

      await store.dispatch(actions.authenticate(null, null));

      await waitFor(() => {
        const state = store.getState();
        expect(state.userLogin.loginModalState.showLoginModal).toBe(true);
        expect(state.userLogin.loginModalState.showSso).toBeFalsy();
      });
    });

    it('opens login modal with SSO button when SAML is available and SSO-only is disabled', async () => {
      axiosMock.onGet(Locations.getEnableSsoOnly()).reply(200, []);

      const { store } = render(<LoginModal />);

      await store.dispatch(actions.authenticate('SAML', '/saml/login'));

      await waitFor(() => {
        const state = store.getState();
        expect(state.userLogin.loginModalState.showLoginModal).toBe(true);
        expect(state.userLogin.loginModalState.showSso).toBe(true);
        expect(state.userLogin.loginModalState.ssoLoginUrl).toBe('/saml/login');
      });
    });

    it('opens login modal with SSO button when OIDC is available and SSO-only is disabled', async () => {
      axiosMock.onGet(Locations.getEnableSsoOnly()).reply(200, []);

      const { store } = render(<LoginModal />);

      await store.dispatch(actions.authenticate('OIDC', '/oidc/login'));

      await waitFor(() => {
        const state = store.getState();
        expect(state.userLogin.loginModalState.showLoginModal).toBe(true);
        expect(state.userLogin.loginModalState.showSso).toBe(true);
        expect(state.userLogin.loginModalState.ssoLoginUrl).toBe('/oidc/login');
      });
    });

    it('redirects to IdP when SSO-only is enabled and SAML is available', async () => {
      axiosMock.onGet(Locations.getEnableSsoOnly()).reply(200, ['enable-sso-only']);

      const { store } = render(<LoginModal />);

      await store.dispatch(actions.authenticate('SAML', '/saml/login'));

      await waitFor(() => {
        expect(assignSpy).toHaveBeenCalledWith('/saml/login');
      });
    });

    it('redirects to IdP when SSO-only is enabled and OIDC is available', async () => {
      axiosMock.onGet(Locations.getEnableSsoOnly()).reply(200, ['enable-sso-only']);

      const { store } = render(<LoginModal />);

      await store.dispatch(actions.authenticate('OIDC', '/oidc/login'));

      await waitFor(() => {
        expect(assignSpy).toHaveBeenCalledWith('/oidc/login');
      });
    });

    it('opens login modal even when SSO-only is enabled if on backupLogin route', async () => {
      axiosMock.onGet(Locations.getEnableSsoOnly()).reply(200, ['enable-sso-only']);
      window.location.hash = '#/backupLogin';

      const { store } = render(<LoginModal />);

      await store.dispatch(actions.authenticate('SAML', '/saml/login'));

      await waitFor(() => {
        const state = store.getState();
        expect(state.userLogin.loginModalState.showLoginModal).toBe(true);
        expect(assignSpy).not.toHaveBeenCalled();
      });

      window.location.hash = '';
    });

    it('redirects to SSO with hash parameter when hash is present in URL', async () => {
      axiosMock.onGet(Locations.getEnableSsoOnly()).reply(200, ['enable-sso-only']);
      window.location.hash = '#/applications/app123/report/scanId456';

      const { store } = render(<LoginModal />);

      await store.dispatch(actions.authenticate('SAML', '/saml/login'));

      const expectedHash = encodeURIComponent('#/applications/app123/report/scanId456');
      await waitFor(() => {
        expect(assignSpy).toHaveBeenCalledWith(`/saml/login?hash=${expectedHash}`);
      });

      window.location.hash = '';
    });
  });
});
