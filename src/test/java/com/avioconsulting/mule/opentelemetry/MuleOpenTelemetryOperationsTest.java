package com.avioconsulting.mule.opentelemetry;

import com.avioconsulting.mule.opentelemetry.internal.opentelemetry.sdk.test.DelegatedLoggingSpanTestExporter;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Test;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class MuleOpenTelemetryOperationsTest extends AbstractMuleArtifactTraceTest {

  @Override
  protected String getConfigFile() {
    return "mule-opentelemetry-operations.xml";
  }

  @Test
  public void testHttpTracing_WithCustomTags() throws Exception {
    sendRequest(CORRELATION_ID, "tags", 200, Collections.emptyMap(),
        Collections.singletonMap("orderId", "order123"));
    await().untilAsserted(() -> assertThat(DelegatedLoggingSpanTestExporter.spanQueue)
        .hasSize(1)
        .element(0)
        .extracting("spanName", "spanKind", "spanStatus")
        .containsOnly("GET /tags", "SERVER", "UNSET"));
    assertThat(DelegatedLoggingSpanTestExporter.spanQueue)
        .element(0)
        .extracting("attributes", InstanceOfAssertFactories.map(String.class, String.class))
        .containsEntry("http.response.status_code", "200")
        .containsEntry("custom.orderId", "order123")
        .containsEntry("custom.quantity", "20")
        .containsEntry("custom.payload", "Tag Payload");
  }

  @Test
  public void testHttpTracing_WithTransactionTags() throws Exception {
    sendRequest(CORRELATION_ID, "transaction-tags", 200, Collections.emptyMap(),
        Collections.singletonMap("orderId", "order123"));
    await().untilAsserted(() -> assertThat(DelegatedLoggingSpanTestExporter.spanQueue)
        .hasSize(1)
        .element(0)
        .extracting("spanName", "spanKind", "spanStatus")
        .containsOnly("GET /transaction-tags", "SERVER", "UNSET"));
    assertThat(DelegatedLoggingSpanTestExporter.spanQueue)
        .element(0)
        .extracting("attributes", InstanceOfAssertFactories.map(String.class, String.class))
        .containsEntry("http.response.status_code", "200")
        .containsEntry("custom.orderId", "order123")
        .containsEntry("custom.quantity", "20")
        .containsEntry("custom.payload", "Tag Payload");
  }

  @Test
  public void testHttpTracing_GetTraceContext() throws Exception {
    CoreEvent coreEvent = flowRunner("mule-opentelemetry-get-trace-context").run();
    assertThat(coreEvent.getVariables())
        .as("Variables that should contain OTEL injected context")
        .containsKeys("OTEL_TRACE_CONTEXT", "OTEL_CONTEXT");
    TypedValue<Map<String, String>> otel_trace_context_from_interceptor = (TypedValue<Map<String, String>>) coreEvent
        .getVariables().get("OTEL_TRACE_CONTEXT");
    TypedValue<Map<String, String>> otel_context_from_operation = (TypedValue<Map<String, String>>) coreEvent
        .getVariables().get("OTEL_CONTEXT");
    assertThat(otel_trace_context_from_interceptor.getValue())
        .containsExactlyEntriesOf(otel_context_from_operation.getValue());
    assertThat(otel_trace_context_from_interceptor.getValue())
        .containsKeys("traceId", "spanId", "spanIdLong", "traceIdLongLowPart", "TRACE_TRANSACTION_ID");
    assertThat(otel_trace_context_from_interceptor.getValue())
        .hasEntrySatisfying("spanIdLong", (value) -> assertThat(Long.parseUnsignedLong(value)).isNotEqualTo(0));
    assertThat(otel_trace_context_from_interceptor.getValue())
        .hasEntrySatisfying("traceIdLongLowPart",
            (value) -> assertThat(Long.parseUnsignedLong(value)).isNotEqualTo(0));
  }

  @Test
  public void testHttpTracing_GetCurrentTraceContext() throws Exception {
    CoreEvent coreEvent = flowRunner("mule-opentelemetry-get-current-trace-context")
        .withSourceCorrelationId("test_123").run();
    assertThat(coreEvent.getVariables())
        .as("Variables that should contain OTEL injected context")
        .containsKeys("OTEL_TRACE_CONTEXT", "OTEL_CONTEXT");
    TypedValue<Map<String, String>> otel_trace_context_from_interceptor = (TypedValue<Map<String, String>>) coreEvent
        .getVariables().get("OTEL_TRACE_CONTEXT");
    TypedValue<Map<String, String>> otel_context_from_operation = (TypedValue<Map<String, String>>) coreEvent
        .getVariables().get("OTEL_CONTEXT");
    assertThat(otel_trace_context_from_interceptor.getValue())
        .containsExactlyEntriesOf(otel_context_from_operation.getValue());
    assertThat(otel_trace_context_from_interceptor.getValue())
        .containsKeys("traceId", "spanId", "spanIdLong", "traceIdLongLowPart", "TRACE_TRANSACTION_ID");
    assertThat(otel_trace_context_from_interceptor.getValue())
        .hasEntrySatisfying("spanIdLong", (value) -> assertThat(Long.parseUnsignedLong(value)).isNotEqualTo(0));
    assertThat(otel_trace_context_from_interceptor.getValue())
        .hasEntrySatisfying("traceIdLongLowPart",
            (value) -> assertThat(Long.parseUnsignedLong(value)).isNotEqualTo(0));
  }

}
