/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter, fireEvent, within } from 'TestRoot/SpecUtil';
import { getAppsWithoutCiIntegrations } from 'MainRoot/util/CLMLocation';
import AppsWithoutCiIntegrations from 'MainRoot/integrations/sections/AppsWithoutCiIntegrations/AppsWithoutCiIntegrations';
import { map, range } from 'ramda';
import { NX_STANDARD_DEBOUNCE_TIME } from '@sonatype/react-shared-components';

describe('AppsWithoutCiIntegrations Page', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it('should render a loading message when network call is pending', () => {
    render(<AppsWithoutCiIntegrations />);

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  describe('on a failed http call', () => {
    it('displays a proper error', async () => {
      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(404, 'Error');
      render(<AppsWithoutCiIntegrations />);
      const properErrorMsg = await screen.findByText(/an error occurred loading data\./i);
      expect(properErrorMsg).toBeInTheDocument();
    });
  });

  describe('on successful http calls retrieving an empty list', () => {
    beforeEach(() => {
      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(200, {
        dashboardResults: [],
        numResults: 0,
      });
      render(<AppsWithoutCiIntegrations />);
    });

    it('renders and only renders the desired column headers', () => {
      assertHeaders();
    });

    it('render "All of your apps are integrated with CI" message eventually', async () => {
      const msg = await screen.findByRole('cell', {
        name: /All of your apps are integrated with CI\./i,
      });
      expect(msg).toBeInTheDocument();
    });
  });

  describe('on successful http calls retrieving a non-empty list', () => {
    beforeEach(() => {
      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(200, {
        dashboardResults: createAppArrayWithLength(15),
        numResults: 20,
      });
      render(<AppsWithoutCiIntegrations />);
    });

    it('renders and only renders the desired column headers', () => {
      assertHeaders();
    });

    it('renders the first page button eventually', async () => {
      const button = await screen.findByRole('button', {
        name: /goto first page/i,
      });
      expect(button).toBeInTheDocument();
    });

    it('renders all 15 rows of data cells', async () => {
      const appNames = await screen.findAllByRole('cell', { name: /^App\d+$/i });
      const appRisks = await screen.findAllByRole('cell', { name: /^\d+$/i });
      expect(appNames.length).toBe(15);
      expect(appRisks.length).toBe(15);
    });
  });

  describe('pagination buttons', () => {
    it('make correct POST requests', async () => {
      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(200, {
        dashboardResults: createAppArrayWithLength(14),
        numResults: 50,
      });

      const givenTime = new Date();
      jest.setSystemTime(givenTime);

      render(<AppsWithoutCiIntegrations />);

      const paginationBtnBar = await screen.findByRole('navigation');
      const nextPageBtn = await within(paginationBtnBar).findByRole('button', { name: 'goto next page' });

      expect(axiosMock.history.post.length).toBe(1);

      fireEvent.click(nextPageBtn);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.post.length).toBe(2);
      expect(axiosMock.history.post[0].url).toBe(getAppsWithoutCiIntegrations());
      expect(axiosMock.history.post[1].url).toBe(getAppsWithoutCiIntegrations());

      // this gets added to the system clock by calling fireEvent and needs to be accounted for in terms of what gets
      // returned in the calculation for sinceUtcTimestamp
      const timeAddedToSystemByFireEvent = 250;
      const expectedTimestampForQuery = givenTime.setMonth(givenTime.getMonth() - 3) + timeAddedToSystemByFireEvent;

      expect(axiosMock.history.post[1].data).toBe(
        JSON.stringify({
          page: 1,
          pageSize: 14,
          sinceUtcTimestamp: expectedTimestampForQuery,
          optionalOrderBy: '-TOTAL_RISK',
          optionalFilterApplicationNamesBy: '',
        })
      );

      fireEvent.click(nextPageBtn);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.post.length).toBe(3);

      expect(axiosMock.history.post[2].data).toEqual(
        JSON.stringify({
          page: 2,
          pageSize: 14,
          sinceUtcTimestamp: expectedTimestampForQuery,
          optionalOrderBy: '-TOTAL_RISK',
          optionalFilterApplicationNamesBy: '',
        })
      );
    });

    it('change data on the table', async () => {
      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(200, {
        dashboardResults: createAppArrayWithLength(14),
        numResults: 50,
      });

      render(<AppsWithoutCiIntegrations />);

      const paginationBtnBar = await screen.findByRole('navigation');
      const nextPageBtn = await within(paginationBtnBar).findByRole('button', { name: 'goto next page' });

      let rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(16);

      expect(within(rows[2]).getAllByRole('cell')[0]).toHaveTextContent('App0');
      expect(within(rows[2]).getAllByRole('cell')[1]).toHaveTextContent('0');

      axiosMock.reset();

      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(200, {
        dashboardResults: createAppArrayWithLength(14, 15),
        numResults: 50,
      });

      fireEvent.click(nextPageBtn);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(16);

      expect(within(rows[2]).getAllByRole('cell')[0]).toHaveTextContent('App15');
      expect(within(rows[2]).getAllByRole('cell')[1]).toHaveTextContent('15');
    });
  });

  describe('sorting', () => {
    it('TOTAL RISK is sortable and sorted descending by default', async () => {
      const totalDataRows = 10;
      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(200, {
        dashboardResults: createAppArrayWithLength(totalDataRows).reverse(),
        numResults: 10,
      });

      const givenTime = new Date();
      jest.setSystemTime(givenTime);

      render(<AppsWithoutCiIntegrations />);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      const expectedTimestampForQuery = givenTime.setMonth(givenTime.getMonth() - 3);

      expect(axiosMock.history.post.length).toBe(1);
      expect(axiosMock.history.post[0].data).toBe(
        JSON.stringify({
          page: 0,
          pageSize: 14,
          sinceUtcTimestamp: expectedTimestampForQuery,
          optionalOrderBy: '-TOTAL_RISK',
          optionalFilterApplicationNamesBy: '',
        })
      );

      let rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      let totalRiskHeader = await screen.findByRole('columnheader', { name: /total risk/i });
      expect(totalRiskHeader).toBeInTheDocument();
      expect(totalRiskHeader).toHaveAttribute('aria-sort', 'descending');

      for (let i = 0; i < totalDataRows; i++) {
        expect(within(rows[i + 2]).getAllByRole('cell')[1]).toHaveTextContent((totalDataRows - 1 - i).toString());
      }

      axiosMock.reset();

      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(200, {
        dashboardResults: createAppArrayWithLength(totalDataRows),
        numResults: 10,
      });

      fireEvent.click(totalRiskHeader);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.post[0].data).toBe(
        JSON.stringify({
          page: 0,
          pageSize: 14,
          sinceUtcTimestamp: expectedTimestampForQuery,
          optionalOrderBy: 'TOTAL_RISK',
          optionalFilterApplicationNamesBy: '',
        })
      );

      rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      totalRiskHeader = await screen.findByRole('columnheader', { name: /total risk/i });
      expect(totalRiskHeader).toBeInTheDocument();
      expect(totalRiskHeader).toHaveAttribute('aria-sort', 'ascending');

      for (let i = 0; i < totalDataRows; i++) {
        expect(within(rows[i + 2]).getAllByRole('cell')[1]).toHaveTextContent(i.toString());
      }
    });

    it('APPLICATIONS is sortable and unsorted by default', async () => {
      const totalDataRows = 10;
      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(200, {
        dashboardResults: createAppArrayWithLength(totalDataRows).reverse(),
        numResults: 10,
      });

      const givenTime = new Date();
      jest.setSystemTime(givenTime);

      render(<AppsWithoutCiIntegrations />);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      const expectedTimestampForQuery = givenTime.setMonth(givenTime.getMonth() - 3);

      let rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      let applicationsHeader = await screen.findByRole('columnheader', { name: /applications/i });
      expect(applicationsHeader).toBeInTheDocument();
      expect(applicationsHeader).toHaveAttribute('aria-sort', 'none');

      for (let i = 0; i < totalDataRows; i++) {
        expect(within(rows[i + 2]).getAllByRole('cell')[0]).toHaveTextContent(
          `App${(totalDataRows - 1 - i).toString()}`
        );
      }

      axiosMock.reset();

      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(200, {
        dashboardResults: createAppArrayWithLength(totalDataRows),
        numResults: 10,
      });

      fireEvent.click(applicationsHeader);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.post[0].data).toBe(
        JSON.stringify({
          page: 0,
          pageSize: 14,
          sinceUtcTimestamp: expectedTimestampForQuery,
          optionalOrderBy: 'NAME',
          optionalFilterApplicationNamesBy: '',
        })
      );

      rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      applicationsHeader = await screen.findByRole('columnheader', { name: /applications/i });
      expect(applicationsHeader).toBeInTheDocument();
      expect(applicationsHeader).toHaveAttribute('aria-sort', 'ascending');

      for (let i = 0; i < totalDataRows; i++) {
        expect(within(rows[i + 2]).getAllByRole('cell')[0]).toHaveTextContent(`App${i.toString()}`);
      }
    });
  });

  describe('searching', () => {
    it('can be performed for specific applications', async () => {
      const totalDataRows = 10;
      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(200, {
        dashboardResults: createAppArrayWithLength(totalDataRows),
        numResults: 10,
      });

      const givenTime = new Date();
      jest.setSystemTime(givenTime);

      render(<AppsWithoutCiIntegrations />);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      let rows = await screen.findAllByRole('row');

      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      const searchBox = await screen.findByRole('textbox');
      fireEvent.focus(searchBox);
      fireEvent.change(searchBox, { target: { value: 'App5' } });

      jest.advanceTimersByTime(NX_STANDARD_DEBOUNCE_TIME);

      const expectedTimestampForQuery = givenTime.setMonth(givenTime.getMonth() - 3) + NX_STANDARD_DEBOUNCE_TIME;

      expect(searchBox).toHaveValue('App5');

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.post.length).toBe(2);
      expect(axiosMock.history.post[1].data).toBe(
        JSON.stringify({
          page: 0,
          pageSize: 14,
          sinceUtcTimestamp: expectedTimestampForQuery,
          optionalOrderBy: '-TOTAL_RISK',
          optionalFilterApplicationNamesBy: 'App5',
        })
      );
    });
  });

  function createAppArrayWithLength(length, startIndex = 0) {
    return map(
      (i) => ({
        applicationPublicId: `App${i}`,
        applicationName: `App${i}`,
        totalRisk: i,
      }),
      range(startIndex, startIndex + length)
    );
  }

  function assertHeaders() {
    const allHeaders = screen.getAllByRole('columnheader');
    const applicationsHeader = screen.getByRole('columnheader', {
      name: /applications/i,
    });
    const totalRisksHeader = screen.getByRole('columnheader', {
      name: /total risk/i,
    });
    expect(allHeaders.length).toBe(2);
    expect(applicationsHeader).toBeInTheDocument();
    expect(totalRisksHeader).toBeInTheDocument();
  }
});
