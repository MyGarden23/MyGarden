import main
from datetime import datetime, timezone, timedelta

# Helper function to transform datetime into 
# a Firestore number that stored a (java.sql) Timestamp
def ms(dt): return int(dt.timestamp() * 1000)

# Here are several tests that ensure the behaviour is the same as expected
# for a plant next status computation.

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

def test_overwatered_threshold():
    now = datetime.now(timezone.utc)
    last = now - timedelta(days=2)
    watering_freq = 10

    # the plant is at 20% => should be OVERWATERED
    status = main.compute_status(ms(last), watering_freq)
    assert status == main.PlantHealthStatus.OVERWATERED

def test_severely_overwatered_threshold():
    now = datetime.now(timezone.utc)
    last = now - timedelta(days=1)
    watering_freq = 20

    # the plant is at 5% => should be SEVERELY_OVERWATERED
    status = main.compute_status(ms(last), watering_freq)
    assert status == main.PlantHealthStatus.SEVERELY_OVERWATERED

def test_grace_period():
    now = datetime.now(timezone.utc)
    last = now - timedelta(days=0.4)
    prev = last - timedelta(days=12)
    watering_freq = 10

    # this setting should trigger the grace period
    status = main.compute_status(ms(last), watering_freq, ms(prev))
    assert status == main.PlantHealthStatus.HEALTHY

def test_initial_watering():
    now = datetime.now(timezone.utc)
    last = now - timedelta(days=0.4)
    watering_freq = 10

    # this setting should trigger the initial watering
    status = main.compute_status(ms(last), watering_freq)
    assert status == main.PlantHealthStatus.HEALTHY