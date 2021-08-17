/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import NoProductLicense from '../../../../../main/frontend/configuration/license/contents/NoProductLicense';
import * as enzymeUtils from '../../../enzymeUtils';

describe('NoProductLicense', () => {
  let getShallowComponent;

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(NoProductLicense);
  });

  it('renders a component with a p tag', () => {
    expect(getShallowComponent().find('p')).toExist();
  });
});
