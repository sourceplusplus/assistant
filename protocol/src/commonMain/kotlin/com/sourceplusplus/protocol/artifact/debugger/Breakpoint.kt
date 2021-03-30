package com.sourceplusplus.protocol.artifact.debugger

/**
 * todo: description.
 *
 * @since 0.2.2
 * @author [Brandon Fergerson](mailto:bfergerson@apache.org)
 */
data class Breakpoint(
    val id: String,
    val location: Location,
    val condition: Any,
)