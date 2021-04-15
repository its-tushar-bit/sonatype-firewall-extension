/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import NewOrganizationModal from '../../../../../main/frontend/configuration/scmOnboarding/components/NewOrganizationModal';
import {
  NxButton,
  NxForm,
  NxModal,
  NxTextInput,
} from '@sonatype/react-shared-components';

describe('NewOrganizationModal', function () {
  let minimalProps, getShallowComponent, getMountedComponent;

  beforeEach(() => {
    minimalProps = {
      setIsNewOrganizationModalVisible: jasmine.createSpy(
        'setIsNewOrganizationModalVisible'
      ),
      addOrganization: jasmine.createSpy('addOrganization'),
    };

    getShallowComponent = enzymeUtils.getShallowComponent(
      NewOrganizationModal,
      minimalProps
    );
    getMountedComponent = enzymeUtils.getMountedComponent(
      NewOrganizationModal,
      minimalProps
    );
  });

  it('renders a narrow NxModal', () => {
    const component = getShallowComponent(),
      modal = component.find(NxModal);

    expect(modal).toExist();
    expect(modal).toHaveProp('id', 'new-organization-modal');
    expect(modal).toHaveProp('variant', 'narrow');
  });

  it('renders an error message', () => {
    const component = getMountedComponent({ addOrganizationError: 'BOOM' }),
      loadError = component.find(NxForm);

    expect(loadError).toHaveProp('submitError', 'BOOM');
  });

  it('cancel button closes modal', () => {
    const component = getMountedComponent(),
      cancelButton = component.find(NxButton).first();

    cancelButton.simulate('click');

    expect(minimalProps.setIsNewOrganizationModalVisible).toHaveBeenCalled();
  });

  it('calls addOrganization with provided org name', () => {
    const addOrganization = jasmine.createSpy('addOrganization');
    const component = getShallowComponent({ addOrganization }),
      newOrgInput = component.find(NxTextInput).first();

    // when the org name is submitted
    newOrgInput.simulate('change', 'something');
    component.find(NxForm).invoke('onSubmit')();

    // then the addOrganization action is invoked
    expect(addOrganization).toHaveBeenCalledWith('something');
  });

  it('Validates there are no invalid characters', () => {
    const addOrganization = jasmine.createSpy('addOrganization');
    const component = getShallowComponent({ addOrganization }),
      newOrgInput = component.find(NxTextInput).first();

    // when special characters are submitted
    newOrgInput.simulate('change', '!!!!');

    // then a validation error is generated
    expect(component.find(NxForm).prop('validationErrors')).toEqual([
      'Organization name contains an invalid character',
    ]);
  });

  it('Validates the input is non-empty', () => {
    const addOrganization = jasmine.createSpy('addOrganization');
    const component = getShallowComponent({ addOrganization }),
      newOrgInput = component.find(NxTextInput).first();

    // when no input is provided
    newOrgInput.simulate('change', '');

    // then a validation error is generated
    expect(component.find(NxForm).prop('validationErrors')).toEqual([
      'Must be non-empty',
    ]);
  });

  it('trims leading and trailing whitespace', () => {
    const addOrganization = jasmine.createSpy('addOrganization');
    const component = getShallowComponent({ addOrganization }),
      newOrgInput = component.find(NxTextInput).first();

    // when trailing or leading whitspace is submitted
    newOrgInput.simulate('change', '   orgname   ');
    component.find(NxForm).invoke('onSubmit')();

    // then whitespace is trimmed
    expect(addOrganization).toHaveBeenCalledWith('orgname');
  });
});
