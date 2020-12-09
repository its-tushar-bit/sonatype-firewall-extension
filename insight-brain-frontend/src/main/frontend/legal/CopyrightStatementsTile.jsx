/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { componentPropType } from './advancedLegalPropTypes';

export default function CopyrightStatementsTile(props) {
  const {
    component
  } = props;

  return (
    <section id="copyright-statements-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Copyright Statements</h2>
        </div>
        <div className="nx-tile__actions">
          <a href="">View Details</a>
        </div>
      </header>
      <div className="nx-tile-content">
        <ul className="nx-list">
          { component.licenseLegalData.copyrights.map(createItem) }
        </ul>
      </div>
    </section>
  );
}

const createItem = (copyright, index) => {
  return (
    <li className="nx-list__item" key={ index }>
      <span className="nx-list__text">
        { copyright }
      </span>
    </li>
  );
};

CopyrightStatementsTile.propTypes = {
  component: componentPropType
};
