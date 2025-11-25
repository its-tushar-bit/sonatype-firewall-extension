/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen } from 'TestRoot/SpecUtil';
import UserTokenModal from 'MainRoot/mainHeader/MenuBar/UserMenu/UserToken/UserTokenModal';

describe('UserTokenModal', () => {
  let axiosMock;

  const defaultProps = {
    checkUserTokenExistence: jest.fn(),
    generateUserToken: jest.fn(),
    deleteUserToken: jest.fn(),
    hideUserTokenModal: jest.fn(),
    fetchTokenExpirationConfig: jest.fn(),
    fetchTokenCreateTime: jest.fn(),
    checkUserTokenLoading: false,
    generateUserTokenLoading: null,
    deleteUserTokenLoading: null,
    checkUserTokenError: null,
    generateUserTokenError: null,
    deleteUserTokenError: null,
    userToken: null,
    tokenExpirationDays: null,
    tokenCreateTime: null,
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  const renderComponent = (props = {}) => {
    return render(<UserTokenModal {...defaultProps} {...props} />);
  };

  it('should display expiration info for existing token when config is set', () => {
    const now = Date.now();
    const createTime = now - 10 * 24 * 60 * 60 * 1000; // 10 days ago
    const expirationDays = 30;

    renderComponent({
      userToken: true,
      tokenExpirationDays: expirationDays,
      tokenCreateTime: createTime,
    });

    // Verify warning alert is shown
    expect(screen.getByText(/A user token already exists for this user/i)).toBeInTheDocument();

    // Verify expiration section is displayed
    expect(screen.getByText('User Token Status')).toBeInTheDocument();
    expect(screen.getByText('Time remaining until user token expires')).toBeInTheDocument();
    expect(screen.getByText(/Expires:/i)).toBeInTheDocument();
  });

  it('should not display expiration info when tokenExpirationDays is null', () => {
    renderComponent({
      userToken: true,
      tokenExpirationDays: null,
      tokenCreateTime: Date.now(),
    });

    // Verify warning alert is shown
    expect(screen.getByText(/A user token already exists for this user/i)).toBeInTheDocument();

    // Verify expiration section is NOT displayed
    expect(screen.queryByText('User Token Status')).not.toBeInTheDocument();
  });

  it('should not display expiration info when tokenCreateTime is null', () => {
    renderComponent({
      userToken: true,
      tokenExpirationDays: 30,
      tokenCreateTime: null,
    });

    // Verify warning alert is shown
    expect(screen.getByText(/A user token already exists for this user/i)).toBeInTheDocument();

    // Verify expiration section is NOT displayed
    expect(screen.queryByText('User Token Status')).not.toBeInTheDocument();
  });

  it('should call fetchTokenCreateTime when modal opens with existing token', () => {
    const fetchTokenCreateTimeSpy = jest.fn();

    renderComponent({
      userToken: true,
      tokenExpirationDays: 30,
      tokenCreateTime: null,
      fetchTokenCreateTime: fetchTokenCreateTimeSpy,
    });

    expect(fetchTokenCreateTimeSpy).toHaveBeenCalled();
  });

  it('should not call fetchTokenCreateTime when token does not exist', () => {
    const fetchTokenCreateTimeSpy = jest.fn();

    renderComponent({
      userToken: false,
      tokenExpirationDays: 30,
      tokenCreateTime: null,
      fetchTokenCreateTime: fetchTokenCreateTimeSpy,
    });

    expect(fetchTokenCreateTimeSpy).not.toHaveBeenCalled();
  });

  it('should display "Expired:" for expired tokens', () => {
    const now = Date.now();
    const createTime = now - 40 * 24 * 60 * 60 * 1000; // 40 days ago
    const expirationDays = 30;

    renderComponent({
      userToken: true,
      tokenExpirationDays: expirationDays,
      tokenCreateTime: createTime,
    });

    // Verify "Expired:" is shown instead of "Expires:"
    expect(screen.getByText(/Expired:/i)).toBeInTheDocument();
    expect(screen.queryByText(/Expires:/i)).not.toBeInTheDocument();
  });

  it('should format date in human-readable format with timezone', () => {
    const now = Date.now();
    const createTime = now - 10 * 24 * 60 * 60 * 1000; // 10 days ago
    const expirationDays = 30;

    renderComponent({
      userToken: true,
      tokenExpirationDays: expirationDays,
      tokenCreateTime: createTime,
    });

    // Verify date format contains expected elements (month, year, "at", timezone)
    const expirationDateElement = screen.getByText(/Expires:/i).parentElement;
    expect(expirationDateElement.textContent).toMatch(/\d{4}/); // Year
    expect(expirationDateElement.textContent).toMatch(/[A-Z][a-z]{2}/); // Month abbreviation (e.g., Jan, Feb)
    expect(expirationDateElement.textContent).toContain('at'); // "at" separator
    expect(expirationDateElement.textContent).toMatch(/[A-Z]{3,4}/); // Timezone abbreviation (e.g., EST, PST, UTC)
  });

  it('should display error alert when token is expired', () => {
    const now = Date.now();
    const createTime = now - 40 * 24 * 60 * 60 * 1000; // 40 days ago (expired)
    const expirationDays = 30;

    const { container } = renderComponent({
      userToken: true,
      tokenExpirationDays: expirationDays,
      tokenCreateTime: createTime,
    });

    // Verify error alert is displayed with correct text and ID
    const errorAlert = container.querySelector('#user-token-expired-alert');
    expect(errorAlert).toBeInTheDocument();
    expect(errorAlert).toHaveTextContent('Your user token has expired. Delete the user token to generate a new one.');
  });

  it('should not display error alert when token is not expired', () => {
    const now = Date.now();
    const createTime = now - 10 * 24 * 60 * 60 * 1000; // 10 days ago (not expired)
    const expirationDays = 30;

    renderComponent({
      userToken: true,
      tokenExpirationDays: expirationDays,
      tokenCreateTime: createTime,
    });

    // Verify error alert is NOT displayed
    expect(
      screen.queryByText('Your user token has expired. Delete the user token to generate a new one.')
    ).not.toBeInTheDocument();
  });
});
