package com.masesas.exercise.bcaf_test_1.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal

/** Membaca/menulis nominal JSON sebagai angka tanpa kutip, tanpa melewati Double. */
object BigDecimalSerializer : KSerializer<BigDecimal> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val raw = (decoder as? JsonDecoder)
            ?.decodeJsonElement()
            ?.jsonPrimitive
            ?.content
            ?: decoder.decodeString()

        return BigDecimal(raw)
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: return encoder.encodeString(value.toPlainString())

        jsonEncoder.encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
    }
}
