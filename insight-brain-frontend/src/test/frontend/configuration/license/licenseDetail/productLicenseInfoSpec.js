/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import ProductLicenseInfo from '../../../../../main/frontend/configuration/license/contents/ProductLicenseInfo';
import * as enzymeUtils from '../../../enzymeUtils';

describe('ProductLicenseDetailInfo', () => {
  let minimalProps, getShallowComponent;

  beforeEach(() => {
    minimalProps = {
      license: {},
    };
    getShallowComponent = enzymeUtils.getShallowComponent(ProductLicenseInfo, minimalProps);
  });

  it('renders a component with nx-grid-row class', () => {
    expect(getShallowComponent().find('.nx-grid-row')).toExist();
  });
});
