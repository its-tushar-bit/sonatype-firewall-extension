/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
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
import PRStatus from 'MainRoot/components/prStatus/PRStatus';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from '../overviewSlice';
import CreatePRModal from 'MainRoot/manualPullRequest/CreatePRModal';
import { selectIsHrcReport } from 'MainRoot/applicationReport/applicationReportSelectors';

export function RecommendedVersionsList({ versionChanges, actualVersion, handleCompare, automatedRemediationStatus }) {
  const dispatch = useDispatch();
  // HRC (hosted-repository component) scans have no source manifest to open a PR against,
  // so the Create PR / retry / view-PR affordance never applies. Hide the whole PRStatus
  // widget for HRC and let the Compare button stand alone in the row.
  const isHrcReport = useSelector(selectIsHrcReport);
  const remediationStatusPollingRef = useRef(null);

  function startPRStatusPollingAndSaveReference(id) {
    if (id == null) return;

    remediationStatusPollingRef.current?.abort?.();

    remediationStatusPollingRef.current = dispatch(
      actions.startPRStatusPolling({
        id,
      })
    );
  }

  const handlePRCreated = (result) => {
    if (result?.id) {
      startPRStatusPollingAndSaveReference(result.id);
    }
  };

  const handleCreatePR = (version, breakingChangesCount) => {
    dispatch(actions.openCreatePRModal({ version, breakingChangesCount }));
  };

  const handleRetryPR = async (version) => {
    const { payload } = await dispatch(actions.createPR({ version }));
    if (payload?.data?.id) {
      startPRStatusPollingAndSaveReference(payload.data.id);
    }
  };

  useEffect(() => {
    if (automatedRemediationStatus?.status === 'PULL_REQUEST_CREATION_PENDING' && automatedRemediationStatus?.id) {
      startPRStatusPollingAndSaveReference(automatedRemediationStatus.id);
    }
  }, [automatedRemediationStatus]);

  useEffect(() => {
    return () => {
      remediationStatusPollingRef.current?.abort?.();
    };
  }, []);

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
          automatedRemediationStatus={automatedRemediationStatus}
          onCreatePR={handleCreatePR}
          onRetryPR={handleRetryPR}
          isHrcReport={isHrcReport}
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
      <CreatePRModal onSuccess={handlePRCreated} />
    </>
  );
}

function VersionListItem({
  versionItem,
  actualVersion,
  handleCompare,
  isSuggestedVersion,
  automatedRemediationStatus,
  onCreatePR,
  onRetryPR,
  isHrcReport,
}) {
  const { id, version, type, text, isGolden, breakingChangesCount } = versionItem || {};

  if (!version || actualVersion === version) {
    return null;
  }

  const handleCreatePR = () => {
    onCreatePR(version, breakingChangesCount);
  };

  const handleRetryPR = async () => {
    onRetryPR(version);
  };

  return (
    <NxList.Item key={id} className="iq-version-item">
      <NxList.Text>{isSuggestedVersion ? `Upgrade to ${version}` : `Version ${version}`}</NxList.Text>
      <NxList.Subtext>
        {isGolden && <GoldenVersionText />}
        <VersionChecklist type={type} text={text} />
      </NxList.Subtext>
      <NxList.Actions>
        {isSuggestedVersion && !isHrcReport && (
          <PRStatus
            automatedRemediationStatus={automatedRemediationStatus}
            onCreatePR={handleCreatePR}
            onRetry={handleRetryPR}
          />
        )}
        <NxButton className="nx-btn--small" variant="tertiary" onClick={() => handleCompare(version)} id={id}>
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
  versionItem: PropTypes.shape(VersionChangePropTypes),
  actualVersion: PropTypes.string.isRequired,
  handleCompare: PropTypes.func.isRequired,
  isSuggestedVersion: PropTypes.bool.isRequired,
  automatedRemediationStatus: PropTypes.object,
  onCreatePR: PropTypes.func,
  onRetryPR: PropTypes.func,
  isHrcReport: PropTypes.bool,
};

RecommendedVersionsList.propTypes = {
  versionChanges: PropTypes.arrayOf(VersionChangePropTypes),
  actualVersion: PropTypes.string.isRequired,
  handleCompare: PropTypes.func.isRequired,
  automatedRemediationStatus: PropTypes.object,
};
