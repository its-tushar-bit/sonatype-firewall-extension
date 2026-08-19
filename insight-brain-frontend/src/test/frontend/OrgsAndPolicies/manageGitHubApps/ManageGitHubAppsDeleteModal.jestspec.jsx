/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'TestRoot/SpecUtil';
import ManageGitHubAppsDeleteModal from 'MainRoot/OrgsAndPolicies/manageGitHubApps/ManageGitHubAppsDeleteModal';

describe('ManageGitHubAppsDeleteModal', () => {
  const defaultState = {
    manageGitHubApps: {
      githubApps: [],
      loading: false,
      error: null,
      deleteModal: {
        isOpen: true,
        app: { id: 'uuid-1', slug: 'my-app', appId: 123 },
        isDeleting: false,
      },
    },
    orgsAndPolicies: {
      root: { selectedOwner: { id: 'org-1' } },
    },
  };

  it('renders warning with app name', () => {
    render(<ManageGitHubAppsDeleteModal />, { preloadedState: defaultState });
    expect(screen.getByText(/my-app/)).toBeInTheDocument();
    expect(screen.getByText(/Remove GitHub App configuration/)).toBeInTheDocument();
  });

  it('calls delete on confirm', async () => {
    const user = userEvent.setup({ delay: null });
    render(<ManageGitHubAppsDeleteModal />, { preloadedState: defaultState });

    await user.click(screen.getByRole('button', { name: /Confirm Deletion/ }));
  });

  it('does not render when closed', () => {
    const closedState = {
      ...defaultState,
      manageGitHubApps: {
        ...defaultState.manageGitHubApps,
        deleteModal: { isOpen: false, app: null, isDeleting: false },
      },
    };
    const { container } = render(<ManageGitHubAppsDeleteModal />, { preloadedState: closedState });
    expect(container).toBeEmptyDOMElement();
  });
});
