/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { availableScopesPropType, componentPropType } from '../advancedLegalPropTypes';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faAngleRight, faPen } from '@fortawesome/pro-solid-svg-icons';
import CopyrightOverrideFormContainer from './CopyrightOverrideFormContainer';
import * as PropTypes from 'prop-types';

export default function CopyrightStatementsTile(props) {
  const {
    component,
    showEditCopyrightOverrideModal,
    ownerType,
    ownerId,
    hash,
    $state,

    //actions
    setDisplayCopyrightOverrideModal,
  } = props;

  const createAttributionModal = <CopyrightOverrideFormContainer />;

  const isCopyrightPresent = () =>
    component.licenseLegalData.copyrights.filter((c) => c.status === 'enabled').length > 0;

  const noDataText = () => (component.licenseLegalData.copyrights.length > 0 ? 'None enabled' : 'None found');

  const createItem = (copyright, index) => {
    return (
      <li className="nx-list__item nx-list__item--link" key={index}>
        <a
          className="nx-list__link"
          href={$state.href('componentCopyrightDetails.copyrightDetails', {
            ownerType,
            ownerId,
            hash,
            copyrightIndex: index,
          })}
        >
          <span className="nx-list__text">{copyright.content}</span>
          <NxFontAwesomeIcon icon={faAngleRight} className="nx-chevron" />
        </a>
      </li>
    );
  };

  return (
    <section id="copyright-statements-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Copyright Statements</h2>
        </div>
        <div className="nx-tile__actions">
          <NxButton id="edit-copyrights" variant="tertiary" onClick={() => setDisplayCopyrightOverrideModal(true)}>
            <NxFontAwesomeIcon icon={faPen} />
            <span>Edit</span>
          </NxButton>
        </div>
        {showEditCopyrightOverrideModal && createAttributionModal}
      </header>
      <div className="nx-tile-content">
        <ul className="nx-list nx-list--clickable">
          {isCopyrightPresent()
            ? component.licenseLegalData.copyrights
                .map((c, index) => [c, index])
                .filter((pair) => pair[0].status === 'enabled')
                .map((pair) => createItem(pair[0], pair[1]))
            : noDataText()}
        </ul>
      </div>
    </section>
  );
}

CopyrightStatementsTile.propTypes = {
  component: componentPropType,
  availableScopes: availableScopesPropType,
  showEditCopyrightOverrideModal: PropTypes.bool,
  setDisplayCopyrightOverrideModal: PropTypes.func.isRequired,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  hash: PropTypes.string.isRequired,
  $state: PropTypes.object.isRequired,
};
