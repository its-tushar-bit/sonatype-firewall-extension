/**
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
            messages: {
              required: 'Name is required',
              alphanumeric: 'Must be alpha numeric',
              spaces: 'No leading, trailing or double spaces or tabs',
              duplicate: 'Name is already in use'
            },
            validateName : function(name, currentItem, existingItems) {
                // field is required, alphanumeric, and no unnecessary spaces
                if (!name) {
                    return this.messages.required;
                } else if (name.match(new RegExp('[^-' + regexFactory.allLetters().source + '0-9 ]', 'i'))) {
                    return this.messages.alphanumeric;
                } else if (name.match(/^ | {2,}|\t| $/)) {
                    return this.messages.spaces;
                }

                // check for uniqueness
                for ( var i = 0; i < existingItems.length; i++) {
                    if (existingItems[i].name.toLowerCase() === name.toLowerCase()
                          && existingItems[i].id !== currentItem.id) {
                        return this.messages.duplicate;
                    }
                }

                return true;
            },
            generateIcon : function(name) {
            	var hash = 0;
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