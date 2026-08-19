/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, render, within, axiosMockAdapter } from 'TestRoot/SpecUtil';
import DeleteAutoWaiverModal from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/DeleteAutoWaiverModal';
import userEvent from '@testing-library/user-event';
import { getAutoWaiversConfigurationURLWaiver } from 'MainRoot/util/CLMLocation';

describe('DeleteAutoWaiverModal', () => {
  let axiosMock, renderComponent;

  const defaultPreloadedState = {
    orgsAndPolicies: {
      root: {
        selectedOwner: {
          id: 'Application1',
          publicId: 'Application1',
          name: 'Application1',
        },
      },
      autoWaivers: {
        applicableAutoWaivers: {
          isDeleteModalOpen: true,
          deleteError: null,
          autoWaiverIdToDelete: 'idToDelete',
        },
      },
    },
    router: {
      currentState: {
        name: 'management.edit.application.auto-waivers-config',
      },
      currentParams: {
        applicationPublicId: 'Application1',
      },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    renderComponent = (preloadedState) =>
      render(<DeleteAutoWaiverModal />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('should render the modal with the correct title and content', () => {
    renderComponent();

    expect(screen.getByTestId('iq-delete-auto-waiver-modal')).toBeInTheDocument();

    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText('Delete Auto-Waiver')).toBeInTheDocument();
    expect(
      within(dialog).getByText('You are about to permanently delete an auto-waiver. This action cannot be undone.')
    ).toBeInTheDocument();
  });

  it('should render the cancel button', async () => {
    renderComponent();

    const dialog = screen.getByRole('dialog');
    const cancelButton = within(dialog).getByRole('button', { name: 'Cancel' });
    expect(cancelButton).toBeInTheDocument();
  });

  it('should render the delete button', async () => {
    const user = userEvent.setup();
    renderComponent();

    const dialog = screen.getByRole('dialog');
    const deleteButton = within(dialog).getByRole('button', { name: 'Delete' });
    expect(deleteButton).toBeInTheDocument();

    await user.click(deleteButton);
    expect(axiosMock.history.delete.length).toBe(1);
    expect(axiosMock.history.delete[0].url).toBe(
      getAutoWaiversConfigurationURLWaiver('application', 'Application1', 'idToDelete')
    );
  });

  it('should render the submit error message when deleteError is present', () => {
    renderComponent({
      ...defaultPreloadedState,
      orgsAndPolicies: {
        autoWaivers: {
          applicableAutoWaivers: {
            ...defaultPreloadedState.orgsAndPolicies.applicableAutoWaivers,
            deleteError: 'Error deleting auto waiver',
          },
        },
      },
    });

    const error = screen.getByRole('alert');
    expect(error).toBeInTheDocument();
    expect(error).toHaveTextContent('Error deleting auto waiver');
  });
});
