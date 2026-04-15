/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, within, fireEvent } from '@testing-library/react';

import { render } from 'TestRoot/SpecUtil';
import FirewallContainerQuarantineTable from 'MainRoot/firewall/FirewallContainerQuarantineTable';
import { formatDate, FIREWALL_DATE_TIME_FORMAT, FIREWALL_TIME_DATE_FORMAT } from 'MainRoot/util/dateUtils';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';
import {
  FIREWALL_CONTAINER_REPOSITORY_RESULTS,
  FIREWALL_FIREWALLPAGE_CONTAINERS,
} from 'MainRoot/constants/states/firewall';

describe('FirewallContainerQuarantineTable', () => {
  const props = {
    loadContainerQuarantineList: jest.fn(),
    setContainerQuarantineGridPage: jest.fn(),
    loadedContainerQuarantineList: true,
    loadContainerQuarantineGridError: null,
    containerQuarantinePageCount: 0,
    containerQuarantineList: [],
    containerCurrentPage: null,
    containerLastUpdated: null,
  };

  it('renders the FirewallContainerQuarantineTable component correctly when no data found', () => {
    render(<FirewallContainerQuarantineTable {...props} />);

    expect(screen.getByRole('heading', { level: 2 })).toHaveTextContent('Containers Actively in Quarantine');

    const table = screen.getByRole('table');
    const rowGroups = within(table).getAllByRole('rowgroup');
    const headerRows = within(rowGroups[0]).getAllByRole('row');
    const headerColumnHeaders = within(headerRows[0]).getAllByRole('columnheader');
    const bodyRows = within(rowGroups[1]).getAllByRole('row');
    const bodyCells = within(bodyRows[0]).getAllByRole('cell');

    expect(headerColumnHeaders).toHaveLength(5);
    expect(headerColumnHeaders[0]).toHaveTextContent('Threat');
    expect(headerColumnHeaders[1]).toHaveTextContent('Policy');
    expect(headerColumnHeaders[2]).toHaveTextContent('Quarantine Time');
    expect(headerColumnHeaders[3]).toHaveTextContent('Container');
    expect(headerColumnHeaders[4]).toHaveTextContent('Repository');
    expect(bodyCells).toHaveLength(1);
    expect(bodyCells[0]).toHaveTextContent('No data found.');
    expect(screen.getByRole('button', { name: 'Refresh' })).toBeInTheDocument();
  });

  it('renders table with data', () => {
    const hrefMock = jest.fn().mockReturnValue('testHref');
    jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue({
      href: hrefMock,
    });
    const mockData = {
      containerQuarantineList: [
        {
          threatLevel: 9,
          openTime: 1750872768184,
          applicationPublicId: 'localhost_8070-docker-proxy-library-alpine-3.6',
          applicationId: '266e42f89ea6478d8bc60fca362391e2',
          applicationName: 'localhost_8070-docker-proxy-library-alpine-3.6',
          repositoryPublicId: 'docker-proxy',
          repositoryId: '42b8e3703eb94a89ac920eaeba0b612e',
          policyViolationCount: 10,
          scanId: '7a0a2d89dffa4277ba4fd8ce6c550f38',
        },
      ],
      containerQuarantinePageCount: 1,
      containerCurrentPage: 0,
    };

    render(<FirewallContainerQuarantineTable {...{ ...props, ...mockData }} />);

    const table = screen.getByRole('table');
    const rowGroups = within(table).getAllByRole('rowgroup');
    const bodyRows = within(rowGroups[1]).getAllByRole('row');
    const bodyCells = within(bodyRows[0]).getAllByRole('cell');

    expect(bodyRows).toHaveLength(1);
    expect(bodyCells[0]).toHaveTextContent('9');
    expect(bodyCells[1]).toHaveTextContent('Multiple-Policy-Types(10)');
    expect(bodyCells[2]).toHaveTextContent(formatDate(1750872768184, FIREWALL_DATE_TIME_FORMAT));

    const containerReportLink = within(bodyCells[3]).getByRole('link', {
      name: 'localhost_8070-docker-proxy-library-alpine-3.6',
    });
    expect(containerReportLink).toHaveAttribute('href', 'testHref');
    expect(hrefMock).toHaveBeenCalledWith('firewall.containerReport', {
      origin: FIREWALL_FIREWALLPAGE_CONTAINERS,
      publicId: 'localhost_8070-docker-proxy-library-alpine-3.6',
      scanId: '7a0a2d89dffa4277ba4fd8ce6c550f38',
    });

    const reportLink = within(bodyCells[4]).getByRole('link', { name: 'docker-proxy' });
    expect(reportLink).toHaveAttribute('href', 'testHref');
    expect(hrefMock).toHaveBeenCalledWith(FIREWALL_CONTAINER_REPOSITORY_RESULTS, {
      repositoryId: '42b8e3703eb94a89ac920eaeba0b612e',
    });
  });

  it('renders error alert when loadContainerQuarantineGridError is provided', () => {
    const loadContainerQuarantineGridError = 'Failed to load containers in quarantine';
    render(<FirewallContainerQuarantineTable {...{ ...props, loadContainerQuarantineGridError }} />);

    const table = screen.getByRole('table');
    const rowGroups = within(table).getAllByRole('rowgroup');
    const bodyRows = within(rowGroups[1]).getAllByRole('row');
    const bodyCells = within(bodyRows[0]).getAllByRole('cell');
    const errorAlert = within(bodyCells[0]).getByRole('alert');

    expect(bodyRows).toHaveLength(1);
    expect(errorAlert).toHaveTextContent('An error occurred loading data. Failed to load containers in quarantine');
  });

  it('renders last update timestamp when containerLastUpdated is provided', () => {
    const lastUpdated = new Date('2025-06-19T12:00:00Z');
    render(<FirewallContainerQuarantineTable {...{ ...props, containerLastUpdated: lastUpdated }} />);

    const formattedDate = 'Updated ' + formatDate(lastUpdated, FIREWALL_TIME_DATE_FORMAT);
    expect(screen.getByText(formattedDate)).toBeInTheDocument();
  });

  it('calls loadContainerQuarantineList on refresh button click', () => {
    render(<FirewallContainerQuarantineTable {...props} />);

    const refreshButton = screen.getByRole('button', { name: 'Refresh' });
    fireEvent.click(refreshButton);

    expect(props.loadContainerQuarantineList).toHaveBeenCalled();
  });

  it('renders pagination controls and calls setContainerQuarantineGridPage when page is clicked', () => {
    const mockData = {
      containerQuarantineList: [
        {
          threatLevel: 9,
          openTime: 1750872768184,
          applicationPublicId: 'localhost_8070-docker-proxy-library-alpine-3.6',
          applicationId: '266e42f89ea6478d8bc60fca362391e2',
          applicationName: 'localhost_8070-docker-proxy-library-alpine-3.6',
          repositoryPublicId: 'docker-proxy',
          repositoryId: '42b8e3703eb94a89ac920eaeba0b612e',
          policyViolationCount: 10,
          scanId: '7a0a2d89dffa4277ba4fd8ce6c550f38',
        },
        {
          threatLevel: 5,
          openTime: 1750274522199,
          applicationPublicId: 'localhost_8070-docker-proxy-library-alpine-3.60',
          applicationId: '84c0bdbd19eb41388326d732b7247830',
          applicationName: 'localhost_8070-docker-proxy-library-alpine-3.60',
          repositoryPublicId: 'docker-proxy',
          repositoryId: '42b8e3703eb94a89ac920eaeba0b612e',
          policyViolationCount: 10,
          scanId: 'dafb500efe4049ca966e98abddcd6aa6',
        },
      ],
      containerQuarantinePageCount: 2,
      containerCurrentPage: 1,
    };

    render(<FirewallContainerQuarantineTable {...{ ...props, ...mockData }} />);

    const pagination = screen.getByRole('navigation');
    const pages = within(pagination).getAllByRole('button');

    expect(pages).toHaveLength(3);
    expect(pages[0]).toHaveAttribute('aria-label', 'goto previous page');
    expect(pages[1]).toHaveTextContent('1');
    expect(pages[2]).toHaveTextContent('2');

    fireEvent.click(pages[1]);
    expect(props.setContainerQuarantineGridPage).toHaveBeenCalledWith(0, expect.anything());
  });
});
