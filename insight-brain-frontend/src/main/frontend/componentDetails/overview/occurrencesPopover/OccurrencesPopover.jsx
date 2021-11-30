/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import { NxList, NxTextLink } from '@sonatype/react-shared-components';

import IqPopover from '../../../react/IqPopover';
import Occurrence from './Occurrence';
import { parseOccurrencePathname } from '../../componentDetailsUtils';

const EXTERNAL_OCCURRENCES_INFO_LINK =
  'http://links.sonatype.com/products/nxiq/doc/component-information-panel/occurrences';

export default function OccurrencesPopover(props) {
  const { occurrences, onClose, showOccurrencesPopover } = props;

  const occurrencesDocumentationLink = (
    <NxTextLink external href={EXTERNAL_OCCURRENCES_INFO_LINK}>
      Learn more about Component Occurrences here.
    </NxTextLink>
  );

  const occurrencesInfoText =
    'Component Occurrences shows a list of file names and locations where the component was encountered.';

  const parsedPathNames = occurrences.map(parseOccurrencePathname);

  return showOccurrencesPopover ? (
    <IqPopover id="occurrences-popover" size="large" onClose={onClose}>
      <IqPopover.Header buttonId="iq-occurrences-popover-close-btn" onClose={onClose} headerTitle="Occurrences" />
      <div className="iq-occurrences-popover-content">
        <p className="iq-occurrences-popover-content__message">
          {occurrencesInfoText} {occurrencesDocumentationLink}
        </p>
        <h3 className="nx-h3">Component Occurrences</h3>
        <NxList>
          {parsedPathNames.map((occurrence) => (
            <Occurrence occurrence={occurrence} key={occurrence.basename} />
          ))}
        </NxList>
      </div>
    </IqPopover>
  ) : null;
}

OccurrencesPopover.propTypes = {
  occurrences: PropTypes.arrayOf(PropTypes.string).isRequired,
  onClose: PropTypes.func.isRequired,
  showOccurrencesPopover: PropTypes.bool.isRequired,
};
