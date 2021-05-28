/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configureStore from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import { copyrightState } from './copyrightCommonState';
import CopyrightDetailsHeader from '../../../../main/frontend/legal/copyright/CopyrightDetailsHeader';
import { copyrightDetailsStateNameSuffix } from '../../../../main/frontend/legal/copyright/copyrightDetailsUtils';
import { mergeDeepRight } from 'ramda';

describe('CopyrightDetailsHeaderContainer', function () {
  let store, state, vdom, CopyrightDetailsHeaderContainer, loadComponentAndCopyrightDetailsMock;

  beforeEach(function () {
    state = copyrightState;
    loadComponentAndCopyrightDetailsMock = jasmine
      .createSpy('loadComponentAndCopyrightDetails')
      .and.returnValue({ type: 'FOO' });
    CopyrightDetailsHeaderContainer = require('inject-loader!../../../../main/frontend/legal/copyright/CopyrightDetailsHeaderContainer')(
      {
        './componentCopyrightDetailsActions': {
          loadComponentAndCopyrightDetails: loadComponentAndCopyrightDetailsMock,
        },
      }
    ).default;

    store = configureStore()(() => state);
    vdom = <CopyrightDetailsHeaderContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('loading', 'loading');
    expect(wrapper).toHaveProp('error', 'error');
    expect(wrapper).toHaveProp('hash', 'fooHash');
    expect(wrapper).toHaveProp('ownerType', 'organization');
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).not.toHaveProp('stageTypeId');
    expect(wrapper).toHaveProp('copyrightIndex', '12');
  });

  it('maps the state slice to props with stageTypeId routing', () => {
    store = configureStore()(() => mergeDeepRight(state, { router: { currentParams: { stageTypeId: 'build' } } }));
    vdom = <CopyrightDetailsHeaderContainer store={store} />;

    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('loading', 'loading');
    expect(wrapper).toHaveProp('error', 'error');
    expect(wrapper).toHaveProp('hash', 'fooHash');
    expect(wrapper).toHaveProp('ownerType', 'organization');
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).toHaveProp('stageTypeId', 'build');
    expect(wrapper).toHaveProp('copyrightIndex', '12');
  });

  it('correctly maps the action creators to the CopyrightDetailsHeaderContainer props', function () {
    const wrapper = shallow(vdom).dive();
    const loadComponentAndCopyrightDetailsCreator = wrapper.prop('loadComponentAndCopyrightDetails');

    expect(loadComponentAndCopyrightDetailsCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadComponentAndCopyrightDetailsCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);
  });

  it('renders CopyrightDetailsHeader component', function () {
    const copyrightDetailsHeader = shallow(vdom).find(CopyrightDetailsHeader);
    expect(copyrightDetailsHeader).toExist();
  });

  it('handles route switch when current state has changed', () => {
    state = {
      ...copyrightState,
      router: {
        currentState: { name: 'ComponentOverview' },
        currentParams: { hash: 'fooHash', applicationPublicId: 'appId' },
        prevParams: {
          hash: 'fooHash',
          ownerType: 'organization',
          ownerId: 'org',
          copyrightIndex: '12',
        },
        prevState: { name: copyrightDetailsStateNameSuffix },
      },
    };

    store = configureStore()(() => state);
    vdom = <CopyrightDetailsHeaderContainer store={store} />;

    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('hash', 'fooHash');
    expect(wrapper).toHaveProp('ownerType', 'organization');
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).toHaveProp('copyrightIndex', '12');
  });
});
