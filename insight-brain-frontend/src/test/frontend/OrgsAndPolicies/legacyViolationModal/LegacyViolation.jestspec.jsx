/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import LegacyViolationModal from 'MainRoot/OrgsAndPolicies/legacyViolationModal/LegacyViolationModal';
import { fireEvent, render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getLegacyViolationModalUrl } from 'MainRoot/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('LegacyViolation modal', () => {
  let renderComponent, axiosMock;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    const defaultPreloadedState = {
      orgsAndPolicies: {
        ownerActions: {
          legacyViolations: {
            submitError: null,
            isModalOpen: true,
          },
        },
      },
    };

    renderComponent = (preloadedState) =>
      render(<LegacyViolationModal />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('does not render the modal when isModalOpen is false', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          legacyViolations: {
            submitError: null,
            isModalOpen: false,
          },
        },
      },
    });

    const initialTitle = screen.queryAllByText('Grant Legacy Violation Status');
    expect(initialTitle.length).toBe(0);
  });

  it('renders modal with correct content', () => {
    renderComponent();
    expect(screen.getByText('This action itself does not perform a new scan or re-evaluation.')).toBeVisible();

    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Update' })).toBeVisible();
  });

  it('renders error on submitError', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          legacyViolations: {
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

  it('triggers legacyViolations status', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          legacyViolations: {
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

    const submitButton = screen.getByRole('button', { name: 'Update' });
    expect(submitButton).toBeVisible();
    expect(submitButton).not.toHaveClass('disabled');
    fireEvent.click(submitButton);
    expect(axiosMock.history.put.length).toBe(1);
    expect(axiosMock.history.put[0].url).toBe(getLegacyViolationModalUrl('123123'));
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
