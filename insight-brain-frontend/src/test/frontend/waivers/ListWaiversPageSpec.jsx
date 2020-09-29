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
      MaximizedContainerMock,
      loadViolationSpy,
      stateMock,
      stateGoSpy,
      stateHrefSpy,
      violationDetailsMock,
      getShallowComponent;

  beforeEach(function() {
    MaximizedContainerMock = jasmine.createSpy('MaximizedContainerMock')
        .and.returnValue(<div>MaximizedContainer</div>);

    ListWaiversPage = require('inject-loader!../../../main/frontend/waivers/ListWaiversPage')({
      '../react/MaximizedContainer': MaximizedContainerMock
    }).default;

    loadViolationSpy = jasmine.createSpy('loadViolationSpy');
    stateGoSpy = jasmine.createSpy();
    stateHrefSpy = jasmine.createSpy().and.returnValue('href');
    stateMock = {
      get: jasmine.createSpy().and.returnValue('/violation'),
      go: stateGoSpy,
      href: stateHrefSpy
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
      loadViolation: loadViolationSpy
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
    expect(stateHrefSpy).toHaveBeenCalledWith('/violation', { id: 'violationId' });
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
    const component = getShallowComponent({ violationDetailsError: 'error' });
    const loadWrapper = component.find(LoadWrapper);
    expect(loadWrapper).toHaveProp('error', 'error');
  });

  it('has the policy name and the correct threat level classes in the header', function() {
    const component = getShallowComponent({ violationDetails: violationDetailsMock });
    const policyNameSpan = component.find('.list-waivers--threat-indicator span');
    expect(policyNameSpan).toHaveText('policyName');
    expect(policyNameSpan).toMatchSelector('.iq-threat-level');
    expect(policyNameSpan).toMatchSelector('.iq-threat-level--severe');
  });

  it('renders two nx-tiles', function() {
    const component = getShallowComponent();
    const nxTiles = component.find('.nx-tile');
    expect(nxTiles.length).toEqual(2);
    expect(nxTiles.at(0).find('.nx-h2')).toHaveText('Waiver Details');
    expect(nxTiles.at(1).find('.nx-h2')).toHaveText('Waiver List Table');
  });

  it('properly renders the constraint section', function() {
    const component = getShallowComponent({ violationDetails: violationDetailsMock });
    const constraintSection = component.find('.list-waivers--constraint');
    expect(constraintSection.find('h3.nx-label')).toHaveText('Constraint Name');
    expect(constraintSection.find('div.iq-read-only-data')).toHaveText('constraint name');
  });

  it('properly renders the conditions section', function() {
    const component = getShallowComponent({ violationDetails: violationDetailsMock });
    const conditionsSection = component.find('.list-waivers--conditions');
    expect(conditionsSection.find('h3.nx-label')).toHaveText('Conditions');
    expect(conditionsSection.find('span')).toHaveText('reason');
  });

  it('properly renders the component name section', function() {
    const component = getShallowComponent({ violationDetails: violationDetailsMock });
    const componentNameSection = component.find('.list-waivers--component-name');
    expect(componentNameSection.find('h3.nx-label')).toHaveText('Component Name');
    expect(componentNameSection.find('div.iq-read-only-data')).toHaveText('filename');
  });

  it('shows a button on the waiver list table header', function() {
    const component = getShallowComponent();
    const buttonSection = component.find('.nx-tile__actions');
    const button = buttonSection.find(NxButton);
    expect(button).toMatchSelector('.nx-btn--tertiary');
    expect(button.find('span')).toHaveText('Add Waiver');
    const icon = button.find(NxFontAwesomeIcon);
    expect(icon).toHaveProp('icon', faPlus);
  });

  it('redirects to the add waiver page when clicking the waiver list table header button', function() {
    const component = getShallowComponent();
    const buttonSection = component.find('.nx-tile__actions');
    const button = buttonSection.find(NxButton);
    expect(stateGoSpy).not.toHaveBeenCalled();
    button.simulate('click');
    expect(stateGoSpy).toHaveBeenCalledWith('addWaiver', { violationId: 'violationId' });
  });

  it('renders an .nx-table', function() {
    const component = getShallowComponent();
    const table = component.find('.nx-table');
    expect(table).toExist();
  });
});
