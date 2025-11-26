package com.android.mygarden.model.caretips

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.utils.FirestoreProfileTest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CareTipsRepositoryEmulatorTest : FirestoreProfileTest() {

  private lateinit var tipsRepo: CareTipsRepository
  private val TIP_FIELD = "tip"

  @Before
  override fun setUp() {
    super.setUp()
    tipsRepo = CareTipsRepositoryFirestore(db)
  }

  @Test
  fun noTipReturnsNull() = runTest {
    val latinName = "latin1"
    val tip = tipsRepo.getTip(latinName, PlantHealthStatus.HEALTHY)
    assertNull(tip)
  }

  @Test
  fun getTipRetrievesCorrectTip() = runTest {
    val latinName = "latin2"
    // sets a fake tip on firestore
    db.collection("tips")
        .document(latinName)
        .collection("health_status")
        .document(PlantHealthStatus.HEALTHY.name)
        .set(mapOf(TIP_FIELD to "tip2"))
        .await()

    // retrieve the fake tip
    val tip = tipsRepo.getTip(latinName, PlantHealthStatus.HEALTHY)
    assertEquals("tip2", tip)
  }

  @Test
  fun addTipCachesCorrectlyOnFirestore() = runTest {
    val latinName = "latin3"

    tipsRepo.addTip(latinName, PlantHealthStatus.HEALTHY, "tip3")

    val snapshot =
        db.collection("tips")
            .document(latinName)
            .collection("health_status")
            .document(PlantHealthStatus.HEALTHY.name)
            .get()
            .await()

    assertTrue(snapshot.exists())
    assertEquals("tip3", snapshot.getString(TIP_FIELD))
  }

  @Test
  fun addThenGetReturnsSame() = runTest {
    val latinName = "latin4"
    val tip = "tip4"

    tipsRepo.addTip(latinName, PlantHealthStatus.HEALTHY, tip)
    val got = tipsRepo.getTip(latinName, PlantHealthStatus.HEALTHY)

    assertEquals(tip, got)
  }

  @Test
  fun differentStatusesStoreDifferentTips() = runTest {
    val latinName = "latin5"

    tipsRepo.addTip(latinName, PlantHealthStatus.HEALTHY, "healthy")
    tipsRepo.addTip(latinName, PlantHealthStatus.SLIGHTLY_DRY, "under")

    assertEquals("healthy", tipsRepo.getTip(latinName, PlantHealthStatus.HEALTHY))
    assertEquals("under", tipsRepo.getTip(latinName, PlantHealthStatus.SLIGHTLY_DRY))
  }
}
