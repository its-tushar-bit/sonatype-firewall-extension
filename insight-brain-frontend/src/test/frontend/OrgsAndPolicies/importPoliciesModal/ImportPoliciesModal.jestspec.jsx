/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import ImportPoliciesModal from 'MainRoot/OrgsAndPolicies/importPoliciesModal/ImportPoliciesModal';
import { fireEvent, render, screen } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

describe('ImportPoliciesModal', () => {
  const defaultPreloadedState = {
    orgsAndPolicies: {
      ownerActions: {
        importPolicies: {
          submitError: null,
          isModalOpen: true,
          ownerFile: { isPristine: true, files: null },
        },
      },
    },
  };

  const renderComponent = (preloadedState) =>
    render(<ImportPoliciesModal />, { preloadedState: preloadedState || defaultPreloadedState });

  it('does not render the modal when isModalOpen is false', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          importPolicies: {
            submitError: null,
            isModalOpen: false,
            ownerFile: { isPristine: true, files: null },
          },
        },
      },
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('renders modal with correct content', () => {
    renderComponent();
    expect(screen.getByText('Import Policies')).toBeVisible();
    const textToFind = /Note: Importing policies is destructive, all existing policies, waivers, and license threat groups belonging to this organization and any of its descendants will be permanently deleted before importing./i;
    expect(screen.getByRole('dialog')).toHaveTextContent(textToFind);
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Import' })).toBeVisible();
  });

  it('renders error on submitError', () => {
    renderComponent({
      orgsAndPolicies: {
        ownerActions: {
          importPolicies: {
            submitError: 'Error 404',
            isModalOpen: true,
            ownerFile: { isPristine: true, files: null },
          },
        },
      },
    });

    const error = screen.getByRole('alert');
    expect(error).toBeVisible();
    expect(screen.getByText('An error occurred saving data. Error 404')).toBeVisible();
  });

  it('closes modal on cancel', () => {
    renderComponent();
    const closeButton = screen.getByRole('button', { name: 'Cancel' });
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(closeButton).toBeVisible();
    expect(closeButton).not.toHaveClass('disabled');
    fireEvent.click(closeButton);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
