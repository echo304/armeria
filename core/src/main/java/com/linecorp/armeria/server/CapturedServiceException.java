/*
 * Copyright 2021 LINE Corporation
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
package com.linecorp.armeria.server;

import com.linecorp.armeria.common.annotation.Nullable;

import io.netty.util.AttributeKey;

// TODO(minwoox): Use RequestLogBuilder.responseCause() instead.
@SuppressWarnings("NonExceptionNameEndsWithException")
final class CapturedServiceException {

    private static final AttributeKey<Throwable> CAPTURED_SERVICE_EXCEPTION =
            AttributeKey.valueOf(CapturedServiceException.class, "CAPTURED_SERVICE_EXCEPTION");

    static void set(ServiceRequestContext ctx, Throwable cause) {
        ctx.setAttr(CAPTURED_SERVICE_EXCEPTION, cause);
    }

    @Nullable
    static Throwable get(ServiceRequestContext ctx) {
        return ctx.attr(CAPTURED_SERVICE_EXCEPTION);
    }

    private CapturedServiceException() {}
}