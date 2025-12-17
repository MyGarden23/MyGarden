import logging
from datetime import datetime, timezone

import firestore_client
from models import PlantHealthStatus
from plant_health import compute_status, _days, _dt
from notifications import send_water_notification

def update_all_plants_status_impl():
    """
    Unscheduled implementation called by the scheduled Cloud Function.
    Iterates over users/plants, recomputes status, updates Firestore,
    and sends notifications when needed.
    """
    db = firestore_client.get_firestore_client()
    users_ref = db.collection("users")
    users = users_ref.stream()

    now = datetime.now(timezone.utc)
    now_ms = int(now.timestamp() * 1000)

    for user_snap in users:
        uid = user_snap.id
        plants_ref = users_ref.document(uid).collection("plants")

        for plant_snap in plants_ref.stream():
            try:
                doc = plant_snap.to_dict()
                if not doc:
                    continue

                last_watered = doc.get("lastWatered")
                prev_last_watered = doc.get("previousLastWatered")
                plant_map = doc.get("plant")
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

                # achievements logic (same as yours)
                healthy_streak_ref = users_ref.document(uid).collection("achievements").document("HEALTHY_STREAK")
                healthy_streak_doc = healthy_streak_ref.get()
                if healthy_streak_doc.exists:
                    data = healthy_streak_doc.to_dict() or {}
                    max_streak = data.get("value")
                    healthy_since = doc.get("healthySince")
                    if healthy_since != 0:
                        current_streak = int(_days(_dt(healthy_since), now))
                        if max_streak is not None and current_streak > max_streak:
                            healthy_streak_ref.update({"value": current_streak})

                if old_status_enum != new_status:
                    plants_ref.document(plant_snap.id).update({"plant.healthStatus": new_status.value})

                    was_healthy = old_status_enum in (PlantHealthStatus.HEALTHY, PlantHealthStatus.SLIGHTLY_DRY)
                    is_now_healthy = new_status in (PlantHealthStatus.HEALTHY, PlantHealthStatus.SLIGHTLY_DRY)

                    if (not was_healthy and is_now_healthy):
                        plants_ref.document(plant_snap.id).update({"healthySince": now_ms})
                    elif (was_healthy and not is_now_healthy):
                        plants_ref.document(plant_snap.id).update({"healthySince": 0})

                    if new_status in (PlantHealthStatus.NEEDS_WATER, PlantHealthStatus.SEVERELY_DRY):
                        plant_id = doc.get("id")
                        plant_name = (plant_map or {}).get("name")
                        if plant_id and plant_name:
                            send_water_notification(uid, plant_id, plant_name, new_status)

            except Exception as e:
                logging.exception(f"[update_all_plants_status] uid={uid} | plant={plant_snap.id} | error={e}")
