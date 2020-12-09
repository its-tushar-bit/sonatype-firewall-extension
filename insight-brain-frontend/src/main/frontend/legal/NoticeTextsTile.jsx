/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faChevronRight } from '@fortawesome/free-solid-svg-icons';
import { componentPropType } from './advancedLegalPropTypes';

export default function NoticeTextsTile(props) {
  const {
    component
  } = props;

  return (
    <section id="notice-texts-tile" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Notice Texts</h2>
        </div>
      </header>
      <div className="nx-tile-content legal-files">
        { component.licenseLegalData.noticeFiles.map(createItem) }
      </div>
    </section>
  );
}

const createItem = (notice, index) => {
  return <div className="legal-file" key={ index }>
    <span className="legal-file-path">{ notice.relPath }</span>
    <span className="nx-tile__actions">
      <a href="">View More Details <NxFontAwesomeIcon icon={ faChevronRight }/></a>
    </span>
    <blockquote className="legal-file-content">{ notice.content }</blockquote>
  </div>;
};

NoticeTextsTile.propTypes = {
  component: componentPropType
};
