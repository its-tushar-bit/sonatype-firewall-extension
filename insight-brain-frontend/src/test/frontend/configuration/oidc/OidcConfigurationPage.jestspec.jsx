/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import OidcConfigurationPage from 'MainRoot/configuration/oidc/OidcConfigurationPage';
import { getOidcConfigurationUrl } from 'MainRoot/util/CLMLocation';
import userEvent from '@testing-library/user-event';
import * as authorizationUtil from 'MainRoot/util/authorizationUtil';

describe('OidcConfigurationPage', () => {
  let axiosMock;

  const defaultPreloadedState = {
    oidcConfiguration: {
      isLoading: false,
      submitState: null,
      submitMaskError: null,
      loadError: null,
      isConfigured: false,
      isDeleteModalShown: false,
      isDirty: false,
      configurationValues: {
        oauth2IdpIssuer: { value: '', isPristine: true },
        oauth2IdpJwksUrl: { value: '', isPristine: true },
        oauth2IdpJwsAlgorithm: { value: '', isPristine: true },
        oauth2IdpJwks: { value: '', isPristine: true },
        oauth2UsernameClaim: { value: '', isPristine: true },
        oauth2FirstNameClaim: { value: '', isPristine: true },
        oauth2LastNameClaim: { value: '', isPristine: true },
        oauth2EmailClaim: { value: '', isPristine: true },
        oauth2GroupsClaim: { value: '', isPristine: true },
        oauth2ExactMatchClaimsJson: { value: '', isPristine: true },
        oidcIdpIssuer: { value: '', isPristine: true },
        oidcClientId: { value: '', isPristine: true },
        oidcClientSecret: { value: '', isPristine: true },
        oidcIdpAuthorizationUrl: { value: '', isPristine: true },
        oidcIdpTokenUrl: { value: '', isPristine: true },
        oidcAuthorizationCustomParamsJson: { value: '', isPristine: true },
        oidcTokenRequestCustomParamsJson: { value: '', isPristine: true },
      },
      loadedConfigurationValues: null,
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getOidcConfigurationUrl()).reply(404);
    jest.spyOn(authorizationUtil, 'checkPermissions').mockResolvedValue();
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  const renderComponent = (props = {}, preloadedState) => {
    return render(<OidcConfigurationPage {...props} />, {
      preloadedState: preloadedState || defaultPreloadedState,
    });
  };

  it('should render the OIDC configuration page', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText('OpenID Connect (OIDC) Configuration')).toBeInTheDocument();
      expect(screen.getByText('* Currently not configured')).toBeInTheDocument();
    });
  });

  it('should display all form fields', async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByLabelText(/Client ID/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/Client Secret/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/IDP Issuer/i)).toBeInTheDocument();
      // Only one IDP Issuer field now (used for both OIDC and OAuth2)
    });
  });

  it('should load existing configuration', async () => {
    const mockConfiguration = {
      oauth2Configuration: {
        idpIssuer: 'https://identity-provider.com/',
        idpJwksUrl: 'https://identity-provider.com/.well-known/jwks.json',
        idpJwsAlgorithm: 'RS256',
        usernameClaim: 'email',
        emailClaim: 'email',
      },
      oidcConfiguration: {
        idpIssuer: 'https://identity-provider.com/',
        clientId: 'test-client-id',
        clientSecret: 'test-client-secret',
        idpAuthorizationUrl: 'https://identity-provider.com/authorize',
        idpTokenUrl: 'https://identity-provider.com/oauth/token',
      },
    };

    axiosMock.onGet(getOidcConfigurationUrl()).reply(200, mockConfiguration);

    renderComponent();

    await waitFor(() => {
      const clientIdInput = screen.getByLabelText(/Client ID/i);
      expect(clientIdInput).toHaveValue('test-client-id');
    });
  });

  it('should disable save button when required fields are empty', async () => {
    renderComponent();

    await waitFor(() => {
      const saveButton = screen.getByText('Save');
      expect(saveButton.closest('button')).toHaveClass('disabled');
    });
  });

  it('should enable save button when all required fields are filled', async () => {
    // delay: null removes the artificial per-keystroke delay so typing many fields does not time out
    // under a heavily loaded CI box (the one-box run saturates the agent).
    const user = userEvent.setup({ delay: null });
    renderComponent();

    await waitFor(() => {
      expect(screen.getByLabelText(/Client ID/i)).toBeInTheDocument();
    });

    // Fill in all required OIDC fields
    await user.type(screen.getByLabelText(/Client ID/i), 'test-client-id');
    await user.type(screen.getByLabelText(/Client Secret/i), 'test-secret');
    await user.type(screen.getByLabelText(/IDP Issuer/i), 'https://identity-provider.com/');
    await user.type(screen.getByLabelText(/Authorization URL/i), 'https://identity-provider.com/authorize');
    await user.type(screen.getByLabelText(/Token URL/i), 'https://identity-provider.com/token');

    // Fill in all required OAuth2 fields (IDP Issuer is shared, so only JWS Algorithm and JWKS)
    await user.type(screen.getByLabelText(/JWS Algorithm/i), 'RS256');
    await user.type(screen.getByLabelText(/JWKS URL/i), 'https://identity-provider.com/.well-known/jwks.json');

    // The Save button enables only after async form validation settles, so poll for the enabled state
    // rather than asserting synchronously immediately after the last keystroke.
    await waitFor(() => {
      expect(screen.getByText('Save').closest('button')).not.toHaveClass('disabled');
    }, { timeout: 10000 });
  }, 15000); // Increase timeout to 15 seconds for slow user interactions

  it('should show delete modal when delete button is clicked', async () => {
    const user = userEvent.setup();
    const configuredState = {
      ...defaultPreloadedState,
      oidcConfiguration: {
        ...defaultPreloadedState.oidcConfiguration,
        isConfigured: true,
        configurationValues: {
          ...defaultPreloadedState.oidcConfiguration.configurationValues,
          oidcClientId: { value: 'test-client-id', isPristine: true },
        },
      },
    };

    axiosMock.onGet(getOidcConfigurationUrl()).reply(200, {
      oauth2Configuration: { idpIssuer: 'https://test.com/' },
      oidcConfiguration: { clientId: 'test-client-id', clientSecret: 'secret', idpIssuer: 'https://test.com/' },
    });

    renderComponent({}, configuredState);

    // Wait for the configuration to load and client ID to be populated
    await waitFor(() => {
      expect(screen.getByLabelText(/Client ID/i)).toHaveValue('test-client-id');
    });

    // Now delete button should be available
    const deleteButton = screen.getByRole('button', { name: /Delete Configuration/i });
    await user.click(deleteButton);

    // Wait for modal to appear
    await waitFor(() => {
      expect(screen.getByText('Delete OIDC Configuration?')).toBeInTheDocument();
    });
  });
});
