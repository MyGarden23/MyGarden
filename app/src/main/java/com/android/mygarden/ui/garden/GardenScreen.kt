package com.android.mygarden.ui.garden

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.PlantHealthCalculator
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.theme.CustomColors
import com.android.mygarden.ui.theme.ExtendedTheme

/** Test tags to test the screen displays */
object GardenScreenTestTags {
  const val TITLE = "MyGardenTitle"
  const val USER_PROFILE_PICTURE = "UserProfilePicture"
  const val USERNAME = "Username"
  const val EDIT_PROFILE_BUTTON = "EditProfileButton"
  const val GARDEN_LIST = "GardenList"
  const val EMPTY_GARDEN_MSG = "EmptyGardenMsg"
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

// Font sizes
private val PLANT_NAME_FONT_SIZE = 20.sp
private val PLANT_CARD_INFO_FONT_SIZE = 14.sp

// Text values
private const val MY_GARDEN_TITLE_TEXT = "My Garden"
private const val EMPTY_GARDEN_MESSAGE_TEXT =
    "You don't have a plant yet ! Use the button below to add a plant."
private const val ADD_PLANT_FAB_TEXT = "Add a plant"
private const val WATER_BUTTON_ICON_DESCRIPTION = "Water plant button"

private fun getOwnedPlantImageDescription(ownedPlant: OwnedPlant): String =
    "Image of a ${ownedPlant.plant.name}"

/**
 * The screen of the garden with some user profile infos and the list of plants owned by the user.
 *
 * @param modifier the optional modifier of the composable
 * @param gardenViewModel the viewModel that manages the user interactions
 * @param onEditProfile the function to launch when a user clicks on the edit profile button
 * @param onAddPlant the function to launch when a user clicks on the FAB (add a plant button)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(
    modifier: Modifier = Modifier,
    gardenViewModel: GardenViewModel = viewModel(),
    onEditProfile: () -> Unit,
    onAddPlant: () -> Unit
) {

  val context = LocalContext.current
  val uiState by gardenViewModel.uiState.collectAsState()
  val plants = uiState.plants

  // Fetch correct owned plants list at each recomposition of the screen
  LaunchedEffect(Unit) { gardenViewModel.refreshUIState() }

  // Display the error message if the list fetch fails
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      gardenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = modifier.testTag(NavigationTestTags.GARDEN_SCREEN),
      // The top bar is only used to display the title of the screen
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  fontWeight = FontWeight.ExtraBold,
                  style = MaterialTheme.typography.titleLarge,
                  text = MY_GARDEN_TITLE_TEXT)
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background),
            modifier = modifier.testTag(GardenScreenTestTags.TITLE))
      },
      // The button to add a new plant to the collection
      floatingActionButton = { AddPlantFloatingButton(onAddPlant, modifier) },
      containerColor = MaterialTheme.colorScheme.background,
      content = { pd ->
        Column(modifier = modifier.fillMaxWidth().padding(pd)) {
          // Profile row with user profile picture, username and a button to edit the profile
          ProfileRow(onEditProfile, modifier)
          Spacer(modifier = modifier.height(16.dp))
          if (plants.isNotEmpty()) {
            // The full list of owned plant
            LazyColumn(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .padding(horizontal = PLANT_ITEM_HORIZONTAL_PADDING)
                        .testTag(GardenScreenTestTags.GARDEN_LIST),
                verticalArrangement = Arrangement.spacedBy(PLANT_LIST_ITEM_SPACING)) {
                  items(plants.size) { index ->
                    PlantCard(plants[index], modifier, gardenViewModel)
                  }
                }
          } else {
            // The list of plant is empty : display a simple message instead
            Box(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(EMPTY_LIST_MESSAGE_PADDING)
                        .testTag(GardenScreenTestTags.EMPTY_GARDEN_MSG),
                contentAlignment = Alignment.Center) {
                  Text(text = EMPTY_GARDEN_MESSAGE_TEXT)
                }
          }
        }
      })
}

/**
 * The profile row with the user profile picture, its username and a button to edit the profile.
 *
 * @param onEditProfile the function to launch when the edit button is clicked on
 * @param modifier the optional modifier of the composable
 */
@Composable
fun ProfileRow(onEditProfile: () -> Unit, modifier: Modifier = Modifier) {
  Row(
      modifier = modifier.fillMaxWidth().padding(horizontal = PROFILE_ROW_HORIZONTAL_PADDING),
      verticalAlignment = Alignment.CenterVertically) {
        // User profile picture
        Icon(
            modifier = modifier.testTag(GardenScreenTestTags.USER_PROFILE_PICTURE),
            painter = painterResource(R.drawable.profile_unknown_photo_icon_2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = modifier.weight(1f))

        // Username
        Text(
            modifier = modifier.testTag(GardenScreenTestTags.USERNAME),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            text = "Username") /* TODO: Replace with real user name when implementation is done */
        Spacer(modifier = modifier.weight(1f))

        // Edit profile button
        IconButton(
            modifier = modifier.testTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON),
            onClick = onEditProfile) {
              Icon(
                  painter = painterResource(R.drawable.edit_icon),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary)
            }
      }
}

/**
 * Represents the floating button to add a plant to the garden.
 *
 * @param onAddPlant the function to launch when the button is clicked on
 * @param modifier the optional modifier of the composable
 */
@Composable
fun AddPlantFloatingButton(onAddPlant: () -> Unit, modifier: Modifier = Modifier) {
  ExtendedFloatingActionButton(
      modifier = modifier.testTag(GardenScreenTestTags.ADD_PLANT_FAB), onClick = onAddPlant) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
              painter = painterResource(R.drawable.tree_icon),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary)
          Text(text = ADD_PLANT_FAB_TEXT)
        }
      }
}

/**
 * An item of the list of owned plants. Changes color depending on the health status of the plant.
 *
 * @param ownedPlant the owned plant with characteristics to display
 * @param modifier the optional modifier of the composable
 */
@Composable
fun PlantCard(ownedPlant: OwnedPlant, modifier: Modifier = Modifier, viewModel: GardenViewModel) {
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
                        ImageRequest.Builder(LocalContext.current)
                            .data(ownedPlant.plant.image ?: "")
                            .build(),
                    contentDescription = getOwnedPlantImageDescription(ownedPlant),
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
                  onButtonPressed = { viewModel.waterPlant(ownedPlant) })
            }
      })
}

/**
 * The watering button that users can press when they water the plant that is on the plant card.
 *
 * @param modifier the optional modifier of the composable
 * @param color the color of the button
 * @param onButtonPressed the lambda passed when the button is pressed
 */
@Composable
fun WaterButton(modifier: Modifier = Modifier, color: Color, onButtonPressed: () -> Unit) {
  Box(
      modifier =
          modifier
              .size(WATER_BUTTON_SIZE)
              .clip(RoundedCornerShape(PLANT_CARD_ROUND_SHAPING))
              .border(
                  WATER_BUTTON_BORDER_WIDTH, color, RoundedCornerShape(PLANT_CARD_ROUND_SHAPING))
              .clickable(onClick = onButtonPressed),
      contentAlignment = Alignment.Center) {
        Icon(
            Icons.Default.WaterDrop,
            contentDescription = WATER_BUTTON_ICON_DESCRIPTION,
            tint = color,
            modifier = modifier.size(WATER_BUTTON_DROP_ICON_SIZE))
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
