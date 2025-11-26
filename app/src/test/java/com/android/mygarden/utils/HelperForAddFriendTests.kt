package com.android.mygarden.utils

import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.profile.PseudoRepository
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.ui.addFriend.AddFriendViewModel

/**
 * Creates an [AddFriendViewModel] with fake repositories by default.
 *
 * Callers can override individual repositories when they need specific behavior for a given test.
 */
fun createViewModel(
    friendsRepo: FriendsRepository = FakeFriendsRepository(),
    userProfileRepo: UserProfileRepository = FakeUserProfileRepository(),
    pseudoRepo: PseudoRepository = FakePseudoRepository()
): AddFriendViewModel =
    AddFriendViewModel(
        friendsRepository = friendsRepo,
        userProfileRepository = userProfileRepo,
        pseudoRepository = pseudoRepo)

/**
 * Test-only fake implementation of [PseudoRepository] used in this test class.
 *
 * It allows configuring:
 * - [searchResults] for [searchPseudoStartingWith],
 * - [uidMap] for [getUidFromPseudo].
 *
 * This avoids changing the global [FakePseudoRepository] used in other tests.
 */
class TestPseudoRepository : PseudoRepository {

  /** List of pseudos returned by [searchPseudoStartingWith]. */
  var searchResults: List<String> = emptyList()

  /** Mapping from pseudo (lowercased) to UID returned by [getUidFromPseudo]. */
  val uidMap: MutableMap<String, String> = mutableMapOf()

  override suspend fun isPseudoAvailable(pseudo: String) = true

  override suspend fun savePseudo(pseudo: String, userId: String) {
    uidMap[pseudo.lowercase()] = userId
  }

  override suspend fun deletePseudo(pseudo: String) {
    uidMap.remove(pseudo.lowercase())
  }

  override suspend fun searchPseudoStartingWith(query: String): List<String> = searchResults

  override suspend fun getUidFromPseudo(pseudo: String): String? = uidMap[pseudo.lowercase()]
}
