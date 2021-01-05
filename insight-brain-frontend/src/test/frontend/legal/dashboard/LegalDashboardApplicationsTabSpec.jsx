/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxTable } from '@sonatype/react-shared-components';
import LegalDashboardApplicationsTab from '../../../../main/frontend/legal/dashboard/LegalDashboardApplicationsTab';
import LegalDashboardApplicationRow from '../../../../main/frontend/legal/dashboard/LegalDashboardApplicationRow';

describe('LegalDashboardApplicationsTab component', function() {

  let getShallowComponent;

  const minimalProps = {
    applications: ['row1', 'row2']
  };

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardApplicationsTab, minimalProps);
  });

  it('renders a table', function() {
    const wrapper = getShallowComponent();
    let table = wrapper.find(NxTable);
    expect(table).toExist();
    expect(table).toHaveClassName('legal-dashboard-table');
  });

  it('renders LegalDashboardApplicationRow components for each application passed in', function() {
    const wrapper = getShallowComponent();
    let table = wrapper.find(NxTable);
    let rows = table.find(LegalDashboardApplicationRow);
    expect(rows).toExist();
    expect(rows.length).toEqual(2);
    expect(rows.at(0)).toHaveProp('row', 'row1');
    expect(rows.at(1)).toHaveProp('row', 'row2');
  });
});
