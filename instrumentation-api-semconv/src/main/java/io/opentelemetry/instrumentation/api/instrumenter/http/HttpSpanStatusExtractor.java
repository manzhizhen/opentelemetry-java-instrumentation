/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.http;

import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import javax.annotation.Nullable;

/**
 * Extractor of the <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/main/docs/http/http-spans.md#status">HTTP
 * span status</a>. Instrumentation of HTTP server or client frameworks should use this class to
 * comply with OpenTelemetry HTTP semantic conventions.
 */
public final class HttpSpanStatusExtractor<REQUEST, RESPONSE>
    implements SpanStatusExtractor<REQUEST, RESPONSE> {

  /**
   * Returns the {@link SpanStatusExtractor} for HTTP requests, which will use the HTTP status code
   * to determine the {@link StatusCode} if available or fallback to {@linkplain #getDefault() the
   * default status} otherwise.
   */
  public static <REQUEST, RESPONSE> SpanStatusExtractor<REQUEST, RESPONSE> create(
      HttpClientAttributesGetter<? super REQUEST, ? super RESPONSE> getter) {
    return new HttpSpanStatusExtractor<>(getter, HttpStatusCodeConverter.CLIENT);
  }

  /**
   * Returns the {@link SpanStatusExtractor} for HTTP requests, which will use the HTTP status code
   * to determine the {@link StatusCode} if available or fallback to {@linkplain #getDefault() the
   * default status} otherwise.
   */
  public static <REQUEST, RESPONSE> SpanStatusExtractor<REQUEST, RESPONSE> create(
      HttpServerAttributesGetter<? super REQUEST, ? super RESPONSE> getter) {
    return new HttpSpanStatusExtractor<>(getter, HttpStatusCodeConverter.SERVER);
  }

  private final HttpCommonAttributesGetter<? super REQUEST, ? super RESPONSE> getter;
  private final HttpStatusCodeConverter statusCodeConverter;

  private HttpSpanStatusExtractor(
      HttpCommonAttributesGetter<? super REQUEST, ? super RESPONSE> getter,
      HttpStatusCodeConverter statusCodeConverter) {
    this.getter = getter;
    this.statusCodeConverter = statusCodeConverter;
  }

  @Override
  public void extract(
      SpanStatusBuilder spanStatusBuilder,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {

    if (response != null) {
      Integer statusCode = getter.getHttpResponseStatusCode(request, response, error);
      if (statusCode != null) {
        if (statusCodeConverter.isError(statusCode)) {
          spanStatusBuilder.setStatus(StatusCode.ERROR);
          return;
        }
      }
    }
    SpanStatusExtractor.getDefault().extract(spanStatusBuilder, request, response, error);
  }
}
