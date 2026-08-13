package com.example.calculators

import kotlin.math.pow

object FinanceCalculators {

    data class LoanInput(
        val principal: Double,
        val annualInterestRate: Double,
        val termInYears: Double
    )

    data class LoanResult(
        val monthlyEmi: Double,
        val totalInterest: Double,
        val totalPayment: Double
    )

    fun calculateLoanEmi(input: LoanInput): LoanResult {
        val p = input.principal
        val r = input.annualInterestRate / 12 / 100
        val n = input.termInYears * 12

        if (n <= 0) {
             return LoanResult(0.0, 0.0, p)
        }

        val emi = if (r == 0.0) {
            p / n
        } else {
            p * r * (1 + r).pow(n) / ((1 + r).pow(n) - 1)
        }

        val totalPayment = emi * n
        val totalInterest = totalPayment - p

        return LoanResult(emi, totalInterest, totalPayment)
    }

    data class DiscountInput(
        val originalPrice: Double,
        val discountPercentage: Double,
        val additionalDiscountPercentage: Double? = null
    )

    data class DiscountResult(
        val finalPrice: Double,
        val amountSaved: Double
    )

    fun calculateDiscount(input: DiscountInput): DiscountResult {
        val originalPrice = input.originalPrice
        val discount = input.discountPercentage
        val additionalDiscount = input.additionalDiscountPercentage ?: 0.0

        val priceAfterFirstDiscount = originalPrice * (1 - discount / 100.0)
        val finalPrice = priceAfterFirstDiscount * (1 - additionalDiscount / 100.0)
        val amountSaved = originalPrice - finalPrice

        return DiscountResult(finalPrice, amountSaved)
    }
}
