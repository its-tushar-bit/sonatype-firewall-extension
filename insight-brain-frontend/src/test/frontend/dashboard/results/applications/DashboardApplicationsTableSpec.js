/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import { lensPath, set } from 'ramda';
import {
  NxButton,
  NxInfoAlert,
  NxLoadError,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxIndeterminatePagination,
} from '@sonatype/react-shared-components';

import DashboardApplicationsTableRow from '../../../../../main/frontend/dashboard/results/applications/DashboardApplicationsTableRow';
import DashboardApplicationsTable from '../../../../../main/frontend/dashboard/results/applications/DashboardApplicationsTable';

describe('DashboardApplicationsTable', function () {
  let minimalProps,
    getShallowComponent,
    getMountedComponent,
    setNextApplicationsPageSpy,
    setPreviousApplicationsPageSpy;

  beforeEach(function () {
    setNextApplicationsPageSpy = jasmine.createSpy('setNextViolationsPage');
    setPreviousApplicationsPageSpy = jasmine.createSpy('setPreviousViolationsPage');
    minimalProps = {
      reload: jasmine.createSpy('reload'),
      sortApplications: jasmine.createSpy('sortApplications'),
      applicationResults: {
        results: [{ applicationId: 'app1' }, { applicationId: 'app2' }],
        hasNextPage: true,
        sortFields: ['-totalApplicationRisk.totalRisk'],
        hasMultiplePages: true,
        page: 0,
      },
      setNextApplicationsPage: setNextApplicationsPageSpy,
      setPreviousApplicationsPage: setPreviousApplicationsPageSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(DashboardApplicationsTable, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(DashboardApplicationsTable, minimalProps);
  });

  it('renders a NxTable', function () {
    const dashboardApplicationsProps = {
      ...minimalProps,
      needsAcknowledgement: true,
    };

    const dashboardApplicationsTable = getShallowComponent(dashboardApplicationsProps),
      table = dashboardApplicationsTable.find(NxTable);
    expect(table).toExist();
  });

  describe('contents of the table', function () {
    it('renders a NxTableHead with a headers row with cells for each header', function () {
      const dashboardApplicationsTable = getShallowComponent(),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        headers = headerRow.find(NxTableCell);

      expect(headers.length).toBe(6);
      expect(headers.at(0).children()).toHaveText('Name');
      expect(headers.at(1).children()).toHaveText('Total Risk');

      // Risk columns have a threat indicator as well as the header text, so choosing the last child for comparisons
      expect(headers.at(2).children().at(1)).toHaveText('Critical');
      expect(headers.at(3).children().at(1)).toHaveText('Severe');
      expect(headers.at(4).children().at(1)).toHaveText('Moderate');
      expect(headers.at(5).children().at(1)).toHaveText('Low');
    });

    it('renders DashboardApplicationsTableRow per application to display inside the NxTableBody', function () {
      const dashboardApplicationsTable = getShallowComponent(),
        table = dashboardApplicationsTable.find(NxTable),
        body = table.find(NxTableBody);

      expect(body.children().length).toBe(2);

      const rows = body.find(DashboardApplicationsTableRow);
      expect(rows.at(0).key()).toBe('app1');
      expect(rows.at(1).key()).toBe('app2');
    });

    it('renders an empty message row if there are no applications to display', function () {
      const dashboardApplicationsProps = {
        applicationResults: {
          results: [],
          hasMultiplePages: false,
          page: null,
        },
      };
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        body = table.find(NxTableBody),
        emptyMessageRow = body.find(NxTableRow);

      expect(emptyMessageRow).toHaveText('No data available given the applied filters and permissions.');
    });

    it('renders a row with an alert message when the filter needs acknowledgement', function () {
      const dashboardApplicationsProps = {
        applicationResults: {
          results: [],
          hasMultiplePages: false,
          page: null,
        },
        needsAcknowledgement: true,
      };
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        body = table.find(NxTableBody),
        alertRow = body.find(NxTableRow);

      expect(alertRow).toExist();
      expect(alertRow.find(NxInfoAlert)).toHaveText("Select your filter criteria and click 'apply' to see results.");
    });

    it('renders a row with an error message when an error is present with the appropriate retry handler', function () {
      const dashboardApplicationsProps = {
        applicationResults: {
          results: [],
          error: 'error while retrieving results',
          hasMultiplePages: false,
          page: null,
        },
      };
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        body = table.find(NxTableBody),
        errorRow = body.find(NxLoadError);

      expect(body).toHaveProp('error', 'error while retrieving results');
      expect(body).toHaveProp('retryHandler', minimalProps.reload);

      // to avoid the text added by the internal error components, we'll find the first span for the error message
      expect(errorRow.find('span').at(0)).toHaveText('An error occurred loading data. error while retrieving results');

      errorRow.find(NxButton).simulate('click');
      expect(minimalProps.reload).toHaveBeenCalled();
    });
  });

  describe('Column sorting', function () {
    const applicationsToDisplay = [
      {
        applicationId: 'app1',
        totalApplicationRisk: {},
        stageRisks: [],
      },
      {
        applicationId: 'app2',
        totalApplicationRisk: {},
        stageRisks: [],
      },
    ];

    const defaultPropsForSortChecks = {
      applicationResults: {
        results: applicationsToDisplay,
        sortFields: ['-totalApplicationRisk.totalRisk'],
        hasMultiplePages: false,
        page: 0,
      },
    };

    const defaultApplicationResults = defaultPropsForSortChecks.applicationResults;

    const defaultPropsSortBy = (field) => {
      return {
        applicationResults: {
          ...defaultApplicationResults,
          sortFields: [field],
        },
      };
    };

    it('identifies default direction of sorting for the columns on render', function () {
      const dashboardApplicationsTable = getShallowComponent(defaultPropsForSortChecks),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        headers = headerRow.find(NxTableCell);

      expect(headers.at(0)).toHaveProp('sortDir', null);

      // This column represents the total risk, which is the default sorting
      expect(headers.at(1)).toHaveProp('sortDir', 'desc');

      expect(headers.at(2)).toHaveProp('sortDir', null);
      expect(headers.at(3)).toHaveProp('sortDir', null);
      expect(headers.at(4)).toHaveProp('sortDir', null);
      expect(headers.at(5)).toHaveProp('sortDir', null);
    });

    it('calls sortApplications with applicationName if clicked none to asc', function () {
      const dashboardApplicationsTable = getMountedComponent(defaultPropsForSortChecks),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        applicationNameHeader = headerRow.find(NxTableCell).at(0);

      expect(applicationNameHeader).toHaveProp('sortDir', null);
      applicationNameHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['applicationName']);
    });

    it('calls sortApplications with -applicationName if clicked asc to desc', function () {
      const dashboardApplicationsProps = defaultPropsSortBy('applicationName');
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        applicationNameHeader = headerRow.find(NxTableCell).at(0);

      expect(applicationNameHeader).toHaveProp('sortDir', 'asc');
      applicationNameHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['-applicationName']);
    });

    it('calls sortApplications with applicationName if clicked desc to asc', function () {
      const dashboardApplicationsProps = defaultPropsSortBy('-applicationName');
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        applicationNameHeader = headerRow.find(NxTableCell).at(0);

      expect(applicationNameHeader).toHaveProp('sortDir', 'desc');
      applicationNameHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['applicationName']);
    });

    it('calls sortApplications with -totalApplicationRisk.totalRisk if clicked none to desc', function () {
      const dashboardApplicationsProps = defaultPropsSortBy('applicationName');
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(1);

      expect(totalRiskHeader).toHaveProp('sortDir', null);
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['-totalApplicationRisk.totalRisk']);
    });

    it('calls sortApplications with totalApplicationRisk.totalRisk if clicked desc to asc', function () {
      const dashboardApplicationsProps = {
        ...minimalProps,
        applicationResults: {
          ...minimalProps.applicationResults,
          results: applicationsToDisplay,
        },
      };
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(1);

      expect(totalRiskHeader).toHaveProp('sortDir', 'desc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['totalApplicationRisk.totalRisk']);
    });

    it('calls sortApplications with -totalApplicationRisk.totalRisk if clicked asc to desc', function () {
      const dashboardApplicationsProps = defaultPropsSortBy('totalApplicationRisk.totalRisk');
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(1);

      expect(totalRiskHeader).toHaveProp('sortDir', 'asc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['-totalApplicationRisk.totalRisk']);
    });

    it('calls sortApplications with -totalApplicationRisk.criticalRisk if clicked none to desc', function () {
      const dashboardApplicationsTable = getMountedComponent(defaultPropsForSortChecks),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(2);

      expect(totalRiskHeader).toHaveProp('sortDir', null);
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['-totalApplicationRisk.criticalRisk']);
    });

    it('calls sortApplications with totalApplicationRisk.criticalRisk if clicked desc to asc', function () {
      const dashboardApplicationsProps = defaultPropsSortBy('-totalApplicationRisk.criticalRisk');
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(2);

      expect(totalRiskHeader).toHaveProp('sortDir', 'desc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['totalApplicationRisk.criticalRisk']);
    });

    it('calls sortApplications with -totalApplicationRisk.criticalRisk if clicked asc to desc', function () {
      const dashboardApplicationsProps = defaultPropsSortBy('totalApplicationRisk.criticalRisk');
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(2);

      expect(totalRiskHeader).toHaveProp('sortDir', 'asc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['-totalApplicationRisk.criticalRisk']);
    });

    it('calls sortApplications with -totalApplicationRisk.severeRisk if clicked none to desc', function () {
      const dashboardApplicationsTable = getMountedComponent(defaultPropsForSortChecks),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(3);

      expect(totalRiskHeader).toHaveProp('sortDir', null);
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['-totalApplicationRisk.severeRisk']);
    });

    it('calls sortApplications with totalApplicationRisk.severeRisk if clicked desc to asc', function () {
      const dashboardApplicationsProps = defaultPropsSortBy('-totalApplicationRisk.severeRisk');
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(3);

      expect(totalRiskHeader).toHaveProp('sortDir', 'desc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['totalApplicationRisk.severeRisk']);
    });

    it('calls sortApplications with -totalApplicationRisk.severeRisk if clicked asc to desc', function () {
      const dashboardApplicationsProps = defaultPropsSortBy('totalApplicationRisk.severeRisk');
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(3);

      expect(totalRiskHeader).toHaveProp('sortDir', 'asc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['-totalApplicationRisk.severeRisk']);
    });

    it('calls sortApplications with -totalApplicationRisk.moderateRisk if clicked none to desc', function () {
      const dashboardApplicationsTable = getMountedComponent(defaultPropsForSortChecks),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(4);

      expect(totalRiskHeader).toHaveProp('sortDir', null);
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['-totalApplicationRisk.moderateRisk']);
    });

    it('calls sortApplications with totalApplicationRisk.moderateRisk if clicked desc to asc', function () {
      const dashboardApplicationsProps = defaultPropsSortBy('-totalApplicationRisk.moderateRisk');
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(4);

      expect(totalRiskHeader).toHaveProp('sortDir', 'desc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['totalApplicationRisk.moderateRisk']);
    });

    it('calls sortApplications with -totalApplicationRisk.moderateRisk if clicked asc to desc', function () {
      const dashboardApplicationsProps = defaultPropsSortBy('totalApplicationRisk.moderateRisk');
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(4);

      expect(totalRiskHeader).toHaveProp('sortDir', 'asc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['-totalApplicationRisk.moderateRisk']);
    });

    it('calls sortApplications with -totalApplicationRisk.lowRisk if clicked none to desc', function () {
      const dashboardApplicationsTable = getMountedComponent(defaultPropsForSortChecks),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(5);

      expect(totalRiskHeader).toHaveProp('sortDir', null);
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['-totalApplicationRisk.lowRisk']);
    });

    it('calls sortApplications with totalApplicationRisk.lowRisk if clicked desc to asc', function () {
      const dashboardApplicationsProps = defaultPropsSortBy('-totalApplicationRisk.lowRisk');
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(5);

      expect(totalRiskHeader).toHaveProp('sortDir', 'desc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['totalApplicationRisk.lowRisk']);
    });

    it('calls sortApplications with -totalApplicationRisk.lowRisk if clicked asc to desc', function () {
      const dashboardApplicationsProps = defaultPropsSortBy('totalApplicationRisk.lowRisk');
      const dashboardApplicationsTable = getMountedComponent(dashboardApplicationsProps),
        table = dashboardApplicationsTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(5);

      expect(totalRiskHeader).toHaveProp('sortDir', 'asc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortApplications).toHaveBeenCalledWith(['-totalApplicationRisk.lowRisk']);
    });
  });

  describe('pagination', () => {
    it('renders a NxIndeterminatePagination component', () => {
      const pagination = getShallowComponent().find(NxIndeterminatePagination);
      expect(pagination).toExist();
      expect(pagination).toHaveProp('isFirstPage', true);
      expect(pagination).toHaveProp('isLastPage', false);
      expect(pagination).toHaveProp('onPrevPageSelect', setPreviousApplicationsPageSpy);
      expect(pagination).toHaveProp('onNextPageSelect', setNextApplicationsPageSpy);
    });

    it('does not render NxIndeterminatePagination component when there is no Next Page', () => {
      const minimalProps = {
        reload: jasmine.createSpy('reload'),
        sortApplications: jasmine.createSpy('sortApplications'),
        applicationResults: {
          results: [],
          hasNextPage: false,
          sortFields: ['-totalApplicationRisk.totalRisk'],
          hasMultiplePages: false,
          page: null,
        },
        setNextApplicationsPage: setNextApplicationsPageSpy,
        setPreviousApplicationsPage: setPreviousApplicationsPageSpy,
      };
      const props = set(lensPath(['applications', 'NextPage']), false, minimalProps);
      const pagination = getShallowComponent(props).find(NxIndeterminatePagination);
      expect(pagination).not.toExist();
    });

    it('does not render NxIndeterminatePagination component when there are no results', () => {
      const minimalProps = {
        reload: jasmine.createSpy('reload'),
        sortApplications: jasmine.createSpy('sortApplications'),
        applicationResults: {
          results: [],
          sortFields: ['-totalApplicationRisk.totalRisk'],
          hasMultiplePages: false,
          page: null,
        },
        setNextApplicationsPage: setNextApplicationsPageSpy,
        setPreviousApplicationsPage: setPreviousApplicationsPageSpy,
      };
      const props = set(lensPath(['applications', 'hasMultiplePages']), 0, minimalProps);
      const pagination = getShallowComponent(props).find(NxIndeterminatePagination);
      expect(pagination).not.toExist();
    });
  });
});
