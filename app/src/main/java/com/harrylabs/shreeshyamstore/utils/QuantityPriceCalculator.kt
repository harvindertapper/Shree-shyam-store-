package com.harrylabs.shreeshyamstore.utils

import java.math.BigInteger

enum class ProductUnitType {
    PIECE,
    WEIGHT,
    VOLUME
}

enum class QuantityDisplayUnit(
    val unitType: ProductUnitType,
    val baseUnitsPerDisplayUnit: Long,
    val maxFractionDigits: Int
) {
    PIECE(ProductUnitType.PIECE, baseUnitsPerDisplayUnit = 1, maxFractionDigits = 0),
    GRAM(ProductUnitType.WEIGHT, baseUnitsPerDisplayUnit = 1, maxFractionDigits = 0),
    KILOGRAM(ProductUnitType.WEIGHT, baseUnitsPerDisplayUnit = 1_000, maxFractionDigits = 3),
    MILLILITER(ProductUnitType.VOLUME, baseUnitsPerDisplayUnit = 1, maxFractionDigits = 0),
    LITER(ProductUnitType.VOLUME, baseUnitsPerDisplayUnit = 1_000, maxFractionDigits = 3)
}

data class UnitRate(
    val pricePerUnitPaise: Long,
    val priceUnitBaseQty: Long
)

data class LineAmountResult(
    val lineTotalPaise: Long,
    val originalRate: UnitRate,
    val effectiveRate: UnitRate,
    val rateOverridden: Boolean
)

enum class CalculationError {
    INVALID_RATE,
    INVALID_QUANTITY,
    INVALID_AMOUNT,
    FRACTIONAL_PIECE,
    UNSUPPORTED_PRECISION,
    MALFORMED_INPUT
}

sealed class CalculationResult<out T> {
    data class Success<T>(val value: T) : CalculationResult<T>()
    data class Failure(val error: CalculationError) : CalculationResult<Nothing>()
}

object QuantityPriceCalculator {
    private val decimalPattern = Regex("""\d+(\.\d+)?""")
    private val negativeDecimalPattern = Regex("""-\d+(\.\d+)?""")

    fun parseQuantityBase(input: String, unit: QuantityDisplayUnit): CalculationResult<Long> {
        val trimmed = input.trim()
        if (unit == QuantityDisplayUnit.PIECE && trimmed.contains(".")) {
            return CalculationResult.Failure(CalculationError.FRACTIONAL_PIECE)
        }

        return parsePositiveDecimal(
            input = trimmed,
            scale = unit.baseUnitsPerDisplayUnit,
            maxFractionDigits = unit.maxFractionDigits,
            invalidError = CalculationError.INVALID_QUANTITY
        )
    }

    fun parseAmountPaise(input: String): CalculationResult<Long> {
        return parsePositiveDecimal(
            input = input.trim(),
            scale = 100,
            maxFractionDigits = 2,
            invalidError = CalculationError.INVALID_AMOUNT
        )
    }

    fun amountForQuantity(quantityBase: Long, rate: UnitRate): CalculationResult<Long> {
        if (quantityBase <= 0) {
            return CalculationResult.Failure(CalculationError.INVALID_QUANTITY)
        }
        if (!rate.isValid()) {
            return CalculationResult.Failure(CalculationError.INVALID_RATE)
        }

        return CalculationResult.Success(
            multiplyDivideHalfUp(
                multiplicand = quantityBase,
                multiplier = rate.pricePerUnitPaise,
                divisor = rate.priceUnitBaseQty
            )
        )
    }

    fun quantityForAmount(
        amountPaise: Long,
        rate: UnitRate,
        unitType: ProductUnitType
    ): CalculationResult<Long> {
        if (amountPaise <= 0) {
            return CalculationResult.Failure(CalculationError.INVALID_AMOUNT)
        }
        if (!rate.isValid()) {
            return CalculationResult.Failure(CalculationError.INVALID_RATE)
        }
        if (unitType == ProductUnitType.PIECE && !dividesExactly(amountPaise, rate.priceUnitBaseQty, rate.pricePerUnitPaise)) {
            return CalculationResult.Failure(CalculationError.FRACTIONAL_PIECE)
        }

        val quantityBase = multiplyDivideHalfUp(
            multiplicand = amountPaise,
            multiplier = rate.priceUnitBaseQty,
            divisor = rate.pricePerUnitPaise
        )

        return if (quantityBase > 0) {
            CalculationResult.Success(quantityBase)
        } else {
            CalculationResult.Failure(CalculationError.INVALID_QUANTITY)
        }
    }

