/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, within, fireEvent } from '@testing-library/react';

import { render } from 'TestRoot/SpecUtil';
import FirewallContainerWaiverTable from 'MainRoot/firewall/FirewallContainerWaiverTable';
import * as RouterActions from 'MainRoot/reduxUiRouter/routerActions';

describe('FirewallContainerWaiverTable', () => {
  const props = {
    loadContainerWaiverList: jest.fn(),
    setContainerWaiverGridPage: jest.fn(),
    loadContainerWaiverGridError: null,
    loadingContainerWaiverList: false,
    containerWaiverList: [],
    containerWaiverPageCount: 0,
    containerWaiverCurrentPage: null,
    containerWaiverLastUpdated: null,
  };

  it('renders the initial component correctly when no data found', () => {
    render(<FirewallContainerWaiverTable {...props} />);

    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Containers Waived');

    const table = screen.getByRole('table');
    const rowGroups = within(table).getAllByRole('rowgroup');
    const headerRows = within(rowGroups[0]).getAllByRole('row');
    const headerColumnHeaders = within(headerRows[0]).getAllByRole('columnheader');
    const bodyRows = within(rowGroups[1]).getAllByRole('row');
    const bodyCells = within(bodyRows[0]).getAllByRole('cell');

    expect(headerColumnHeaders).toHaveLength(7);
    expect(headerColumnHeaders[0]).toHaveTextContent('Threat');
    expect(headerColumnHeaders[1]).toHaveTextContent('Date Created');
    expect(headerColumnHeaders[2]).toHaveTextContent('Expirations');
    expect(headerColumnHeaders[3]).toHaveTextContent('Policy');
    expect(headerColumnHeaders[4]).toHaveTextContent('Scope');
    expect(headerColumnHeaders[5]).toHaveTextContent('Components');
    expect(headerColumnHeaders[6]).toHaveTextContent('Select Row');
    expect(bodyCells).toHaveLength(1);
    expect(bodyCells[0]).toHaveTextContent('No data found');
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeInTheDocument();
  });

  it('renders table with data', () => {
    const mockData = {
      containerWaiverList: [
        {
          policyWaiverId: 'b57049bf6e41424ebfa2002e06f955e5',
          ownerId: 'f63ae7c6a97745cba6f1a99975e47dd1',
          createTime: 1750368468308,
          expiryTime: 1782856750977,
          maxThreatLevel: 6,
          applicationScope: 'localhost_8070-docker-proxy-library-alpine-3.61',
          uniquePolicyCount: 3,
          uniqueComponentCount: 9,
        },
      ],
      containerWaiverPageCount: 1,
      containerWaiverCurrentPage: 0,
    };

    render(<FirewallContainerWaiverTable {...{ ...props, ...mockData }} />);

    const table = screen.getByRole('table');
    const rowGroups = within(table).getAllByRole('rowgroup');
    const bodyRows = within(rowGroups[1]).getAllByRole('row');
    const bodyCells = within(bodyRows[0]).getAllByRole('cell');

    expect(bodyRows).toHaveLength(1);
    expect(bodyCells[0]).toHaveTextContent('6');
    expect(bodyCells[1]).toHaveTextContent('2025-06-19');
    expect(bodyCells[2]).toHaveTextContent('2026-06-30');
    expect(bodyCells[3]).toHaveTextContent('Multiple-Policy-Types(3)');
    expect(bodyCells[4]).toHaveTextContent('localhost_8070-docker-proxy-library-alpine-3.61');
    expect(bodyCells[5]).toHaveTextContent('Multiple Components(9)');
  });

  it('renders error alert when loadContainerWaiverGridError is provided', () => {
    const loadContainerWaiverGridError = 'Failed to load container waivers';
    render(
      <FirewallContainerWaiverTable {...{ ...props, loadContainerWaiverGridError: loadContainerWaiverGridError }} />
    );

    const table = screen.getByRole('table');
    const rowGroups = within(table).getAllByRole('rowgroup');
    const bodyRows = within(rowGroups[1]).getAllByRole('row');
    const bodyCells = within(bodyRows[0]).getAllByRole('cell');
    const errorAlert = within(bodyCells[0]).getByRole('alert');

    expect(bodyRows).toHaveLength(1);
    expect(errorAlert).toHaveTextContent('An error occurred loading data. Failed to load container waivers');
  });

  it('renders last update timestamp when containerWaiverLastUpdated is provided', () => {
    const lastUpdated = new Date('2025-06-19T12:00:00Z');
    render(<FirewallContainerWaiverTable {...{ ...props, containerWaiverLastUpdated: lastUpdated }} />);

    expect(screen.getByText('Updated 8:00:00 AM 2025-06-19')).toBeInTheDocument();
  });

  it('calls loadContainerWaiverList when Refresh button is clicked', () => {
    render(<FirewallContainerWaiverTable {...props} />);

    const refreshButton = screen.getByRole('button', { name: 'Refresh' });
    fireEvent.click(refreshButton);

    expect(props.loadContainerWaiverList).toHaveBeenCalled();
  });

  it('renders pagination controls and calls setContainerQuarantineGridPage when page is clicked', () => {
    const mockData = {
      containerWaiverList: [
        {
          policyWaiverId: 'b57049bf6e41424ebfa2002e06f955e5',
          createTime: 1750368468308,
          expiryTime: 1782856750977,
          maxThreatLevel: 6,
          applicationScope: 'localhost_8070-docker-proxy-library-alpine-3.61',
          uniquePolicyCount: 3,
          uniqueComponentCount: 9,
        },
        {
          policyWaiverId: 'f8f549fc0e114826aac68ef6d889ca91',
          createTime: 1750286688042,
          expiryTime: 1782856750977,
          maxThreatLevel: 5,
          applicationScope: 'localhost_8070-docker-proxy-library-alpine-3.60',
          uniquePolicyCount: 2,
          uniqueComponentCount: 9,
        },
      ],
      containerWaiverPageCount: 2,
      containerWaiverCurrentPage: 1,
    };

    render(<FirewallContainerWaiverTable {...{ ...props, ...mockData }} />);

    const pagination = screen.getByRole('navigation');
    const pages = within(pagination).getAllByRole('button');

    expect(pages).toHaveLength(3);
    expect(pages[0]).toHaveAttribute('aria-label', 'goto previous page');
    expect(pages[1]).toHaveTextContent('1');
    expect(pages[2]).toHaveTextContent('2');

    fireEvent.click(pages[1]);
    expect(props.setContainerWaiverGridPage).toHaveBeenCalledWith(0, expect.anything());
  });

  it('calls stateGo when table row is clicked', async () => {
    const stateGoSpy = jest.spyOn(RouterActions, 'stateGo');
    const mockData = {
      containerWaiverList: [
        {
          policyWaiverId: 'b57049bf6e41424ebfa2002e06f955e5',
          ownerId: 'f63ae7c6a97745cba6f1a99975e47dd1',
          createTime: 1750368468308,
          expiryTime: 1782856750977,
          maxThreatLevel: 6,
          applicationScope: 'localhost_8070-docker-proxy-library-alpine-3.61',
          uniquePolicyCount: 3,
          uniqueComponentCount: 9,
        },
      ],
      containerWaiverPageCount: 1,
      containerWaiverCurrentPage: 0,
    };

    render(<FirewallContainerWaiverTable {...{ ...props, ...mockData }} stateGo={stateGoSpy} />);

    const table = screen.getByRole('table');
    const rowGroups = within(table).getAllByRole('rowgroup');
    const bodyRows = within(rowGroups[1]).getAllByRole('row');

    fireEvent.click(bodyRows[0]);
    expect(stateGoSpy).toHaveBeenCalledWith('firewall.waiver.details', {
      waiverId: 'b57049bf6e41424ebfa2002e06f955e5',
      ownerId: 'f63ae7c6a97745cba6f1a99975e47dd1',
      ownerType: 'application',
      type: 'waiver',
      sidebarReference: 'filter',
      page: 1,
    });
  });
});
