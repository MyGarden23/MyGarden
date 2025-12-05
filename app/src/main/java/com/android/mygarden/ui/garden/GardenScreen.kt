package com.android.mygarden.ui.garden

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.mygarden.R
import com.android.mygarden.model.offline.OfflineStateManager
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.PlantHealthCalculator
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.navigation.TopBar
import com.android.mygarden.ui.theme.CustomColors
import com.android.mygarden.ui.theme.ExtendedTheme
import com.android.mygarden.ui.utils.OfflineMessages
import com.android.mygarden.ui.utils.handleOfflineClick

/** Test tags to test the screen displays */
object GardenScreenTestTags {
  const val USER_PROFILE_PICTURE = "UserProfilePicture"
  const val USERNAME = "Username"
  const val EDIT_PROFILE_BUTTON = "EditProfileButton"
  const val GARDEN_LIST = "GardenList"
  const val EMPTY_GARDEN_MSG = "EmptyGardenMsg"
  const val EMPTY_FILTER_MSG = "EmptyFilterMsg"
  const val ADD_PLANT_FAB = "AddPlantFAB"

  // Test tags that are Plant Card specific
  fun getTestTagForOwnedPlant(plant: OwnedPlant): String = "OwnedPlantNumber${plant.id}"

  fun getTestTagForOwnedPlantImage(plant: OwnedPlant): String = "OwnedPlantNumber${plant.id}_image"

  fun getTestTagForOwnedPlantName(plant: OwnedPlant): String = "OwnedPlantNumber${plant.id}_name"

  fun getTestTagForOwnedPlantLatinName(plant: OwnedPlant): String =
      "OwnedPlantNumber${plant.id}_latin_name"

  fun getTestTagForOwnedPlantStatus(plant: OwnedPlant): String =
      "OwnedPlantNumber${plant.id}_status"

  fun getTestTagForOwnedPlantWaterBar(plant: OwnedPlant): String =
      "OwnedPlantNumber${plant.id}_water_bar"

  fun getTestTagForOwnedPlantWaterButton(plant: OwnedPlant): String =
      "OwnedPlantNumber${plant.id}_water_button"
}

/**
 * Represents the color palette used to style a PlantCard composable. These colors should depend on
 * the health status of the plant for which the card is being created.
 *
 * @param backgroundColor the background color of the card
 * @param wateringColor the color of both the watering button and the water level bar
 */
data class PlantCardColorPalette(val backgroundColor: Color, val wateringColor: Color)

// All paddings (excepts the spacers)
private val PLANT_ITEM_HORIZONTAL_PADDING = 30.dp
private val PROFILE_ROW_HORIZONTAL_PADDING = 30.dp
private val EMPTY_LIST_MESSAGE_PADDING = 40.dp
private val PLANT_CHARACTERISTICS_COL_HORIZONTAL_PADDING = 10.dp
private val PLANT_CARD_INNER_ROW_PADDING = 12.dp
private val LOGOUT_BUTTON_PADDING = 10.dp

// Other used dimensions
private val PLANT_LIST_ITEM_SPACING = 10.dp
private val PLANT_CARD_HEIGHT = 110.dp
private val PLANT_CARD_ELEVATION = 2.dp
private val PLANT_CARD_ROUND_SHAPING = 8.dp
private val WATER_BUTTON_SIZE = 35.dp
private val WATER_BUTTON_BORDER_WIDTH = 2.dp
private val WATER_BUTTON_DROP_ICON_SIZE = 20.dp
private val WATER_BAR_HEIGHT = 14.dp
private val WATER_BAR_WRAPPER_HEIGHT = 14.dp
private val AVATAR_SIZE = 40.dp

// Font sizes
private val PLANT_NAME_FONT_SIZE = 20.sp
private val PLANT_CARD_INFO_FONT_SIZE = 14.sp

