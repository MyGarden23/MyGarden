package com.android.mygarden.ui.addFriend

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.android.mygarden.R

enum class FriendRelation(@StringRes val labelRes: Int) {
  ADD(R.string.add_enum),
  ADDED(R.string.added_enum),
  PENDING(R.string.pending_enum),
    ADDBACK(R.string.add_back_enum)
}

@Composable fun FriendRelation.label(): String = stringResource(labelRes)

val FriendRelation.color: Color
  @Composable
  get() =
      when (this) {
        FriendRelation.ADD -> colorScheme.primary
        FriendRelation.ADDED -> colorScheme.outline
        FriendRelation.PENDING -> colorScheme.tertiary
        FriendRelation.ADDBACK -> colorScheme.primary
      }
