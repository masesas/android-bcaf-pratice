package com.masesas.exercise.bcaf_test_1.core.database

import androidx.room.TypeConverter
import java.math.BigDecimal

/** Nominal disimpan sebagai TEXT agar presisinya tidak hilang. */
class BigDecimalConverter {

    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)
}
