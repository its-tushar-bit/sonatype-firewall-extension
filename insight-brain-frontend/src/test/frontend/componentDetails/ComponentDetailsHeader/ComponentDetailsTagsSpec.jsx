/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { NxTooltip } from '@sonatype/react-shared-components';
import { ComponentDetailsTags } from '../../../../main/frontend/componentDetails/ComponentDetailsHeader';
import ComponentFormatTag from '../../../../main/frontend/react/tag/ComponentFormatTag';
import DependencyTypeTag from '../../../../main/frontend/react/tag/DependencyTypeTag';
import ComponentLabelTag from '../../../../main/frontend/react/tag/ComponentLabelTag';

describe('ComponentDetailsTags', () => {
  let minimalProps;
  let getShallowComponent;
  let getShallowComponentNoProps;
  const mockLabels = [
    {
      id: 'orange-label',
      label: 'Orange Label',
      description: 'It is the orange label',
      color: 'orange',
    },
    {
      id: 'purple-label',
      label: 'Purple Label',
      description: 'It is the purple label',
      color: 'purple',
    },
  ];

  beforeEach(() => {
    minimalProps = {
      format: 'maven',
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ComponentDetailsTags, minimalProps);
    getShallowComponentNoProps = enzymeUtils.getShallowComponent(ComponentDetailsTags);
  });

  it('renders a component thats root element has the component-details-header__tags class', () => {
    const component = getShallowComponent();
    expect(component).toExist();
    const el = component.first();
    expect(el).toHaveClassName('component-details-header__tags');
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

  it('does not render if there is no format, dependencyType, isInnerSource=true or labels props passed', () => {
    const component = getShallowComponentNoProps();
    expect(component).toBeEmptyRender();
  });

  it('only renders format tag if `format` prop is passed', () => {
    const format = 'maven';

    const component = getShallowComponentNoProps({ format });
    expect(component.find(ComponentFormatTag)).toExist();

    const componentWithout = getShallowComponentNoProps({ dependencyType: 'transitive', labels: mockLabels });
    expect(componentWithout.find(ComponentFormatTag)).not.toExist();
  });

  it('only renders dependency type tag if `dependencyType` prop is passed and is not `"unknown"`', () => {
    const dependencyType = 'direct';

    const component = getShallowComponentNoProps({ dependencyType });
    expect(component.find(DependencyTypeTag)).toExist();

    const componentWithout = getShallowComponentNoProps({ format: 'maven', labels: mockLabels });
    expect(componentWithout.find(DependencyTypeTag)).not.toExist();
  });

  describe('innerSource tag', () => {
    it('only renders dependency type innerSource tag if `isInnerSource` prop is passed as `true`', () => {
      const component = getShallowComponentNoProps({ isInnerSource: true });
      expect(component.find(DependencyTypeTag)).toExist();
      expect(component.find(DependencyTypeTag).findWhere((tag) => tag.prop('type') === 'innerSource')).toExist();
    });

    it('does NOT render a dependency type innerSource tag if `isInnerSource` prop is passed as `false`', () => {
      const component = getShallowComponentNoProps({
        format: 'maven',
        labels: mockLabels,
        dependencyType: 'transitive',
        isInnerSource: false,
      });
      expect(component.find(DependencyTypeTag).findWhere((tag) => tag.prop('type') === 'innerSource')).not.toExist();
    });

    it('does NOT render a dependency type innerSource tag no `isInnerSource` prop is passed', () => {
      const component = getShallowComponentNoProps({
        format: 'maven',
        labels: mockLabels,
        dependencyType: 'transitive',
      });
      expect(component.find(DependencyTypeTag).findWhere((tag) => tag.prop('type') === 'innerSource')).not.toExist();
    });
  });

  it('does not render dependency type tag if `dependencyType` prop is `"unknown"`', () => {
    const dependencyType = 'unknown';

    const component = getShallowComponent({ dependencyType });
    expect(component.find(DependencyTypeTag)).not.toExist();
  });

  it('only renders label tags if `labels` prop is passed and has more than 0 label objects', () => {
    const labels = mockLabels;

    const component = getShallowComponentNoProps({ labels });
    expect(component.find(ComponentLabelTag)).toExist();

    const componentWithEmptyArray = getShallowComponentNoProps({ labels: [] });
    expect(componentWithEmptyArray.find(ComponentLabelTag)).not.toExist();

    const componentWithout = getShallowComponentNoProps({ format: 'maven', dependencyType: 'transitive' });
    expect(componentWithout.find(ComponentLabelTag)).not.toExist();
  });

  it('renders a label tag for every label object passed in the labels prop', () => {
    const labels = mockLabels;

    const component = getShallowComponentNoProps({ labels });
    expect(component.find(ComponentLabelTag).length).toBe(mockLabels.length);
  });

  it('renders a NxTooltip for every label object passed in the labels prop', () => {
    const labels = mockLabels;

    const component = getShallowComponentNoProps({ labels });
    expect(component.find(NxTooltip).length).toBe(mockLabels.length);
  });
});
