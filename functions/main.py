"""
Cloud Functions for Firebase: Plant Health updating & Notifications sending

This module:
- Update each plant health status every hours like it is done in the app's PlantHealthCalculator.
- Sends push notifications via FCM when plants go to NEED_WATER or SEVERLY_DRY state.
"""

from enum import Enum
from firebase_functions import scheduler_fn
from firebase_functions import https_fn
from firebase_admin import initialize_app, firestore, messaging
from datetime import datetime, timezone, timedelta
from google.cloud.firestore_v1.base_document import DocumentSnapshot
import functools, random, logging, time

initialize_app()
# Initialize the database globally, but with None, so it don't run on import
db = None


# Notification Text Catalogs
notifications_title_list_need_water = [
    "Time to give your plant a drink 🌱",
    "Your plant is feeling a bit thirsty 🌿",
    "Hey, your green friend needs some water 🌱",
    "Don't forget to water your plant today 🌿",
    "A little hydration goes a long way 🌱",
    "Your plant could use a refreshing sip 🌿",
    "It's watering time for your plant 🌱",
    "Your plant's leaves are calling for water 🌿",
    "Keep your plant happy — water it now 🌱",
    "Looks like your plant needs a bit of care 🌿"
]

notifications_title_list_critically_dry = [
    "Your plant is really thirsty ⚠️",
    "Emergency hydration needed 🚨",
    "Your plant is drying out fast ⚠️",
    "Uh oh...your plant needs water ASAP 🚨",
]


# Analogous to the app's PlantHealthStatus enum
class PlantHealthStatus(str, Enum):
    """Enum describing discrete plant health states."""
    SEVERELY_OVERWATERED = "SEVERELY_OVERWATERED"
    OVERWATERED = "OVERWATERED"
    HEALTHY = "HEALTHY"
    SLIGHTLY_DRY = "SLIGHTLY_DRY"
    NEEDS_WATER = "NEEDS_WATER"
    SEVERELY_DRY = "SEVERELY_DRY"
    UNKNOWN = "UNKNOWN"


# Same threshold values to compute health status as in the app's logic
SEVERELY_OVERWATERED_MAX_THRESHOLD = 30.0
OVERWATERED_MAX_THRESHOLD = 70.0
HEALTHY_MAX_THRESHOLD = 70.0
SLIGHTLY_DRY_MAX_THRESHOLD = 100.0
NEEDS_WATER_MAX_THRESHOLD = 130.0

OVERWATER_STATE_RECOVERY_END_THRESHOLD = 30.0
OVERWATERING_SEVERITY_LEVEL_THRESHOLD = 0.5


# Notification error sending handling
MAX_RETRY_ATTEMPTS = 3
BACKOFF_SECONDS = 1

# Firestore Client
@functools.lru_cache(maxsize=1)
def get_firestore_client():
    """
    Initialize and return a cached Firestore client.

    Returns:
        google.cloud.firestore.Client: The Firestore client instance
    """
    global db
    if db is None:
        db = firestore.client()
    return db


# Notification Utilities
def _get_user_token(uid: str) -> str | None:
    """
    Fetch the FCM token stored in the given user's Firestore document.

    Args:
        uid (str): The user's unique identifier

    Returns:
        str | None: The FCM token if present and valid, else None
    """
    db = get_firestore_client()
    doc = db.collection("users").document(uid).get()
    if not doc.exists:
        return None
    data = doc.to_dict() or {}
    token = data.get("fcmToken") or None
    return token if isinstance(token, str) else None

