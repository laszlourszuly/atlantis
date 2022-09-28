package com.echsylon.atlantis.message

/**
 * The set of defined WebSocket close reasons.
 */
enum class Reason(val code: Int) {
    NORMAL_CLOSURE(1000),
    GOING_AWAY(1001),
    PROTOCOL_ERROR(1002),
    UNSUPPORTED_DATA(1003),
    NO_STATUS_RECEIVED(1005),
    ABNORMAL_CLOSURE(1006),
    INVALID_FRAME_PAYLOAD_DATA(1007),
    POLICY_VIOLATION(1008),
    MESSAGE_TO_BIG(1009),
    MISSING_EXTENSION(1010),
    INTERNAL_ERROR(1011),
    SERVICE_RESTART(1012),
    TRY_AGAIN_LATER(1013),
    BAD_GATEWAY(1014),
    TLS_HANDSHAKE(1015);

    companion object {
        fun fromInt(type: Int): Reason? =
            values().associateBy(Reason::code)[type]
    }
}
