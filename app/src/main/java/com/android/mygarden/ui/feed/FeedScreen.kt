package com.android.mygarden.ui.feed

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.android.mygarden.R
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAchievement
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddFriend
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityWaterPlant
import com.android.mygarden.model.gardenactivity.activityclasses.GardenActivity
import com.android.mygarden.model.offline.OfflineStateManager
import com.android.mygarden.ui.navigation.NavigationActions
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.TopBar
import com.android.mygarden.ui.theme.CustomColors
import com.android.mygarden.ui.theme.ExtendedTheme
import com.android.mygarden.ui.utils.OfflineMessages
import com.android.mygarden.ui.utils.handleOfflineClick

/*----------------- PADDING / SIZE / OPACITY CONSTANTS ------------------*/
private val BUTTON_ROW_HORIZONTAL_PADDING = 24.dp
private val BETWEEN_BUTTON_AND_ACTIVITIES_SPACER_PADDING = 20.dp
private val LAZY_COLUMN_HORIZONTAL_PADDING = 8.dp
private val VERTICAL_SPACE_BETWEEN_ACTIVITIES_PADDING = 16.dp
private val CARD_PADDING = 12.dp
private val CARD_ELEVATION = 6.dp
private val ROUND_CORNER = 6.dp
private val IN_CARD_ROW_PADDING = 6.dp
private const val IN_CARD_ACTIVITY_ICON_OPACITY = 0.5f
private const val WEIGHT_1 = 1f
private val IN_CARD_TEXT_HORIZONTAL_PADDING = 8.dp
private val NO_ACTIVITY_MSG_PADDING = 40.dp
private val BORDER_CARD_WIDTH = 3.dp
private val ICON_PADDING = 5.dp
private val ICON_SIZE = 69.dp
private val NOTIF_ICON_SIZE = 50.dp
private val BADGE_ICON_SIZE = 16.dp
private val BADGE_ICON_X = (-17).dp
private val BADGE_ICON_Y = 7.dp
private val BADGE_BORDER_WIDTH = 1.dp

/*---------------- TEST TAGS FOR ALL COMPONENTS OF THE SCREEN --------------*/
object FeedScreenTestTags {
  const val ADD_FRIEND_BUTTON = "AddFriendButton"
  const val FRIENDS_REQUESTS_BUTTON = "FriendsRequestsButton"
  const val FRIEND_LIST_BUTTON = "FriendListButton"
  const val NO_ACTIVITY_MESSAGE = "NoActivityMessage"
  const val GENERIC_CARD_DESCRIPTION = "GenericCardDescription"
  const val GENERIC_CARD_ICON = "GenericCardIcon"

  /** get unique test tag for a specific activity */
  fun getTestTagForActivity(activity: GardenActivity) = "Activity${activity.createdAt}"
}

/* used to choose the correct background and text colors for a card depending on the activity type*/
data class CardColorPalette(val backgroundColor: Color, val textColor: Color)

/**
 * The feed screen where a user can see all of his friends activities (alongside with his), and
 * finds a button where he can add new friends
 *
 * @param modifier the given modifier for this screen (new one by default)
 * @param feedViewModel the view model used to update the list of activities to be displayed
 * @param onAddFriend the callback to be triggered when the user presses on the add friend button
 */
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    navigationActions: NavigationActions? = null,
    navController: NavHostController? = null,
    feedViewModel: FeedViewModel = viewModel(factory = FeedViewModelFactory(navigationActions)),
    onAddFriend: () -> Unit = {},
    onNotifClick: () -> Unit = {},
    onFriendList: () -> Unit = {}
) {

  val uiState by feedViewModel.uiState.collectAsState()
  val activities = uiState.activities

  // Collect offline state
  val isOnline by OfflineStateManager.isOnline.collectAsState()

  val context = LocalContext.current

  // Ensures that we start collecting from the repository's list of activities
  LaunchedEffect(Unit) { feedViewModel.refreshUIState() }

  if (uiState.isWatchingFriendsActivity) {
    FriendActivityPopup(
        onDismiss = { feedViewModel.setIsWatchingFriendsActivity(false) },
        feedViewModel = feedViewModel)
  }

  Scaffold(
      modifier = modifier.testTag(NavigationTestTags.FEED_SCREEN),
      topBar = { TopBar(title = stringResource(R.string.feed_screen_title)) },
      containerColor = MaterialTheme.colorScheme.background,
      floatingActionButton = {
        AddFriendButton(
            modifier = Modifier,
            onClick = {
              handleOfflineClick(isOnline, context, OfflineMessages.CANNOT_ADD_FRIENDS, onAddFriend)
            },
            isOnline = isOnline)
      },
      content = { pd ->
        Column(modifier = modifier.fillMaxSize().padding(pd)) {
          // Buttons - we want them below the top bar but above the activities
          Row(
              modifier =
                  modifier.fillMaxWidth().padding(horizontal = BUTTON_ROW_HORIZONTAL_PADDING),
              horizontalArrangement = Arrangement.SpaceBetween) {
                NotificationButton(modifier, onClick = onNotifClick, uiState.hasRequests)
                FriendListButton(modifier, onFriendList)
              }
          // space between buttons and activities
          Spacer(modifier = modifier.height(BETWEEN_BUTTON_AND_ACTIVITIES_SPACER_PADDING))

          Box(modifier = Modifier.weight(WEIGHT_1).fillMaxWidth()) {
            if (activities.isEmpty()) {
              // display a specific message if there are no activity to display
              Box(
                  modifier =
                      modifier
                          .fillMaxSize()
                          .padding(NO_ACTIVITY_MSG_PADDING)
                          .testTag(FeedScreenTestTags.NO_ACTIVITY_MESSAGE),
                  contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.no_activity_message))
                  }
            } else {
              LazyColumn(
                  modifier =
                      modifier.fillMaxWidth().padding(horizontal = LAZY_COLUMN_HORIZONTAL_PADDING),
                  verticalArrangement =
                      Arrangement.spacedBy(VERTICAL_SPACE_BETWEEN_ACTIVITIES_PADDING),
                  contentPadding = PaddingValues(VERTICAL_SPACE_BETWEEN_ACTIVITIES_PADDING)) {
                    items(activities.size) { index ->
                      ActivityItem(
                          modifier = modifier,
                          activity = activities[index],
                          navigationActions = navigationActions,
                          navController = navController,
                          feedViewModel = feedViewModel)
                    }
                  }
            }
          }
        }
      })
}
/**
 * The notification button to go to the FriendsRequestsScreen.
 *
 * The icon has a red button or not
 *
 * @param modifier the used modifier for the composable
 * @param onClick the callback to be triggered when the user clicks on the button
 * @param hasRequests the boolean used to know which icon to display
 */
