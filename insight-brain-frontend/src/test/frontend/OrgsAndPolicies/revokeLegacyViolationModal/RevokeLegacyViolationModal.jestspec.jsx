/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import RevokeLegacyViolationModal from 'MainRoot/OrgsAndPolicies/revokeLegacyViolationModal/RevokeLegacyViolationModal';
import { fireEvent, render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getRevokeLegacyViolationUrl } from 'MainRoot/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('RevokeLegacyViolationModal', () => {
  let renderComponent, axiosMock;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    const defaultPreloadedState = {
      orgsAndPolicies: {
        ownerActions: {
          revokeLegacyViolations: {
            submitError: null,
            isModalOpen: true,
          },
        },
      },
    };

    renderComponent = (preloadedState) =>
      render(<RevokeLegacyViolationModal />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('does not render the modal when isModalOpen is false', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          revokeLegacyViolations: {
            submitError: null,
            isModalOpen: false,
          },
        },
      },
    });

    const initialTitle = screen.queryAllByText('Revoke Legacy Violation Status');
    expect(initialTitle.length).toBe(0);
  });

  it('renders modal with correct content', () => {
    renderComponent();
    expect(
      screen.getByText(
        'Subsequent scans and re-evaluations will treat applicable policy violations as active and trigger configured actions.'
      )
    ).toBeVisible();

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Revoke' })).toBeVisible();
  });

  it('renders error on submitError', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          revokeLegacyViolations: {
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

  it('triggers revokeLegacyViolations status', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          revokeLegacyViolations: {
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
    expect(submitButton).not.toHaveClass('disabled');
    fireEvent.click(submitButton);
    expect(axiosMock.history.put.length).toBe(1);
    expect(axiosMock.history.put[0].url).toBe(getRevokeLegacyViolationUrl('123123'));
  });

  it('close modal on cancel', () => {
    renderComponent();
    const closeButton = screen.getByRole('button', { name: 'Cancel' });

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(closeButton).toBeVisible();
    expect(closeButton).not.toHaveClass('disabled');
    fireEvent.click(closeButton);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
