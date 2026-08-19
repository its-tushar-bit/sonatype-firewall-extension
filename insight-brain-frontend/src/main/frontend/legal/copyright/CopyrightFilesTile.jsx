/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { componentCopyrightDetailsPropType } from '../advancedLegalPropTypes';
import { pageCount, pageRange } from './copyrightDetailsUtils';
import { NxLoadWrapper, NxPagination, NxTreeView } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { propEq } from 'ramda';

export default function CopyrightFilesTile(props) {
  const { componentCopyrightDetails, loadCopyrightContexts, hideCopyrightContext, pageChange } = props;

  function filePathTitle(filePath) {
    const multiMatchSuffix = filePath.copyrightMatches > 1 ? ` (${filePath.copyrightMatches} matches)` : '';
    return <span className="truncate-ellipsis-left">{`${filePath.filePath}${multiMatchSuffix}`}</span>;
  }

  function isFilePathOpen(filePath) {
    return componentCopyrightDetails.selectedFilePaths.includes(filePath);
  }

  function highlightCopyright(context, copyright) {
    const start = context.indexOf(copyright);
    if (start < 0) {
      return context;
    }

    const end = start + copyright.length;
    return (
      <Fragment>
        {context.slice(0, start)}
        <mark className="copyright-highlight">{context.slice(start, end)}</mark>
        {context.slice(end)}
      </Fragment>
    );
  }

  function copyrightContexts(filePathItem) {
    const matchingContextsByFilePath = componentCopyrightDetails.copyrightContexts.find(
      propEq('filePath', filePathItem.filePath)
    );

    return (
      <NxLoadWrapper
        retryHandler={() => loadCopyrightContexts(filePathItem.filePath)}
        error={componentCopyrightDetails.errorCopyrightContext}
        loading={
          componentCopyrightDetails.loadingCopyrightContext &&
          (!matchingContextsByFilePath || matchingContextsByFilePath.contexts.length === 0)
        }
      >
        {matchingContextsByFilePath &&
          matchingContextsByFilePath.contexts.map((text, index) => (
            <blockquote key={index} className="nx-blockquote copyright-preformatted">
              {highlightCopyright(text, componentCopyrightDetails.selectedCopyright.content)}
            </blockquote>
          ))}
      </NxLoadWrapper>
    );
  }

  const toggle = (filePathItem) => {
    if (!isFilePathOpen(filePathItem.filePath)) {
      loadCopyrightContexts(filePathItem.filePath);
    } else {
      hideCopyrightContext(filePathItem.filePath);
    }
  };

  function createFilePathItem(index, filePathItem) {
    return (
      <NxTreeView
        key={filePathItem.filePath}
        className="file-path-item"
        isOpen={isFilePathOpen(filePathItem.filePath)}
        onToggleCollapse={() => toggle(filePathItem)}
        triggerTooltip={filePathItem.filePath}
        triggerContent={filePathTitle(filePathItem)}
      >
        {copyrightContexts(filePathItem)}
      </NxTreeView>
    );
  }

  function hasFilePaths() {
    return componentCopyrightDetails.filePaths && componentCopyrightDetails.filePaths.length > 0;
  }

  function showingPathsHeader() {
    if (hasFilePaths() && !componentCopyrightDetails.loadingFilePaths) {
      return `Showing 
          ${pageRange(componentCopyrightDetails.filePathsPage, componentCopyrightDetails.filePaths)} of 
          ${componentCopyrightDetails.totalFileMatches} file paths`;
    }
    return '';
  }

  function filePathsPagination(filePathPageCount) {
    return (
      filePathPageCount > 1 && (
        <NxPagination
          pageCount={filePathPageCount}
          currentPage={componentCopyrightDetails.filePathsPage}
          onChange={pageChange}
        />
      )
    );
  }

  function filePathsPage() {
    const filePathPageCount = pageCount(componentCopyrightDetails.totalFileMatches);
    return (
      <Fragment>
        {componentCopyrightDetails.filePaths.map((path, index) => createFilePathItem(index, path))}
        {filePathsPagination(filePathPageCount)}
      </Fragment>
    );
  }

  function filePathsOrEmptyLabel() {
    return hasFilePaths() ? (
      filePathsPage()
    ) : (
      <div className="copyright-no-files">No file paths to display for manually added copyrights</div>
    );
  }

  return (
    <section id="copyright-file-paths" className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">File Paths</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <p className="nx-p">{showingPathsHeader()}</p>
        <NxLoadWrapper
          retryHandler={() => pageChange(componentCopyrightDetails.filePathsPage)}
          error={componentCopyrightDetails.errorFilePaths}
          loading={componentCopyrightDetails.loadingFilePaths}
        >
          {filePathsOrEmptyLabel()}
        </NxLoadWrapper>
      </div>
    </section>
  );
}

CopyrightFilesTile.propTypes = {
  componentCopyrightDetails: componentCopyrightDetailsPropType,
  loadCopyrightContexts: PropTypes.func.isRequired,
  hideCopyrightContext: PropTypes.func.isRequired,
  pageChange: PropTypes.func.isRequired,
};
