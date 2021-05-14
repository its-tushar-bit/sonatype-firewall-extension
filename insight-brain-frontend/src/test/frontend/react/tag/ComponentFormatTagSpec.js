/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ComponentFormatTag from '../../../../main/frontend/react/tag/ComponentFormatTag';

describe('ComponentFormatTag', function () {
  let getShallow;

  beforeEach(function () {
    getShallow = enzymeUtils.getShallowComponent(ComponentFormatTag);
  });

  it('displays an icon next to the tag if the format has an icon image available', function () {
    const component = getShallow({ name: 'maven' });
    const icon = component.find('img');

    expect(icon).toExist();
  });

  it('does not display an icon if the format does not have an icon image available', function () {
    const component = getShallow({ name: 'unavailableFormat' });
    const icon = component.find('img');

    expect(icon).not.toExist();
  });
});
