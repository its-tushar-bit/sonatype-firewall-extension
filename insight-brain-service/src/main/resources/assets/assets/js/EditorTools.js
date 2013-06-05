/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 *          third-party code listed at
 *          http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 *          trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
    'use strict';
    function wrap($http, method, args, clmLocations) {
    }

    angular.module('EditorTools', []).service('editorTools', [ 'regexFactory', function(regexFactory) {
        return {
            validateName : function(name, currentItem, existingItems) {
                // field is required, alphanumeric, and no unnecessary spaces
                if (!name) {
                    return 'Name is required';
                } else if (name.match(new RegExp('[^-' + regexFactory.allLetters().source + '0-9 ]', 'i'))) {
                    return 'Must be alpha numeric';
                } else if (name.match(/^ | {2,}|\t| $/)) {
                    return 'No leading, trailing or double spaces or tabs';
                }

                // check for uniqueness
                for ( var i = 0; i < existingItems.length; i++) {
                    if (existingItems[i].name === name && existingItems[i].id !== currentItem.id) {
                        return 'Name is already in use';
                    }
                }

                return true;
            },
            generateIcon : function(name) {
                if (!name) {
                    hash = Math.floor(Math.random() * 100);
                } else {
                    for ( var i = 0; i < name.length; i++) {
                        var charAtI = name.charCodeAt(i);
                        hash = ((hash << 5) - hash) + charAtI;
                        hash = hash & hash;
                    }
                }

                return hash;
            },
            getIconSource : function(element, defaultSource) {
                if (element.files && element.files.length > 0) {
                    var file = element.files[0], src;
                    if (window.URL) {
                        src = window.URL.createObjectURL(file);
                    } else if (window.webkitURL) {
                        src = window.webkitURL.createObjectURL(file);
                    }
                    if (src) {
                        return src;
                    }
                }

                return defaultSource;
            }
        };
    } ]);
}());