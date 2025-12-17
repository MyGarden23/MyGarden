import logging
from typing import Optional, Dict, List

from datetime import datetime, timezone
from firebase_functions import firestore_fn
from firebase_admin import firestore

import firestore_client

# Same as the app's Achievements thresholds
ACHIEVEMENT_THRESHOLDS: Dict[str, List[int]] = {
    "PLANTS_NUMBER":  [1, 3, 5, 10, 15, 20, 30, 40, 50],
    "FRIENDS_NUMBER": [1, 3, 5, 10, 15, 20, 25, 30, 40],
    "HEALTHY_STREAK": [1, 3, 5, 7, 10, 20, 30, 40, 50],
}

ACHIEVEMENTS_LEVEL_NUMBER = 10

def compute_level(value: int, thresholds: List[int]) -> int:
    """
    Computes the achievement level for a given progress value
    similarly to the app's `AchievementDefinition.computeLevel` function.

    Args:
        value: Current numeric progress value for the achievement.
        thresholds: Ordered list of threshold values.

    Returns:
        The computed achievement level.
    """
    for i, t in enumerate(thresholds):
        if value < t:
            return 1 + i
    return ACHIEVEMENTS_LEVEL_NUMBER


def _get_user_pseudo(db, user_id: str) -> Optional[str]:
    """
    Gets the pseudo of a user given their Firebase Auth UID.

    Args:
        db: Firestore client.
        user_id: Firebase Auth UID of the user.

    Returns:
        The user's pseudo if present and valid, otherwise None.
    """
    snap = db.collection("users").document(user_id).get()
    if not snap.exists:
        return None
    data = snap.to_dict() or {}
    pseudo = data.get("pseudo")
    return pseudo if isinstance(pseudo, str) and pseudo.strip() else None


def _activity_doc_id(achievement_type: str, level: int) -> str:
    # Prevents duplicates on retries/reconnects of Firebase
    return f"ACHIEVEMENT_{achievement_type}_LEVEL_{level}"


@firestore_fn.on_document_written(
    document="users/{userId}/achievements/{achievementType}",
    region="europe-west4",
)
def on_achievement_progress_written(
    event: firestore_fn.Event[
        firestore_fn.Change[firestore_fn.DocumentSnapshot]
    ]
):
    """
    Firestore trigger executed whenever an achievement progress document changes.

    This function:
    - Detects forward progress in achievement value.
    - Computes achievement levels before and after the update.
    - Emits a single ActivityAchievement when a new level is reached.

    The activity document ID is deterministic (this differs from other activity types) 
    to prevent having multiple activities for the same achievement because the function 
    could be triggered mutliple times for the same event on the database.

    Args:
        event: Firestore document change event containing `before` and `after`
               snapshots.
    """
    try:
        user_id = event.params.get("userId")
        achievement_type = event.params.get("achievementType")
        if not user_id or not achievement_type:
            return

        thresholds = ACHIEVEMENT_THRESHOLDS.get(achievement_type)

        # This ensures that achievementType is really part of the enum
        if thresholds is None:
            return

        before_snap = event.data.before
        after_snap = event.data.after

        # Ignore deletions
        if after_snap is None or not after_snap.exists:
            return

        before_raw = before_snap.get("value") if (before_snap and before_snap.exists) else 0
        after_raw = after_snap.get("value")

        before_value = before_raw or 0
        after_value = after_raw

        if after_value is None:
            return

        # Should technically only happen for =
        if after_value <= before_value:
            return

        before_level = compute_level(before_value, thresholds)
        after_level = compute_level(after_value, thresholds)

        # Should technically only happen for =
        if after_level <= before_level:
            return

        db = firestore_client.get_firestore_client()
        pseudo = _get_user_pseudo(db, user_id)
        if pseudo is None:
            return

        # Determinist document ID (because of the nature of the function)
        activity_id = _activity_doc_id(achievement_type, after_level)

        activity_ref = (
            db.collection("users")
              .document(user_id)
              .collection("activities")
              .document(activity_id)
        )

        created_time = int(datetime.now(timezone.utc).timestamp() * 1000)

        # Set the document with the right value
        activity_ref.set(
            {
                "type": "ACHIEVEMENT",
                "userId": user_id,
                "pseudo": pseudo,
                "achievementType": achievement_type,
                "levelReached": after_level,
                "createdAt": created_time,
            },
            merge=True,
        )

    except Exception:
        logging.exception("[achievement_activity] failed")
