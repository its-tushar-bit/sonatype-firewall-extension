/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';

import {
  NxButton,
  NxList,
  NxAccordion,
  NxStatefulAccordion,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { VersionChangePropTypes } from '../overviewTypes';
import { faCheck } from '@fortawesome/pro-solid-svg-icons';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import GoldenStar from 'MainRoot/img/golden-star.svg';
import { RECOMMENDED_NON_BREAKING, RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES } from './recommendedVersionsUtils';

export function RecommendedVersionsList({ versionChanges, actualVersion, handleCompare }) {
  if (!versionChanges || versionChanges.length === 0) {
    return <span>There are no suggested versions for this component</span>;
  }

  const [suggestedVersion, ...alternateVersions] = versionChanges;
  const hasAlternateVersions = !isNilOrEmpty(alternateVersions);

  return (
    <>
      <NxList className="iq-version-container" emptyMessage="There are no suggested versions for this component">
        <VersionListItem
          versionItem={suggestedVersion}
          actualVersion={actualVersion}
          handleCompare={handleCompare}
          isSuggestedVersion={true}
        />
      </NxList>
      {hasAlternateVersions && (
        <NxStatefulAccordion defaultOpen={false} className="iq-alternate-versions-accordion">
          <NxAccordion.Header>
            <NxAccordion.Title>Alternate Versions</NxAccordion.Title>
          </NxAccordion.Header>
          <NxList className="iq-version-container">
            {alternateVersions.map((alternateVersion) => (
              <VersionListItem
                key={alternateVersion.id}
                versionItem={alternateVersion}
                actualVersion={actualVersion}
                handleCompare={handleCompare}
                isSuggestedVersion={false}
              />
            ))}
          </NxList>
        </NxStatefulAccordion>
      )}
    </>
  );
}

function VersionListItem({ versionItem, actualVersion, handleCompare, isSuggestedVersion }) {
  const { id, version, type, text, isGolden } = versionItem || {};

  if (!version || actualVersion === version) {
    return null;
  }

  return (
    <NxList.Item key={id} className="iq-version-item">
      <NxList.Text>{isSuggestedVersion ? `Upgrade to ${version}` : `Version ${version}`}</NxList.Text>
      <NxList.Subtext>
        {isGolden && <GoldenVersionText />}
        <VersionChecklist type={type} text={text} />
      </NxList.Subtext>
      <NxList.Actions>
        <NxButton
          variant={isSuggestedVersion ? 'secondary' : 'tertiary'}
          onClick={() => handleCompare(version)}
          id={id}
        >
          Compare
        </NxButton>
      </NxList.Actions>
    </NxList.Item>
  );
}

function CheckIcon() {
  return <NxFontAwesomeIcon icon={faCheck} className="iq-recommended-version-check-icon" />;
}

function GoldenVersionText() {
  return (
    <div className="iq-golden-version-text-container">
      <img src={GoldenStar} />
      <span className="iq-golden-version-text">Golden Version</span>
    </div>
  );
}

function VersionChecklist({ type, text }) {
  const hasNoBreakingChanges = type === RECOMMENDED_NON_BREAKING || type === RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES;
  return (
    <div className="iq-version-checklist">
      {hasNoBreakingChanges ? (
        <ul className="iq-version-checklist-list">
          {text.split(',').map((text) => (
            <li className="iq-version-checklist-item" key={text}>
              <CheckIcon />
              {text.trim()}
            </li>
          ))}
        </ul>
      ) : (
        text
      )}
    </div>
  );
}

VersionChecklist.propTypes = {
  type: PropTypes.string.isRequired,
  text: PropTypes.string.isRequired,
};

VersionListItem.propTypes = {
  versionChanges: PropTypes.arrayOf(VersionChangePropTypes).isRequired,
  actualVersion: PropTypes.string.isRequired,
  handleCompare: PropTypes.func.isRequired,
  versionItem: PropTypes.shape(VersionChangePropTypes).isRequired,
  isSuggestedVersion: PropTypes.bool.isRequired,
};

RecommendedVersionsList.propTypes = {
  versionChanges: PropTypes.arrayOf(VersionChangePropTypes).isRequired,
  actualVersion: PropTypes.string.isRequired,
  handleCompare: PropTypes.func.isRequired,
};
