/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {availableScopesPropType, componentPropType} from '../advancedLegalPropTypes';
import {NxButton, NxFontAwesomeIcon} from '@sonatype/react-shared-components';
import {faPen} from '@fortawesome/pro-solid-svg-icons';
import CopyrightOverrideFormContainer from './CopyrightOverrideFormContainer';
import * as PropTypes from 'prop-types';

export default function CopyrightStatementsTile(props) {
  const {
    component,
    showEditCopyrightOverrideModal,

    //actions
    setDisplayCopyrightOverrideModal
  } = props;

  const createAttributionModal = <CopyrightOverrideFormContainer/>;

  return (
    <section id="copyright-statements-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Copyright Statements</h2>
        </div>
        <div className="nx-tile__actions">
          <NxButton id="edit-copyrights" variant="tertiary" onClick={() => setDisplayCopyrightOverrideModal(true)}>
            <NxFontAwesomeIcon icon={faPen}/>
            <span>Edit Copyrights</span>
          </NxButton>
        </div>
        {showEditCopyrightOverrideModal && createAttributionModal}
      </header>
      <div className="nx-tile-content">
        <ul className="nx-list">
          {component.licenseLegalData.copyrights.filter(c => c.status === 'enabled').map(createItem)}
        </ul>
      </div>
    </section>
  );
}

const createItem = (copyright, index) => {
  return <li className="nx-list__item" key={index}>
    <span className="nx-list__text">
      { copyright.content }
    </span>
  </li>;
};

CopyrightStatementsTile.propTypes =
  {
    component: componentPropType,
    availableScopes: availableScopesPropType,
    showEditCopyrightOverrideModal: PropTypes.bool,
    setDisplayCopyrightOverrideModal: PropTypes.func.isRequired
  }
;
