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

describe('ComponentLegalOverviewPage', function() {
  let minimalProps,
      ComponentLegalOverviewPage,
      MaximizedContainerMock,
      loadResultsSpy,
      getShallowComponent;

  beforeEach(function() {
    MaximizedContainerMock = jasmine.createSpy('MaximizedContainerMock')
        .and.returnValue(<div>MaximizedContainer</div>);

    ComponentLegalOverviewPage =
        require('inject-loader!../../../main/frontend/legal/ComponentLegalOverviewPage')({
          '../react/MaximizedContainer': MaximizedContainerMock
        }).default;

    loadResultsSpy = jasmine.createSpy('loadResults');

    minimalProps = {
      components: 'components',
      loadResults: loadResultsSpy
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentLegalOverviewPage, minimalProps);
  });

  it('renders a component with the "nx-page-content" class', function() {
    expect(getShallowComponent()).toMatchSelector('.nx-page-content');
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
