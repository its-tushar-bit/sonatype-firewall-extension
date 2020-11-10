/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import ComponentLegalOverviewPage from '../../../main/frontend/legal/ComponentLegalOverviewPage';
import ComponentLegalOverviewContainer from '../../../main/frontend/legal/ComponentLegalOverviewContainer';

describe('ComponentLegalOverviewContainer', function() {
  let store,
      state,
      vdom;

  beforeEach(function() {
    state = {
      dashboard: {
        components: 'some components'
      }
    };

    store = configureStore()(() => state);
    vdom = <ComponentLegalOverviewContainer store={store}/>;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('components', 'some components');
  });

  it('renders ComponentLegalOverviewPage component', function() {
    const componentLegalOverviewPage = shallow(vdom).find(ComponentLegalOverviewPage);
    expect(componentLegalOverviewPage).toExist();
  });
});
