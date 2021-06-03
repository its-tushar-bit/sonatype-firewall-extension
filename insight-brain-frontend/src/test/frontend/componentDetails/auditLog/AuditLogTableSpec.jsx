/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../enzymeUtils';
import AuditLogTable, { AUDIT_DATE_FORMAT } from '../../../../main/frontend/componentDetails/auditLog/AuditLogTable';
import * as dateUtils from '../../../../main/frontend/util/dateUtils';

describe('AuditLogTable', function () {
  let minimalProps, getShallow, auditRecords;

  beforeEach(function () {
    spyOn(dateUtils, 'formatDate').and.callThrough();

    auditRecords = [
      {
        hash: 'hash1',
        time: 123456,
        user: 'user',
        action: 'action1',
        detail: 'detail1',
        comment: 'comment1',
      },
      {
        hash: 'hash2',
        time: 123457,
        user: 'user',
        action: 'action2',
        detail: 'detail2',
        comment: 'comment2',
      },
    ];
    minimalProps = {
      auditRecords: [],
      isLoading: false,
      sortAuditLog: jasmine.createSpy('sortAuditLog'),
      loadAuditLogForComponent: jasmine.createSpy('loadAuditLogForComponent'),
    };

    getShallow = enzymeUtils.getShallowComponent(AuditLogTable, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallow()).toExist();
  });

  describe('Table headers', () => {
    function getTableHeaders(props) {
      const component = getShallow(props);
      const table = component.find(NxTable);
      const tHeader = table.find(NxTableHead);
      const headerRow = tHeader.find(NxTableRow);
      const headers = headerRow.find(NxTableCell);

      return headers;
    }

    it('renders an NxTable with headers', () => {
      const headers = getTableHeaders();

      expect(headers.length).toEqual(5);
      expect(headers.at(0)).toHaveProp('children', 'Date');
      expect(headers.at(1)).toHaveProp('children', 'User');
      expect(headers.at(2)).toHaveProp('children', 'Action');
      expect(headers.at(3)).toHaveProp('children', 'Detail');
      expect(headers.at(4)).toHaveProp('children', 'Comment');
    });

    it('renders a sort direction based on the given appliedSort prop', () => {
      let headers;

      headers = getTableHeaders({ auditRecords, appliedSort: '-time' });
      let timeHeader = headers.at(0);
      expect(timeHeader).toHaveProp('sortDir', 'desc');
      expect(headers.at(1)).toHaveProp('sortDir', null);

      headers = getTableHeaders({ auditRecords, appliedSort: 'time' });
      timeHeader = headers.at(0);
      expect(timeHeader).toHaveProp('sortDir', 'asc');
      expect(headers.at(1)).toHaveProp('sortDir', null);

      headers = getTableHeaders({ auditRecords, appliedSort: 'user' });
      let userHeader = headers.at(1);
      expect(userHeader).toHaveProp('sortDir', 'asc');
      expect(headers.at(0)).toHaveProp('sortDir', null);

      headers = getTableHeaders({ auditRecords, appliedSort: '-user' });
      userHeader = headers.at(1);
      expect(userHeader).toHaveProp('sortDir', 'desc');
      expect(headers.at(0)).toHaveProp('sortDir', null);

      headers = getTableHeaders({ auditRecords, appliedSort: 'action' });
      let actionHeader = headers.at(2);
      expect(actionHeader).toHaveProp('sortDir', 'asc');
      expect(headers.at(0)).toHaveProp('sortDir', null);

      headers = getTableHeaders({ auditRecords, appliedSort: '-action' });
      actionHeader = headers.at(2);
      expect(actionHeader).toHaveProp('sortDir', 'desc');
      expect(headers.at(0)).toHaveProp('sortDir', null);

      headers = getTableHeaders({ auditRecords, appliedSort: 'detail' });
      let detailHeader = headers.at(3);
      expect(detailHeader).toHaveProp('sortDir', 'asc');
      expect(headers.at(0)).toHaveProp('sortDir', null);

      headers = getTableHeaders({ auditRecords, appliedSort: '-detail' });
      detailHeader = headers.at(3);
      expect(detailHeader).toHaveProp('sortDir', 'desc');
      expect(headers.at(0)).toHaveProp('sortDir', null);

      headers = getTableHeaders({ auditRecords, appliedSort: 'comment' });
      let commentHeader = headers.at(4);
      expect(commentHeader).toHaveProp('sortDir', 'asc');
      expect(headers.at(0)).toHaveProp('sortDir', null);

      headers = getTableHeaders({ auditRecords, appliedSort: '-comment' });
      commentHeader = headers.at(4);
      expect(commentHeader).toHaveProp('sortDir', 'desc');
      expect(headers.at(0)).toHaveProp('sortDir', null);
    });

    it('calls sortAuditLog with a give direction depending on the column', () => {
      let headers, chosenHeader;

      headers = getTableHeaders({ auditRecords, appliedSort: '-time' });
      chosenHeader = headers.at(0);
      chosenHeader.simulate('click');
      expect(minimalProps.sortAuditLog).toHaveBeenCalledWith('time');

      headers = getTableHeaders({ auditRecords, appliedSort: 'time' });
      chosenHeader = headers.at(0);
      chosenHeader.simulate('click');
      expect(minimalProps.sortAuditLog).toHaveBeenCalledWith('-time');

      headers = getTableHeaders({ auditRecords, appliedSort: '-time' });
      chosenHeader = headers.at(1);
      chosenHeader.simulate('click');
      expect(minimalProps.sortAuditLog).toHaveBeenCalledWith('user');

      headers = getTableHeaders({ auditRecords, appliedSort: 'user' });
      chosenHeader = headers.at(1);
      chosenHeader.simulate('click');
      expect(minimalProps.sortAuditLog).toHaveBeenCalledWith('-user');

      headers = getTableHeaders({ auditRecords, appliedSort: 'user' });
      chosenHeader = headers.at(2);
      chosenHeader.simulate('click');
      expect(minimalProps.sortAuditLog).toHaveBeenCalledWith('action');

      headers = getTableHeaders({ auditRecords, appliedSort: 'action' });
      chosenHeader = headers.at(2);
      chosenHeader.simulate('click');
      expect(minimalProps.sortAuditLog).toHaveBeenCalledWith('-action');

      headers = getTableHeaders({ auditRecords, appliedSort: 'user' });
      chosenHeader = headers.at(3);
      chosenHeader.simulate('click');
      expect(minimalProps.sortAuditLog).toHaveBeenCalledWith('detail');

      headers = getTableHeaders({ auditRecords, appliedSort: 'detail' });
      chosenHeader = headers.at(3);
      chosenHeader.simulate('click');
      expect(minimalProps.sortAuditLog).toHaveBeenCalledWith('-detail');

      headers = getTableHeaders({ auditRecords, appliedSort: 'detail' });
      chosenHeader = headers.at(4);
      chosenHeader.simulate('click');
      expect(minimalProps.sortAuditLog).toHaveBeenCalledWith('comment');

      headers = getTableHeaders({ auditRecords, appliedSort: 'comment' });
      chosenHeader = headers.at(4);
      chosenHeader.simulate('click');
      expect(minimalProps.sortAuditLog).toHaveBeenCalledWith('-comment');
    });
  });

  describe('Table body', () => {
    it('renders an NxTable with empty message if auditRecords are empty', () => {
      const component = getShallow();
      const body = component.find(NxTableBody).dive();
      const tRow = body.find(NxTableRow).dive();
      const tCell = tRow.find(NxTableCell);
      expect(tCell.dive()).toHaveText('No changes were found for this component.');
    });

    it('sets isLoading in the table body', () => {
      let component = getShallow({ isLoading: true });
      let body = component.find(NxTableBody);
      expect(body).toHaveProp('isLoading', true);

      component = getShallow({ isLoading: false });
      body = component.find(NxTableBody);
      expect(body).toHaveProp('isLoading', false);
    });

    it('sets the error prop on the table body', () => {
      let component = getShallow({ error: 'some err' });
      let body = component.find(NxTableBody);
      expect(body).toHaveProp('error', 'some err');

      component = getShallow();
      body = component.find(NxTableBody).dive();
      expect(body).not.toHaveProp('error');
    });

    it('renders an NxRow per each auditRecord', () => {
      const component = getShallow({ auditRecords });
      const table = component.find(NxTable);
      const tBody = table.find(NxTableBody);
      const rows = tBody.find(NxTableRow);

      expect(rows.length).toEqual(2);
      const row1 = rows.at(0);
      const row2 = rows.at(1);

      const row1Cells = row1.dive().find(NxTableCell);
      expect(row1Cells.length).toEqual(5);
      expect(dateUtils.formatDate).toHaveBeenCalledWith(123456, AUDIT_DATE_FORMAT);
      expect(row1Cells.at(1).dive()).toHaveText('user');
      expect(row1Cells.at(2).dive()).toHaveText('action1');
      expect(row1Cells.at(3).dive()).toHaveText('detail1');
      expect(row1Cells.at(4).dive()).toHaveText('comment1');

      const row2Cells = row2.dive().find(NxTableCell);
      expect(row2Cells.length).toEqual(5);
      expect(dateUtils.formatDate).toHaveBeenCalledWith(123457, AUDIT_DATE_FORMAT);
      expect(row2Cells.at(1).dive()).toHaveText('user');
      expect(row2Cells.at(2).dive()).toHaveText('action2');
      expect(row2Cells.at(3).dive()).toHaveText('detail2');
      expect(row2Cells.at(4).dive()).toHaveText('comment2');
    });
  });
});
