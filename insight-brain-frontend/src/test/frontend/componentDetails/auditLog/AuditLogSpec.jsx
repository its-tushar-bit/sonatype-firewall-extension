/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import AuditLogTable from '../../../../main/frontend/componentDetails/auditLog/AuditLogTable';
import AuditLog from '../../../../main/frontend/componentDetails/auditLog/AuditLog';

describe('AuditLog', function () {
  let minimalProps, getShallow, getMounted, loadAuditLogSpy, sortAuditLogSpy;

  beforeEach(function () {
    loadAuditLogSpy = jasmine.createSpy('loadAuditLogForComponent');
    sortAuditLogSpy = jasmine.createSpy('sortAuditLog');
    minimalProps = {
      auditRecords: [],
      isLoading: false,
      error: null,
      appliedSort: null,
      loadAuditLogForComponent: loadAuditLogSpy,
      sortAuditLog: sortAuditLogSpy,
    };

    getShallow = enzymeUtils.getShallowComponent(AuditLog, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(AuditLog, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallow()).toExist();
  });

  it('calls loadAuditLogForComponent', () => {
    getMounted();
    expect(loadAuditLogSpy).toHaveBeenCalled();
  });

  it('renders an AuditLogTable component with all props passed', () => {
    let auditLogTable;
    auditLogTable = getShallow().find(AuditLogTable);

    expect(auditLogTable).toExist();
    expect(auditLogTable).toHaveProp('auditRecords', minimalProps.auditRecords);
    expect(auditLogTable).toHaveProp('isLoading', false);
    expect(auditLogTable).toHaveProp('error', null);
    expect(auditLogTable).toHaveProp('appliedSort', null);
    expect(auditLogTable).toHaveProp('sortAuditLog', minimalProps.sortAuditLog);

    auditLogTable = getShallow({ isLoading: true }).find(AuditLogTable);
    expect(auditLogTable).toHaveProp('isLoading', true);

    auditLogTable = getShallow({ error: 'some error' }).find(AuditLogTable);
    expect(auditLogTable).toHaveProp('error', 'some error');

    auditLogTable = getShallow({ appliedSort: '-time' }).find(AuditLogTable);
    expect(auditLogTable).toHaveProp('appliedSort', '-time');

    const auditRecords = [
      {
        hash: 'hash',
        time: 12345,
        action: 'action',
        detail: 'detail',
        comment: 'comment',
      },
    ];
    auditLogTable = getShallow({ auditRecords }).find(AuditLogTable);
    expect(auditLogTable).toHaveProp('auditRecords', auditRecords);
  });
});
