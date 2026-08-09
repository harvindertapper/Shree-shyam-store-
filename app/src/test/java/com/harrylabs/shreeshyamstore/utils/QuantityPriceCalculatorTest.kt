package com.harrylabs.shreeshyamstore.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuantityPriceCalculatorTest {

    @Test
    fun weightQuantityCalculatesAmountInPaise() {
        val rate = UnitRate(pricePerUnitPaise = 4_700, priceUnitBaseQty = 1_000)

        val result = QuantityPriceCalculator.amountForQuantity(
            quantityBase = 160,
            rate = rate
        ).value()

        assertEquals(752, result)
    }

    @Test
    fun amountCalculatesWeightQuantityRoundedToNearestGram() {
        val rate = UnitRate(pricePerUnitPaise = 4_700, priceUnitBaseQty = 1_000)

        val result = QuantityPriceCalculator.quantityForAmount(
            amountPaise = 3_000,
            rate = rate,
            unitType = ProductUnitType.WEIGHT
        ).value()

        assertEquals(638, result)
    }

    @Test
    fun overrideRateCalculatesLineFromEffectiveRateWithoutChangingOriginalRate() {
        val originalRate = UnitRate(pricePerUnitPaise = 2_500, priceUnitBaseQty = 1_000)
        val overrideRate = UnitRate(pricePerUnitPaise = 2_200, priceUnitBaseQty = 1_000)

        val result = QuantityPriceCalculator.lineAmount(
            quantityBase = 500,
            originalRate = originalRate,
            overrideRate = overrideRate
        ).value()

        assertEquals(1_100, result.lineTotalPaise)
        assertEquals(originalRate, result.originalRate)
        assertEquals(overrideRate, result.effectiveRate)
        assertTrue(result.rateOverridden)
    }

    @Test
    fun pieceQuantityCalculatesAmountAndRejectsFractionalPieces() {
        val rate = UnitRate(pricePerUnitPaise = 2_500, priceUnitBaseQty = 1)

        val quantity = QuantityPriceCalculator.parseQuantityBase("2", QuantityDisplayUnit.PIECE).value()
        val amount = QuantityPriceCalculator.amountForQuantity(quantity, rate).value()

        assertEquals(2, quantity)
        assertEquals(5_000, amount)
        assertEquals(
            CalculationError.FRACTIONAL_PIECE,
            QuantityPriceCalculator.parseQuantityBase("1.5", QuantityDisplayUnit.PIECE).error()
        )
    }

    @Test
    fun litreQuantityUsesMillilitreBaseForVolumeAmount() {
        val rate = UnitRate(pricePerUnitPaise = 6_000, priceUnitBaseQty = 1_000)

        val quantity = QuantityPriceCalculator.parseQuantityBase("1.25", QuantityDisplayUnit.LITER).value()
        val amount = QuantityPriceCalculator.amountForQuantity(quantity, rate).value()

        assertEquals(1_250, quantity)
        assertEquals(7_500, amount)
    }

    @Test
    fun invalidZeroNegativeMalformedAndUnsupportedPrecisionInputsAreRejected() {
        val rate = UnitRate(pricePerUnitPaise = 4_700, priceUnitBaseQty = 1_000)

        assertEquals(
            CalculationError.INVALID_QUANTITY,
            QuantityPriceCalculator.amountForQuantity(0, rate).error()
        )
        assertEquals(
            CalculationError.INVALID_QUANTITY,
            QuantityPriceCalculator.parseQuantityBase("-1", QuantityDisplayUnit.GRAM).error()
        )
        assertEquals(
            CalculationError.MALFORMED_INPUT,
            QuantityPriceCalculator.parseAmountPaise("abc").error()
        )
        assertEquals(
            CalculationError.UNSUPPORTED_PRECISION,
            QuantityPriceCalculator.parseAmountPaise("12.345").error()
        )
        assertEquals(
            CalculationError.UNSUPPORTED_PRECISION,
            QuantityPriceCalculator.parseQuantityBase("0.0005", QuantityDisplayUnit.KILOGRAM).error()
        )
    }

    private fun <T> CalculationResult<T>.value(): T {
        return when (this) {
            is CalculationResult.Success -> value
            is CalculationResult.Failure -> error("Expected success but was $error")
        }
    }

    private fun <T> CalculationResult<T>.error(): CalculationError {
        return when (this) {
            is CalculationResult.Success -> error("Expected failure but was $value")
            is CalculationResult.Failure -> error
        }
    }
}
