/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import { always } from 'ramda';

import DashboardHeatMapCell from '../../../../main/frontend/dashboard/results/DashboardHeatMapCell';

describe('DashboardHeatMapCell', function () {
  let getShallowComponent;

  beforeEach(function () {
    const minimalProps = {
      colorStyler: {
        isWhiteText: always(true),
        getColor: always('test-background-color'),
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(
      DashboardHeatMapCell,
      minimalProps
    );
  });

  it('renders score passed as children prop', function () {
    const component = getShallowComponent({
      children: 123,
    });

    expect(component.childAt(0).text()).toEqual('123');
  });

  it('renders score passed as threatScore even if children prop is provided', function () {
    const component = getShallowComponent({
      children: 123,
      threatScore: 456,
    });

    expect(component.childAt(0).text()).toEqual('456');
  });

  it('does not render the score if chevron prop is provided', function () {
    const component = getShallowComponent({
      children: 123,
      threatScore: 456,
      chevron: true,
    });

    expect(component.children().length).toBe(0);
  });

  it('render NxTableCell with chevron prop if chevron prop is provided', function () {
    const component = getShallowComponent({
      children: 123,
      chevron: true,
    });

    expect(component).toHaveDisplayName('NxTableCell');
    expect(component).toHaveProp('chevron', true);
  });

  it('render NxTableCell without chevron prop if chevron prop is not provided', function () {
    const component = getShallowComponent({
      children: 123,
    });

    expect(component).toHaveDisplayName('NxTableCell');
    expect(component).not.toHaveProp('chevron', true);
  });

  it('renders grey-text class if provided score is zero', function () {
    const component = getShallowComponent({
      children: 0,
    });

    expect(component).toHaveClassName('grey-text');
  });

  it('renders text color class from colorStyler if provided score is not zero', function () {
    const component = getShallowComponent({
      children: 1,
    });

    expect(component).toHaveClassName('white-text');
  });

  it('renders no text color classes if chevron prop is provided', function () {
    const component = getShallowComponent({
      children: 1,
      chevron: true,
    });

    expect(component).not.toHaveClassName('white-text');
    expect(component).not.toHaveClassName('grey-text');
  });

  it('renders backgroundColor style returned by colorStyler', function () {
    const component = getShallowComponent({
      children: 1,
      chevron: true,
    });

    expect(component).toHaveProp('style', {
      backgroundColor: 'test-background-color',
    });
  });
});
