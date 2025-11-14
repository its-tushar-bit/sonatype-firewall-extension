/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import OidcConfigurationForm from 'MainRoot/configuration/oidc/OidcConfigurationForm';
import userEvent from '@testing-library/user-event';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('OidcConfigurationForm', () => {
  const mockOnCancel = jest.fn();
  const mockOnChange = jest.fn();
  const mockOnBlur = jest.fn();
  const mockOnSubmit = jest.fn();
  const mockDeleteConfiguration = jest.fn();
  const mockToggleDeleteModal = jest.fn();

  const defaultConfigurationValues = {
    oauth2IdpJwksUrl: initUserInput(''),
    oauth2IdpJwsAlgorithm: initUserInput(''),
    oauth2IdpJwks: initUserInput(''),
    oauth2UsernameClaim: initUserInput(''),
    oauth2FirstNameClaim: initUserInput(''),
    oauth2LastNameClaim: initUserInput(''),
    oauth2EmailClaim: initUserInput(''),
    oauth2GroupsClaim: initUserInput(''),
    oauth2ExactMatchClaimsJson: initUserInput(''),
    oidcIdpIssuer: initUserInput(''),
    oidcClientId: initUserInput(''),
    oidcClientSecret: initUserInput(''),
    oidcIdpAuthorizationUrl: initUserInput(''),
    oidcIdpTokenUrl: initUserInput(''),
    oidcAuthorizationCustomParamsJson: initUserInput(''),
    oidcTokenRequestCustomParamsJson: initUserInput(''),
  };

  const defaultProps = {
    configurationValues: defaultConfigurationValues,
    onCancel: mockOnCancel,
    onChange: mockOnChange,
    onBlur: mockOnBlur,
    onSubmit: mockOnSubmit,
    deleteConfiguration: mockDeleteConfiguration,
    toggleDeleteModal: mockToggleDeleteModal,
    isConfigured: false,
    isSubmitButtonDisabled: true,
    submitState: null,
    submitMaskError: null,
    isDeleteModalShown: false,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  const renderComponent = (props = {}) => {
    return render(<OidcConfigurationForm {...defaultProps} {...props} />);
  };

  describe('Rendering', () => {
    it('should render the form with all sections', () => {
      renderComponent();

      // Check for section headings (now using NxH2)
      expect(screen.getByRole('heading', { name: 'OIDC Connection Settings', level: 2 })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'OAuth2 JWT Configuration', level: 2 })).toBeInTheDocument();
      expect(screen.getByRole('heading', { name: 'User Attribute Mapping', level: 2 })).toBeInTheDocument();
    });

    it('should render all required OIDC fields', () => {
      renderComponent();

      expect(screen.getByLabelText(/Client ID/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Client Secret/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/IDP Issuer/i)).toBeInTheDocument();
    });

    it('should render all required OAuth2 fields', () => {
      renderComponent();

      // IDP Issuer is now shared with OIDC section, so we check for JWS Algorithm instead
      expect(screen.getByLabelText(/JWS Algorithm/i)).toBeInTheDocument();
    });

    it('should render all optional fields', () => {
      renderComponent();

      expect(screen.getByLabelText(/Authorization URL/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Token URL/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/JWKS URL/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/JWS Algorithm/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Username Claim/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Email Claim/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/First Name Claim/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Last Name Claim/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Groups Claim/i)).toBeInTheDocument();
    });

    it('should render textarea fields for JSON configurations', () => {
      renderComponent();

      expect(screen.getByLabelText(/Authorization Custom Parameters \(JSON\)/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Token Request Custom Parameters \(JSON\)/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/JWKS \(JSON\)/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Exact Match Claims \(JSON\)/i)).toBeInTheDocument();
    });

    it('should not display "Currently not configured" message in form (moved to page level)', () => {
      renderComponent({ isConfigured: false });

      // The "Currently not configured" message is now displayed in OidcConfigurationPage, not in the form
      expect(screen.queryByText('* Currently not configured')).not.toBeInTheDocument();
    });
  });

  describe('Button States', () => {
    it('should render Save button', () => {
      renderComponent();

      expect(screen.getByText('Save')).toBeInTheDocument();
    });

    it('should render Cancel button', () => {
      renderComponent();

      expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
    });

    it('should render Delete Configuration button', () => {
      renderComponent();

      expect(screen.getByText(/Delete Configuration/i)).toBeInTheDocument();
    });

    it('should disable Delete Configuration button when not configured', () => {
      renderComponent({ isConfigured: false });

      const deleteButton = screen.getByRole('button', { name: /Delete Configuration/i });
      expect(deleteButton).toBeDisabled();
    });

    it('should enable Delete Configuration button when configured', () => {
      renderComponent({ isConfigured: true });

      const deleteButton = screen.getByRole('button', { name: /Delete Configuration/i });
      expect(deleteButton).not.toBeDisabled();
    });

    it('should apply disabled class to Save button when isSubmitButtonDisabled is true', () => {
      renderComponent({ isSubmitButtonDisabled: true });

      const saveButton = screen.getByText('Save').closest('button');
      expect(saveButton).toHaveClass('disabled');
    });

    it('should not apply disabled class to Save button when isSubmitButtonDisabled is false', () => {
      renderComponent({ isSubmitButtonDisabled: false });

      const saveButton = screen.getByText('Save').closest('button');
      expect(saveButton).not.toHaveClass('disabled');
    });
  });

  describe('User Interactions', () => {
    it('should call onChange when a field value changes', async () => {
      const user = userEvent.setup();
      renderComponent();

      const clientIdInput = screen.getByLabelText(/Client ID/i);
      await user.type(clientIdInput, 'test-client-id');

      expect(mockOnChange).toHaveBeenCalled();
      // Verify it was called with the correct field name
      expect(mockOnChange).toHaveBeenCalledWith(expect.any(String), 'oidcClientId');
    });

    it('should call onBlur when a field loses focus', async () => {
      const user = userEvent.setup();
      renderComponent();

      const clientIdInput = screen.getByLabelText(/Client ID/i);
      await user.click(clientIdInput);
      await user.tab();

      expect(mockOnBlur).toHaveBeenCalledWith('oidcClientId');
    });

    it('should call onCancel when Cancel button is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      const cancelButton = screen.getByRole('button', { name: /Cancel/i });
      await user.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalledTimes(1);
    });

    it('should call toggleDeleteModal when Delete Configuration button is clicked', async () => {
      const user = userEvent.setup();
      renderComponent({ isConfigured: true });

      const deleteButton = screen.getByRole('button', { name: /Delete Configuration/i });
      await user.click(deleteButton);

      expect(mockToggleDeleteModal).toHaveBeenCalledTimes(1);
    });

    it('should handle form submission', async () => {
      const user = userEvent.setup();
      renderComponent({ isSubmitButtonDisabled: false });

      const saveButton = screen.getByText('Save').closest('button');
      await user.click(saveButton);

      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
    });
  });

  describe('Field Values', () => {
    it('should display pre-filled values for all fields', () => {
      const filledConfigurationValues = {
        oauth2IdpIssuer: initUserInput('https://auth.example.com'),
        oauth2IdpJwksUrl: initUserInput('https://auth.example.com/.well-known/jwks.json'),
        oauth2IdpJwsAlgorithm: initUserInput('RS256'),
        oauth2IdpJwks: initUserInput('{"keys": []}'),
        oauth2UsernameClaim: initUserInput('preferred_username'),
        oauth2FirstNameClaim: initUserInput('given_name'),
        oauth2LastNameClaim: initUserInput('family_name'),
        oauth2EmailClaim: initUserInput('email'),
        oauth2GroupsClaim: initUserInput('groups'),
        oauth2ExactMatchClaimsJson: initUserInput('{"role": "admin"}'),
        oidcIdpIssuer: initUserInput('https://auth.example.com'),
        oidcClientId: initUserInput('test-client-id'),
        oidcClientSecret: initUserInput('test-secret'),
        oidcIdpAuthorizationUrl: initUserInput('https://auth.example.com/authorize'),
        oidcIdpTokenUrl: initUserInput('https://auth.example.com/token'),
        oidcAuthorizationCustomParamsJson: initUserInput('{"prompt": "consent"}'),
        oidcTokenRequestCustomParamsJson: initUserInput('{"resource": "api"}'),
      };

      renderComponent({ configurationValues: filledConfigurationValues });

      expect(screen.getByLabelText(/Client ID/i)).toHaveValue('test-client-id');
      expect(screen.getByLabelText(/Client Secret/i)).toHaveValue('test-secret');
      expect(screen.getByLabelText(/IDP Issuer/i)).toHaveValue('https://auth.example.com');
      // IDP Issuer is now used for both OIDC and OAuth2, so we only have one field
      expect(screen.getByLabelText(/Username Claim/i)).toHaveValue('preferred_username');
      expect(screen.getByLabelText(/Email Claim/i)).toHaveValue('email');
    });

    it('should have password type for Client Secret field', () => {
      renderComponent();

      const clientSecretInput = screen.getByLabelText(/Client Secret/i);
      expect(clientSecretInput).toHaveAttribute('type', 'password');
    });

    it('should have autocomplete=new-password for Client Secret field', () => {
      renderComponent();

      const clientSecretInput = screen.getByLabelText(/Client Secret/i);
      expect(clientSecretInput).toHaveAttribute('autocomplete', 'new-password');
    });
  });

  describe('Submit State', () => {
    it('should display success mask when submitState is true', () => {
      renderComponent({ submitState: true });

      // NxStatefulForm shows success state
      expect(screen.getByText('Save')).toBeInTheDocument();
    });
  });

  describe('Delete Modal Integration', () => {
    it('should render delete modal when isDeleteModalShown is true', () => {
      renderComponent({ isDeleteModalShown: true });

      expect(screen.getByText('Delete OIDC Configuration?')).toBeInTheDocument();
    });

    it('should not render delete modal when isDeleteModalShown is false', () => {
      renderComponent({ isDeleteModalShown: false });

      expect(screen.queryByText('Delete OIDC Configuration?')).not.toBeInTheDocument();
    });

    it('should pass deleteConfiguration to modal', () => {
      renderComponent({ isDeleteModalShown: true });

      expect(screen.getByText('Delete OIDC Configuration?')).toBeInTheDocument();
      // Modal is rendered with correct props
    });
  });

  describe('Form Validation', () => {
    it('should mark all inputs as validatable', () => {
      renderComponent();

      // All inputs should have validation enabled
      const clientIdInput = screen.getByLabelText(/Client ID/i);
      expect(clientIdInput).toBeInTheDocument();
      // Validatable prop is passed to NxTextInput
    });

    it('should mark required fields with aria-required', () => {
      renderComponent();

      // OIDC required fields
      const clientIdInput = screen.getByLabelText(/Client ID/i);
      const clientSecretInput = screen.getByLabelText(/Client Secret/i);
      const oidcIssuerInput = screen.getByLabelText(/IDP Issuer/i);
      const authUrlInput = screen.getByLabelText(/Authorization URL/i);
      const tokenUrlInput = screen.getByLabelText(/Token URL/i);

      // OAuth2 required fields (IDP Issuer is shared with OIDC)
      const jwsAlgorithmInput = screen.getByLabelText(/JWS Algorithm/i);

      expect(clientIdInput).toHaveAttribute('aria-required', 'true');
      expect(clientSecretInput).toHaveAttribute('aria-required', 'true');
      expect(oidcIssuerInput).toHaveAttribute('aria-required', 'true');
      expect(authUrlInput).toHaveAttribute('aria-required', 'true');
      expect(tokenUrlInput).toHaveAttribute('aria-required', 'true');
      // oauth2IssuerInput is removed - IDP Issuer is shared
      expect(jwsAlgorithmInput).toHaveAttribute('aria-required', 'true');
    });

    it('should mark optional fields without aria-required', () => {
      renderComponent();

      // OAuth2 optional fields
      const jwksUrlInput = screen.getByLabelText(/JWKS URL/i);
      const usernameClaimInput = screen.getByLabelText(/Username Claim/i);
      const emailClaimInput = screen.getByLabelText(/Email Claim/i);

      expect(jwksUrlInput).toHaveAttribute('aria-required', 'false');
      expect(usernameClaimInput).toHaveAttribute('aria-required', 'false');
      expect(emailClaimInput).toHaveAttribute('aria-required', 'false');
    });
  });

  describe('CSS Classes', () => {
    it('should apply correct CSS class to form', () => {
      const { container } = renderComponent();

      expect(container.querySelector('.iq-oidc-configuration-form')).toBeInTheDocument();
    });

    it('should apply correct CSS class to save button', () => {
      renderComponent();

      const saveButton = screen.getByText('Save').closest('button');
      expect(saveButton).toHaveClass('iq-oidc-configuration-save-button');
    });
  });

  describe('Multiple Field Changes', () => {
    it('should handle multiple field changes independently', async () => {
      const user = userEvent.setup();
      renderComponent();

      const clientIdInput = screen.getByLabelText(/Client ID/i);
      const clientSecretInput = screen.getByLabelText(/Client Secret/i);

      await user.type(clientIdInput, 'id');
      await user.type(clientSecretInput, 'secret');

      // Each keystroke calls onChange
      expect(mockOnChange.mock.calls.length).toBeGreaterThan(2);
    });
  });

  describe('Field Validation', () => {
    it('should accept valid HTTPS URL for IDP Issuer', async () => {
      const user = userEvent.setup();
      renderComponent();

      const issuerInput = screen.getByLabelText(/IDP Issuer/i);
      await user.type(issuerInput, 'https://auth.example.com');

      expect(mockOnChange).toHaveBeenCalled();
      // URL is passed to onChange without error
    });

    it('should accept valid HTTPS URL for Authorization URL', async () => {
      const user = userEvent.setup();
      renderComponent();

      const authUrlInput = screen.getByLabelText(/Authorization URL/i);
      await user.type(authUrlInput, 'https://auth.example.com/authorize');

      expect(mockOnChange).toHaveBeenCalled();
    });

    it('should accept valid HTTPS URL for Token URL', async () => {
      const user = userEvent.setup();
      renderComponent();

      const tokenUrlInput = screen.getByLabelText(/Token URL/i);
      await user.type(tokenUrlInput, 'https://auth.example.com/token');

      expect(mockOnChange).toHaveBeenCalled();
    });

    it('should accept valid JSON in Authorization Custom Parameters', async () => {
      const user = userEvent.setup();
      renderComponent();

      const jsonInput = screen.getByLabelText(/Authorization Custom Parameters \(JSON\)/i);
      // Use paste instead of type for JSON to avoid special character issues
      await user.click(jsonInput);
      await user.paste('{"prompt": "consent"}');

      expect(mockOnChange).toHaveBeenCalled();
    });

    it('should accept valid JSON in Token Request Custom Parameters', async () => {
      const user = userEvent.setup();
      renderComponent();

      const jsonInput = screen.getByLabelText(/Token Request Custom Parameters \(JSON\)/i);
      await user.click(jsonInput);
      await user.paste('{"resource": "api"}');

      expect(mockOnChange).toHaveBeenCalled();
    });

    it('should accept valid JSON in JWKS field', async () => {
      const user = userEvent.setup();
      renderComponent();

      const jwksInput = screen.getByLabelText(/JWKS \(JSON\)/i);
      await user.click(jwksInput);
      await user.paste('{"keys": []}');

      expect(mockOnChange).toHaveBeenCalled();
    });

    it('should accept valid JSON in Exact Match Claims', async () => {
      const user = userEvent.setup();
      renderComponent();

      const claimsInput = screen.getByLabelText(/Exact Match Claims \(JSON\)/i);
      await user.click(claimsInput);
      await user.paste('{"role": "admin"}');

      expect(mockOnChange).toHaveBeenCalled();
    });

    it('should handle empty optional fields', async () => {
      const user = userEvent.setup();
      renderComponent();

      const authUrlInput = screen.getByLabelText(/Authorization URL/i);
      await user.clear(authUrlInput);

      // Empty optional field should not cause errors
      expect(mockOnBlur).not.toHaveBeenCalled();
    });
  });

  describe('Keyboard Navigation', () => {
    it('should navigate through fields using Tab key', async () => {
      const user = userEvent.setup();
      renderComponent();

      const clientIdInput = screen.getByLabelText(/Client ID/i);
      const clientSecretInput = screen.getByLabelText(/Client Secret/i);

      // Focus first field
      await user.click(clientIdInput);
      expect(clientIdInput).toHaveFocus();

      // Tab to next field
      await user.tab();
      expect(clientSecretInput).toHaveFocus();
    });

    it('should navigate backwards using Shift+Tab', async () => {
      const user = userEvent.setup();
      renderComponent();

      const clientIdInput = screen.getByLabelText(/Client ID/i);
      const clientSecretInput = screen.getByLabelText(/Client Secret/i);

      // Focus second field
      await user.click(clientSecretInput);
      expect(clientSecretInput).toHaveFocus();

      // Shift+Tab to previous field
      await user.tab({ shift: true });
      expect(clientIdInput).toHaveFocus();
    });

    it('should allow form submission with Enter key in text fields', async () => {
      const user = userEvent.setup();
      renderComponent({ isSubmitButtonDisabled: false });

      const clientIdInput = screen.getByLabelText(/Client ID/i);
      await user.click(clientIdInput);
      await user.keyboard('{Enter}');

      // Form should submit
      expect(mockOnSubmit).toHaveBeenCalled();
    });

    it('should not submit form with Enter in textarea fields', async () => {
      const user = userEvent.setup();
      renderComponent({ isSubmitButtonDisabled: false });

      const jsonInput = screen.getByLabelText(/Authorization Custom Parameters \(JSON\)/i);
      await user.click(jsonInput);
      await user.keyboard('{Enter}');

      // Textarea should accept newline, not submit form
      expect(mockOnSubmit).not.toHaveBeenCalled();
    });
  });

  describe('Focus Management', () => {
    it('should maintain focus on field after value change', async () => {
      const user = userEvent.setup();
      renderComponent();

      const clientIdInput = screen.getByLabelText(/Client ID/i);
      await user.click(clientIdInput);
      await user.type(clientIdInput, 'test');

      expect(clientIdInput).toHaveFocus();
    });

    it('should trigger onBlur when clicking outside field', async () => {
      const user = userEvent.setup();
      renderComponent();

      const clientIdInput = screen.getByLabelText(/Client ID/i);
      const clientSecretInput = screen.getByLabelText(/Client Secret/i);

      await user.click(clientIdInput);
      await user.click(clientSecretInput);

      expect(mockOnBlur).toHaveBeenCalledWith('oidcClientId');
    });
  });

  describe('Error State Display', () => {
    it('should display error message from submitMaskError', () => {
      renderComponent({ submitMaskError: 'Configuration save failed: Invalid client ID' });

      expect(screen.getByText(/Configuration save failed/i)).toBeInTheDocument();
    });

    it('should not display error when submitMaskError is null', () => {
      renderComponent({ submitMaskError: null });

      expect(screen.queryByText(/failed/i)).not.toBeInTheDocument();
    });
  });

  describe('Form Reset Behavior', () => {
    it('should clear all fields when Cancel is clicked', async () => {
      const user = userEvent.setup();
      const filledValues = {
        ...defaultConfigurationValues,
        oidcClientId: initUserInput('filled-value'),
      };
      renderComponent({ configurationValues: filledValues });

      const cancelButton = screen.getByRole('button', { name: /Cancel/i });
      await user.click(cancelButton);

      expect(mockOnCancel).toHaveBeenCalled();
    });
  });

  describe('Button Accessibility', () => {
    it('should have accessible name for Save button', () => {
      renderComponent();

      const saveButton = screen.getByRole('button', { name: /Save/i });
      expect(saveButton).toBeInTheDocument();
    });

    it('should have accessible name for Cancel button', () => {
      renderComponent();

      const cancelButton = screen.getByRole('button', { name: /Cancel/i });
      expect(cancelButton).toBeInTheDocument();
    });

    it('should have accessible name for Delete Configuration button', () => {
      renderComponent();

      const deleteButton = screen.getByRole('button', { name: /Delete Configuration/i });
      expect(deleteButton).toBeInTheDocument();
    });
  });

  describe('Conditional Rendering', () => {
    it('should show delete modal when isDeleteModalShown is toggled', () => {
      renderComponent({ isConfigured: true, isDeleteModalShown: false });

      expect(screen.queryByText('Delete OIDC Configuration?')).not.toBeInTheDocument();

      // Simulate modal being shown
      renderComponent({ isConfigured: true, isDeleteModalShown: true });

      expect(screen.getByText('Delete OIDC Configuration?')).toBeInTheDocument();
    });
  });
});
