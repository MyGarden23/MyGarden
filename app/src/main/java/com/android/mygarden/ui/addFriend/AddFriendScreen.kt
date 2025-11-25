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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.mygarden.R
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.TopBar

object AddFriendTestTags {

  // Test tags that are Friend Card specific
  fun getTestTagForFriendCard(pseudo: String): String = "friendCardTestTag/${pseudo}"

  fun getTestTagForPseudoOnFriendCard(pseudo: String) = "pseudoOnFriendCardTestTag/${pseudo}"
}

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    onBackPressed: () -> Unit = {},
    // addFriendViewModel: EditPlantViewModel = viewModel(),
) {
  val context = LocalContext.current
  // val addFriendUIState by addFriendViewModel.uiState.collectAsState()
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
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(0.6f),
                value = "Pseudo",
                onValueChange = {},
            )
            Button(
                // modifier = Modifier.fillMaxWidth(),
                onClick = {}, // Start the research
            ) {
              Text(
                  text = "Search",
                  // maxLines = 1,
              )
            }
          }
          Column(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(horizontal = 20.dp, vertical = 35.dp)
                      .verticalScroll(rememberScrollState()),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FriendCard("Pseudo", context)
                FriendCard("LONG long NAME YOOO", context)
                FriendCard("Jean_wile ROdrigue", context)
                FriendCard("Pseudo", context)
                FriendCard("Pseudo", context)
                FriendCard("Pseudo", context)
                FriendCard("Pseudo", context)
                FriendCard("Pseudo", context)
              }
        }
      },
      containerColor = MaterialTheme.colorScheme.background,
  )
}

@Composable
fun FriendCard(
    pseudo: String,
    context: Context,
) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .height(80.dp)
              .testTag(AddFriendTestTags.getTestTagForFriendCard(pseudo))) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Row(
              modifier = Modifier.fillMaxWidth(0.94f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween) {
                Card(modifier = Modifier.clip(CircleShape).size(80.dp)) {
                  Image(
                      painter = painterResource(R.drawable.avatar_a1),
                      contentDescription =
                          context.getString(R.string.avatar_description_friend_screen, "test"),
                      modifier = Modifier.fillMaxSize())
                }
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(0.6f),
                    contentAlignment = Alignment.Center,
                ) {
                  Text(
                      text = pseudo,
                      fontWeight = FontWeight.Bold,
                      fontSize = 20.sp,
                      modifier =
                          Modifier
                              // .alignByBaseline()
                              .testTag(AddFriendTestTags.getTestTagForPseudoOnFriendCard(pseudo)),
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                  )
                }
                Button(
                    onClick = {}, // Update the UIstate to change the relation.
                    content = { Text("Add") })
              }
        }
      }
}

enum class RELATION {
  ADD,
  ADDED,
  ASKED;

  val color: Color
    get() =
        when (this) {
          ADD -> TODO()
          RELATION.ADDED -> TODO()
          RELATION.ASKED -> TODO()
        }
}
