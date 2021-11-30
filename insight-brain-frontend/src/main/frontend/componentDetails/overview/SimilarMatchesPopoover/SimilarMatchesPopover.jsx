/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import PropTypes from 'prop-types';
import { NxList, NxTextLink } from '@sonatype/react-shared-components';

import { IqPopover } from '../../../react/IqPopover';
import ComponentDisplay from '../../../ComponentDisplay/ReactComponentDisplay';

const SimilarMatchesPopover = ({ showSimilarMatchesPopover, onClose, similarMatches }) => {
  if (!showSimilarMatchesPopover) {
    return null;
  }

  const contentText = `A similar match is found using various, proprietary matching algorithms. Best Match shows the most likely match and
  Other Matches shows all components found to be similar.`;
  const componentIdentificationInfoLink = 'http://links.sonatype.com/products/nxiq/doc/component-identification';
  const contentLink = (
    <NxTextLink external href={componentIdentificationInfoLink}>
      Learn more about Component Identification here.
    </NxTextLink>
  );

  const [bestMatch, ...otherMatches] = similarMatches;
  const otherMatchesSection =
    otherMatches.length === 0 ? null : (
      <Fragment>
        <h3 className="nx-h3">Other Matches</h3>
        <NxList>
          {otherMatches.map((match, index) => (
            <NxList.Item className="iq-similar-match" key={index}>
              <NxList.Text>
                <ComponentDisplay component={match} />
              </NxList.Text>
            </NxList.Item>
          ))}
        </NxList>
      </Fragment>
    );

  return (
    <IqPopover id="similar-matches-popover" size="large" onClose={onClose}>
      <IqPopover.Header
        buttonId="iq-similar-matches-popover-close-btn"
        onClose={onClose}
        headerTitle="Similar Matches"
      />
      <div className="iq-similar-matches-popover-content">
        <p className="iq-similar-matches-popover-content__message">
          {contentText} {contentLink}
        </p>
        <h3 className="nx-h3">Best Match</h3>
        <NxList>
          <NxList.Item className="iq-similar-match">
            <NxList.Text>
              <ComponentDisplay component={bestMatch} />
            </NxList.Text>
          </NxList.Item>
        </NxList>
        {otherMatchesSection}
      </div>
    </IqPopover>
  );
};

const similarMatchPropType = { displayName: ComponentDisplay.propTypes.displayName };
SimilarMatchesPopover.propTypes = {
  similarMatches: PropTypes.arrayOf(PropTypes.shape(similarMatchPropType)).isRequired,
  onClose: PropTypes.func.isRequired,
  showSimilarMatchesPopover: PropTypes.bool.isRequired,
};
export default SimilarMatchesPopover;
