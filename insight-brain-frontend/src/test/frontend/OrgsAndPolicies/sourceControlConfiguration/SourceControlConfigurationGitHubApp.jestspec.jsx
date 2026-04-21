/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import SourceControlConfiguration from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/SourceControlConfiguration';

describe.skip('SourceControlConfiguration - GitHub App Success Flow', () => {
  let axiosMock;

  const defaultPreloadedState = {
    orgsAndPolicies: {
      root: {
        selectedOwner: {
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
          type: 'ORGANIZATION',
        },
        loadError: null,
        loading: false,
      },
      sourceControlConfiguration: {
        formLoading: false,
        loadError: null,
        submitError: null,
        submitMaskState: null,
        resetSubmitError: null,
        sourceControl: {
          provider: { value: 'GitHub', rscValue: { value: 'GitHub' }, isInherited: false },
          repositoryUrl: {
            value: 'https://github.com/test/repo',
            rscValue: { value: 'https://github.com/test/repo' },
            isInherited: false,
          },
          token: { rscValue: { value: '', isPristine: true }, isInherited: false },
          username: { rscValue: { value: '' }, isInherited: false },
          baseBranch: { rscValue: { value: 'main' }, isInherited: false },
          remediationPullRequestsEnabled: { value: false, isInherited: false },
          manualPullRequestsEnabled: { value: false, isInherited: false },
          closePrAfterDays: { rscValue: { value: '' }, isInherited: false },
          closePrAfterDaysOpenEnabled: { value: false, isInherited: false },
          closePrOnFailedChecksEnabled: { value: false, isInherited: false },
          githubApp: {
            value: {
              installationId: '12345',
              name: 'sonatype-iq-server',
              accountName: 'test-org',
              configurationDate: '2025-02-04T00:00:00Z',
            },
          },
        },
        serverSourceControl: null,
        sourceControlMetrics: { results: [] },
        scmConfigValidation: {
          result: null,
          error: null,
          loading: false,
        },
        isResetModalOpen: false,
        isConfirmationModalOpen: false,
        isDirty: false,
        isRepoUrlDirty: false,
        showGitHubAppSuccessModal: false,
      },
    },
    router: {
      currentParams: { organizationId: 'ROOT_ORGANIZATION_ID' },
      currentState: { name: 'organizations.edit.sourceControl' },
    },
    productFeatures: {
      productFeatures: {
        'source-control-for-source-tile-supported': true,
        automation: true,
        'github-app-authentication': true,
      },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    // Mock axios calls
    axiosMock.onGet(/\/api\/v2\/compositeSourceControl/).reply(200, {
      provider: 'GitHub',
      repositoryUrl: 'https://github.com/test-org',
      remediationPullRequestsEnabled: false,
      manualPullRequestsEnabled: false,
      githubApp: {
        value: {
          installationId: '12345',
          name: 'sonatype-iq-server',
          accountName: 'test-org',
          configurationDate: '2025-02-04T00:00:00Z',
        },
      },
    });

    axiosMock.onGet(/\/api\/v2\/sourceControl\/.*\/metrics/).reply(200, { results: [] });
  });

  afterEach(() => {
    axiosMock.reset();
    jest.clearAllTimers();
  });

  const renderComponent = (preloadedState = defaultPreloadedState) => {
    return render(<SourceControlConfiguration />, { preloadedState });
  };

  describe('Modal Opening', () => {
    it('should open modal when githubAppId parameter is present in route params', async () => {
      const stateWithSuccessParam = {
        ...defaultPreloadedState,
        router: {
          ...defaultPreloadedState.router,
          currentParams: {
            ...defaultPreloadedState.router.currentParams,
            githubAppId: 'github-app-12345',
          },
        },
      };

      renderComponent(stateWithSuccessParam);

      await waitFor(() => {
        expect(screen.getByText('GitHub Setup Complete')).toBeInTheDocument();
      });
    });

    it('should not open modal when githubAppId parameter is not present', async () => {
      renderComponent();

      await waitFor(() => {
        expect(screen.queryByText('GitHub Setup Complete')).not.toBeInTheDocument();
      });
    });

    it('should not reopen modal after modal is already showing', async () => {
      const stateWithSuccessParam = {
        ...defaultPreloadedState,
        router: {
          ...defaultPreloadedState.router,
          currentParams: {
            ...defaultPreloadedState.router.currentParams,
            githubAppId: 'github-app-12345',
          },
        },
        orgsAndPolicies: {
          ...defaultPreloadedState.orgsAndPolicies,
          sourceControlConfiguration: {
            ...defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration,
            showGitHubAppSuccessModal: true,
          },
        },
      };

      renderComponent(stateWithSuccessParam);

      // Modal should already be open from state
      expect(screen.getByText('GitHub Setup Complete')).toBeInTheDocument();
    });
  });

  describe('Feature Enabling', () => {
    it('should enable PR features in UI state when Done is clicked (persisted on Save)', async () => {
      const user = userEvent.setup({ delay: null });
      const stateWithModalOpen = {
        ...defaultPreloadedState,
        orgsAndPolicies: {
          ...defaultPreloadedState.orgsAndPolicies,
          sourceControlConfiguration: {
            ...defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration,
            showGitHubAppSuccessModal: true,
            sourceControl: {
              ...defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration.sourceControl,
              remediationPullRequestsEnabled: { value: false },
              manualPullRequestsEnabled: { value: false },
            },
          },
        },
      };

      renderComponent(stateWithModalOpen);

      const doneButton = screen.getByRole('button', { name: 'Done' });
      await user.click(doneButton);

      // Modal should close (features enabled in UI state, will be persisted when user clicks Save)
      await waitFor(() => {
        expect(screen.queryByText('GitHub Setup Complete')).not.toBeInTheDocument();
      });
    });

    it('should display correct message when both PR features are newly enabled', () => {
      const stateWithModalOpen = {
        ...defaultPreloadedState,
        orgsAndPolicies: {
          ...defaultPreloadedState.orgsAndPolicies,
          sourceControlConfiguration: {
            ...defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration,
            showGitHubAppSuccessModal: true,
            sourceControl: {
              ...defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration.sourceControl,
              remediationPullRequestsEnabled: { value: false },
              manualPullRequestsEnabled: { value: false },
            },
          },
        },
      };

      renderComponent(stateWithModalOpen);

      expect(screen.getByText('Create Golden PRs')).toBeInTheDocument();
      expect(screen.getByText('Recommend Manual Pull Requests')).toBeInTheDocument();
    });

    it('should display correct message when only Golden PRs newly enabled', () => {
      const stateWithModalOpen = {
        ...defaultPreloadedState,
        orgsAndPolicies: {
          ...defaultPreloadedState.orgsAndPolicies,
          sourceControlConfiguration: {
            ...defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration,
            showGitHubAppSuccessModal: true,
            sourceControl: {
              ...defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration.sourceControl,
              remediationPullRequestsEnabled: { value: false },
              manualPullRequestsEnabled: { value: true },
            },
          },
        },
      };

      renderComponent(stateWithModalOpen);

      expect(screen.getByText('Create Golden PRs')).toBeInTheDocument();
      expect(screen.queryByText('Recommend Manual Pull Requests')).not.toBeInTheDocument();
    });

    it('should display correct message when only Manual PRs newly enabled', () => {
      const stateWithModalOpen = {
        ...defaultPreloadedState,
        orgsAndPolicies: {
          ...defaultPreloadedState.orgsAndPolicies,
          sourceControlConfiguration: {
            ...defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration,
            showGitHubAppSuccessModal: true,
            sourceControl: {
              ...defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration.sourceControl,
              remediationPullRequestsEnabled: { value: true },
              manualPullRequestsEnabled: { value: false },
            },
          },
        },
      };

      renderComponent(stateWithModalOpen);

      expect(screen.queryByText('Create Golden PRs')).not.toBeInTheDocument();
      expect(screen.getByText('Recommend Manual Pull Requests')).toBeInTheDocument();
    });
  });

  describe('Modal Content', () => {
    it('should display server ID and organization name', () => {
      const stateWithModalOpen = {
        ...defaultPreloadedState,
        orgsAndPolicies: {
          ...defaultPreloadedState.orgsAndPolicies,
          sourceControlConfiguration: {
            ...defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration,
            showGitHubAppSuccessModal: true,
          },
        },
      };

      renderComponent(stateWithModalOpen);

      expect(screen.getByText(/sonatype-iq-server/i)).toBeInTheDocument();
      expect(screen.getByText(/test-org/i)).toBeInTheDocument();
    });

    it('should handle missing server ID gracefully', () => {
      const stateWithModalOpen = {
        ...defaultPreloadedState,
        orgsAndPolicies: {
          ...defaultPreloadedState.orgsAndPolicies,
          sourceControlConfiguration: {
            ...defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration,
            showGitHubAppSuccessModal: true,
            sourceControl: {
              ...defaultPreloadedState.orgsAndPolicies.sourceControlConfiguration.sourceControl,
              githubApp: { value: null },
            },
          },
        },
      };

      renderComponent(stateWithModalOpen);

      expect(screen.getByText('GitHub Setup Complete')).toBeInTheDocument();
    });
  });
});
