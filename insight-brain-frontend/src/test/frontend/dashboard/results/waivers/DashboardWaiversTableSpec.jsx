/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import DashboardWaiversTable from 'MainRoot/dashboard/results/waivers/DashboardWaiversTable';
import * as DashboardSelectors from 'MainRoot/dashboard/dashboardSelectors';

describe('DashboardWaiversTable', function () {
  let renderComponent, dashboardWaiversProps, sortWaiversSpy;

  beforeEach(function () {
    sortWaiversSpy = jasmine.createSpy('sortWaivers');
    dashboardWaiversProps = {
      waivers: {
        error: null,
        numResults: 5,
        sortFields: ['expiryTime'],
        results: [
          {
            id: 'a815dd98fdfc448fb69c800bb6d13cc9',
            threatLevel: 7,
            createTime: 1661485610116,
            expiryTime: 1664081999999,
            policyId: '7e2f3dc0202c4f06ae9d288dba0fcf97',
            policyName: 'Security-Medium',
            ownerId: '642a106467c74f6eb5f90eade8ceb5f9',
            ownerName: 'root-org',
            ownerType: 'organization',
            scope: 'Organization - root-org',
            componentMatchStrategy: 'EXACT_COMPONENT',
            hash: '6a1d836b6a4c77ec11ac',
          },
          {
            id: '9ba41779ad63456788bbdb223ae5322a',
            threatLevel: 10,
            createTime: 1661485661600,
            expiryTime: 1671857999999,
            policyId: '766012e0acf8464dbd7973ec928e2210',
            policyName: 'Security-Critical',
            ownerId: '0305d75f92c04c459b7d24c8bc406f7e',
            ownerName: 'app1',
            ownerType: 'application',
            scope: 'Applicationapp - app1',
            componentMatchStrategy: 'ALL_COMPONENTS',
            hash: null,
          },
          {
            id: 'b54ff0c9e37f475ab3028a3312f48634',
            threatLevel: 10,
            createTime: 1661527948432,
            expiryTime: null,
            policyId: '766012e0acf8464dbd7973ec928e2210',
            policyName: 'Security-Critical',
            ownerId: '0305d75f92c04c459b7d24c8bc406f7e',
            ownerName: 'app1',
            ownerType: 'application',
            scope: 'Applicationapp - app1',
            componentMatchStrategy: 'EXACT_COMPONENT',
            hash: '20554954120b3cc9f088',
          },
          {
            id: 'd1c20763e4394786afddd154e17e2c6b',
            threatLevel: 5,
            createTime: 1661531418904,
            expiryTime: null,
            policyId: 'ce6ca7e95261441586a0e3f1f934dd37',
            policyName: 'Figue-policy',
            ownerId: '642a106467c74f6eb5f90eade8ceb5f9',
            ownerName: 'root-org',
            ownerType: 'organization',
            scope: 'Organization - root-org',
            componentMatchStrategy: 'EXACT_COMPONENT',
            hash: '51031e9c43ae47693c99',
          },
          {
            id: '4ac5e46025c941e68a335b61eb3165d2',
            threatLevel: 5,
            createTime: 1661532973306,
            expiryTime: null,
            policyId: 'ce6ca7e95261441586a0e3f1f934dd37',
            policyName: 'Figue-policy',
            ownerId: '642a106467c74f6eb5f90eade8ceb5f9',
            ownerName: 'root-org',
            ownerType: 'organization',
            scope: 'Organization - root-org',
            componentMatchStrategy: 'ALL_COMPONENTS',
            hash: null,
          },
        ],
      },
      sortWaivers: sortWaiversSpy,
      stateGo: () => {},
      maxDaysOld: 0,
      needsAcknowledgement: false,
      reload: () => {},
    };
    spyOn(DashboardSelectors, 'selectDashboardFilter').and.returnValue({ needsAcknowledgement: false });
    renderComponent = (additionalProps = {}) => render(<DashboardWaiversTable {...additionalProps} />);
  });

  it('renders NxTable headers and entries', async () => {
    renderComponent(dashboardWaiversProps);

    const [tableHeaders, tableEntries] = await screen.findAllByRole('rowgroup');
    expect(tableHeaders).toBeVisible();
    expect(screen.getByText('Threat')).toBeVisible();
    expect(screen.getByText('Date Created')).toBeVisible();
    expect(screen.getByText('Policy')).toBeVisible();
    expect(screen.getByText('Scope')).toBeVisible();
    expect(screen.getByText('Components')).toBeVisible();
    expect(tableEntries).toBeVisible();
  });

  it('renders a row with an alert message when the filter needs acknowledgement', () => {
    dashboardWaiversProps.needsAcknowledgement = true;

    renderComponent(dashboardWaiversProps);

    expect(screen.getByText("Select your filter criteria and click 'apply' to see results.")).toBeVisible();
  });

  it('renders an empty message on the NxTableBody if there are no components to display', () => {
    dashboardWaiversProps.waivers.results = [];
    dashboardWaiversProps.waivers.numResults = 0;
    renderComponent(dashboardWaiversProps);

    expect(screen.getByText('No data available given the applied filters and permissions.')).toBeVisible();
  });

  it('renders an empty message on the NxTableBody if there are no components to display in the last 20 days', () => {
    dashboardWaiversProps.waivers.results = [];
    dashboardWaiversProps.waivers.numResults = 0;
    dashboardWaiversProps.maxDaysOld = 20;
    renderComponent(dashboardWaiversProps);

    expect(
      screen.getByText('No data available in the last 20 days given the applied filters and permissions.')
    ).toBeVisible();
  });

  it('renders max results info when numResult is higher than 100', () => {
    dashboardWaiversProps.waivers.numResults = 101;
    dashboardWaiversProps.waivers.results.length = 101;
    renderComponent(dashboardWaiversProps);

    expect(screen.getByText(`First 100 results shown`)).toBeVisible();
  });

  describe('Cell sorting', () => {
    const columns = [
      {
        columnName: 'Threat',
        roleName: /Threat/,
        filter: ['threatLevel', '-threatLevel'],
        doBackendCall: false,
      },
      {
        columnName: 'Date Created',
        roleName: /Date Created/,
        filter: ['createTime', '-createTime'],
        doBackendCall: false,
      },
      {
        columnName: 'Expiration',
        roleName: /Expiration/,
        filter: ['expiryTime', '-expiryTime'],
        doBackendCall: false,
      },
      {
        columnName: 'Policy',
        roleName: /Policy/,
        filter: ['policyName', '-policyName'],
        doBackendCall: false,
      },
      {
        columnName: 'Scope',
        roleName: /Scope/,
        filter: ['scope', '-scope'],
        doBackendCall: false,
      },
      {
        columnName: 'Components',
        roleName: 'Components',
        filter: ['component', '-component'],
        doBackendCall: true,
      },
    ];
    columns.forEach((column) => {
      column.filter.forEach((filter) => {
        const ascOrDesc = filter.includes('-') ? 'desc to asc' : 'asc to desc';
        const expectedFilterAfterClick = filter.includes('-') ? filter.substring(1) : `-${filter}`;
        it(`calls the sortWaivers function with the ${column.columnName} column fields if clicked: ${ascOrDesc}`, () => {
          dashboardWaiversProps.waivers.sortFields = [filter];
          renderComponent(dashboardWaiversProps);

          const headerCellButton = screen.getByRole('columnheader', { name: column.roleName }).children[0];
          expect(headerCellButton).toHaveClass('nx-cell__sort-btn');
          expect(headerCellButton).toHaveAttribute('title');

          fireEvent.click(headerCellButton);

          expect(sortWaiversSpy).toHaveBeenCalledWith([expectedFilterAfterClick], column.doBackendCall);
        });
      });
    });
  });
});
