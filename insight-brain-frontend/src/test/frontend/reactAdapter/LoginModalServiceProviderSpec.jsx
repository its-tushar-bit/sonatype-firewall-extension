/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { shallow } from 'enzyme';
import { NxButton } from '@sonatype/react-shared-components';
import withLoginModalService from 'MainRoot/reactAdapter/LoginModalServiceProvider';
import LoginModalService from 'MainRoot/user/LoginModal/LoginModalService';

describe('LoginModalServiceProvider (higher-order component)', function () {
  let TestComponent;

  beforeEach(function () {
    TestComponent = function TestComponent() {
      return <NxButton data-testid="test-component">(component content)</NxButton>;
    };
  });

  it('renders the wrapped component', function () {
    const ShallowHOC = withLoginModalService(TestComponent);
    const wrapper = shallow(<ShallowHOC LoginModalService={LoginModalService} />);
    const button = wrapper.find(TestComponent).dive().find(NxButton);

    expect(wrapper.find(TestComponent)).toExist();
    expect(button).toExist();
    expect(button).toHaveText('(component content)');
  });

  it('renders component with expected props from the service', function () {
    const ShallowHOC = withLoginModalService(TestComponent);
    const wrapper = shallow(<ShallowHOC LoginModalService={LoginModalService} />);

    expect(wrapper.find(TestComponent)).toHaveProp('onSubmit');
    expect(wrapper.find(TestComponent)).toHaveProp('onClickSSO');
    expect(wrapper.find(TestComponent)).toHaveProp('onDismiss');
  });
});
