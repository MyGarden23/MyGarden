package com.android.mygarden.ui.garden

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
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
import com.android.mygarden.ui.utils.OfflineMessages
import com.android.mygarden.ui.utils.handleOfflineClick

object GardenAchievementsParentScreenTestTags {
  const val AVATAR_EDIT_PROFILE = "UserAvatarEditProfile"
  const val PSEUDO = "Pseudo"
  const val LIKE_BUTTON = "LikeButton"
  const val LIKE_COUNTER = "LikeCounter"

  fun getTestTagForTab(tab: GardenTab) = "${tab.titleRes}_Tab"
}

// Padding constants
private val PROFILE_ROW_HORIZONTAL_PADDING = 30.dp
private val PROFILE_ROW_VERTICAL_PADDING = 6.dp
private val COLUMN_VERTICAL_PADDING = 12.dp
private val TAB_EXTERNAL_HORIZONTAL_PADDING = 16.dp
private val TAB_INTERNAL_HORIZONTAL_PADDING = 20.dp
private val INSIDE_TAB_PADDING = 4.dp

// Weights, percentages, etc.
private const val FULL_WEIGHT = 1f
private const val TAB_SHAPE_PERCENTAGE = 50
private val TAB_TONAL_ELEVATION = 2.dp

// Sizes
private val AVATAR_SIZE = 40.dp
private val TAB_HEIGHT = 44.dp

enum class GardenTab(@StringRes val titleRes: Int) {
  GARDEN(R.string.garden_screen_title),
  ACHIEVEMENTS(R.string.achievements_screen_title)
}

/**
 * Parent screen for the Garden section that hosts both the Garden and Achievements tabs.
 *
 * This composable:
 * - Shows a shared top app bar with the user's profile information (avatar, pseudo)
 * - Switches between the Garden and Achievements using a styled tab row
 * - Displays a FAB to add a plant only on the Garden tab and only when not in view mode
 *
 * Online/offline information is read from [OfflineStateManager] to enable/disable actions
 * accordingly in both tabs.
 *
 * @param modifier Optional modifier used to adjust the layout or styling of the root Scaffold.
 * @param friendId Optional ID of a friend whose garden is being viewed. If null, shows the current
 *   user's garden.
 * @param isViewMode If true, the screen runs in read-only mode with changes:
 * - The back button is shown in the top bar instead of the log out button
 * - The "add plant" FAB is hidden
 * - Actions on the plant and the profile are disabled by the child screens
 *
 * @param gardenCallbacks Callbacks for navigation and actions in the Garden flow (editing profile,
 *   signing out, adding a plant, navigating back, etc.).
 * @param gardenViewModel ViewModel that exposes the garden UI state, including the profile
 *   information shown in the top bar. Defaults to a [GardenViewModel] created via
 *   [GardenViewModelFactory] for the given [friendId].
 * @param achievementsViewModel ViewModel providing the achievements UI state for the
 *   [AchievementsScreen]. Defaults to an [AchievementsViewModel] created via
 *   [AchievementsViewModelFactory] for the given [friendId].
 */
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
  var selectedTab by remember { mutableStateOf(GardenTab.GARDEN) }

  val isOnline by OfflineStateManager.isOnline.collectAsState()

  // Garden UI state used to show the profile in the TopBar
  val gardenUiState by gardenViewModel.uiState.collectAsState()

  Scaffold(
      modifier = modifier.testTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN),
      topBar = {
        // One common top bar for both tabs
        TopBarGardenParent(
            gardenCallbacks = gardenCallbacks,
            gardenUiState = gardenUiState,
            isOnline = isOnline,
            isViewMode = isViewMode,
            onLikeClick = { gardenViewModel.toggleLike() },
            modifier = modifier)
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
        verticalArrangement = Arrangement.spacedBy(COLUMN_VERTICAL_PADDING)) {
          TableRowGardenParent(
              selectedTab = selectedTab, onTabClick = { selectedTab = it }, modifier = modifier)

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
              AchievementsScreen(modifier = modifier, viewModel = achievementsViewModel)
            }
          }
        }
  }
}

/**
 * Top app bar used by the [ParentTabScreenGarden]. Displays the [ProfileRow] with the user's
 * avatar, pseudo and sign-out button. Moreover, this composable shows a back navigation button when
 * the screen is in view mode.
 *
 * @param gardenCallbacks Callbacks for navigation and actions in the Garden flow (editing profile,
 *   signing out, navigating back, etc.).
 * @param gardenUiState UI state of the garden used for the the profile row.
 * @param isOnline Whether the device is currently online.
 * @param isViewMode If true, displays the back button instead of the sign out button.
 * @param modifier Optional modifier of the composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarGardenParent(
    gardenCallbacks: GardenScreenCallbacks,
    gardenUiState: GardenUIState,
    isOnline: Boolean,
    isViewMode: Boolean,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  CenterAlignedTopAppBar(
      title = {
        ProfileRow(
            gardenCallbacks.onEditProfile,
            gardenCallbacks.onSignOut,
            modifier,
            gardenUiState,
            isOnline,
            isViewMode,
            onLikeClick)
      },
      navigationIcon = {
        if (isViewMode) {
          NavigationButton(gardenCallbacks.onBackPressed)
        }
      })
}

/**
 * Tab row used by [ParentTabScreenGarden] to switch between the Garden/Achievements content. Render
 * a smooth rounded-shaped TableRow with one tab per [GardenTab] entry.
 *
 * Each tab is tagged by the corresponding test tag (getTestTagForTab) for testing.
 *
 * @param selectedTab The currently selected [GardenTab].
 * @param onTabClick Callback called when the user selects a tab.
 * @param modifier Optional modifier of the composable.
 */
