/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import React from 'react';
import LoadWrapper from '../../../main/frontend/react/LoadWrapper';
import ViolationExclamation from '../../../main/frontend/react/ViolationExclamation';
import { NxPageTitle, NxH1, NxH2, NxFontAwesomeIcon, NxTile } from '@sonatype/react-shared-components';
import { faPlus } from '@fortawesome/free-solid-svg-icons';

describe('ListWaiversPage', function () {
  let minimalProps,
    ListWaiversPage,
    ListWaiversTableMock,
    loadManageWaiversDataSpy,
    violationDetailsMock,
    getShallowComponent,
    DeleteWaiverModalMock,
    setWaiverToDeleteMock,
    BackButtonMock,
    stateGoSpy;

  beforeEach(function () {
    ListWaiversTableMock = jasmine.createSpy('ListWaiversTableMock').and.returnValue(<div>ListWaiversTable</div>);
    DeleteWaiverModalMock = jasmine.createSpy('DeleteWaiverModalMock').and.returnValue(<div>Delete Waiver Modal</div>);
    BackButtonMock = jasmine.createSpy('ListWaiversBackButton').and.returnValue(<div>List Waivers Back Button</div>);

    ListWaiversPage = require('inject-loader!../../../main/frontend/waivers/ListWaiversPage')({
      './ListWaiversTable': ListWaiversTableMock,
      './deleteWaiverModal/DeleteWaiverModalContainer': DeleteWaiverModalMock,
      './ListWaiversBackButton': BackButtonMock,
    }).default;

    loadManageWaiversDataSpy = jasmine.createSpy('loadManageWaiversDataSpy');
    setWaiverToDeleteMock = () => {};
    stateGoSpy = jasmine.createSpy();

    violationDetailsMock = {
      filename: 'filename',
      constraintViolations: [
        {
          constraintName: 'constraint name',
          reasons: [
            {
              reason: 'reason',
              reference: {
                value: 'CVE-67890',
              },
            },
          ],
        },
      ],
      policyName: 'policyName',
      policyViolationId: 'policyViolationId',
      threatLevel: 5,
    };

    minimalProps = {
      loadingManageWaiversData: false,
      violationId: 'violationId',
      waiverComments: {
        value: '',
        isPristine: true,
      },
      backButtonStateName: 'backButtonStateName',
      loadManageWaiversData: loadManageWaiversDataSpy,
      hasPermissionForAppWaivers: false,
      setWaiverToDelete: setWaiverToDeleteMock,
      waiverToDelete: null,
      stateGo: stateGoSpy,
      isCurrentRouteName: false,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ListWaiversPage, minimalProps);
  });

  it('renders a component with the "nx-page-main" class', function () {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  it('renders a ListWaiversBackButton with correct props', function () {
    let backButton = getShallowComponent().find(BackButtonMock);
    expect(backButton).toExist();
    expect(backButton).toHaveProp('violationId', 'violationId');

    const stateProps = {
      type: 'violation',
      sidebarReference: 'sidebarRef',
      hash: 'a-hash',
      publicId: 'publicId1',
      scanId: 'scanId1',
    };

    backButton = getShallowComponent(stateProps).find(BackButtonMock);
    expect(backButton).toExist();
    expect(backButton).toHaveProp('violationId', 'violationId');
    expect(backButton).toHaveProp('type', 'violation');
    expect(backButton).toHaveProp('sidebarReference', 'sidebarRef');
    expect(backButton).toHaveProp('hash', 'a-hash');
    expect(backButton).toHaveProp('publicId', 'publicId1');
    expect(backButton).toHaveProp('scanId', 'scanId1');
  });

  it('renders a page title', function () {
    const component = getShallowComponent();
    expect(component.find(NxPageTitle)).toExist();
    expect(component.find(NxH1)).toHaveText('Waivers for Violation');
  });

  it('does not have a ViolationExclamation without a threatLevelCategory', function () {
    const component = getShallowComponent();
    expect(component.find(ViolationExclamation)).not.toExist();
  });

  it('renders a ViolationExclamation if there is a threatLevelCategory', function () {
    const component = getShallowComponent({
      violationDetails: violationDetailsMock,
    });
    expect(component.find(ViolationExclamation)).toExist();
    expect(component.find(ViolationExclamation)).toHaveProp('threatLevelCategory', 'severe');
  });

  it('renders the DeleteWaiverModal component if there is a waiverToDelete in the state', function () {
    const component = getShallowComponent({
      waiverToDelete: { waiverId: 'foo' },
    });
    expect(component.find(DeleteWaiverModalMock)).toExist();
  });

  it('does not render the DeleteWaiverModal if there is not a waiverToDelete in the state', function () {
    const component = getShallowComponent();
    expect(component.find(DeleteWaiverModalMock)).not.toExist();
  });

  it('renders a loading LoadWrapper when loadingManageWaiversData is true', function () {
    const component = getShallowComponent({ loadingManageWaiversData: true });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('renders a loading LoadWrapper when the violationDetails prop is missing', function () {
    const component = getShallowComponent({ violationDetails: null });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('loading', true);
  });

  it('passes any error to the LoadWrapper', function () {
    const component = getShallowComponent({
      loadManageWaiversDataError: 'error',
    });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('error', 'error');
  });

  it('calls loadManageWaiversData when the LoadWrapper retryHandler is invoked', function () {
    const loadWrapper = getShallowComponent().find(LoadWrapper),
      retryHandler = loadWrapper.prop('retryHandler');

    expect(loadManageWaiversDataSpy).not.toHaveBeenCalled();

    retryHandler();

    expect(loadManageWaiversDataSpy).toHaveBeenCalledWith('violationId');
  });

  it('has the policy name and the correct threat level classes in the header', function () {
    const component = getShallowComponent({
      violationDetails: violationDetailsMock,
    });
    const policyNameSpan = component.find('.iq-threat-level');
    expect(policyNameSpan).toHaveText('policyName');
    expect(policyNameSpan).toMatchSelector('.iq-threat-level--severe');
  });

  it('renders two nx-tiles', function () {
    const component = getShallowComponent();
    const nxTiles = component.find(NxTile);
    const tileHeadings = component.find(NxH2);

    expect(nxTiles.length).toEqual(2);
    expect(tileHeadings.length).toEqual(2);
    expect(tileHeadings.at(0)).toHaveText('Violation Details');
    expect(tileHeadings.at(1)).toHaveText('Applicable Waivers');
  });

  it('properly renders the constraint section', function () {
    const component = getShallowComponent({
      violationDetails: violationDetailsMock,
    });
    expect(component.find('#list-waivers-constraint-name')).toHaveText('constraint name');
  });

  it('properly renders the conditions section', function () {
    const component = getShallowComponent({
      violationDetails: violationDetailsMock,
    });

    expect(component.find('.list-waivers-condition')).toHaveText('reason');
  });

  it('properly renders the component name section', function () {
    const component = getShallowComponent({
      violationDetails: violationDetailsMock,
    });
    expect(component.find('#list-waivers-component-name')).toHaveText('filename');
  });

  it('shows a Request Waiver button on the waiver list table header', () => {
    const component = getShallowComponent();
    const requestWaiverButton = component.find('#request-waiver-btn');
    expect(requestWaiverButton.find('span')).toHaveText('Request Waiver');
  });

  it('shows an Add Waiver button on the waiver list table header', () => {
    const component = getShallowComponent();
    const addWaiverButton = component.find('#add-waiver-btn');
    const icon = addWaiverButton.find(NxFontAwesomeIcon);

    expect(addWaiverButton.find('span')).toHaveText('Add Waiver');
    expect(icon).toHaveProp('icon', faPlus);
  });

  describe('Add Waiver button', function () {
    describe('if hasPermissionForAppWaivers is true', function () {
      let addWaiverButton;
      beforeEach(function () {
        const component = getShallowComponent({
          hasPermissionForAppWaivers: true,
        });
        addWaiverButton = component.find('#add-waiver-btn');
      });
      it('redirects to the add waiver page', function () {
        expect(stateGoSpy).not.toHaveBeenCalled();
        addWaiverButton.simulate('click');
        expect(stateGoSpy).toHaveBeenCalledWith('addWaiver', {
          violationId: 'violationId',
        });
      });
      it('renders as enabled', function () {
        expect(addWaiverButton).not.toHaveClassName('disabled');
      });
      it('renders with no tooltip', function () {
        expect(addWaiverButton.parent()).toHaveProp('title', '');
      });
    });

    describe('if hasPermissionForAppWaivers is false', function () {
      let addWaiverButton;
      beforeEach(function () {
        const component = getShallowComponent();
        addWaiverButton = component.find('#add-waiver-btn');
      });
      it('does not redirect to the add waiver', function () {
        addWaiverButton.simulate('click');
        expect(stateGoSpy).not.toHaveBeenCalled();
      });
      it('renders as disabled', function () {
        expect(addWaiverButton).toHaveClassName('disabled');
      });
      it('renders with tooltip', function () {
        expect(addWaiverButton.parent()).toHaveProp('title', 'Insufficient permissions to Add Waiver');
      });
    });
  });

  it('renders the ListWaiversTable component', function () {
    const activeWaivers = [{ foo: 'bar1' }],
      expiredWaivers = [{ foo: 'bar2' }],
      loadingApplicableWaivers = false,
      loadApplicableWaiversError = 'error',
      loadApplicableWaiversSpy = jasmine.createSpy();
    const component = getShallowComponent({
      activeWaivers: activeWaivers,
      expiredWaivers: expiredWaivers,
      violationDetails: violationDetailsMock,
      loadingApplicableWaivers,
      loadApplicableWaiversError,
      loadApplicableWaivers: loadApplicableWaiversSpy,
    });
    const table = component.find(ListWaiversTableMock);
    expect(table).toExist();
    expect(table).toHaveProp('activeWaivers', activeWaivers);
    expect(table).toHaveProp('expiredWaivers', expiredWaivers);
    expect(table).toHaveProp('violationDetails', violationDetailsMock);
    expect(table).toHaveProp('setWaiverToDelete', setWaiverToDeleteMock);
    expect(table).toHaveProp('loadingApplicableWaivers', loadingApplicableWaivers);
    expect(table).toHaveProp('loadApplicableWaiversError', loadApplicableWaiversError);

    const reloadApplicableWaiversCallback = table.prop('reloadApplicableWaivers');
    reloadApplicableWaiversCallback();
    expect(loadApplicableWaiversSpy).toHaveBeenCalledWith('violationId');
  });

  describe('Request Waiver button', () => {
    let requestWaiverButton;
    it('if button is availabe', () => {
      const component = getShallowComponent();
      requestWaiverButton = component.find('#request-waiver-btn');
      expect(requestWaiverButton).toExist();
    });

    it('if button is not availabe', () => {
      const component = getShallowComponent({ isCurrentRouteName: true });
      requestWaiverButton = component.find('#request-waiver-btn');
      expect(requestWaiverButton).not.toExist();
    });
  });
});
