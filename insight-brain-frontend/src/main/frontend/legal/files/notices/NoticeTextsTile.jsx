/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { legalFilesPropType } from '../../advancedLegalPropTypes';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';
import NoticesModalContainer from './NoticesModalContainer';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';

export default function NoticeTextsTile(props) {
  const {
    setShowNoticesModal,
    noticeFiles,
    showNoticesModal
  } = props;

  const isNoticePresent = () => noticeFiles.length > 0;

  const classes = classnames('nx-tile-content', { 'license-no-legal-elements-text': !isNoticePresent() });

  return (
    <section id="notice-texts-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Notice Texts</h2>
        </div>
        <div className="nx-tile__actions">
          <NxButton id="edit-notices" variant="tertiary" onClick={ () => setShowNoticesModal(true) }>
            <NxFontAwesomeIcon icon={ isNoticePresent() ? faPen : faPlus }/>
            <span>{ isNoticePresent() ? 'Edit' : 'Add' }</span>
          </NxButton>
        </div>
        { showNoticesModal && <NoticesModalContainer/> }
      </header>
      <div className={ classes }>
        { isNoticePresent() ? noticeFiles.map(createItem) : 'None found' }
      </div>
    </section>
  );
}

const createItem = (notice, index) => {
  return <section id={ 'notice-section-' + index } key={ index } className="nx-tile-subsection legal-file">
    { notice.relPath &&
    <header className="nx-tile-subsection__header">
      <h3 id={ 'notice-path-' + index } className="nx-h3">{ notice.relPath }</h3>
    </header> }
    <span className="nx-tile__actions">
      <a href="">View More Details <NxFontAwesomeIcon icon={ faChevronRight }/></a>
    </span>
    <blockquote id={ 'notice-text-' + index } className="legal-file-content">{ notice.originalContent }</blockquote>
  </section>;
};

NoticeTextsTile.propTypes = {
  setShowNoticesModal: PropTypes.func.isRequired,
  noticeFiles: legalFilesPropType,
  showNoticesModal: PropTypes.bool.isRequired
};
