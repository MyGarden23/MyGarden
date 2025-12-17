from datetime import datetime, timezone
from models import PlantHealthStatus
from constants import (
    SEVERELY_OVERWATERED_MAX_THRESHOLD,
    OVERWATERED_MAX_THRESHOLD,
    HEALTHY_MAX_THRESHOLD,
    SLIGHTLY_DRY_MAX_THRESHOLD,
    NEEDS_WATER_MAX_THRESHOLD,
    OVERWATER_STATE_RECOVERY_END_THRESHOLD,
    OVERWATERING_SEVERITY_LEVEL_THRESHOLD,
)

def _dt(x: int | float | None):
    """
    Convert stored Firestore millisecond timestamps to a UTC-aware datetime.

    Args:
        x (int | float | None): Milliseconds since epoch.

    Returns:
        datetime | None: UTC-aware datetime if input is not None, otherwise None.
    """
    if x is None:
        return None
    return datetime.fromtimestamp(x / 1000.0, timezone.utc)

def _days(a: datetime, b: datetime) -> float:
    """
    Compute the difference in days between two datetimes.

    Args:
        a (datetime): Start datetime
        b (datetime): End datetime

    Returns:
        float: The difference in days
    """
    return (b - a).total_seconds() / float(60 * 60 * 24)

def _relative_percentage(x: float, y: float, z: float) -> float:
    """Normalize z in the interval [x,y] to [0,1]."""
    if y == x:
        return 0.0
    z_clamped = max(x, min(y, z))
    return (z_clamped - x) / (y - x)

def compute_status(last_watered: int, watering_frequency_days: int, previous_last_watered: int | None = None) -> PlantHealthStatus:
    """
    Compute plant health status following the modified app model:
    - drynessPct = days since last watering / wateringFrequency * 100
    - Overwatering is computed from intervalPct between previousLastWatered and lastWatered (if previous exists)
      then mapped to [0,1]
    - Overwatering decays linearly as drynessPct increases and disappears by OVERWATER_STATE_RECOVERY_END_THRESHOLD
    - While effective overwatering > 0, status is OVERWATERED or SEVERELY_OVERWATERED
      Once it reaches 0, we fall back to the dryness ladder.
    """
    last = _dt(last_watered)
    prev = _dt(previous_last_watered) if previous_last_watered is not None else None
    now = datetime.now(timezone.utc)

    if watering_frequency_days <= 0 or last is None:
        return PlantHealthStatus.UNKNOWN

    days_since = _days(last, now)
    dryness_pct = (days_since / watering_frequency_days) * 100.0

    interval_pct = None
    if prev is not None:
        days_between = _days(prev, last)
        interval_pct = (days_between / watering_frequency_days) * 100.0

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

    overwater_decay = max(
        0.0,
        min(1.0, 1.0 - (dryness_pct / OVERWATER_STATE_RECOVERY_END_THRESHOLD))
    )
    effective_overwater_severity = starting_overwater_severity * overwater_decay

    if effective_overwater_severity > 0.0:
        if effective_overwater_severity > OVERWATERING_SEVERITY_LEVEL_THRESHOLD:
            return PlantHealthStatus.SEVERELY_OVERWATERED
        return PlantHealthStatus.OVERWATERED

    if dryness_pct <= HEALTHY_MAX_THRESHOLD:
        return PlantHealthStatus.HEALTHY
    if dryness_pct <= SLIGHTLY_DRY_MAX_THRESHOLD:
        return PlantHealthStatus.SLIGHTLY_DRY
    if dryness_pct <= NEEDS_WATER_MAX_THRESHOLD:
        return PlantHealthStatus.NEEDS_WATER
    return PlantHealthStatus.SEVERELY_DRY
