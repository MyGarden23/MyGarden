package com.android.mygarden.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAchievement
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddFriend
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityWaterPlant
import com.android.mygarden.model.gardenactivity.activityclasses.GardenActivity
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.TopBar

/*----------------- PADDING / SIZE / OPACITY CONSTANTS ------------------*/
private val BUTTON_ROW_HORIZONTAL_PADDING = 24.dp
private val BETWEEN_BUTTON_AND_ACTIVITIES_SPACER_PADDING = 8.dp
private val LAZY_COLUMN_HORIZONTAL_PADDING = 8.dp
private val VERTICAL_SPACE_BETWEEN_ACTIVITIES_PADDING = 8.dp
private val CARD_PADDING = 12.dp
private val CARD_ELEVATION_PADDING = 6.dp
private val ROUND_CORNER = 6.dp
private val IN_CARD_ROW_PADDING = 6.dp
private val IN_CARD_ACTIVITY_TITLE_SIZE = 18.sp
private val IN_CARD_ACTIVITY_TITLE_OPACITY = 0.5f
private val IN_CARD_ACTIVITY_ICON_OPACITY = 0.5f
private val IN_CARD_TEXT_WEIGHT = 1f
private val IN_CARD_TEXT_HORIZONTAL_PADDING = 8.dp
private val NO_ACTIVITY_MSG_PADDING = 40.dp

/*---------------- TEST TAGS FOR ALL COMPONENTS OF THE SCREEN --------------*/
object FeedScreenTestTags {
  const val ADD_FRIEND_BUTTON = "AddFriendButton"
  const val NO_ACTIVITY_MESSAGE = "NoActivityMessage"
  const val ADDED_PLANT_DESCRIPTION = "AddedPlantDescription"
  const val ADDED_FRIEND_DESCRIPTION = "AddedFriendDescription"
  const val WATERED_PLANT_DESCRIPTION = "WateredPlantDescription"
  const val GOT_ACHIEVEMENT_DESCRIPTION = "GotAchievementDescription"

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
    feedViewModel: FeedViewModel = viewModel(),
    onAddFriend: () -> Unit = {}
) {

  val uiState by feedViewModel.uiState.collectAsState()
  val activities = uiState.activities

  // Ensures that we start collecting from the repository's list of activities
  LaunchedEffect(Unit) { feedViewModel.refreshUIState() }

  Scaffold(
      modifier = modifier.testTag(NavigationTestTags.FEED_SCREEN),
      topBar = { TopBar(title = stringResource(R.string.feed_screen_title)) },
      containerColor = MaterialTheme.colorScheme.background,
      content = { pd ->
        Column(modifier = modifier.fillMaxWidth().padding(pd)) {
          // Row for the button - we want it below the top bar but above the activities
          Row(
              modifier =
                  modifier.fillMaxWidth().padding(horizontal = BUTTON_ROW_HORIZONTAL_PADDING),
              horizontalArrangement = Arrangement.End) {
                AddFriendButton(modifier, onAddFriend)
              }
          // space between button and activities
          Spacer(modifier = modifier.height(BETWEEN_BUTTON_AND_ACTIVITIES_SPACER_PADDING))
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
                    Arrangement.spacedBy(VERTICAL_SPACE_BETWEEN_ACTIVITIES_PADDING)) {
                  items(activities.size) { index ->
                    ActivityItem(modifier = modifier, activity = activities[index])
                  }
                }
          }
        }
      })
}

/**
 * The button to add a friend
 *
 * @param modifier the used modifier for the composable
 * @param onClick the callback to be triggered when the user clicks on the button
 */
@Composable
fun AddFriendButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
  Button(modifier = modifier.testTag(FeedScreenTestTags.ADD_FRIEND_BUTTON), onClick = onClick) {
    Text(stringResource(R.string.add_friend_button))
  }
}

/**
 * Generic composable for any given GardenActivity, that creates the standard card to display
 *
 * @param modifier the used modifier for the composable and its potential users
 * @param activity the generic activity
 */
@Composable
fun ActivityItem(modifier: Modifier = Modifier, activity: GardenActivity) {
  val colorPalette = activityTypeColor(activity, MaterialTheme.colorScheme)
  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(CARD_PADDING)
              .testTag(FeedScreenTestTags.getTestTagForActivity(activity)),
      colors = CardDefaults.cardColors(containerColor = colorPalette.backgroundColor),
      elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION_PADDING),
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
  Row(
      modifier = modifier.fillMaxSize().padding(IN_CARD_ROW_PADDING),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Text(
                  fontSize = IN_CARD_ACTIVITY_TITLE_SIZE,
                  fontWeight = FontWeight.ExtraBold,
                  color = colorPalette.textColor.copy(alpha = IN_CARD_ACTIVITY_TITLE_OPACITY),
                  text = stringResource(R.string.in_card_added_activity_title))
              Icon(
                  painter = painterResource(R.drawable.potted_plant_icon),
                  contentDescription = null,
                  tint = colorPalette.textColor.copy(IN_CARD_ACTIVITY_ICON_OPACITY))
            }
        Text(
            modifier =
                modifier
                    .weight(IN_CARD_TEXT_WEIGHT)
                    .padding(horizontal = IN_CARD_TEXT_HORIZONTAL_PADDING)
                    .testTag(FeedScreenTestTags.ADDED_PLANT_DESCRIPTION),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            text =
                stringResource(
                    R.string.added_plant_activity, activity.pseudo, activity.ownedPlant.plant.name))
      }
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
  Text(
      modifier = modifier.testTag(FeedScreenTestTags.ADDED_FRIEND_DESCRIPTION),
      color = colorPalette.textColor,
      text = stringResource(R.string.added_friend_activity, activity.type))
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
  Text(
      modifier = modifier.testTag(FeedScreenTestTags.GOT_ACHIEVEMENT_DESCRIPTION),
      color = colorPalette.textColor,
      text = stringResource(R.string.got_achievement_activity, activity.type))
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
  Text(
      modifier = modifier.testTag(FeedScreenTestTags.WATERED_PLANT_DESCRIPTION),
      color = colorPalette.textColor,
      text = stringResource(R.string.watered_plant_activity, activity.type))
}

/**
 * Function that maps the correct color palette to each activity type
 *
 * @param activity the activity to match the correct color palette
 * @param colorScheme the colorscheme used in the app for specific/consistent colors
 */
fun activityTypeColor(activity: GardenActivity, colorScheme: ColorScheme): CardColorPalette {
  return when (activity) {
    is ActivityAchievement ->
        CardColorPalette(colorScheme.errorContainer, colorScheme.onErrorContainer)
    is ActivityAddFriend ->
        CardColorPalette(colorScheme.errorContainer, colorScheme.onErrorContainer)
    is ActivityAddedPlant ->
        CardColorPalette(colorScheme.primaryContainer, colorScheme.onPrimaryContainer)
    is ActivityWaterPlant ->
        CardColorPalette(colorScheme.errorContainer, colorScheme.onErrorContainer)
  }
}
