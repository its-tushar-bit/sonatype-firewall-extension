/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter, within } from 'TestRoot/SpecUtil';
import { getProductFeaturesUrl, getApplicableAutoWaiversURL } from 'MainRoot/util/CLMLocation';
import AutoWaiversConfiguration from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiversConfiguration';
import { DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/LicenseLockScreenForAutoWaivers';
import {
  mockResponse_Application_All_Local,
  mockResponse_Application_Local_Org_RootOrg,
  mockResponse_Application_Local_RootOrg,
  mockResponse_Organization_Local,
  mockResponse_Organization_Local_RootOrg,
  mockResponse_RootOrg_Local,
} from './mockApplicableWaiversResponses';
import { formatDate } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiversConfiguration';
import userEvent from '@testing-library/user-event';

describe('Auto Waivers Configuration Component', () => {
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
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['developer-dashboard', 'auto-waivers', 'auto-waiver-management']);

    renderComponent = (preloadedState) =>
      render(<AutoWaiversConfiguration />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders a loading spinner', () => {
    renderComponent();

    const loading = screen.getByText('Loading…');
    expect(loading).toBeInTheDocument();

    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getProductFeaturesUrl());
  });

  it('displays an error message when the product features request fails', async () => {
    axiosMock.onGet(getProductFeaturesUrl()).reply(500, 'Unable to load product features');
    renderComponent();
    const errorMessage = await screen.findByRole('alert');
    expect(errorMessage).toBeInTheDocument();
    expect(errorMessage).toHaveTextContent('Unable to load product features');
    expect(axiosMock.history.get.length).toBe(1);
    expect(axiosMock.history.get[0].url).toBe(getProductFeaturesUrl());
  });

  it('displays an error message when the auto-waivers request fails', async () => {
    axiosMock
      .onGet(getApplicableAutoWaiversURL('application', 'Application1'))
      .reply(500, 'Unable to load auto waivers');
    renderComponent();
    const errorMessage = await screen.findByRole('alert');
    expect(errorMessage).toBeInTheDocument();
    expect(errorMessage).toHaveTextContent('Unable to load auto waivers');
  });

  it('makes correct requests to get auto-waivers', async () => {
    axiosMock.onGet(getApplicableAutoWaiversURL('application', 'Application1')).reply(200, []);
    renderComponent();

    await screen.findByRole('table');
    expect(axiosMock.history.get.length).toBe(2);
    expect(axiosMock.history.get[0].url).toBe(getProductFeaturesUrl());
    expect(axiosMock.history.get[1].url).toBe(getApplicableAutoWaiversURL('application', 'Application1'));
  });

  it('renders an empty message when there are no auto-waivers', async () => {
    axiosMock.onGet(getApplicableAutoWaiversURL('application', 'Application1')).reply(200, []);
    renderComponent();
    const emptyMessage = await screen.findByText('No automations to display');
    expect(emptyMessage).toBeInTheDocument();
  });

  describe('missing features', () => {
    it('displays LicenseLockScreen if all features are missing', async () => {
      axiosMock.onGet(getProductFeaturesUrl()).reply(200, []);

      renderComponent();

      const alert = await screen.findByRole('alert');
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent(DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS);
    });

    it('displays LicenseLockScreen if developer-dashboard feature is missing', async () => {
      axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['auto-waivers']);

      renderComponent();

      const alert = await screen.findByRole('alert');
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent(DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS);
    });

    it('displays LicenseLockScreen if auto-waiver feature is missing', async () => {
      axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['developer-dashboard']);

      renderComponent();

      const alert = await screen.findByRole('alert');
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent(DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS);
    });
  });

  it('renders the content when the feature is enabled for the license', async () => {
    renderComponent();

    expect(await screen.findByTestId('auto-waivers-configuration')).toBeInTheDocument();
    expect(screen.queryByText(DEVELOPER_FEATURE_DISABLED_MESSAGE_WAIVERS)).not.toBeInTheDocument();
  });

  it('renders the page title and description', async () => {
    renderComponent();

    expect(await screen.findByRole('heading', { name: 'Automated Waivers' })).toBeInTheDocument();
    expect(
      await screen.findByText(
        'Limit disruptions by deprioritizing low-threat violations until a remediation path is available.'
      )
    ).toBeInTheDocument();
  });

  it('renders the configured auto-waivers tile', async () => {
    renderComponent();

    expect(await screen.findByRole('heading', { name: 'Configured Auto-Waivers' })).toBeInTheDocument();
  });

  it('renders the new auto-waiver button', async () => {
    const user = userEvent.setup();
    renderComponent();

    const newAutoWaiverButton = await screen.findByRole('button', { name: 'New Auto-Waiver' });
    expect(newAutoWaiverButton).toBeInTheDocument();
    expect(newAutoWaiverButton).not.toHaveClass('disabled');

    await user.click(newAutoWaiverButton);
    const autoWaiverModal = await screen.findByTestId('iq-auto-waiver-modal');
    expect(autoWaiverModal).toBeInTheDocument();
    expect(screen.getByRole('dialog', { name: 'New Auto-Waiver' })).toBeInTheDocument();
  });

  it('renders a disabled new auto-waiver button when there are 3 local waivers', async () => {
    const user = userEvent.setup();
    const mockResponse = mockResponse_Application_All_Local;
    axiosMock.onGet(getApplicableAutoWaiversURL('application', 'Application1')).reply(200, mockResponse);
    renderComponent();

    const newAutoWaiverButton = await screen.findByRole('button', { name: 'New Auto-Waiver' });
    expect(newAutoWaiverButton).toBeInTheDocument();
    expect(newAutoWaiverButton).toHaveClass('disabled');

    await user.hover(newAutoWaiverButton);
    const tooltip = await screen.findByText('Max. configurations reached');
    expect(tooltip).toBeInTheDocument();

    await user.click(newAutoWaiverButton);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('renders the table with the correct columns', async () => {
    renderComponent();

    const table = await screen.findByRole('table');
    //renders 6 columns

    const columnHeaders = within(table).getAllByRole('columnheader');
    expect(columnHeaders.length).toBe(6);
    expect(columnHeaders[0]).toHaveAccessibleName('Created');
    expect(columnHeaders[1]).toHaveAccessibleName('Owner');
    expect(columnHeaders[2]).toHaveAccessibleName('Max. Threat');
    expect(columnHeaders[3]).toHaveAccessibleName('Scope');
    expect(columnHeaders[4]).toHaveAccessibleName('Details');
    expect(columnHeaders[5]).toHaveAccessibleName('Delete');
  });

  describe('table rows render correct contents', () => {
    describe('when at the application level', () => {
      it('all auto-waivers are local', async () => {
        const mockResponse = mockResponse_Application_All_Local;
        axiosMock.onGet(getApplicableAutoWaiversURL('application', 'Application1')).reply(200, mockResponse);

        renderComponent();

        const table = await screen.findByRole('table');
        const rows = within(table).getAllByRole('row');
        expect(rows.length).toBe(4); // 3 auto-waivers + 1 header

        // table header row is [0] data rows are [1, 2, 3]
        rows.slice(1).forEach((row, id) => assertCorrectRows(row, mockResponse, id));
      });

      it('1 auto-waiver is local and 2 are inherited from Root Org ', async () => {
        const mockResponse = mockResponse_Application_Local_RootOrg;
        axiosMock.onGet(getApplicableAutoWaiversURL('application', 'Application1')).reply(200, mockResponse);

        renderComponent();

        const table = await screen.findByRole('table');
        const rows = within(table).getAllByRole('row');
        expect(rows.length).toBe(5); // 3 auto-waivers + 1 header + 1 inherited header

        expect(within(table).getByRole('row', { name: 'Inherited from Root Organization' })).toBeInTheDocument();

        // Table header row is [0],
        // local row is [1],
        // Root Org herited header is [2],
        // Roor Org inherited rows are [3, 4]
        assertCorrectRows(rows[1], mockResponse, 0);
        expect(rows[2]).toHaveTextContent('Inherited from Root Organization');
        assertCorrectRows(rows[3], mockResponse, 1);
        assertCorrectRows(rows[4], mockResponse, 2);
      });

      it('1 auto-waiver is local, 1 is inherited from Test Org and 1 is inherited from Root Org ', async () => {
        const mockResponse = mockResponse_Application_Local_Org_RootOrg;
        axiosMock.onGet(getApplicableAutoWaiversURL('application', 'Application1')).reply(200, mockResponse);

        renderComponent();

        const table = await screen.findByRole('table');
        const rows = within(table).getAllByRole('row');
        expect(rows.length).toBe(6); // 1 table header + 2 inherited headers + 3 auto-waivers

        expect(within(table).getByRole('row', { name: 'Inherited from Test Organization' })).toBeInTheDocument();
        expect(within(table).getByRole('row', { name: 'Inherited from Root Organization' })).toBeInTheDocument();

        // Table header row is [0], local row is [1],
        // Test Organization inherited header is [2],
        // Test Organization inherited row is [3],
        // Root Org inherited header is [4],
        // Root Org inherited row is [5]
        assertCorrectRows(rows[1], mockResponse, 0);
        expect(rows[2]).toHaveTextContent('Inherited from Test Organization');
        assertCorrectRows(rows[3], mockResponse, 1);
        expect(rows[4]).toHaveTextContent('Inherited from Root Organization');
        assertCorrectRows(rows[5], mockResponse, 2);
      });
    });

    describe('when at the org level', () => {
      it('all auto-waivers are local', async () => {
        const mockResponse = mockResponse_Organization_Local;
        axiosMock.onGet(getApplicableAutoWaiversURL('organization', 'Organization1')).reply(200, mockResponse);

        renderComponent({
          orgsAndPolicies: {
            root: {
              selectedOwner: {
                id: 'Organization1',
                name: 'Organization1',
              },
            },
          },
          router: {
            currentState: {
              name: 'management.edit.organization.auto-waivers-config',
            },
            currentParams: {
              organizationId: 'Organization1',
            },
          },
        });

        const table = await screen.findByRole('table');
        const rows = within(table).getAllByRole('row');
        expect(screen.getByRole('heading', { name: 'Configured Auto-Waivers' })).toBeInTheDocument();
        expect(rows.length).toBe(4); // 3 auto-waivers + 1 header

        // table header row is [0] data rows are [1, 2, 3]
        rows.slice(1).forEach((row, id) => assertCorrectRows(row, mockResponse, id));
      });

      it('1 auto-waiver is local and 2 are inherited from Root Org ', async () => {
        const mockResponse = mockResponse_Organization_Local_RootOrg;
        axiosMock.onGet(getApplicableAutoWaiversURL('organization', 'Organization1')).reply(200, mockResponse);

        renderComponent({
          orgsAndPolicies: {
            root: {
              selectedOwner: {
                id: 'Organization1',
                name: 'Organization1',
              },
            },
          },
          router: {
            currentState: {
              name: 'management.edit.organization.auto-waivers-config',
            },
            currentParams: {
              organizationId: 'Organization1',
            },
          },
        });

        const table = await screen.findByRole('table');
        const rows = within(table).getAllByRole('row');
        expect(rows.length).toBe(5); // 1 table header + 1 inherited header + 3 auto-waivers

        expect(within(table).getByRole('row', { name: 'Inherited from Root Organization' })).toBeInTheDocument();

        // Table header row is [0],
        // Local rows are [1,2]
        // Root Org  inherited header is [3],
        // Root Org inherited row is [4],

        assertCorrectRows(rows[1], mockResponse, 0);
        assertCorrectRows(rows[2], mockResponse, 2);
        expect(rows[3]).toHaveTextContent('Inherited from Root Organization');
        assertCorrectRows(rows[4], mockResponse, 1);
      });
    });

    describe('when at the root org level', () => {
      it('all auto-waivers are local', async () => {
        const mockResponse = mockResponse_RootOrg_Local;
        axiosMock.onGet(getApplicableAutoWaiversURL('organization', 'ROOT_ORGANIZATION')).reply(200, mockResponse);

        renderComponent({
          orgsAndPolicies: {
            root: {
              selectedOwner: {
                id: 'ROOT_ORGANIZATION',
                name: 'Root Organization',
              },
            },
          },
          router: {
            currentState: {
              name: 'management.edit.organization.auto-waivers-config',
            },
            currentParams: {
              organizationId: 'ROOT_ORGANIZATION',
            },
          },
        });

        const table = await screen.findByRole('table');
        const rows = within(table).getAllByRole('row');
        expect(rows.length).toBe(4); // 3 auto-waivers + 1 header

        // table header row is [0] data rows are [1, 2, 3]
        rows.slice(1).forEach((row, id) => assertCorrectRows(row, mockResponse, id));
      });
    });
  });

  it('opens the delete confirmation modal when the delete button is clicked', async () => {
    const mockResponse = mockResponse_Application_All_Local;
    axiosMock.onGet(getApplicableAutoWaiversURL('application', 'Application1')).reply(200, mockResponse);

    renderComponent();

    const table = await screen.findByRole('table');
    const rows = within(table).getAllByRole('row');
    expect(rows.length).toBe(4); // 3 auto-waivers + 1 header

    const deleteButton = within(rows[1]).getByRole('button');
    expect(deleteButton).toBeInTheDocument();
    deleteButton.click();

    const deleteConfirmationModal = await screen.findByTestId('iq-delete-auto-waiver-modal');
    expect(deleteConfirmationModal).toBeInTheDocument();
  });

  describe('Pro Tier Gating', () => {
    const proState = {
      ...defaultPreloadedState,
      productLicense: { license: { products: ['Sonatype Lifecycle Pro'] } },
    };

    beforeEach(() => {
      axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['developer-dashboard', 'auto-waivers']);
    });

    it('shows Enterprise Feature tag when auto-waiver-management feature is absent', async () => {
      renderComponent(proState);
      expect(await screen.findByText('Enterprise Feature')).toBeVisible();
    });

    it('shows enterprise banner when auto-waiver-management feature is absent', async () => {
      renderComponent(proState);
      expect(await screen.findByText(/Automatically apply waivers/)).toBeVisible();
    });
  });
});

