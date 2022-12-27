/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import GrandfatheringModal from 'MainRoot/OrgsAndPolicies/grandfatheringModal/GrandfatheringModal';
import { fireEvent, render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getGrandfatheringModalUrl } from 'MainRoot/util/CLMLocation';

describe('Grandfathering modal', () => {
  let renderComponent, axiosMock;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    const defaultPreloadedState = {
      orgsAndPolicies: {
        ownerActions: {
          grandfathering: {
            submitError: null,
            isModalOpen: true,
          },
        },
      },
    };

    renderComponent = (preloadedState) =>
      render(<GrandfatheringModal />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('does not render the modal when isModalOpen is false', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          grandfathering: {
            submitError: null,
            isModalOpen: false,
          },
        },
      },
    });

    const initialTitle = screen.queryAllByText('Grandfather Policy Violations');
    expect(initialTitle.length).toBe(0);
  });

  it('renders modal with correct content', () => {
    renderComponent();
    expect(
      screen.getByText('Policy violations for the application will be grandfathered without performing an evaluation.')
    ).toBeVisible();

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Grandfather' })).toBeVisible();
  });

  it('renders error on submitError', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          grandfathering: {
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

  it('triggers grandfathering', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          grandfathering: {
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

    const submitButton = screen.getByRole('button', { name: 'Grandfather' });
    expect(submitButton).toBeVisible();
    expect(submitButton).not.toHaveClassName('disabled');
    fireEvent.click(submitButton);
    expect(axiosMock.history.put.length).toBe(1);
    expect(axiosMock.history.put[0].url).toBe(getGrandfatheringModalUrl('123123'));
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
