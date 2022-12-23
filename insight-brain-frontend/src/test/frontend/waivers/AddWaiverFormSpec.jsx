/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import moment from 'moment';
import {
  NxButton,
  NxFieldset,
  NxRadio,
  NxTextInput,
  nxDateInputStateHelpers,
  NxFormSelect,
  NxTooltip,
} from '@sonatype/react-shared-components';

import * as enzymeUtils from '../enzymeUtils';
import AddWaiverForm from '../../../main/frontend/waivers/AddWaiverForm';
import ArtifactNameDisplay from '../../../main/frontend/react/ArtifactNameDisplay';
import ViolationExclamation from '../../../main/frontend/react/ViolationExclamation';
import * as VulnerabilityDetailsModalContainer from '../../../main/frontend/vulnerabilityDetails/VulnerabilityDetailsModalContainer';
import LoadError from '../../../main/frontend/react/LoadError';

describe('AddWaiverForm', function () {
  let minimalProps,
    getShallowComponent,
    getMountedComponent,
    saveWaiverSpy,
    setWaiverCommentSpy,
    setWaiverScopeSpy,
    setComponentMatcherStrategySpy,
    setExpiryTimeSpy,
    setCustomExpiryTimeSpy,
    openVulnerabilityDetailsModalSpy,
    closeVulnerabilityDetailsModalSpy,
    cancelActionSpy;

  beforeEach(function () {
    saveWaiverSpy = jasmine.createSpy('saveWaiver');
    setWaiverCommentSpy = jasmine.createSpy('setWaiverComment');
    setWaiverScopeSpy = jasmine.createSpy('setWaiverScope');
    setComponentMatcherStrategySpy = jasmine.createSpy('setComponentMatcherStrategy');
    openVulnerabilityDetailsModalSpy = jasmine.createSpy('openVulnerabilityDetailsModal');
    closeVulnerabilityDetailsModalSpy = jasmine.createSpy('closeVulnerabilityDetailsModal');
    cancelActionSpy = jasmine.createSpy('cancelAction');
    setExpiryTimeSpy = jasmine.createSpy('setExpiryTime');
    setCustomExpiryTimeSpy = jasmine.createSpy('setCustomExpiryTime');

    minimalProps = {
      componentIdentifier: { format: 'maven', coordinates: 'test' },
      componentMatcherStrategy: 'EXACT_COMPONENT',
      artifactName: 'artifact name',
      componentName: 'component name',
      allVersionsComponentName: 'component name',
      constraintName: 'constraint name',
      policyName: 'policy name',
      policyViolationId: 'violationId',
      expiryTime: '7',
      customExpiryTime: nxDateInputStateHelpers.initialState(''),
      reasons: ['reason1', 'reason2'],
      threatLevelCategory: 'severe',
      waiverComments: {
        value: 'waiver comments',
        isPristine: true,
      },
      availableWaiverScopes: [
        {
          id: 'id1',
          name: 'target1',
          label: 'Application',
          type: 'application',
        },
        {
          id: 'id2',
          name: 'target2',
          label: 'Organization',
          type: 'organization',
        },
        {
          id: 'ROOT_ORGANIZATION_ID',
          name: 'target3',
          label: 'Organization',
          type: 'organization',
        },
      ],
      selectedWaiverScope: {
        id: 'id1',
        name: 'target1',
        label: 'Application',
        type: 'application',
      },
      setWaiverScope: setWaiverScopeSpy,
      setComponentMatcherStrategy: setComponentMatcherStrategySpy,
      setWaiverComment: setWaiverCommentSpy,
      setExpiryTime: setExpiryTimeSpy,
      setCustomExpiryTime: setCustomExpiryTimeSpy,
      saveWaiver: saveWaiverSpy,
      openVulnerabilityDetailsModal: openVulnerabilityDetailsModalSpy,
      closeVulnerabilityDetailsModal: closeVulnerabilityDetailsModalSpy,
      vulnerabilityId: 'CVE-12345',
      cancelAction: cancelActionSpy,
      currentUser: 'test user',
      isFirewall: true,
      isFirewallOrRepositoryComponent: true,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(AddWaiverForm, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(AddWaiverForm, minimalProps);
  });

  it('renders a form with the appropriate classes', function () {
    const component = getShallowComponent();
    expect(component).toMatchSelector('form.nx-form.iq-add-waiver-form');
  });

  it('calls closeVulnerabilityDetailsModal when unmounting', function () {
    spyOn(VulnerabilityDetailsModalContainer, 'default').and.returnValue(
      <div>Vulnerability Details Modal Container</div>
    );

    const component = getMountedComponent();
    component.unmount();

    expect(closeVulnerabilityDetailsModalSpy).toHaveBeenCalledTimes(1);
  });

  it('renders a tile with the artifact and component names', function () {
    const component = getShallowComponent(),
      componentInfo = component.find('.iq-add-waiver-form__component'),
      artifactNameComponent = componentInfo.find(ArtifactNameDisplay),
      componentName = componentInfo.find('.nx-read-only__data');

    expect(componentInfo).toHaveClassName('nx-read-only iq-add-waiver-form__component');
    expect(artifactNameComponent).toHaveProp('artifactName', 'artifact name');
    expect(componentName).toHaveText('component name');
  });

  it('renders an nx-read-only with the policy info', function () {
    const component = getShallowComponent(),
      policySection = component.find('.iq-add-waiver-form__policy'),
      ViolationExclamationComponent = policySection.find(ViolationExclamation),
      policySpan = policySection.find('.iq-threat-level');

    expect(policySection).toHaveClassName('.nx-read-only');
    expect(ViolationExclamationComponent).toHaveProp('threatLevelCategory', 'severe');
    expect(policySpan).toHaveClassName('.iq-threat-level--severe');
    expect(policySpan).toHaveText('policy name');
  });

  it('renders an nx-read-only with the constraint info', function () {
    const component = getShallowComponent(),
      constraintSection = component.find('.iq-add-waiver-form__constraint'),
      constraintName = constraintSection.find('.nx-read-only__data');

    expect(constraintName).toHaveText('constraint name');
  });

  it('renders an nx-read-only with the conditions', function () {
    const component = getShallowComponent(),
      conditionsSection = component.find('.iq-add-waiver-form__conditions'),
      reasons = conditionsSection.find('.nx-read-only__data');

    expect(reasons.length).toBe(minimalProps.reasons.length);
    expect(reasons.at(0).find('span')).toHaveText('reason1');
    expect(reasons.at(1).find('span')).toHaveText('reason2');
  });

  it('renders an nx-read-only with the current user under "created by"', function () {
    const component = getShallowComponent(),
      createdBySection = component.find('.iq-add-waiver-form__created-by'),
      currentUser = createdBySection.find('.nx-read-only__data');

    expect(currentUser).toHaveText('test user');
  });

  it('renders a link to see vulnerability details and opens the modal on click', function () {
    const component = getShallowComponent(),
      vulnerabilityDetailsSection = component.find('.iq-add-waiver-form__vulnerability_details_link'),
      vulnerabilityDetailsLink = vulnerabilityDetailsSection.find('a');

    expect(vulnerabilityDetailsLink).toHaveText('See Security Vulnerability Details');
    expect(openVulnerabilityDetailsModalSpy).not.toHaveBeenCalled();
    vulnerabilityDetailsLink.simulate('click');
    expect(openVulnerabilityDetailsModalSpy).toHaveBeenCalledWith({
      vulnerabilityId: 'CVE-12345',
      componentIdentifier: { format: 'maven', coordinates: 'test' },
    });
  });

  it('renders a VulnerabilityDetailsModalContainer IFF vulnerabilityId is truthy', function () {
    let component = getShallowComponent();
    expect(component.find(VulnerabilityDetailsModalContainer.default)).toExist();

    component = getShallowComponent({ vulnerabilityId: null });
    expect(component.find(VulnerabilityDetailsModalContainer.default)).not.toExist();
    expect(component.find('.iq-add-waiver-form__vulnerability_details_link')).not.toExist();
  });

  it('renders a fieldset with NxRadios for the WaiverTargets', function () {
    const component = getShallowComponent(),
      waiverTargetsSection = component.find('.iq-add-waiver-form__scope'),
      targetRadios = waiverTargetsSection.find(NxRadio);

    expect(waiverTargetsSection.find(NxFieldset)).toExist();
    expect(waiverTargetsSection).toHaveProp('label', 'Scope');

    expect(targetRadios.length).toBe(3);
    expect(targetRadios.at(0)).toHaveProp('id', 'application-scope');
    expect(targetRadios.at(0)).toHaveProp('name', 'add-waiver-target');
    expect(targetRadios.at(0)).toHaveProp('value', 'id1');
    expect(targetRadios.at(0)).toHaveProp('isChecked', true);
    expect(targetRadios.at(0)).toHaveText('Application - target1');

    expect(targetRadios.at(1)).toHaveProp('id', 'organization-scope');
    expect(targetRadios.at(1)).toHaveProp('name', 'add-waiver-target');
    expect(targetRadios.at(1)).toHaveProp('value', 'id2');
    expect(targetRadios.at(1)).toHaveProp('isChecked', false);
    expect(targetRadios.at(1)).toHaveText('Organization - target2');

    expect(targetRadios.at(2)).toHaveProp('id', 'root-scope');
    expect(targetRadios.at(2)).toHaveProp('name', 'add-waiver-target');
    expect(targetRadios.at(2)).toHaveProp('value', 'ROOT_ORGANIZATION_ID');
    expect(targetRadios.at(2)).toHaveProp('isChecked', false);
    expect(targetRadios.at(2)).toHaveText('Organization - target3');
  });

  it('calls `setWaiverScope` when the waiver target is changed', function () {
    let component = getShallowComponent(),
      waiverTargetsSection = component.find('.iq-add-waiver-form__scope'),
      targetRadios = waiverTargetsSection.find(NxRadio),
      radio1 = targetRadios.at(0),
      radio2 = targetRadios.at(1);

    expect(radio1).toHaveProp('isChecked', true);
    expect(radio2).toHaveProp('isChecked', false);

    radio2.simulate('change', 'id2');
    expect(setWaiverScopeSpy).toHaveBeenCalledWith({
      id: 'id2',
      name: 'target2',
      label: 'Organization',
      type: 'organization',
    });
  });

  it('renders a fieldset with NxRadios for the waiver components', function () {
    const component = getShallowComponent(),
      componentsSection = component.find('.iq-add-waiver-form__components'),
      componentRadios = componentsSection.find(NxRadio);

    expect(componentsSection.find(NxFieldset)).toExist();
    expect(componentsSection).toHaveProp('label', 'Components');

    expect(componentRadios.length).toBe(3);

    expect(componentRadios.at(0)).toHaveProp('name', 'add-waiver-components');
    expect(componentRadios.at(0)).toHaveProp('value', 'EXACT_COMPONENT');
    expect(componentRadios.at(0)).toHaveProp('isChecked', true);
    expect(componentRadios.at(0)).toHaveText('component name');

    expect(componentRadios.at(1)).toHaveProp('name', 'add-waiver-components');
    expect(componentRadios.at(1)).toHaveProp('value', 'ALL_VERSIONS');
    expect(componentRadios.at(1)).toHaveProp('isChecked', false);
    expect(componentRadios.at(1)).toHaveText('component name (all versions)');

    expect(componentRadios.at(2)).toHaveProp('name', 'add-waiver-components');
    expect(componentRadios.at(2)).toHaveProp('value', 'ALL_COMPONENTS');
    expect(componentRadios.at(2)).toHaveProp('isChecked', false);
    expect(componentRadios.at(2)).toHaveText('All Components');
  });

  it('renders a disabled "all versions" radio button with tooltip when component identifier is null', function () {
    const component = getShallowComponent({ componentIdentifier: null }),
      componentsSection = component.find('.iq-add-waiver-form__components'),
      componentTooltip = componentsSection.find(NxTooltip),
      componentRadios = componentsSection.find(NxRadio);

    expect(componentRadios.length).toBe(3);

    expect(componentRadios.at(1)).toHaveProp('disabled', true);
    expect(componentTooltip).toHaveProp('title', 'Claim this component to apply all versions waiver');
  });

  it('calls `setComponentMatcherStrategy` when the waiver components are changed', function () {
    const component = getShallowComponent(),
      componentsSection = component.find('.iq-add-waiver-form__components'),
      componentRadios = componentsSection.find(NxRadio),
      component1 = componentRadios.at(0),
      component2 = componentRadios.at(1),
      component3 = componentRadios.at(2);

    expect(component1).toHaveProp('isChecked', true);
    expect(component2).toHaveProp('isChecked', false);
    expect(component3).toHaveProp('isChecked', false);

    component2.simulate('change', 'ALL_COMPONENTS');
    expect(setComponentMatcherStrategySpy).toHaveBeenCalledWith('ALL_COMPONENTS');

    component2.simulate('change', 'EXACT_COMPONENT');
    expect(setComponentMatcherStrategySpy).toHaveBeenCalledWith('EXACT_COMPONENT');

    component2.simulate('change', 'ALL_VERSIONS');
    expect(setComponentMatcherStrategySpy).toHaveBeenCalledWith('ALL_VERSIONS');
  });

  it('renders a fieldset with NxFormSelect for the expiry times', function () {
    const component = getShallowComponent(),
      expiryTimeSection = component.find('.iq-add-waiver-form__expiryTime'),
      selectComponent = expiryTimeSection.find(NxFormSelect),
      options = selectComponent.find('option');

    expect(expiryTimeSection.find(NxFieldset)).toExist();
    expect(expiryTimeSection).toHaveProp('label', 'Waiver Expiration');
    expect(selectComponent).toExist();
    expect(options.length).toBe(8);

    expect(options.at(0)).toHaveText('Never');
    expect(options.at(0)).toHaveProp('value', 'never');

    expect(options.at(1)).toHaveText('7 Days');
    expect(options.at(1)).toHaveProp('value', '7');

    expect(options.at(2)).toHaveText('14 Days');
    expect(options.at(2)).toHaveProp('value', '14');

    expect(options.at(3)).toHaveText('30 Days');
    expect(options.at(3)).toHaveProp('value', '30');

    expect(options.at(4)).toHaveText('60 Days');
    expect(options.at(4)).toHaveProp('value', '60');

    expect(options.at(5)).toHaveText('90 Days');
    expect(options.at(5)).toHaveProp('value', '90');

    expect(options.at(6)).toHaveText('120 Days');
    expect(options.at(6)).toHaveProp('value', '120');

    expect(options.at(7)).toHaveText('Custom');
    expect(options.at(7)).toHaveProp('value', 'custom');
  });

  it('renders the message "This waiver will expire in {n} days" when we set an expire time different from "never"', () => {
    const component = getShallowComponent({ expiryTime: '7' });
    expect(component.find('.iq-add-waiver-form__expiration-days-diff')).toHaveText('This waiver will expire in 7 days');
  });

  it('not renders the message "This waiver will expire in {n} days" when we set an expire time equal to "never"', () => {
    const component = getShallowComponent({ expiryTime: 'never' });
    expect(component.find('.iq-add-waiver-form__expiration-days-diff')).toHaveText('');
  });

  it('renders the message "This waiver will expire in {n} days" when we add a custom date', () => {
    const dayAfterToday = moment().add(1, 'days').format('YYYY-MM-DD');
    const component = getShallowComponent({
      expiryTime: 'custom',
      customExpiryTime: nxDateInputStateHelpers.initialState(dayAfterToday),
    });
    const customExpiryTimeSection = component.find('.iq-add-waiver-form__date-input');
    expect(customExpiryTimeSection).toExist();
    expect(component.find('.iq-add-waiver-form__expiration-days-diff')).toHaveText('This waiver will expire in 1 days');
  });

  it('does not render the message "This waiver will expire in {n} days" when we do not add a custom date', () => {
    const component = getShallowComponent({
      expiryTime: 'custom',
      customExpiryTime: nxDateInputStateHelpers.initialState(''),
    });
    const customExpiryTimeSection = component.find('.iq-add-waiver-form__date-input');
    expect(customExpiryTimeSection).toExist();
    expect(component.find('.iq-add-waiver-form__expiration-days-diff')).toHaveText('');
  });

  it('renders a NxDateInput when user select the "custom" option from the Waiver Expiration', () => {
    const component = getShallowComponent({ expiryTime: 'custom' });
    const customExpiryTimeSection = component.find('.iq-add-waiver-form__date-input');
    const dayAfterToday = moment().add(1, 'days').format('YYYY-MM-DD');
    expect(customExpiryTimeSection).toExist();
    customExpiryTimeSection.simulate('change', dayAfterToday);
    expect(setCustomExpiryTimeSpy).toHaveBeenCalledWith(dayAfterToday);
  });

  it('calls saveWaiver when the user has a valid custom waiver expiration', () => {
    const preventDefaultSpy = jasmine.createSpy('preventDefault');
    const dayAfterToday = moment().add(1, 'days').format('YYYY-MM-DD');
    const component = getShallowComponent({
      expiryTime: 'custom',
      customExpiryTime: nxDateInputStateHelpers.initialState(dayAfterToday),
    });
    const customExpiryTimeSection = component.find('.iq-add-waiver-form__date-input');
    const form = component.find('.nx-form');
    expect(customExpiryTimeSection).toExist();
    form.simulate('submit', { preventDefault: preventDefaultSpy });
    expect(saveWaiverSpy).toHaveBeenCalledWith(
      'violationId',
      'application',
      'id1',
      'waiver comments',
      'EXACT_COMPONENT',
      dayAfterToday
    );
  });

  it('prevents calls to saveWaiver when the user has an invalid custom waiver expiration', () => {
    const dayBeforeToday = moment().subtract(1, 'days').format('YYYY-MM-DD');
    const component = getShallowComponent({
      expiryTime: 'custom',
      customExpiryTime: nxDateInputStateHelpers.initialState(dayBeforeToday),
    });
    const customExpiryTimeSection = component.find('.iq-add-waiver-form__date-input');
    const form = component.find('.nx-form');
    expect(customExpiryTimeSection).toExist();
    form.simulate('submit', {
      preventDefault: () => {},
    });
    expect(saveWaiverSpy).not.toHaveBeenCalled();
  });

  it('calls `setExpiryTime` when the expiry time is changed', function () {
    const component = getShallowComponent(),
      selectComponent = component.find(NxFormSelect),
      mockEvent = { currentTarget: { value: '7' } };

    selectComponent.simulate('change', mockEvent);

    expect(setExpiryTimeSpy).toHaveBeenCalledWith('7');
  });

  it('renders a fieldset with a text area for the comments', function () {
    const component = getShallowComponent(),
      commentsSection = component.find('.iq-add-waiver-form__comments'),
      textArea = commentsSection.find(NxTextInput);

    expect(commentsSection.find(NxFieldset)).toExist();
    expect(textArea).toHaveProp('type', 'textarea');
    expect(textArea).toHaveProp('value', 'waiver comments');
    expect(textArea).toHaveProp('isPristine', true);
  });

  it('calls `setWaiverComment` when the comments change', function () {
    const component = getShallowComponent(),
      commentsSection = component.find('.iq-add-waiver-form__comments'),
      textArea = commentsSection.find(NxTextInput);

    textArea.simulate('change', 'Foo');
    expect(setWaiverCommentSpy).toHaveBeenCalledWith('Foo');
  });

  it('renders a btn bar with action buttons in the footer', function () {
    const component = getShallowComponent(),
      buttonBar = component.find('.nx-footer .nx-btn-bar'),
      buttons = buttonBar.find(NxButton);

    expect(buttons.length).toBe(2);

    expect(buttons.at(0)).toHaveProp('id', 'add-waiver-cancel');
    expect(buttons.at(0)).toHaveProp('onClick', jasmine.any(Function));
    expect(buttons.at(0)).toHaveText('Cancel');

    expect(buttons.at(1)).toHaveProp('id', 'add-waiver-submit');
    expect(buttons.at(1)).toHaveProp('type', 'submit');
    expect(buttons.at(1)).toHaveProp('variant', 'primary');
    expect(buttons.at(1)).toHaveText('Submit');
  });

  describe('it calls `saveWaiver` when form is submitted', function () {
    const preventDefaultSpy = jasmine.createSpy('preventDefault');

    it('passes null as expiryTime if never is chosen as expiry time', function () {
      const component = getShallowComponent({ expiryTime: 'never' }),
        form = component.find('.nx-form');

      form.simulate('submit', { preventDefault: preventDefaultSpy });
      expect(saveWaiverSpy).toHaveBeenCalledWith(
        'violationId',
        'application',
        'id1',
        'waiver comments',
        'EXACT_COMPONENT',
        null
      );
    });

    it('passes the number of days chosen for the expiry time', function () {
      let component = getShallowComponent(),
        form = component.find('.nx-form');

      form.simulate('submit', { preventDefault: preventDefaultSpy });
      expect(saveWaiverSpy).toHaveBeenCalledWith(
        'violationId',
        'application',
        'id1',
        'waiver comments',
        'EXACT_COMPONENT',
        7
      );

      component = getShallowComponent({
        selectedWaiverScope: {
          id: 'idOrg',
          name: 'target2',
          label: 'Organization',
          type: 'organization',
        },
        expiryTime: '30',
      });
      form = component.find('.nx-form');
      form.simulate('submit', { preventDefault: preventDefaultSpy });
      expect(saveWaiverSpy).toHaveBeenCalledWith(
        'violationId',
        'organization',
        'idOrg',
        'waiver comments',
        'EXACT_COMPONENT',
        30
      );
    });
  });

  it('calls `cancelAction` when cancel button is clicked', function () {
    const preventDefaultSpy = jasmine.createSpy('preventDefault');
    const component = getShallowComponent();
    const cancelButton = component.find('#add-waiver-cancel');

    cancelButton.simulate('click', { preventDefault: preventDefaultSpy });
    expect(cancelActionSpy).toHaveBeenCalled();
  });

  it('renders an LoadError when submitError is present', function () {
    const submitErrorObject = new Error('an error');
    const component = getShallowComponent({ submitError: submitErrorObject }),
      loadError = component.find(LoadError);

    expect(loadError).toHaveProp('error', submitErrorObject);
  });
});
