/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import RevokeGrandfatheringModal from 'MainRoot/OrgsAndPolicies/revokeGrandfatheringModal/RevokeGrandfatheringModal';
import { fireEvent, render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getRevokeGrandfatheringUrl } from 'MainRoot/util/CLMLocation';

describe('RevokeGrandfatheringModal', () => {
  let renderComponent, axiosMock;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    const defaultPreloadedState = {
      orgsAndPolicies: {
        ownerEditor: {
          revokeGrandfathering: {
            submitError: null,
            isModalOpen: true,
          },
        },
      },
    };

    renderComponent = (preloadedState) =>
      render(<RevokeGrandfatheringModal />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('does not render the modal when isModalOpen is false', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerEditor: {
          revokeGrandfathering: {
            submitError: null,
            isModalOpen: false,
          },
        },
      },
    });

    const initialTitle = screen.queryAllByText('Revoke Grandfathered Policy Violations');
    expect(initialTitle.length).toBe(0);
  });

  it('renders modal with correct content', () => {
    renderComponent();
    expect(
      screen.getByText(
        'Revoking the grandfathered policy violations for the application will reinstate violations if applicable.'
      )
    ).toBeVisible();

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Revoke' })).toBeVisible();
  });

  it('renders error on submitError', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerEditor: {
          revokeGrandfathering: {
            submitError: 'Error 404',
            isModalOpen: true,
          },
        },
      },
    });

    const error = screen.getByRole('alert');

    expect(error).toBeVisible();
    expect(screen.getByText('An error occurred saving data. Error 404')).toBeVisible();
  });

  it('triggers revokeGrandfathering', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerEditor: {
          revokeGrandfathering: {
            submitError: null,
            isModalOpen: true,
          },
        },
        root: {
          selectedOwner: {
            publicId: '123123',
          },
        },
      },
    });

    const submitButton = screen.getByRole('button', { name: 'Revoke' });

    expect(submitButton).toBeVisible();
    expect(submitButton).not.toHaveClassName('disabled');
    fireEvent.click(submitButton);
    expect(axiosMock.history.put.length).toBe(1);
    expect(axiosMock.history.put[0].url).toBe(getRevokeGrandfatheringUrl('123123'));
  });

  it('close modal on cancel', () => {
    renderComponent();
    const closeButton = screen.getByRole('button', { name: 'Cancel' });

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(closeButton).toBeVisible();
    expect(closeButton).not.toHaveClassName('disabled');
    fireEvent.click(closeButton);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