def _send_friend_request_notification(target_uid: str, from_pseudo: str) -> bool:
    """
    Send a push notification to the user with UID `target_uid`
    telling them that `from_pseudo` sent a friend request.

    Args:
        target_uid (str): The user receiving the friend request
        from_pseudo (str): The pseudo of the user who sent the request

    Returns:
        bool: True if notification sent successfully, False otherwise
    """
    token = _get_user_token(target_uid)
    if token is None:
        logging.info(f"No valid FCM token found for user {target_uid}. No friend request notification sent.")
        return False

    message = messaging.Message(
        token=token,
        notification=messaging.Notification(
            title="New Friend Request 🤝",
            body=f"{from_pseudo} wants to be your friend!"
        ),
        data={
            "type": "FRIEND_REQUEST",
            "fromPseudo": from_pseudo,
        }
    )

    for attempt in range(1, MAX_RETRY_ATTEMPTS + 1):
        try:
            response = messaging.send(message)
            logging.info(
                f"Friend request notification sent to {target_uid} "
                f"on attempt {attempt} | response={response}"
            )
            return True

        except messaging.UnregisteredError as e:
            logging.warning(f"Unregistered token for user {target_uid} | {e}")
            db.collection("users").document(target_uid).update({"fcmToken": firestore.DELETE_FIELD})
            return False

        except (messaging.QuotaExceededError, messaging.InternalError) as e:
            if attempt < MAX_RETRY_ATTEMPTS:
                logging.warning(
                    f"Failed to send friend request notification to {target_uid}: {e} "
                    f"| retry {attempt}/{MAX_RETRY_ATTEMPTS}"
                )
                backoff_time = BACKOFF_SECONDS * (2 ** (attempt - 1))
                time.sleep(min(backoff_time, 8))
            else:
                logging.exception(
                    f"Failed to send friend request notification to user {target_uid} "
                    f"after {MAX_RETRY_ATTEMPTS} attempts | {e}"
                )
                return False

        except Exception as e:
            logging.exception(f"Error sending friend request notification to {target_uid} | {e}")
            return False


@https_fn.on_call()
def send_friend_request_notification(req: https_fn.CallableRequest):
    """
    Firebase Callable Function that the Android app calls.
    It triggers sending a friend request push notification.
    """
    data = req.data
    target_uid = data.get("targetUid")
    from_pseudo = data.get("fromPseudo")

    if not target_uid or not from_pseudo:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            message="Missing targetUid or fromPseudo"
        )

    success = _send_friend_request_notification(target_uid, from_pseudo)
    return { "success": success }


def _send_water_notification(uid: str, plant_id: str, plant_name: str, new_status: PlantHealthStatus) -> bool:
    """
    Send an FCM notification to the user for a specific plant, based on the newly computed status.

    Args:
        uid (str): User ID who owns the plant
        plant_id (str): Identifier of the plant (stored in data payload)
        plant_name (str): Human-readable plant name for the message body
        new_status (PlantHealthStatus): The new computed status prompting the notification

    Returns:
        bool: True if the message was successfully sent, False otherwise
    """
    token = _get_user_token(uid)
    if token is None:
        logging.info(f"No valid FCM token found for user {uid}. No notification sent.")
        return False

    # Choose a random title depending on the new status of the plant
    titles = (
        notifications_title_list_need_water
        if new_status == PlantHealthStatus.NEEDS_WATER
        else notifications_title_list_critically_dry
    )
    title = random.choice(titles)

    # Choose a body corresponding to the new status of the plant
    body = (
        f"{plant_name} needs water!"
        if new_status == PlantHealthStatus.NEEDS_WATER
        else f"{plant_name} is severely dry and needs immediate watering to recover!"
    )

    message = messaging.Message(
        token=token,
        notification=messaging.Notification(title=title, body=body),
        data={
            "type": "WATER_PLANT",
            "plantId": plant_id,
        }
    )

    # Try to send the notification a maximum of MAX_RETRY_ATTEMPTS times
    for attempt in range(1, MAX_RETRY_ATTEMPTS + 1):
        # Try to send the notification to the given token
        try:
            response = messaging.send(message)
            logging.info(f"Valid notification was sent to user {uid} on attempt {attempt} | response={response}")
            return True

        # If an UnregisteredError happens, the token is not valid -> delete from Firestore
        except messaging.UnregisteredError as e:
            logging.warning(f"Token for user {uid} is no longer valid | error = {e}")
            db.collection("users").document(uid).update({"fcmToken": firestore.DELETE_FIELD})
            return False
        
        # Only try again if the execption recieved is QuotaExceededError or InternalError
        except (messaging.QuotaExceededError, messaging.InternalError) as e:
            if attempt < MAX_RETRY_ATTEMPTS:
                logging.warning(f"Failed to send notification to user {uid} | error = {e}")
                logging.warning(f"Try again: current attempt: {attempt}/{MAX_RETRY_ATTEMPTS}.")

                # Try 1, 2 and 4 seconds later to let the system time to adapt (max 8s)
                backoff_time = BACKOFF_SECONDS * (2 ** (attempt - 1))
                time.sleep(min(backoff_time, 8))
            else:
                logging.exception(f"Failed to send notification to user {uid} after {MAX_RETRY_ATTEMPTS} attempts | error = {e}")
                return False

        except Exception as e:
            logging.exception(f"Failed to send notification to user {uid} | error = {e}")
            return False


