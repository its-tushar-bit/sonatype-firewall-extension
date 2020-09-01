/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxButton,
  NxTextInput,
  NxRadio,
  NxLoadError
} from '@sonatype/react-shared-components';

import * as enzymeUtils from '../enzymeUtils';
import AddWaiverForm from '../../../main/frontend/waivers/AddWaiverForm';
import ArtifactNameDisplay from '../../../main/frontend/react/ArtifactNameDisplay';
import ViolationExclamation from '../../../main/frontend/react/ViolationExclamation';
import VulnerabilityDetailsModalContainer
  from '../../../main/frontend/vulnerabilityDetails/VulnerabilityDetailsModalContainer';

describe('AddWaiverForm', function() {
  let minimalProps,
      getShallowComponent,
      saveWaiverSpy,
      setWaiverCommentSpy,
      setWaiverScopeSpy,
      setApplyToAllComponentsSpy,
      openVulnerabilityDetailsModalSpy;

  beforeEach(function() {
    saveWaiverSpy = jasmine.createSpy('saveWaiver');
    setWaiverCommentSpy = jasmine.createSpy('setWaiverComment');
    setWaiverScopeSpy = jasmine.createSpy('setWaiverScope');
    setApplyToAllComponentsSpy = jasmine.createSpy('setApplyToAllComponents');
    openVulnerabilityDetailsModalSpy = jasmine.createSpy('loadAddWaiverDataSpy');

    minimalProps = {
      applyToAllComponents: false,
      artifactName: 'artifact name',
      componentName: 'component name',
      constraintName: 'constraint name',
      policyName: 'policy name',
      policyViolationId: 'violationId',
      reasons: ['reason1', 'reason2'],
      threatLevelCategory: 'severe',
      waiverComments: {
        value: 'waiver comments',
        isPristine: true
      },
      availableWaiverScopes: [
        {
          id: 'id1',
          name: 'target1',
          label: 'Application',
          type: 'application'
        },
        {
          id: 'id2',
          name: 'target2',
          label: 'Organization',
          type: 'organization'
        }
      ],
      selectedWaiverScope: {
        id: 'id1',
        name: 'target1',
        label: 'Application',
        type: 'application'
      },
      setWaiverScope: setWaiverScopeSpy,
      setApplyToAllComponents: setApplyToAllComponentsSpy,
      setWaiverComment: setWaiverCommentSpy,
      saveWaiver: saveWaiverSpy,
      openVulnerabilityDetailsModal: openVulnerabilityDetailsModalSpy,
      vulnerabilityId: 'CVE-12345'
    };

    getShallowComponent = enzymeUtils.getShallowComponent(AddWaiverForm, minimalProps);
  });

  it('renders a form with the appropriate classes', function() {
    const component = getShallowComponent();
    expect(component).toMatchSelector('form.nx-tile-content.nx-form.iq-add-waiver-form');
  });

  it('renders a tile header with the artifact and component names', function() {
    const component = getShallowComponent(),
        tileHeader = component.find('.iq-add-waiver-form__component'),
        artifactNameComponent = tileHeader.find(ArtifactNameDisplay),
        componentName = tileHeader.find('.nx-tile-header__subtitle');

    expect(tileHeader).toHaveClassName('.nx-tile-header');
    expect(artifactNameComponent).toHaveProp('artifactName', 'artifact name');
    expect(componentName).toHaveText('component name');
  });

  it('renders a form group with the policy info', function() {
    const component = getShallowComponent(),
        policySection = component.find('.iq-add-waiver-form__policy'),
        ViolationExclamationComponent = policySection.find(ViolationExclamation),
        policySpan = policySection.find('.iq-threat-level');

    expect(policySection).toHaveClassName('.nx-form-group');
    expect(ViolationExclamationComponent).toHaveProp('threatLevelCategory', 'severe');
    expect(policySpan).toHaveClassName('.iq-threat-level--severe');
    expect(policySpan).toHaveText('policy name');
  });

  it('renders a form group with the constraint info', function() {
    const component = getShallowComponent(),
        constraintSection = component.find('.iq-add-waiver-form__constraint'),
        constraintName = constraintSection.find('.iq-read-only-data');

    expect(constraintName).toHaveText('constraint name');
  });

  it('renders a form group with the conditions', function() {
    const component = getShallowComponent(),
        conditionsSection = component.find('.iq-add-waiver-form__conditions'),
        reasons = conditionsSection.find('.iq-read-only-data');

    expect(reasons.prop('children').length).toBe(minimalProps.reasons.length);
    expect(reasons.childAt(0)).toHaveText('reason1');
    expect(reasons.childAt(1)).toHaveText('reason2');
  });

  it('renders a link to see vulnerability details and opens the modal on click', function() {
    const component = getShallowComponent(),
        vulnerabilityDetailsSection = component.find('.iq-add-waiver-form__vulnerability_details_link'),
        vulnerabilityDetailsLink = vulnerabilityDetailsSection.find('a');

    expect(vulnerabilityDetailsLink).toHaveText('See Security Vulnerability Details');
    expect(openVulnerabilityDetailsModalSpy).not.toHaveBeenCalled();
    vulnerabilityDetailsLink.simulate('click');
    expect(openVulnerabilityDetailsModalSpy).toHaveBeenCalledWith({
      vulnerabilityId: 'CVE-12345'
    });
  });

  it('renders a VulnerabilityDetailsModalContainer IFF vulnerabilityId is truthy', function() {
    let component = getShallowComponent();
    expect(component.find(VulnerabilityDetailsModalContainer)).toExist();

    component = getShallowComponent({vulnerabilityId: null});
    expect(component.find(VulnerabilityDetailsModalContainer)).not.toExist();
    expect(component.find('.iq-add-waiver-form__vulnerability_details_link')).not.toExist();
  });

  it('renders a fieldset with NxRadios for the WaiverTargets', function() {
    const component = getShallowComponent(),
        waiverTargetsSection = component.find('.iq-add-waiver-form__scope'),
        targetRadios = waiverTargetsSection.find(NxRadio);

    expect(waiverTargetsSection).toHaveClassName('.nx-fieldset');

    expect(targetRadios.length).toBe(2);
    expect(targetRadios.at(0)).toHaveProp('name', 'add-waiver-target');
    expect(targetRadios.at(0)).toHaveProp('value', 'id1');
    expect(targetRadios.at(0)).toHaveProp('isChecked', true);
    expect(targetRadios.at(0)).toHaveText('Application - target1');

    expect(targetRadios.at(1)).toHaveProp('name', 'add-waiver-target');
    expect(targetRadios.at(1)).toHaveProp('value', 'id2');
    expect(targetRadios.at(1)).toHaveProp('isChecked', false);
    expect(targetRadios.at(1)).toHaveText('Organization - target2');
  });

  it('calls `setWaiverScope` when the waiver target is changed', function() {
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
      type: 'organization'
    });
  });

  it('renders a fieldset with NxRadios for the waiver components', function() {
    const component = getShallowComponent(),
        componentsSection = component.find('.iq-add-waiver-form__components'),
        componentRadios = componentsSection.find(NxRadio);

    expect(componentsSection).toHaveClassName('.nx-fieldset');
    expect(componentRadios.length).toBe(2);

    expect(componentRadios.at(0)).toHaveProp('name', 'add-waiver-components');
    expect(componentRadios.at(0)).toHaveProp('value', 'component name');
    expect(componentRadios.at(0)).toHaveProp('isChecked', true);
    expect(componentRadios.at(0)).toHaveText('component name');

    expect(componentRadios.at(1)).toHaveProp('name', 'add-waiver-components');
    expect(componentRadios.at(1)).toHaveProp('value', 'ALL_COMPONENTS');
    expect(componentRadios.at(1)).toHaveProp('isChecked', false);
    expect(componentRadios.at(1)).toHaveText('All Components');
  });

  it('calls `setApplyToAllComponents` when the waiver components are changed', function() {
    const component = getShallowComponent(),
        componentsSection = component.find('.iq-add-waiver-form__components'),
        componentRadios = componentsSection.find(NxRadio),
        component1 = componentRadios.at(0),
        component2 = componentRadios.at(1);

    expect(component1).toHaveProp('isChecked', true);
    expect(component2).toHaveProp('isChecked', false);

    component2.simulate('change', 'ALL_COMPONENTS');
    expect(setApplyToAllComponentsSpy).toHaveBeenCalledWith(true);

    component2.simulate('change', 'component name');
    expect(setApplyToAllComponentsSpy).toHaveBeenCalledWith(false);
  });

  it('renders a form group with a text area for the comments', function() {
    const component = getShallowComponent(),
        commentsSection = component.find('.iq-add-waiver-form__comments'),
        textArea = commentsSection.find(NxTextInput);

    expect(commentsSection).toHaveClassName('.nx-form-group');
    expect(textArea).toHaveProp('type', 'textarea');
    expect(textArea).toHaveProp('value', 'waiver comments');
    expect(textArea).toHaveProp('isPristine', true);
  });

  it('calls `setWaiverComment` when the comments change', function() {
    const component = getShallowComponent(),
        commentsSection = component.find('.iq-add-waiver-form__comments'),
        textArea = commentsSection.find(NxTextInput);

    textArea.simulate('change', 'Foo');
    expect(setWaiverCommentSpy).toHaveBeenCalledWith('Foo');
  });

  it('renders a btn bar with action buttons', function() {
    const component = getShallowComponent(),
        buttonBar = component.find('.nx-btn-bar'),
        buttons = buttonBar.find(NxButton);

    expect(buttonBar).toHaveClassName('.nx-btn-bar--forms');
    expect(buttons.length).toBe(2);

    expect(buttons.at(0)).toHaveProp('id', 'id-waiver-cancel');
    expect(buttons.at(0)).toHaveProp('onClick', jasmine.any(Function));
    expect(buttons.at(0)).toHaveText('Cancel');

    expect(buttons.at(1)).toHaveProp('id', 'add-waiver-submit');
    expect(buttons.at(1)).toHaveProp('type', 'submit');
    expect(buttons.at(1)).toHaveProp('variant', 'primary');
    expect(buttons.at(1)).toHaveText('Submit');
  });

  it('calls `saveWaiver` when form is submitted using the chosen selectedWaiverScope', function() {
    const preventDefaultSpy = jasmine.createSpy('preventDefault');
    let component, form;

    component = getShallowComponent(),
    form = component.find('.nx-form');
    form.simulate('submit', { preventDefault: preventDefaultSpy });
    expect(saveWaiverSpy).toHaveBeenCalledWith('violationId', 'application', 'id1', 'waiver comments', false);

    component = getShallowComponent({
      selectedWaiverScope: {
        id: 'idOrg',
        name: 'target2',
        label: 'Organization',
        type: 'organization'
      }
    });
    form = component.find('.nx-form');
    form.simulate('submit', { preventDefault: preventDefaultSpy });
    expect(saveWaiverSpy).toHaveBeenCalledWith('violationId', 'organization', 'idOrg', 'waiver comments', false);
  });

  it('renders an NxLoadError when submitError is present', function() {
    const component = getShallowComponent({ submitError: 'an error' }),
        buttonBar = component.find('.nx-btn-bar'),
        loadError = buttonBar.find(NxLoadError);

    expect(loadError).toHaveProp('error', 'an error');
  });
});
