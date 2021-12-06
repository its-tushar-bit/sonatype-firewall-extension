/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxPagination, NxTable, NxTableCell, NxTableHead } from '@sonatype/react-shared-components';
import LegalDashboardApplicationsTab from '../../../../main/frontend/legal/dashboard/LegalDashboardApplicationsTab';
import LegalDashboardApplicationRow from '../../../../main/frontend/legal/dashboard/LegalDashboardApplicationRow';
import { DASHBOARD } from '../../../../main/frontend/legal/advancedLegalConstants';
import DashboardMask from '../../../../main/frontend/dashboard/results/dashboardMask/DashboardMask';

describe('LegalDashboardApplicationsTab component', function () {
  let getShallowComponent;

  const minimalProps = {
    applications: {
      results: [
        {
          applicationId: '1',
          applicationName: 'app1',
          lastScanTime: 1000,
          applicationTagNames: ['tag'],
          componentsReviewedCount: 1,
          componentsTotalCount: 2,
        },
        {
          applicationId: '2',
          applicationName: 'app2',
          lastScanTime: 2000,
          applicationTagNames: ['tag'],
          componentsReviewedCount: 2,
          componentsTotalCount: 3,
        },
      ],
      totalResultsCount: 2,
      backendPage: 1,
    },
    fetchBackendPage: () => {},
    changeSortField: () => {},
    legalDashboardSetPage: () => {},
  };

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardApplicationsTab, minimalProps);
  });

  it('renders a table', function () {
    const wrapper = getShallowComponent();
    let table = wrapper.find(NxTable);
    expect(table).toExist();
    expect(table).toHaveClassName('legal-dashboard-table');
  });

  it('renders LegalDashboardApplicationRow components for each application passed in', function () {
    const wrapper = getShallowComponent();
    let table = wrapper.find(NxTable);
    let rows = table.find(LegalDashboardApplicationRow);
    expect(rows).toExist();
    expect(rows.length).toEqual(2);
    expect(rows.at(0)).toHaveProp('row', minimalProps.applications.results[0]);
    expect(rows.at(1)).toHaveProp('row', minimalProps.applications.results[1]);
  });

  it('displays the mask if filtersAreDirty is true', function () {
    const wrapper = getShallowComponent({ filtersAreDirty: true });
    let mask = wrapper.find(DashboardMask);
    expect(mask).toExist();
  });

  it('does not display the mask if filtersAreDirty is false', function () {
    const wrapper = getShallowComponent({ filtersAreDirty: false });
    let mask = wrapper.find('.form-mask');
    expect(mask).not.toExist();
  });

  it('starts client side pagination on zero by default', function () {
    const wrapper = getShallowComponent();
    const pagination = wrapper.find(NxPagination);
    expect(pagination).toExist();
    expect(pagination).toHaveProp('currentPage', 0);
  });

  it('paginates locally without calling backend until reaching end of pages loaded', function () {
    const { itemsPerPage, pagesToFill } = DASHBOARD.applications;
    const items = [];
    for (let index = 0; index < itemsPerPage * pagesToFill; index++) {
      items.push({
        applicationId: `app${index}`,
        applicationName: `app${index}`,
        lastScanTime: 1000,
        applicationTagNames: ['tag'],
        componentsReviewedCount: 1,
        componentsTotalCount: 2,
      });
    }

    const appProps = {
      applications: {
        results: items,
        totalResultsCount: items.length * 3,
        backendPage: 1,
      },
      fetchBackendPage: () => {},
      legalDashboardSetPage: () => {},
    };

    spyOn(appProps, 'fetchBackendPage');
    spyOn(appProps, 'legalDashboardSetPage');

    const wrapper = enzymeUtils.getShallowComponent(LegalDashboardApplicationsTab, appProps)();
    let pagination = wrapper.find(NxPagination);
    expect(pagination).toExist();

    const onChangePage = pagination.prop('onChange');
    for (let index = 0; index < pagesToFill; index++) {
      onChangePage(index);
      expect(appProps.legalDashboardSetPage).toHaveBeenCalledWith({ resultsType: 'applications', page: index });
    }

    expect(appProps.fetchBackendPage).not.toHaveBeenCalled();

    onChangePage(pagesToFill);
    expect(appProps.legalDashboardSetPage).toHaveBeenCalledWith({ resultsType: 'applications', page: pagesToFill });
    expect(appProps.fetchBackendPage).toHaveBeenCalledWith('applications', 2);

    onChangePage(pagesToFill * 2);
    expect(appProps.legalDashboardSetPage).toHaveBeenCalledWith({
      resultsType: 'applications',
      page: pagesToFill * 2,
    });
    expect(appProps.fetchBackendPage).toHaveBeenCalledWith('applications', 3);
  });

  it('changes the sortField properly', function () {
    spyOn(minimalProps, 'changeSortField');
    const wrapper = getShallowComponent();
    const table = wrapper.find(NxTable);
    const tableHeadCells = table.find(NxTableHead).find(NxTableCell);

    expect(tableHeadCells).toExist();
    expect(tableHeadCells.length).toBe(5);

    const expectedResults = ['APPLICATION_NAME', 'LAST_SCAN_TIME', 'TAG_NAMES'];

    for (let index = 0; index < 3; index++) {
      const onClickSort = tableHeadCells.at(index).prop('onClick');

      onClickSort();
      let expectedResult = `${expectedResults[index]}_ASC`;
      expect(minimalProps.changeSortField).toHaveBeenCalledWith('applications', expectedResult);
      minimalProps.applications.sortField = expectedResult;

      onClickSort();
      expectedResult = `${expectedResults[index]}_DESC`;
      expect(minimalProps.changeSortField).toHaveBeenCalledWith('applications', expectedResult);
      minimalProps.applications.sortField = expectedResult;

      onClickSort();
      expectedResult = null;
      expect(minimalProps.changeSortField).toHaveBeenCalledWith('applications', expectedResult);
    }
  });
});
