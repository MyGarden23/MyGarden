import logging, random, time
from firebase_admin import messaging, firestore

import firestore_client
from models import PlantHealthStatus
from constants import (
    MAX_RETRY_ATTEMPTS,
    BACKOFF_SECONDS,
    notifications_title_list_need_water,
    notifications_title_list_critically_dry,
)

def _get_user_token(uid: str) -> str | None:
    """
    Fetch the FCM token stored in the given user's Firestore document.

    Args:
        uid (str): The user's unique identifier

    Returns:
        str | None: The FCM token if present and valid, else None
    """
    db = firestore_client.get_firestore_client()
    doc = db.collection("users").document(uid).get()

    if not doc.exists:
        return None
    
    data = doc.to_dict() or {}
    token = data.get("fcmToken") or None

    return token if isinstance(token, str) else None

def send_friend_request_notification_impl(target_uid: str, from_pseudo: str) -> bool:
    """
    Send a push notification to the user with UID `target_uid`
    telling them that `from_pseudo` sent a friend request.

    Returns:
        bool: True if notification sent successfully, False otherwise
    """
    db = firestore_client.get_firestore_client()
    token = _get_user_token(target_uid)
    if token is None:
        logging.info(f"No valid FCM token for user {target_uid}.")
        return False

    message = messaging.Message(
        token=token,
        notification=messaging.Notification(
            title="New Friend Request 🤝",
            body=f"{from_pseudo} wants to be your friend!"
        ),
        data={"type": "FRIEND_REQUEST", "fromPseudo": from_pseudo}
    )

    for attempt in range(1, MAX_RETRY_ATTEMPTS + 1):
        try:
            messaging.send(message)
            return True
        except messaging.UnregisteredError as e:
            logging.warning(f"Unregistered token for user {target_uid} | {e}")
            db.collection("users").document(target_uid).update({"fcmToken": firestore.DELETE_FIELD})
            return False
        except (messaging.QuotaExceededError, messaging.InternalError) as e:
            if attempt < MAX_RETRY_ATTEMPTS:
                time.sleep(min(BACKOFF_SECONDS * (2 ** (attempt - 1)), 8))
            else:
                logging.exception(f"FCM failed after retries | {e}")
                return False
        except Exception as e:
            logging.exception(f"FCM error | {e}")
            return False

def send_water_notification(uid: str, plant_id: str, plant_name: str, new_status: PlantHealthStatus) -> bool:
    """
    Send an FCM notification to the user for a specific plant, based on the newly computed status.

    Returns:
        bool: True if the message was successfully sent, False otherwise
    """
    db = firestore_client.get_firestore_client()
    token = _get_user_token(uid)
    if token is None:
        return False

    titles = notifications_title_list_need_water if new_status == PlantHealthStatus.NEEDS_WATER else notifications_title_list_critically_dry
    title = random.choice(titles)

    body = (
        f"{plant_name} needs water!"
        if new_status == PlantHealthStatus.NEEDS_WATER
        else f"{plant_name} is severely dry and needs immediate watering to recover!"
    )

    message = messaging.Message(
        token=token,
        notification=messaging.Notification(title=title, body=body),
        data={"type": "WATER_PLANT", "plantId": plant_id},
    )

    for attempt in range(1, MAX_RETRY_ATTEMPTS + 1):
        try:
            messaging.send(message)
            return True
        except messaging.UnregisteredError as e:
            db.collection("users").document(uid).update({"fcmToken": firestore.DELETE_FIELD})
            return False
        except (messaging.QuotaExceededError, messaging.InternalError):
            if attempt < MAX_RETRY_ATTEMPTS:
                time.sleep(min(BACKOFF_SECONDS * (2 ** (attempt - 1)), 8))
            else:
                return False
        except Exception:
            return False
