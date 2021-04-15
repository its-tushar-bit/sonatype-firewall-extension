/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import {
  NxButton,
  NxInfoAlert,
  NxLoadError,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';

import DashboardComponentsTableRow from '../../../../../main/frontend/dashboard/results/components/DashboardComponentsTableRow';
import DashboardComponentsTable from '../../../../../main/frontend/dashboard/results/components/DashboardComponentsTable';
import MaxResultsInfoRow from '../../../../../main/frontend/dashboard/results/MaxResultsInfoRow';

describe('DashboardComponentsTable', function () {
  let minimalProps, getShallowComponent, getMountedComponent;

  beforeEach(function () {
    minimalProps = {
      reload: jasmine.createSpy('reload'),
      sortComponents: jasmine.createSpy('sortComponents'),
      stateGo: jasmine.createSpy('stateGo'),
      componentResults: {
        results: [{ hash: 'hash1' }, { hash: 'hash2' }],
        sortFields: ['-score'],
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(
      DashboardComponentsTable,
      minimalProps
    );
    getMountedComponent = enzymeUtils.getMountedComponent(
      DashboardComponentsTable,
      minimalProps
    );
  });

  it('renders a NxTable', function () {
    const dashboardComponentTable = getShallowComponent(),
      table = dashboardComponentTable.find(NxTable);
    expect(table).toExist();
  });

  describe('contents of the table', function () {
    it('renders a NxTableHead with a headers row with cells for each header', function () {
      const dashboardComponentTable = getShallowComponent(),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        headers = headerRow.find(NxTableCell);

      expect(headers.length).toBe(8);
      expect(headers.at(0).children()).toHaveText('Name');
      expect(headers.at(1).children()).toHaveText('Apps');
      expect(headers.at(2).children()).toHaveText('Total Risk');

      // Risk columns have a threat indicator as well as the header text, so choosing the last child for comparisons
      expect(headers.at(3).children().at(1)).toHaveText('Critical');
      expect(headers.at(4).children().at(1)).toHaveText('Severe');
      expect(headers.at(5).children().at(1)).toHaveText('Moderate');
      expect(headers.at(6).children().at(1)).toHaveText('Low');

      expect(headers.at(7)).toHaveProp('chevron', true);
    });

    it('renders DashboardViolationsTableRow per component to display inside the NxTableBody', function () {
      const dashboardComponentTable = getShallowComponent(),
        table = dashboardComponentTable.find(NxTable),
        body = table.find(NxTableBody);

      expect(body.children().length).toBe(2);

      const rows = body.find(DashboardComponentsTableRow);
      expect(rows.at(0).key()).toBe('hash1');
      expect(rows.at(1).key()).toBe('hash2');
    });

    it('Does not render max results row when there are less than 100 results', function () {
      const dashboardComponentProps = {
        componentResults: {
          numResults: 99,
        },
      };

      const dashBoardComponents = getShallowComponent(dashboardComponentProps),
        table = dashBoardComponents.find(NxTable),
        body = table.find(NxTableBody),
        maxResultsInfoRow = body.find(MaxResultsInfoRow);
      expect(maxResultsInfoRow).not.toExist();
    });

    it('Does not render max results row when there are exactly 100 results', function () {
      const dashboardComponentProps = {
        ...minimalProps,
        componentResults: {
          ...minimalProps.componentResults,
          numResults: 100,
        },
      };

      const dashBoardComponents = getShallowComponent(dashboardComponentProps),
        table = dashBoardComponents.find(NxTable),
        body = table.find(NxTableBody),
        maxResultsInfoRow = body.find(MaxResultsInfoRow);
      expect(maxResultsInfoRow).not.toExist();
    });

    it('renders max results row when there are more than 100 results', function () {
      const dashboardComponentProps = {
        ...minimalProps,
        componentResults: {
          ...minimalProps.componentResults,
          numResults: 101,
        },
      };

      const dashBoardComponents = getShallowComponent(dashboardComponentProps),
        table = dashBoardComponents.find(NxTable),
        body = table.find(NxTableBody),
        maxResultsInfoRow = body.find(MaxResultsInfoRow);

      expect(maxResultsInfoRow).toExist();
    });

    it('renders a row with an alert message when the filter needs acknowledgement', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: [],
          sortFields: ['-score'],
        },
        needsAcknowledgement: true,
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        body = table.find(NxTableBody),
        alertRow = body.find(NxTableRow);

      expect(alertRow).toExist();
      expect(alertRow.find(NxInfoAlert)).toHaveText(
        "Select your filter criteria and click 'apply' to see results."
      );
    });

    it('renders an empty message on the NxTableBody if there are no components to display', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: [],
          sortFields: ['-score'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        body = table.find(NxTableBody),
        emptyMessageRow = body.find(NxTableRow);

      expect(emptyMessageRow).toHaveText(
        'No data available given the applied filters and permissions.'
      );
    });

    it('renders a row with an error message when an error is present with the appropriate retry handler', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: [],
          sortFields: ['-score'],
          error: 'error while retrieving results',
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        body = table.find(NxTableBody),
        errorRow = body.find(NxLoadError);

      expect(body).toHaveProp(
        'error',
        dashboardComponentProps.componentResults.error
      );
      expect(body).toHaveProp('retryHandler', minimalProps.reload);

      // to avoid the text added by the internal error components, we'll find the first span for the error message
      expect(errorRow.find('span').at(0)).toHaveText(
        'An error occurred loading data. error while retrieving results'
      );

      errorRow.find(NxButton).simulate('click');
      expect(minimalProps.reload).toHaveBeenCalled();
    });
  });

  describe('Column sorting', function () {
    const componentsToDisplay = [
      {
        hash: 'hash1',
        derivedComponentName: 'componentNameFromCoordinates',
        affectedApplications: 9001,
        score: 94784,
        scoreCritical: 300,
        scoreSevere: 20,
        scoreModerate: 10,
        scoreLow: 35,
      },
      {
        hash: 'hash2',
        derivedComponentName: 'componentNameFromCoordinates',
        affectedApplications: 657,
        score: 3249,
        scoreCritical: 2,
        scoreSevere: 1,
        scoreModerate: 1,
        scoreLow: 1,
      },
    ];

    const defaultPropsForSortChecks = {
      componentResults: {
        results: componentsToDisplay,
        sortFields: ['-score'],
      },
    };

    it('identifies default direction of sorting for the columns on render', function () {
      const dashboardComponentTable = getShallowComponent(
          defaultPropsForSortChecks
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        headers = headerRow.find(NxTableCell);

      expect(headers.at(0)).toHaveProp('sortDir', null);
      expect(headers.at(1)).toHaveProp('sortDir', null);

      // This column represents the threat score, which is the default sorting
      expect(headers.at(2)).toHaveProp('sortDir', 'desc');

      expect(headers.at(3)).toHaveProp('sortDir', null);
      expect(headers.at(4)).toHaveProp('sortDir', null);
      expect(headers.at(5)).toHaveProp('sortDir', null);
      expect(headers.at(6)).toHaveProp('sortDir', null);
    });

    it('calls sortComponents with derivedComponentName if clicked none to asc', function () {
      const dashboardComponentTable = getMountedComponent(
          defaultPropsForSortChecks
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        derivedComponentNameHeader = headerRow.find(NxTableCell).at(0);

      expect(derivedComponentNameHeader).toHaveProp('sortDir', null);
      derivedComponentNameHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        'derivedComponentName',
      ]);
    });

    it('calls sortComponents with -derivedComponentName if clicked asc to desc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['derivedComponentName'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        derivedComponentNameHeader = headerRow.find(NxTableCell).at(0);

      expect(derivedComponentNameHeader).toHaveProp('sortDir', 'asc');
      derivedComponentNameHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        '-derivedComponentName',
      ]);
    });

    it('calls sortComponents with derivedComponentName if clicked desc to asc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['-derivedComponentName'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        derivedComponentNameHeader = headerRow.find(NxTableCell).at(0);

      expect(derivedComponentNameHeader).toHaveProp('sortDir', 'desc');
      derivedComponentNameHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        'derivedComponentName',
      ]);
    });

    it('calls sortComponents with -affectedApplications if clicked none to desc', function () {
      const dashboardComponentTable = getMountedComponent(
          defaultPropsForSortChecks
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        affectedApplicationsHeader = headerRow.find(NxTableCell).at(1);

      expect(affectedApplicationsHeader).toHaveProp('sortDir', null);
      affectedApplicationsHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        '-affectedApplications',
      ]);
    });

    it('calls sortComponents with affectedApplications if clicked desc to asc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['-affectedApplications'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        affectedApplicationsHeader = headerRow.find(NxTableCell).at(1);

      expect(affectedApplicationsHeader).toHaveProp('sortDir', 'desc');
      affectedApplicationsHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        'affectedApplications',
      ]);
    });

    it('calls sortComponents with -affectedApplications if clicked asc to desc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['affectedApplications'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        affectedApplicationsHeader = headerRow.find(NxTableCell).at(1);

      expect(affectedApplicationsHeader).toHaveProp('sortDir', 'asc');
      affectedApplicationsHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        '-affectedApplications',
      ]);
    });

    it('calls sortComponents with -score if clicked none to desc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['affectedApplications'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(2);

      expect(totalRiskHeader).toHaveProp('sortDir', null);
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith(['-score']);
    });

    it('calls sortComponents with score if clicked desc to asc', function () {
      const dashboardComponentProps = defaultPropsForSortChecks;
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(2);

      expect(totalRiskHeader).toHaveProp('sortDir', 'desc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith(['score']);
    });

    it('calls sortComponents with -score if clicked asc to desc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['score'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(2);

      expect(totalRiskHeader).toHaveProp('sortDir', 'asc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith(['-score']);
    });

    it('calls sortComponents with -scoreCritical if clicked none to desc', function () {
      const dashboardComponentTable = getMountedComponent(
          defaultPropsForSortChecks
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(3);

      expect(totalRiskHeader).toHaveProp('sortDir', null);
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        '-scoreCritical',
      ]);
    });

    it('calls sortComponents with scoreCritical if clicked desc to asc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['-scoreCritical'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(3);

      expect(totalRiskHeader).toHaveProp('sortDir', 'desc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        'scoreCritical',
      ]);
    });

    it('calls sortComponents with -scoreCritical if clicked asc to desc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['scoreCritical'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(3);

      expect(totalRiskHeader).toHaveProp('sortDir', 'asc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        '-scoreCritical',
      ]);
    });

    it('calls sortComponents with -scoreSevere if clicked none to desc', function () {
      const dashboardComponentTable = getMountedComponent(
          defaultPropsForSortChecks
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(4);

      expect(totalRiskHeader).toHaveProp('sortDir', null);
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        '-scoreSevere',
      ]);
    });

    it('calls sortComponents with scoreSevere if clicked desc to asc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['-scoreSevere'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(4);

      expect(totalRiskHeader).toHaveProp('sortDir', 'desc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith(['scoreSevere']);
    });

    it('calls sortComponents with -scoreSevere if clicked asc to desc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['scoreSevere'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(4);

      expect(totalRiskHeader).toHaveProp('sortDir', 'asc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        '-scoreSevere',
      ]);
    });

    it('calls sortComponents with -scoreModerate if clicked none to desc', function () {
      const dashboardComponentTable = getMountedComponent(
          defaultPropsForSortChecks
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(5);

      expect(totalRiskHeader).toHaveProp('sortDir', null);
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        '-scoreModerate',
      ]);
    });

    it('calls sortComponents with scoreModerate if clicked desc to asc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['-scoreModerate'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(5);

      expect(totalRiskHeader).toHaveProp('sortDir', 'desc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        'scoreModerate',
      ]);
    });

    it('calls sortComponents with -scoreModerate if clicked asc to desc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['scoreModerate'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(5);

      expect(totalRiskHeader).toHaveProp('sortDir', 'asc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith([
        '-scoreModerate',
      ]);
    });

    it('calls sortComponents with -scoreLow if clicked none to desc', function () {
      const dashboardComponentTable = getMountedComponent(
          defaultPropsForSortChecks
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(6);

      expect(totalRiskHeader).toHaveProp('sortDir', null);
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith(['-scoreLow']);
    });

    it('calls sortComponents with scoreLow if clicked desc to asc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['-scoreLow'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(6);

      expect(totalRiskHeader).toHaveProp('sortDir', 'desc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith(['scoreLow']);
    });

    it('calls sortComponents with -scoreLow if clicked asc to desc', function () {
      const dashboardComponentProps = {
        componentResults: {
          results: componentsToDisplay,
          sortFields: ['scoreLow'],
        },
      };
      const dashboardComponentTable = getMountedComponent(
          dashboardComponentProps
        ),
        table = dashboardComponentTable.find(NxTable),
        head = table.find(NxTableHead),
        headerRow = head.find(NxTableRow),
        totalRiskHeader = headerRow.find(NxTableCell).at(6);

      expect(totalRiskHeader).toHaveProp('sortDir', 'asc');
      totalRiskHeader.simulate('click');
      expect(minimalProps.sortComponents).toHaveBeenCalledWith(['-scoreLow']);
    });
  });
});
