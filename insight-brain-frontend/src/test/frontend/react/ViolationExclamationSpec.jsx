/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { faExclamationCircle } from '@fortawesome/free-solid-svg-icons';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';

import ViolationExclamation from '../../../main/frontend/react/ViolationExclamation';
import * as enzymeUtils from '../enzymeUtils';

describe('ViolationExclamation', function() {
  const minimalProps = { threatLevelCategory: 'unspecified' },
      getShallowComponent = enzymeUtils.getShallowComponent(ViolationExclamation, minimalProps);

  it('renders a non-fixed width exclamation NxFontAwesomeIcon', function() {
    const component = getShallowComponent();

    expect(component).toMatchSelector(NxFontAwesomeIcon);
    expect(component).not.toHaveProp('fixedWidth');
    expect(component).toHaveProp('icon', faExclamationCircle);
  });

  it('has a iq-violation-exclamation class', function() {
    expect(getShallowComponent()).toHaveClassName('iq-violation-exclamation');
  });

  it('has an iq-violation-exclamation modifier class based on the threatLevelCategory', function() {
    expect(getShallowComponent()).toHaveClassName('iq-violation-exclamation--unspecified');
    expect(getShallowComponent({ threatLevelCategory: 'none' })).toHaveClassName('iq-violation-exclamation--none');
    expect(getShallowComponent({ threatLevelCategory: 'low' })).toHaveClassName('iq-violation-exclamation--low');
    expect(getShallowComponent({ threatLevelCategory: 'moderate' }))
        .toHaveClassName('iq-violation-exclamation--moderate');
    expect(getShallowComponent({ threatLevelCategory: 'severe' })).toHaveClassName('iq-violation-exclamation--severe');
    expect(getShallowComponent({ threatLevelCategory: 'critical' }))
        .toHaveClassName('iq-violation-exclamation--critical');
  });

  it('has an iq-violation-exclamation--disabled modifier class threatLevelCategory is disabled', function() {
    expect(getShallowComponent({ threatLevelCategory: 'disabled' })).toHaveClassName(
        'iq-violation-exclamation--disabled');
  });
});
