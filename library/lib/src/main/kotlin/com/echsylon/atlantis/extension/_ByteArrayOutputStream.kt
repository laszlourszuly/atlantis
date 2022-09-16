package com.echsylon.atlantis.extension

import java.io.ByteArrayOutputStream

fun ByteArrayOutputStream.writeIfNotNull(bytes: ByteArray?) {
    bytes?.let { write(it) }
}
