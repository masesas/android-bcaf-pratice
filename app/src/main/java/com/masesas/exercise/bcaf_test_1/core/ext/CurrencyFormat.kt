package com.masesas.exercise.bcaf_test_1.core.ext

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val rupiahFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale("in", "ID"))

/** Format nominal tanpa desimal, mis. 5.000.000 — dipakai UI legacy maupun Compose. */
fun BigDecimal.toRupiahDigits(): String = rupiahFormat.format(this)
