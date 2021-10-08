/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { faTag } from '@fortawesome/pro-solid-svg-icons';
import { NxFontAwesomeIcon, NxTag } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../../enzymeUtils';
import { TagWithFontAwesomeIcon } from '../../../../main/frontend/react/tag/ComponentLabelTag';

describe('TagWithFontAwesomeIcon', function () {
  let minimalProps, getShallowTag;

  beforeEach(function () {
    minimalProps = {
      children: 'test-label',
      faIcon: faTag,
    };

    getShallowTag = enzymeUtils.getShallowComponent(TagWithFontAwesomeIcon, minimalProps);
  });

  it('renders an NxTag with a color', function () {
    const component = getShallowTag({ color: 'test-color' });
    expect(component).toMatchSelector(NxTag);
    expect(component).toHaveProp('color', 'test-color');
  });

  it('renders an NxFontAwesomeIcon with the given icon', function () {
    const iconComponent = getShallowTag().find(NxFontAwesomeIcon);
    expect(iconComponent).toMatchSelector(NxFontAwesomeIcon);
    expect(iconComponent).toHaveProp('icon', minimalProps.faIcon);
  });

  it('renders a span with the given label text (aka "children")', function () {
    const labelText = getShallowTag().find('span');
    expect(labelText).toHaveText(minimalProps.children);
  });
});