/**
 * The screen of the garden with some user profile infos and the list of plants owned by the user.
 *
 * @param modifier the optional modifier of the composable
 * @param gardenViewModel the viewModel that manages the user interactions
 * @param onEditProfile the function to launch when a user clicks on the edit profile button
 * @param onAddPlant the function to launch when a user clicks on the FAB (add a plant button)
 * @param onSignOut the function to sign out the user from the app
 * @param onPlantClick the function to launch when a user clicks on a plant card (default value for
 *   test compatibility)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(
    modifier: Modifier = Modifier,
    gardenViewModel: GardenViewModel = viewModel(),
    onEditProfile: () -> Unit,
    onAddPlant: () -> Unit,
    onSignOut: () -> Unit = {},
    onPlantClick: (OwnedPlant) -> Unit = {}
) {

  val context = LocalContext.current
  val uiState by gardenViewModel.uiState.collectAsState()
  val plants = uiState.plants
  val filteredAndSortedPlants = uiState.filteredAndSortedPlants

  // Collect offline state
  val isOnline by OfflineStateManager.isOnline.collectAsState()

  // Fetch correct owned plants list at each recomposition of the screen
  LaunchedEffect(Unit) { gardenViewModel.refreshUIState() }

  // Display the error message if the list fetch fails
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { resId ->
      Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
      gardenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = modifier.testTag(NavigationTestTags.GARDEN_SCREEN),
      // The top bar is only used to display the title of the screen
      topBar = {
        TopBar(
            title = context.getString(Screen.Garden.nameResId),
            hasSignOutButton = true,
            onSignOut = onSignOut)
      },
      // The button to add a new plant to the collection
      floatingActionButton = { AddPlantFloatingButton(onAddPlant, modifier, isOnline) },
      floatingActionButtonPosition = FabPosition.Start,
      containerColor = MaterialTheme.colorScheme.background,
      content = { pd ->
        Column(modifier = modifier.fillMaxWidth().padding(pd)) {
          // Profile row with user profile picture, username and a button to edit the profile
          ProfileRow(onEditProfile, modifier, uiState, isOnline)
          Spacer(modifier = modifier.height(16.dp))

          // Sort and filter bar - only show if there are plants in the garden
          if (plants.isNotEmpty()) {
            SortFilterBar(
                currentSort = uiState.currentSortOption,
                currentFilter = uiState.currentFilterOption,
                onSortChange = { gardenViewModel.setSortOption(it) },
                onFilterChange = { gardenViewModel.setFilterOption(it) },
                modifier = modifier)
            Spacer(modifier = modifier.height(12.dp))
          }

          // Display the garden content (list or empty message)
          GardenContent(
              plants = plants,
              filteredAndSortedPlants = filteredAndSortedPlants,
              onPlantClick = onPlantClick,
              gardenViewModel = gardenViewModel,
              modifier = modifier,
              isOnline = isOnline)
        }
      })
}

/**
 * The profile row with the user profile picture, its username and a button to edit the profile.
 *
 * @param onEditProfile the function to launch when the edit button is clicked on
 * @param modifier the modifier for the row
 * @param uiState the UI state
 * @param isOnline whether the device is online
 */
@Composable
fun ProfileRow(
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
    uiState: GardenUIState,
    isOnline: Boolean
) {
  val context = LocalContext.current
  Row(
      modifier = modifier.fillMaxWidth().padding(horizontal = PROFILE_ROW_HORIZONTAL_PADDING),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          // User avatar
          Card(
              modifier =
                  modifier
                      .clip(CircleShape)
                      .size(AVATAR_SIZE)
                      .testTag(GardenScreenTestTags.USER_PROFILE_PICTURE)) {
                Image(
                    painter = painterResource(uiState.userAvatar.resId),
                    contentDescription =
                        context.getString(R.string.avatar_description, uiState.userAvatar.name),
                    modifier = modifier.fillMaxSize())
              }
          Spacer(modifier = modifier.weight(1f))

          // Username
          Text(
              modifier = modifier.testTag(GardenScreenTestTags.USERNAME),
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
              style = MaterialTheme.typography.titleLarge,
              text = uiState.userName)
          Spacer(modifier = modifier.weight(1f))

          // Edit profile button
          IconButton(
              modifier = modifier.testTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON),
              onClick = {
                handleOfflineClick(
                    isOnline = isOnline,
                    context = context,
                    offlineMessageResId = OfflineMessages.CANNOT_EDIT_PROFILE) {
                      onEditProfile()
                    }
              }) {
                Icon(
                    painter = painterResource(R.drawable.edit_icon),
                    contentDescription = null,
                    tint =
                        if (isOnline) {
                          MaterialTheme.colorScheme.primary
                        } else {
                          MaterialTheme.colorScheme.onSurfaceVariant
                        })
              }
        }
      }
}

