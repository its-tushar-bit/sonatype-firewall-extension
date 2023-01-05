/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ComponentLabelTag, { TagWithFontAwesomeIcon } from 'MainRoot/react/tag/ComponentLabelTag';
import { NxTag } from '@sonatype/react-shared-components';
import { faTag } from '@fortawesome/pro-solid-svg-icons';

describe('ComponentLabelTag', function () {
  let minimalProps, getShallowTag;

  beforeEach(function () {
    minimalProps = {
      children: 'label',
    };

    getShallowTag = enzymeUtils.getShallowComponent(ComponentLabelTag, minimalProps);
  });

  it('renders a TagWithFontAwesomeIcon with the given props', function () {
    const component = getShallowTag({
      description: 'test description',
    });
    expect(component).toMatchSelector(TagWithFontAwesomeIcon);
    expect(component).toHaveProp('faIcon', faTag);
    expect(component).toHaveProp('tooltip', 'test description');
    expect(component.dive().find(NxTag)).toHaveProp('aria-label', 'Label label');
  });

  it('should map old color names to the new rsc colors', function () {
    expect(getShallowTag({ color: 'light-purple' })).toHaveProp('color', 'purple');
    expect(getShallowTag({ color: 'dark-purple' })).toHaveProp('color', 'indigo');
    expect(getShallowTag({ color: 'dark-red' })).toHaveProp('color', 'red');
    expect(getShallowTag({ color: 'light-red' })).toHaveProp('color', 'pink');
    expect(getShallowTag({ color: 'dark-blue' })).toHaveProp('color', 'blue');
    expect(getShallowTag({ color: 'light-blue' })).toHaveProp('color', 'light-blue');
    expect(getShallowTag({ color: 'dark-green' })).toHaveProp('color', 'green');
    expect(getShallowTag({ color: 'orange' })).toHaveProp('color', 'orange');
    expect(getShallowTag({ color: 'yellow' })).toHaveProp('color', 'yellow');
    expect(getShallowTag({ color: 'light-green' })).toHaveProp('color', 'lime');
  });
});
