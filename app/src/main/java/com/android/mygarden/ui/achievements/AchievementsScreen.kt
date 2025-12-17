package com.android.mygarden.ui.achievements

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R
import com.android.mygarden.model.achievements.ACHIEVEMENTS_LEVEL_NUMBER
import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.ui.navigation.NavigationTestTags

/** Object used for compose UI testing */
object AchievementsScreenTestTags {
  fun getTestTagForAchievementCard(achievementType: AchievementType) =
      "AchievementCard_${achievementType.name}"

  fun getTestTagForCardLevel(achievementType: AchievementType) =
      "CurrentLevel_${achievementType.name}"

  fun getTestTagForPopup(achievementType: AchievementType) =
      "AchievementPopup_${achievementType.name}"

  fun getTestTagClosingButton(achievementType: AchievementType) =
      "ClosingDialogButton_${achievementType.name}"

  fun getTestTagForRemainingUnits(achievementType: AchievementType) =
      "RemainingUnits_${achievementType.name}"
}

// Padding constants
private val COLUMN_VERTICAL_PADDING = 20.dp
private val ACHIEVEMENT_CARD_HORIZONTAL_PADDING = 20.dp
private val INNER_CARD_PADDING = 14.dp
private val INNER_CARD_COLUMN_VERTICAL_PADDING = 8.dp
private val ACHIEVEMENT_NAME_TOP_PADDING = 18.dp
private val INNER_DIALOG_PADDING = 30.dp
private val INNER_DIALOG_COLUMN_VERTICAL_PADDING = 10.dp
private val REMAINING_UNITS_RIGHT_PADDING = 10.dp

// Sizes
private val ACHIEVEMENT_CARD_HEIGHT = 150.dp
private val ACHIEVEMENT_CARD_ELEVATION = 8.dp
private val DIALOG_WIDTH = 300.dp
private val DIALOG_ROUNDED_SHAPE = 16.dp
private val CLOSE_BUTTON_SIZE = 30.dp
private const val Z_INDEX_DIALOG_REMAINING_UNITS = 1f
private val PROGRESS_BAR_HEIGHT = 22.dp
private const val PROGRESS_BAR_ROUNDED_SHAPE = 50
private val ACHIEVEMENT_IMAGE_SIZE = 130.dp
private val EMPTY_SPACER_HEIGHT = 0.dp

// Font sizes
private val ACHIEVEMENT_CARD_NAME_FONT_SIZE = 23.sp
private val DIALOG_NAME_FONT_SIZE = 25.sp
private val DIALOG_INFO_FONT_SIZE = 20.sp
private val DIALOG_REMAINING_FONT_SIZE = 14.sp

/**
 * Screen that displays a card for each [AchievementType] and the corresponding level of the user
 * for it. Additionally, it displays more information about the achievement (i.e. how to get more
 * levels in this achievement, what is the current level of the user, how much units of progress are
 * needed to reach next level, etc.) when users click on one of the card.
 *
 * This screen provides a user-friendly achievement-tracking feature.
 *
 * @param modifier Optional modifier of the composable.
 * @param friendId Either the id of the friend we want to see the achievements or null for the
 *   current user.
 * @param viewModel The ViewModel providing the UI state and useful functions.
 */
@Composable
fun AchievementsScreen(
    modifier: Modifier = Modifier,
    friendId: String? = null,
    viewModel: AchievementsViewModel =
        viewModel(factory = AchievementsViewModelFactory(friendId = friendId))
) {
  val uiState by viewModel.uiState.collectAsState()
  var showAchievementInfo by remember { mutableStateOf<AchievementType?>(null) }

  Column(
      modifier =
          modifier
              .verticalScroll(rememberScrollState())
              .testTag(NavigationTestTags.INTERNAL_ACHIEVEMENTS_SCREEN),
      verticalArrangement = Arrangement.spacedBy(COLUMN_VERTICAL_PADDING),
      horizontalAlignment = Alignment.CenterHorizontally) {
        for (type in AchievementType.entries) {
          AchievementCard(
              type = type,
              currentLevel = viewModel.getCorrespondingLevel(type, uiState),
              onClick = { showAchievementInfo = type })
          if (showAchievementInfo == type) {
            AchievementDialogInfo(
                modifier = modifier,
                type = type,
                currentLevel = viewModel.getCorrespondingLevel(type, uiState),
                currentValue = viewModel.getCorrespondingValue(type, uiState),
                neededMore = viewModel.getCorrespondingNeededForNextLevel(type, uiState),
                nextThreshold = viewModel.getCorrespondingNextThreshold(type, uiState),
                onDismiss = { showAchievementInfo = null },
            )
          }
        }
        // Empty spacer that adds an element in the column so it adds a COLUMN_VERTICAL_PADDING at
        // the end for cleaner UI
        Spacer(modifier.height(EMPTY_SPACER_HEIGHT))
      }
}

