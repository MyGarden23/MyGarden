package com.android.mygarden.ui.addFriend

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.TopBar
import com.android.mygarden.ui.profile.Avatar

/** Contains all test tags used within the Add Friend screen UI. */
object AddFriendTestTags {

  const val SEARCH_TEXT = "searchText"
  const val SEARCH_BUTTON = "searchButton"
  const val FRIEND_COLUMN = "friendColumn"

  // Test tags that are Friend Card specific
  fun getTestTagForFriendCard(pseudo: String): String = "friendCardTestTag/${pseudo}"

  fun getTestTagForPseudoOnFriendCard(pseudo: String) = "pseudoOnFriendCardTestTag/${pseudo}"

  fun getTestTagForRowOnFriendCard(pseudo: String) = "rowOnFriendCardTestTag/${pseudo}"

  fun getTestTagForAvatarOnFriendCard(pseudo: String) = "avatarOnFriendCardTestTag/${pseudo}"

  fun getTestTagForButtonOnFriendCard(pseudo: String) = "buttonOnFriendCardTestTag/${pseudo}"
}

/**
 * Screen for adding friends.
 *
 * This screen allows the user to:
 * - enter a search query,
 * - trigger a search,
 * - view the list of found user profiles,
 * - send friend requests.
 *
 * @param onBackPressed Action triggered when the top bar back button is pressed.
 * @param addFriendViewModel The [AddFriendViewModel] responsible for handling UI state and events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    onBackPressed: () -> Unit = {},
    addFriendViewModel: AddFriendViewModel = viewModel(),
) {
  val context = LocalContext.current
  val uiState by addFriendViewModel.uiState.collectAsState()
  Scaffold(
      topBar = { TopBar(title = "Add friend", hasGoBackButton = true, onGoBack = onBackPressed) },
      modifier = Modifier.testTag(NavigationTestTags.ADD_FRIEND_SCREEN),
      content = { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Spacer(modifier = Modifier.fillMaxHeight(0.02f))

          // Search bar row
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(0.6f).testTag(AddFriendTestTags.SEARCH_TEXT),
                value = uiState.query,
                onValueChange = { addFriendViewModel.onQueryChange(it) },
            )
            Button(
                modifier = Modifier.testTag(AddFriendTestTags.SEARCH_BUTTON),
                onClick = { addFriendViewModel.onSearch({}) }, // Start the research
            ) {
              Text(
                  text = "Search",
                  maxLines = 1,
              )
            }
          }

          // List of user results
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(horizontal = 20.dp, vertical = 35.dp)
                      .verticalScroll(rememberScrollState())
                      .testTag(AddFriendTestTags.FRIEND_COLUMN),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.searchResults.forEach { u ->
                  FriendCard(
                      u.id, u.pseudo, u.avatar, u.friendRelation, addFriendViewModel, context)
                }
              }
        }
      },
      containerColor = colorScheme.background,
  )
}

/**
 * Displays a card representing a user in the search results.
 *
 * The card includes:
 * - the user's avatar,
 * - their pseudo,
 * - a button showing the current friend relation (e.g., "Add" or "Added").
 *
 * Clicking the button triggers a friend request action through the [viewModel].
 *
 * @param userId The ID of the user represented by this card.
 * @param pseudo The username displayed on the card.
 * @param avatar The user's avatar.
 * @param friendRelation Current relationship status with the user.
 * @param viewModel ViewModel handling friend request actions.
 * @param context Android context used for localized strings.
 */
@Composable
fun FriendCard(
    userId: String,
    pseudo: String,
    avatar: Avatar,
    friendRelation: FriendRelation,
    viewModel: AddFriendViewModel,
    context: Context,
) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .height(80.dp)
              .testTag(AddFriendTestTags.getTestTagForFriendCard(pseudo))) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Row(
              modifier =
                  Modifier.fillMaxWidth(0.94f)
                      .testTag(AddFriendTestTags.getTestTagForRowOnFriendCard(pseudo)),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween) {
                // Avatar
                Card(
                    modifier =
                        Modifier.clip(CircleShape)
                            .size(80.dp)
                            .testTag(AddFriendTestTags.getTestTagForAvatarOnFriendCard(pseudo))) {
                      Image(
                          painter = painterResource(avatar.resId),
                          contentDescription =
                              context.getString(R.string.avatar_description_friend_screen, "test"),
                          modifier = Modifier.fillMaxSize())
                    }

                // Pseudo
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(0.6f),
                    contentAlignment = Alignment.Center,
                ) {
                  Text(
                      text = pseudo,
                      fontWeight = FontWeight.Bold,
                      fontSize = 20.sp,
                      modifier =
                          Modifier.testTag(
                              AddFriendTestTags.getTestTagForPseudoOnFriendCard(pseudo)),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                  )
                }

                // Relation button
                Button(
                    modifier =
                        Modifier.testTag(AddFriendTestTags.getTestTagForButtonOnFriendCard(pseudo)),
                    onClick = { viewModel.onAdd(userId, {}, {}) },
                    colors = ButtonDefaults.buttonColors(friendRelation.color),
                    content = { Text(friendRelation.toString()) })
              }
        }
      }
}

/**
 * Represents the relationship status between the current user and another user's profile.
 *
 * Each relation provides:
 * - A readable string representation via [toString], used for the button of a [FriendCard].
 * - A [color] property that exposes the appropriate color for the given relation.
 */
enum class FriendRelation {
  /** Indicates that the user can send a friend request. */
  ADD,
  /** Indicates that the users are already connected. */
  ADDED;

  override fun toString(): String {
    return when (this) {
      FriendRelation.ADD -> "Add"
      FriendRelation.ADDED -> "Added"
    }
  }

  /** A color representing this friend relation. */
  val color: Color
    @Composable
    get() =
        when (this) {
          ADD -> colorScheme.primary
          ADDED -> colorScheme.outline
        }
}
