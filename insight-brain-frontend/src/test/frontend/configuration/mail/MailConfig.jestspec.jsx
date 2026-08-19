/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { fireEvent, render, screen } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import MailConfig from 'MainRoot/configuration/mail/MailConfig';

describe('MailConfig', () => {
  let renderComponent;
  const defaultProps = {
    // Action functions
    load: jest.fn(),
    save: jest.fn(),
    del: jest.fn(),
    resetForm: jest.fn(),
    setHostname: jest.fn(),
    setPort: jest.fn(),
    setUsername: jest.fn(),
    setPassword: jest.fn(),
    setSslEnabled: jest.fn(),
    setStartTlsEnabled: jest.fn(),
    setSystemEmail: jest.fn(),
    setShowDeleteModal: jest.fn(),
    setTestEmail: jest.fn(),
    sendTestEmail: jest.fn(),
    getFipsStatus: jest.fn(),
    // State props
    loading: false,
    submitMaskState: null,
    submitMaskMessage: null,
    hasAllRequiredData: false,
    isDirty: false,
    isValid: false,
    loadError: null,
    saveError: null,
    deleteError: null,
    testEmailError: null,
    serverData: null,
    showDeleteModal: false,
    mustReenterPassword: false,
    testEmailSent: false,
    isAuthorized: true,
    isEmailStopped: false,
    isFipsEnabled: false,
    fipsStatusLoading: false,
    fipsStatusError: null,
    // Form state
    hostnameState: { value: '', trimmedValue: '', isPristine: true, validationErrors: null },
    portState: { value: '', trimmedValue: '', isPristine: true, validationErrors: null },
    usernameState: { value: '', trimmedValue: '', isPristine: true, validationErrors: null },
    passwordState: { value: '', trimmedValue: '', isPristine: true, validationErrors: null },
    sslEnabledState: false,
    startTlsEnabledState: false,
    systemEmailState: { value: '', trimmedValue: '', isPristine: true, validationErrors: null },
    testEmailState: { value: '', trimmedValue: '', isPristine: true, validationErrors: null },
  };

  beforeEach(() => {
    jest.clearAllMocks();
    renderComponent = (props = {}) => render(<MailConfig {...defaultProps} {...props} />);
  });

  describe('SSL Enabled checkbox', () => {
    it('should not be disabled when FIPS mode is disabled', () => {
      renderComponent({ isFipsEnabled: false });

      const sslCheckbox = screen.getByLabelText('SSL Enabled');
      expect(sslCheckbox).not.toBeDisabled();
    });

    it('should be disabled when FIPS mode is enabled', async () => {
      renderComponent({ isFipsEnabled: true });

      const sslCheckbox = screen.getByLabelText('SSL Enabled');
      expect(sslCheckbox).toBeDisabled();

      fireEvent.mouseOver(sslCheckbox);
      const tooltip = await screen.findByRole('tooltip');
      expect(tooltip).toBeInTheDocument();
      expect(tooltip).toHaveTextContent('SSL is disabled due to FIPS compliance requirements.');
    });

    it('should call setSslEnabled when clicked and FIPS mode is disabled', async () => {
      const user = userEvent.setup();
      const setSslEnabledMock = jest.fn();
      renderComponent({
        isFipsEnabled: false,
        setSslEnabled: setSslEnabledMock,
        sslEnabledState: false,
      });

      const sslCheckbox = screen.getByLabelText('SSL Enabled');
      await user.click(sslCheckbox);

      expect(setSslEnabledMock).toHaveBeenCalledWith(true);
    });

    it('should not call setSslEnabled when clicked and FIPS mode is enabled', async () => {
      const user = userEvent.setup();
      const setSslEnabledMock = jest.fn();
      renderComponent({
        isFipsEnabled: true,
        setSslEnabled: setSslEnabledMock,
        sslEnabledState: false,
      });

      const sslCheckbox = screen.getByLabelText('SSL Enabled');
      await user.click(sslCheckbox);

      expect(setSslEnabledMock).not.toHaveBeenCalled();
    });
  });

  describe('FIPS status loading', () => {
    it('should call getFipsStatus on component mount', () => {
      const getFipsStatusMock = jest.fn();
      renderComponent({ getFipsStatus: getFipsStatusMock });

      expect(getFipsStatusMock).toHaveBeenCalled();
    });

    it('should show loading spinner when FIPS status is loading', () => {
      renderComponent({ fipsStatusLoading: true });

      expect(screen.getByText('Loading…')).toBeInTheDocument();
    });

    it('should show error message with retry when FIPS status fails to load', () => {
      const getFipsStatusMock = jest.fn();
      const mockError = { message: 'Network error' };
      renderComponent({
        fipsStatusError: mockError,
        getFipsStatus: getFipsStatusMock,
      });

      expect(screen.getByText(/Unable to load FIPS status/)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
    });
  });
});