/**
 * Generic Card composable that can be used by each achievement type. This card displays the current
 * level of the user. Each [AchievementCard] should be used with the according
 * [AchievementDialogInfo] that should display more information about the achievement when the card
 * is being clicked on.
 *
 * @param modifier Optional modifier of the composable
 * @param onClick The callback that is being called when clicking the Card.
 * @param type The given [AchievementType] for which the card is being created.
 * @param currentLevel The current level of the user to be displayed (from the UI state).
 */
@Composable
fun AchievementCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    type: AchievementType,
    currentLevel: Int,
) {
  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .height(ACHIEVEMENT_CARD_HEIGHT)
              .padding(horizontal = ACHIEVEMENT_CARD_HORIZONTAL_PADDING)
              .clickable(onClick = onClick)
              .testTag(AchievementsScreenTestTags.getTestTagForAchievementCard(type)),
      elevation = CardDefaults.cardElevation(defaultElevation = ACHIEVEMENT_CARD_ELEVATION),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.padding(INNER_CARD_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(INNER_CARD_COLUMN_VERTICAL_PADDING)) {
              ImageForAchievementType(achievementType = type, modifier = modifier)

              Column(
                  modifier = Modifier.fillMaxHeight(),
                  verticalArrangement = Arrangement.SpaceAround,
                  horizontalAlignment = Alignment.Start) {
                    Text(
                        text = type.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = ACHIEVEMENT_CARD_NAME_FONT_SIZE,
                        modifier = modifier.padding(top = ACHIEVEMENT_NAME_TOP_PADDING))
                    ProgressBar(
                        modifier = Modifier,
                        currentValue = currentLevel,
                        maxValue = ACHIEVEMENTS_LEVEL_NUMBER,
                        color = MaterialTheme.colorScheme.primary)
                    Text(
                        text =
                            stringResource(
                                R.string.level_for_achievement,
                                currentLevel,
                                ACHIEVEMENTS_LEVEL_NUMBER),
                        fontWeight = FontWeight.Medium,
                        modifier =
                            modifier.testTag(
                                AchievementsScreenTestTags.getTestTagForCardLevel(type)))
                  }
            }
      }
}

/**
 * This Dialog brings more information about a certain [AchievementType]. It specifies what is the
 * goal of the achievement, what is the current level of the user and what unit of progress should
 * be done to reach the next level in this achievement. It should be used with a corresponding
 * [AchievementCard].
 *
 * @param modifier Optional modifier of the composable.
 * @param type The given [AchievementType] for which the dialog is being created.
 * @param currentLevel The current level in the given [AchievementType] (from the UI state).
 * @param currentValue The current raw value in the given [AchievementType] (from the UI state).
 * @param neededMore The number of units of progress needed to reach the next level.
 * @param nextThreshold The total number of units to have to reach the next level.
 * @param onDismiss The callback that is being called when the Dialog is being dismissed.
 */
@Composable
fun AchievementDialogInfo(
    modifier: Modifier = Modifier,
    type: AchievementType,
    currentLevel: Int,
    currentValue: Int,
    neededMore: Int,
    nextThreshold: Int,
    onDismiss: () -> Unit = {}
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
        modifier =
            modifier
                .width(DIALOG_WIDTH)
                .clip(RoundedCornerShape(DIALOG_ROUNDED_SHAPE))
                .testTag(AchievementsScreenTestTags.getTestTagForPopup(type))) {
          Column(
              modifier = modifier.padding(INNER_DIALOG_PADDING),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(INNER_DIALOG_COLUMN_VERTICAL_PADDING)) {
                Box(modifier = modifier.fillMaxWidth()) {
                  Icon(
                      painter = painterResource(R.drawable.x_circle),
                      contentDescription =
                          stringResource(R.string.popup_dismiss_button_content_description),
                      tint = MaterialTheme.colorScheme.tertiary,
                      modifier =
                          modifier
                              .size(CLOSE_BUTTON_SIZE)
                              .align(Alignment.TopStart)
                              .clickable(onClick = onDismiss)
                              .testTag(AchievementsScreenTestTags.getTestTagClosingButton(type)))
                  ImageForAchievementType(type, modifier = modifier.align(Alignment.Center))
                }

                Text(
                    text = type.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = DIALOG_NAME_FONT_SIZE)

                DescriptionForAchievementType(achievementType = type)

                Text(
                    text =
                        stringResource(
                            R.string.level_for_achievement_info,
                            currentLevel,
                            ACHIEVEMENTS_LEVEL_NUMBER),
                    fontWeight = FontWeight.Medium,
                    fontSize = DIALOG_INFO_FONT_SIZE)

                if (nextThreshold > 0) {
                  Box() {
                    ProgressBar(
                        modifier = modifier,
                        currentValue = currentValue,
                        maxValue = nextThreshold,
                        color = MaterialTheme.colorScheme.tertiary)
                    Text(
                        text =
                            stringResource(
                                R.string.left_for_threshold, currentValue, nextThreshold),
                        fontSize = DIALOG_REMAINING_FONT_SIZE,
                        modifier =
                            modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = REMAINING_UNITS_RIGHT_PADDING)
                                .zIndex(Z_INDEX_DIALOG_REMAINING_UNITS))
                  }
                }
                if (neededMore > 0) {
                  RemainingProgressForAchievementType(type, neededMore, currentLevel + 1, modifier)
                }
              }
        }
  }
}

