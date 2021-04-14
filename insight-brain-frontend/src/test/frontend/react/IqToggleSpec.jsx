/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxToggle, NxTooltip } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../enzymeUtils';
import IqToggle from '../../../main/frontend/react/IqToggle';

describe('IqToggle (React)', function() {
  let minimalProps,
      getShallowComponent,
      onChangeSpy;

  beforeEach(function() {
    onChangeSpy = jasmine.createSpy('onChange');

    minimalProps = {
      toggleLabel: 'Aggregate by component',
      toggleTooltip: 'Aggregate Tooltip',
      inputId: 'aggregateId',
      isChecked: true,
      onChange: onChangeSpy
    };

    getShallowComponent = enzymeUtils.getShallowComponent(IqToggle, minimalProps);
  });

  it('renders an NxToggle within an NxTooltip with the supplied props', function() {
    const toggle = getShallowComponent();

    expect(toggle).toMatchSelector(NxToggle);
    expect(toggle).toHaveProp('inputId', 'aggregateId');
    expect(toggle).toHaveProp('isChecked', true);

    const tooltip = toggle.childAt(0);
    expect(tooltip.find('span')).toHaveText('Aggregate by component');
    expect(tooltip).toMatchSelector(NxTooltip);
    expect(tooltip).toHaveProp('title', 'Aggregate Tooltip');

    expect(onChangeSpy).not.toHaveBeenCalled();
  });

  it('calls a function when NxToggle is clicked', function() {
    const toggle = getShallowComponent();
    expect(toggle).toMatchSelector(NxToggle);
    expect(onChangeSpy).not.toHaveBeenCalled();
    toggle.simulate('change');
    expect(onChangeSpy).toHaveBeenCalled();
  });
});
