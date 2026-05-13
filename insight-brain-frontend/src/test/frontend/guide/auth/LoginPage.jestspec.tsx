/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Theme } from '@radix-ui/themes';
import { LoginPage } from 'GuideRoot/auth/LoginPage';
import type { SsoConfig } from 'GuideRoot/auth/loginApi';

function renderLoginPage(overrides: {
  login?: (u: string, p: string) => Promise<void>;
  ssoConfig?: SsoConfig | null;
} = {}) {
  const defaultLogin = jest.fn().mockResolvedValue(undefined);
  const props = {
    login: overrides.login ?? defaultLogin,
    ssoConfig: overrides.ssoConfig ?? null,
  };
  render(
    <Theme appearance="dark" accentColor="indigo" panelBackground="solid">
      <LoginPage {...props} />
    </Theme>
  );
  return { login: props.login as jest.Mock };
}

describe('LoginPage', () => {
  it('renders a sign-in heading', () => {
    renderLoginPage();
    expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument();
  });

  it('renders username and password fields', () => {
    renderLoginPage();
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
  });

  it('renders a sign in button', () => {
    renderLoginPage();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('does not render SSO button when ssoConfig is null', () => {
    renderLoginPage({ ssoConfig: null });
    expect(screen.queryByRole('button', { name: /sso/i })).not.toBeInTheDocument();
  });

  it('renders SSO button when ssoConfig is present', () => {
    renderLoginPage({ ssoConfig: { type: 'SAML', loginUrl: '/saml/login' } });
    expect(screen.getByRole('button', { name: /sso/i })).toBeInTheDocument();
  });

  it('calls login with username and password on form submission', async () => {
    const user = userEvent.setup();
    const { login } = renderLoginPage();

    await user.type(screen.getByLabelText(/username/i), 'admin');
    await user.type(screen.getByLabelText(/password/i), 'admin123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(login).toHaveBeenCalledWith('admin', 'admin123');
  });

  it('disables the sign in button when fields are empty', () => {
    renderLoginPage();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeDisabled();
  });

  it('enables the sign in button when both fields have values', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await user.type(screen.getByLabelText(/username/i), 'admin');
    await user.type(screen.getByLabelText(/password/i), 'pass');

    expect(screen.getByRole('button', { name: /sign in/i })).toBeEnabled();
  });

  it('shows error message when login fails', async () => {
    const user = userEvent.setup();
    const failingLogin = jest.fn().mockRejectedValue(new Error('Invalid username or password'));
    renderLoginPage({ login: failingLogin });

    await user.type(screen.getByLabelText(/username/i), 'admin');
    await user.type(screen.getByLabelText(/password/i), 'wrong');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/invalid username or password/i);
    });
  });

  it('clears error message when user types again', async () => {
    const user = userEvent.setup();
    const failingLogin = jest.fn().mockRejectedValue(new Error('Invalid username or password'));
    renderLoginPage({ login: failingLogin });

    await user.type(screen.getByLabelText(/username/i), 'admin');
    await user.type(screen.getByLabelText(/password/i), 'wrong');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText(/username/i), 'x');

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('shows loading state on sign in button during submission', async () => {
    const user = userEvent.setup();
    let resolveLogin: () => void;
    const pendingLogin = jest.fn(
      () => new Promise<void>((resolve) => { resolveLogin = resolve; })
    );
    renderLoginPage({ login: pendingLogin });

    await user.type(screen.getByLabelText(/username/i), 'admin');
    await user.type(screen.getByLabelText(/password/i), 'admin123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(screen.getByRole('button', { name: /signing in/i })).toBeDisabled();

    resolveLogin!();
  });

  it('SSO button navigates to ssoConfig.loginUrl with origin=guide', async () => {
    const assignSpy = jest.fn();
    Object.defineProperty(window, 'location', {
      value: { ...window.location, hash: '', assign: assignSpy },
      writable: true,
    });

    const user = userEvent.setup();
    renderLoginPage({ ssoConfig: { type: 'SAML', loginUrl: '/saml/login' } });

    await user.click(screen.getByRole('button', { name: /sso/i }));

    expect(assignSpy).toHaveBeenCalledWith('http://localhost/saml/login?origin=guide');
  });

  it('SSO button appends current hash to loginUrl after origin', async () => {
    const assignSpy = jest.fn();
    Object.defineProperty(window, 'location', {
      value: { ...window.location, hash: '#/some/route', assign: assignSpy },
      writable: true,
    });

    const user = userEvent.setup();
    renderLoginPage({ ssoConfig: { type: 'SAML', loginUrl: '/saml/login' } });

    await user.click(screen.getByRole('button', { name: /sso/i }));

    expect(assignSpy).toHaveBeenCalledWith('http://localhost/saml/login?origin=guide&hash=%23%2Fsome%2Froute');
  });

  it('SSO button does not navigate for javascript: protocol URLs', async () => {
    const assignSpy = jest.fn();
    Object.defineProperty(window, 'location', {
      value: { ...window.location, hash: '', origin: 'http://localhost', assign: assignSpy },
      writable: true,
    });

    const user = userEvent.setup();
    renderLoginPage({ ssoConfig: { type: 'SAML', loginUrl: 'javascript:alert(1)' } as SsoConfig });

    await user.click(screen.getByRole('button', { name: /sso/i }));

    expect(assignSpy).not.toHaveBeenCalled();
  });

  it('SSO button preserves origin for absolute loginUrl', async () => {
    const assignSpy = jest.fn();
    Object.defineProperty(window, 'location', {
      value: { ...window.location, hash: '#/some/route', origin: 'http://localhost:8070', assign: assignSpy },
      writable: true,
    });

    const user = userEvent.setup();
    renderLoginPage({
      ssoConfig: { type: 'OIDC', loginUrl: 'https://keycloak.company.com/auth/realms/master/protocol/openid-connect/auth' },
    });

    await user.click(screen.getByRole('button', { name: /sso/i }));

    expect(assignSpy).toHaveBeenCalledWith(
      'https://keycloak.company.com/auth/realms/master/protocol/openid-connect/auth?origin=guide&hash=%23%2Fsome%2Froute'
    );
  });
});
