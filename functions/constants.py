# In this file are thresholds for healths status computation
# and fixed Notification Text Catalogs

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