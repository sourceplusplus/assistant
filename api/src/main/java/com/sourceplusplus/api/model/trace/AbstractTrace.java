package com.sourceplusplus.api.model.trace;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sourceplusplus.api.model.SourceStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A specific trace result.
 *
 * @version 0.3.2
 * @since 0.1.0
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 */
@SourceStyle
@Value.Immutable
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonSerialize(as = Trace.class)
@JsonDeserialize(as = Trace.class)
public interface AbstractTrace {

    @Nullable //when partial
    String key();

    @JsonAlias({"operationNames"})
    List<String> operationNames();

    int duration();

    long start();

    @Value.Auxiliary
    @JsonAlias({"isError", "error"})
    boolean isError();

    @JsonAlias({"traceIds"})
    List<String> traceIds();

    @Nullable
    @Value.Auxiliary
    String prettyDuration();

    @Nullable
    @JsonAlias({"isPartial", "partial"})
    Boolean isPartial();
}
