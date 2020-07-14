package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Used to query core for an artifact's trace spans stacks.
 *
 * @version 0.3.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@SourceStyle
@Value.Immutable
@JsonSerialize(as = TraceSpanStackQuery.class)
@JsonDeserialize(as = TraceSpanStackQuery.class)
public interface AbstractTraceSpanStackQuery {

    boolean oneLevelDeep();

    String traceId();

    @Nullable
    String segmentId();

    @Nullable
    Long spanId();

    @Value.Default
    default boolean followExit() {
        return false;
    }

    @Nullable
    Boolean systemRequest();

    //todo: spanId and segmentId can't be null at same time
}
