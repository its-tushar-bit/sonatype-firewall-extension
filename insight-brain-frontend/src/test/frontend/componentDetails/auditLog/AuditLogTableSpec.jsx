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
    };

    getShallow = enzymeUtils.getShallowComponent(AuditLogTable, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallow()).toExist();
  });

  it('renders an NxTable with headers', () => {
    const component = getShallow();
    const table = component.find(NxTable);
    const tHeader = table.find(NxTableHead);
    const headerRow = tHeader.find(NxTableRow);
    const headers = headerRow.find(NxTableCell);

    expect(headers.length).toEqual(5);
    expect(headers.at(0).dive()).toHaveText('Date');
    expect(headers.at(1).dive()).toHaveText('User');
    expect(headers.at(2).dive()).toHaveText('Action');
    expect(headers.at(3).dive()).toHaveText('Detail');
    expect(headers.at(4).dive()).toHaveText('Comment');
  });

  it('renders an NxTable with empty message if auditRecords are empty', () => {
    const component = getShallow();
    const body = component.find(NxTableBody).dive();
    const tRow = body.find(NxTableRow).dive();
    const tCell = tRow.find(NxTableCell);
    expect(tCell.dive()).toHaveText('No changes were found for this component.');
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
