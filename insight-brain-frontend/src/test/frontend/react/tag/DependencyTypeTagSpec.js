/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import DependencyTypeTag from '../../../../main/frontend/react/tag/DependencyTypeTag';
import { NxTag } from '@sonatype/react-shared-components';

describe('DependencyTypeTag', function () {
  let getShallow;

  beforeEach(function () {
    getShallow = enzymeUtils.getShallowComponent(DependencyTypeTag);
  });

  it('displays direct dependency tag if it is direct and not innersource', function () {
    const component = getShallow({ isDirect: true, isInnerSource: false });
    const tag = component.find(NxTag);

    expect(tag).toHaveProp('children', 'Direct Dependency');
  });

  it('displays transitive dependency tag if it is not direct and not innersource', function () {
    const component = getShallow({ isDirect: false, isInnerSource: false });
    const tag = component.find(NxTag);

    expect(tag).toHaveProp('children', 'Transitive Dependency');
  });

  it('displays innersource tag if it is innersource', function () {
    const component = getShallow({ isDirect: true, isInnerSource: true });
    const tag = component.find(NxTag);

    expect(tag).toHaveProp('children', 'InnerSource');
  });
});
