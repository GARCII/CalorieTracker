package com.plcoding.onboarding_presentation.nutrient

sealed class NutrientGoalTypeEvent {
    data class OnCarbRatioEnter(val ratio: String) : NutrientGoalTypeEvent()
    data class OnProteinRatioEnter(val ratio: String) : NutrientGoalTypeEvent()
    data class OnFatRatioEnter(val ratio: String) : NutrientGoalTypeEvent()
    object OnNextClick : NutrientGoalTypeEvent()
}