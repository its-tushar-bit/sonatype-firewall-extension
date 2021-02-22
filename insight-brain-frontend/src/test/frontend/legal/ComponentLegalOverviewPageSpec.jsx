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
import LicenseDetailsTile from '../../../main/frontend/legal/LicenseDetailsTile';
import CopyrightStatementsTile from '../../../main/frontend/legal/CopyrightStatementsTile';
import ComponentLegalOverviewPage from '../../../main/frontend/legal/ComponentLegalOverviewPage';
import { mount } from 'enzyme/build';
import LicenseObligationsTileContainer from '../../../main/frontend/legal/LicenseObligationsTileContainer';

describe('ComponentLegalOverviewPage', function() {
  let minimalProps,
      loadComponentSpy,
      loadAvailableScopesSpy,
      getShallowComponent;

  beforeEach(function() {
    loadComponentSpy = jasmine.createSpy('loadComponent');
    loadAvailableScopesSpy = jasmine.createSpy('loadAvailableScopes');
    const licenseLegalMetadata = {
      0: {
        licenseName: 'license1',
        obligations: [{
          name: 'obligation 1',
          obligationTexts: [
            'text1',
            'text2'
          ]
        }, {
          name: 'obligation 2',
          obligationTexts: [
            'text3',
            'text4'
          ]
        }]
      },
      1: {
        licenseName: 'license2',
        obligations: [{
          name: 'obligation 2',
          obligationTexts: [
            'text5',
            'text6'
          ]
        }, {
          name: 'obligation 3',
          obligationTexts: [
            'text7',
            'text8'
          ]
        }]
      },
      2: {
        licenseName: 'multilicense',
        obligations: null
      }
    };

    const obligations = [{
      name: 'obligation 1',
      status: 'OPEN',
      comment: null,
      attributions: []
    },
    {
      name: 'obligation 2',
      status: 'IGNORED',
      comment: 'comment',
      attributions: [{ id: 'attribution1', content: 'attributionText' }]
    },
    {
      name: 'obligation 3',
      status: 'FULFILLED',
      comment: null,
      attributions: [
        { id: 'attribution2', content: 'attributionText1' }, { id: 'attribution3', content: 'attributionText2' }
      ]
    }];

    minimalProps = {
      loadComponent: loadComponentSpy,
      loadAvailableScopes: loadAvailableScopesSpy,
      licenseLegalMetadata,
      obligations,
      hash: '1e48256a2341047e7d72'
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentLegalOverviewPage, minimalProps);
  });

  it('loads the expected data using the root organization id', function() {
    const component = mount(<ComponentLegalOverviewPage {...minimalProps} loading={true} />);
    expect(loadComponentSpy).toHaveBeenCalledWith('organization', 'ROOT_ORGANIZATION_ID', '1e48256a2341047e7d72');
    expect(loadAvailableScopesSpy).toHaveBeenCalledWith('organization', 'ROOT_ORGANIZATION_ID');
    component.unmount();
  });

  it('loads the expected data using the organization id', function() {
    const component = mount(<ComponentLegalOverviewPage {...{ ...minimalProps, organizationId: 'orgId' }}
                                                        loading={true}/>);
    expect(loadComponentSpy).toHaveBeenCalledWith('organization', 'orgId', '1e48256a2341047e7d72');
    expect(loadAvailableScopesSpy).toHaveBeenCalledWith('organization', 'orgId');
    component.unmount();
  });

  it('loads the expected data using the application public id', function() {
    const component = mount(<ComponentLegalOverviewPage {...{ ...minimalProps, applicationPublicId: 'appId' }}
                                                        loading={true}/>);
    expect(loadComponentSpy).toHaveBeenCalledWith('application', 'appId', '1e48256a2341047e7d72');
    expect(loadAvailableScopesSpy).toHaveBeenCalledWith('application', 'appId');
    component.unmount();
  });

  it('does not load the data if there is no hash', function() {
    const component = mount(<ComponentLegalOverviewPage loadComponent={ loadComponentSpy } loading={true} />);
    expect(loadComponentSpy).not.toHaveBeenCalled();
    expect(loadAvailableScopesSpy).not.toHaveBeenCalled();
    component.unmount();
  });

  it('renders a component with the "nx-page-main" class', function() {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
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
    expect(wrapper.find(LicenseObligationsTileContainer)).toExist();
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
