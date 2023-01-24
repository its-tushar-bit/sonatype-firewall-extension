/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { fireEvent, render, screen } from 'TestRoot/SpecUtil';
import { actions } from 'MainRoot/user/LoginModal/userLoginSlice';

import LoginModal from 'MainRoot/user/LoginModal/LoginModal';
import * as userLoginSelectors from 'MainRoot/user/LoginModal/userLoginSelectors';
import * as routeSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import * as routerContext from 'MainRoot/react/RouterStateContext';

const { userInput, initialState } = nxTextInputStateHelpers;

describe('LoginModal', () => {
  const originalLoginStateSelector = userLoginSelectors.selectLoginModalState;

  let renderComponent,
    minimalProps,
    onSubmitSpy,
    onDismissSpy,
    onClickSSOSpy,
    setUsernameSpy,
    setPasswordSpy,
    useSelectorLoginStateSpy,
    useSelectorLoginSubmitStateSpy,
    mockRouteStateNameIncludesVulnerabilitySearch,
    loginState,
    loginSubmitState;

  beforeEach(() => {
    onSubmitSpy = jasmine.createSpy('onSubmit', () => {});
    onDismissSpy = jasmine.createSpy('onDismiss');
    onClickSSOSpy = jasmine.createSpy('onClickSSO');
    setUsernameSpy = spyOn(actions, 'setUsername').and.callThrough();
    setPasswordSpy = spyOn(actions, 'setPassword').and.callThrough();

    loginState = {
      username: initialState(''),
      password: initialState(''),
      isLicensed: true,
      showLoginModal: true,
      showSamlSso: false,
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

    minimalProps = {
      onSubmit: onSubmitSpy,
      onDismiss: onDismissSpy,
      onClickSSO: onClickSSOSpy,
    };

    useSelectorLoginSubmitStateSpy = spyOn(userLoginSelectors, 'selectLoginModalSubmitState').and.callThrough();
    useSelectorLoginStateSpy = spyOn(userLoginSelectors, 'selectLoginModalState').and.callFake((state) => {
      const originalSelection = originalLoginStateSelector(state);
      return { ...originalSelection, isLicensed: true, showLoginModal: true };
    });
    renderComponent = (additionalProps = {}) => render(<LoginModal {...minimalProps} {...additionalProps} />);
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
    spyOn(routeSelectors, 'selectRouterState').and.returnValue(mockRouteStateNameIncludesVulnerabilitySearch);
    useSelectorLoginStateSpy.and.returnValue(loginState);
    renderComponent();

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toBeNull();
  });

  it('does not render Vulnerability Lookup link when isUnauthenticatedPagesEnabled false', () => {
    useSelectorLoginStateSpy.and.callFake((state) => {
      const originalSelection = originalLoginStateSelector(state);
      return { ...originalSelection, isLicensed: true, showLoginModal: true, isUnauthenticatedPagesEnabled: false };
    });
    renderComponent();

    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toBeNull();
  });

  it('renders Vulnerability Lookup link in index but NOT cancel button when unauthenticated pages is enabled', () => {
    let hrefSpy = jasmine.createSpy('href').and.callFake((args) => `href-${args}`);
    spyOn(routeSelectors, 'selectRouterState').and.returnValue({ name: 'index' });
    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: hrefSpy,
    });
    useSelectorLoginStateSpy.and.returnValue(loginState);
    renderComponent();

    expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toBeVisible();
  });

  it('gets the href for Vulnerability Link', () => {
    spyOn(routeSelectors, 'selectRouterState').and.returnValue({ name: 'index' });
    useSelectorLoginStateSpy.and.returnValue(loginState);
    let hrefSpy = jasmine.createSpy('href').and.callFake((args) => `href-${args}`);
    let includesSpy = jasmine.createSpy('includes').and.returnValue(false);
    spyOn(routerContext, 'useRouterState').and.returnValue({
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
    spyOn(routeSelectors, 'selectRouterState').and.returnValue(mockRouteStateNameIncludesVulnerabilitySearch);
    useSelectorLoginStateSpy.and.callFake((state) => {
      const originalSelection = originalLoginStateSelector(state);
      return { ...originalSelection, isLicensed: false, showLoginModal: true };
    });
    renderComponent();

    expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
    expect(screen.queryByRole('link', { name: 'Vulnerability Lookup' })).toBeNull();
  });

  it('renders a system notice alert when one is enabled', () => {
    spyOn(userLoginSelectors, 'selectSystemNoticeServerData').and.returnValue({
      enabled: true,
      message: 'test notice',
    });
    renderComponent();

    expect(screen.getByText('test notice')).toBeVisible();
  });

  it('does not render a system notice alert when not enabled', () => {
    spyOn(userLoginSelectors, 'selectSystemNoticeServerData').and.returnValue({
      enabled: false,
      message: 'test notice',
    });
    renderComponent();
    expect(screen.queryByText('test notice')).toBeNull();
  });

  it('still renders login modal when system notice is not configured', () => {
    spyOn(userLoginSelectors, 'selectSystemNoticeServerData').and.returnValue(undefined);
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

    it('dismisses the modal when cancel button is clicked', () => {
      spyOn(routeSelectors, 'selectRouterState').and.returnValue(mockRouteStateNameIncludesVulnerabilitySearch);
      useSelectorLoginStateSpy.and.returnValue({ ...loginState, showSamlSso: true });
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
      it('renders login modal with "single sign-on (sso)" button if showSamlSso is true', () => {
        useSelectorLoginStateSpy.and.returnValue({ ...loginState, showSamlSso: true });
        renderComponent();
        expect(screen.getByRole('heading', { name: 'Sign in' })).toBeVisible();
        expect(screen.getByText('Single Sign-On (SSO)')).toBeVisible();
      });

      it('renders login modal without the "single sign-on (sso)" button if showSamlSso is false', () => {
        useSelectorLoginStateSpy.and.returnValue({ ...loginState, showSamlSso: false });
        renderComponent();
        expect(screen.getByRole('heading', { name: 'Sign in' })).toBeVisible();
        expect(() => screen.getByText('Single Sign-On (SSO)')).toThrowError();
      });

      it('redirects to sso login when "single sign-on (sso)" button is clicked', () => {
        useSelectorLoginStateSpy.and.returnValue({ ...loginState, showSamlSso: true });
        renderComponent();

        const ssoButton = screen.getByText('Single Sign-On (SSO)');
        fireEvent.click(ssoButton);

        expect(onClickSSOSpy).toHaveBeenCalled();
      });
    });

    it('renders pending submit mask upon firing login request', () => {
      useSelectorLoginSubmitStateSpy.and.returnValue({ ...loginSubmitState, loginSubmitMaskState: false });
      renderComponent();

      expect(screen.getByRole('status')).toBeVisible();
      expect(screen.getByText('Submitting…')).toBeVisible();
    });

    it('renders success submit mask upon successful login request', () => {
      useSelectorLoginSubmitStateSpy.and.returnValue({ ...loginSubmitState, loginSubmitMaskState: true });
      renderComponent();

      expect(screen.getByRole('status')).toBeVisible();
      expect(screen.getByText('Success!')).toBeVisible();
    });

    it('renders error alert with retry button if login error is thrown', () => {
      useSelectorLoginSubmitStateSpy.and.returnValue({
        ...loginSubmitState,
        loginSubmitError: 'Invalid credentials. Please try again.',
      });
      renderComponent();

      expect(screen.getByText('Invalid credentials. Please try again.', { exact: false })).toBeVisible();
      expect(screen.getByRole('button', { name: 'Retry' })).toBeVisible();
    });
  });
});
