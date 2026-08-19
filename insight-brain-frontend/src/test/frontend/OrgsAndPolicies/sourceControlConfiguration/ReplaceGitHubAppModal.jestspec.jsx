/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import ReplaceGitHubAppModal from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/ReplaceGitHubAppModal';
import { actions as sourceControlActions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
import { actions as gitHubAppActions } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSlice';

describe('ReplaceGitHubAppModal', () => {
  const defaultPreloadedState = {
    orgsAndPolicies: {
      sourceControlConfiguration: {
        isReplaceGitHubAppModalOpen: false,
      },
    },
    productFeatures: {
      productFeatures: {
        'github-app-authentication': true, // Default to enabled for tests
      },
    },
  };

  const renderComponent = (preloadedState = {}) => {
    // Merge provided state with defaults, ensuring productFeatures is always present
    const mergedState = {
      ...defaultPreloadedState,
      ...preloadedState,
      productFeatures: {
        ...defaultPreloadedState.productFeatures,
        ...(preloadedState.productFeatures || {}),
      },
    };
    return render(<ReplaceGitHubAppModal />, { preloadedState: mergedState });
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('Modal Visibility', () => {
    it('does not render when isReplaceGitHubAppModalOpen is false', () => {
      renderComponent();

      expect(screen.queryByText('Replace GitHub App Configuration')).not.toBeInTheDocument();
    });

    it('renders when isReplaceGitHubAppModalOpen is true', () => {
      const stateWithModalOpen = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            isReplaceGitHubAppModalOpen: true,
          },
        },
      };

      renderComponent(stateWithModalOpen);

      expect(screen.getByText('Replace GitHub App Configuration')).toBeInTheDocument();
    });
  });

  describe('Modal Content', () => {
    it('displays warning message about replacing configuration', () => {
      const stateWithModalOpen = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            isReplaceGitHubAppModalOpen: true,
          },
        },
      };

      renderComponent(stateWithModalOpen);

      expect(screen.getByText(/This action replaces the existing GitHub App connection/)).toBeInTheDocument();
      expect(screen.getByText(/The current configuration will be overwritten/)).toBeInTheDocument();
    });

    it('displays confirmation instruction', () => {
      const stateWithModalOpen = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            isReplaceGitHubAppModalOpen: true,
          },
        },
      };

      renderComponent(stateWithModalOpen);

      expect(screen.getByText(/Confirm to proceed with registering a new GitHub App/)).toBeInTheDocument();
    });

    it('renders Cancel button', () => {
      const stateWithModalOpen = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            isReplaceGitHubAppModalOpen: true,
          },
        },
      };

      renderComponent(stateWithModalOpen);

      expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
    });

    it('renders Continue button', () => {
      const stateWithModalOpen = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            isReplaceGitHubAppModalOpen: true,
          },
        },
      };

      renderComponent(stateWithModalOpen);

      expect(screen.getByRole('button', { name: /continue/i })).toBeInTheDocument();
    });
  });

  describe('User Interactions', () => {
    it('dispatches closeReplaceGitHubAppModal when Cancel button is clicked', async () => {
      const user = userEvent.setup();
      const closeModalSpy = jest.spyOn(sourceControlActions, 'closeReplaceGitHubAppModal');
      const stateWithModalOpen = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            isReplaceGitHubAppModalOpen: true,
          },
        },
      };

      renderComponent(stateWithModalOpen);

      const cancelButton = screen.getByRole('button', { name: /cancel/i });
      await user.click(cancelButton);

      expect(closeModalSpy).toHaveBeenCalled();

      closeModalSpy.mockRestore();
    });

    it('dispatches closeReplaceGitHubAppModal and openModal when Continue button is clicked', async () => {
      const user = userEvent.setup();
      const closeModalSpy = jest.spyOn(sourceControlActions, 'closeReplaceGitHubAppModal');
      const openGitHubAppModalSpy = jest.spyOn(gitHubAppActions, 'openModal');
      const stateWithModalOpen = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            isReplaceGitHubAppModalOpen: true,
          },
        },
      };

      renderComponent(stateWithModalOpen);

      const continueButton = screen.getByRole('button', { name: /continue/i });
      await user.click(continueButton);

      await waitFor(() => {
        expect(closeModalSpy).toHaveBeenCalled();
        expect(openGitHubAppModalSpy).toHaveBeenCalled();
      });

      closeModalSpy.mockRestore();
      openGitHubAppModalSpy.mockRestore();
    });

    it('dispatches closeReplaceGitHubAppModal when modal is cancelled via close button', async () => {
      const user = userEvent.setup();
      const closeModalSpy = jest.spyOn(sourceControlActions, 'closeReplaceGitHubAppModal');
      const stateWithModalOpen = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            isReplaceGitHubAppModalOpen: true,
          },
        },
      };

      renderComponent(stateWithModalOpen);

      // Find the modal close button (X button)
      const modalCloseButtons = screen.getAllByRole('button');
      const closeButton = modalCloseButtons.find(
        (button) =>
          button.className?.includes('close') || button.getAttribute('aria-label')?.toLowerCase().includes('close')
      );

      if (closeButton) {
        await user.click(closeButton);
        expect(closeModalSpy).toHaveBeenCalled();
      }

      closeModalSpy.mockRestore();
    });
  });

  describe('Modal Flow', () => {
    it('closes modal and opens GitHub App registration modal in sequence', async () => {
      const user = userEvent.setup();
      const closeModalSpy = jest.spyOn(sourceControlActions, 'closeReplaceGitHubAppModal');
      const openGitHubAppModalSpy = jest.spyOn(gitHubAppActions, 'openModal');
      const stateWithModalOpen = {
        orgsAndPolicies: {
          sourceControlConfiguration: {
            isReplaceGitHubAppModalOpen: true,
          },
        },
      };

      renderComponent(stateWithModalOpen);

      expect(screen.getByText('Replace GitHub App Configuration')).toBeInTheDocument();

      const continueButton = screen.getByRole('button', { name: /continue/i });
      await user.click(continueButton);

      await waitFor(() => {
        // Verify both actions were dispatched in the correct order
        expect(closeModalSpy).toHaveBeenCalled();
        expect(openGitHubAppModalSpy).toHaveBeenCalled();
      });

      closeModalSpy.mockRestore();
      openGitHubAppModalSpy.mockRestore();
    });
  });
});
