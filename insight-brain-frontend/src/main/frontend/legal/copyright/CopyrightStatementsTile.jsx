/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { availableScopesPropType, componentPropType } from '../advancedLegalPropTypes';
import { NxButton, NxFontAwesomeIcon, NxAccordion, useToggle } from '@sonatype/react-shared-components';
import { faAngleRight, faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';
import CopyrightOverrideFormContainer from './CopyrightOverrideFormContainer';
import * as PropTypes from 'prop-types';

export default function CopyrightStatementsTile(props) {
  const {
    component,
    showEditCopyrightOverrideModal,
    ownerType,
    ownerId,
    hash,
    componentIdentifier,
    stageTypeId,
    $state,

    //actions
    setDisplayCopyrightOverrideModal,
  } = props;

  const createAttributionModal = <CopyrightOverrideFormContainer />;

  const noDataText = () => (
    <li className={'license-no-legal-elements-text'}>
      {component.licenseLegalData.copyrights.length > 0 ? 'None enabled' : 'None found'}
    </li>
  );

  const copyrightDetailsTargetState = () => {
    if (stageTypeId) {
      return 'legal.stageTypeComponentCopyrightDetails.copyrightDetails';
    }
    return hash
      ? 'legal.componentCopyrightDetails.copyrightDetails'
      : 'legal.componentCopyrightDetailsByComponentIdentifier.copyrightDetails';
  };

  const createItem = (copyright, index) => {
    return (
      <li className="nx-list__item nx-list__item--link" key={index}>
        <a
          className="nx-list__link"
          href={$state.href(copyrightDetailsTargetState(), {
            ownerType,
            ownerId,
            hash,
            componentIdentifier,
            stageTypeId,
            copyrightIndex: index,
          })}
        >
          <span className="nx-list__text">{copyright.content}</span>
          <NxFontAwesomeIcon icon={faAngleRight} className="nx-chevron" />
        </a>
      </li>
    );
  };

  const isCopyrightPresent = component.licenseLegalData.copyrights.filter((c) => c.status === 'enabled').length > 0;

  const [open, toggleOpen] = useToggle(true);

  return (
    <React.Fragment>
      <NxAccordion open={open} onToggle={toggleOpen} id="copyright-statements-tile">
        <NxAccordion.Header>
          <NxAccordion.Title>Copyright Notices</NxAccordion.Title>
          <div className="nx-btn-bar">
            <NxButton id="edit-copyrights" variant="tertiary" onClick={() => setDisplayCopyrightOverrideModal(true)}>
              <NxFontAwesomeIcon icon={isCopyrightPresent ? faPen : faPlus} />
              <span>{isCopyrightPresent ? 'Edit' : 'Add'}</span>
            </NxButton>
          </div>
        </NxAccordion.Header>
        <ul className="nx-list nx-list--clickable">
          {isCopyrightPresent
            ? component.licenseLegalData.copyrights
                .map((c, index) => [c, index])
                .filter((pair) => pair[0].status === 'enabled')
                .map((pair) => createItem(pair[0], pair[1]))
            : noDataText()}
        </ul>
      </NxAccordion>
      {showEditCopyrightOverrideModal && createAttributionModal}
    </React.Fragment>
  );
}

CopyrightStatementsTile.propTypes = {
  component: componentPropType,
  availableScopes: availableScopesPropType,
  showEditCopyrightOverrideModal: PropTypes.bool,
  setDisplayCopyrightOverrideModal: PropTypes.func.isRequired,
  ownerType: PropTypes.string.isRequired,
  ownerId: PropTypes.string.isRequired,
  hash: PropTypes.string,
  componentIdentifier: PropTypes.string,
  stageTypeId: PropTypes.string,
  $state: PropTypes.object.isRequired,
};
