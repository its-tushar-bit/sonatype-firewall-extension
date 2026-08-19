/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, waitFor } from 'TestRoot/SpecUtil';
import ContainerImageWaiversTable from 'MainRoot/firewall/waiverRequests/ContainerImageWaiversTable';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';
import { getContainerImageAllRepositoriesWaiversUrl } from 'MainRoot/util/CLMLocation';

const mockWaiver1 = {
  policyWaiverId: 'b57049bf6e41424ebfa2002e06f955e5',
  scopeOwnerId: 'f63ae7c6a97745cba6f1a99975e47dd1',
  scopeOwnerType: 'application',
  scopeOwnerName: 'my-docker-app',
  createTime: '2025-06-19T10:00:00.000Z',
  expiryTime: '2026-06-30T10:00:00.000Z',
  threatLevel: 7,
  policyName: 'Docker Security Policy',
  matcherStrategy: 'ALL_COMPONENTS',
  componentDisplayName: null,
  forContainerImage: true,
};

const mockWaiver2 = {
  policyWaiverId: 'a1b2c3d4e5f64a7b8c9d0e1f2a3b4c5d',
  scopeOwnerId: '0caa731cc7b149e7bc24fe9602e3a7dd',
  scopeOwnerType: 'application',
  scopeOwnerName: 'another-container-app',
  createTime: '2025-05-10T08:00:00.000Z',
  expiryTime: null,
  threatLevel: 5,
  policyName: 'Container Policy',
  matcherStrategy: 'EXACT_COMPONENT',
  componentDisplayName: 'alpine:3.6',
  forContainerImage: false,
};

const baseRouterState = {
  router: {
    currentParams: {},
    currentState: { name: 'firewall.waivers.containers' },
  },
};

describe('ContainerImageWaiversTable', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, []);
  });

  const renderComponent = (extraState = {}) =>
    render(<ContainerImageWaiversTable />, {
      preloadedState: { ...baseRouterState, ...extraState },
    });

  it('renders the table headers correctly', async () => {
    renderComponent();

    await waitFor(() => {
      const table = screen.getByRole('table');
      const headerCells = within(table).getAllByRole('columnheader');
      expect(headerCells[0]).toHaveTextContent('Threat');
      expect(headerCells[1]).toHaveTextContent('Date Created');
      expect(headerCells[2]).toHaveTextContent('Expiration');
      expect(headerCells[3]).toHaveTextContent('Policy');
      expect(headerCells[4]).toHaveTextContent('Scope');
      expect(headerCells[5]).toHaveTextContent('Components');
    });
  });

  it('renders empty message when there are no waivers', async () => {
    renderComponent();

    expect(await screen.findByText('No container image waivers found.')).toBeInTheDocument();
  });

  it('renders error state with retry handler when API fails', async () => {
    axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(500, { message: 'Server Error' });
    renderComponent();

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Retry' })).toBeInTheDocument();
  });

  it('renders waiver rows with correct data after fetch', async () => {
    axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, [mockWaiver1, mockWaiver2]);
    renderComponent();

    const table = await screen.findByRole('table');
    const rowGroups = within(table).getAllByRole('rowgroup');
    const bodyRows = within(rowGroups[1]).getAllByRole('row');

    expect(bodyRows).toHaveLength(2);

    const firstRowCells = within(bodyRows[0]).getAllByRole('cell');
    expect(firstRowCells[0]).toHaveTextContent('7');
    expect(firstRowCells[1]).toHaveTextContent('2025-06-19');
    expect(firstRowCells[2]).toHaveTextContent('2026-06-30');
    expect(firstRowCells[3]).toHaveTextContent('Docker Security Policy');
    expect(firstRowCells[4]).toHaveTextContent('my-docker-app');
    expect(firstRowCells[5]).toHaveTextContent('All Components');
  });

  it('renders "Never" for expiration when expiryTime is null', async () => {
    axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, [mockWaiver2]);
    renderComponent();

    const table = await screen.findByRole('table');
    const rowGroups = within(table).getAllByRole('rowgroup');
    const bodyRows = within(rowGroups[1]).getAllByRole('row');
    const cells = within(bodyRows[0]).getAllByRole('cell');

    expect(cells[2]).toHaveTextContent('Never');
  });

  it('renders component display name for non-ALL_COMPONENTS matcher strategy', async () => {
    axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, [mockWaiver2]);
    renderComponent();

    const table = await screen.findByRole('table');
    const rowGroups = within(table).getAllByRole('rowgroup');
    const bodyRows = within(rowGroups[1]).getAllByRole('row');
    const cells = within(bodyRows[0]).getAllByRole('cell');

    expect(cells[5]).toHaveTextContent('alpine:3.6');
  });

  it('renders scopeOwnerId as fallback when scopeOwnerName is absent', async () => {
    const waiverWithoutOwnerName = { ...mockWaiver1, scopeOwnerName: null };
    axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, [waiverWithoutOwnerName]);
    renderComponent();

    expect(await screen.findByText('f63ae7c6a97745cba6f1a99975e47dd1')).toBeInTheDocument();
  });

  it('navigates to waiver details with correct params when a row is clicked', async () => {
    const stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
    const user = userEvent.setup();

    axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, [mockWaiver1]);
    renderComponent();

    const table = await screen.findByRole('table');
    const rowGroups = within(table).getAllByRole('rowgroup');
    const bodyRows = within(rowGroups[1]).getAllByRole('row');

    await user.click(bodyRows[0]);

    expect(stateGoSpy).toHaveBeenCalledWith('firewall.waiver.details', {
      waiverId: mockWaiver1.policyWaiverId,
      ownerId: mockWaiver1.scopeOwnerId,
      ownerType: mockWaiver1.scopeOwnerType,
      type: 'waiver',
      sidebarReference: 'filter',
    });
  });

  it('falls back to "application" ownerType when scopeOwnerType is absent', async () => {
    const stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
    const user = userEvent.setup();

    const waiverWithoutOwnerType = { ...mockWaiver1, scopeOwnerType: null };
    axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, [waiverWithoutOwnerType]);
    renderComponent();

    const table = await screen.findByRole('table');
    const rowGroups = within(table).getAllByRole('rowgroup');
    const bodyRows = within(rowGroups[1]).getAllByRole('row');

    await user.click(bodyRows[0]);

    expect(stateGoSpy).toHaveBeenCalledWith('firewall.waiver.details', expect.objectContaining({
      ownerType: 'application',
    }));
  });

  it('calls the waivers API on mount', async () => {
    renderComponent();

    await waitFor(() => {
      expect(axiosMock.history.get.some((req) => req.url === getContainerImageAllRepositoriesWaiversUrl())).toBe(true);
    });
  });

  it('shows retry button after an API error and re-fetches on click', async () => {
    const user = userEvent.setup();
    axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(500, { message: 'Error' });
    renderComponent();

    const retryButton = await screen.findByRole('button', { name: 'Retry' });
    expect(retryButton).toBeInTheDocument();

    axiosMock.onGet(getContainerImageAllRepositoriesWaiversUrl()).reply(200, []);
    await user.click(retryButton);

    await waitFor(() => {
      const getRequests = axiosMock.history.get.filter(
        (req) => req.url === getContainerImageAllRepositoriesWaiversUrl()
      );
      expect(getRequests.length).toBeGreaterThanOrEqual(2);
    });
  });
});
