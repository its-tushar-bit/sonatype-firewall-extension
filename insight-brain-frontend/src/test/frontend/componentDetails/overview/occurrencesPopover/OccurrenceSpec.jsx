/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxList } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../../../enzymeUtils';
import Occurrence from '../../../../../main/frontend/componentDetails/overview/occurrencesPopover/Occurrence';

describe('Occurrence', function () {
  let minimalProps, getShallowComponent;

  beforeEach(function () {
    minimalProps = {
      occurrence: {
        basename: 'a-component',
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(Occurrence, minimalProps);
  });

  it('renders an NxList.Item with basename', () => {
    const component = getShallowComponent();
    expect(component).toMatchSelector(NxList.Item);
    const baseName = component.find('.iq-occurrence__basename');
    expect(baseName).toHaveText('a-component');
  });

  it('renders an NxList.Item with dirname if provided', () => {
    const component = getShallowComponent({
      occurrence: {
        basename: 'a-component',
        dirname: '/a/directory/path',
      },
    });
    expect(component).toMatchSelector(NxList.Item);
    const baseName = component.find('.iq-occurrence__basename');
    expect(baseName).toHaveText('a-component');
    const dirname = component.dive().find('.iq-occurrence__dirname');
    expect(dirname).toHaveText(' located at /a/directory/path');
  });

  it('renders an NxList.Item with dependency modifiers if provided', () => {
    const component = getShallowComponent({
      occurrence: {
        basename: 'a-component',
        dirname: '/a/directory/path',
        isDependency: true,
      },
    });
    expect(component).toMatchSelector(NxList.Item);
    const baseName = component.find('.iq-occurrence__basename');
    expect(baseName).toHaveText('Dependency a-component');
    const dirname = component.dive().find('.iq-occurrence__dirname');
    expect(dirname).toHaveText(' located at Module /a/directory/path');
  });
});
