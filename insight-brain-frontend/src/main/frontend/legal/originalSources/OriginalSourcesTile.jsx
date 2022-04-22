/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxAccordion,
  NxButton,
  NxFontAwesomeIcon,
  NxList,
  NxTextLink,
  useToggle,
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';
import OriginalSourcesFormContainer from 'MainRoot/legal/originalSources/OriginalSourcesFormContainer';

export default function OriginalSourcesTile(props) {
  let { sourceLinks, showOriginalSourcesModal, setDisplayOriginalSourcesOverrideModal } = props;

  const areSourceLinksPresent = () => sourceLinks && sourceLinks.length > 0;
  const enabledSources = sourceLinks.filter((sourceLink) => sourceLink.status === 'enabled');

  const classes = classnames({
    'license-no-legal-elements-text': !areSourceLinksPresent(),
  });

  const [open, toggleOpen] = useToggle(true);

  const displaySource = (sourceContent) => {
    if (sourceContent !== null && (sourceContent.startsWith('https:') || sourceContent.startsWith('http:'))) {
      return (
        <NxTextLink external href={sourceContent}>
          {sourceContent}
        </NxTextLink>
      );
    }
    return sourceContent;
  };

  const createItem = (source, index) => (
    <NxList.Item key={index}>
      <NxList.Text className="license-list-item">{displaySource(source.content)}</NxList.Text>
    </NxList.Item>
  );

  const createSourceList = (displaySources) => {
    return <NxList>{displaySources.map(createItem)}</NxList>;
  };

  return (
    <React.Fragment>
      <NxAccordion open={open} onToggle={toggleOpen} id="original-sources-tile">
        <NxAccordion.Header>
          <NxAccordion.Title>Original Source Code</NxAccordion.Title>
          <div className="nx-btn-bar">
            <NxButton
              id="edit-original-sources"
              variant="tertiary"
              onClick={() => setDisplayOriginalSourcesOverrideModal(true)}
            >
              <NxFontAwesomeIcon icon={areSourceLinksPresent() ? faPen : faPlus} />
              <span>{areSourceLinksPresent() ? 'Edit' : 'Add'}</span>
            </NxButton>
          </div>
        </NxAccordion.Header>
        {enabledSources.length > 0 ? createSourceList(enabledSources) : <div className={classes}>None found</div>}
      </NxAccordion>
      {showOriginalSourcesModal && <OriginalSourcesFormContainer />}
    </React.Fragment>
  );
}

OriginalSourcesTile.propTypes = {
  sourceLinks: PropTypes.arrayOf(
    PropTypes.shape({
      content: PropTypes.string.isRequired,
      originalContent: PropTypes.string.isRequired,
      status: PropTypes.string.isRequired,
      id: PropTypes.string,
    })
  ),
  setDisplayOriginalSourcesOverrideModal: PropTypes.func.isRequired,
  showOriginalSourcesModal: PropTypes.bool.isRequired,
};