function assertCorrectRows(row, mockResponse, id) {
  const {
    createTime: expectedCreateTime,
    autoPolicyWaiverOwnerName: expectedOwnerName,
    threatLevel: expectedThreatLevel,
    hasNoPathForward,
    hasNotReachable,
    isInherited,
  } = mockResponse[id];

  const createdDate = within(row).getAllByRole('cell')[0];
  expect(createdDate).toBeInTheDocument();
  expect(createdDate).toHaveTextContent(formatDate(expectedCreateTime));

  const owner = within(row).getAllByRole('cell')[1];
  expect(owner).toBeInTheDocument();
  expect(owner).toHaveTextContent(expectedOwnerName);

  const maxThreat = within(row).getAllByRole('cell')[2];
  expect(maxThreat).toBeInTheDocument();
  expect(maxThreat).toHaveTextContent(expectedThreatLevel);

  const scope = within(row).getAllByRole('cell')[3];
  expect(scope).toBeInTheDocument();
  if (hasNoPathForward && hasNotReachable) {
    expect(scope).toHaveTextContent('No Path Forward; Not Reachable');
  } else if (hasNoPathForward) {
    expect(scope).toHaveTextContent('No Path Forward');
  } else if (hasNotReachable) {
    expect(scope).toHaveTextContent('Not Reachable');
  }

  const details = within(row).getAllByRole('cell')[4];
  expect(details).toBeInTheDocument();
  if (isInherited) {
    expect(details).toHaveTextContent('View');
  } else {
    expect(details).toHaveTextContent('View/Edit');
  }

  const deleteButton = within(row).getByRole('button');
  expect(deleteButton).toBeInTheDocument();
  if (isInherited) {
    expect(deleteButton).toHaveClass('disabled');
  } else {
    expect(deleteButton).not.toHaveClass('disabled');
  }
}
