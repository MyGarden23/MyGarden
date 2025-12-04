package com.android.mygarden.ui.achievements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R

/**
 * Temporary screen used to manually verify that the Achievements flow emits the correct values.
 *
 * This UI is intentionally minimal and not intended for end-users, because a proper design will be
 * implemented next week.
 *
 * The screen observes [AchievementsViewModel.uiState] and shows the current progress levels for
 * each achievement type.
 *
 * @param modifier Optional modifier for layout adjustments.
 * @param viewModel The ViewModel providing achievement state.
 */
@Composable
fun AchievementsScreen(
    modifier: Modifier = Modifier,
    viewModel: AchievementsViewModel = viewModel()
) {
  Scaffold(
      modifier = modifier,
      content = { paddingValues ->
        val uiState by viewModel.uiState.collectAsState()

        // For now just display the levels of each achievements
        Column(modifier = modifier.padding(paddingValues)) {
          Text(text = stringResource(R.string.plants_number_achievement, uiState.plantsNumberLevel))
          Text(
              text =
                  stringResource(R.string.friends_number_achievement, uiState.friendsNumberLevel))
          Text(
              text =
                  stringResource(R.string.healthy_streak_achievement, uiState.healthyStreakLevel))
        }
      })
}
