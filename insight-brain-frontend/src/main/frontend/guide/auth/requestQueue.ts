/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

interface QueueEntry {
  thunk: () => Promise<Response>;
  resolve: (response: Response) => void;
  reject: (error: Error) => void;
}

export class RequestQueue {
  private static readonly MAX_SIZE = 50;
  private queue: QueueEntry[] = [];
  private _isReauthenticating = false;

  get size(): number {
    return this.queue.length;
  }

  get isReauthenticating(): boolean {
    return this._isReauthenticating;
  }

  enqueue(thunk: () => Promise<Response>): Promise<Response> {
    if (this.queue.length >= RequestQueue.MAX_SIZE) {
      return Promise.reject(new Error('Request queue full'));
    }
    this._isReauthenticating = true;
    return new Promise<Response>((resolve, reject) => {
      this.queue.push({ thunk, resolve, reject });
    });
  }

  async replayAll(): Promise<void> {
    const entries = this.queue.splice(0);
    this._isReauthenticating = false;
    await Promise.allSettled(entries.map(async (entry) => {
      try {
        const response = await entry.thunk();
        entry.resolve(response);
      } catch (error) {
        entry.reject(error instanceof Error ? error : new Error(String(error)));
      }
    }));
  }

  rejectAll(error?: Error): void {
    const entries = this.queue.splice(0);
    this._isReauthenticating = false;
    const rejectError = error ?? new Error('Authentication cancelled');
    for (const entry of entries) {
      entry.reject(rejectError);
    }
  }
}
