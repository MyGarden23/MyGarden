package com.android.mygarden.ui.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.android.mygarden.ui.theme.CustomColors
import com.android.mygarden.ui.theme.ExtendedTheme

/*----------------- PADDING / SIZE / OPACITY CONSTANTS ------------------*/
private val BUTTON_ROW_HORIZONTAL_PADDING = 24.dp
private val BETWEEN_BUTTON_AND_ACTIVITIES_SPACER_PADDING = 8.dp
private val LAZY_COLUMN_HORIZONTAL_PADDING = 8.dp
private val VERTICAL_SPACE_BETWEEN_ACTIVITIES_PADDING = 16.dp
private val CARD_PADDING = 12.dp
private val CARD_ELEVATION_PADDING = 6.dp
private val ROUND_CORNER = 6.dp
private val IN_CARD_ROW_PADDING = 6.dp
private val IN_CARD_ACTIVITY_TITLE_SIZE = 18.sp
private val IN_CARD_ACTIVITY_TITLE_OPACITY = 0.5f
private val IN_CARD_ACTIVITY_ICON_OPACITY = 0.5f
private val WEIGHT_1 = 1f
private val IN_CARD_TEXT_HORIZONTAL_PADDING = 8.dp
private val NO_ACTIVITY_MSG_PADDING = 40.dp

/*---------------- TEST TAGS FOR ALL COMPONENTS OF THE SCREEN --------------*/
object FeedScreenTestTags {
  const val ADD_FRIEND_BUTTON = "AddFriendButton"
  const val FRIEND_LIST_BUTTON = "FriendListButton"
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
    onAddFriend: () -> Unit = {},
    onNotifClick: () -> Unit = {},
    onFriendList: () -> Unit = {}
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
        Column(modifier = modifier.fillMaxSize().padding(pd)) {
          // Row for the button - we want it below the top bar but above the activities
          Row(
              modifier =
                  modifier.fillMaxWidth().padding(horizontal = BUTTON_ROW_HORIZONTAL_PADDING),
              horizontalArrangement = Arrangement.SpaceBetween) {
                NotificationButton(onClick = onNotifClick)
                FriendListButton(modifier, onFriendList)
              }
          // space between button and activities
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
                      Arrangement.spacedBy(VERTICAL_SPACE_BETWEEN_ACTIVITIES_PADDING)) {
                    items(activities.size) { index ->
                      ActivityItem(modifier = modifier, activity = activities[index])
                    }
                  }
            }
          }
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(
                          end = BUTTON_ROW_HORIZONTAL_PADDING,
                          bottom = BUTTON_ROW_HORIZONTAL_PADDING),
              horizontalArrangement = Arrangement.End) {
                AddFriendButton(modifier = Modifier, onClick = onAddFriend)
              }
        }
      })
}
/**
 * The notification button to go to the RequestsScreen
 *
 * @param onClick the callback to be triggered when the user clicks on the button
 */
@Composable
fun NotificationButton(onClick: () -> Unit = {}) {
  IconButton(
      onClick = onClick,
  ) {
    Image(
        painter = painterResource(R.drawable.notif),
        contentDescription = "",
    )
  }
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
 * The button to see your friend list
 *
 * @param modifier the used modifier for the composable
 * @param onClick the callback to be triggered when the user clicks on the button
 */
@Composable
fun FriendListButton(modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
  Button(modifier = modifier.testTag(FeedScreenTestTags.FRIEND_LIST_BUTTON), onClick = onClick) {
    Text(stringResource(R.string.friend_list_button))
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
  val colorPalette =
      activityTypeColor(activity, MaterialTheme.colorScheme, customColors = ExtendedTheme.colors)
  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = CARD_PADDING)
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
  val titleText = stringResource(R.string.in_card_added_activity_title)
  val icon = R.drawable.potted_plant_icon
  val cardText =
      stringResource(R.string.added_plant_activity, activity.pseudo, activity.ownedPlant.plant.name)

  GenericCard(colorPalette, modifier, titleText, icon, cardText)
}

@Composable
fun GenericCard(
    colorPalette: CardColorPalette,
    modifier: Modifier = Modifier,
    titleText: String,
    icon: Int,
    cardText: String,
) { // TODO Add const for borders
  Row(
      modifier =
          modifier
              .fillMaxSize()
              .border(
                  width = 3.dp, color = Color(0xFF4A4A4A), shape = RoundedCornerShape(ROUND_CORNER))
              .padding(IN_CARD_ROW_PADDING),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = modifier.padding(top = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom) {
              Icon(
                  painter = painterResource(icon),
                  contentDescription =
                      stringResource(R.string.icon_of_the_activity_card_description),
                  tint = colorPalette.textColor.copy(IN_CARD_ACTIVITY_ICON_OPACITY),
                  modifier = modifier.size(50.dp))
              //                Text(
              //                    fontSize = IN_CARD_ACTIVITY_TITLE_SIZE,
              //                    fontWeight = FontWeight.ExtraBold,
              //                    color = colorPalette.textColor.copy(alpha =
              // IN_CARD_ACTIVITY_TITLE_OPACITY),
              //                    text = titleText)
            }
        Text(
            modifier =
                modifier
                    .weight(WEIGHT_1)
                    .padding(horizontal = IN_CARD_TEXT_HORIZONTAL_PADDING)
                    .testTag(FeedScreenTestTags.ADDED_PLANT_DESCRIPTION),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            text = cardText)
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
  val titleText = stringResource(R.string.in_card_friends_activity_title)
  val icon = R.drawable.friends_icon
  val cardText =
      stringResource(R.string.added_friend_activity, activity.pseudo, activity.friendPseudo)

  GenericCard(colorPalette, modifier, titleText, icon, cardText)
  //    Icon(
  //        painter = painterResource(R.drawable.friends_icon),
  //        contentDescription = null,
  //        tint = colorPalette.textColor.copy(IN_CARD_ACTIVITY_ICON_OPACITY))
  //      Text(
  //          modifier = modifier.testTag(FeedScreenTestTags.ADDED_FRIEND_DESCRIPTION),
  //          //color = colorPalette.textColor,
  //          text = stringResource(R.string.added_friend_activity, "élkj", "asdf"))
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
  val titleText = stringResource(R.string.in_card_achievement_activity_title)
  val icon = R.drawable.achievement
  val cardText =
      stringResource(R.string.got_achievement_activity, activity.pseudo, activity.achievementName)

  GenericCard(colorPalette, modifier, titleText, icon, cardText)
  //  Text(
  //      modifier = modifier.testTag(FeedScreenTestTags.GOT_ACHIEVEMENT_DESCRIPTION),
  //      color = colorPalette.textColor,
  //      text = stringResource(R.string.got_achievement_activity, activity.type))
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
  val titleText = stringResource(R.string.in_card_watered_activity_title)
  val icon = R.drawable.watering_can
  val cardText =
      stringResource(
          R.string.watered_plant_activity, activity.pseudo, activity.ownedPlant.plant.name)

  GenericCard(colorPalette, modifier, titleText, icon, cardText)
  //  Text(
  //      modifier = modifier.testTag(FeedScreenTestTags.WATERED_PLANT_DESCRIPTION),
  //      //color = colorPalette.textColor,
  //      text = stringResource(R.string.watered_plant_activity, activity.type))
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
        CardColorPalette(colorScheme.primaryContainer, colorScheme.onPrimaryContainer)
    is ActivityWaterPlant ->
        CardColorPalette(customColors.waterActivityBlue, customColors.onWaterActivityBlue)
  }
}
