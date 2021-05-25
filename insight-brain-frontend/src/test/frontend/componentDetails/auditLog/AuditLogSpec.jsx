/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import AuditLogTable from '../../../../main/frontend/componentDetails/auditLog/AuditLogTable';
import AuditLog from '../../../../main/frontend/componentDetails/auditLog/AuditLog';
import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';

describe('AuditLog', function () {
  let minimalProps, getShallow, getMounted, loadAuditLogSpy;

  beforeEach(function () {
    loadAuditLogSpy = jasmine.createSpy('loadAuditLogForComponent');
    minimalProps = {
      auditRecord: [],
      isLoading: false,
      error: null,
      loadAuditLogForComponent: loadAuditLogSpy,
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

  it('renders a LoadWrapper if loading prop is true', () => {
    const component = getShallow({ isLoading: true });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toExist();
    expect(loadWrapper).toHaveProp('loading', true);
    expect(loadWrapper).toHaveProp('error', null);
  });

  it('propagates an error to the LoadWrapper if present', () => {
    const component = getShallow({ isLoading: false, error: 'Some error' });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toExist();
    expect(loadWrapper).toHaveProp('loading', false);
    expect(loadWrapper).toHaveProp('error', 'Some error');
    expect(loadWrapper).toHaveProp('retryHandler', loadAuditLogSpy);
  });

  it('renders an AuditLogTable when the auditRecords are loaded', () => {
    const auditRecords = [
      {
        hash: 'hash',
        time: 12345,
        action: 'action',
        detail: 'detail',
        comment: 'comment',
      },
    ];
    const component = getMounted({ auditRecords });
    const table = component.find(AuditLogTable);

    expect(table).toExist();
    expect(table).toHaveProp('auditRecords', auditRecords);
  });
});
