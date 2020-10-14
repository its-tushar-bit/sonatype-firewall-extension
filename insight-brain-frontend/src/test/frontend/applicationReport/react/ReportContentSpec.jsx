/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ReportContent from '../../../../main/frontend/applicationReport/react/ReportContent';
import {NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow} from '@sonatype/react-shared-components';

describe('ReportContent component', function() {
  let getShallowComponent;

  beforeEach(function() {

    const minimalProps = {
      selectedReport: {
        displayedEntries: []
      }
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ReportContent, minimalProps);
  });

  it('renders a div', function() {
    const shallowComponent = getShallowComponent();
    expect(shallowComponent).toMatchSelector('div');
    expect(shallowComponent).toMatchSelector('.nx-tile-content.nx-scrollable.nx-scrollable--report-table');
  });

  it('renders table header and body', function() {
    const shallowComponent = getShallowComponent();
    const table = shallowComponent.find(NxTable);
    const header = shallowComponent.find(NxTableHead);
    const body = shallowComponent.find(NxTableBody);
    expect(table).toExist();
    expect(header).toExist();
    expect(body).toExist();
  });

  it('renders a single row with a single nx-cell--empty when there are no display entries', function() {
    const emptyCell = getShallowComponent().find('.nx-cell--empty');
    const span = getShallowComponent().find('.nx-cell--empty').find('span');
    expect(emptyCell).toExist();
    expect(span).toHaveText('No Results');
  });

  it('render the table header with sordir desc', function() {

    const props = {
      selectedReport: {
        displayedEntries: [
          {
            derivedComponentName: 'Component A',
            policyName: 'None',
            policyThreatLevel: 0
          },
          {
            derivedComponentName: 'Component B',
            policyName: 'Security-High',
            policyThreatLevel: 9
          }
        ]
      },
      sortConfiguration: {
        key: 'policyThreatLevel',
        sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
        dir: 'desc'
      }
    };

    const shallowComponent = getShallowComponent(props),
        head = shallowComponent.find(NxTableHead),
        rows = head.find(NxTableRow),
        firstRowTds = rows.at(0).find(NxTableCell);

    expect(firstRowTds.at(0)).toHaveProp('isSortable', true);
    expect(firstRowTds.at(0)).toHaveProp('sortDir', 'desc');
    expect(firstRowTds.at(1)).toHaveProp('isSortable', true);
    expect(firstRowTds.at(2)).toHaveProp('isSortable', true);
  });

  it('render the table header with sordir asc', function() {

    const props = {
      selectedReport: {
        displayedEntries: [
          {
            derivedComponentName: 'Component A',
            policyName: 'None',
            policyThreatLevel: 0
          },
          {
            derivedComponentName: 'Component B',
            policyName: 'Security-High',
            policyThreatLevel: 9
          }
        ]
      },
      sortConfiguration: {
        key: 'policyName',
        sortFields: ['policyName', '-policyThreatLevel', 'derivedComponentName'],
        dir: 'asc'
      }
    };

    const shallowComponent = getShallowComponent(props),
        head = shallowComponent.find(NxTableHead),
        rows = head.find(NxTableRow),
        firstRowTds = rows.at(0).find(NxTableCell);

    expect(firstRowTds.at(0)).toHaveProp('isSortable', true);
    expect(firstRowTds.at(1)).toHaveProp('isSortable', true);
    expect(firstRowTds.at(1)).toHaveProp('sortDir', 'asc');
    expect(firstRowTds.at(2)).toHaveProp('isSortable', true);
  });

  it('renders a ReportTableRow for each entry', function() {
    const props = {
          selectedReport: {
            displayedEntries: [
              {
                derivedComponentName: 'Component B',
                policyName: 'Security-Critical',
                policyThreatLevel: 9
              },
              {
                derivedComponentName: 'Component A',
                policyName: 'None',
                policyThreatLevel: 0
              }
            ]
          },
          sortConfiguration: {
            key: 'policyThreatLevel',
            sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
            dir: 'desc'
          }
        },
        shallowComponent = getShallowComponent(props),
        body = shallowComponent.find(NxTableBody),
        tableRow = body.find('ReportTableRow');

    expect(body).toExist();
    expect(tableRow).toExist();
    expect(tableRow.at(0)).toHaveProp('component', props.selectedReport.displayedEntries[0]);
    expect(tableRow.at(0)).toHaveProp('index', 0);
    expect(tableRow.at(1)).toHaveProp('component', props.selectedReport.displayedEntries[1]);
    expect(tableRow.at(1)).toHaveProp('index', 1);
  });
});
