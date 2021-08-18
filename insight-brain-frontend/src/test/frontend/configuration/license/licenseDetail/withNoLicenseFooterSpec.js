/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxButton } from '@sonatype/react-shared-components';
import WithNoLicenseFooter from '../../../../../main/frontend/configuration/license/footers/WithNoLicenseFooter';
import * as enzymeUtils from '../../../enzymeUtils';

describe('WithNoLicenseFooter', () => {
  let minimalProps, mockFileChangeHandler, getShallowComponent;

  beforeEach(() => {
    mockFileChangeHandler = jasmine.createSpy('fileChangeHandler');
    minimalProps = {
      fileChangeHandler: mockFileChangeHandler,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(WithNoLicenseFooter, minimalProps);
  });

  it('renders a NxButton', () => {
    expect(getShallowComponent().find(NxButton)).toExist();
  });

  it('calls the fileChangeHandler on inputChange', () => {
    const shallowComponent = getShallowComponent();
    const input = shallowComponent.find('input[type="file"]');
    input.simulate('change');
    expect(mockFileChangeHandler).toHaveBeenCalledTimes(1);
  });
});
