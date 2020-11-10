/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import NoticeTextsTile from '../../../main/frontend/legal/NoticeTextsTile';

describe('NoticeTextsTile component', function() {

  let getShallowComponent;

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(NoticeTextsTile);
  });

  it('renders a header with label `Notice Texts`', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('Notice Texts');
  });
});
