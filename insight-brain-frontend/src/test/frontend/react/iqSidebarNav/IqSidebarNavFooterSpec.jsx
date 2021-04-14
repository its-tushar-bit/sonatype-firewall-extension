/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import IqSidebarNavFooter from '../../../../main/frontend/react/iqSidebarNav/IqSidebarNavFooter';

describe('IqSidebarNavFooter', function() {
  let getShallowComponent;

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(IqSidebarNavFooter, {});
  });

  it('renders a footer with supplied props', function() {
    expect(getShallowComponent()).toMatchSelector('.iq-sidebar-nav-footer');
    expect(getShallowComponent().find('.iq-sidebar-nav-footer__product-name')).not.toExist();
    expect(getShallowComponent({ productName: 'product' }).find('.iq-sidebar-nav-footer__product-name'))
        .toHaveText('product');
    expect(getShallowComponent().find('.iq-sidebar-nav-footer__release-number')).not.toExist();
    expect(getShallowComponent({ releaseNumber: '10x' }).find('.iq-sidebar-nav-footer__release-number'))
        .toHaveText('Release 10x');

    expect(getShallowComponent().find('.iq-sidebar-nav-footer__powered'))
        .toHaveText('Powered by Nexus IQ Server');
    expect(getShallowComponent().find('.iq-sidebar-nav-footer__created'))
        .toHaveText('Created by Sonatype');
  });
});
