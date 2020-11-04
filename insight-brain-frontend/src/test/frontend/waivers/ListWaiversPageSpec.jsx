/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import React from 'react';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';
import ViolationExclamation from '../../../main/frontend/react/ViolationExclamation';
import { NxBackButton, NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faPlus } from '@fortawesome/free-solid-svg-icons';

describe('ListWaiversPage', function() {
  let minimalProps,
      ListWaiversPage,
      ListWaiversTableMock,
      MaximizedContainerMock,
      loadManageWaiversDataSpy,
      stateMock,
      stateGoSpy,
      stateHrefSpy,
      violationDetailsMock,
      getShallowComponent,
      DeleteWaiverModalMock,
      setWaiverToDeleteMock;

  beforeEach(function() {
    ListWaiversTableMock = jasmine.createSpy('ListWaiversTableMock')
        .and.returnValue(<div>ListWaiversTable</div>);
    MaximizedContainerMock = jasmine.createSpy('MaximizedContainerMock')
        .and.returnValue(<div>MaximizedContainer</div>);
    DeleteWaiverModalMock = jasmine.createSpy('DeleteWaiverModalMock')
        .and.returnValue(<div>Delete Waiver Modal</div>);

    ListWaiversPage = require('inject-loader!../../../main/frontend/waivers/ListWaiversPage')({
      '../react/MaximizedContainer': MaximizedContainerMock,
      './ListWaiversTable': ListWaiversTableMock,
      './deleteWaiverModal/DeleteWaiverModalContainer': DeleteWaiverModalMock
    }).default;

    loadManageWaiversDataSpy = jasmine.createSpy('loadManageWaiversDataSpy');
    setWaiverToDeleteMock = () => {};
    stateGoSpy = jasmine.createSpy();
    stateHrefSpy = jasmine.createSpy().and.returnValue('href');
    stateMock = {
      get: jasmine.createSpy().and.returnValue('/violation'),
      go: stateGoSpy,
      href: stateHrefSpy,
      params: {id: 'violationId', type: 'violation', sidebarReference: 'filter'}
    };

    violationDetailsMock = {
      filename: 'filename',
      constraintViolations: [{
        constraintName: 'constraint name',
        reasons: [{
          reason: 'reason',
          reference: {
            value: 'CVE-67890'
          }
        }]
      }],
      policyName: 'policyName',
      policyViolationId: 'policyViolationId',
      threatLevel: 5
    };

    minimalProps = {
      loading: false,
      violationId: 'violationId',
      waiverComments: {
        value: '',
        isPristine: true
      },
      $state: stateMock,
      backButtonStateName: 'backButtonStateName',
      loadManageWaiversData: loadManageWaiversDataSpy,
      hasPermissionForAppWaivers: false,
      setWaiverToDelete: setWaiverToDeleteMock,
      waiverToDelete: null
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ListWaiversPage, minimalProps);
  });

  it('renders a component with the "nx-page-content" class', function() {
    expect(getShallowComponent()).toMatchSelector('.nx-page-content');
  });

  it('renders a NxBackButton with correct href and targetPageTitle properties', function() {
    const component = getShallowComponent();
    const backButton = component.find(NxBackButton);
    expect(backButton).toExist();
    expect(backButton).toHaveProp('targetPageTitle', 'Violation Details');
    expect(stateHrefSpy).toHaveBeenCalledWith(
        '/violation',
        { id: 'violationId', type: 'violation', sidebarReference: 'filter' }
    );
    expect(backButton).toHaveProp('href', 'href');
  });

  it('renders a page title', function() {
    const component = getShallowComponent();
    expect(component.find('.nx-page-title')).toExist();
    expect(component.find('.nx-h1')).toHaveText('Waivers for Violation');
  });

  it('does not have a ViolationExclamation without a threatLevelCategory', function() {
    const component = getShallowComponent();
    expect(component.find(ViolationExclamation)).not.toExist();
  });

  it('renders a ViolationExclamation if there is a threatLevelCategory', function() {
    const component = getShallowComponent({ violationDetails: violationDetailsMock });
    expect(component.find(ViolationExclamation)).toExist();
    expect(component.find(ViolationExclamation)).toHaveProp('threatLevelCategory', 'severe');
  });

  it('renders the DeleteWaiverModal component if there is a waiverToDelete in the state', function() {
    const component = getShallowComponent({ waiverToDelete: { waiverId: 'foo' } });
    expect(component.find(DeleteWaiverModalMock)).toExist();
  });

  it('does not render the DeleteWaiverModal if there is not a waiverToDelete in the state', function() {
    const component = getShallowComponent();
    expect(component.find(DeleteWaiverModalMock)).not.toExist();
  });

  it('renders a loading LoadWrapper when loading is true', function() {
    const component = getShallowComponent({ loading: true});
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('renders a loading LoadWrapper when the violationDetails prop is missing', function() {
    const component = getShallowComponent({ violationDetails: null });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('passes any error to the LoadWrapper', function() {
    const component = getShallowComponent({ loadError: 'error' });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('error', 'error');
  });

  it('calls loadManageWaiversData when the LoadWrapper retryHandler is invoked', function() {
    const loadWrapper = getShallowComponent().find(LoadWrapper),
        retryHandler = loadWrapper.prop('retryHandler');

    expect(loadManageWaiversDataSpy).not.toHaveBeenCalled();

    retryHandler();

    expect(loadManageWaiversDataSpy).toHaveBeenCalledWith('violationId');
  });

  it('has the policy name and the correct threat level classes in the header', function() {
    const component = getShallowComponent({ violationDetails: violationDetailsMock });
    const policyNameSpan = component.find('.list-waivers__threat-indicator span');
    expect(policyNameSpan).toHaveText('policyName');
    expect(policyNameSpan).toMatchSelector('.iq-threat-level');
    expect(policyNameSpan).toMatchSelector('.iq-threat-level--severe');
  });

  it('renders two nx-tiles', function() {
    const component = getShallowComponent();
    const nxTiles = component.find('.nx-tile');
    expect(nxTiles.length).toEqual(2);
    expect(nxTiles.at(0).find('.nx-h2')).toHaveText('Violation Details');
    expect(nxTiles.at(1).find('.nx-h2')).toHaveText('Applicable Waivers');
  });

  it('properly renders the constraint section', function() {
    const component = getShallowComponent({ violationDetails: violationDetailsMock });
    expect(component.find('#list-waivers-constraint-name')).toHaveText('constraint name');
  });

  it('properly renders the conditions section', function() {
    const component = getShallowComponent({ violationDetails: violationDetailsMock });
    expect(component.find('#list-waivers-conditions')).toHaveText('reason');
  });

  it('properly renders the component name section', function() {
    const component = getShallowComponent({ violationDetails: violationDetailsMock });
    expect(component.find('#list-waivers-component-name')).toHaveText('filename');
  });

  it('shows a button on the waiver list table header', function() {
    const component = getShallowComponent();
    const buttonSection = component.find('.nx-tile__actions');
    const button = buttonSection.find(NxButton);
    expect(button.find('span')).toHaveText('Add Waiver');
    const icon = button.find(NxFontAwesomeIcon);
    expect(icon).toHaveProp('icon', faPlus);
  });

  describe('Add Waiver button', function() {
    describe('if hasPermissionForAppWaivers is true', function() {
      let addWaiverButton;
      beforeEach(function() {
        const component = getShallowComponent({
          hasPermissionForAppWaivers: true
        });
        addWaiverButton = component.find('#add-waiver-btn');
      });
      it('redirects to the add waiver page', function() {
        expect(stateGoSpy).not.toHaveBeenCalled();
        addWaiverButton.simulate('click');
        expect(stateGoSpy).toHaveBeenCalledWith('addWaiver', { violationId: 'violationId' });
      });
      it('renders as enabled', function() {
        expect(addWaiverButton).not.toHaveClassName('disabled');
      });
      it('renders with no tooltip', function() {
        expect(addWaiverButton.parent()).toHaveProp('title', '');
      });
    });

    describe('if hasPermissionForAppWaivers is false', function() {
      let addWaiverButton;
      beforeEach(function() {
        const component = getShallowComponent();
        addWaiverButton = component.find('#add-waiver-btn');
      });
      it('does not redirect to the add waiver', function() {
        addWaiverButton.simulate('click');
        expect(stateGoSpy).not.toHaveBeenCalled();
      });
      it('renders as disabled', function() {
        expect(addWaiverButton).toHaveClassName('disabled');
      });
      it('renders with tooltip', function() {
        expect(addWaiverButton.parent()).toHaveProp('title', 'Insufficient permissions to Add Waiver');
      });
    });
  });

  it('renders the ListWaiversTable component', function() {
    const activeWaivers = [{ foo: 'bar1'}];
    const expiredWaivers = [{ foo: 'bar2'}];
    const component = getShallowComponent({
      activeWaivers: activeWaivers,
      expiredWaivers: expiredWaivers,
      violationDetails: violationDetailsMock
    });
    const table = component.find(ListWaiversTableMock);
    expect(table).toExist();
    expect(table).toHaveProp('activeWaivers', activeWaivers);
    expect(table).toHaveProp('expiredWaivers', expiredWaivers);
    expect(table).toHaveProp('violationDetails', violationDetailsMock);
    expect(table).toHaveProp('setWaiverToDelete', setWaiverToDeleteMock);
  });
});
