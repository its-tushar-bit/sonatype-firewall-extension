/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import AuditLogTable from '../../../../main/frontend/componentDetails/auditLog/AuditLogTable';
import AuditLog from '../../../../main/frontend/componentDetails/auditLog/AuditLog';
import { NxLoadWrapper } from '@sonatype/react-shared-components';

describe('AuditLog', function () {
  let minimalProps, getShallow;

  beforeEach(function () {
    minimalProps = {
      isLoadingComponentDetails: true,
      componentDetailsLoadError: null,
      loadComponentDetails: () => {},
      auditRecords: [],
      isLoading: false,
      error: null,
      appliedSort: null,
      loadAuditLogForComponent: () => {},
      sortAuditLog: () => {},
    };

    getShallow = enzymeUtils.getShallowComponent(AuditLog, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallow()).toExist();
  });

  it('renders a loading indicator if component details are loading', () => {
    const component = getShallow();
    const loadWrapper = component.find(NxLoadWrapper);

    expect(loadWrapper).toHaveProp('loading', minimalProps.isLoadingComponentDetails);
    expect(loadWrapper).toHaveProp('error', minimalProps.componentDetailsLoadError);
    expect(loadWrapper).toHaveProp('retryHandler', minimalProps.loadComponentDetails);
  });

  it('renders an AuditLogTable component with all props passed', () => {
    const getTable = (el) => {
      return el.find(NxLoadWrapper).dive().find(AuditLogTable);
    };

    let auditLogTable;
    auditLogTable = getTable(getShallow({ isLoadingComponentDetails: false }));

    expect(auditLogTable).toExist();
    expect(auditLogTable).toHaveProp('auditRecords', minimalProps.auditRecords);
    expect(auditLogTable).toHaveProp('isLoading', false);
    expect(auditLogTable).toHaveProp('error', null);
    expect(auditLogTable).toHaveProp('appliedSort', null);
    expect(auditLogTable).toHaveProp('sortAuditLog', minimalProps.sortAuditLog);

    auditLogTable = getTable(getShallow({ isLoadingComponentDetails: false, isLoading: true }));
    expect(auditLogTable).toHaveProp('isLoading', true);

    auditLogTable = getTable(getShallow({ isLoadingComponentDetails: false, error: 'some error' }));
    expect(auditLogTable).toHaveProp('error', 'some error');

    auditLogTable = getTable(getShallow({ isLoadingComponentDetails: false, appliedSort: '-time' }));
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
    auditLogTable = getTable(getShallow({ isLoadingComponentDetails: false, auditRecords }));
    expect(auditLogTable).toHaveProp('auditRecords', auditRecords);
  });
});
