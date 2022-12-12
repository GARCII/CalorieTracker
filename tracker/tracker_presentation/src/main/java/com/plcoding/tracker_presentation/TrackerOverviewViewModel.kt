package com.plcoding.tracker_presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plcoding.core.domain.preferences.Preferences
import com.plcoding.core.navigation.Route
import com.plcoding.core.util.UiEvent
import com.plcoding.tracker_domain.use_case.TrackerUseCases
import com.plcoding.tracker_presentation.tracker_overview.TrackerOverviewEvent
import com.plcoding.tracker_presentation.tracker_overview.TrackerOverviewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackerOverviewViewModel @Inject constructor(
    private val preferences: Preferences,
    private val trackerUseCases: TrackerUseCases
) : ViewModel() {

    var state by mutableStateOf(TrackerOverviewState())
        private set
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    var getFoodsForDateJob: Job? = null

    init {

        preferences.saveShouldShowOnBoarding(false)
    }

    fun onEvent(event: TrackerOverviewEvent) {
        when (event) {
            is TrackerOverviewEvent.OnAddFoodClick -> {
                viewModelScope.launch {
                    _uiEvent.send(
                        UiEvent.Navigate(
                            route = Route.SEARCH
                                    + "/${event.meal.mealType.name}"
                                    + "/${state.date.dayOfMonth}"
                                    + "/${state.date.monthValue}"
                                    + "/${state.date.dayOfYear}"
                        )
                    )
                }
            }
            is TrackerOverviewEvent.OnDeleteTrackedFoodClick -> {
                viewModelScope.launch {
                    trackerUseCases.deleteTrackedFood(event.trackedFood)
                    refreshFood()
                }
            }
            TrackerOverviewEvent.OnNextDayClick -> {
                state = state.copy(
                    date = state.date.plusDays(1)
                )
                refreshFood()
            }
            TrackerOverviewEvent.OnPreviousDayClicked -> {
                state = state.copy(
                    date = state.date.minusDays(1)
                )
                refreshFood()
            }
            is TrackerOverviewEvent.OnToggleMealClick -> {
                state = state.copy(
                    meals = state.meals.map {
                        if(it.name == event.meal.name) {
                            it.copy(isExpanded = !it.isExpanded)
                        } else it
                    }
                )
            }
        }
    }

    private fun refreshFood() {
        getFoodsForDateJob?.cancel()
        getFoodsForDateJob = trackerUseCases.getFoodsForDate(state.date)
            .onEach { foods ->
                val nutrientsResult = trackerUseCases.calculateMealNutrients(foods)
                state = state.copy(
                    totalCarbs = nutrientsResult.totalCarbs,
                    totalFat = nutrientsResult.totalFat,
                    totalProtein = nutrientsResult.totalProtein,
                    totalCalories = nutrientsResult.totalCalories,
                    carbsGoal = nutrientsResult.carbsGoal,
                    proteinGoal = nutrientsResult.proteinGoal,
                    fatGoal = nutrientsResult.fatGoal,
                    caloriesGoal = nutrientsResult.caloriesGoal,
                    meals = state.meals.map {
                        val nutrientsForMeal = nutrientsResult.mealNutrients[it.mealType]
                            ?: return@map it.copy(
                                carbs = 0,
                                protein = 0,
                                fat = 0,
                                calories = 0
                            )
                        it.copy(
                            carbs = nutrientsForMeal.carbs,
                            protein = nutrientsForMeal.carbs,
                            fat = nutrientsForMeal.fat
                        )
                    }
                )
            }.launchIn(viewModelScope)
    }
}