@Composable
fun NotificationButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    hasRequests: Boolean
) {
  BadgedBox(
      badge = {
        if (hasRequests) {
          Badge(
              containerColor = ExtendedTheme.colors.notificationRed,
              modifier =
                  Modifier.offset(x = BADGE_ICON_X, y = BADGE_ICON_Y)
                      .size(BADGE_ICON_SIZE)
                      .border(
                          width = BADGE_BORDER_WIDTH,
                          color = MaterialTheme.colorScheme.onPrimaryContainer,
                          shape = CircleShape),
          )
        }
      },
  ) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier.size(NOTIF_ICON_SIZE).testTag(FeedScreenTestTags.FRIENDS_REQUESTS_BUTTON)) {
          Icon(
              painter = painterResource(R.drawable.notif),
              contentDescription =
                  stringResource(R.string.icon_of_the_notification_button_description),
              tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
  }
}

/**
 * The button to add a friend
 *
 * @param modifier the used modifier for the composable
 * @param onClick the callback to be triggered when the user clicks on the button
 */
@Composable
fun AddFriendButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}, isOnline: Boolean) {
  ExtendedFloatingActionButton(
      modifier = modifier.testTag(FeedScreenTestTags.ADD_FRIEND_BUTTON),
      onClick = onClick,
      containerColor =
          if (isOnline) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceVariant,
      contentColor =
          if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer
          else MaterialTheme.colorScheme.onSurfaceVariant) {
        Text(stringResource(R.string.add_friend_button))
      }
}

/**
 * The button to see your friend list
 *
 * @param modifier the used modifier for the composable
 * @param onClick the callback to be triggered when the user clicks on the button
 */
@Composable
fun FriendListButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
  Button(
      modifier = modifier.testTag(FeedScreenTestTags.FRIEND_LIST_BUTTON),
      onClick = onClick,
      colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primaryContainer)) {
        Text(
            stringResource(R.string.friend_list_button),
            color = MaterialTheme.colorScheme.onPrimaryContainer)
      }
}

/**
 * Generic composable for any given GardenActivity, that creates the standard card to display
 *
 * @param modifier the used modifier for the composable and its potential users
 * @param activity the generic activity
 */
@Composable
fun ActivityItem(
    modifier: Modifier = Modifier,
    activity: GardenActivity,
    navigationActions: NavigationActions?,
    navController: NavHostController?,
    feedViewModel: FeedViewModel
) {
  val context = LocalContext.current
  val isOnline by OfflineStateManager.isOnline.collectAsState()

  val colorPalette = activityTypeColor(activity, MaterialTheme.colorScheme, ExtendedTheme.colors)

  Card(
      modifier =
          modifier
              .testTag(FeedScreenTestTags.getTestTagForActivity(activity))
              .fillMaxWidth()
              .padding(horizontal = CARD_PADDING)
              .clickable {
                handleOfflineClick(
                    isOnline,
                    context,
                    OfflineMessages.CANNOT_CLICK_ACTIVITY,
                    {
                      feedViewModel.handleActivityClick(activity, navigationActions, navController)
                    })
              },
      colors = CardDefaults.cardColors(containerColor = colorPalette.backgroundColor),
      elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION),
      shape = RoundedCornerShape(ROUND_CORNER),
      content = {
        when (activity) {
          is ActivityAchievement -> GotAnAchievementCard(modifier, activity, colorPalette)
          is ActivityAddFriend -> AddedAFriendCard(modifier, activity, colorPalette)
          is ActivityAddedPlant -> AddedAPlantCard(modifier, activity, colorPalette)
          is ActivityWaterPlant -> WateredAPlantCard(modifier, activity, colorPalette)
        }
      })
}

