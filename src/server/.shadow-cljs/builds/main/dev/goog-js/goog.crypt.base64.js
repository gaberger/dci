["^ ","~:resource-id",["~:shadow.build.classpath/resource","goog/crypt/base64.js"],"~:js","goog.provide(\"goog.crypt.base64\");\ngoog.require(\"goog.asserts\");\ngoog.require(\"goog.crypt\");\ngoog.require(\"goog.string\");\ngoog.require(\"goog.userAgent\");\ngoog.require(\"goog.userAgent.product\");\n/** @private @type {?Object} */ goog.crypt.base64.byteToCharMap_ = null;\n/** @private @type {?Object} */ goog.crypt.base64.charToByteMap_ = null;\n/** @private @type {?Object} */ goog.crypt.base64.byteToCharMapWebSafe_ = null;\n/** @type {string} */ goog.crypt.base64.ENCODED_VALS_BASE = \"ABCDEFGHIJKLMNOPQRSTUVWXYZ\" + \"abcdefghijklmnopqrstuvwxyz\" + \"0123456789\";\n/** @type {string} */ goog.crypt.base64.ENCODED_VALS = goog.crypt.base64.ENCODED_VALS_BASE + \"+/\\x3d\";\n/** @type {string} */ goog.crypt.base64.ENCODED_VALS_WEBSAFE = goog.crypt.base64.ENCODED_VALS_BASE + \"-_.\";\n/** @private @type {boolean} */ goog.crypt.base64.ASSUME_NATIVE_SUPPORT_ = goog.userAgent.GECKO || goog.userAgent.WEBKIT && !goog.userAgent.product.SAFARI || goog.userAgent.OPERA;\n/** @private @type {boolean} */ goog.crypt.base64.HAS_NATIVE_ENCODE_ = goog.crypt.base64.ASSUME_NATIVE_SUPPORT_ || typeof goog.global.btoa == \"function\";\n/** @private @type {boolean} */ goog.crypt.base64.HAS_NATIVE_DECODE_ = goog.crypt.base64.ASSUME_NATIVE_SUPPORT_ || !goog.userAgent.product.SAFARI && !goog.userAgent.IE && typeof goog.global.atob == \"function\";\n/**\n @param {(Array<number>|Uint8Array)} input\n @param {boolean=} opt_webSafe\n @return {string}\n */\ngoog.crypt.base64.encodeByteArray = function(input, opt_webSafe) {\n  goog.asserts.assert(goog.isArrayLike(input), \"encodeByteArray takes an array as a parameter\");\n  goog.crypt.base64.init_();\n  var byteToCharMap = opt_webSafe ? goog.crypt.base64.byteToCharMapWebSafe_ : goog.crypt.base64.byteToCharMap_;\n  var output = [];\n  for (var i = 0; i < input.length; i += 3) {\n    var byte1 = input[i];\n    var haveByte2 = i + 1 < input.length;\n    var byte2 = haveByte2 ? input[i + 1] : 0;\n    var haveByte3 = i + 2 < input.length;\n    var byte3 = haveByte3 ? input[i + 2] : 0;\n    var outByte1 = byte1 >> 2;\n    var outByte2 = (byte1 & 3) << 4 | byte2 >> 4;\n    var outByte3 = (byte2 & 15) << 2 | byte3 >> 6;\n    var outByte4 = byte3 & 63;\n    if (!haveByte3) {\n      outByte4 = 64;\n      if (!haveByte2) {\n        outByte3 = 64;\n      }\n    }\n    output.push(byteToCharMap[outByte1], byteToCharMap[outByte2], byteToCharMap[outByte3], byteToCharMap[outByte4]);\n  }\n  return output.join(\"\");\n};\n/**\n @param {string} input\n @param {boolean=} opt_webSafe\n @return {string}\n */\ngoog.crypt.base64.encodeString = function(input, opt_webSafe) {\n  if (goog.crypt.base64.HAS_NATIVE_ENCODE_ && !opt_webSafe) {\n    return goog.global.btoa(input);\n  }\n  return goog.crypt.base64.encodeByteArray(goog.crypt.stringToByteArray(input), opt_webSafe);\n};\n/**\n @param {string} input\n @param {boolean=} opt_webSafe\n @return {string}\n */\ngoog.crypt.base64.decodeString = function(input, opt_webSafe) {\n  if (goog.crypt.base64.HAS_NATIVE_DECODE_ && !opt_webSafe) {\n    return goog.global.atob(input);\n  }\n  var output = \"\";\n  function pushByte(b) {\n    output += String.fromCharCode(b);\n  }\n  goog.crypt.base64.decodeStringInternal_(input, pushByte);\n  return output;\n};\n/**\n @param {string} input\n @param {boolean=} opt_ignored\n @return {!Array<number>}\n */\ngoog.crypt.base64.decodeStringToByteArray = function(input, opt_ignored) {\n  var output = [];\n  function pushByte(b) {\n    output.push(b);\n  }\n  goog.crypt.base64.decodeStringInternal_(input, pushByte);\n  return output;\n};\n/**\n @param {string} input\n @return {!Uint8Array}\n */\ngoog.crypt.base64.decodeStringToUint8Array = function(input) {\n  goog.asserts.assert(!goog.userAgent.IE || goog.userAgent.isVersionOrHigher(\"10\"), \"Browser does not support typed arrays\");\n  var len = input.length;\n  var placeholders = 0;\n  if (input[len - 2] === \"\\x3d\") {\n    placeholders = 2;\n  } else {\n    if (input[len - 1] === \"\\x3d\") {\n      placeholders = 1;\n    }\n  }\n  var output = new Uint8Array(Math.ceil(len * 3 / 4) - placeholders);\n  var outLen = 0;\n  function pushByte(b) {\n    output[outLen++] = b;\n  }\n  goog.crypt.base64.decodeStringInternal_(input, pushByte);\n  return output.subarray(0, outLen);\n};\n/**\n @private\n @param {string} input\n @param {function(number):void} pushByte\n */\ngoog.crypt.base64.decodeStringInternal_ = function(input, pushByte) {\n  goog.crypt.base64.init_();\n  var nextCharIndex = 0;\n  /**\n   @param {number} default_val\n   @return {number}\n   */\n  function getByte(default_val) {\n    while (nextCharIndex < input.length) {\n      var ch = input.charAt(nextCharIndex++);\n      var b = goog.crypt.base64.charToByteMap_[ch];\n      if (b != null) {\n        return b;\n      }\n      if (!goog.string.isEmptyOrWhitespace(ch)) {\n        throw new Error(\"Unknown base64 encoding at char: \" + ch);\n      }\n    }\n    return default_val;\n  }\n  while (true) {\n    var byte1 = getByte(-1);\n    var byte2 = getByte(0);\n    var byte3 = getByte(64);\n    var byte4 = getByte(64);\n    if (byte4 === 64) {\n      if (byte1 === -1) {\n        return;\n      }\n    }\n    var outByte1 = byte1 << 2 | byte2 >> 4;\n    pushByte(outByte1);\n    if (byte3 != 64) {\n      var outByte2 = byte2 << 4 & 240 | byte3 >> 2;\n      pushByte(outByte2);\n      if (byte4 != 64) {\n        var outByte3 = byte3 << 6 & 192 | byte4;\n        pushByte(outByte3);\n      }\n    }\n  }\n};\n/** @private */ goog.crypt.base64.init_ = function() {\n  if (!goog.crypt.base64.byteToCharMap_) {\n    goog.crypt.base64.byteToCharMap_ = {};\n    goog.crypt.base64.charToByteMap_ = {};\n    goog.crypt.base64.byteToCharMapWebSafe_ = {};\n    for (var i = 0; i < goog.crypt.base64.ENCODED_VALS.length; i++) {\n      goog.crypt.base64.byteToCharMap_[i] = goog.crypt.base64.ENCODED_VALS.charAt(i);\n      goog.crypt.base64.charToByteMap_[goog.crypt.base64.byteToCharMap_[i]] = i;\n      goog.crypt.base64.byteToCharMapWebSafe_[i] = goog.crypt.base64.ENCODED_VALS_WEBSAFE.charAt(i);\n      if (i >= goog.crypt.base64.ENCODED_VALS_BASE.length) {\n        goog.crypt.base64.charToByteMap_[goog.crypt.base64.ENCODED_VALS_WEBSAFE.charAt(i)] = i;\n      }\n    }\n  }\n};\n","~:source","// Copyright 2007 The Closure Library Authors. All Rights Reserved.\n//\n// Licensed under the Apache License, Version 2.0 (the \"License\");\n// you may not use this file except in compliance with the License.\n// You may obtain a copy of the License at\n//\n//      http://www.apache.org/licenses/LICENSE-2.0\n//\n// Unless required by applicable law or agreed to in writing, software\n// distributed under the License is distributed on an \"AS-IS\" BASIS,\n// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n// See the License for the specific language governing permissions and\n// limitations under the License.\n\n/**\n * @fileoverview Base64 en/decoding. Not much to say here except that we\n * work with decoded values in arrays of bytes. By \"byte\" I mean a number\n * in [0, 255].\n *\n * @author doughtie@google.com (Gavin Doughtie)\n */\n\ngoog.provide('goog.crypt.base64');\n\ngoog.require('goog.asserts');\ngoog.require('goog.crypt');\ngoog.require('goog.string');\ngoog.require('goog.userAgent');\ngoog.require('goog.userAgent.product');\n\n// Static lookup maps, lazily populated by init_()\n\n\n/**\n * Maps bytes to characters.\n * @type {?Object}\n * @private\n */\ngoog.crypt.base64.byteToCharMap_ = null;\n\n\n/**\n * Maps characters to bytes. Used for normal and websafe characters.\n * @type {?Object}\n * @private\n */\ngoog.crypt.base64.charToByteMap_ = null;\n\n\n/**\n * Maps bytes to websafe characters.\n * @type {?Object}\n * @private\n */\ngoog.crypt.base64.byteToCharMapWebSafe_ = null;\n\n\n/**\n * Our default alphabet, shared between\n * ENCODED_VALS and ENCODED_VALS_WEBSAFE\n * @type {string}\n */\ngoog.crypt.base64.ENCODED_VALS_BASE = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' +\n    'abcdefghijklmnopqrstuvwxyz' +\n    '0123456789';\n\n\n/**\n * Our default alphabet. Value 64 (=) is special; it means \"nothing.\"\n * @type {string}\n */\ngoog.crypt.base64.ENCODED_VALS = goog.crypt.base64.ENCODED_VALS_BASE + '+/=';\n\n\n/**\n * Our websafe alphabet.\n * @type {string}\n */\ngoog.crypt.base64.ENCODED_VALS_WEBSAFE =\n    goog.crypt.base64.ENCODED_VALS_BASE + '-_.';\n\n\n/**\n * White list of implementations with known-good native atob and btoa functions.\n * Listing these explicitly (via the ASSUME_* wrappers) benefits dead-code\n * removal in per-browser compilations.\n * @private {boolean}\n */\ngoog.crypt.base64.ASSUME_NATIVE_SUPPORT_ = goog.userAgent.GECKO ||\n    (goog.userAgent.WEBKIT && !goog.userAgent.product.SAFARI) ||\n    goog.userAgent.OPERA;\n\n\n/**\n * Does this browser have a working btoa function?\n * @private {boolean}\n */\ngoog.crypt.base64.HAS_NATIVE_ENCODE_ =\n    goog.crypt.base64.ASSUME_NATIVE_SUPPORT_ ||\n    typeof(goog.global.btoa) == 'function';\n\n\n/**\n * Does this browser have a working atob function?\n * We blacklist known-bad implementations:\n *  - IE (10+) added atob() but it does not tolerate whitespace on the input.\n * @private {boolean}\n */\ngoog.crypt.base64.HAS_NATIVE_DECODE_ =\n    goog.crypt.base64.ASSUME_NATIVE_SUPPORT_ ||\n    (!goog.userAgent.product.SAFARI && !goog.userAgent.IE &&\n     typeof(goog.global.atob) == 'function');\n\n\n/**\n * Base64-encode an array of bytes.\n *\n * @param {Array<number>|Uint8Array} input An array of bytes (numbers with\n *     value in [0, 255]) to encode.\n * @param {boolean=} opt_webSafe True indicates we should use the alternative\n *     alphabet, which does not require escaping for use in URLs.\n * @return {string} The base64 encoded string.\n */\ngoog.crypt.base64.encodeByteArray = function(input, opt_webSafe) {\n  // Assert avoids runtime dependency on goog.isArrayLike, which helps reduce\n  // size of jscompiler output, and which yields slight performance increase.\n  goog.asserts.assert(\n      goog.isArrayLike(input), 'encodeByteArray takes an array as a parameter');\n\n  goog.crypt.base64.init_();\n\n  var byteToCharMap = opt_webSafe ? goog.crypt.base64.byteToCharMapWebSafe_ :\n                                    goog.crypt.base64.byteToCharMap_;\n\n  var output = [];\n\n  for (var i = 0; i < input.length; i += 3) {\n    var byte1 = input[i];\n    var haveByte2 = i + 1 < input.length;\n    var byte2 = haveByte2 ? input[i + 1] : 0;\n    var haveByte3 = i + 2 < input.length;\n    var byte3 = haveByte3 ? input[i + 2] : 0;\n\n    var outByte1 = byte1 >> 2;\n    var outByte2 = ((byte1 & 0x03) << 4) | (byte2 >> 4);\n    var outByte3 = ((byte2 & 0x0F) << 2) | (byte3 >> 6);\n    var outByte4 = byte3 & 0x3F;\n\n    if (!haveByte3) {\n      outByte4 = 64;\n\n      if (!haveByte2) {\n        outByte3 = 64;\n      }\n    }\n\n    output.push(\n        byteToCharMap[outByte1], byteToCharMap[outByte2],\n        byteToCharMap[outByte3], byteToCharMap[outByte4]);\n  }\n\n  return output.join('');\n};\n\n\n/**\n * Base64-encode a string.\n *\n * @param {string} input A string to encode.\n * @param {boolean=} opt_webSafe True indicates we should use the alternative\n *     alphabet, which does not require escaping for use in URLs.\n * @return {string} The base64 encoded string.\n */\ngoog.crypt.base64.encodeString = function(input, opt_webSafe) {\n  // Shortcut for browsers that implement\n  // a native base64 encoder in the form of \"btoa/atob\"\n  if (goog.crypt.base64.HAS_NATIVE_ENCODE_ && !opt_webSafe) {\n    return goog.global.btoa(input);\n  }\n  return goog.crypt.base64.encodeByteArray(\n      goog.crypt.stringToByteArray(input), opt_webSafe);\n};\n\n\n/**\n * Base64-decode a string.\n *\n * @param {string} input Input to decode. Any whitespace is ignored, and the\n *     input maybe encoded with either supported alphabet (or a mix thereof).\n * @param {boolean=} opt_webSafe True indicates we should use the alternative\n *     alphabet, which does not require escaping for use in URLs. Note that\n *     passing false may also still allow webSafe input decoding, when the\n *     fallback decoder is used on browsers without native support.\n * @return {string} string representing the decoded value.\n */\ngoog.crypt.base64.decodeString = function(input, opt_webSafe) {\n  // Shortcut for browsers that implement\n  // a native base64 encoder in the form of \"btoa/atob\"\n  if (goog.crypt.base64.HAS_NATIVE_DECODE_ && !opt_webSafe) {\n    return goog.global.atob(input);\n  }\n  var output = '';\n  function pushByte(b) { output += String.fromCharCode(b); }\n\n  goog.crypt.base64.decodeStringInternal_(input, pushByte);\n\n  return output;\n};\n\n\n/**\n * Base64-decode a string to an Array of numbers.\n *\n * In base-64 decoding, groups of four characters are converted into three\n * bytes.  If the encoder did not apply padding, the input length may not\n * be a multiple of 4.\n *\n * In this case, the last group will have fewer than 4 characters, and\n * padding will be inferred.  If the group has one or two characters, it decodes\n * to one byte.  If the group has three characters, it decodes to two bytes.\n *\n * @param {string} input Input to decode. Any whitespace is ignored, and the\n *     input maybe encoded with either supported alphabet (or a mix thereof).\n * @param {boolean=} opt_ignored Unused parameter, retained for compatibility.\n * @return {!Array<number>} bytes representing the decoded value.\n */\ngoog.crypt.base64.decodeStringToByteArray = function(input, opt_ignored) {\n  var output = [];\n  function pushByte(b) { output.push(b); }\n\n  goog.crypt.base64.decodeStringInternal_(input, pushByte);\n\n  return output;\n};\n\n\n/**\n * Base64-decode a string to a Uint8Array.\n *\n * Note that Uint8Array is not supported on older browsers, e.g. IE < 10.\n * @see http://caniuse.com/uint8array\n *\n * In base-64 decoding, groups of four characters are converted into three\n * bytes.  If the encoder did not apply padding, the input length may not\n * be a multiple of 4.\n *\n * In this case, the last group will have fewer than 4 characters, and\n * padding will be inferred.  If the group has one or two characters, it decodes\n * to one byte.  If the group has three characters, it decodes to two bytes.\n *\n * @param {string} input Input to decode. Any whitespace is ignored, and the\n *     input maybe encoded with either supported alphabet (or a mix thereof).\n * @return {!Uint8Array} bytes representing the decoded value.\n */\ngoog.crypt.base64.decodeStringToUint8Array = function(input) {\n  goog.asserts.assert(\n      !goog.userAgent.IE || goog.userAgent.isVersionOrHigher('10'),\n      'Browser does not support typed arrays');\n  var len = input.length;\n  // Check if there are trailing '=' as padding in the b64 string.\n  var placeholders = 0;\n  if (input[len - 2] === '=') {\n    placeholders = 2;\n  } else if (input[len - 1] === '=') {\n    placeholders = 1;\n  }\n  var output = new Uint8Array(Math.ceil(len * 3 / 4) - placeholders);\n  var outLen = 0;\n  function pushByte(b) {\n    output[outLen++] = b;\n  }\n\n  goog.crypt.base64.decodeStringInternal_(input, pushByte);\n\n  return output.subarray(0, outLen);\n};\n\n\n/**\n * @param {string} input Input to decode.\n * @param {function(number):void} pushByte result accumulator.\n * @private\n */\ngoog.crypt.base64.decodeStringInternal_ = function(input, pushByte) {\n  goog.crypt.base64.init_();\n\n  var nextCharIndex = 0;\n  /**\n   * @param {number} default_val Used for end-of-input.\n   * @return {number} The next 6-bit value, or the default for end-of-input.\n   */\n  function getByte(default_val) {\n    while (nextCharIndex < input.length) {\n      var ch = input.charAt(nextCharIndex++);\n      var b = goog.crypt.base64.charToByteMap_[ch];\n      if (b != null) {\n        return b;  // Common case: decoded the char.\n      }\n      if (!goog.string.isEmptyOrWhitespace(ch)) {\n        throw new Error('Unknown base64 encoding at char: ' + ch);\n      }\n      // We encountered whitespace: loop around to the next input char.\n    }\n    return default_val;  // No more input remaining.\n  }\n\n  while (true) {\n    var byte1 = getByte(-1);\n    var byte2 = getByte(0);\n    var byte3 = getByte(64);\n    var byte4 = getByte(64);\n\n    // The common case is that all four bytes are present, so if we have byte4\n    // we can skip over the truncated input special case handling.\n    if (byte4 === 64) {\n      if (byte1 === -1) {\n        return;  // Terminal case: no input left to decode.\n      }\n      // Here we know an intermediate number of bytes are missing.\n      // The defaults for byte2, byte3 and byte4 apply the inferred padding\n      // rules per the public API documentation. i.e: 1 byte\n      // missing should yield 2 bytes of output, but 2 or 3 missing bytes yield\n      // a single byte of output. (Recall that 64 corresponds the padding char).\n    }\n\n    var outByte1 = (byte1 << 2) | (byte2 >> 4);\n    pushByte(outByte1);\n\n    if (byte3 != 64) {\n      var outByte2 = ((byte2 << 4) & 0xF0) | (byte3 >> 2);\n      pushByte(outByte2);\n\n      if (byte4 != 64) {\n        var outByte3 = ((byte3 << 6) & 0xC0) | byte4;\n        pushByte(outByte3);\n      }\n    }\n  }\n};\n\n\n/**\n * Lazy static initialization function. Called before\n * accessing any of the static map variables.\n * @private\n */\ngoog.crypt.base64.init_ = function() {\n  if (!goog.crypt.base64.byteToCharMap_) {\n    goog.crypt.base64.byteToCharMap_ = {};\n    goog.crypt.base64.charToByteMap_ = {};\n    goog.crypt.base64.byteToCharMapWebSafe_ = {};\n\n    // We want quick mappings back and forth, so we precompute two maps.\n    for (var i = 0; i < goog.crypt.base64.ENCODED_VALS.length; i++) {\n      goog.crypt.base64.byteToCharMap_[i] =\n          goog.crypt.base64.ENCODED_VALS.charAt(i);\n      goog.crypt.base64.charToByteMap_[goog.crypt.base64.byteToCharMap_[i]] = i;\n      goog.crypt.base64.byteToCharMapWebSafe_[i] =\n          goog.crypt.base64.ENCODED_VALS_WEBSAFE.charAt(i);\n\n      // Be forgiving when decoding and correctly decode both encodings.\n      if (i >= goog.crypt.base64.ENCODED_VALS_BASE.length) {\n        goog.crypt.base64\n            .charToByteMap_[goog.crypt.base64.ENCODED_VALS_WEBSAFE.charAt(i)] =\n            i;\n      }\n    }\n  }\n};\n","~:compiled-at",1554900569657,"~:source-map-json","{\n\"version\":3,\n\"file\":\"goog.crypt.base64.js\",\n\"lineCount\":171,\n\"mappings\":\"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\",\n\"sources\":[\"goog/crypt/base64.js\"],\n\"sourcesContent\":[\"// Copyright 2007 The Closure Library Authors. All Rights Reserved.\\n//\\n// Licensed under the Apache License, Version 2.0 (the \\\"License\\\");\\n// you may not use this file except in compliance with the License.\\n// You may obtain a copy of the License at\\n//\\n//      http://www.apache.org/licenses/LICENSE-2.0\\n//\\n// Unless required by applicable law or agreed to in writing, software\\n// distributed under the License is distributed on an \\\"AS-IS\\\" BASIS,\\n// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\\n// See the License for the specific language governing permissions and\\n// limitations under the License.\\n\\n/**\\n * @fileoverview Base64 en/decoding. Not much to say here except that we\\n * work with decoded values in arrays of bytes. By \\\"byte\\\" I mean a number\\n * in [0, 255].\\n *\\n * @author doughtie@google.com (Gavin Doughtie)\\n */\\n\\ngoog.provide('goog.crypt.base64');\\n\\ngoog.require('goog.asserts');\\ngoog.require('goog.crypt');\\ngoog.require('goog.string');\\ngoog.require('goog.userAgent');\\ngoog.require('goog.userAgent.product');\\n\\n// Static lookup maps, lazily populated by init_()\\n\\n\\n/**\\n * Maps bytes to characters.\\n * @type {?Object}\\n * @private\\n */\\ngoog.crypt.base64.byteToCharMap_ = null;\\n\\n\\n/**\\n * Maps characters to bytes. Used for normal and websafe characters.\\n * @type {?Object}\\n * @private\\n */\\ngoog.crypt.base64.charToByteMap_ = null;\\n\\n\\n/**\\n * Maps bytes to websafe characters.\\n * @type {?Object}\\n * @private\\n */\\ngoog.crypt.base64.byteToCharMapWebSafe_ = null;\\n\\n\\n/**\\n * Our default alphabet, shared between\\n * ENCODED_VALS and ENCODED_VALS_WEBSAFE\\n * @type {string}\\n */\\ngoog.crypt.base64.ENCODED_VALS_BASE = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' +\\n    'abcdefghijklmnopqrstuvwxyz' +\\n    '0123456789';\\n\\n\\n/**\\n * Our default alphabet. Value 64 (=) is special; it means \\\"nothing.\\\"\\n * @type {string}\\n */\\ngoog.crypt.base64.ENCODED_VALS = goog.crypt.base64.ENCODED_VALS_BASE + '+/=';\\n\\n\\n/**\\n * Our websafe alphabet.\\n * @type {string}\\n */\\ngoog.crypt.base64.ENCODED_VALS_WEBSAFE =\\n    goog.crypt.base64.ENCODED_VALS_BASE + '-_.';\\n\\n\\n/**\\n * White list of implementations with known-good native atob and btoa functions.\\n * Listing these explicitly (via the ASSUME_* wrappers) benefits dead-code\\n * removal in per-browser compilations.\\n * @private {boolean}\\n */\\ngoog.crypt.base64.ASSUME_NATIVE_SUPPORT_ = goog.userAgent.GECKO ||\\n    (goog.userAgent.WEBKIT && !goog.userAgent.product.SAFARI) ||\\n    goog.userAgent.OPERA;\\n\\n\\n/**\\n * Does this browser have a working btoa function?\\n * @private {boolean}\\n */\\ngoog.crypt.base64.HAS_NATIVE_ENCODE_ =\\n    goog.crypt.base64.ASSUME_NATIVE_SUPPORT_ ||\\n    typeof(goog.global.btoa) == 'function';\\n\\n\\n/**\\n * Does this browser have a working atob function?\\n * We blacklist known-bad implementations:\\n *  - IE (10+) added atob() but it does not tolerate whitespace on the input.\\n * @private {boolean}\\n */\\ngoog.crypt.base64.HAS_NATIVE_DECODE_ =\\n    goog.crypt.base64.ASSUME_NATIVE_SUPPORT_ ||\\n    (!goog.userAgent.product.SAFARI && !goog.userAgent.IE &&\\n     typeof(goog.global.atob) == 'function');\\n\\n\\n/**\\n * Base64-encode an array of bytes.\\n *\\n * @param {Array<number>|Uint8Array} input An array of bytes (numbers with\\n *     value in [0, 255]) to encode.\\n * @param {boolean=} opt_webSafe True indicates we should use the alternative\\n *     alphabet, which does not require escaping for use in URLs.\\n * @return {string} The base64 encoded string.\\n */\\ngoog.crypt.base64.encodeByteArray = function(input, opt_webSafe) {\\n  // Assert avoids runtime dependency on goog.isArrayLike, which helps reduce\\n  // size of jscompiler output, and which yields slight performance increase.\\n  goog.asserts.assert(\\n      goog.isArrayLike(input), 'encodeByteArray takes an array as a parameter');\\n\\n  goog.crypt.base64.init_();\\n\\n  var byteToCharMap = opt_webSafe ? goog.crypt.base64.byteToCharMapWebSafe_ :\\n                                    goog.crypt.base64.byteToCharMap_;\\n\\n  var output = [];\\n\\n  for (var i = 0; i < input.length; i += 3) {\\n    var byte1 = input[i];\\n    var haveByte2 = i + 1 < input.length;\\n    var byte2 = haveByte2 ? input[i + 1] : 0;\\n    var haveByte3 = i + 2 < input.length;\\n    var byte3 = haveByte3 ? input[i + 2] : 0;\\n\\n    var outByte1 = byte1 >> 2;\\n    var outByte2 = ((byte1 & 0x03) << 4) | (byte2 >> 4);\\n    var outByte3 = ((byte2 & 0x0F) << 2) | (byte3 >> 6);\\n    var outByte4 = byte3 & 0x3F;\\n\\n    if (!haveByte3) {\\n      outByte4 = 64;\\n\\n      if (!haveByte2) {\\n        outByte3 = 64;\\n      }\\n    }\\n\\n    output.push(\\n        byteToCharMap[outByte1], byteToCharMap[outByte2],\\n        byteToCharMap[outByte3], byteToCharMap[outByte4]);\\n  }\\n\\n  return output.join('');\\n};\\n\\n\\n/**\\n * Base64-encode a string.\\n *\\n * @param {string} input A string to encode.\\n * @param {boolean=} opt_webSafe True indicates we should use the alternative\\n *     alphabet, which does not require escaping for use in URLs.\\n * @return {string} The base64 encoded string.\\n */\\ngoog.crypt.base64.encodeString = function(input, opt_webSafe) {\\n  // Shortcut for browsers that implement\\n  // a native base64 encoder in the form of \\\"btoa/atob\\\"\\n  if (goog.crypt.base64.HAS_NATIVE_ENCODE_ && !opt_webSafe) {\\n    return goog.global.btoa(input);\\n  }\\n  return goog.crypt.base64.encodeByteArray(\\n      goog.crypt.stringToByteArray(input), opt_webSafe);\\n};\\n\\n\\n/**\\n * Base64-decode a string.\\n *\\n * @param {string} input Input to decode. Any whitespace is ignored, and the\\n *     input maybe encoded with either supported alphabet (or a mix thereof).\\n * @param {boolean=} opt_webSafe True indicates we should use the alternative\\n *     alphabet, which does not require escaping for use in URLs. Note that\\n *     passing false may also still allow webSafe input decoding, when the\\n *     fallback decoder is used on browsers without native support.\\n * @return {string} string representing the decoded value.\\n */\\ngoog.crypt.base64.decodeString = function(input, opt_webSafe) {\\n  // Shortcut for browsers that implement\\n  // a native base64 encoder in the form of \\\"btoa/atob\\\"\\n  if (goog.crypt.base64.HAS_NATIVE_DECODE_ && !opt_webSafe) {\\n    return goog.global.atob(input);\\n  }\\n  var output = '';\\n  function pushByte(b) { output += String.fromCharCode(b); }\\n\\n  goog.crypt.base64.decodeStringInternal_(input, pushByte);\\n\\n  return output;\\n};\\n\\n\\n/**\\n * Base64-decode a string to an Array of numbers.\\n *\\n * In base-64 decoding, groups of four characters are converted into three\\n * bytes.  If the encoder did not apply padding, the input length may not\\n * be a multiple of 4.\\n *\\n * In this case, the last group will have fewer than 4 characters, and\\n * padding will be inferred.  If the group has one or two characters, it decodes\\n * to one byte.  If the group has three characters, it decodes to two bytes.\\n *\\n * @param {string} input Input to decode. Any whitespace is ignored, and the\\n *     input maybe encoded with either supported alphabet (or a mix thereof).\\n * @param {boolean=} opt_ignored Unused parameter, retained for compatibility.\\n * @return {!Array<number>} bytes representing the decoded value.\\n */\\ngoog.crypt.base64.decodeStringToByteArray = function(input, opt_ignored) {\\n  var output = [];\\n  function pushByte(b) { output.push(b); }\\n\\n  goog.crypt.base64.decodeStringInternal_(input, pushByte);\\n\\n  return output;\\n};\\n\\n\\n/**\\n * Base64-decode a string to a Uint8Array.\\n *\\n * Note that Uint8Array is not supported on older browsers, e.g. IE < 10.\\n * @see http://caniuse.com/uint8array\\n *\\n * In base-64 decoding, groups of four characters are converted into three\\n * bytes.  If the encoder did not apply padding, the input length may not\\n * be a multiple of 4.\\n *\\n * In this case, the last group will have fewer than 4 characters, and\\n * padding will be inferred.  If the group has one or two characters, it decodes\\n * to one byte.  If the group has three characters, it decodes to two bytes.\\n *\\n * @param {string} input Input to decode. Any whitespace is ignored, and the\\n *     input maybe encoded with either supported alphabet (or a mix thereof).\\n * @return {!Uint8Array} bytes representing the decoded value.\\n */\\ngoog.crypt.base64.decodeStringToUint8Array = function(input) {\\n  goog.asserts.assert(\\n      !goog.userAgent.IE || goog.userAgent.isVersionOrHigher('10'),\\n      'Browser does not support typed arrays');\\n  var len = input.length;\\n  // Check if there are trailing '=' as padding in the b64 string.\\n  var placeholders = 0;\\n  if (input[len - 2] === '=') {\\n    placeholders = 2;\\n  } else if (input[len - 1] === '=') {\\n    placeholders = 1;\\n  }\\n  var output = new Uint8Array(Math.ceil(len * 3 / 4) - placeholders);\\n  var outLen = 0;\\n  function pushByte(b) {\\n    output[outLen++] = b;\\n  }\\n\\n  goog.crypt.base64.decodeStringInternal_(input, pushByte);\\n\\n  return output.subarray(0, outLen);\\n};\\n\\n\\n/**\\n * @param {string} input Input to decode.\\n * @param {function(number):void} pushByte result accumulator.\\n * @private\\n */\\ngoog.crypt.base64.decodeStringInternal_ = function(input, pushByte) {\\n  goog.crypt.base64.init_();\\n\\n  var nextCharIndex = 0;\\n  /**\\n   * @param {number} default_val Used for end-of-input.\\n   * @return {number} The next 6-bit value, or the default for end-of-input.\\n   */\\n  function getByte(default_val) {\\n    while (nextCharIndex < input.length) {\\n      var ch = input.charAt(nextCharIndex++);\\n      var b = goog.crypt.base64.charToByteMap_[ch];\\n      if (b != null) {\\n        return b;  // Common case: decoded the char.\\n      }\\n      if (!goog.string.isEmptyOrWhitespace(ch)) {\\n        throw new Error('Unknown base64 encoding at char: ' + ch);\\n      }\\n      // We encountered whitespace: loop around to the next input char.\\n    }\\n    return default_val;  // No more input remaining.\\n  }\\n\\n  while (true) {\\n    var byte1 = getByte(-1);\\n    var byte2 = getByte(0);\\n    var byte3 = getByte(64);\\n    var byte4 = getByte(64);\\n\\n    // The common case is that all four bytes are present, so if we have byte4\\n    // we can skip over the truncated input special case handling.\\n    if (byte4 === 64) {\\n      if (byte1 === -1) {\\n        return;  // Terminal case: no input left to decode.\\n      }\\n      // Here we know an intermediate number of bytes are missing.\\n      // The defaults for byte2, byte3 and byte4 apply the inferred padding\\n      // rules per the public API documentation. i.e: 1 byte\\n      // missing should yield 2 bytes of output, but 2 or 3 missing bytes yield\\n      // a single byte of output. (Recall that 64 corresponds the padding char).\\n    }\\n\\n    var outByte1 = (byte1 << 2) | (byte2 >> 4);\\n    pushByte(outByte1);\\n\\n    if (byte3 != 64) {\\n      var outByte2 = ((byte2 << 4) & 0xF0) | (byte3 >> 2);\\n      pushByte(outByte2);\\n\\n      if (byte4 != 64) {\\n        var outByte3 = ((byte3 << 6) & 0xC0) | byte4;\\n        pushByte(outByte3);\\n      }\\n    }\\n  }\\n};\\n\\n\\n/**\\n * Lazy static initialization function. Called before\\n * accessing any of the static map variables.\\n * @private\\n */\\ngoog.crypt.base64.init_ = function() {\\n  if (!goog.crypt.base64.byteToCharMap_) {\\n    goog.crypt.base64.byteToCharMap_ = {};\\n    goog.crypt.base64.charToByteMap_ = {};\\n    goog.crypt.base64.byteToCharMapWebSafe_ = {};\\n\\n    // We want quick mappings back and forth, so we precompute two maps.\\n    for (var i = 0; i < goog.crypt.base64.ENCODED_VALS.length; i++) {\\n      goog.crypt.base64.byteToCharMap_[i] =\\n          goog.crypt.base64.ENCODED_VALS.charAt(i);\\n      goog.crypt.base64.charToByteMap_[goog.crypt.base64.byteToCharMap_[i]] = i;\\n      goog.crypt.base64.byteToCharMapWebSafe_[i] =\\n          goog.crypt.base64.ENCODED_VALS_WEBSAFE.charAt(i);\\n\\n      // Be forgiving when decoding and correctly decode both encodings.\\n      if (i >= goog.crypt.base64.ENCODED_VALS_BASE.length) {\\n        goog.crypt.base64\\n            .charToByteMap_[goog.crypt.base64.ENCODED_VALS_WEBSAFE.charAt(i)] =\\n            i;\\n      }\\n    }\\n  }\\n};\\n\"],\n\"names\":[\"goog\",\"provide\",\"require\",\"crypt\",\"base64\",\"byteToCharMap_\",\"charToByteMap_\",\"byteToCharMapWebSafe_\",\"ENCODED_VALS_BASE\",\"ENCODED_VALS\",\"ENCODED_VALS_WEBSAFE\",\"ASSUME_NATIVE_SUPPORT_\",\"userAgent\",\"GECKO\",\"WEBKIT\",\"product\",\"SAFARI\",\"OPERA\",\"HAS_NATIVE_ENCODE_\",\"global\",\"btoa\",\"HAS_NATIVE_DECODE_\",\"IE\",\"atob\",\"encodeByteArray\",\"goog.crypt.base64.encodeByteArray\",\"input\",\"opt_webSafe\",\"asserts\",\"assert\",\"isArrayLike\",\"init_\",\"byteToCharMap\",\"output\",\"i\",\"length\",\"byte1\",\"haveByte2\",\"byte2\",\"haveByte3\",\"byte3\",\"outByte1\",\"outByte2\",\"outByte3\",\"outByte4\",\"push\",\"join\",\"encodeString\",\"goog.crypt.base64.encodeString\",\"stringToByteArray\",\"decodeString\",\"goog.crypt.base64.decodeString\",\"pushByte\",\"b\",\"String\",\"fromCharCode\",\"decodeStringInternal_\",\"decodeStringToByteArray\",\"goog.crypt.base64.decodeStringToByteArray\",\"opt_ignored\",\"decodeStringToUint8Array\",\"goog.crypt.base64.decodeStringToUint8Array\",\"isVersionOrHigher\",\"len\",\"placeholders\",\"Uint8Array\",\"Math\",\"ceil\",\"outLen\",\"subarray\",\"goog.crypt.base64.decodeStringInternal_\",\"nextCharIndex\",\"getByte\",\"default_val\",\"ch\",\"charAt\",\"string\",\"isEmptyOrWhitespace\",\"Error\",\"byte4\",\"goog.crypt.base64.init_\"]\n}\n"]