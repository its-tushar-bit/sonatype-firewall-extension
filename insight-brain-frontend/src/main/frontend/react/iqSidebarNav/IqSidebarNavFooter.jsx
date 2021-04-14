/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

export default function IqSidebarNavFooter(props) {
  const { productName, releaseNumber } = props;

  const productNameClasses =
      'iq-sidebar-nav-footer__product-name nx-global-sidebar__expanded-content visual-testing-ignore';

  return (
    <footer className="iq-sidebar-nav-footer">
      <div className="iq-sidebar-nav-footer__product-info">
        { productName &&
          <span className={productNameClasses}>
            {productName}
          </span>
        }
        {' '}
        {
          releaseNumber &&
          <span className="iq-sidebar-nav-footer__release-number visual-testing-ignore">
            Release {releaseNumber}
          </span>
        }
      </div>
      <div className="iq-sidebar-nav-footer__powered">
        Powered by Nexus IQ Server
      </div>
      <div className="iq-sidebar-nav-footer__created">
        Created by Sonatype
      </div>
    </footer>
  );
}

IqSidebarNavFooter.propTypes = {
  productName: PropTypes.string,
  releaseNumber: PropTypes.string
};
