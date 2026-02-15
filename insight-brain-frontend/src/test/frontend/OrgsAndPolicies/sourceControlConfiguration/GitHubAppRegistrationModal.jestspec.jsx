/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import GitHubAppRegistrationModal from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/GitHubAppRegistrationModal';
import { initialState } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSlice';

const { initialState: nxTextInputInitialState } = nxTextInputStateHelpers;

describe('GitHubAppRegistrationModal', () => {
  let axiosMock;

  const renderComponent = (preloadedState = {}) => {
    return render(<GitHubAppRegistrationModal />, {
      preloadedState: {
        gitHubAppConfiguration: {
          ...initialState,
          isModalOpen: true, // Default to open for most tests
          ...preloadedState,
        },
      },
    });
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.reset();
    jest.clearAllMocks();
  });

  describe('Rendering', () => {
    it('renders modal when isOpen is true', () => {
      renderComponent();

      expect(screen.getByRole('heading', { name: /connect to github/i })).toBeInTheDocument();
    });

    it('renders modal with proper accessibility attributes', () => {
      renderComponent();

      const modal = screen.getByRole('dialog', { name: /connect to github/i });
      expect(modal).toHaveAccessibleName('Connect to GitHub');
    });

    it('does not render modal when isOpen is false', () => {
      renderComponent({ isModalOpen: false });

      expect(screen.queryByRole('heading', { name: /connect to github/i })).not.toBeInTheDocument();
    });

    it('displays account type fieldset', () => {
      renderComponent();

      expect(screen.getByRole('group', { name: /github account type/i })).toBeInTheDocument();
    });

    it('shows "Organization Account (recommended)" radio button', () => {
      renderComponent();

      expect(screen.getByRole('radio', { name: /organization account \(recommended\)/i })).toBeInTheDocument();
    });

    it('shows "Personal Account" radio button', () => {
      renderComponent();

      expect(screen.getByRole('radio', { name: /personal account/i })).toBeInTheDocument();
    });

    it('defaults to "organization" account type selected', () => {
      renderComponent();

      const orgRadio = screen.getByRole('radio', { name: /organization account \(recommended\)/i });
      const personalRadio = screen.getByRole('radio', { name: /personal account/i });

      expect(orgRadio).toBeChecked();
      expect(personalRadio).not.toBeChecked();
    });

    it('shows helper text explaining GitHub organization URL format', () => {
      renderComponent();

      expect(screen.getByText(/match the name shown in your github url/i)).toBeInTheDocument();
      expect(screen.getByText(/github\.com\/your-org-name/i)).toBeInTheDocument();
      expect(screen.getByText(/the github app will be registered under this organization/i)).toBeInTheDocument();
    });

    it('shows organization name input when organization is selected', () => {
      renderComponent();

      expect(screen.getByLabelText(/organization name/i)).toBeInTheDocument();
    });

    it('hides organization name input when personal account is selected', () => {
      renderComponent({
        formState: { accountType: 'personal', organizationName: nxTextInputInitialState('') },
      });

      expect(screen.queryByLabelText(/organization name/i)).not.toBeInTheDocument();
    });

    it('shows "Register & Create GitHub App" submit button', () => {
      renderComponent();

      expect(screen.getByRole('button', { name: /register & create github app/i })).toBeInTheDocument();
    });

    it('shows cancel button', () => {
      renderComponent();

      const cancelButtons = screen.getAllByRole('button');
      expect(cancelButtons.some((btn) => btn.textContent.includes('Cancel'))).toBe(true);
    });
  });

  describe('Form Validation', () => {
    it('shows validation error when submitting with empty organization name', async () => {
      const user = userEvent.setup();
      renderComponent();

      const submitButton = screen.getByRole('button', { name: /register & create github app/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/organization name is required/i)).toBeInTheDocument();
      });
    });

    it('does not initiate registration when organization name is empty', async () => {
      const user = userEvent.setup();
      renderComponent();

      const submitButton = screen.getByRole('button', { name: /register & create github app/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/organization name is required/i)).toBeInTheDocument();
      });

      expect(submitButton).not.toBeDisabled();
    });

    it('clears validation error when valid name is entered', async () => {
      const user = userEvent.setup();
      renderComponent();

      const orgNameInput = screen.getByLabelText(/organization name/i);
      const submitButton = screen.getByRole('button', { name: /register & create github app/i });

      // Trigger validation error
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/organization name is required/i)).toBeInTheDocument();
      });

      // Enter valid name
      await user.type(orgNameInput, 'valid-org');

      await waitFor(() => {
        expect(screen.queryByText(/organization name is required/i)).not.toBeInTheDocument();
      });

      // Verify input value is correct
      expect(orgNameInput).toHaveValue('valid-org');
    });

    it('treats whitespace-only organization name as invalid', async () => {
      const user = userEvent.setup();
      renderComponent();

      const orgNameInput = screen.getByLabelText(/organization name/i);
      await user.type(orgNameInput, '   ');

      const submitButton = screen.getByRole('button', { name: /register & create github app/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/organization name is required/i)).toBeInTheDocument();
      });
    });
  });

  describe('User Interaction', () => {
    it('switches to personal account when personal radio is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      const personalRadio = screen.getByRole('radio', { name: /personal account/i });

      // Verify organization input and helper text are initially visible
      expect(screen.getByLabelText(/organization name/i)).toBeInTheDocument();
      expect(screen.getByText(/match the name shown in your github url/i)).toBeInTheDocument();

      await user.click(personalRadio);

      expect(personalRadio).toBeChecked();

      // Verify organization input and helper text disappear
      expect(screen.queryByLabelText(/organization name/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/match the name shown in your github url/i)).not.toBeInTheDocument();
    });

    it('switches back to organization when organization radio is clicked', async () => {
      const user = userEvent.setup();
      renderComponent();

      const personalRadio = screen.getByRole('radio', { name: /personal account/i });
      const orgRadio = screen.getByRole('radio', { name: /organization account \(recommended\)/i });

      // First switch to personal account
      await user.click(personalRadio);

      // Verify organization input and helper text are not visible
      expect(screen.queryByLabelText(/organization name/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/match the name shown in your github url/i)).not.toBeInTheDocument();

      // Switch back to organization account
      await user.click(orgRadio);

      expect(orgRadio).toBeChecked();

      // Verify organization input and helper text appear
      expect(screen.getByLabelText(/organization name/i)).toBeInTheDocument();
      expect(screen.getByText(/match the name shown in your github url/i)).toBeInTheDocument();
    });

    it('updates organization name input value correctly', async () => {
      const user = userEvent.setup();
      renderComponent();

      const orgNameInput = screen.getByLabelText(/organization name/i);
      await user.type(orgNameInput, 'test-organization');

      expect(orgNameInput).toHaveValue('test-organization');
    });

    it('dispatches closeModal action when cancel button is clicked', async () => {
      const user = userEvent.setup();
      const { store } = renderComponent();

      const cancelButtons = screen.getAllByRole('button');
      const cancelButton = cancelButtons.find((btn) => btn.textContent.includes('Cancel'));
      await user.click(cancelButton);

      // Verify modal is closed via Redux state
      const state = store.getState();
      expect(state.gitHubAppConfiguration.isModalOpen).toBe(false);
    });

    it('resets form state when modal is closed', async () => {
      const user = userEvent.setup();
      const { store } = renderComponent();

      // Make changes to the modal: switch to personal account
      const personalRadio = screen.getByRole('radio', { name: /personal account/i });
      await user.click(personalRadio);

      // Verify personal account is selected
      expect(personalRadio).toBeChecked();
      let state = store.getState();
      expect(state.gitHubAppConfiguration.formState.accountType).toBe('personal');

      // Close modal
      const cancelButtons = screen.getAllByRole('button');
      const cancelButton = cancelButtons.find((btn) => btn.textContent.includes('Cancel'));
      await user.click(cancelButton);

      // Verify state is reset to initial
      state = store.getState();
      expect(state.gitHubAppConfiguration.formState.accountType).toBe('organization');
      expect(state.gitHubAppConfiguration.formState.organizationName.value).toBe('');
    });
  });

  describe('Form Submission', () => {
    it('displays API error in UI when backend fails', async () => {
      const user = userEvent.setup();
      const errorMessage = 'Failed to generate manifest';
      // Mock the correct URL pattern with owner ID parameter
      axiosMock.onPost(/\/api\/v2\/githubApp\/manifest\?ownerId=/).reply(500, { message: errorMessage });

      const { container } = renderComponent();

      await user.type(screen.getByLabelText(/organization name/i), 'sonatype');
      await user.click(screen.getByRole('button', { name: /register & create github app/i }));

      // Wait for error to appear in UI - error message might be wrapped or formatted
      await waitFor(
        () => {
          expect(screen.getByText(new RegExp(errorMessage, 'i'))).toBeInTheDocument();
        },
        { timeout: 3000, container }
      );
    });
  });

  describe('Error Handling', () => {
    it('clears validation error when entering valid organization name', async () => {
      const user = userEvent.setup();
      renderComponent();

      // Trigger validation error
      const submitButton = screen.getByRole('button', { name: /register & create github app/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/organization name is required/i)).toBeInTheDocument();
      });

      // Enter valid organization name
      const orgNameInput = screen.getByLabelText(/organization name/i);
      await user.type(orgNameInput, 'valid-org');

      // Verify validation error is cleared
      await waitFor(() => {
        expect(screen.queryByText(/organization name is required/i)).not.toBeInTheDocument();
      });
    });

    it('clears validation error and hides org input when switching to personal account', async () => {
      const user = userEvent.setup();
      renderComponent();

      // Trigger validation error with organization account
      const submitButton = screen.getByRole('button', { name: /register & create github app/i });
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/organization name is required/i)).toBeInTheDocument();
      });

      // Switch to personal account (no organization name needed)
      await user.click(screen.getByRole('radio', { name: /personal account/i }));

      // Validation error should be cleared and org input hidden
      await waitFor(() => {
        expect(screen.queryByText(/organization name is required/i)).not.toBeInTheDocument();
        expect(screen.queryByLabelText(/organization name/i)).not.toBeInTheDocument();
      });
    });
  });
});
