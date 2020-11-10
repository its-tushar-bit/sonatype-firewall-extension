/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import ComponentOverviewTile from '../../../main/frontend/legal/ComponentOverviewTile';

describe('ComponentOverviewTile component', function() {

  let getShallowComponent;

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(ComponentOverviewTile);
  });

  it('renders a header with label `Component Overview`', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('Component Overview');
  });
});
