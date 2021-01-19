/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseObligationAttributionTile from '../../../main/frontend/legal/LicenseObligationAttributionTile';

describe('LicenseObligationAttributionTile component', function() {
  let getShallowComponent,
      setAttributionTextSpy = jasmine.createSpy('setAttributionTextSpy'),
      setObligationFulfilledSpy = jasmine.createSpy('setObligationFulfilledSpy'),
      setScopeSpy = jasmine.createSpy('setScopeSpy');

  const minimalProps = {
    setAttributionText: setAttributionTextSpy,
    setObligationFulfilled: setObligationFulfilledSpy,
    setScope: setScopeSpy,
    name: 'License Obligation Name',
    attributionText: '',
    obligationFulfilled: false,
    scope: 'ROOT_ORGANIZATION_ID'
  };

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseObligationAttributionTile, minimalProps);
  });

  it('renders a header with a label matching the license obligation name', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('.nx-h2')).toHaveText('Attribution for "License Obligation Name"');
  });

  it('renders an Add Attribution button if there is no attribution', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('.nx-tile__actions span')).toHaveText('Add Attribution');
  });

  it('renders an Edit Attribution button if there is an attribution', function() {
    const wrapper = getShallowComponent({ attributionText: 'Some attribution text.' });
    expect(wrapper.find('.nx-tile__actions span')).toHaveText('Edit Attribution');
  });

  it('renders `None added` as content if there is no attribution', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('.nx-tile-content')).toHaveText('None added');
  });

  it('renders the attribution text as content if there is an attribution', function() {
    const attribution = { attributionText: 'Some attribution text.' };
    const wrapper = getShallowComponent(attribution);
    expect(wrapper.find('.nx-tile-content')).toHaveText(attribution.attributionText);
  });
});
