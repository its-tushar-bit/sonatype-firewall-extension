/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import {NxDropdown} from '@sonatype/react-shared-components';

import ObligationStatusComponent from '../../../../main/frontend/legal/shared/ObligationStatusComponent';

describe('ObligationStatusComponent component', function() {

  let getShallowComponent;

  const minimalProps = {
    existingObligation: {
      name: 'Name of Obligation',
      status: 'FLAGGED'
    }
  };

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(ObligationStatusComponent, minimalProps);
  });

  it('displays the obligation status', function() {
    let wrapper = getShallowComponent();
    const statusDropdown = wrapper.find(NxDropdown);
    const statusLabelChildren = statusDropdown.prop('label').props['children'];
    expect(statusLabelChildren[0].props['icon'].iconName).toBe('exclamation-triangle');
    expect(statusLabelChildren[1].props['children']).toEqual('Flagged');
    const statusOptions = statusDropdown.find('button');
    expect(statusOptions.length).toBe(3);
    expect(statusOptions.at(0).childAt(1)).toHaveText('Fulfilled');
    expect(statusOptions.at(1).childAt(1)).toHaveText('Not Applicable');
    expect(statusOptions.at(2).childAt(0)).toHaveText('Unreviewed'); // No icon
  });

  it('displays the obligation name', function() {
    let wrapper = getShallowComponent();
    const nameLabel = wrapper.find('.nx-sub-label');
    expect(nameLabel).toHaveText('Change the review status of the obligation "Name of Obligation" to');
  });
});
