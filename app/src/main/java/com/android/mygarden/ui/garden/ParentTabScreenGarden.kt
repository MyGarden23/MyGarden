package com.android.mygarden.ui.garden

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R
import com.android.mygarden.model.offline.OfflineStateManager
import com.android.mygarden.ui.achievements.AchievementsScreen
import com.android.mygarden.ui.achievements.AchievementsViewModel
import com.android.mygarden.ui.achievements.AchievementsViewModelFactory
import com.android.mygarden.ui.navigation.NavigationButton
import com.android.mygarden.ui.navigation.NavigationTestTags

enum class GardenTab(@StringRes val titleRes: Int) {
  GARDEN(R.string.garden_screen_title),
  ACHIEVEMENTS(R.string.achievements_screen_title)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentTabScreenGarden(
    modifier: Modifier = Modifier,
    friendId: String? = null,
    isViewMode: Boolean = false,
    gardenCallbacks: GardenScreenCallbacks,
    gardenViewModel: GardenViewModel =
        viewModel(factory = GardenViewModelFactory(friendId = friendId)),
    achievementsViewModel: AchievementsViewModel =
        viewModel(factory = AchievementsViewModelFactory(friendId = friendId))
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableStateOf(GardenTab.GARDEN) }

  val isOnline by OfflineStateManager.isOnline.collectAsState()

  // Garden UI state used to show the profile in the TopBar
  val gardenUiState by gardenViewModel.uiState.collectAsState()

  Scaffold(
      modifier = modifier.testTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN),
      topBar = {
        // One common top bar for both tabs
        CenterAlignedTopAppBar(
            title = {
              ProfileRow(
                  gardenCallbacks.onEditProfile,
                  gardenCallbacks.onSignOut,
                  modifier,
                  gardenUiState,
                  isOnline,
                  isViewMode)
            },
            navigationIcon = {
              if (isViewMode) {
                NavigationButton(gardenCallbacks.onBackPressed)
              }
            })
      },
      floatingActionButton = {
        // FAB only on Garden tab and only when not in view mode
        if (!isViewMode && selectedTab == GardenTab.GARDEN) {
          AddPlantFloatingButton(
              onAddPlant = gardenCallbacks.onAddPlant, modifier = modifier, isOnline = isOnline)
        }
      },
      floatingActionButtonPosition = FabPosition.Start,
      containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Column(
        modifier = modifier.padding(innerPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Surface(
              modifier = modifier.padding(horizontal = 16.dp).clip(RoundedCornerShape(50)),
              color = MaterialTheme.colorScheme.background,
              tonalElevation = 2.dp) {
                Row(
                    modifier = modifier.height(44.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically) {
                      GardenTab.entries.forEachIndexed { index, tab ->
                        val selected = index == selectedTab.ordinal

                        Box(
                            modifier =
                                modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable { selectedTab = tab }
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.surface
                                        else Color.Transparent)
                                    .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center) {
                              Text(
                                  text = stringResource(tab.titleRes),
                                  style = MaterialTheme.typography.bodyLarge,
                                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                      }
                    }
              }

          when (selectedTab) {
            GardenTab.GARDEN -> {
              GardenScreen(
                  modifier = modifier,
                  friendId = friendId,
                  isViewMode = isViewMode,
                  gardenViewModel = gardenViewModel,
                  callbacks = gardenCallbacks,
                  isOnline = isOnline)
            }
            GardenTab.ACHIEVEMENTS -> {
              AchievementsScreen(
                  modifier = modifier, viewModel = achievementsViewModel, isOnline = isOnline)
            }
          }
        }
  }
}
