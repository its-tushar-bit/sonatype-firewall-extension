/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';

import { ComponentDetailsHeader, Title } from '../../../../main/frontend/componentDetails/ComponentDetailsHeader';

describe('ComponentDetailsHeader', () => {
  let minimalProps;
  let getShallowComponent;

  beforeEach(() => {
    minimalProps = {};

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentDetailsHeader, minimalProps);
  });

  it('merges the className attribute to the root element if className is passed as props', () => {
    const component = getShallowComponent();
    const currentClassNames = component.first().prop('className');
    component.setProps({ className: 'my-class' });
    const el = component.first();
    expect(el).toHaveClassName(currentClassNames);
    expect(el).toHaveClassName('my-class');
  });

  it('forwards all extra props to the root element', () => {
    const component = getShallowComponent({ id: 'thisone', tabIndex: 0, 'data-testid': 'bar' });
    const el = component.first();
    expect(el).toHaveProp('id', 'thisone');
    expect(el).toHaveProp('tabIndex', 0);
    expect(el).toHaveProp('data-testid', 'bar');
  });

  describe('ComponentDetailsHeader.Title', () => {
    let minimalProps;
    let getShallowComponent;

    beforeEach(() => {
      minimalProps = {};

      getShallowComponent = enzymeUtils.getShallowComponent(Title, minimalProps);
    });

    it('merges the className attribute to the root element if className is passed as props', () => {
      const component = getShallowComponent();
      const currentClassNames = component.first().prop('className');
      component.setProps({ className: 'my-class' });
      const el = component.first();
      expect(el).toHaveClassName(currentClassNames);
      expect(el).toHaveClassName('my-class');
    });

    it('forwards all extra props to the root element', () => {
      const component = getShallowComponent({ id: 'thisone', tabIndex: 0, 'data-testid': 'bar' });
      const el = component.first();
      expect(el).toHaveProp('id', 'thisone');
      expect(el).toHaveProp('tabIndex', 0);
      expect(el).toHaveProp('data-testid', 'bar');
    });
  });
});