# Scheduled Job
@scheduler_fn.on_schedule(schedule="every 60 minutes")
def update_all_plants_status(_event: scheduler_fn.ScheduledEvent):
    """
    Scheduled task that iterates over all users and their plants, recomputes
    health status, updates Firestore if there is a change, and sends FCM
    notifications when a plant transition to a NEED_WATER or SEVERLY_DRY status.
    """
    # Wrap the function in a scheduled one for testing
    _update_all_plants_status()


def _update_all_plants_status():
    """Unscheduled function called by update_all_plants_status()."""
    db_client = get_firestore_client()

    users_ref = db_client.collection("users")
    users = users_ref.stream()

    now = datetime.now(timezone.utc)
    now_millisec = int(now.timestamp() * 1000)

    # Update plant status for each plant for each user
    for user_snap in users:
        uid = user_snap.id
        plants_ref = users_ref.document(uid).collection("plants")

        for plant_snap in plants_ref.stream():
            # Wrap the update in a try/catch to avoid crashing in any way
            try:
                doc = plant_snap.to_dict()

                # Skip if the plant document can't be fetched
                if not doc:
                    continue

                last_watered = doc.get("lastWatered")
                prev_last_watered = doc.get("previousLastWatered")
                plant_map = doc.get("plant")

                # Skip if the document contains invalid fields
                if plant_map is None or last_watered is None:
                    continue

                watering_frequency_days = plant_map.get("wateringFrequency")
                old_status = plant_map.get("healthStatus")

                if watering_frequency_days is None or old_status is None:
                    continue

                try:
                    old_status_enum = PlantHealthStatus(old_status)
                except ValueError:
                    old_status_enum = PlantHealthStatus.UNKNOWN

                new_status = compute_status(last_watered, watering_frequency_days, prev_last_watered)

                # Achievements logic: handle the healthy streak
                healthy_streak_ref = users_ref.document(uid).collection("achievements").document("HEALTHY_STREAK")
                healthy_streak_doc = healthy_streak_ref.get()

                # Skip the achievements updates if it can't be fetched
                if healthy_streak_doc.exists:
                    # Update the streak value
                    data = healthy_streak_doc.to_dict()
                    max_streak = data.get("value")

                    healthy_since = doc.get("healthySince")
                    if (healthy_since != 0):
                        # Check wether the current streak is higher than the previous one
                        current_streak = int(_days(_dt(healthy_since), now))

                        if (current_streak > max_streak):
                            healthy_streak_ref.update({
                                "value": current_streak
                            })

                # If the new health status is different then updates it
                if old_status_enum != new_status:
                    plants_ref.document(plant_snap.id).update({
                        "plant.healthStatus": new_status.value
                    })

                    # Achievements logic: update the healthySince field of the plant
                    was_healthy = old_status_enum in (PlantHealthStatus.HEALTHY, PlantHealthStatus.SLIGHTLY_DRY)
                    is_now_healthy = new_status in (PlantHealthStatus.HEALTHY, PlantHealthStatus.SLIGHTLY_DRY)

                    if (not was_healthy and is_now_healthy):
                        plants_ref.document(plant_snap.id).update({
                            "healthySince": now_millisec
                        })
                    
                    elif(not is_now_healthy and was_healthy):
                        plants_ref.document(plant_snap.id).update({
                            "healthySince": 0
                        })


                    # If the plant needs water or goes critically dry, send notification
                    if new_status == PlantHealthStatus.NEEDS_WATER or new_status == PlantHealthStatus.SEVERELY_DRY:
                        # Handle exceptions directly in the _send_water_notification function
                        plant_id = doc.get("id")
                        plant_name = plant_map.get("name")
                        if plant_id is None or plant_name is None:
                            continue
                        _send_water_notification(uid, plant_id, plant_name, new_status)

            except Exception as e:
                logging.exception(f"[update_all_plants_status] uid={uid} | plant={plant_snap.id} | error={e}")

