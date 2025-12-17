"""
Cloud Functions for Firebase: Plant Health updating & Notifications sending

This module:
- Update each plant health status every hours like it is done in the app's PlantHealthCalculator.
- Sends push notifications via FCM when plants go to NEED_WATER or SEVERLY_DRY state.
- Sends push notifications via FCM when users request eachothers to be friends. 
"""

from firebase_functions import scheduler_fn, https_fn
from firebase_admin import initialize_app
from notifications import send_friend_request_notification_impl
from jobs import update_all_plants_status_impl
# imported on_achievement_progress_written in main.py so that it can be triggered correctly
from achievement_activities import on_achievement_progress_written 

initialize_app()

@https_fn.on_call()
def send_friend_request_notification(req: https_fn.CallableRequest):
    """
    Firebase Callable Function that the Android app calls.
    It triggers sending a friend request push notification.
    """
    data = req.data or {}
    target_uid = data.get("targetUid")
    from_pseudo = data.get("fromPseudo")

    if not target_uid or not from_pseudo:
        raise https_fn.HttpsError(
            code=https_fn.FunctionsErrorCode.INVALID_ARGUMENT,
            message="Missing targetUid or fromPseudo"
        )

    success = send_friend_request_notification_impl(target_uid, from_pseudo)
    return {"success": success}

@scheduler_fn.on_schedule(schedule="every 60 minutes")
def update_all_plants_status(_event: scheduler_fn.ScheduledEvent):
    """
    Scheduled task that iterates over all users and their plants, recomputes
    health status, updates Firestore if there is a change, and sends FCM
    notifications when a plant transitions to a NEEDS_WATER or SEVERELY_DRY status.
    """
    update_all_plants_status_impl()
