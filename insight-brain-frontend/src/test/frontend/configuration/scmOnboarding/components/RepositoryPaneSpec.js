/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import RepositoryPane from '../../../../../main/frontend/configuration/scmOnboarding/components/RepositoryPane';

describe('RepositoryPane', function () {
  let minimalProps,
      getShallowComponent,
      mock$State;

  beforeEach(() => {
    mock$State = jasmine.createSpyObj('$state', ['get', 'href']);

    minimalProps = {$state: mock$State};

    getShallowComponent = enzymeUtils.getShallowComponent(RepositoryPane, minimalProps);
  });

  it('displays add org button', () => {
    const component = getShallowComponent(),
        button = component.find('#repository-pane-add-org');

    expect(button).toExist();
  });

  it('shows modal when clicking add org button', () => {
    const props = {
      setIsNewOrganizationModalVisible: jasmine.createSpy('setIsNewOrganizationModalVisible'),
      isNewOrganizationModalVisible: false
    };
    const component = getShallowComponent(props),
        button = component.find('#repository-pane-add-org');

    button.simulate('click');

    expect(props.setIsNewOrganizationModalVisible).toHaveBeenCalled();
  });

});
