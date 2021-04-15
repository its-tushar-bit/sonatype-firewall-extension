/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faCircle } from '@fortawesome/pro-solid-svg-icons';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';

import StatusIndicatorIcon from '../../../../main/frontend/react/statusIndicatorIcon/StatusIndicatorIcon';
import * as enzymeUtils from '../../enzymeUtils';

describe('StatusIndicatorIcon', function () {
  const minimalProps = { status: false },
    getShallowComponent = enzymeUtils.getShallowComponent(StatusIndicatorIcon, minimalProps);

  it('renders a non-fixed width circle NxFontAwesomeIcon', function () {
    const component = getShallowComponent();

    expect(component).toMatchSelector(NxFontAwesomeIcon);
    expect(component).not.toHaveProp('fixedWidth');
    expect(component).toHaveProp('icon', faCircle);
  });

  it('has a iq-status-indicator-icon class', function () {
    expect(getShallowComponent()).toHaveClassName('iq-status-indicator-icon');
  });

  it('has an iq-status-indicator-icon modifier class based on the status', function () {
    expect(getShallowComponent()).not.toHaveClassName('iq-status-indicator-icon--active');

    expect(getShallowComponent({ status: true })).toHaveClassName(
      'iq-status-indicator-icon iq-status-indicator-icon--active'
    );

    expect(getShallowComponent({ status: false })).not.toHaveClassName('iq-status-indicator-icon--active');
  });
});