/**
 * Represents the floating button to add a plant to the garden.
 *
 * @param onAddPlant the function to launch when the button is clicked on
 * @param modifier the optional modifier of the composable
 * @param isOnline whether the device is currently online
 */
@Composable
fun AddPlantFloatingButton(
    onAddPlant: () -> Unit,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true
) {
  val context = LocalContext.current
  ExtendedFloatingActionButton(
      modifier = modifier.testTag(GardenScreenTestTags.ADD_PLANT_FAB),
      onClick = {
        handleOfflineClick(
            isOnline = isOnline,
            context = context,
            offlineMessageResId = OfflineMessages.CANNOT_ADD_PLANTS) {
              onAddPlant()
            }
      },
      containerColor =
          if (isOnline) MaterialTheme.colorScheme.primaryContainer
          else MaterialTheme.colorScheme.surfaceVariant) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
              painter = painterResource(R.drawable.tree_icon),
              contentDescription = null,
              tint =
                  if (isOnline) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
              text = context.getString(R.string.add_plant_fab_text),
              color =
                  if (isOnline) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
}

/**
 * An item of the list of owned plants. Changes color depending on the health status of the plant.
 *
 * @param ownedPlant the owned plant with characteristics to display
 * @param modifier the optional modifier of the composable
 * @param onClick the callback called when clicked on the plant card
 * @param viewModel the viewModel of the screen (used to update when watering button is pressed)
 * @param isOnline a boolean indicating if the device is online
 */
@Composable
fun PlantCard(
    ownedPlant: OwnedPlant,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    viewModel: GardenViewModel,
    isOnline: Boolean
) {
  val context = LocalContext.current
  // The color palette of the card depending on the health status of the plant
  val colorPalette =
      colorsFromHealthStatus(
          status = ownedPlant.plant.healthStatus,
          colorScheme = MaterialTheme.colorScheme,
          customColors = ExtendedTheme.colors)
  // The water level Float used in the water level bar
  val waterLevel =
      remember(
          ownedPlant.lastWatered,
          ownedPlant.previousLastWatered,
          ownedPlant.plant.wateringFrequency) {
            PlantHealthCalculator()
                .calculateInStatusFloat(
                    lastWatered = ownedPlant.lastWatered,
                    wateringFrequency = ownedPlant.plant.wateringFrequency,
                    previousLastWatered = ownedPlant.previousLastWatered)
          }
  // The colored box container
  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .height(PLANT_CARD_HEIGHT)
              .clickable(onClick = { onClick() })
              .testTag(GardenScreenTestTags.getTestTagForOwnedPlant(ownedPlant)),
      // Color changing
      colors = CardDefaults.cardColors(containerColor = colorPalette.backgroundColor),
      elevation = CardDefaults.cardElevation(defaultElevation = PLANT_CARD_ELEVATION),
      shape = RoundedCornerShape(PLANT_CARD_ROUND_SHAPING),
      content = {
        Row(
            modifier = modifier.fillMaxSize().padding(PLANT_CARD_INNER_ROW_PADDING),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceAround) {
              // The image of the plant
              Box {
                AsyncImage(
                    model =
                        ImageRequest.Builder(context).data(ownedPlant.plant.image ?: "").build(),
                    contentDescription =
                        context.getString(
                            R.string.owned_plant_image_description, ownedPlant.plant.name),
                    modifier =
                        modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(PLANT_CARD_ROUND_SHAPING))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag(GardenScreenTestTags.getTestTagForOwnedPlantImage(ownedPlant)),
                    contentScale = ContentScale.Crop)
              }
              /* All the characteristics of the plant: it's name, latin name,
              health status and watering level */
              Column(
                  modifier =
                      modifier
                          .fillMaxHeight()
                          .weight(1f)
                          .padding(horizontal = PLANT_CHARACTERISTICS_COL_HORIZONTAL_PADDING),
                  verticalArrangement = Arrangement.SpaceAround) {
                    Row(verticalAlignment = Alignment.Bottom) {
                      /* Make both the plant name and the latin name do an ellipsis when
                      the name is too long to fit on the card */
                      Text(
                          text = ownedPlant.plant.name,
                          fontWeight = FontWeight.Bold,
                          fontSize = PLANT_NAME_FONT_SIZE,
                          modifier =
                              modifier
                                  .alignByBaseline()
                                  .testTag(
                                      GardenScreenTestTags.getTestTagForOwnedPlantName(ownedPlant)),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis)
                      Spacer(modifier = modifier.width(10.dp))
                      Text(
                          text = ownedPlant.plant.latinName,
                          fontStyle = FontStyle.Italic,
                          fontSize = PLANT_CARD_INFO_FONT_SIZE,
                          modifier =
                              modifier
                                  .alignByBaseline()
                                  .testTag(
                                      GardenScreenTestTags.getTestTagForOwnedPlantLatinName(
                                          ownedPlant)),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        text = stringResource(ownedPlant.plant.healthStatus.descriptionRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            modifier.testTag(
                                GardenScreenTestTags.getTestTagForOwnedPlantStatus(ownedPlant)),
                        fontSize = PLANT_CARD_INFO_FONT_SIZE)
                    // Wrap the water level bar in a Box to make
                    // it have the right dimensions in the Column
                    Box(
                        modifier = modifier.height(WATER_BAR_WRAPPER_HEIGHT),
                        contentAlignment = Alignment.Center) {
                          WaterBar(
                              waterLevel = waterLevel,
                              color = colorPalette.wateringColor,
                              modifier = modifier,
                              ownedPlant = ownedPlant)
                        }
                  }
              WaterButton(
                  color = colorPalette.wateringColor,
                  modifier =
                      modifier.testTag(
                          GardenScreenTestTags.getTestTagForOwnedPlantWaterButton(ownedPlant)),
                  onButtonPressed = {
                    handleOfflineClick(
                        isOnline = isOnline,
                        context = context,
                        offlineMessageResId = OfflineMessages.CANNOT_WATER_PLANTS) {
                          viewModel.waterPlant(ownedPlant)
                        }
                  },
                  isOnline = isOnline)
            }
      })
}

