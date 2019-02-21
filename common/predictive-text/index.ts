/*
 * Copyright (c) 2018 National Research Council Canada (author: Eddie A. Santos)
 * Copyright (c) 2018 SIL International
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/// <reference path="node_modules/promise-polyfill/lib/polyfill.js" />
/// <reference path="promise-store.ts" />

/**
 * Top-level interface to the Language Modelling layer, or "LMLayer" for short.
 * 
 * The Language Modelling layer provides a way for keyboards to offer prediction and
 * correction functionalities. The LMLayer proper runs within a Web Worker, however,
 * this class is intended to run in the main thread, and automatically spawn a Web
 * Worker, capable of offering predictions.
 * 
 * Since the Worker runs in a different thread, the public methods of this class are
 * asynchronous. Methods of note include:
 * 
 *  - #initialize() -- initialize the LMLayer with a configuration and language model
 *  - #predict() -- ask the LMLayer to offer suggestions (predictions or corrections) for
 *                  the input event
 * 
 * The top-level LMLayer will automatically starts up its own Web Worker.
 */
class LMLayer {
  /**
   * The underlying worker instance. By default, this is the LMLayerWorker. 
   */
  private _worker: Worker;
  /** Call this when the LMLayer has sent us the 'ready' message! */
  private _declareLMLayerReady: (conf: Configuration) => void;
  private _promises: PromiseStore<Suggestion[]>;
  private _nextToken: number;

  /**
   * Construct the top-level LMLayer interface. This also starts the underlying Worker.
   * Make sure to call .initialize() when using the default Worker.
   * 
   * @param uri URI of the underlying LMLayer worker code. This will usually be a blob:
   *            or file: URI. If uri is not provided, this will start the default Worker.
   */
  constructor(worker?: Worker) {
    // Either use the given worker, or instantiate the default worker.
    this._worker = worker || new Worker(LMLayer.asBlobURI(LMLayerWorkerCode));
    this._worker.onmessage = this.onMessage.bind(this)
    this._declareLMLayerReady = null;
    this._promises = new PromiseStore;
    this._nextToken = Number.MIN_SAFE_INTEGER;
  }

  /**
   * Initializes the LMLayer worker with the keyboard/platform's capabilities,
   * as well as a description of the model required.
   */
  initialize(capabilities: Capabilities, model: ModelDescription): Promise<Configuration> {
    return new Promise((resolve, _reject) => {
      this._worker.postMessage({
        message: 'initialize',
        capabilities,
        model
      });

      // Sets up so the promise is resolved in the onMessage() callback, when it receives
      // the 'ready' message.
      this._declareLMLayerReady = resolve;
    });
  }

  predict(transform: Transform, context: Context): Promise<Suggestion[]> {
    let token = this._nextToken++;
    return new Promise((resolve, reject) => {
      this._promises.make(token, resolve, reject);
      this._worker.postMessage({
        message: 'predict',
        token: token,
        transform: transform,
        context: context,
      });
    });
  }

  // TODO: asynchronous close() method.
  //       Worker code must recognize message and call self.close().

  private onMessage(event: MessageEvent): void {
    let payload: OutgoingMessage = event.data;
    if (payload.message === 'ready') {
      this._declareLMLayerReady(event.data.configuration);
    } else if (payload.message === 'suggestions') {
      this._promises.keep(payload.token, payload.suggestions);
    } else {
      // This branch should never execute, but just in case...
      //@ts-ignore
      throw new Error(`Message not implemented: ${payload.message}`);
    }
  }

  /**
   * Given a function, this utility returns the source code within it, as a string.
   * This is intended to unwrap the "wrapped" source code created in the LMLayerWorker
   * build process.
   *
   * @param fn The function whose body will be returned.
   */
  static unwrap(fn: Function): string {
      let wrapper = fn.toString();
      let match = wrapper.match(/function[^{]+{((?:.|\r|\n)+)}[^}]*$/);
      return match[1];
  }

  /**
   * Converts the INSIDE of a function into a blob URI that can
   * be passed as a valid URI for a Worker.
   * @param fn Function whose body will be referenced by a URI.
   * 
   * This function makes the following possible:
   * 
   *    let worker = new Worker(LMLayer.asBlobURI(function myWorkerCode () {
   *      postMessage('inside Web Worker')
   *      function onmessage(event) {
   *        // handle message inside Web Worker.
   *      }
   *    }));
   */
  static asBlobURI(fn: Function): string {
    let code = LMLayer.unwrap(fn);
    let blob = new Blob([code], { type: 'text/javascript' });
    return URL.createObjectURL(blob);
  }
}

// Let LMLayer be available both in the browser and in Node.
if (typeof module !== 'undefined' && typeof module.exports !== 'undefined') {
  module.exports = LMLayer;
  //@ts-ignore
  LMLayer.PromiseStore = PromiseStore;
} else {
  //@ts-ignore
  window.LMLayer = LMLayer;
}
