/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, waitFor } from 'TestRoot/SpecUtil';
import * as sourceControlSelectors from 'MainRoot/OrgsAndPolicies/sourceControlSelectors';
import { actions as sourceControlActions } from 'MainRoot/OrgsAndPolicies/sourceControlSlice';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as rootSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';
import SourceControlTile from 'MainRoot/OrgsAndPolicies/ownerSummary/SourceControlTile';

describe('SourceControlTile', () => {
  let renderComponent, selectIsOrganizationSpy, selectIsSourceControlForSourceTileSupportedSpy;

  beforeEach(() => {
    spyOn(routerSelectors, 'selectRouterSlice').and.returnValue(() => ({
      currentState: { name: 'organization' },
      currentParams: {
        organizationId: 'organizationId',
      },
    }));

    spyOn(sourceControlActions, 'loadSourceControl').and.returnValue({
      type: 'sourceControl/loadSourceControl/fulfilled',
      payload: {},
    });

    selectIsOrganizationSpy = spyOn(routerSelectors, 'selectIsOrganization').and.returnValue(true);
    selectIsSourceControlForSourceTileSupportedSpy = spyOn(
      productFeaturesSelectors,
      'selectIsSourceControlForSourceTileSupported'
    ).and.returnValue(true);
    spyOn(sourceControlSelectors, 'selectItemText').and.returnValue('itemText');
    spyOn(sourceControlSelectors, 'selectItemSubText').and.returnValue('itemSubText');
    spyOn(rootSelectors, 'selectSelectedOwnerName').and.returnValue('owner');
    renderComponent = () => render(<SourceControlTile />);
  });

  it('renders loading indicator', () => {
    spyOn(sourceControlSelectors, 'selectLoading').and.returnValue(true);

    renderComponent();

    expect(screen.getByText('Loading…')).toBeVisible();
  });

  it('renders error alert on load error', () => {
    spyOn(sourceControlSelectors, 'selectLoadError').and.returnValue('loadError');
    renderComponent();

    const error = screen.getByRole('alert');

    expect(error).toBeVisible();
  });

  describe('tile header`s subtitle text', () => {
    it('renders subtitle for application', () => {
      selectIsOrganizationSpy.and.returnValue(false);

      renderComponent();

      expect(
        screen.getByText('Configures the integration with an external SCM for the owner application')
      ).toBeVisible();
    });

    it('renders subtitle for organization', () => {
      renderComponent();

      expect(
        screen.getByText('Configures the integration with an external SCM for the owner organization')
      ).toBeVisible();
    });

    it('renders subtitle for root organization', () => {
      spyOn(routerSelectors, 'selectIsRootOrganization').and.returnValue(true);

      renderComponent();

      expect(screen.getByText('Configures the integration with an external SCM for the owner')).toBeVisible();
    });
  });

  it('renders source control not supported alert', () => {
    selectIsSourceControlForSourceTileSupportedSpy.and.returnValue(false);

    renderComponent();

    expect(screen.getByText('Source Control is not supported by your license')).toBeVisible();
  });

  it('renders item text and subtext', () => {
    spyOn(sourceControlSelectors, 'selectSourceControl').and.returnValue({});
    spyOn(sourceControlSelectors, 'selectEffectiveProvider').and.returnValue('someProvider');

    renderComponent();

    expect(screen.getByText('itemText')).toBeVisible();
    expect(screen.getByText('itemSubText')).toBeVisible();
  });

  it('renders item subtext only', () => {
    renderComponent();

    expect(screen.queryByText('itemText')).toBe(null);
    expect(screen.getByText('itemSubText')).toBeVisible();
  });

  it('renders link with href to Source Control Configuration page', () => {
    spyOn(routerStateContext, 'useRouterState').and.returnValue({
      href: jasmine.createSpy('href').and.returnValue('editPageHref'),
    });
    renderComponent();

    const linkItem = screen.getByText('itemSubText');
    expect(linkItem.closest('a')).toHaveAttribute('href', 'editPageHref');
  });

  describe('FirewallOnlyLicense', () => {
    const ownerId = 'e270271429f747ef9bebf4ca88f5e6c0';

    const renderComponentWithState = (preloadedState) => render(<SourceControlTile />, { preloadedState });

    it('does not render with a Firewall only license', async () => {
      const state = {
        productLicense: {
          license: {
            products: ['Firewall'],
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
              id: ownerId,
              name: 'broadcast-org',
            },
          },
        },
      };

      renderComponentWithState(state);

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Source Control');
      expect(title).toBeNull();
    });

    it('renders with a non-Firewall only license', async () => {
      const state = {
        productLicense: {
          license: {
            products: ['CLM'],
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
              id: ownerId,
              name: 'broadcast-org',
            },
          },
        },
      };

      renderComponentWithState(state);

      await waitFor(() => expect(screen.queryByText('Loading')).toBeNull());
      const title = await screen.queryByText('Source Control');
      expect(title).not.toBeNull();
    });
  });
});
