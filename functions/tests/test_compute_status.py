import main
from datetime import datetime, timezone, timedelta

# Helper function to transform datetime into 
# a Firestore number that stored a (java.sql) Timestamp
def ms(dt): return int(dt.timestamp() * 1000)

# Here are small tests that ensure the behaviour is the same as expected
# for a plant in the Kotlin app (up to date with new algorithm).

def test_severely_dry_threshold():
    now = datetime.now(timezone.utc)
    last = now - timedelta(days=20)
    watering_freq = 10

    # the plant is at 200% => should be in SEVERELY_DRY
    status = main.compute_status(ms(last), watering_freq)
    assert status == main.PlantHealthStatus.SEVERELY_DRY

def test_needs_water_threshold():
    now = datetime.now(timezone.utc)
    last = now - timedelta(days=11)
    watering_freq = 10

    # the plant is at 110% => should be in NEEDS_WATER
    status = main.compute_status(ms(last), watering_freq)
    assert status == main.PlantHealthStatus.NEEDS_WATER

def test_slightly_dry_threshold():
    now = datetime.now(timezone.utc)
    last = now - timedelta(days=9)
    watering_freq = 10

    # the plant is at 90% => should be SLIGHTLY_DRY
    status = main.compute_status(ms(last), watering_freq)
    assert status == main.PlantHealthStatus.SLIGHTLY_DRY

def test_healthy_threshold():
    now = datetime.now(timezone.utc)
    last = now - timedelta(days=5)
    watering_freq = 10

    # the plant is at 50% => should be HEALTHY
    status = main.compute_status(ms(last), watering_freq)
    assert status == main.PlantHealthStatus.HEALTHY

def test_no_overwatering_when_no_previous_watering():
    now = datetime.now(timezone.utc)
    # Dryness of 10%
    last = now - timedelta(days=1)
    watering_freq = 10

    # Without previous watering should be just HEALTHY
    status = main.compute_status(ms(last), watering_freq)
    assert status == main.PlantHealthStatus.HEALTHY

def test_severely_overwatered_full_severity():
    now = datetime.now(timezone.utc)
    watering_freq = 10
    
    # Difference between prev and last is 10%
    last = now - timedelta(hours=1)
    prev = last - timedelta(days=1) 
    
    # Difference between last and now is 0.4%  -> Severly overwatered
    status = main.compute_status(ms(last), watering_freq, ms(prev))
    assert status == main.PlantHealthStatus.SEVERELY_OVERWATERED

def test_overwatered_moderate_severity():
    now = datetime.now(timezone.utc)
    watering_freq = 10
    
    # Difference between prev and last is 50% !!
    last = now - timedelta(hours=1)
    prev = last - timedelta(days=5) 
    
    # Difference between last and now is 0.4%  -> Just overwatered
    status = main.compute_status(ms(last), watering_freq, ms(prev))
    assert status == main.PlantHealthStatus.OVERWATERED

def test_overwatering_decay_to_healthy():
    now = datetime.now(timezone.utc)
    watering_freq = 10
    
    # Difference between prev and last is 10%
    last = now - timedelta(days=5)
    prev = last - timedelta(days=1) 
    
    # Now is far enough to have reached the OVERWATER_STATE_RECOVERY_END_THRESHOLD
    # The plant should then be HEALTHY
    status = main.compute_status(ms(last), watering_freq, ms(prev))
    assert status == main.PlantHealthStatus.HEALTHY

def test_initial_watering():
    now = datetime.now(timezone.utc)
    last = now - timedelta(hours=1)
    watering_freq = 10

    status = main.compute_status(ms(last), watering_freq)
    assert status == main.PlantHealthStatus.HEALTHY

def test_invalid_watering_frequency():
    now = datetime.now(timezone.utc)
    last = now - timedelta(days=1)
    watering_freq = 0 # Invalid

    status = main.compute_status(ms(last), watering_freq)
    assert status == main.PlantHealthStatus.UNKNOWN

def test_invalid_last_watered_timestamp():
    watering_freq = 10

    status = main.compute_status(None, watering_freq) # Invalid input
    assert status == main.PlantHealthStatus.UNKNOWN