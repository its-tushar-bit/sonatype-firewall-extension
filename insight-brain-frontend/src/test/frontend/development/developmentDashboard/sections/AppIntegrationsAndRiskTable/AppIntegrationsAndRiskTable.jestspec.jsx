/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter, fireEvent, within } from 'TestRoot/SpecUtil';
import { getAppIntegrationsAndRisk } from 'MainRoot/util/CLMLocation';
import AppIntegrationsAndRiskTable from 'MainRoot/development/developmentDashboard/sections/AppIntegrationsAndRiskTable/AppIntegrationsAndRiskTable';
import { map, range } from 'ramda';
import { NX_STANDARD_DEBOUNCE_TIME, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import * as routerStateContext from 'MainRoot/react/RouterStateContext';

describe('AppIntegrationsAndRiskTable', () => {
  let axiosMock;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  it('should render a loading message when network call is pending', () => {
    render(<AppIntegrationsAndRiskTable />);

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  describe('on a failed http call', () => {
    it('displays a proper error', async () => {
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(404, 'Error');

      render(<AppIntegrationsAndRiskTable />);

      const properErrorMsg = await screen.findByText(/an error occurred loading data\./i);
      expect(properErrorMsg).toBeInTheDocument();
    });
  });

  describe('on successful http calls retrieving an empty list', () => {
    beforeEach(() => {
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: [],
        total: 0,
      });
    });

    it('renders and only renders the desired column headers', () => {
      render(<AppIntegrationsAndRiskTable />);

      assertHeaders();
    });

    it('render "No data available given the applied filters and permissions." message eventually', async () => {
      render(<AppIntegrationsAndRiskTable />);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      const msg = await screen.findByRole('cell', {
        name: /No data available given the applied filters and permissions\./i,
      });
      expect(msg).toBeInTheDocument();
    });
  });

  describe('on successful http calls retrieving a non-empty list', () => {
    beforeEach(() => {
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(15),
        total: 15,
        page: 1,
        pageSize: 10,
        pageCount: 2,
      });
    });

    it('renders and only renders the desired column headers', () => {
      render(<AppIntegrationsAndRiskTable />);

      assertHeaders();
    });

    it('renders the first page button eventually', async () => {
      render(<AppIntegrationsAndRiskTable />);

      const button = await screen.findByRole('button', {
        name: /goto first page/i,
      });
      expect(button).toBeInTheDocument();
    });

    it('renders data correctly in each table cell', async () => {
      const date = new Date('January 1, 2023');
      const timestamp = date.getTime();

      const results = [
        {
          applicationName: `App1`,
          applicationId: `AppId1`,
          applicationPublicId: `App1`,
          lastCommitTimestamp: timestamp,
          lastEvaluationTimestamp: timestamp,
          totalRiskScore: 404,
          ciIntegrationEnabled: true,
          automatedSourceControlFeedbackEnabled: true,
          hasPrioritiesReport: true,
          lastScanId: 'lastScanId',
        },
      ];
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: results,
        total: 1,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });

      render(<AppIntegrationsAndRiskTable />);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      const rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(3);

      const cells = within(rows[2]).getAllByRole('cell');

      expect(cells[0]).toHaveTextContent('App1');
      expect(within(cells[1]).getByRole('img', { hidden: true })).toBeInTheDocument();
      expect(within(cells[2]).getByRole('img', { hidden: true })).toBeInTheDocument();
      expect(cells[3]).toHaveTextContent('January 1, 2023');
      expect(cells[4]).toHaveTextContent('January 1, 2023');
      expect(cells[5]).toHaveTextContent('404');
    });

    it('renders total risk tooltip correctly', async () => {
      const date = new Date('January 1, 2023');
      const timestamp = date.getTime();

      const results = [
        {
          applicationName: `App1`,
          applicationId: `AppId1`,
          applicationPublicId: `App1`,
          lastCommitTimestamp: timestamp,
          lastEvaluationTimestamp: timestamp,
          totalRiskScore: -1,
          ciIntegrationEnabled: true,
          automatedSourceControlFeedbackEnabled: true,
          hasPrioritiesReport: true,
          lastScanId: 'lastScanId',
        },
      ];
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: results,
        total: 1,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });

      render(<AppIntegrationsAndRiskTable />);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      const rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(3);

      const cells = within(rows[2]).getAllByRole('cell');
      expect(cells[5]).toHaveTextContent('N/A');

      fireEvent.mouseOver(screen.getByText('N/A'));
      const tooltip = await screen.findByRole('tooltip');

      expect(
        within(tooltip).getByText('Evaluate this application at the build stage to see its risk score')
      ).toBeInTheDocument();
    });
  });

  describe('pagination buttons', () => {
    it('make correct GET requests', async () => {
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(10),
        total: 50,
        page: 1,
        pageSize: 10,
        pageCount: 5,
      });

      render(<AppIntegrationsAndRiskTable />);

      const paginationBtnBar = await screen.findByRole('navigation');
      const nextPageBtn = await within(paginationBtnBar).findByRole('button', { name: 'goto next page' });

      expect(axiosMock.history.get.length).toBe(1);
      expect(axiosMock.history.get[0].params).toEqual({
        optionalFilterApplicationNamesBy: '',
        optionalFilterCiCdIsIntegrated: null,
        optionalFilterScmIsIntegrated: null,
        optionalOrderBy: '-TOTAL_RISK',
        page: 1,
        pageSize: 10,
      });

      fireEvent.click(nextPageBtn);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.get.length).toBe(2);
      expect(axiosMock.history.get[0].url).toBe(getAppIntegrationsAndRisk());
      expect(axiosMock.history.get[1].url).toBe(getAppIntegrationsAndRisk());
      expect(axiosMock.history.get[1].params).toEqual({
        optionalFilterApplicationNamesBy: '',
        optionalFilterCiCdIsIntegrated: null,
        optionalFilterScmIsIntegrated: null,
        optionalOrderBy: '-TOTAL_RISK',
        page: 2,
        pageSize: 10,
      });

      fireEvent.click(nextPageBtn);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.get.length).toBe(3);
      expect(axiosMock.history.get[2].params).toEqual({
        optionalFilterApplicationNamesBy: '',
        optionalFilterCiCdIsIntegrated: null,
        optionalFilterScmIsIntegrated: null,
        optionalOrderBy: '-TOTAL_RISK',
        page: 3,
        pageSize: 10,
      });
    });

    it('change data on the table', async () => {
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(10),
        total: 50,
        page: 1,
        pageSize: 10,
        pageCount: 5,
      });

      render(<AppIntegrationsAndRiskTable />);

      const paginationBtnBar = await screen.findByRole('navigation');
      const nextPageBtn = await within(paginationBtnBar).findByRole('button', { name: 'goto next page' });
      let rows = await screen.findAllByRole('row');

      expect(rows.length).toBe(12);
      expect(within(rows[2]).getAllByRole('cell')[0]).toHaveTextContent('App0');
      expect(within(rows[2]).getAllByRole('cell')[5]).toHaveTextContent('0');

      axiosMock.reset();

      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(10, 20),
        total: 50,
        page: 2,
        pageSize: 10,
        pageCount: 5,
      });

      fireEvent.click(nextPageBtn);

      expect(await screen.findByRole('table')).toBeInTheDocument();
      rows = await screen.findAllByRole('row');

      expect(rows.length).toBe(12);
      expect(within(rows[2]).getAllByRole('cell')[0]).toHaveTextContent('App20');
      expect(within(rows[2]).getAllByRole('cell')[5]).toHaveTextContent('20');
    });
  });

  describe('sorting', () => {
    it('TOTAL RISK is sortable and sorted descending by default', async () => {
      const totalDataRows = 10;
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(totalDataRows).reverse(),
        numResults: 10,
        total: 10,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });

      render(<AppIntegrationsAndRiskTable />);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.get.length).toBe(1);
      expect(axiosMock.history.get[0].params).toEqual({
        optionalFilterApplicationNamesBy: '',
        optionalFilterCiCdIsIntegrated: null,
        optionalFilterScmIsIntegrated: null,
        optionalOrderBy: '-TOTAL_RISK',
        page: 1,
        pageSize: 10,
      });

      let rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      let totalRiskHeader = await screen.findByRole('columnheader', { name: /total risk/i });
      expect(totalRiskHeader).toBeInTheDocument();
      expect(totalRiskHeader).toHaveAttribute('aria-sort', 'descending');

      for (let i = 0; i < totalDataRows; i++) {
        expect(within(rows[i + 2]).getAllByRole('cell')[5]).toHaveTextContent((totalDataRows - 1 - i).toString());
      }

      axiosMock.reset();

      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(totalDataRows),
        numResults: 10,
        total: 10,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });

      fireEvent.click(totalRiskHeader);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.get[0].params).toEqual({
        optionalFilterApplicationNamesBy: '',
        optionalFilterCiCdIsIntegrated: null,
        optionalFilterScmIsIntegrated: null,
        optionalOrderBy: 'TOTAL_RISK',
        page: 1,
        pageSize: 10,
      });

      rows = await screen.findAllByRole('row');

      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      totalRiskHeader = await screen.findByRole('columnheader', { name: /total risk/i });
      expect(totalRiskHeader).toBeInTheDocument();
      expect(totalRiskHeader).toHaveAttribute('aria-sort', 'ascending');

      for (let i = 0; i < totalDataRows; i++) {
        expect(within(rows[i + 2]).getAllByRole('cell')[5]).toHaveTextContent(i.toString());
      }
    });

    it('APPLICATIONS is sortable and unsorted by default', async () => {
      const totalDataRows = 10;
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(totalDataRows).reverse(),
        numResults: 10,
        total: 10,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });

      render(<AppIntegrationsAndRiskTable />);

      expect(await screen.findByRole('table')).toBeInTheDocument();

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

      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(totalDataRows),
        numResults: 10,
        total: 10,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });

      fireEvent.click(applicationsHeader);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.get[0].params).toEqual({
        optionalFilterApplicationNamesBy: '',
        optionalFilterCiCdIsIntegrated: null,
        optionalFilterScmIsIntegrated: null,
        optionalOrderBy: 'NAME',
        page: 1,
        pageSize: 10,
      });

      rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      applicationsHeader = await screen.findByRole('columnheader', { name: /applications/i });
      expect(applicationsHeader).toBeInTheDocument();
      expect(applicationsHeader).toHaveAttribute('aria-sort', 'ascending');

      for (let i = 0; i < totalDataRows; i++) {
        expect(within(rows[i + 2]).getAllByRole('cell')[0]).toHaveTextContent(`App${i.toString()}`);
      }
    });

    it('LAST COMMIT is sortable and unsorted by default', async () => {
      const totalDataRows = 10;
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(totalDataRows).reverse(),
        numResults: 10,
        total: 10,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });

      render(<AppIntegrationsAndRiskTable />);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      let rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      let lastCommitHeader = await screen.findByRole('columnheader', { name: /last commit/i });
      expect(lastCommitHeader).toBeInTheDocument();
      expect(lastCommitHeader).toHaveAttribute('aria-sort', 'none');

      expect(within(rows[2]).getAllByRole('cell')[3]).toHaveTextContent('January 10, 2023');
      expect(within(rows[3]).getAllByRole('cell')[3]).toHaveTextContent('January 9, 2023');
      expect(within(rows[4]).getAllByRole('cell')[3]).toHaveTextContent('January 8, 2023');
      expect(within(rows[5]).getAllByRole('cell')[3]).toHaveTextContent('January 7, 2023');
      expect(within(rows[6]).getAllByRole('cell')[3]).toHaveTextContent('January 6, 2023');
      expect(within(rows[7]).getAllByRole('cell')[3]).toHaveTextContent('January 5, 2023');
      expect(within(rows[8]).getAllByRole('cell')[3]).toHaveTextContent('January 4, 2023');
      expect(within(rows[9]).getAllByRole('cell')[3]).toHaveTextContent('January 3, 2023');
      expect(within(rows[10]).getAllByRole('cell')[3]).toHaveTextContent('January 2, 2023');
      expect(within(rows[11]).getAllByRole('cell')[3]).toHaveTextContent('January 1, 2023');

      axiosMock.reset();

      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(totalDataRows),
        numResults: 10,
        total: 10,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });

      fireEvent.click(lastCommitHeader);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.get[0].params).toEqual({
        optionalFilterApplicationNamesBy: '',
        optionalFilterCiCdIsIntegrated: null,
        optionalFilterScmIsIntegrated: null,
        optionalOrderBy: 'COMMIT',
        page: 1,
        pageSize: 10,
      });

      rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      lastCommitHeader = await screen.findByRole('columnheader', { name: /last commit/i });
      expect(lastCommitHeader).toBeInTheDocument();
      expect(lastCommitHeader).toHaveAttribute('aria-sort', 'ascending');

      expect(within(rows[2]).getAllByRole('cell')[3]).toHaveTextContent('January 1, 2023');
      expect(within(rows[3]).getAllByRole('cell')[3]).toHaveTextContent('January 2, 2023');
      expect(within(rows[4]).getAllByRole('cell')[3]).toHaveTextContent('January 3, 2023');
      expect(within(rows[5]).getAllByRole('cell')[3]).toHaveTextContent('January 4, 2023');
      expect(within(rows[6]).getAllByRole('cell')[3]).toHaveTextContent('January 5, 2023');
      expect(within(rows[7]).getAllByRole('cell')[3]).toHaveTextContent('January 6, 2023');
      expect(within(rows[8]).getAllByRole('cell')[3]).toHaveTextContent('January 7, 2023');
      expect(within(rows[9]).getAllByRole('cell')[3]).toHaveTextContent('January 8, 2023');
      expect(within(rows[10]).getAllByRole('cell')[3]).toHaveTextContent('January 9, 2023');
      expect(within(rows[11]).getAllByRole('cell')[3]).toHaveTextContent('January 10, 2023');
    });

    it('LAST EVALUATION is sortable and unsorted by default', async () => {
      const totalDataRows = 10;
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(totalDataRows).reverse(),
        numResults: 10,
        total: 10,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });

      render(<AppIntegrationsAndRiskTable />);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      let rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      let lastEvaluationHeader = await screen.findByRole('columnheader', { name: /last evaluation/i });
      expect(lastEvaluationHeader).toBeInTheDocument();
      expect(lastEvaluationHeader).toHaveAttribute('aria-sort', 'none');

      expect(within(rows[2]).getAllByRole('cell')[4]).toHaveTextContent('January 10, 2023');
      expect(within(rows[3]).getAllByRole('cell')[4]).toHaveTextContent('January 9, 2023');
      expect(within(rows[4]).getAllByRole('cell')[4]).toHaveTextContent('January 8, 2023');
      expect(within(rows[5]).getAllByRole('cell')[4]).toHaveTextContent('January 7, 2023');
      expect(within(rows[6]).getAllByRole('cell')[4]).toHaveTextContent('January 6, 2023');
      expect(within(rows[7]).getAllByRole('cell')[4]).toHaveTextContent('January 5, 2023');
      expect(within(rows[8]).getAllByRole('cell')[4]).toHaveTextContent('January 4, 2023');
      expect(within(rows[9]).getAllByRole('cell')[4]).toHaveTextContent('January 3, 2023');
      expect(within(rows[10]).getAllByRole('cell')[4]).toHaveTextContent('January 2, 2023');
      expect(within(rows[11]).getAllByRole('cell')[4]).toHaveTextContent('January 1, 2023');

      axiosMock.reset();
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(totalDataRows),
        numResults: 10,
        total: 10,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });

      fireEvent.click(lastEvaluationHeader);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.get[0].params).toEqual({
        optionalFilterApplicationNamesBy: '',
        optionalFilterCiCdIsIntegrated: null,
        optionalFilterScmIsIntegrated: null,
        optionalOrderBy: 'EVALUATION',
        page: 1,
        pageSize: 10,
      });

      rows = await screen.findAllByRole('row');
      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      lastEvaluationHeader = await screen.findByRole('columnheader', { name: /last evaluation/i });
      expect(lastEvaluationHeader).toBeInTheDocument();
      expect(lastEvaluationHeader).toHaveAttribute('aria-sort', 'ascending');

      expect(within(rows[2]).getAllByRole('cell')[4]).toHaveTextContent('January 1, 2023');
      expect(within(rows[3]).getAllByRole('cell')[4]).toHaveTextContent('January 2, 2023');
      expect(within(rows[4]).getAllByRole('cell')[4]).toHaveTextContent('January 3, 2023');
      expect(within(rows[5]).getAllByRole('cell')[4]).toHaveTextContent('January 4, 2023');
      expect(within(rows[6]).getAllByRole('cell')[4]).toHaveTextContent('January 5, 2023');
      expect(within(rows[7]).getAllByRole('cell')[4]).toHaveTextContent('January 6, 2023');
      expect(within(rows[8]).getAllByRole('cell')[4]).toHaveTextContent('January 7, 2023');
      expect(within(rows[9]).getAllByRole('cell')[4]).toHaveTextContent('January 8, 2023');
      expect(within(rows[10]).getAllByRole('cell')[4]).toHaveTextContent('January 9, 2023');
      expect(within(rows[11]).getAllByRole('cell')[4]).toHaveTextContent('January 10, 2023');
    });
  });

  describe('searching', () => {
    it('can be performed for specific applications', async () => {
      const totalDataRows = 10;
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(totalDataRows).reverse(),
        numResults: 10,
        total: 10,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });

      render(<AppIntegrationsAndRiskTable />);

      expect(await screen.findByRole('table')).toBeInTheDocument();

      let rows = await screen.findAllByRole('row');

      expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

      const searchBox = await screen.findByRole('textbox');
      fireEvent.focus(searchBox);
      fireEvent.change(searchBox, { target: { value: 'App5' } });

      jest.advanceTimersByTime(NX_STANDARD_DEBOUNCE_TIME);

      expect(searchBox).toHaveValue('App5');

      expect(await screen.findByRole('table')).toBeInTheDocument();

      expect(axiosMock.history.get.length).toBe(2);
      expect(axiosMock.history.get[1].params).toEqual({
        optionalFilterApplicationNamesBy: 'App5',
        optionalFilterCiCdIsIntegrated: null,
        optionalFilterScmIsIntegrated: null,
        optionalOrderBy: '-TOTAL_RISK',
        page: 1,
        pageSize: 10,
      });
    });
  });

  it('renders the configure button if the user has not enabled the CICD and SCM configuration', async () => {
    const totalDataRows = 10;
    axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
      results: createAppArrayWithLength(totalDataRows).reverse(),
      numResults: 10,
      total: 10,
      page: 1,
      pageSize: 10,
      pageCount: 1,
    });
    render(<AppIntegrationsAndRiskTable />);
    expect(await screen.findByRole('table')).toBeInTheDocument();

    let rows = await screen.findAllByRole('row');
    expect(rows.length).toBe(totalDataRows + 2); //10 data rows, 1 filter row and 1 header

    for (let i = 0; i < totalDataRows; i++) {
      expect(within(rows[i + 2]).queryAllByRole('cell', { name: 'Configure' })[0]).toBeInTheDocument();
      expect(within(rows[i + 2]).queryAllByRole('cell', { name: 'Configure' })[1]).toBeInTheDocument();
    }
  });

  it('renders the enabled icon if the user has enabled the SCM and CICD configuration', async () => {
    const totalDataRows = 10;
    axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
      results: createAppArrayWithLength(totalDataRows, 0, true, true).reverse(),
      numResults: 10,
      total: 10,
      page: 1,
      pageSize: 10,
      pageCount: 1,
    });
    render(<AppIntegrationsAndRiskTable />);
    expect(await screen.findByRole('table')).toBeInTheDocument();

    let rows = await screen.findAllByRole('row');
    for (let i = 0; i < totalDataRows; i++) {
      expect(within(rows[i + 2]).queryAllByRole('cell', { name: NxFontAwesomeIcon.name })[0]).toBeInTheDocument();
      expect(within(rows[i + 2]).queryAllByRole('cell', { name: NxFontAwesomeIcon.name })[1]).toBeInTheDocument();
    }
  });

  describe('Priorities Report column', () => {
    it('renders "N/A" when hasPrioritiesReport is false', async () => {
      const totalDataRows = 10;
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results: createAppArrayWithLength(totalDataRows, 0, true, true, false).reverse(),
        numResults: 10,
        total: 10,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });
      render(<AppIntegrationsAndRiskTable />);
      expect(await screen.findByRole('table')).toBeInTheDocument();

      let rows = await screen.findAllByRole('row');
      for (let i = 0; i < totalDataRows; i++) {
        const prioritiesReportCell = within(rows[i + 2]).queryAllByRole('cell')[6];
        expect(prioritiesReportCell).toBeInTheDocument();
        expect(prioritiesReportCell).toHaveTextContent('N/A');

        expect(within(prioritiesReportCell).queryByRole('link')).not.toBeInTheDocument();
      }
    });

    it('renders a link "View" when hasPrioritiesReport is true', async () => {
      const hrefSpy = jest
        .fn('href')
        .mockImplementation((_, params) => `#/development/priorities/${params.publicAppId}/${params.scanId}`);
      const routerContextMock = { href: hrefSpy };
      jest.spyOn(routerStateContext, 'useRouterState').mockReturnValue(routerContextMock);

      const totalDataRows = 10;
      const results = createAppArrayWithLength(totalDataRows, 0, true, true, true).reverse();
      axiosMock.onGet(getAppIntegrationsAndRisk()).reply(200, {
        results,
        numResults: 10,
        total: 10,
        page: 1,
        pageSize: 10,
        pageCount: 1,
      });

      render(<AppIntegrationsAndRiskTable />);
      expect(await screen.findByRole('table')).toBeInTheDocument();

      let rows = await screen.findAllByRole('row');
      for (let i = 0; i < totalDataRows; i++) {
        const prioritiesReportCell = within(rows[i + 2]).queryAllByRole('cell')[6];
        expect(prioritiesReportCell).toBeInTheDocument();

        const prioritiesReportLink = within(prioritiesReportCell).getByRole('link', { name: /view/i });
        expect(prioritiesReportLink).toBeInTheDocument();
        expect(prioritiesReportLink).toHaveAttribute(
          'href',
          `#/development/priorities/${results[i].applicationPublicId}/${results[i].lastScanId}`
        );
      }
    });
  });

  function createAppArrayWithLength(
    length,
    startIndex = 0,
    cicdEnabled = false,
    scmEnabled = false,
    hasPrioritiesReport = false
  ) {
    // Create a date object for January 1, 2023
    const date = new Date('January 1, 2023');

    // Get the timestamp in milliseconds
    const timestamp = date.getTime();

    const oneDayMilliseconds = 24 * 60 * 60 * 1000;

    return map(
      (i) => ({
        applicationName: `App${i}`,
        applicationId: `AppId${i}`,
        applicationPublicId: `App${i}`,
        lastCommitTimestamp: timestamp + i * oneDayMilliseconds,
        lastEvaluationTimestamp: timestamp + i * oneDayMilliseconds,
        totalRiskScore: i,
        ciIntegrationEnabled: cicdEnabled,
        automatedSourceControlFeedbackEnabled: scmEnabled,
        organizationId: `OrgId${i}`,
        hasPrioritiesReport: hasPrioritiesReport,
        lastScanId: hasPrioritiesReport ? `lastScanId${i}` : null,
      }),
      range(startIndex, startIndex + length)
    );
  }

  function assertHeaders() {
    const allHeaders = screen.getAllByRole('columnheader');
    const applicationsHeader = screen.getByRole('columnheader', {
      name: /applications/i,
    });
    const cicdHeader = screen.getByRole('columnheader', {
      name: /ci\/cd/i,
    });
    const scmFeedbackHeader = screen.getByRole('columnheader', {
      name: /scm feedback/i,
    });
    const lastCommitHeader = screen.getByRole('columnheader', {
      name: /last commit/i,
    });
    const lastEvaluationHeader = screen.getByRole('columnheader', {
      name: /last evaluation/i,
    });
    const totalRiskHeader = screen.getByRole('columnheader', {
      name: /total risk/i,
    });
    const prioritiesHeader = screen.getByRole('columnheader', {
      name: /priorities/i,
    });
    expect(allHeaders.length).toBe(7);
    expect(applicationsHeader).toBeInTheDocument();
    expect(cicdHeader).toBeInTheDocument();
    expect(scmFeedbackHeader).toBeInTheDocument();
    expect(lastCommitHeader).toBeInTheDocument();
    expect(lastEvaluationHeader).toBeInTheDocument();
    expect(totalRiskHeader).toBeInTheDocument();
    expect(prioritiesHeader).toBeInTheDocument();
  }
});
