/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function PropertiesService(Coordinates) {
  var properties = {};
  return {
    getFilename: function () {
      return properties.filename;
    },
    getHash: function () {
      return properties.hash;
    },
    getMatchState: function () {
      return Coordinates.isOriginalVersion() ? properties.matchState : null;
    },
    getPathname: function () {
      return properties.pathname;
    },
    getProprietary: function () {
      return properties.proprietary;
    },
    getStageId: function () {
      return properties.stageId;
    },
    getDependencyType: function () {
      return properties.dependencyType;
    },
    isInnerSource: function () {
      return properties.innerSource;
    },
    reset: function () {
      properties = {};
    },
    setFilename: function (filename) {
      properties.filename = filename;
    },
    setHash: function (hash) {
      properties.hash = hash;
    },
    setMatchState: function (matchState) {
      properties.matchState = matchState;
    },
    setPathname: function (pathname) {
      properties.pathname = pathname;
    },
    setProprietary: function (proprietary) {
      properties.proprietary = proprietary;
    },
    setStageId: function (stageId) {
      properties.stageId = stageId;
    },
    setDependencyType: function (dependencyType) {
      properties.dependencyType = dependencyType;
    },
    setInnerSource: function (innerSource) {
      properties.innerSource = innerSource;
    },
    isUnknown: function () {
      return (properties.matchState || '').toLowerCase() === 'unknown';
    },
  };
}

PropertiesService.$inject = ['Coordinates'];
