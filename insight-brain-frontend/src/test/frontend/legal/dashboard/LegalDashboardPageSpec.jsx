/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import React from 'react';
import LegalDashboardApplicationsTab from '../../../../main/frontend/legal/dashboard/LegalDashboardApplicationsTab';
import LegalDashboardComponentsTab from '../../../../main/frontend/legal/dashboard/LegalDashboardComponentsTab';

describe('LegalDashboardPage', function() {
  let minimalProps,
      LegalDashboardPage,
      LegalDashboardFilterContainerMock,
      loadResultsSpy,
      getShallowComponent;
  const mockApplications = {
    results: []
  };

  beforeEach(function() {
    LegalDashboardFilterContainerMock = jasmine.createSpy('MaximizedContainerMock')
        .and.returnValue(<div>MaximizedContainer</div>);

    LegalDashboardPage =
        require('inject-loader!../../../../main/frontend/legal/dashboard/LegalDashboardPage')({
          './filter/LegalDashboardFilterContainer': LegalDashboardFilterContainerMock
        }).default;

    loadResultsSpy = jasmine.createSpy('loadResults');
    minimalProps = {
      applications: mockApplications,
      components: [],
      loadResults: loadResultsSpy,
      isAuthorized: true,
      filtersAreDirty: 'filtersAreDirty'
    };

    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardPage, minimalProps);
  });

  it('renders an aside and a main', function() {
    expect(getShallowComponent().find('aside.nx-page-sidebar')).toExist();
    expect(getShallowComponent().find('main.nx-page-main')).toExist();
  });

  it('renders an LegalDashboardApplicationsTab', function() {
    const wrapper = getShallowComponent();
    let applicationsTab = wrapper.find(LegalDashboardApplicationsTab);
    expect(applicationsTab).toExist();
    expect(applicationsTab).toHaveProp('applications', mockApplications.results);
    expect(applicationsTab).toHaveProp('filtersAreDirty', 'filtersAreDirty');
  });

  it('renders an LegalDashboardComponentsTab', function() {
    const wrapper = getShallowComponent();
    let componentsTab = wrapper.find(LegalDashboardComponentsTab);
    expect(componentsTab).toExist();
    expect(componentsTab).toHaveProp('components');
    expect(componentsTab).toHaveProp('filtersAreDirty', 'filtersAreDirty');
  });
});
