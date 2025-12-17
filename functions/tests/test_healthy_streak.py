from unittest.mock import patch, MagicMock
from datetime import datetime, timezone, timedelta
from freezegun import freeze_time

# Helper function to transform datetime into 
# a Firestore number that stored a (java.sql) Timestamp
def ms(dt): return int(dt.timestamp() * 1000)

# Define a fixed current time
TEST_NOW = datetime(2025, 10, 10, 0, 0, 0, tzinfo=timezone.utc)

def seed_user_with_plants(db):
    """Mock the given database to have one user and two plants."""

    uid = "user_1"
    uref = db.collection("users").document(uid)
    uref.set({"name": "Alice", "fcmToken": "fake-token-123"})

    plants_col = uref.collection("plants")

    # The plant should transition to NEEDS_WATER hence the healthySince should be transition to 0
    plants_col.document("plant_need_water").set({
        "id": "plant_need_water",
        "lastWatered": ms(TEST_NOW - timedelta(days=8)),
        "healthySince": ms(TEST_NOW - timedelta(days=8)),
        "previousLastWatered": 0,
        "plant": {
            "name": "Rose",
            "wateringFrequency": 7,
            "healthStatus": "HEALTHY", # Should go to NEEDS_WATER after update
        }
    })
    return uid


# Patch the real-time sources: FCM send and datetime.datetime.now()
@patch("firebase_admin.messaging.send")
@freeze_time("2025-10-10") # = TEST_NOW
def test_update_all_plants_status(mock_send, db):
    # Mock the send function to return a success response
    mock_send.return_value = "mock-message-id"
    
    import firestore_client
    
    # Clear the LRU cache to ensure fresh state
    firestore_client.get_firestore_client.cache_clear()
    
    # Mock the get_firestore_client to return our mock db
    with patch('firestore_client.get_firestore_client', return_value=db):    
        # Seed the database
        uid = seed_user_with_plants(db)

        import jobs
        # Run the function
        jobs.update_all_plants_status_impl()

    # Verifications
    # Verify the needs water plant status (NEEDS_WATER)
    rose = db.collection("users").document(uid).collection("plants").document("plant_need_water").get().to_dict()
    assert rose["plant"]["healthStatus"] == "NEEDS_WATER", f"Expected NEEDS_WATER but got {rose['plant']['healthStatus']}"
    assert rose["healthySince"] == 0, f"Expected a 0 healhySince but got {rose['healthySince']}"
