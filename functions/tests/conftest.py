import os
import pytest
import firebase_admin
from unittest.mock import Mock, MagicMock, patch
from collections import defaultdict

# Config for testing
os.environ.setdefault("GOOGLE_CLOUD_PROJECT", "demo-test")
os.environ.setdefault("GCLOUD_PROJECT", "demo-test")

APP_NAME = 'pytest-app'
PROJECT_ID = os.environ["GOOGLE_CLOUD_PROJECT"]


# Mock Firestore implementation by hand
class MockFirestoreDocument:
    """Mock a Firestore document."""
    def __init__(self, doc_id, data, parent_ref):
        self.id = doc_id
        self._data = data
        self.reference = Mock()
        self.reference.delete = Mock()
        self.reference.update = Mock()
        self.reference.collection = lambda name: parent_ref.document(doc_id).collection(name)
        self._parent_ref = parent_ref
    
    def to_dict(self):
        return self._data.copy() if self._data else {}
    
    def get(self):
        """For document.get() calls"""
        return self
    
    @property
    def exists(self):
        return self._data is not None


class MockFirestoreCollection:
    """Mock a Firestore collection."""
    def __init__(self, name, parent_path=""):
        self.name = name
        self.parent_path = parent_path
        self._documents = {}
        self._subcollections = defaultdict(lambda: defaultdict(dict))
    
    def document(self, doc_id):
        """Get or create a document reference."""
        doc_ref = Mock()
        
        def _set(data):
            self._documents[doc_id] = data
        
        def _update(updates):
            if doc_id not in self._documents:
                self._documents[doc_id] = {}
            # Handle nested updates like "plant.healthStatus"
            for key, value in updates.items():
                if "." in key:
                    parts = key.split(".")
                    target = self._documents[doc_id]
                    for part in parts[:-1]:
                        if part not in target:
                            target[part] = {}
                        target = target[part]
                    target[parts[-1]] = value
                else:
                    self._documents[doc_id][key] = value
        
        def _get():
            data = self._documents.get(doc_id)
            full_path = f"{self.parent_path}/{self.name}/{doc_id}"
            return MockFirestoreDocument(doc_id, data, self)
        
        def _collection(subcoll_name):
            # Store subcollection under this document
            if doc_id not in self._subcollections:
                self._subcollections[doc_id] = {}
            if subcoll_name not in self._subcollections[doc_id]:
                full_path = f"{self.parent_path}/{self.name}/{doc_id}"
                self._subcollections[doc_id][subcoll_name] = MockFirestoreCollection(subcoll_name, full_path)
            return self._subcollections[doc_id][subcoll_name]
        
        def _delete():
            if doc_id in self._documents:
                del self._documents[doc_id]
        
        doc_ref.set = _set
        doc_ref.update = _update
        doc_ref.get = _get
        doc_ref.collection = _collection
        doc_ref.delete = _delete
        doc_ref.id = doc_id
        
        return doc_ref
    
    def stream(self):
        """Stream all documents in this collection."""
        for doc_id, data in self._documents.items():
            yield MockFirestoreDocument(doc_id, data, self)
    
    def get(self):
        """Get all documents."""
        return list(self.stream())


class MockFirestoreClient:
    """Mock the Firestore client."""
    def __init__(self):
        self._collections = {}
    
    def collection(self, name):
        if name not in self._collections:
            self._collections[name] = MockFirestoreCollection(name)
        return self._collections[name]


@pytest.fixture(scope="function")
def db():
    """Provide a mock Firestore client for testing."""
    mock_db = MockFirestoreClient()
    yield mock_db


# Initialize Firebase Admin once if not already done
try:
    firebase_admin.get_app(APP_NAME)
except ValueError:
    # Initialize with mock credentials for testing
    firebase_admin.initialize_app(
        name=APP_NAME,
        options={"projectId": PROJECT_ID}
    )