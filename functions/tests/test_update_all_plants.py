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

    # Plant 1 should be HEALTHY (watered 3 days ago, watering frequency is 7 days)
    plants_col.document("plant_ok").set({
        "id": "plant_ok",
        "lastWatered": ms(TEST_NOW - timedelta(days=3)), 
        "previousLastWatered": 0,
        "plant": {
            "name": "Ficus",
            "wateringFrequency": 7,
            "healthStatus": "HEALTHY", # Should remain HEALTHY after update
        }
    })

    # Plant 2 should transition to SEVERELY_DRY (watered 20 days ago, watering frequency is 5 days)
    plants_col.document("plant_dry").set({
        "id": "plant_dry",
        "lastWatered": ms(TEST_NOW - timedelta(days=20)),
        "previousLastWatered": ms(TEST_NOW - timedelta(days=30)),
        "plant": {
            "name": "Fern",
            "wateringFrequency": 5,
            "healthStatus": "NEEDS_WATER",  # Should transition to SEVERELY_DRY after update
        }
    })

    # Plant 3 should transition to NEEDS_WATER (watered 7 days ago, watering frequency is 7 days)
    plants_col.document("plant_need_water").set({
        "id": "plant_need_water",
        "lastWatered": ms(TEST_NOW - timedelta(days=8)), 
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
    
    import main
    
    # Clear the LRU cache to ensure fresh state
    main.get_firestore_client.cache_clear()
    
    # Mock the get_firestore_client to return our mock db
    with patch('main.get_firestore_client', return_value=db):
        # Also set main.db directly for the helper functions
        main.db = db
        
        # Seed the database
        uid = seed_user_with_plants(db)

        # Run the function
        main._update_all_plants_status()

    # Verifications
    # Verify the dry plant status (SEVERELY_DRY)
    fern = db.collection("users").document(uid).collection("plants").document("plant_dry").get().to_dict()
    assert fern["plant"]["healthStatus"] == "SEVERELY_DRY", f"Expected SEVERELY_DRY but got {fern['plant']['healthStatus']}"

    # Verify the healthy plant status (HEALTHY)
    ficus = db.collection("users").document(uid).collection("plants").document("plant_ok").get().to_dict()
    assert ficus["plant"]["healthStatus"] == "HEALTHY", f"Expected HEALTHY but got {ficus['plant']['healthStatus']}"
    
    # Verify the needs water plant status (NEEDS_WATER)
    rose = db.collection("users").document(uid).collection("plants").document("plant_need_water").get().to_dict()
    assert rose["plant"]["healthStatus"] == "NEEDS_WATER", f"Expected NEEDS_WATER but got {rose['plant']['healthStatus']}"

    # Verify exactly two notifications were sent
    assert mock_send.call_count == 2, f"Expected 2 notifications but got {mock_send.call_count}"
           
    # Verify content of the first sent message (plant_dry -> SEVERELY_DRY)
    sent_msg1 = mock_send.call_args_list[0][0][0]
    
    # Verify token
    assert sent_msg1.token == "fake-token-123", f"Expected token 'fake-token-123' but got {sent_msg1.token}"
           
    # Verify plant ID data
    plant_id1 = sent_msg1.data.get("plantId")
    notification_type1 = sent_msg1.data.get("type")
    assert plant_id1 == "plant_dry", f"Expected plantId 'plant_dry' but got {plant_id1}"
    assert notification_type1 == "WATER_PLANT", f"Expected type 'WATER_PLANT' but got {notification_type1}"

    # Verify content of the second sent message (plant_need_water -> NEEDS_WATER)
    sent_msg2 = mock_send.call_args_list[1][0][0]
    
    # Verify token
    assert sent_msg2.token == "fake-token-123", f"Expected token 'fake-token-123' but got {sent_msg2.token}"
           
    # Verify plant ID data
    plant_id2 = sent_msg2.data.get("plantId")
    notification_type2 = sent_msg2.data.get("type")
    assert plant_id2 == "plant_need_water", f"Expected plantId 'plant_need_water' but got {plant_id2}"
    assert notification_type2 == "WATER_PLANT", f"Expected type 'WATER_PLANT' but got {notification_type2}"