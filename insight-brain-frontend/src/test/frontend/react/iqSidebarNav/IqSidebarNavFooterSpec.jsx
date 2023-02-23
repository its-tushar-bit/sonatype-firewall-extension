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
    getShallowComponent = enzymeUtils.getShallowComponent(IqSidebarNavFooter, { tenantMode: 'single' });
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

  // Note: normally you might say this is the sort of detail that should be left to a visual test.  However visual
  // tests of this area are covered in ignore regions since the release number changes over time
  it('passes the productName within an iq-sidebar-nav-footer__product-name span in the releaseText', function () {
    const component = getShallowComponent({ productName: 'Cool Stuff' }),
      releaseText = component.prop('releaseText');

    function Fixture() {
      return releaseText;
    }

    const productName = shallow(<Fixture />).find('span.iq-sidebar-nav-footer__product-name');

    expect(productName).toHaveText('Cool Stuff');
  });

  it('does not display release until mode is established', function () {
    const component = getShallowComponent({ releaseNumber: '2', tenantMode: 'unknown' }),
      releaseText = component.prop('releaseText');

    function Fixture() {
      return releaseText;
    }

    expect(shallow(<Fixture />)).not.toIncludeText('Release 2');
  });

  it('does not display release when in multi tenant mode', function () {
    const component = getShallowComponent({ releaseNumber: '2', tenantMode: 'multi' }),
      releaseText = component.prop('releaseText');

    function Fixture() {
      return releaseText;
    }

    expect(shallow(<Fixture />)).not.toIncludeText('Release 2');
  });
});
