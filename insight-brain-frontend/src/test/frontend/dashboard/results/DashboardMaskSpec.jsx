/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import DashboardMask from '../../../../main/frontend/dashboard/results/dashboardMask/DashboardMask';

describe('DashboardMask', function() {
  let getShallowComponent;

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(DashboardMask, {});
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('has informative text', () => {
    const component = getShallowComponent();

    expect(component).toHaveText('Please apply or revert filter to see results');
  });
});
