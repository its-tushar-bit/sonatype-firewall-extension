/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';
import moment from 'moment';
import ComponentDisplay from '../../../main/frontend/ComponentDisplay/ReactComponentDisplay';

describe('ListWaiversTable', function() {
  let minimalProps,
      ListWaiversTable,
      violationDetailsMock,
      getShallowComponent;

  beforeEach(function() {
    ListWaiversTable = require('inject-loader!../../../main/frontend/waivers/ListWaiversTable')().default;

    violationDetailsMock = {
      filename: 'filename',
      constraintViolations: [{
        constraintName: 'constraint name',
        reasons: [{
          reason: 'reason',
          reference: {
            value: 'CVE-67890'
          }
        }]
      }],
      policyName: 'policyName',
      policyViolationId: 'policyViolationId',
      threatLevel: 5
    };

    minimalProps = {
      loading: false
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ListWaiversTable, minimalProps);
  });

  it('renders an NxTable with header', function() {
    const component = getShallowComponent();
    const table = component.find(NxTable);
    expect(table).toExist();
    const tableHeaderRow = table.find(NxTableHead).find(NxTableRow);
    expect(tableHeaderRow).toExist();
    const tableHeaderCells = tableHeaderRow.find(NxTableCell);
    expect(tableHeaderCells.length).toBe(5);
  });

  it('renders an empty NxTableBody if there are no active or expired waivers', function() {
    const component = getShallowComponent();
    const table = component.find(NxTable);
    expect(table).toExist();
    const tableBody = table.find(NxTableBody);
    const emptyCell = tableBody.find(NxTableRow).find(NxTableCell);
    expect(emptyCell).toHaveClassName('nx-cell--empty');
    expect(emptyCell.childAt(0).text())
        .toBe('You don\'t have any waivers: to learn more about waivers you can check our ');
    const waiverLinkHtml =
        '<a href="https://help.sonatype.com/iqserver/reporting/application-composition-report/waivers">' +
        'help documentation.</a>';
    expect(emptyCell.childAt(1).html()).toBe(waiverLinkHtml);
  });

  const assertWaiverTableRow = (tableRow, dateCreated, scope, components, expiration, comments, isExpired) => {
    if (isExpired) {
      expect(tableRow).toHaveClassName('list-waivers-row--expired');
    }
    else {
      expect(tableRow).not.toHaveClassName('list-waivers-row--expired');
    }

    const tableCells = tableRow.children();
    expect(tableCells.length).toBe(5);
    expect(tableCells.at(0).childAt(0).text()).toBe(dateCreated);
    expect(tableCells.at(1).childAt(0).text()).toBe(scope);

    if (components) {
      let componentCell = tableCells.at(2).childAt(0).find(ComponentDisplay);
      expect(componentCell).toHaveProp('component', components);
    }
    else {
      expect(tableCells.at(2).childAt(0).text()).toBe('All');
    }

    expect(tableCells.at(3).childAt(0).text()).toBe(expiration);
    expect(tableCells.at(4).childAt(0).text()).toBe(comments);
  };

  it('renders an NxTableBody with active and expired waivers sorted by createTime desc', function() {
    const baselineDate = moment('2020-10-05T19:56:17.509+0000', 'YYYY-MM-DDThh:mm:ss.SSS+0000');
    moment.now = () => baselineDate;

    const props = {
      activeWaivers: [{
        policyWaiverId: '1',
        comment: 'comment1',
        createTime: baselineDate,
        expiryTime: baselineDate.clone().add(7, 'days'),
        hash: null,
        scopeOwnerType: 'root_organization',
        scopeOwnerName: 'Root Organization'
      }, {
        policyWaiverId: '2',
        comment: 'comment2',
        createTime: baselineDate.clone().add(2, 'days'),
        hash: '1e48256a2341047e7d72',
        scopeOwnerType: 'application',
        scopeOwnerName: 'test'
      }],
      expiredWaivers: [{
        policyWaiverId: '3',
        comment: 'comment3',
        createTime: baselineDate.clone().add(10, 'days'),
        expiryTime: baselineDate.clone().subtract(1, 'year'),
        hash: '1e48256a2341047e7d72',
        scopeOwnerType: 'root_organization',
        scopeOwnerName: 'Root Organization'
      }, {
        comment: 'comment4',
        policyWaiverId: '4',
        createTime: baselineDate.clone().add(13, 'days'),
        expiryTime: baselineDate.clone().subtract(1, 'month'),
        hash: '1e48256a2341047e7d72',
        scopeOwnerType: 'organization',
        scopeOwnerName: 'suborg'
      }],
      violationDetails: violationDetailsMock
    };

    const component = getShallowComponent(props);
    const tableBody = component.find(NxTable).find(NxTableBody);
    expect(tableBody).not.toHaveClassName('nx-cell--empty');
    const tableRows = tableBody.find(NxTableRow);
    expect(tableRows.length).toBe(4);
    assertWaiverTableRow(tableRows.at(0), '10/07/2020', 'Application - test', violationDetailsMock, 'Does not expire',
        'comment2', false);
    assertWaiverTableRow(tableRows.at(1), '10/05/2020', 'Root Organization', null, 'in 7 days', 'comment1', false);
    assertWaiverTableRow(tableRows.at(2), '10/18/2020', 'Organization - suborg', violationDetailsMock, 'a month ago',
        'comment4', true);
    assertWaiverTableRow(tableRows.at(3), '10/15/2020', 'Root Organization', violationDetailsMock, 'a year ago',
        'comment3', true);
  });
});
