import functools
from firebase_admin import firestore

@functools.lru_cache(maxsize=1)
def get_firestore_client():
    """ 
    Initialize and return a cached Firestore client.
    Note:
        Firebase Admin must already be initialized (initialize_app()).
        This is done in main.py.

    Returns: 
        google.cloud.firestore.Client: The Firestore client instance 
    """
    return firestore.client()
