/*
 * Copyright 2016 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linecorp.armeria.internal;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Stream;

/**
 * Handles GOAWAY frames.
 */
public final class Http2GoAwayHandler {

    private static final Logger logger = LoggerFactory.getLogger(Http2GoAwayHandler.class);

    private boolean goAwaySent;
    private long goAwayReceived; // -1 if not received, errorCode if received.

    /**
     * Returns {@code true} if the connection has sent a GOAWAY frame.
     */
    public boolean sentGoAway() {
        return goAwaySent;
    }

    /**
     * Returns {@code true} if the connection has received a GOAWAY frame.
     */
    public boolean receivedGoAway() {
        return goAwayReceived >= 0;
    }

    /**
     * Returns {@code true} if the connection has received a GOAWAY frame with non-zero error code.
     */
    public boolean receivedErrorGoAway() {
        return goAwayReceived > Http2Error.NO_ERROR.code();
    }

    public void onGoAwaySent(Channel channel, int lastStreamId, long errorCode, ByteBuf debugData) {
        goAwaySent = true;
        onGoAway(channel, "Sent", lastStreamId, errorCode, debugData);
    }

    public void onGoAwayReceived(Channel channel, int lastStreamId, long errorCode, ByteBuf debugData) {
        goAwayReceived = errorCode;

        onGoAway(channel, "Received", lastStreamId, errorCode, debugData);

        // Send a GOAWAY back to the peer and close the connection gracefully if we did not send GOAWAY yet.
        // This makes sure that the connection is closed eventually once we receive GOAWAY.
        if (!goAwaySent) {
            // This does not close the connection immediately but sends a GOAWAY frame.
            // The connection will be closed when all streams are closed.
            // See AbstractHttp2ConnectionHandler.close().
            channel.close();
        }
    }

    private static void onGoAway(Channel channel, String sentOrReceived,
                                 int lastStreamId, long errorCode, ByteBuf debugData) {
        if (errorCode != Http2Error.NO_ERROR.code()) {
            if (logger.isWarnEnabled()) {
                logger.warn("{} {} a GOAWAY frame: lastStreamId={}, errorCode={}, debugData=\"{}\"",
                            channel, sentOrReceived, lastStreamId, errorStr(errorCode),
                            debugData.toString(StandardCharsets.UTF_8));
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("{} {} a GOAWAY frame: lastStreamId={}, errorCode=NO_ERROR(0)",
                             channel, sentOrReceived, lastStreamId);
            }
        }
    }

    private static String errorStr(long errorCode) {
        final Http2Error error = Http2Error.valueOf(errorCode);
        return error != null ? error.toString() + '(' + errorCode + ')'
                             : "UNKNOWN(" + errorCode + ')';
    }

    public void onStreamClosed(Channel channel, Http2Stream stream) {
        if (stream.id() == 1) {
            logger.debug("{} HTTP/2 upgrade stream closed: {}", channel, stream.state());
        }
    }
}