["^ ","~:resource-id",["~:shadow.build.classpath/resource","goog/promise/thenable.js"],"~:js","goog.provide(\"goog.Thenable\");\n/**\n @suppress {extraRequire}\n */\ngoog.forwardDeclare(\"goog.Promise\");\n/**\n @interface\n @extends {IThenable<TYPE>}\n @template TYPE\n */\ngoog.Thenable = function() {\n};\n/**\n @param {?function(this:THIS,TYPE):VALUE=} opt_onFulfilled\n @param {?function(this:THIS,*):*=} opt_onRejected\n @param {THIS=} opt_context\n @return {RESULT}\n @template VALUE,THIS\n @template RESULT := type(\"goog.Promise\",cond(isUnknown(VALUE),unknown(),mapunion(VALUE,V=>cond(isTemplatized(V)&&sub(rawTypeOf(V),\"IThenable\"),templateTypeOf(V,0),cond(sub(V,\"Thenable\"),unknown(),V))))) =:\n */\ngoog.Thenable.prototype.then = function(opt_onFulfilled, opt_onRejected, opt_context) {\n};\n/** @const */ goog.Thenable.IMPLEMENTED_BY_PROP = \"$goog_Thenable\";\n/**\n @param {function(new:goog.Thenable,...?)} ctor\n */\ngoog.Thenable.addImplementation = function(ctor) {\n  if (COMPILED) {\n    ctor.prototype[goog.Thenable.IMPLEMENTED_BY_PROP] = true;\n  } else {\n    ctor.prototype.$goog_Thenable = true;\n  }\n};\n/**\n @param {?} object\n @return {boolean}\n */\ngoog.Thenable.isImplementedBy = function(object) {\n  if (!object) {\n    return false;\n  }\n  try {\n    if (COMPILED) {\n      return !!object[goog.Thenable.IMPLEMENTED_BY_PROP];\n    }\n    return !!object.$goog_Thenable;\n  } catch (e) {\n    return false;\n  }\n};\n","~:source","// Copyright 2013 The Closure Library Authors. All Rights Reserved.\n//\n// Licensed under the Apache License, Version 2.0 (the \"License\");\n// you may not use this file except in compliance with the License.\n// You may obtain a copy of the License at\n//\n//      http://www.apache.org/licenses/LICENSE-2.0\n//\n// Unless required by applicable law or agreed to in writing, software\n// distributed under the License is distributed on an \"AS-IS\" BASIS,\n// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n// See the License for the specific language governing permissions and\n// limitations under the License.\n\ngoog.provide('goog.Thenable');\n\n/** @suppress {extraRequire} */\ngoog.forwardDeclare('goog.Promise'); // for the type reference.\n\n\n\n/**\n * Provides a more strict interface for Thenables in terms of\n * http://promisesaplus.com for interop with {@see goog.Promise}.\n *\n * @interface\n * @extends {IThenable<TYPE>}\n * @template TYPE\n */\ngoog.Thenable = function() {};\n\n\n/**\n * Adds callbacks that will operate on the result of the Thenable, returning a\n * new child Promise.\n *\n * If the Thenable is fulfilled, the `onFulfilled` callback will be\n * invoked with the fulfillment value as argument, and the child Promise will\n * be fulfilled with the return value of the callback. If the callback throws\n * an exception, the child Promise will be rejected with the thrown value\n * instead.\n *\n * If the Thenable is rejected, the `onRejected` callback will be invoked\n * with the rejection reason as argument, and the child Promise will be rejected\n * with the return value of the callback or thrown value.\n *\n * @param {?(function(this:THIS, TYPE): VALUE)=} opt_onFulfilled A\n *     function that will be invoked with the fulfillment value if the Promise\n *     is fulfilled.\n * @param {?(function(this:THIS, *): *)=} opt_onRejected A function that will\n *     be invoked with the rejection reason if the Promise is rejected.\n * @param {THIS=} opt_context An optional context object that will be the\n *     execution context for the callbacks. By default, functions are executed\n *     with the default this.\n *\n * @return {RESULT} A new Promise that will receive the result\n *     of the fulfillment or rejection callback.\n * @template VALUE\n * @template THIS\n *\n * When a Promise (or thenable) is returned from the fulfilled callback,\n * the result is the payload of that promise, not the promise itself.\n *\n * @template RESULT := type('goog.Promise',\n *     cond(isUnknown(VALUE), unknown(),\n *       mapunion(VALUE, (V) =>\n *         cond(isTemplatized(V) && sub(rawTypeOf(V), 'IThenable'),\n *           templateTypeOf(V, 0),\n *           cond(sub(V, 'Thenable'),\n *              unknown(),\n *              V)))))\n *  =:\n *\n */\ngoog.Thenable.prototype.then = function(\n    opt_onFulfilled, opt_onRejected, opt_context) {};\n\n\n/**\n * An expando property to indicate that an object implements\n * `goog.Thenable`.\n *\n * {@see addImplementation}.\n *\n * @const\n */\ngoog.Thenable.IMPLEMENTED_BY_PROP = '$goog_Thenable';\n\n\n/**\n * Marks a given class (constructor) as an implementation of Thenable, so\n * that we can query that fact at runtime. The class must have already\n * implemented the interface.\n * Exports a 'then' method on the constructor prototype, so that the objects\n * also implement the extern {@see goog.Thenable} interface for interop with\n * other Promise implementations.\n * @param {function(new:goog.Thenable,...?)} ctor The class constructor. The\n *     corresponding class must have already implemented the interface.\n */\ngoog.Thenable.addImplementation = function(ctor) {\n  if (COMPILED) {\n    ctor.prototype[goog.Thenable.IMPLEMENTED_BY_PROP] = true;\n  } else {\n    // Avoids dictionary access in uncompiled mode.\n    ctor.prototype.$goog_Thenable = true;\n  }\n};\n\n\n/**\n * @param {?} object\n * @return {boolean} Whether a given instance implements `goog.Thenable`.\n *     The class/superclass of the instance must call `addImplementation`.\n */\ngoog.Thenable.isImplementedBy = function(object) {\n  if (!object) {\n    return false;\n  }\n  try {\n    if (COMPILED) {\n      return !!object[goog.Thenable.IMPLEMENTED_BY_PROP];\n    }\n    return !!object.$goog_Thenable;\n  } catch (e) {\n    // Property access seems to be forbidden.\n    return false;\n  }\n};\n","~:compiled-at",1554900569545,"~:source-map-json","{\n\"version\":3,\n\"file\":\"goog.promise.thenable.js\",\n\"lineCount\":51,\n\"mappings\":\"AAcAA,IAAAC,QAAA,CAAa,eAAb,CAAA;AAGA;;;AAAAD,IAAAE,eAAA,CAAoB,cAApB,CAAA;AAYA;;;;;AAAAF,IAAAG,SAAA,GAAgBC,QAAQ,EAAG;CAA3B;AA6CA;;;;;;;;AAAAJ,IAAAG,SAAAE,UAAAC,KAAA,GAA+BC,QAAQ,CACnCC,eADmC,EAClBC,cADkB,EACFC,WADE,CACW;CADlD;AAYA,cAAAV,IAAAG,SAAAQ,oBAAA,GAAoC,gBAApC;AAaA;;;AAAAX,IAAAG,SAAAS,kBAAA,GAAkCC,QAAQ,CAACC,IAAD,CAAO;AAC/C,MAAIC,QAAJ;AACED,QAAAT,UAAA,CAAeL,IAAAG,SAAAQ,oBAAf,CAAA,GAAoD,IAApD;AADF;AAIEG,QAAAT,UAAAW,eAAA,GAAgC,IAAhC;AAJF;AAD+C,CAAjD;AAeA;;;;AAAAhB,IAAAG,SAAAc,gBAAA,GAAgCC,QAAQ,CAACC,MAAD,CAAS;AAC/C,MAAI,CAACA,MAAL;AACE,WAAO,KAAP;AADF;AAGA,KAAI;AACF,QAAIJ,QAAJ;AACE,aAAO,CAAC,CAACI,MAAA,CAAOnB,IAAAG,SAAAQ,oBAAP,CAAT;AADF;AAGA,WAAO,CAAC,CAACQ,MAAAH,eAAT;AAJE,GAKF,QAAOI,CAAP,CAAU;AAEV,WAAO,KAAP;AAFU;AATmC,CAAjD;;\",\n\"sources\":[\"goog/promise/thenable.js\"],\n\"sourcesContent\":[\"// Copyright 2013 The Closure Library Authors. All Rights Reserved.\\n//\\n// Licensed under the Apache License, Version 2.0 (the \\\"License\\\");\\n// you may not use this file except in compliance with the License.\\n// You may obtain a copy of the License at\\n//\\n//      http://www.apache.org/licenses/LICENSE-2.0\\n//\\n// Unless required by applicable law or agreed to in writing, software\\n// distributed under the License is distributed on an \\\"AS-IS\\\" BASIS,\\n// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\\n// See the License for the specific language governing permissions and\\n// limitations under the License.\\n\\ngoog.provide('goog.Thenable');\\n\\n/** @suppress {extraRequire} */\\ngoog.forwardDeclare('goog.Promise'); // for the type reference.\\n\\n\\n\\n/**\\n * Provides a more strict interface for Thenables in terms of\\n * http://promisesaplus.com for interop with {@see goog.Promise}.\\n *\\n * @interface\\n * @extends {IThenable<TYPE>}\\n * @template TYPE\\n */\\ngoog.Thenable = function() {};\\n\\n\\n/**\\n * Adds callbacks that will operate on the result of the Thenable, returning a\\n * new child Promise.\\n *\\n * If the Thenable is fulfilled, the `onFulfilled` callback will be\\n * invoked with the fulfillment value as argument, and the child Promise will\\n * be fulfilled with the return value of the callback. If the callback throws\\n * an exception, the child Promise will be rejected with the thrown value\\n * instead.\\n *\\n * If the Thenable is rejected, the `onRejected` callback will be invoked\\n * with the rejection reason as argument, and the child Promise will be rejected\\n * with the return value of the callback or thrown value.\\n *\\n * @param {?(function(this:THIS, TYPE): VALUE)=} opt_onFulfilled A\\n *     function that will be invoked with the fulfillment value if the Promise\\n *     is fulfilled.\\n * @param {?(function(this:THIS, *): *)=} opt_onRejected A function that will\\n *     be invoked with the rejection reason if the Promise is rejected.\\n * @param {THIS=} opt_context An optional context object that will be the\\n *     execution context for the callbacks. By default, functions are executed\\n *     with the default this.\\n *\\n * @return {RESULT} A new Promise that will receive the result\\n *     of the fulfillment or rejection callback.\\n * @template VALUE\\n * @template THIS\\n *\\n * When a Promise (or thenable) is returned from the fulfilled callback,\\n * the result is the payload of that promise, not the promise itself.\\n *\\n * @template RESULT := type('goog.Promise',\\n *     cond(isUnknown(VALUE), unknown(),\\n *       mapunion(VALUE, (V) =>\\n *         cond(isTemplatized(V) && sub(rawTypeOf(V), 'IThenable'),\\n *           templateTypeOf(V, 0),\\n *           cond(sub(V, 'Thenable'),\\n *              unknown(),\\n *              V)))))\\n *  =:\\n *\\n */\\ngoog.Thenable.prototype.then = function(\\n    opt_onFulfilled, opt_onRejected, opt_context) {};\\n\\n\\n/**\\n * An expando property to indicate that an object implements\\n * `goog.Thenable`.\\n *\\n * {@see addImplementation}.\\n *\\n * @const\\n */\\ngoog.Thenable.IMPLEMENTED_BY_PROP = '$goog_Thenable';\\n\\n\\n/**\\n * Marks a given class (constructor) as an implementation of Thenable, so\\n * that we can query that fact at runtime. The class must have already\\n * implemented the interface.\\n * Exports a 'then' method on the constructor prototype, so that the objects\\n * also implement the extern {@see goog.Thenable} interface for interop with\\n * other Promise implementations.\\n * @param {function(new:goog.Thenable,...?)} ctor The class constructor. The\\n *     corresponding class must have already implemented the interface.\\n */\\ngoog.Thenable.addImplementation = function(ctor) {\\n  if (COMPILED) {\\n    ctor.prototype[goog.Thenable.IMPLEMENTED_BY_PROP] = true;\\n  } else {\\n    // Avoids dictionary access in uncompiled mode.\\n    ctor.prototype.$goog_Thenable = true;\\n  }\\n};\\n\\n\\n/**\\n * @param {?} object\\n * @return {boolean} Whether a given instance implements `goog.Thenable`.\\n *     The class/superclass of the instance must call `addImplementation`.\\n */\\ngoog.Thenable.isImplementedBy = function(object) {\\n  if (!object) {\\n    return false;\\n  }\\n  try {\\n    if (COMPILED) {\\n      return !!object[goog.Thenable.IMPLEMENTED_BY_PROP];\\n    }\\n    return !!object.$goog_Thenable;\\n  } catch (e) {\\n    // Property access seems to be forbidden.\\n    return false;\\n  }\\n};\\n\"],\n\"names\":[\"goog\",\"provide\",\"forwardDeclare\",\"Thenable\",\"goog.Thenable\",\"prototype\",\"then\",\"goog.Thenable.prototype.then\",\"opt_onFulfilled\",\"opt_onRejected\",\"opt_context\",\"IMPLEMENTED_BY_PROP\",\"addImplementation\",\"goog.Thenable.addImplementation\",\"ctor\",\"COMPILED\",\"$goog_Thenable\",\"isImplementedBy\",\"goog.Thenable.isImplementedBy\",\"object\",\"e\"]\n}\n"]