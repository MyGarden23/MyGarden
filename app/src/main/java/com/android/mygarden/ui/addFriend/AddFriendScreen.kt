package com.android.mygarden.ui.addFriend

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R
import com.android.mygarden.model.offline.OfflineStateManager
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.TopBar
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.utils.OfflineMessages
import com.android.mygarden.ui.utils.handleOfflineClick

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

const val FRACTION_SPACER = 0.02f
const val FRACTION_OUTLINED_TEXT = 0.6f
const val MAX_LINE = 1
val PADDING_VERTICAL = 20.dp
val PADDING_HORIZONTAL = 35.dp
val VERTICAL_ARRANGEMENT_SPACE = 10.dp
val CARD_HEIGHT = 80.dp
const val FRACTION_ROW_WIDTH = 0.94f
const val FRACTION_BOX_WIDTH = 0.6f
val TEXT_FONT_SIZE = 20.sp
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
  // Collect offline state
  val isOnline by OfflineStateManager.isOnline.collectAsState()

  Scaffold(
      topBar = {
        TopBar(
            title = stringResource(R.string.top_bar_name),
            hasGoBackButton = true,
            onGoBack = onBackPressed)
      },
      modifier = Modifier.testTag(NavigationTestTags.ADD_FRIEND_SCREEN),
      content = { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Spacer(modifier = Modifier.fillMaxHeight(FRACTION_SPACER))

          // Search bar row
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedTextField(
                modifier =
                    Modifier.fillMaxWidth(FRACTION_OUTLINED_TEXT)
                        .testTag(AddFriendTestTags.SEARCH_TEXT),
                value = uiState.query,
                onValueChange = { addFriendViewModel.onQueryChange(it) },
            )
            Button(
                modifier = Modifier.testTag(AddFriendTestTags.SEARCH_BUTTON),
                onClick = {
                  handleOfflineClick(
                      isOnline = isOnline,
                      context = context,
                      offlineMessageResId = OfflineMessages.CANNOT_SEARCH_FRIENDS,
                      onlineAction = {
                        addFriendViewModel.onSearch({ // Start the research
                          Toast.makeText(
                                  context,
                                  context.getString(R.string.error_failed_search),
                                  Toast.LENGTH_SHORT)
                              .show()
                        })
                      })
                },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            if (isOnline) {
                              colorScheme.primaryContainer
                            } else {
                              colorScheme.surfaceVariant
                            })) {
                  Text(
                      text = context.getString(R.string.search_button),
                      maxLines = MAX_LINE,
                      color = colorScheme.onPrimaryContainer)
                }
          }

          // List of user results
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(horizontal = PADDING_VERTICAL, vertical = PADDING_HORIZONTAL)
                      .verticalScroll(rememberScrollState())
                      .testTag(AddFriendTestTags.FRIEND_COLUMN),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(VERTICAL_ARRANGEMENT_SPACE)) {
                uiState.searchResults.forEach { friend ->
                  val relation = uiState.relations[friend.id] ?: FriendRelation.ADD
                  FriendCard(
                      friend.id,
                      friend.pseudo,
                      friend.avatar,
                      relation,
                      addFriendViewModel,
                      context)
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
 * @param relation The current relation status with the user.
 * @param viewModel ViewModel handling friend request actions.
 * @param recvToken the token of the user to be able to send notifications
 * @param context Android context used for localized strings.
 */
@Composable
fun FriendCard(
    userId: String,
    pseudo: String,
    avatar: Avatar,
    relation: FriendRelation,
    viewModel: AddFriendViewModel,
    context: Context,
) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .height(CARD_HEIGHT)
              .testTag(AddFriendTestTags.getTestTagForFriendCard(pseudo))) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Row(
              modifier =
                  Modifier.fillMaxWidth(FRACTION_ROW_WIDTH)
                      .testTag(AddFriendTestTags.getTestTagForRowOnFriendCard(pseudo)),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween) {
                // Avatar
                Card(
                    modifier =
                        Modifier.clip(CircleShape)
                            .size(CARD_HEIGHT)
                            .testTag(AddFriendTestTags.getTestTagForAvatarOnFriendCard(pseudo))) {
                      Image(
                          painter = painterResource(avatar.resId),
                          contentDescription =
                              context.getString(R.string.avatar_description_friend_screen, pseudo),
                          modifier = Modifier.fillMaxSize())
                    }

                // Pseudo
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(FRACTION_BOX_WIDTH),
                    contentAlignment = Alignment.Center,
                ) {
                  Text(
                      text = pseudo,
                      fontWeight = FontWeight.Bold,
                      fontSize = TEXT_FONT_SIZE,
                      modifier =
                          Modifier.testTag(
                              AddFriendTestTags.getTestTagForPseudoOnFriendCard(pseudo)),
                      maxLines = MAX_LINE,
                      overflow = TextOverflow.Ellipsis,
                  )
                }

                // Relation button
                Button(
                    modifier =
                        Modifier.testTag(AddFriendTestTags.getTestTagForButtonOnFriendCard(pseudo)),
                    onClick = {
                      viewModel.onAsk(
                          userId,
                          {
                            Toast.makeText(
                                    context,
                                    context.getString(R.string.ask_friend_failed, pseudo),
                                    Toast.LENGTH_SHORT)
                                .show()
                          },
                          {
                            Toast.makeText(
                                    context,
                                    context.getString(R.string.asked_friend_successfully, pseudo),
                                    Toast.LENGTH_SHORT)
                                .show()
                          })
                    },
                    colors = ButtonDefaults.buttonColors(relation.color),
                    content = { Text(relation.label()) })
              }
        }
      }
}
