from enum import Enum

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