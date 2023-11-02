/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import 'jest-enzyme';
import { shallow } from 'enzyme';

import * as enzymeUtils from '../enzymeUtils';
import { faTrashAlt } from '@fortawesome/free-solid-svg-icons/index';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTextLink,
} from '@sonatype/react-shared-components';
import moment from 'moment';
import ComponentDisplay from '../../../main/frontend/ComponentDisplay/ReactComponentDisplay';
import { waiverMatcherStrategy } from 'MainRoot/util/waiverUtils';
import ListWaiversTable from 'MainRoot/waivers/ListWaiversTable';

describe('ListWaiversTable', function () {
  let minimalProps, violationDetailsMock, getShallowComponent, setWaiverToDeleteSpy;

  beforeEach(function () {
    setWaiverToDeleteSpy = jest.fn();

    violationDetailsMock = {
      filename: 'filename',
      constraintViolations: [
        {
          constraintName: 'constraint name',
          reasons: [
            {
              reason: 'reason',
              reference: {
                value: 'CVE-67890',
              },
            },
          ],
        },
      ],
      policyName: 'policyName',
      policyViolationId: 'policyViolationId',
      threatLevel: 5,
    };

    minimalProps = {
      loading: false,
      setWaiverToDelete: setWaiverToDeleteSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ListWaiversTable, minimalProps);
  });

  it('renders an NxTable with header', function () {
    const component = getShallowComponent();
    const table = component.find(NxTable);
    expect(table).toExist();
    const tableHeaderRow = table.find(NxTableHead).find(NxTableRow);
    expect(tableHeaderRow).toExist();
    const tableHeaderCells = tableHeaderRow.find(NxTableCell);
    expect(tableHeaderCells.length).toBe(7);
  });

  it('sets the emptyMessage on NxTableBody including a link to the waivers help docs', function () {
    const component = getShallowComponent();
    const tableBody = component.find(NxTableBody);

    expect(tableBody).toHaveProp('emptyMessage');

    const emptyMessageShallowRender = shallow(tableBody.prop('emptyMessage'));
    expect(emptyMessageShallowRender.find(NxTextLink)).toHaveProp(
      'href',
      'http://links.sonatype.com/products/nxiq/doc/waivers'
    );
  });

  it('sets the isLoading on NxTableBody', function () {
    const component = getShallowComponent({
      loadingApplicableWaivers: true,
    });
    const tableBody = component.find(NxTableBody);
    expect(tableBody).toHaveProp('isLoading', true);
  });

  it('sets the error on NxTableBody', function () {
    const component = getShallowComponent({
      loadApplicableWaiversError: {
        response: {
          data: 'load waivers error',
        },
      },
    });
    const tableBody = component.find(NxTableBody);
    expect(tableBody).toHaveProp('error', 'load waivers error');
  });

  it('sets the retryHandler on NxTableBody', function () {
    const reloadApplicableWaiversSpy = jest.fn();
    const component = getShallowComponent({
      reloadApplicableWaivers: reloadApplicableWaiversSpy,
    });
    const tableBody = component.find(NxTableBody);
    expect(tableBody).toHaveProp('retryHandler', reloadApplicableWaiversSpy);
  });

  const assertDeleteWaiverBtn = (tableCell, waiver) => {
    const btn = tableCell.childAt(0);
    expect(btn).toMatchSelector(NxButton);
    expect(btn.prop('onClick')).toEqual(expect.any(Function));

    btn.simulate('click');
    expect(setWaiverToDeleteSpy).toHaveBeenCalledWith(waiver);

    const icon = btn.find(NxFontAwesomeIcon);
    expect(icon.prop('icon')).toEqual(faTrashAlt);
  };

  const assertWaiverTableRow = (
    tableRow,
    createdBy,
    dateCreated,
    scope,
    components,
    expiration,
    comments,
    isExpired,
    waiver
  ) => {
    if (isExpired) {
      expect(tableRow).toHaveClassName('list-waivers-row--expired');
    } else {
      expect(tableRow).not.toHaveClassName('list-waivers-row--expired');
    }

    const tableCells = tableRow.children();
    expect(tableCells.length).toBe(7);
    expect(tableCells.at(0).childAt(0).text()).toBe(dateCreated);
    expect(tableCells.at(2).childAt(0).text()).toBe(scope);

    if (components) {
      let componentCell = tableCells.at(3).childAt(0).find(ComponentDisplay);
      expect(componentCell).toHaveProp('component', components);
    } else {
      expect(tableCells.at(3).childAt(0).text()).toBe('All');
    }

    expect(tableCells.at(4).childAt(0).text()).toBe(expiration);
    expect(tableCells.at(1).childAt(0).text()).toBe(createdBy);
    expect(tableCells.at(5).childAt(0).text()).toBe(comments);
    assertDeleteWaiverBtn(tableCells.at(6).childAt(0), waiver);
  };

  it('renders an NxTableBody with active and expired waivers sorted by createTime desc', function () {
    const baselineDate = moment('2020-10-05T19:56:17.509+0000', 'YYYY-MM-DDThh:mm:ss.SSS+0000');
    moment.now = () => baselineDate;

    const activeWaivers = [
      {
        policyWaiverId: '1',
        comment: 'comment1',
        createTime: baselineDate,
        expiryTime: baselineDate.clone().add(7, 'days'),
        hash: null,
        scopeOwnerType: 'root_organization',
        scopeOwnerName: 'Root Organization',
        creatorName: 'User 1',
        matcherStrategy: waiverMatcherStrategy.ALL_COMPONENTS,
      },
      {
        policyWaiverId: '2',
        comment: 'comment2',
        createTime: baselineDate.clone().add(2, 'days'),
        hash: '1e48256a2341047e7d72',
        scopeOwnerType: 'application',
        scopeOwnerName: 'test',
        matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
      },
    ];
    const expiredWaivers = [
      {
        policyWaiverId: '3',
        comment: 'comment3',
        createTime: baselineDate.clone().add(10, 'days'),
        expiryTime: baselineDate.clone().subtract(1, 'year'),
        hash: '1e48256a2341047e7d72',
        scopeOwnerType: 'root_organization',
        scopeOwnerName: 'Root Organization',
        creatorName: 'User 1',
        matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
      },
      {
        comment: 'comment4',
        policyWaiverId: '4',
        createTime: baselineDate.clone().add(13, 'days'),
        expiryTime: baselineDate.clone().subtract(1, 'month'),
        hash: '1e48256a2341047e7d72',
        scopeOwnerType: 'organization',
        scopeOwnerName: 'suborg',
        creatorName: 'User 2',
        matcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
      },
    ];
    const props = {
      activeWaivers,
      expiredWaivers,
      violationDetails: violationDetailsMock,
    };

    const component = getShallowComponent(props);
    const tableBody = component.find(NxTable).find(NxTableBody);
    expect(tableBody).not.toHaveClassName('nx-cell--empty');
    const tableRows = tableBody.find(NxTableRow);
    expect(tableRows.length).toBe(4);
    assertWaiverTableRow(
      tableRows.at(0),
      '- -',
      '2020-10-07',
      'Application - test',
      violationDetailsMock,
      'Does not expire',
      'comment2',
      false,
      activeWaivers[1]
    );
    assertWaiverTableRow(
      tableRows.at(1),
      'User 1',
      '2020-10-05',
      'Root Organization',
      null,
      'in 7 days',
      'comment1',
      false,
      activeWaivers[0]
    );
    assertWaiverTableRow(
      tableRows.at(2),
      'User 2',
      '2020-10-18',
      'Organization - suborg',
      violationDetailsMock,
      'a month ago',
      'comment4',
      true,
      expiredWaivers[1]
    );
    assertWaiverTableRow(
      tableRows.at(3),
      'User 1',
      '2020-10-15',
      'Root Organization',
      violationDetailsMock,
      'a year ago',
      'comment3',
      true,
      expiredWaivers[0]
    );
  });
});