# Date/Time Helpers
def _dt(x: int | float):
    """
    Convert stored Firestore millisecond (java.sql) Timestamps to a 
    python-friendly UTC-aware datetime.

    Args:
        x (int | float): Firestore 'number' that store a Timestamp

    Returns:
        datetime | None: UTC-aware datetime if input is not None, otherwise None
    """
    if x is None:
        return None
    seconds = x / 1000.0
    return datetime.fromtimestamp(seconds, timezone.utc)

def _days(a: datetime, b: datetime) -> float:
    """
    Compute the difference in days between two datetimes.

    Args:
        a (datetime): Start datetime
        b (datetime): End datetime

    Returns:
        float: The difference in days
    """
    seconds_in_day = float(60 * 60 * 24)
    return (b - a).total_seconds() / seconds_in_day

# Helper function for the status computation
def _relative_percentage(x: float, y: float, z: float) -> float:
    """Same as the in-app function calculateRelativePercentage(): normalize z in the interval [x,y] to [0,1]."""
    if y == x:
        return 0.0
    z_clamped = max(x, min(y, z))
    return (z_clamped - x) / (y - x)

# Health Status Computation
def compute_status(
    last_watered: int,
    watering_frequency_days: int,
    previous_last_watered: int = None
) -> PlantHealthStatus:
    """
    Compute plant health status following the modified app model:
    - drynessPct = days since last watering / wateringFrequency * 100
    - Overwatering is computed from the intervalPct between previousLastWatered and 
      lastWatered (if previous exists) then mapped to [0,1]
    - Overwatering decays linearly as drynessPct increases and disappears by
      the given threshold OVERWATER_STATE_RECOVERY_END_THRESHOLD
    - While the effective overwatering is > 0, status is OVERWATERED or SEVERELY_OVERWATERED.
      Once it reaches 0, we fall back to the dryness ladder.
    """
    last = _dt(last_watered)
    prev = _dt(previous_last_watered) if previous_last_watered is not None else None
    now = datetime.now(timezone.utc)

    if watering_frequency_days <= 0 or last is None:
        return PlantHealthStatus.UNKNOWN

    # Dryness computation
    days_since = _days(last, now)
    dryness_pct = (days_since / watering_frequency_days) * 100.0

    # Overwatering computation
    interval_pct = None
    if prev is not None:
        days_between = _days(prev, last)
        interval_pct = (days_between / watering_frequency_days) * 100.0

    # Severity mapped in [0,1]
    if interval_pct is None:
        starting_overwater_severity = 0.0
    else:
        if interval_pct < SEVERELY_OVERWATERED_MAX_THRESHOLD:
            starting_overwater_severity = 1.0
        elif interval_pct < OVERWATERED_MAX_THRESHOLD:
            starting_overwater_severity = 1.0 - _relative_percentage(
                SEVERELY_OVERWATERED_MAX_THRESHOLD,
                OVERWATERED_MAX_THRESHOLD,
                interval_pct
            )
        else:
            starting_overwater_severity = 0.0

    # Decay factor in [0,1] (becomes 0 at the recoveryEnd threshold)
    overwater_decay = max(
        0.0,
        min(1.0, 1.0 - (dryness_pct / OVERWATER_STATE_RECOVERY_END_THRESHOLD))
    )

    effective_overwater_severity = starting_overwater_severity * overwater_decay

    # If still overwatered gets an overwatered status
    if effective_overwater_severity > 0.0:
        if effective_overwater_severity > OVERWATERING_SEVERITY_LEVEL_THRESHOLD:
            return PlantHealthStatus.SEVERELY_OVERWATERED
        return PlantHealthStatus.OVERWATERED

    # Otherwise, follow the dryness ladder (as before)
    if dryness_pct <= HEALTHY_MAX_THRESHOLD:
        return PlantHealthStatus.HEALTHY
    if dryness_pct <= SLIGHTLY_DRY_MAX_THRESHOLD:
        return PlantHealthStatus.SLIGHTLY_DRY
    if dryness_pct <= NEEDS_WATER_MAX_THRESHOLD:
        return PlantHealthStatus.NEEDS_WATER
    return PlantHealthStatus.SEVERELY_DRY