/**
 * The watering button that users can press when they water the plant that is on the plant card.
 *
 * @param modifier the optional modifier of the composable
 * @param color the color of the button
 * @param onButtonPressed the lambda passed when the button is pressed
 * @param isOnline whether the device is currently online
 */
@Composable
fun WaterButton(
    modifier: Modifier = Modifier,
    color: Color,
    onButtonPressed: () -> Unit,
    isOnline: Boolean
) {
  val context = LocalContext.current
  Box(
      modifier =
          modifier
              .size(WATER_BUTTON_SIZE)
              .clip(RoundedCornerShape(PLANT_CARD_ROUND_SHAPING))
              .border(
                  WATER_BUTTON_BORDER_WIDTH,
                  if (isOnline) color else MaterialTheme.colorScheme.surfaceVariant,
                  RoundedCornerShape(PLANT_CARD_ROUND_SHAPING))
              .clickable(onClick = onButtonPressed),
      contentAlignment = Alignment.Center) {
        Icon(
            Icons.Default.WaterDrop,
            contentDescription = context.getString(R.string.water_button_icon_description),
            tint = if (isOnline) color else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(WATER_BUTTON_DROP_ICON_SIZE))
      }
}

/**
 * The water status bar represents the level of water the plant that is on the card has.
 *
 * @param modifier the optional modifier of the composable
 * @param waterLevel the water level of the given plant (should be between 0 and 1)
 * @param color the color of the bar
 * @param ownedPlant the plant to which the water bar is attached to (used for test tags)
 * @throws AssertionError if the waterLevel is not between 0 and 1 (both included)
 */
