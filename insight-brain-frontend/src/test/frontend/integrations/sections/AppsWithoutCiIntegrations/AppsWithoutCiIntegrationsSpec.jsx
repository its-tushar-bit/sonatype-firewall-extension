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

describe('AppsWithoutCiIntegrations Page', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
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

    it('renders a loading spinner while loading', () => {
      expect(screen.getByText('Loading…')).toBeInTheDocument();
    });

    it('render "no data found" message eventually', async () => {
      const msg = await screen.findByRole('cell', {
        name: /no data found\./i,
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

    it('renders a loading spinner while loading', () => {
      expect(screen.getByText('Loading…')).toBeInTheDocument();
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
    beforeEach(() => {
      jasmine.clock().install();
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('make correct POST requests', async () => {
      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(200, {
        dashboardResults: createAppArrayWithLength(14),
        numResults: 50,
      });

      const givenTime = new Date();
      jasmine.clock().mockDate(givenTime);

      render(<AppsWithoutCiIntegrations />);

      const paginationBtnBar = await screen.findByRole('navigation');
      const nextPageBtn = await within(paginationBtnBar).findByRole('button', { name: 'goto next page' });

      expect(axiosMock.history.post.length).toBe(1);

      fireEvent.click(nextPageBtn);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.post.length).toBe(2);
      expect(axiosMock.history.post[0].url).toBe(getAppsWithoutCiIntegrations());
      expect(axiosMock.history.post[1].url).toBe(getAppsWithoutCiIntegrations());

      const expectedTimestampForQuery = givenTime.setMonth(givenTime.getMonth() - 3);

      expect(axiosMock.history.post[1].data).toBe(
        JSON.stringify({
          page: 1,
          pageSize: 14,
          sinceUtcTimestamp: expectedTimestampForQuery,
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
      expect(rows.length).toBe(15);

      expect(screen.getAllByRole('cell')[0]).toHaveTextContent('App0');
      expect(screen.getAllByRole('cell')[1]).toHaveTextContent('0');

      axiosMock.reset();

      axiosMock.onPost(getAppsWithoutCiIntegrations()).reply(200, {
        dashboardResults: createAppArrayWithLength(14, 15),
        numResults: 50,
      });

      fireEvent.click(nextPageBtn);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(15);

      expect(screen.getAllByRole('cell')[0]).toHaveTextContent('App15');
      expect(screen.getAllByRole('cell')[1]).toHaveTextContent('15');
    });
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
