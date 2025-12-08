package com.android.mygarden.ui.achievements

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.android.mygarden.ui.navigation.TopBar

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
    viewModel: AchievementsViewModel = viewModel(),
    onGoBack: () -> Unit = {}
) {
  Scaffold(
      topBar = { TopBar(title = "Achievements", hasGoBackButton = true, onGoBack = onGoBack) },
      modifier = modifier,
      content = { paddingValues ->
        val uiState by viewModel.uiState.collectAsState()
        var showAchievementInfo by remember { mutableStateOf<AchievementType?>(null) }

        Column(
            modifier = modifier.padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              for (type in AchievementType.entries) {
                AchievementCard(
                    achievementType = type,
                    currentLevel = viewModel.getCorrespondingLevel(type, uiState),
                    onClick = { showAchievementInfo = type })
                if (showAchievementInfo == type) {
                  AchievementDialogInfo(
                      modifier = modifier,
                      achievementType = type,
                      currentLevel = viewModel.getCorrespondingLevel(type, uiState),
                      currentValue = viewModel.getCorrespondingValue(type, uiState),
                      neededMore = viewModel.getCorrespondingNeededForNextLevel(type, uiState),
                      nextThreshold = viewModel.getCorrespondingNextThreshold(type, uiState),
                      onDismiss = { showAchievementInfo = null },
                  )
                }
              }
            }
      })
}

@Composable
fun AchievementCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    achievementType: AchievementType,
    currentLevel: Int,
) {
  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .height(150.dp)
              .padding(horizontal = 20.dp)
              .clickable(onClick = onClick),
      elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              ImageForAchievementType(achievementType = achievementType, modifier = modifier)

              Column(
                  modifier = Modifier.fillMaxHeight(),
                  verticalArrangement = Arrangement.SpaceAround,
                  horizontalAlignment = Alignment.Start) {
                    Text(
                        text = achievementType.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 23.sp,
                        modifier = modifier.padding(top = 18.dp))
                    LevelBar(
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
                        fontWeight = FontWeight.Medium)
                  }
            }
      }
}

@Composable
fun AchievementDialogInfo(
    modifier: Modifier = Modifier,
    achievementType: AchievementType,
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
        modifier = modifier.width(300.dp).clip(RoundedCornerShape(16.dp))) {
          Column(
              modifier = modifier.padding(30.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = modifier.fillMaxWidth()) {
                  Icon(
                      painter = painterResource(R.drawable.x_circle),
                      contentDescription =
                          stringResource(R.string.popup_dismiss_button_content_description),
                      tint = MaterialTheme.colorScheme.tertiary,
                      modifier =
                          modifier
                              .size(30.dp)
                              .align(Alignment.TopStart)
                              .clickable(onClick = onDismiss))
                  ImageForAchievementType(
                      achievementType, modifier = modifier.align(Alignment.Center))
                }

                Text(
                    text = achievementType.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp)

                DescriptionForAchievementType(achievementType = achievementType)

                Text(
                    text =
                        stringResource(
                            R.string.level_for_achievement_info,
                            currentLevel,
                            ACHIEVEMENTS_LEVEL_NUMBER),
                    fontWeight = FontWeight.Medium,
                    fontSize = 20.sp)

                if (nextThreshold > 0) {
                  Box() {
                    LevelBar(
                        modifier = modifier,
                        currentValue = currentValue,
                        maxValue = nextThreshold,
                        color = MaterialTheme.colorScheme.tertiary)
                    Text(
                        text =
                            stringResource(
                                R.string.left_for_threshold, currentValue, nextThreshold),
                        fontSize = 14.sp,
                        modifier =
                            modifier.align(Alignment.CenterEnd).padding(end = 10.dp).zIndex(1f))
                  }
                }
                if (neededMore > 0) {
                  RemainingProgressForAchievementType(achievementType, neededMore, currentLevel + 1)
                }
              }
        }
  }
}

@Composable
fun LevelBar(modifier: Modifier = Modifier, currentValue: Int, maxValue: Int, color: Color) {
  assert(currentValue <= 10 && currentValue > 0)
  Box(
      modifier =
          modifier
              .height(22.dp)
              .fillMaxWidth()
              .clip(RoundedCornerShape(50))
              .background(MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.BottomStart) {
        Box(
            modifier =
                modifier
                    .clip(RoundedCornerShape(50))
                    .fillMaxHeight()
                    .fillMaxWidth((currentValue.toFloat() / maxValue))
                    .background(color))
      }
}

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
  Image(modifier = modifier.size(130.dp), painter = pair.first, contentDescription = pair.second)
}

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

@Composable
fun RemainingProgressForAchievementType(
    achievementType: AchievementType,
    neededMore: Int,
    nextLevel: Int
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
  Text(text = text, fontWeight = FontWeight.Medium)
}