@Composable
fun TableRowGardenParent(
    selectedTab: GardenTab,
    onTabClick: (GardenTab) -> Unit,
    modifier: Modifier = Modifier,
) {
  Surface(
      modifier =
          modifier
              .padding(horizontal = TAB_EXTERNAL_HORIZONTAL_PADDING)
              .clip(RoundedCornerShape(TAB_SHAPE_PERCENTAGE)),
      color = MaterialTheme.colorScheme.background,
      tonalElevation = TAB_TONAL_ELEVATION) {
        Row(
            modifier = modifier.height(TAB_HEIGHT).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {
              GardenTab.entries.forEachIndexed { index, tab ->
                val selected = index == selectedTab.ordinal

                Box(
                    modifier =
                        modifier
                            .weight(FULL_WEIGHT)
                            .fillMaxHeight()
                            .padding(INSIDE_TAB_PADDING)
                            .clip(RoundedCornerShape(TAB_SHAPE_PERCENTAGE))
                            .clickable { onTabClick(tab) }
                            .background(
                                if (selected) MaterialTheme.colorScheme.surface
                                else Color.Transparent)
                            .padding(horizontal = TAB_INTERNAL_HORIZONTAL_PADDING)
                            .testTag(GardenAchievementsParentScreenTestTags.getTestTagForTab(tab)),
                    contentAlignment = Alignment.Center) {
                      Text(
                          text = stringResource(tab.titleRes),
                          style = MaterialTheme.typography.bodyLarge,
                          fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
              }
            }
      }
}

/**
 * The profile row with the user profile picture, its username and a button to sign out.
 *
 * @param onEditProfile the callback called when the edit button is clicked on
 * @param onSignOut the callback called when pressed on the log out button
 * @param modifier the modifier for the row
 * @param uiState the UI state
 * @param isOnline whether the device is online
 * @param isViewMode if true, hide the log out button (for viewing a friend's garden)
 * @param onLikeClick if clicked toggle the like button
 */
@Composable
fun ProfileRow(
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: GardenUIState,
    isOnline: Boolean,
    isViewMode: Boolean = false,
    onLikeClick: () -> Unit
) {
  val context = LocalContext.current

  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(
                  horizontal = PROFILE_ROW_HORIZONTAL_PADDING,
                  vertical = PROFILE_ROW_VERTICAL_PADDING),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          // Sign out button (hidden in view mode)
          if (!isViewMode) {
            NavigationButton(onClick = onSignOut, isSignOut = true)
          } else {
            Spacer(modifier = modifier.weight(FULL_WEIGHT))
          }
          // Username (user can click on it to edit profile)
          Text(
              modifier =
                  modifier
                      .testTag(GardenAchievementsParentScreenTestTags.PSEUDO)
                      .clickable(
                          onClick = {
                            handleOfflineClick(
                                isOnline = isOnline,
                                context = context,
                                offlineMessageResId = OfflineMessages.CANNOT_EDIT_PROFILE) {
                                  onEditProfile()
                                }
                          }),
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleLarge,
              text = uiState.userName)

          Spacer(modifier = modifier.weight(FULL_WEIGHT))

          // Like button
          val likeEnabled = isViewMode && isOnline && !uiState.isLikeUpdating

          LikeIndicator(
              uiState.likesCount,
              hasLiked = uiState.hasLiked,
              enabled = likeEnabled,
              onLikeClick = onLikeClick)

          Spacer(modifier = modifier.weight(FULL_WEIGHT))

          // User avatar (user can click on it to edit profile)
          Card(
              modifier =
                  modifier
                      .clip(CircleShape)
                      .size(AVATAR_SIZE)
                      .testTag(GardenAchievementsParentScreenTestTags.AVATAR_EDIT_PROFILE)
                      .clickable(
                          onClick = {
                            handleOfflineClick(
                                isOnline = isOnline,
                                context = context,
                                offlineMessageResId = OfflineMessages.CANNOT_EDIT_PROFILE) {
                                  onEditProfile()
                                }
                          })) {
                Image(
                    painter = painterResource(uiState.userAvatar.resId),
                    contentDescription =
                        context.getString(R.string.avatar_description, uiState.userAvatar.name),
                    modifier = modifier.fillMaxSize())
              }
        }
      }
}
/** The composable used for the like display */
@Composable
fun LikeIndicator(
    likesCount: Int,
    hasLiked: Boolean,
    enabled: Boolean,
    onLikeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
          modifier
              .testTag(GardenAchievementsParentScreenTestTags.LIKE_BUTTON)
              .then(if (enabled) Modifier.clickable { onLikeClick() } else Modifier)) {
        Icon(
            imageVector = if (hasLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (hasLiked) "Liked" else "Not liked",
            tint =
                if (hasLiked) MaterialTheme.colorScheme.error
                else if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp))

        Spacer(modifier = Modifier.size(6.dp))

        Text(
            text = likesCount.toString(),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(GardenAchievementsParentScreenTestTags.LIKE_COUNTER))
      }
}
