/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxButton } from '@sonatype/react-shared-components';
import React from 'react';
import configureStore from 'redux-mock-store';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';
import withStoreProvider from '../../../main/frontend/reactAdapter/StoreProvider';

describe('StoreProvider (higher-order component)', function() {

  let mockNgRedux, TestComponent;

  beforeEach(function() {
    mockNgRedux = configureStore()(() => {});
    TestComponent = function TestComponent() {
      return <NxButton>(component content)</NxButton>;
    };
  });

  it('renders a Provider with $ngRedux as the store', function() {
    const StoreProviderHOC = withStoreProvider(TestComponent);
    const wrapper = shallow(<StoreProviderHOC $ngRedux={mockNgRedux} />);
    expect(wrapper).toMatchSelector(Provider);
    expect(wrapper.find(Provider)).toHaveProp('store', mockNgRedux);
    expect(wrapper).toHaveProp('store', mockNgRedux);
  });

  it('renders the wrapped component', function() {
    const StoreProviderHOC = withStoreProvider(TestComponent);
    const wrapper = shallow(<StoreProviderHOC $ngRedux={mockNgRedux} />);

    expect(wrapper.find(TestComponent)).toExist();
    const button = wrapper.find(TestComponent).dive().find(NxButton);
    expect(button).toExist();
    expect(button).toHaveText('(component content)');
  });

  it('passes down props to the wrapped component', function() {
    const StoreProviderHOC = withStoreProvider(TestComponent);
    const wrapper = shallow(<StoreProviderHOC $ngRedux={mockNgRedux} testProp={'test-prop'} />);
    expect(wrapper.find(TestComponent)).toHaveProp('testProp', 'test-prop');
  });

  it('does not pass down store to the wrapped component', function() {
    const StoreProviderHOC = withStoreProvider(TestComponent);
    const wrapper = shallow(<StoreProviderHOC $ngRedux={mockNgRedux} />);
    expect(wrapper.find(TestComponent)).not.toHaveProp('store');
    expect(wrapper.find(TestComponent)).not.toHaveProp('$ngRedux');
  });

  it('includes the display name of the wrapped component in its displayName', function() {
    const storeProviderHOC = withStoreProvider(TestComponent);
    expect(storeProviderHOC.displayName).toBe('withStoreProvider(TestComponent)');
  });

  it('has a display name of "withStoreProvider(AnonymousComponent)" when wrapping an unnamed component', function() {
    const storeProviderHOC = withStoreProvider(() => {
      return <NxButton>anonymous button</NxButton>;
    });
    expect(storeProviderHOC.displayName).toBe('withStoreProvider(AnonymousComponent)');
  });

});
