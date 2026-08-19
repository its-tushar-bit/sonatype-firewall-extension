/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import userEvent from '@testing-library/user-event';
import { within } from '@testing-library/react';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import { getCreatePullRequestUrl } from 'MainRoot/util/CLMLocation';
import CreatePRModal from 'MainRoot/manualPullRequest/CreatePRModal';
import { actions } from 'MainRoot/manualPullRequest/createPRModalSlice';

describe('CreatePRModal', () => {
  let renderComponent, axiosMock, defaultPreloadedState;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    defaultPreloadedState = {
      applicationReport: {
        metadata: {
          application: {
            id: 'appId',
          },
        },
      },
    };

    renderComponent = (preloadedState) =>
      render(<CreatePRModal />, { preloadedState: { ...defaultPreloadedState, ...preloadedState } });
  });

  describe('show/hide modal', () => {
    it('hides modal when it is not open', () => {
      renderComponent();
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('shows the modal when it is open', async () => {
      const { store } = renderComponent();
      store.dispatch(actions.openModal(getModalPopulatedState()));

      const modal = await screen.findByRole('dialog');
      expect(modal).toBeInTheDocument();
      expect(modal).toHaveTextContent('Create Pull Request');
    });

    it('closes the modal on cancel', async () => {
      const { store } = renderComponent();
      store.dispatch(actions.openModal(getModalPopulatedState()));
      const user = userEvent.setup();

      const modal = await screen.findByRole('dialog');
      expect(modal).toBeInTheDocument();

      const cancelButton = within(modal).getByRole('button', { name: 'Cancel' });
      await user.click(cancelButton);
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });
  });

  describe('modal content', () => {
    it('renders the modal with provided data', async () => {
      const populatedState = getModalPopulatedState();
      const { store } = renderComponent();
      store.dispatch(actions.openModal(populatedState));

      await verifyModalContent({
        name: 'name',
        fullName: 'fullName',
        currentVersion: '1.0',
        targetVersion: '1.2',
        breakingChangesText: 'None',
      });
    });

    it('renders the modal with unknown breaking changes (null value)', async () => {
      const populatedState = getModalPopulatedState();
      const { store } = renderComponent();
      store.dispatch(
        actions.openModal({
          ...populatedState,
          breakingChangesCount: null,
        })
      );

      await verifyModalContent({
        name: 'name',
        fullName: 'fullName',
        currentVersion: '1.0',
        targetVersion: '1.2',
        breakingChangesText: 'Unknown',
      });
    });

    it('renders the modal with unknown breaking changes (negative value)', async () => {
      const populatedState = getModalPopulatedState();
      const { store } = renderComponent();
      store.dispatch(
        actions.openModal({
          ...populatedState,
          breakingChangesCount: -1,
        })
      );

      await verifyModalContent({
        name: 'name',
        fullName: 'fullName',
        currentVersion: '1.0',
        targetVersion: '1.2',
        breakingChangesText: 'Unknown',
      });
    });

    it('renders the modal with several breaking changes (lower bound)', async () => {
      const populatedState = getModalPopulatedState();
      const { store } = renderComponent();
      store.dispatch(
        actions.openModal({
          ...populatedState,
          breakingChangesCount: 1,
        })
      );

      await verifyModalContent({
        name: 'name',
        fullName: 'fullName',
        currentVersion: '1.0',
        targetVersion: '1.2',
        breakingChangesText: 'Few',
      });
    });

    it('renders the modal with several breaking changes (upper bound)', async () => {
      const populatedState = getModalPopulatedState();
      const { store } = renderComponent();
      store.dispatch(
        actions.openModal({
          ...populatedState,
          breakingChangesCount: 5,
        })
      );

      await verifyModalContent({
        name: 'name',
        fullName: 'fullName',
        currentVersion: '1.0',
        targetVersion: '1.2',
        breakingChangesText: 'Few',
      });
    });

    it('renders the modal with multiple breaking changes', async () => {
      const populatedState = getModalPopulatedState();
      const { store } = renderComponent();
      store.dispatch(
        actions.openModal({
          ...populatedState,
          breakingChangesCount: 6,
        })
      );

      await verifyModalContent({
        name: 'name',
        fullName: 'fullName',
        currentVersion: '1.0',
        targetVersion: '1.2',
        breakingChangesText: 'Multiple',
      });
    });

    describe('create button', () => {
      it('hides modal and shows submit mask when pressed', async () => {
        let resolveCreatePR;
        axiosMock.onPost(getCreatePullRequestUrl()).reply(() => {
          return new Promise((resolve) => {
            resolveCreatePR = () => {
              resolve([200, { id: '1234' }]);
            };
          });
        });
        const populatedState = getModalPopulatedState();
        const { store } = renderComponent();
        store.dispatch(actions.openModal(populatedState));
        const user = userEvent.setup();

        const modal = await screen.findByRole('dialog');
        expect(modal).toBeInTheDocument();
        expect(modal).toHaveTextContent('Create Pull Request');

        const createButton = within(modal).getByRole('button', { name: 'Create' });
        await user.click(createButton);
        expect(modal).not.toBeInTheDocument();

        expect(await screen.findByRole('status')).toHaveTextContent('Submitting…');
        resolveCreatePR();

        await waitFor(() => {
          expect(screen.getByRole('status')).toHaveTextContent('Success!');
        });
      });

      it('hides modal and shows retry alert when button is pressed and error is returned', async () => {
        const errorMessage = 'SCM is not configured.';
        let resolveCreatePRFailed;
        axiosMock.onPost(getCreatePullRequestUrl()).replyOnce(() => {
          return new Promise((resolve) => {
            resolveCreatePRFailed = () => {
              resolve([404, { message: errorMessage }]);
            };
          });
        });
        let resolveCreatePRSuccess;
        axiosMock.onPost(getCreatePullRequestUrl()).reply(() => {
          return new Promise((resolve) => {
            resolveCreatePRSuccess = () => {
              resolve([200, { id: '1234' }]);
            };
          });
        });
        const populatedState = getModalPopulatedState();
        const { store } = renderComponent();
        store.dispatch(actions.openModal(populatedState));
        const user = userEvent.setup();

        const modal = await screen.findByRole('dialog');
        expect(modal).toBeInTheDocument();
        expect(modal).toHaveTextContent('Create Pull Request');

        const createButton = within(modal).getByRole('button', { name: 'Create' });
        await user.click(createButton);
        expect(modal).not.toBeInTheDocument();

        expect(await screen.findByRole('status')).toHaveTextContent('Submitting…');
        resolveCreatePRFailed();

        let modalWithRetry = await screen.findByRole('dialog');
        expect(modalWithRetry).toHaveTextContent('Create Pull Request');
        expect(within(modalWithRetry).getByRole('alert')).toHaveTextContent(
          `Failure to create pull request. ${errorMessage}`
        );

        const retryButton = within(modalWithRetry).getByRole('button', { name: 'Retry' });
        expect(retryButton).toBeVisible();
        expect(retryButton).toBeEnabled();
        await user.click(retryButton);

        expect(await screen.findByRole('status')).toHaveTextContent('Submitting…');
        resolveCreatePRSuccess();

        await waitFor(() => {
          expect(screen.getByRole('status')).toHaveTextContent('Success!');
        });

        expect(retryButton).not.toBeInTheDocument();
        expect(modal).not.toBeInTheDocument();
      });
    });

    async function verifyModalContent({ name, fullName, currentVersion, targetVersion, breakingChangesText }) {
      const modal = await screen.findByRole('dialog');
      expect(modal).toBeInTheDocument();
      expect(modal).toHaveTextContent('Create Pull Request');

      const prTitleLabel = within(modal).getByText('Title').closest('dt');
      expect(prTitleLabel).toBeVisible();
      expect(prTitleLabel.nextElementSibling).toHaveTextContent(`Bump ${name} to ${targetVersion}`);

      const componentNameLabel = within(modal).getByText('Component').closest('dt');
      expect(componentNameLabel).toBeVisible();
      expect(componentNameLabel.nextElementSibling).toHaveTextContent(fullName);

      const currentVersionLabel = within(modal).getByText('Current Version').closest('dt');
      expect(currentVersionLabel).toBeVisible();
      expect(currentVersionLabel.nextElementSibling).toHaveTextContent(currentVersion);

      const suggestedVersionLabel = within(modal).getByText('Suggested Version').closest('dt');
      expect(suggestedVersionLabel).toBeVisible();
      expect(suggestedVersionLabel.nextElementSibling).toHaveTextContent(targetVersion);

      const breakingChangesLabel = within(modal).getByText('Breaking Changes').closest('dt');
      expect(breakingChangesLabel).toBeVisible();
      expect(breakingChangesLabel.nextElementSibling).toHaveTextContent(breakingChangesText);
    }
  });

  function getModalPopulatedState() {
    return {
      name: 'name',
      fullName: 'fullName',
      currentVersion: '1.0',
      targetVersion: '1.2',
      breakingChangesCount: 0,
      defaultBranch: 'main',
      scanId: 'scanId',
      identificationSource: 'identificationSource',
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'artifactId',
          version: '1.0',
        },
      },
      isDirectDependency: true,
      submitMaskState: null,
      branchName: 'main',
      error: null,
    };
  }
});
