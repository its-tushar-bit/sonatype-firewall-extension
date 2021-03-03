/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import NewOrganizationModal
  from '../../../../../main/frontend/configuration/scmOnboarding/components/NewOrganizationModal';
import {NxButton, NxForm, NxModal} from '@sonatype/react-shared-components';

describe('NewOrganizationModal', function () {
  let minimalProps,
      getShallowComponent,
      getMountedComponent;

  beforeEach(() => {
    minimalProps = {
      setIsNewOrganizationModalVisible: jasmine.createSpy('setIsNewOrganizationModalVisible'),
      addOrganization: jasmine.createSpy('addOrganization')
    };

    getShallowComponent = enzymeUtils.getShallowComponent(NewOrganizationModal, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(NewOrganizationModal, minimalProps);
  });

  it('renders a narrow NxModal', () => {
    const component = getShallowComponent(),
        modal = component.find(NxModal);

    expect(modal).toExist();
    expect(modal).toHaveProp('id', 'new-organization-modal');
    expect(modal).toHaveProp('variant', 'narrow');
  });

  it('renders an error message', () => {
    const component = getMountedComponent({addOrganizationError: 'BOOM'}),
        loadError = component.find(NxForm);

    expect(loadError).toHaveProp('submitError', 'BOOM');
  });

  it('cancel button closes modal', () => {
    const component = getMountedComponent(),
        cancelButton = component.find(NxButton).first();

    cancelButton.simulate('click');

    expect(minimalProps.setIsNewOrganizationModalVisible).toHaveBeenCalled();
  });
});