/**
 * Displays a generic activity card used in the feed.
 *
 * This function is used by all activity-specific card composables.
 *
 * @param colorPalette the color palette defining the background and icon/text tint associated with
 *   the specific activity type.
 * @param modifier the used modifier for the composable
 * @param icon drawable resource ID for the icon displayed on the left side.
 * @param cardText the description text of the activity.
 */
@Composable
fun GenericCard(
    colorPalette: CardColorPalette,
    modifier: Modifier = Modifier,
    icon: Int,
    cardText: String,
) {
  Row(
      modifier =
          modifier
              .fillMaxWidth()
              .border(
                  width = BORDER_CARD_WIDTH,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  shape = RoundedCornerShape(ROUND_CORNER))
              .padding(IN_CARD_ROW_PADDING),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = modifier.padding(ICON_PADDING)) {
          Icon(
              painter = painterResource(icon),
              contentDescription = stringResource(R.string.icon_of_the_activity_card_description),
              tint = colorPalette.textColor.copy(IN_CARD_ACTIVITY_ICON_OPACITY),
              modifier = modifier.size(ICON_SIZE).testTag(FeedScreenTestTags.GENERIC_CARD_ICON))
        }
        Text(
            modifier =
                modifier
                    .weight(WEIGHT_1)
                    .padding(horizontal = IN_CARD_TEXT_HORIZONTAL_PADDING)
                    .testTag(FeedScreenTestTags.GENERIC_CARD_DESCRIPTION),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            text = cardText,
            fontWeight = FontWeight.Bold)
      }
}

/**
 * The content of the generic card for a [added plant] activity
 *
 * @param modifier the used modifier for the composable
 * @param activity the specific activity
 * @param colorPalette the palette of colors for this specific activity
 */
@Composable
fun AddedAPlantCard(
    modifier: Modifier = Modifier,
    activity: ActivityAddedPlant,
    colorPalette: CardColorPalette
) {
  val icon = R.drawable.potted_plant_icon
  val cardText =
      stringResource(R.string.added_plant_activity, activity.pseudo, activity.ownedPlant.plant.name)

  GenericCard(colorPalette, modifier, icon, cardText)
}

/**
 * The content of the generic card for a [added a friend] activity
 *
 * @param modifier the used modifier for the composable
 * @param activity the specific activity
 * @param colorPalette the palette of colors for this specific activity
 */
@Composable
fun AddedAFriendCard(
    modifier: Modifier = Modifier,
    activity: ActivityAddFriend,
    colorPalette: CardColorPalette
) {
  val icon = R.drawable.friends_icon
  val cardText =
      stringResource(R.string.added_friend_activity, activity.pseudo, activity.friendPseudo)

  GenericCard(colorPalette, modifier, icon, cardText)
}

/**
 * The content of the generic card for a [got an achievement] activity
 *
 * @param modifier the used modifier for the composable
 * @param activity the specific activity
 * @param colorPalette the palette of colors for this specific activity
 */
@Composable
fun GotAnAchievementCard(
    modifier: Modifier = Modifier,
    activity: ActivityAchievement,
    colorPalette: CardColorPalette
) {
  val icon = R.drawable.achievement
  val cardText =
      stringResource(
          R.string.got_achievement_activity,
          activity.pseudo,
          activity.levelReached,
          activity.achievementType.toString())

  GenericCard(colorPalette, modifier, icon, cardText)
}

/**
 * The content of the generic card for a [watered a plant] activity
 *
 * @param modifier the used modifier for the composable
 * @param activity the specific activity
 * @param colorPalette the palette of colors for this specific activity
 */
@Composable
fun WateredAPlantCard(
    modifier: Modifier = Modifier,
    activity: ActivityWaterPlant,
    colorPalette: CardColorPalette
) {
  val icon = R.drawable.watering_can
  val cardText =
      stringResource(
          R.string.watered_plant_activity, activity.pseudo, activity.ownedPlant.plant.name)

  GenericCard(colorPalette, modifier, icon, cardText)
}

/**
 * Function that maps the correct color palette to each activity type
 *
 * @param activity the activity to match the correct color palette
 * @param colorScheme the colorscheme used in the app for specific/consistent colors
 */
fun activityTypeColor(
    activity: GardenActivity,
    colorScheme: ColorScheme,
    customColors: CustomColors
): CardColorPalette {
  return when (activity) {
    is ActivityAchievement ->
        CardColorPalette(customColors.achievementGrey, customColors.onAchievementGrey)
    is ActivityAddFriend ->
        CardColorPalette(customColors.friendActivityRed, customColors.onFriendActivityRed)
    is ActivityAddedPlant ->
        CardColorPalette(customColors.addPlantActivityGreen, colorScheme.onPrimaryContainer)
    is ActivityWaterPlant ->
        CardColorPalette(customColors.waterActivityBlue, customColors.onWaterActivityBlue)
  }
}