/**
 * Composable that displays the progress of a user in the form of a progress bar. The bar is filled
 * with the ratio of the given current value divided by the maximum value and filled with the given
 * Color. If the [currentValue] is greater than the [maxValue] fills the bar and it the [maxValue]
 * is zero empty the bar.
 *
 * @param modifier Optional modifier of the composable.
 * @param currentValue The current value of progress.
 * @param maxValue The maximum value possible to have (corresponding to a 100% filled bar).
 * @param color The color used to fill the bar (the background color is the
 *   'MaterialTheme.colorScheme.background' color.
 */
@Composable
fun ProgressBar(modifier: Modifier = Modifier, currentValue: Int, maxValue: Int, color: Color) {
  // Eliminate error case with default values
  val width =
      when {
        currentValue > maxValue -> maxValue.toFloat()
        maxValue == 0 -> 0f
        else -> currentValue.toFloat() / maxValue
      }

  Box(
      modifier =
          modifier
              .height(PROGRESS_BAR_HEIGHT)
              .fillMaxWidth()
              .clip(RoundedCornerShape(PROGRESS_BAR_ROUNDED_SHAPE))
              .background(MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.BottomStart) {
        Box(
            modifier =
                modifier
                    .clip(RoundedCornerShape(PROGRESS_BAR_ROUNDED_SHAPE))
                    .fillMaxHeight()
                    .fillMaxWidth(width)
                    .background(color))
      }
}

/**
 * Helper function that retrieves the correct Image given the [AchievementType].
 *
 * @param achievementType The [AchievementType] for which the Image should be displayed.
 * @param modifier Optional modifier of the composable.
 */
@Composable
fun ImageForAchievementType(achievementType: AchievementType, modifier: Modifier = Modifier) {
  val pair =
      when (achievementType) {
        AchievementType.PLANTS_NUMBER ->
            Pair(
                painterResource(R.drawable.plant_number_achievement),
                stringResource(R.string.plants_number_achievement))
        AchievementType.FRIENDS_NUMBER ->
            Pair(
                painterResource(R.drawable.friends_number_achievement),
                stringResource(R.string.friends_number_achievement))
        AchievementType.HEALTHY_STREAK ->
            Pair(
                painterResource(R.drawable.healthy_streak_achievement),
                stringResource(R.string.healthy_streak_achievement))
      }
  Image(
      modifier = modifier.size(ACHIEVEMENT_IMAGE_SIZE),
      painter = pair.first,
      contentDescription = pair.second)
}

/**
 * Helper function that retrieves the correct description text given the [AchievementType].
 *
 * @param achievementType The [AchievementType] for which the description text should be displayed.
 */
@Composable
fun DescriptionForAchievementType(achievementType: AchievementType) {
  val text =
      when (achievementType) {
        AchievementType.PLANTS_NUMBER -> stringResource(R.string.plants_number_description)
        AchievementType.FRIENDS_NUMBER -> stringResource(R.string.friends_number_description)
        AchievementType.HEALTHY_STREAK -> stringResource(R.string.healthy_streak_description)
      }
  Text(text = text, textAlign = TextAlign.Justify)
}

/**
 * Helper function that retrieves the correct remaining progress text given the [AchievementType].
 *
 * @param achievementType The [AchievementType] for which the remaining progress text should be
 *   displayed.
 * @param neededMore The number of units of progress needed to reach next level.
 * @param nextLevel The next level that can be reached by the user.
 * @param modifier Optional modifier of the composable.
 */
@Composable
fun RemainingProgressForAchievementType(
    achievementType: AchievementType,
    neededMore: Int,
    nextLevel: Int,
    modifier: Modifier = Modifier
) {
  val text =
      when (achievementType) {
        AchievementType.PLANTS_NUMBER ->
            stringResource(R.string.plants_number_next_level, neededMore, nextLevel)
        AchievementType.FRIENDS_NUMBER ->
            stringResource(R.string.friends_number_next_level, neededMore, nextLevel)
        AchievementType.HEALTHY_STREAK ->
            stringResource(R.string.healthy_streak_next_level, neededMore, nextLevel)
      }
  Text(
      text = text,
      fontWeight = FontWeight.Medium,
      modifier =
          modifier.testTag(AchievementsScreenTestTags.getTestTagForRemainingUnits(achievementType)))
}
