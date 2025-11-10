from enum import Enum
from firebase_functions import scheduler_fn
from firebase_admin import initialize_app, firestore
from datetime import datetime, timezone
import functools

# Initialize these globally, but with None, so they don't run on import
app = None
db = None

@functools.lru_cache(maxsize=1)
def get_firestore_client():
    """Initializes and returns the Firestore client."""
    global app, db
    if app is None:
        app = initialize_app()
    if db is None:
        db = firestore.client()
    return db

# Re-create the plant health status computing
class PlantHealthStatus(str, Enum):
    SEVERELY_OVERWATERED = "SEVERELY_OVERWATERED"
    OVERWATERED = "OVERWATERED"
    HEALTHY = "HEALTHY"
    SLIGHTLY_DRY = "SLIGHTLY_DRY"
    NEEDS_WATER = "NEEDS_WATER"
    SEVERELY_DRY = "SEVERELY_DRY"
    UNKNOWN = "UNKNOWN"

# same thresholds
SEV_OVER = 10.0
OVER = 30.0
HEALTHY_MAX = 70.0
SLIGHT_DRY_MAX = 100.0
NEEDS_WATER_MAX = 130.0
GRACE_DAYS = 0.5

def _dt(x: int | float):
    """Convert stored values (Firestore number) to datetime (UTC)"""
    if x is None:
        return None
    seconds = x / 1000.0
    return datetime.fromtimestamp(seconds, timezone.utc)
    

def _days(a: datetime, b: datetime) -> float:
    """Homologous to calculateDaysDifference() in the MyGarden app"""
    seconds_in_day = float(60 * 60 * 24)
    return (b - a).total_seconds() / seconds_in_day

def compute_status(
    last_watered: int,
    watering_frequency_days: int,
    previous_last_watered: int=None
) -> PlantHealthStatus:
    """Compact version of MyGarden's 'calculateHealthStatus()'"""
    last = _dt(last_watered)
    prev = _dt(previous_last_watered) if previous_last_watered is not None else previous_last_watered
    now = datetime.now(timezone.utc)

    if watering_frequency_days <= 0 or last is None:
        return PlantHealthStatus.UNKNOWN

    pct = _days(last, now) / watering_frequency_days * 100.0

    # Initial-watering grace: first watering very recent -> HEALTHY
    if _days(last, now) <= GRACE_DAYS and prev is None:
        return PlantHealthStatus.HEALTHY

    # Grace after appropriate watering: if previous cycle had reached ≥70%
    if _days(last, now) <= GRACE_DAYS and prev is not None:
        prev_pct = _days(prev, last) / watering_frequency_days * 100.0
        if prev_pct >= HEALTHY_MAX:
            return PlantHealthStatus.HEALTHY

    if pct < SEV_OVER:
        return PlantHealthStatus.SEVERELY_OVERWATERED
    if pct < OVER:
        return PlantHealthStatus.OVERWATERED
    if pct <= HEALTHY_MAX:
        return PlantHealthStatus.HEALTHY
    if pct <= SLIGHT_DRY_MAX:
        return PlantHealthStatus.SLIGHTLY_DRY
    if pct <= NEEDS_WATER_MAX:
        return PlantHealthStatus.NEEDS_WATER
    return PlantHealthStatus.SEVERELY_DRY

# Scheduled job over all users that update plants status
@scheduler_fn.on_schedule(schedule="every 60 minutes")
def update_all_plants_status(_event: scheduler_fn.ScheduledEvent):

    db_client = get_firestore_client()

    users_ref = db_client.collection("users")
    users = users_ref.stream()

    # update plant status for each plant for each user
    for user_snap in users:
        uid = user_snap.id
        plants_ref = users_ref.document(uid).collection("plants")

        for plant_snap in plants_ref.stream():
            # wrap the update in a try/catch to avoid crashing in any way
            try:
                doc = plant_snap.to_dict() or {}

                last_watered = doc.get("lastWatered")          
                prev_last_watered = doc.get("previousLastWatered")
                plant_map = doc.get("plant", {}) or {}
                watering_frequency_days = plant_map.get("wateringFrequency")
                old_status = plant_map.get("healthStatus")

                if watering_frequency_days is None or last_watered is None or old_status is None or not plant_map:
                    continue

                try:
                    old_status_enum = PlantHealthStatus[old_status]
                except ValueError:
                    old_status_enum = PlantHealthStatus.UNKNOWN

                new_status = compute_status(last_watered, watering_frequency_days, prev_last_watered)

                # if the new health status is different then updates it 
                if old_status_enum != new_status:
                    plants_ref.document(plant_snap.id).update({
                        "plant.healthStatus": new_status.value
                    })
            except Exception as e:
                print(f"[update_all_plants_status] uid={uid} plant={plant_snap.id} error={e}")