    fun lineAmount(
        quantityBase: Long,
        originalRate: UnitRate,
        overrideRate: UnitRate? = null
    ): CalculationResult<LineAmountResult> {
        val effectiveRate = overrideRate ?: originalRate
        return when (val amountResult = amountForQuantity(quantityBase, effectiveRate)) {
            is CalculationResult.Failure -> amountResult
            is CalculationResult.Success -> CalculationResult.Success(
                LineAmountResult(
                    lineTotalPaise = amountResult.value,
                    originalRate = originalRate,
                    effectiveRate = effectiveRate,
                    rateOverridden = overrideRate != null && overrideRate != originalRate
                )
            )
        }
    }

    private fun UnitRate.isValid(): Boolean {
        return pricePerUnitPaise > 0 && priceUnitBaseQty > 0
    }

    private fun parsePositiveDecimal(
        input: String,
        scale: Long,
        maxFractionDigits: Int,
        invalidError: CalculationError
    ): CalculationResult<Long> {
        if (input.isBlank()) {
            return CalculationResult.Failure(CalculationError.MALFORMED_INPUT)
        }
        if (negativeDecimalPattern.matches(input)) {
            return CalculationResult.Failure(invalidError)
        }
        if (!decimalPattern.matches(input)) {
            return CalculationResult.Failure(CalculationError.MALFORMED_INPUT)
        }

        val parts = input.split(".")
        val whole = parts[0].toLongOrNull()
            ?: return CalculationResult.Failure(invalidError)
        val fraction = parts.getOrNull(1).orEmpty()
        if (fraction.length > maxFractionDigits) {
            return CalculationResult.Failure(CalculationError.UNSUPPORTED_PRECISION)
        }

        val wholeBase = whole.checkedMultiply(scale)
            ?: return CalculationResult.Failure(invalidError)
        val fractionBase = if (fraction.isEmpty()) {
            0
        } else {
            val paddedFraction = fraction.padEnd(maxFractionDigits, '0')
            val divisor = powerOfTen(maxFractionDigits)
            val fractionValue = paddedFraction.toLongOrNull()
                ?: return CalculationResult.Failure(invalidError)
            multiplyDivideHalfUp(
                multiplicand = fractionValue,
                multiplier = scale,
                divisor = divisor
            )
        }
        val value = wholeBase.checkedAdd(fractionBase)
            ?: return CalculationResult.Failure(invalidError)

        return if (value > 0) {
            CalculationResult.Success(value)
        } else {
            CalculationResult.Failure(invalidError)
        }
    }

    private fun powerOfTen(exponent: Int): Long {
        var value = 1L
        repeat(exponent) {
            value *= 10
        }
        return value
    }

    private fun multiplyDivideHalfUp(multiplicand: Long, multiplier: Long, divisor: Long): Long {
        val product = BigInteger.valueOf(multiplicand).multiply(BigInteger.valueOf(multiplier))
        val divisorValue = BigInteger.valueOf(divisor)
        val quotientAndRemainder = product.divideAndRemainder(divisorValue)
        val doubledRemainder = quotientAndRemainder[1].multiply(BigInteger.valueOf(2))
        val rounded = if (doubledRemainder >= divisorValue) {
            quotientAndRemainder[0] + BigInteger.ONE
        } else {
            quotientAndRemainder[0]
        }
        return rounded.longValueExact()
    }

    private fun dividesExactly(multiplicand: Long, multiplier: Long, divisor: Long): Boolean {
        val product = BigInteger.valueOf(multiplicand).multiply(BigInteger.valueOf(multiplier))
        return product.mod(BigInteger.valueOf(divisor)) == BigInteger.ZERO
    }

    private fun Long.checkedMultiply(other: Long): Long? {
        return try {
            Math.multiplyExact(this, other)
        } catch (exception: ArithmeticException) {
            null
        }
    }

    private fun Long.checkedAdd(other: Long): Long? {
        return try {
            Math.addExact(this, other)
        } catch (exception: ArithmeticException) {
            null
        }
    }
}
