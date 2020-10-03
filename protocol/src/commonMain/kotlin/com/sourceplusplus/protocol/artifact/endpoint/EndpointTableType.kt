package com.sourceplusplus.protocol.artifact.endpoint

import com.sourceplusplus.protocol.TableType

enum class EndpointTableType : TableType {
    NAME,
    TYPE,
    AVG_THROUGHPUT,
    AVG_RESPONSE_TIME,
    AVG_SLA;

    override val isCentered: Boolean = false
    override val description = name
}