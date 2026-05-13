/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import GitHubAppAuthenticationMethod from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/GitHubAppAuthenticationMethod';
import { AUTHENTICATION_TYPES } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/utils';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSlice';

const { initialState: nxTextInputInitialState } = nxTextInputStateHelpers;

describe('GitHubAppAuthenticationMethod', () => {
  const defaultSourceControl = {
    token: {
      rscValue: nxTextInputInitialState(''),
    },
    authenticationType: {
      value: null,
    },
    githubApp: {
      value: null,
    },
  };

  const configuredSourceControl = {
    token: {
      rscValue: nxTextInputInitialState(''),
    },
    authenticationType: {
      value: AUTHENTICATION_TYPES.GITHUB_APP,
    },
    githubApp: {
      value: {
        installationId: '12345',
        accountName: 'sonatype',
        accountType: 'organization',
        name: 'Sonatype IQ App',
        configurationDate: '2024-01-15T10:30:00Z',
      },
    },
    repositoryUrl: {
      value: 'https://github.com/organizations/sonatype/settings/installations/12345',
    },
  };

  const defaultProps = {
    sourceControl: defaultSourceControl,
    setValue: jest.fn(),
    areFieldsDisabled: false,
    onChangeToken: jest.fn(),
  };

  const renderComponent = (props = {}) => {
    return render(<GitHubAppAuthenticationMethod {...defaultProps} {...props} />);
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    describe('when GitHub App authentication is enabled', () => {
      it('renders authentication method fieldset', () => {
        renderComponent();

        expect(screen.getByRole('group', { name: /authentication method/i })).toBeInTheDocument();
      });

      it('renders GitHub App radio button', () => {
        renderComponent();

        expect(screen.getByRole('radio', { name: /github app \(recommended\)/i })).toBeInTheDocument();
      });

      it('renders Personal Access Token radio button', () => {
        renderComponent();

        expect(screen.getByRole('radio', { name: /personal access token/i })).toBeInTheDocument();
      });

      it('renders no radio button selected when authenticationType is null and no token exists', () => {
        renderComponent();

        const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
        const patRadio = screen.getByRole('radio', { name: /personal access token/i });

        expect(githubAppRadio).not.toBeChecked();
        expect(patRadio).not.toBeChecked();
      });

      it('does NOT show Access Token field when no authentication method is selected', () => {
        renderComponent();

        const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
        const patRadio = screen.getByRole('radio', { name: /personal access token/i });

        expect(githubAppRadio).not.toBeChecked();
        expect(patRadio).not.toBeChecked();

        // Access Token field should NOT be visible when no auth method is selected
        expect(document.querySelector('#source-control-token')).not.toBeInTheDocument();
      });

      it('shows Access Token field for legacy config with null authenticationType and existing token after effect runs', async () => {
        const sourceControl = {
          ...defaultSourceControl,
          authenticationType: { value: null },
          token: {
            rscValue: {
              value: 'ghp_existing_legacy_token',
              isPristine: false,
              trimmedValue: 'ghp_existing_legacy_token',
              validationErrors: null,
            },
          },
        };
        const setValue = jest.fn();
        renderComponent({ sourceControl, setValue });

        // Wait for useEffect to run and infer PAT from token presence
        await waitFor(() => {
          expect(setValue).toHaveBeenCalledWith('authenticationType', AUTHENTICATION_TYPES.PAT);
        });

        // PAT radio should be checked after effect
        const patRadio = screen.getByRole('radio', { name: /personal access token/i });
        expect(patRadio).toBeChecked();

        // Access Token field should be visible
        const tokenInput = document.querySelector('#source-control-token');
        expect(tokenInput).toBeInTheDocument();
        expect(tokenInput).toHaveValue('ghp_existing_legacy_token');
      });

      it('renders PAT selected when authenticationType is null but token exists', () => {
        const sourceControl = {
          ...defaultSourceControl,
          authenticationType: { value: null },
          token: {
            rscValue: {
              value: 'ghp_existing_token',
              isPristine: false,
              trimmedValue: 'ghp_existing_token',
              validationErrors: null,
            },
          },
        };
        const setValue = jest.fn();
        renderComponent({ sourceControl, setValue });

        const patRadio = screen.getByRole('radio', { name: /personal access token/i });
        expect(patRadio).toBeChecked();
        // Verify setValue was called to sync the default value
        expect(setValue).toHaveBeenCalledWith('authenticationType', AUTHENTICATION_TYPES.PAT);
      });

      it('renders GitHub App selected when authenticationType is GITHUB_APP', () => {
        const sourceControl = {
          ...defaultSourceControl,
          authenticationType: { value: AUTHENTICATION_TYPES.GITHUB_APP },
        };
        renderComponent({ sourceControl });

        const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
        expect(githubAppRadio).toBeChecked();
      });

      it('renders PAT selected when authenticationType is PAT', () => {
        const sourceControl = {
          ...defaultSourceControl,
          authenticationType: { value: AUTHENTICATION_TYPES.PAT },
        };
        renderComponent({ sourceControl });

        const patRadio = screen.getByRole('radio', { name: /personal access token/i });
        expect(patRadio).toBeChecked();
      });

      it('renders GitHub App selected when installationId exists but authenticationType is null', () => {
        const sourceControl = {
          ...defaultSourceControl,
          authenticationType: { value: null },
          githubApp: {
            value: {
              installationId: 12345,
              accountName: 'test-org',
              name: 'Test App',
            },
          },
        };
        const setValue = jest.fn();
        renderComponent({ sourceControl, setValue });

        const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
        expect(githubAppRadio).toBeChecked();
        expect(setValue).toHaveBeenCalledWith('authenticationType', AUTHENTICATION_TYPES.GITHUB_APP);
      });

      it('renders PAT selected when installationId exists but authenticationType is explicitly PAT', () => {
        const sourceControl = {
          ...defaultSourceControl,
          authenticationType: { value: AUTHENTICATION_TYPES.PAT },
          token: {
            rscValue: {
              value: 'ghp_token',
              isPristine: false,
              trimmedValue: 'ghp_token',
              validationErrors: null,
            },
          },
          githubApp: {
            value: {
              installationId: 12345,
              accountName: 'test-org',
              name: 'Test App',
            },
          },
        };
        renderComponent({ sourceControl });

        const patRadio = screen.getByRole('radio', { name: /personal access token/i });
        expect(patRadio).toBeChecked();

        // GitHub App data should not be visible when PAT is selected
        expect(screen.queryByText('Organization:')).not.toBeInTheDocument();
      });

      it('renders GitHub App selected when installationId exists and authenticationType is GITHUB_APP', () => {
        const sourceControl = {
          ...defaultSourceControl,
          authenticationType: { value: AUTHENTICATION_TYPES.GITHUB_APP },
          githubApp: {
            value: {
              installationId: 12345,
              accountName: 'test-org',
              name: 'Test App',
            },
          },
        };
        renderComponent({ sourceControl });

        const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
        expect(githubAppRadio).toBeChecked();

        // GitHub App data should be visible
        expect(screen.getByText('Organization:')).toBeInTheDocument();
        expect(screen.getByText('test-org')).toBeInTheDocument();
      });
    });

  });

  describe('GitHub App Authentication Method', () => {
    describe('when not configured', () => {
      it('shows Configure GitHub App button when GitHub App is selected', async () => {
        const user = userEvent.setup();
        renderComponent();

        const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
        await user.click(githubAppRadio);

        expect(screen.getByRole('button', { name: /configure github app/i })).toBeInTheDocument();
      });

      it('dispatches openModal action when Configure GitHub App button is clicked', async () => {
        const user = userEvent.setup();
        const openModalSpy = jest.spyOn(actions, 'openModal');
        const sourceControl = {
          ...defaultSourceControl,
          authenticationType: { value: AUTHENTICATION_TYPES.GITHUB_APP },
        };
        renderComponent({ sourceControl });

        const configureButton = screen.getByRole('button', { name: /configure github app/i });
        await user.click(configureButton);

        // Verify Redux action was dispatched to open modal
        expect(openModalSpy).toHaveBeenCalled();

        openModalSpy.mockRestore();
      });

      it('disables Configure GitHub App button when fields are disabled', () => {
        const sourceControl = {
          ...defaultSourceControl,
          authenticationType: { value: AUTHENTICATION_TYPES.GITHUB_APP },
        };
        renderComponent({ sourceControl, areFieldsDisabled: true });

        expect(screen.getByRole('button', { name: /configure github app/i })).toBeDisabled();
      });
    });

    describe('when configured', () => {
      it('shows configured status', () => {
        renderComponent({ sourceControl: configuredSourceControl });

        expect(screen.getByText(/organization:/i)).toBeInTheDocument();
        expect(screen.getByText('sonatype')).toBeInTheDocument();
      });

      it('displays organization account details', () => {
        renderComponent({ sourceControl: configuredSourceControl });

        expect(screen.getByText(/organization:/i)).toBeInTheDocument();
        expect(screen.getByText('sonatype')).toBeInTheDocument();
      });

      it('displays personal account details when account type is personal', () => {
        const personalAccountConfig = {
          ...configuredSourceControl,
          githubApp: {
            value: {
              ...configuredSourceControl.githubApp.value,
              accountType: 'personal',
              accountName: 'john-doe',
            },
          },
        };
        renderComponent({ sourceControl: personalAccountConfig });

        expect(screen.getByText(/organization:/i)).toBeInTheDocument();
        expect(screen.getByText('john-doe')).toBeInTheDocument();
      });

      it('displays GitHub App name', () => {
        renderComponent({ sourceControl: configuredSourceControl });

        expect(screen.getByText(/app:/i)).toBeInTheDocument();
        expect(screen.getByText('Sonatype IQ App')).toBeInTheDocument();
      });

      it('displays repositories link', () => {
        renderComponent({ sourceControl: configuredSourceControl });

        const link = screen.getByRole('link', { name: /go to github installation settings/i });
        expect(link).toBeInTheDocument();
        expect(link).toHaveAttribute('href', 'https://github.com/organizations/sonatype/settings/installations/12345');
      });

      it('displays configuration date', () => {
        renderComponent({ sourceControl: configuredSourceControl });

        expect(screen.getByText(/configuration date:/i)).toBeInTheDocument();
        expect(screen.getByText(/2024/)).toBeInTheDocument();
      });

      it('does not show Configure button when already configured', () => {
        renderComponent({ sourceControl: configuredSourceControl });

        expect(screen.queryByRole('button', { name: /configure github app/i })).not.toBeInTheDocument();
      });

      it('does not display optional details when fields are not present', () => {
        const minimalConfig = {
          ...defaultSourceControl,
          authenticationType: { value: AUTHENTICATION_TYPES.GITHUB_APP },
          githubApp: {
            value: {
              installationId: '12345',
            },
          },
        };
        renderComponent({ sourceControl: minimalConfig });

        expect(screen.getByText(/organization:/i)).toBeInTheDocument();
        expect(screen.queryByText(/app:/i)).not.toBeInTheDocument();
        expect(screen.queryByRole('link', { name: /go to github repositories/i })).not.toBeInTheDocument();
        expect(screen.queryByText(/configuration date:/i)).not.toBeInTheDocument();
      });
    });
  });

  describe('Personal Access Token Method', () => {
    it('shows token input when PAT is selected', async () => {
      const user = userEvent.setup();
      renderComponent();

      const patRadio = screen.getByRole('radio', { name: /personal access token/i });
      await user.click(patRadio);

      expect(document.querySelector('#source-control-token')).toBeInTheDocument();
    });

    it('token input has correct attributes', async () => {
      const user = userEvent.setup();
      renderComponent();

      const patRadio = screen.getByRole('radio', { name: /personal access token/i });
      await user.click(patRadio);

      const tokenInput = document.querySelector('#source-control-token');
      expect(tokenInput).toHaveAttribute('type', 'password');
      expect(tokenInput).toHaveAttribute('autocomplete', 'new-password');
    });

    it('calls onChangeToken when token input changes', async () => {
      const user = userEvent.setup();
      const onChangeToken = jest.fn();
      const sourceControl = {
        ...defaultSourceControl,
        authenticationType: { value: AUTHENTICATION_TYPES.PAT },
      };
      renderComponent({ sourceControl, onChangeToken });

      const tokenInput = document.querySelector('#source-control-token');
      await user.type(tokenInput, 'ghp_test123');

      expect(onChangeToken).toHaveBeenCalled();
    });

    it('disables token input when fields are disabled', () => {
      const sourceControl = {
        ...defaultSourceControl,
        authenticationType: { value: AUTHENTICATION_TYPES.PAT },
      };
      renderComponent({ sourceControl, areFieldsDisabled: true });

      expect(document.querySelector('#source-control-token')).toBeDisabled();
    });

    it('spreads RSC value props to token input', () => {
      const sourceControl = {
        ...defaultSourceControl,
        authenticationType: { value: AUTHENTICATION_TYPES.PAT },
        token: {
          rscValue: {
            value: 'test-token',
            isPristine: false,
            trimmedValue: 'test-token',
            validationErrors: null,
          },
        },
      };
      renderComponent({ sourceControl });

      const tokenInput = document.querySelector('#source-control-token');
      expect(tokenInput).toHaveValue('test-token');
    });
  });

  describe('User Interactions', () => {
    it('switches to GitHub App when GitHub App radio is clicked', async () => {
      const user = userEvent.setup();
      const setValue = jest.fn();
      renderComponent({ setValue });

      const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
      await user.click(githubAppRadio);

      expect(githubAppRadio).toBeChecked();
      expect(setValue).toHaveBeenCalledWith('authenticationType', AUTHENTICATION_TYPES.GITHUB_APP);
    });

    it('switches to PAT when PAT radio is clicked', async () => {
      const user = userEvent.setup();
      const setValue = jest.fn();
      renderComponent({ setValue });

      const patRadio = screen.getByRole('radio', { name: /personal access token/i });
      await user.click(patRadio);

      expect(patRadio).toBeChecked();
      expect(setValue).toHaveBeenCalledWith('authenticationType', AUTHENTICATION_TYPES.PAT);
    });

    it('can switch from GitHub App to PAT', async () => {
      const user = userEvent.setup();
      const setValue = jest.fn();
      renderComponent({ setValue });

      const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
      const patRadio = screen.getByRole('radio', { name: /personal access token/i });

      await user.click(githubAppRadio);
      expect(githubAppRadio).toBeChecked();

      await user.click(patRadio);
      expect(patRadio).toBeChecked();
      expect(githubAppRadio).not.toBeChecked();

      expect(setValue).toHaveBeenCalledTimes(2);
      expect(setValue).toHaveBeenNthCalledWith(1, 'authenticationType', AUTHENTICATION_TYPES.GITHUB_APP);
      expect(setValue).toHaveBeenNthCalledWith(2, 'authenticationType', AUTHENTICATION_TYPES.PAT);
    });

    it('can switch from PAT to GitHub App', async () => {
      const user = userEvent.setup();
      const setValue = jest.fn();
      renderComponent({ setValue });

      const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
      const patRadio = screen.getByRole('radio', { name: /personal access token/i });

      await user.click(patRadio);
      expect(patRadio).toBeChecked();

      await user.click(githubAppRadio);
      expect(githubAppRadio).toBeChecked();
      expect(patRadio).not.toBeChecked();

      expect(setValue).toHaveBeenCalledTimes(2);
      expect(setValue).toHaveBeenNthCalledWith(1, 'authenticationType', AUTHENTICATION_TYPES.PAT);
      expect(setValue).toHaveBeenNthCalledWith(2, 'authenticationType', AUTHENTICATION_TYPES.GITHUB_APP);
    });

    it('hides GitHub App details when switching to PAT', async () => {
      const user = userEvent.setup();
      const sourceControl = {
        ...defaultSourceControl,
        authenticationType: { value: AUTHENTICATION_TYPES.GITHUB_APP },
      };
      renderComponent({ sourceControl });

      expect(screen.getByRole('button', { name: /configure github app/i })).toBeInTheDocument();

      const patRadio = screen.getByRole('radio', { name: /personal access token/i });
      await user.click(patRadio);

      expect(screen.queryByRole('button', { name: /configure github app/i })).not.toBeInTheDocument();
      expect(document.querySelector('#source-control-token')).toBeInTheDocument();
    });

    it('hides token input when switching to GitHub App', async () => {
      const user = userEvent.setup();
      const sourceControl = {
        ...defaultSourceControl,
        authenticationType: { value: AUTHENTICATION_TYPES.PAT },
      };
      renderComponent({ sourceControl });

      expect(document.querySelector('#source-control-token')).toBeInTheDocument();

      const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
      await user.click(githubAppRadio);

      expect(document.querySelector('#source-control-token')).not.toBeInTheDocument();
      expect(screen.getByRole('button', { name: /configure github app/i })).toBeInTheDocument();
    });

    it('does not call setValue when setValue is not provided', async () => {
      const user = userEvent.setup();
      renderComponent({ setValue: undefined });

      const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
      await user.click(githubAppRadio);

      // Should not throw error and radio should still be checked
      expect(githubAppRadio).toBeChecked();
    });
  });

  describe('Disabled State', () => {
    it('disables all radio buttons when fields are disabled', () => {
      renderComponent({ areFieldsDisabled: true });

      const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
      const patRadio = screen.getByRole('radio', { name: /personal access token/i });

      expect(githubAppRadio).toBeDisabled();
      expect(patRadio).toBeDisabled();
    });

    it('disables authentication method radios when fields are disabled', () => {
      renderComponent({ areFieldsDisabled: true });

      const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
      const patRadio = screen.getByRole('radio', { name: /personal access token/i });

      expect(githubAppRadio).toBeDisabled();
      expect(patRadio).toBeDisabled();
    });

    it('allows interactions when fields are not disabled', () => {
      renderComponent({ areFieldsDisabled: false });

      const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
      const patRadio = screen.getByRole('radio', { name: /personal access token/i });

      expect(githubAppRadio).not.toBeDisabled();
      expect(patRadio).not.toBeDisabled();
    });
  });

  describe('PropTypes', () => {
    it('renders without errors with required props', () => {
      const minimalProps = {
        sourceControl: defaultSourceControl,
        areFieldsDisabled: false,
        onChangeToken: jest.fn(),
      };

      expect(() => render(<GitHubAppAuthenticationMethod {...minimalProps} />)).not.toThrow();
    });

    it('renders without errors with all props', () => {
      expect(() => renderComponent()).not.toThrow();
    });
  });

  describe('Inheritance - Org/App Level', () => {
    describe('when inheriting GitHub App from parent', () => {
      const inheritedGitHubAppSourceControl = {
        token: {
          rscValue: nxTextInputInitialState(''),
          isInherited: true,
          parentValue: nxTextInputInitialState('ghp_parent_token'),
        },
        provider: {
          parentName: 'Root Organization',
        },
        authenticationType: {
          value: null,
          isInherited: true,
          parentValue: AUTHENTICATION_TYPES.GITHUB_APP,
        },
        githubApp: {
          value: null,
          isInherited: true,
          parentName: 'Root Organization',
          parentValue: {
            installationId: '99999',
            accountName: 'parent-org',
            accountType: 'organization',
            name: 'Parent GitHub App',
            configurationDate: '2024-02-01T12:00:00Z',
          },
        },
      };

      it('renders inheritance radios when setIsInherited prop is provided', () => {
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl: inheritedGitHubAppSourceControl, setIsInherited });

        expect(screen.getByRole('radio', { name: /inherit from root organization/i })).toBeInTheDocument();
        expect(screen.getByRole('radio', { name: /override/i })).toBeInTheDocument();
      });

      it('shows "Inherit from [parentName]" when parent has GitHub App configured', () => {
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl: inheritedGitHubAppSourceControl, setIsInherited });

        expect(screen.getByRole('radio', { name: /inherit from root organization/i })).toBeInTheDocument();
      });

      it('shows "Inherit (Not Configured)" when parent has no GitHub App', () => {
        const sourceControl = {
          ...inheritedGitHubAppSourceControl,
          token: {
            ...inheritedGitHubAppSourceControl.token,
            parentValue: null, // No parent token either
          },
          githubApp: {
            ...inheritedGitHubAppSourceControl.githubApp,
            parentName: null, // No parent has GitHub App
            parentValue: null,
          },
        };
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl, setIsInherited });

        expect(screen.getByRole('radio', { name: /inherit \(not configured\)/i })).toBeInTheDocument();
      });

      it('checks Inherit radio when isInherited is true', () => {
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl: inheritedGitHubAppSourceControl, setIsInherited });

        const inheritRadio = screen.getByRole('radio', { name: /inherit from root organization/i });
        expect(inheritRadio).toBeChecked();
      });

      it('displays inherited GitHub App details box when parent has GitHub App installed', () => {
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl: inheritedGitHubAppSourceControl, setIsInherited });

        // Verify details box renders
        expect(screen.getByText('Organization:')).toBeInTheDocument();
        expect(screen.getByText('parent-org')).toBeInTheDocument();
        expect(screen.getByText('App:')).toBeInTheDocument();
        expect(screen.getByText('Parent GitHub App')).toBeInTheDocument();
      });

      it('displays GitHub installation link for inherited GitHub App', () => {
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl: inheritedGitHubAppSourceControl, setIsInherited });

        const link = screen.getByRole('link', { name: /view github app configuration/i });
        expect(link).toBeInTheDocument();
        expect(link).toHaveAttribute(
          'href',
          'https://github.com/organizations/parent-org/settings/installations/99999'
        );
      });

      it('displays configuration date for inherited GitHub App', () => {
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl: inheritedGitHubAppSourceControl, setIsInherited });

        expect(screen.getByText(/configuration date:/i)).toBeInTheDocument();
        expect(screen.getByText(/2024/)).toBeInTheDocument();
      });

      it('does NOT display GitHub App details when parent has no installation (hasParentConfig check)', () => {
        const sourceControl = {
          ...inheritedGitHubAppSourceControl,
          githubApp: {
            ...inheritedGitHubAppSourceControl.githubApp,
            parentName: null,
            parentValue: null, // No installation data
          },
        };
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl, setIsInherited });

        // Details box should NOT render
        expect(screen.queryByText('Organization:')).not.toBeInTheDocument();
        expect(screen.queryByText('parent-org')).not.toBeInTheDocument();
      });

      it('does NOT display GitHub App details when parent selected GitHub App but has no installationId', () => {
        const sourceControl = {
          ...inheritedGitHubAppSourceControl,
          githubApp: {
            ...inheritedGitHubAppSourceControl.githubApp,
            parentValue: {
              accountName: 'parent-org',
              // installationId missing - edge case
            },
          },
        };
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl, setIsInherited });

        // Details box should NOT render without installationId
        expect(screen.queryByText('Organization:')).not.toBeInTheDocument();
      });

      it('disables authentication method radios when inheriting', () => {
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl: inheritedGitHubAppSourceControl, setIsInherited });

        const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
        const patRadio = screen.getByRole('radio', { name: /personal access token/i });

        expect(githubAppRadio).toBeDisabled();
        expect(patRadio).toBeDisabled();
      });

      it('shows GitHub App radio as checked when inheriting GitHub App', () => {
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl: inheritedGitHubAppSourceControl, setIsInherited });

        const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
        expect(githubAppRadio).toBeChecked();
      });

      it('calls setIsInherited when Override radio is clicked', async () => {
        const user = userEvent.setup();
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl: inheritedGitHubAppSourceControl, setIsInherited });

        const overrideRadio = screen.getByRole('radio', { name: /override/i });
        await user.click(overrideRadio);

        expect(setIsInherited).toHaveBeenCalledWith('authenticationType', false);
      });

      it('calls setIsInherited when Inherit radio is clicked', async () => {
        const user = userEvent.setup();
        const setIsInherited = jest.fn();
        const sourceControl = {
          ...inheritedGitHubAppSourceControl,
          authenticationType: {
            ...inheritedGitHubAppSourceControl.authenticationType,
            isInherited: false, // Currently overriding
          },
        };
        renderComponent({ sourceControl, setIsInherited });

        const inheritRadio = screen.getByRole('radio', { name: /inherit from root organization/i });
        await user.click(inheritRadio);

        expect(setIsInherited).toHaveBeenCalledWith('authenticationType', true);
      });

      it('displays personal account URL when parent has personal account type', () => {
        const sourceControl = {
          ...inheritedGitHubAppSourceControl,
          githubApp: {
            ...inheritedGitHubAppSourceControl.githubApp,
            parentValue: {
              ...inheritedGitHubAppSourceControl.githubApp.parentValue,
              accountName: 'john-doe(personal)',
            },
          },
        };
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl, setIsInherited });

        const link = screen.getByRole('link', { name: /view github app configuration/i });
        expect(link).toHaveAttribute('href', 'https://github.com/settings/installations/99999');
      });

      it('displays inherited PAT token field when parent uses PAT', () => {
        const sourceControl = {
          ...inheritedGitHubAppSourceControl,
          authenticationType: {
            ...inheritedGitHubAppSourceControl.authenticationType,
            parentValue: AUTHENTICATION_TYPES.PAT,
          },
          githubApp: {
            ...inheritedGitHubAppSourceControl.githubApp,
            parentName: null,
            parentValue: null,
          },
        };
        const setIsInherited = jest.fn();
        renderComponent({ sourceControl, setIsInherited });

        // PAT radio should be checked
        const patRadio = screen.getByRole('radio', { name: /personal access token/i });
        expect(patRadio).toBeChecked();

        // Token field should be visible and disabled
        const tokenInput = document.querySelector('#source-control-token');
        expect(tokenInput).toBeInTheDocument();
        expect(tokenInput).toBeDisabled();
      });
    });

    describe('when NOT inheriting (Root level or Override)', () => {
      it('does NOT render inheritance radios when setIsInherited is not provided', () => {
        renderComponent(); // No setIsInherited prop

        expect(screen.queryByRole('radio', { name: /inherit/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('radio', { name: /override/i })).not.toBeInTheDocument();
      });

      it('enables authentication method radios when not inheriting', () => {
        renderComponent(); // Root level

        const githubAppRadio = screen.getByRole('radio', { name: /github app \(recommended\)/i });
        const patRadio = screen.getByRole('radio', { name: /personal access token/i });

        expect(githubAppRadio).not.toBeDisabled();
        expect(patRadio).not.toBeDisabled();
      });

      it('does not show "Authentication Type" section label at Root level', () => {
        renderComponent(); // No setIsInherited prop

        expect(screen.queryByText('Authentication Type')).not.toBeInTheDocument();
      });

      it('shows "Authentication Type" section label at Org/App level', () => {
        const setIsInherited = jest.fn();
        const sourceControl = {
          ...defaultSourceControl,
          githubApp: {
            value: null,
            isInherited: false,
          },
        };
        renderComponent({ sourceControl, setIsInherited });

        expect(screen.getByText('Authentication Type')).toBeInTheDocument();
      });
    });
  });
});
