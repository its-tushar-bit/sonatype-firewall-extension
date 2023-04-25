/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { shallow } from 'enzyme';
import { NxGlobalSidebarFooter } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../../enzymeUtils';
import IqSidebarNavFooter from '../../../../main/frontend/react/iqSidebarNav/IqSidebarNavFooter';

describe('IqSidebarNavFooter', function () {
  let getShallowComponent;

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(IqSidebarNavFooter, { isShowVersionEnabled: true });
  });

  it('renders an NxGlobalSidebarFooter', function () {
    const component = getShallowComponent();

    expect(component).toMatchSelector(NxGlobalSidebarFooter);
  });

  it('passes a fragment as the releaseText which contains the releaseNumber', function () {
    const component = getShallowComponent({ releaseNumber: '2' }),
      releaseText = component.prop('releaseText');

    function Fixture() {
      return releaseText;
    }

    expect(shallow(<Fixture />)).toIncludeText('Release 2');
  });

  it('does not display release when version is disabled', function () {
    const component = getShallowComponent({ releaseNumber: '2', isShowVersionEnabled: false }),
      releaseText = component.prop('releaseText');

    function Fixture() {
      return releaseText;
    }

    expect(shallow(<Fixture />)).not.toIncludeText('Release 2');
  });

  it('displays release when version is enabled', function () {
    const component = getShallowComponent({ releaseNumber: '2', isShowVersionEnabled: true }),
      releaseText = component.prop('releaseText');

    function Fixture() {
      return releaseText;
    }

    expect(shallow(<Fixture />)).toIncludeText('Release 2');
  });
});