@Composable
fun WaterBar(
    modifier: Modifier = Modifier,
    waterLevel: Float,
    color: Color,
    ownedPlant: OwnedPlant
) {
  assert(waterLevel >= 0f && waterLevel <= 1f)
  Box(
      modifier =
          modifier
              .height(WATER_BAR_HEIGHT)
              .fillMaxWidth()
              .clip(RoundedCornerShape(PLANT_CARD_ROUND_SHAPING))
              .background(MaterialTheme.colorScheme.background),
      contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier =
                modifier
                    .fillMaxHeight()
                    .fillMaxWidth(waterLevel)
                    .background(color)
                    .testTag(GardenScreenTestTags.getTestTagForOwnedPlantWaterBar(ownedPlant)))
      }
}

/**
 * Function that returns the right color palette depending on the given plant health status.
 *
 * @param status the status of the plant we create a card for
 * @param colorScheme the colorscheme of the app's Material theme
 * @param customColors the custom colors in the extended theme
 * @return the right palette depending on the plant's health status
 */
fun colorsFromHealthStatus(
    status: PlantHealthStatus,
    colorScheme: ColorScheme,
    customColors: CustomColors
): PlantCardColorPalette {
  return when (status) {
    PlantHealthStatus.UNKNOWN ->
        PlantCardColorPalette(colorScheme.surfaceVariant, customColors.wateringBlue)
    PlantHealthStatus.HEALTHY ->
        PlantCardColorPalette(colorScheme.primary, customColors.wateringBlue)
    PlantHealthStatus.SLIGHTLY_DRY ->
        PlantCardColorPalette(colorScheme.primaryContainer, customColors.wateringBlue)
    PlantHealthStatus.NEEDS_WATER,
    PlantHealthStatus.OVERWATERED ->
        PlantCardColorPalette(colorScheme.secondaryContainer, customColors.wateringOrange)
    PlantHealthStatus.SEVERELY_DRY,
    PlantHealthStatus.SEVERELY_OVERWATERED ->
        PlantCardColorPalette(customColors.redPlantCardBackground, colorScheme.error)
  }
}

/**
 * Displays the garden content: either the list of plants or an empty message.
 *
 * @param plants The full list of plants (used to determine if garden is empty)
 * @param filteredAndSortedPlants The filtered and sorted list to display
 * @param onPlantClick Callback when a plant card is clicked
 * @param gardenViewModel The view model for plant actions
 * @param modifier Optional modifier for the composable
 * @param isOnline a boolean indicating if the device is online
 */
@Composable
fun GardenContent(
    plants: List<OwnedPlant>,
    filteredAndSortedPlants: List<OwnedPlant>,
    onPlantClick: (OwnedPlant) -> Unit,
    gardenViewModel: GardenViewModel,
    modifier: Modifier = Modifier,
    isOnline: Boolean
) {
  if (filteredAndSortedPlants.isNotEmpty()) {

    // The full list of owned plants
    LazyColumn(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = PLANT_ITEM_HORIZONTAL_PADDING)
                .testTag(GardenScreenTestTags.GARDEN_LIST),
        verticalArrangement = Arrangement.spacedBy(PLANT_LIST_ITEM_SPACING)) {
          items(filteredAndSortedPlants.size) { index ->
            PlantCard(
                filteredAndSortedPlants[index],
                modifier,
                { onPlantClick(filteredAndSortedPlants[index]) },
                gardenViewModel,
                isOnline)
          }
        }
  } else {
    // The list of plants is empty: display a message
    EmptyGardenMessage(plants = plants, modifier = modifier)
  }
}

/**
 * Displays an appropriate message when no plants are shown.
 *
 * Shows different messages depending on whether the garden is truly empty or just filtered out.
 *
 * @param plants The full list of plants (used to determine the correct message)
 * @param modifier Optional modifier for the composable
 */
@Composable
fun EmptyGardenMessage(plants: List<OwnedPlant>, modifier: Modifier = Modifier) {
  val isEmpty = plants.isEmpty()
  val testTag =
      if (isEmpty) GardenScreenTestTags.EMPTY_GARDEN_MSG else GardenScreenTestTags.EMPTY_FILTER_MSG
  val message =
      if (isEmpty) stringResource(R.string.empty_garden_message)
      else stringResource(R.string.empty_filter_message)

  Box(
      modifier = modifier.fillMaxSize().padding(EMPTY_LIST_MESSAGE_PADDING).testTag(testTag),
      contentAlignment = Alignment.Center) {
        Text(text = message)
      }
}
