/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../enzymeUtils';
import { mount } from 'enzyme';
import React from 'react';
import TransitiveViolationsSummary from '../../../main/frontend/violation/TransitiveViolationsSummary';

describe('TransitiveViolationsSummary', function () {
  let minimalProps, getShallowComponent;

  beforeEach(function () {
    minimalProps = {
      threatCounts: {
        critical: 5,
        severe: 4,
        moderate: 3,
        low: 2,
        none: 1,
      },
      threatCountsTotal: 15,
      componentCount: 1,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(TransitiveViolationsSummary, minimalProps);
  });

  it('displays the correct total violations and components', function () {
    let wrapper = getShallowComponent({
      ...minimalProps,
      threatCountsTotal: 3,
      componentCount: 2,
    });
    expect(wrapper.find('.nx-read-only__data').at(0)).toHaveText(
      3 + ' total violations brought in by ' + 2 + ' components'
    );
    wrapper = getShallowComponent({
      ...minimalProps,
      threatCountsTotal: 0,
      componentCount: 0,
    });
    expect(wrapper.find('.nx-read-only__data').at(0)).toHaveText(
      0 + ' total violations brought in by ' + 0 + ' components'
    );
    wrapper = getShallowComponent({
      ...minimalProps,
      threatCountsTotal: 1,
      componentCount: 1,
    });
    expect(wrapper.find('.nx-read-only__data').at(0)).toHaveText(
      1 + ' total violation brought in by ' + 1 + ' component'
    );
  });

  it('displays the correct transitive violation counts', function () {
    const wrapper = mount(<TransitiveViolationsSummary {...minimalProps} />);
    const countsContainer = wrapper.find('.nx-threat-counter-container').at(0);
    expect(countsContainer.find('.nx-threat-counter--critical dt')).toHaveText('Critical');
    expect(countsContainer.find('.nx-threat-counter--critical dd')).toHaveText('5');
    expect(countsContainer.find('.nx-threat-counter--severe dt')).toHaveText('Severe');
    expect(countsContainer.find('.nx-threat-counter--severe dd')).toHaveText('4');
    expect(countsContainer.find('.nx-threat-counter--moderate dt')).toHaveText('Moderate');
    expect(countsContainer.find('.nx-threat-counter--moderate dd')).toHaveText('3');
    expect(countsContainer.find('.nx-threat-counter--low dt')).toHaveText('Low');
    expect(countsContainer.find('.nx-threat-counter--low dd')).toHaveText('2');
    expect(countsContainer.find('.nx-threat-counter--none dt')).toHaveText('None');
    expect(countsContainer.find('.nx-threat-counter--none dd')).toHaveText('1');
  });

  it('hides zero counts', function () {
    const wrapper = mount(
      <TransitiveViolationsSummary
        {...{
          ...minimalProps,
          threatCounts: {
            critical: 0,
            severe: 0,
            moderate: 0,
            low: 0,
            none: 0,
            threatCountsTotal: 0,
            componentCount: 0,
          },
        }}
      />
    );
    expect(wrapper.find('.nx-threat-counter-container')).not.toExist();
  });
});
