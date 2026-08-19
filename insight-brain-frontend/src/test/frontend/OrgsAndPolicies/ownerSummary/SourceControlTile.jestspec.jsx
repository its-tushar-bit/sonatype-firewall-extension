/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';
import SourceControlTile from 'MainRoot/OrgsAndPolicies/ownerSummary/SourceControlTile';
import { getCompositeSourceControlUrl } from 'MainRoot/util/CLMLocation';

describe('SourceControlTile', () => {
  let renderComponent, axiosMock, state;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    state = {
      router: {
        currentState: { name: 'organization' },
        currentParams: {
          organizationId: 'organizationId',
        },
      },
      productFeatures: {
        productFeatures: {
          'saas-lifecycle-scm-enabled': true,
          notifications: true,
          automation: true,
        },
      },
      orgsAndPolicies: {
        sourceControl: {
          loading: false,
        },
        root: {
          selectedOwner: {
            name: 'org',
            id: 'orgId',
          },
        },
      },
    };

    axiosMock.onGet(getCompositeSourceControlUrl('organization', 'orgId')).reply(200, {
      provider: {
        value: 'github',
      },
      token: { value: 'asdf' },
    });

    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue({
      href: jest.fn().mockReturnValue('editPageHref'),
    });

    renderComponent = async (preloadedState = state) => {
      const component = render(<SourceControlTile />, { preloadedState });
      const loadingSpinner = screen.queryByRole('status');

      if (loadingSpinner) {
        await waitFor(() => expect(loadingSpinner).not.toBeInTheDocument());
      }

      return component;
    };
  });

  it('renders loading indicator', () => {
    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders error alert on load error', async () => {
    axiosMock.onGet(getCompositeSourceControlUrl('organization', 'orgId')).reply(500, 'loadError');
    await renderComponent();

    const error = await screen.getByRole('alert');

    expect(error).toBeVisible();
  });

  describe('tile header`s subtitle text', () => {
    it('renders subtitle for application', async () => {
      state.orgsAndPolicies.root.selectedOwner = { name: 'app', id: 'appId', publicId: 'appPublicId' };
      state.router.currentState.name = 'application';
      state.router.currentParams = { applicationId: 'appId' };
      axiosMock.onGet(getCompositeSourceControlUrl('application', 'appId')).reply(200, {
        provider: {
          value: 'github',
        },
        token: { value: 'asdf' },
      });

      await renderComponent();

      expect(screen.getByText('Configures the integration with an external SCM for the app application')).toBeVisible();
    });

    it('renders subtitle for organization', async () => {
      await renderComponent();

      expect(
        screen.getByText('Configures the integration with an external SCM for the org organization')
      ).toBeVisible();
    });

    it('renders subtitle for root organization', async () => {
      state.orgsAndPolicies.root.selectedOwner = { name: 'Root Organization', id: 'ROOT_ORGANIZATION_ID' };
      state.router.currentState.name = 'organization';
      state.router.currentParams = { organizationId: 'ROOT_ORGANIZATION_ID' };
      axiosMock.onGet(getCompositeSourceControlUrl('organization', 'ROOT_ORGANIZATION_ID')).reply(200, {
        provider: {
          value: 'github',
        },
        token: { value: 'asdf' },
      });

      await renderComponent();

      expect(
        screen.getByText('Configures the integration with an external SCM for the Root Organization')
      ).toBeVisible();
    });
  });

  it('renders item text and subtext', async () => {
    await renderComponent();

    expect(screen.getByText('GitHub')).toBeVisible();
    expect(screen.getByText('Provides default access token for org')).toBeVisible();
  });

  it('renders item subtext only', async () => {
    delete state.orgsAndPolicies.sourceControl.data;
    axiosMock.onGet(getCompositeSourceControlUrl('organization', 'orgId')).reply(200);
    await renderComponent();

    expect(screen.queryByText('GitHub')).not.toBeInTheDocument();
    expect(screen.getByText('Source Control not configured')).toBeVisible();
  });

  it('renders link with href to Source Control Configuration page', async () => {
    await renderComponent();

    const linkItem = screen.getByText('Provides default access token for org');
    expect(linkItem.closest('a')).toHaveAttribute('href', 'editPageHref');
  });

  describe('saas-lifecycle-scm-enabled feature', () => {
    const ownerId = 'e270271429f747ef9bebf4ca88f5e6c0';

    it('does not render when the saas-lifecycle-scm-enabled is not present', async () => {
      const state = {
        productFeatures: {
          productFeatures: {},
        },
        router: {
          currentState: {
            name: 'management.view.organization',
            url: '/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              name: 'org',
              id: 'orgId',
            },
          },
        },
      };

      await renderComponent(state);

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Source Control');
      expect(title).toBeNull();
    });

    it('does not render when the saas-lifecycle-scm-enabled is false', async () => {
      const state = {
        productFeatures: {
          productFeatures: { 'saas-lifecycle-scm-enabled': false },
        },
        router: {
          currentState: {
            name: 'management.view.organization',
            url: '/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              name: 'org',
              id: 'orgId',
            },
          },
        },
      };

      await renderComponent(state);

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Source Control');
      expect(title).toBeNull();
    });

    it('renders when the saas-lifecycle-scm-enabled is true', async () => {
      const state = {
        productFeatures: {
          productFeatures: {
            'saas-lifecycle-scm-enabled': true,
            notifications: true,
            automation: true,
          },
        },
        router: {
          currentState: {
            name: 'management.view.organization',
            url: '/organization/{organizationId}',
            data: {
              title: 'Organization Management',
              viewportSized: true,
            },
          },
          currentParams: {
            organizationId: ownerId,
          },
        },
        orgsAndPolicies: {
          root: {
            selectedOwner: {
              name: 'org',
              id: 'orgId',
            },
          },
        },
      };

      await renderComponent(state);

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Source Control');
      expect(title).not.toBeNull();
    });
  });
});
