/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import React from 'react';
import { mount } from 'enzyme/build';
import LegalDashboardApplicationsTab from '../../../../main/frontend/legal/dashboard/LegalDashboardApplicationsTab';
import LegalDashboardComponentsTab from '../../../../main/frontend/legal/dashboard/LegalDashboardComponentsTab';

describe('LegalDashboardPage', function() {
  let minimalProps,
      LegalDashboardPage,
      LegalDashboardFilterContainerMock,
      loadApplicationsSpy,
      getShallowComponent;

  beforeEach(function() {
    LegalDashboardFilterContainerMock = jasmine.createSpy('MaximizedContainerMock')
        .and.returnValue(<div>MaximizedContainer</div>);

    LegalDashboardPage =
        require('inject-loader!../../../../main/frontend/legal/dashboard/LegalDashboardPage')({
          './LegalDashboardFilterContainer': LegalDashboardFilterContainerMock
        }).default;

    loadApplicationsSpy = jasmine.createSpy('loadApplications');
    minimalProps = {
      applications: [],
      components: [],
      loadApplications: loadApplicationsSpy,
      isAuthorized: true
    };

    getShallowComponent = enzymeUtils.getShallowComponent(LegalDashboardPage, minimalProps);
  });

  it('fires the loadApplications action if authorized', function() {
    const component = mount(<LegalDashboardPage { ...minimalProps } />);
    expect(loadApplicationsSpy).toHaveBeenCalledWith();
    component.unmount();
  });

  it('does not fire the loadApplications action if not authorized', function() {
    const props = {
      isAuthorized: true,
      ...minimalProps
    };
    const component = mount(<LegalDashboardPage { ...props } />);
    expect(loadApplicationsSpy).toHaveBeenCalledWith();
    component.unmount();
  });

  it('renders an aside and a main', function() {
    expect(getShallowComponent().find('aside.nx-page-sidebar')).toExist();
    expect(getShallowComponent().find('main.nx-page-main')).toExist();
  });

  it('renders an LegalDashboardApplicationsTab', function() {
    const wrapper = getShallowComponent();
    let applicationsTab = wrapper.find(LegalDashboardApplicationsTab);
    expect(applicationsTab).toExist();
    expect(applicationsTab).toHaveProp('applications');
  });

  it('renders an LegalDashboardComponentsTab', function() {
    const wrapper = getShallowComponent();
    let componentsTab = wrapper.find(LegalDashboardComponentsTab);
    expect(componentsTab).toExist();
    expect(componentsTab).toHaveProp('components');
  });
});
