/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import React from 'react';
import ComponentOverviewTile from '../../../main/frontend/legal/ComponentOverviewTile';
import NoticeTextsTile from '../../../main/frontend/legal/NoticeTextsTile';
import LicenseTextsTile from '../../../main/frontend/legal/LicenseTextsTile';
import LicenseObligationsTile from '../../../main/frontend/legal/LicenseObligationsTile';
import LicenseDetailsTile from '../../../main/frontend/legal/LicenseDetailsTile';
import CopyrightStatementsTile from '../../../main/frontend/legal/CopyrightStatementsTile';
import { mount } from 'enzyme/build';

describe('ComponentLegalOverviewPage', function() {
  let minimalProps,
      ComponentLegalOverviewPage,
      MaximizedContainerMock,
      loadComponentSpy,
      getShallowComponent;

  beforeEach(function() {
    MaximizedContainerMock = jasmine.createSpy('MaximizedContainerMock')
        .and.returnValue(<div>MaximizedContainer</div>);

    ComponentLegalOverviewPage =
        require('inject-loader!../../../main/frontend/legal/ComponentLegalOverviewPage')({
          '../react/MaximizedContainer': MaximizedContainerMock
        }).default;

    loadComponentSpy = jasmine.createSpy('loadComponent');

    minimalProps = {
      loadComponent: loadComponentSpy,
      hash: '1e48256a2341047e7d72'
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentLegalOverviewPage, minimalProps);
  });

  it('fires the loadFilter action', function() {
    const component = mount(<ComponentLegalOverviewPage {...minimalProps} />);
    expect(loadComponentSpy).toHaveBeenCalledWith('organization', 'ROOT_ORGANIZATION_ID', '1e48256a2341047e7d72');
    component.unmount();
  });

  it('does not fire the loadFilter action if there is no hash', function() {
    const component = mount(<ComponentLegalOverviewPage loadComponent={ loadComponentSpy } />);
    expect(loadComponentSpy).not.toHaveBeenCalled();
    component.unmount();
  });

  it('renders a component with the "nx-page-content" class', function() {
    expect(getShallowComponent().find('.nx-page-content')).toExist();
  });

  it('renders the ComponentOverviewTile', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find(ComponentOverviewTile)).toExist();
  });

  it('renders the CopyrightStatementsTile', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find(CopyrightStatementsTile)).toExist();
  });

  it('renders the LicenseDetailsTile', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find(LicenseDetailsTile)).toExist();
  });

  it('renders the LicenseObligationsTile', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find(LicenseObligationsTile)).toExist();
  });

  it('renders the LicenseTextsTile', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find(LicenseTextsTile)).toExist();
  });

  it('renders the NoticeTextsTile', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find(NoticeTextsTile)).toExist();
  });
});
