import sys
import types
from datetime import datetime, timezone
from unittest.mock import MagicMock

import pytest


def _install_firebase_stubs():
    # Generic used to satisfy typing like "Event[Change[DocumentSnapshot]]"
    class _Generic:
        @classmethod
        def __class_getitem__(cls, item):
            return cls

    # firebase_functions.firestore_fn.on_document_written decorator -> no-op decorator
    firestore_fn = types.SimpleNamespace(
        on_document_written=lambda **kwargs: (lambda fn: fn),
        Event=_Generic,
        Change=_Generic,
        DocumentSnapshot=_Generic,
    )
    firebase_functions = types.SimpleNamespace(firestore_fn=firestore_fn)
    sys.modules.setdefault("firebase_functions", firebase_functions)

    # firebase_admin.firestore is imported but not used directly in this file
    firebase_admin = types.SimpleNamespace(firestore=types.SimpleNamespace())
    sys.modules.setdefault("firebase_admin", firebase_admin)


_install_firebase_stubs()

import achievement_activities as aa


class FakeSnap:
    def __init__(self, data=None, exists=True):
        self._data = data or {}
        self.exists = exists

    def get(self, key):
        return self._data.get(key)

    def to_dict(self):
        return dict(self._data)


class FakeChange:
    def __init__(self, before, after):
        self.before = before
        self.after = after


class FakeEvent:
    def __init__(self, user_id=None, achievement_type=None, before=None, after=None):
        self.params = {}
        if user_id is not None:
            self.params["userId"] = user_id
        if achievement_type is not None:
            self.params["achievementType"] = achievement_type
        self.data = FakeChange(before=before, after=after)


def _make_db_with_user_pseudo(pseudo: str):
    """Return a db mock whose users/{uid}.get() returns the pseudo."""
    db = MagicMock(name="db")

    users_col = db.collection.return_value
    user_doc = users_col.document.return_value
    user_doc.get.return_value = FakeSnap({"pseudo": pseudo}, exists=True)

    # activities chain
    activities_col = user_doc.collection.return_value
    activity_doc = activities_col.document.return_value
    activity_doc.set = MagicMock(name="set")

    return db, activity_doc


def test_compute_level_boundaries():
    thresholds = [1, 3, 5]
    assert aa.compute_level(0, thresholds) == 1
    assert aa.compute_level(1, thresholds) == 2
    assert aa.compute_level(2, thresholds) == 2
    assert aa.compute_level(3, thresholds) == 3
    assert aa.compute_level(4, thresholds) == 3
    # At/above last threshold -> max level constant
    assert aa.compute_level(5, thresholds) == aa.ACHIEVEMENTS_LEVEL_NUMBER


def test_get_user_pseudo_missing_user_returns_none():
    db = MagicMock()
    snap = FakeSnap({}, exists=False)
    db.collection.return_value.document.return_value.get.return_value = snap
    assert aa._get_user_pseudo(db, "uid") is None


@pytest.mark.parametrize(
    "stored,expected",
    [
        ({"pseudo": None}, None),
        ({"pseudo": ""}, None),
        ({"pseudo": "   "}, None),
        ({"pseudo": 123}, None),
        ({"pseudo": "Adrien"}, "Adrien"),
    ],
)
def test_get_user_pseudo_validation(stored, expected):
    db = MagicMock()
    db.collection.return_value.document.return_value.get.return_value = FakeSnap(stored, exists=True)
    assert aa._get_user_pseudo(db, "uid") == expected


def test_activity_doc_id_is_deterministic():
    assert aa._activity_doc_id("PLANTS_NUMBER", 3) == "ACHIEVEMENT_PLANTS_NUMBER_LEVEL_3"
    assert aa._activity_doc_id("HEALTHY_STREAK", 10) == "ACHIEVEMENT_HEALTHY_STREAK_LEVEL_10"


def test_trigger_creates_activity_when_new_level_reached(monkeypatch):
    # before_value 2 -> level 2, after_value 3 -> level 3 for PLANTS_NUMBER thresholds [1,3,5...]
    before = FakeSnap({"value": 2}, exists=True)
    after = FakeSnap({"value": 3}, exists=True)
    event = FakeEvent(user_id="u1", achievement_type="PLANTS_NUMBER", before=before, after=after)

    db, activity_doc = _make_db_with_user_pseudo("MyPseudo")
    monkeypatch.setattr(aa.firestore_client, "get_firestore_client", lambda: db)

    fixed_now = datetime(2025, 12, 17, 12, 0, 0, tzinfo=timezone.utc)
    expected_ms = int(fixed_now.timestamp() * 1000)

    class _FixedDateTime:
        @staticmethod
        def now(tz=None):
            assert tz == timezone.utc
            return fixed_now

    # Patch the module's datetime symbol (aa.datetime), not built-in datetime
    monkeypatch.setattr(aa, "datetime", _FixedDateTime)

    aa.on_achievement_progress_written(event)

    # Ensure the activity document used the deterministic id for the *after* level
    db.collection.assert_called_with("users")
    db.collection.return_value.document.assert_called_with("u1")
    db.collection.return_value.document.return_value.collection.assert_called_with("activities")
    db.collection.return_value.document.return_value.collection.return_value.document.assert_called_with(
        "ACHIEVEMENT_PLANTS_NUMBER_LEVEL_3"
    )

    # Ensure we wrote the correct payload
    assert activity_doc.set.call_count == 1
    (payload,) = activity_doc.set.call_args[0]
    kwargs = activity_doc.set.call_args.kwargs

    assert payload["type"] == "ACHIEVEMENT"
    assert payload["userId"] == "u1"
    assert payload["pseudo"] == "MyPseudo"
    assert payload["achievementType"] == "PLANTS_NUMBER"
    assert payload["levelReached"] == 3

    # ✅ createdAt is now epoch milliseconds
    assert payload["createdAt"] == expected_ms
    assert isinstance(payload["createdAt"], int)

    assert kwargs == {"merge": True}


def test_trigger_ignores_unknown_achievement_type(monkeypatch):
    before = FakeSnap({"value": 0}, exists=True)
    after = FakeSnap({"value": 1}, exists=True)
    event = FakeEvent(user_id="u1", achievement_type="NOT_A_REAL_TYPE", before=before, after=after)

    db, activity_doc = _make_db_with_user_pseudo("Pseudo")
    monkeypatch.setattr(aa.firestore_client, "get_firestore_client", lambda: db)

    aa.on_achievement_progress_written(event)
    assert activity_doc.set.call_count == 0


@pytest.mark.parametrize(
    "before_snap,after_snap",
    [
        # deletion
        (FakeSnap({"value": 1}, exists=True), FakeSnap({}, exists=False)),
        # non-increasing
        (FakeSnap({"value": 3}, exists=True), FakeSnap({"value": 3}, exists=True)),
        (FakeSnap({"value": 4}, exists=True), FakeSnap({"value": 2}, exists=True)),
        # missing after value
        (FakeSnap({"value": 1}, exists=True), FakeSnap({}, exists=True)),
        # increase but no level change (e.g., still below next threshold)
        (FakeSnap({"value": 1}, exists=True), FakeSnap({"value": 2}, exists=True)),
    ],
)
def test_trigger_does_not_write_when_no_new_level(monkeypatch, before_snap, after_snap):
    event = FakeEvent(user_id="u1", achievement_type="PLANTS_NUMBER", before=before_snap, after=after_snap)
    db, activity_doc = _make_db_with_user_pseudo("Pseudo")
    monkeypatch.setattr(aa.firestore_client, "get_firestore_client", lambda: db)

    aa.on_achievement_progress_written(event)
    assert activity_doc.set.call_count == 0


def test_trigger_does_not_write_if_pseudo_missing(monkeypatch):
    before = FakeSnap({"value": 2}, exists=True)
    after = FakeSnap({"value": 3}, exists=True)
    event = FakeEvent(user_id="u1", achievement_type="PLANTS_NUMBER", before=before, after=after)

    db = MagicMock(name="db")
    # user doc exists but missing pseudo
    db.collection.return_value.document.return_value.get.return_value = FakeSnap({"pseudo": ""}, exists=True)

    activity_doc = db.collection.return_value.document.return_value.collection.return_value.document.return_value
    activity_doc.set = MagicMock(name="set")

    monkeypatch.setattr(aa.firestore_client, "get_firestore_client", lambda: db)

    aa.on_achievement_progress_written(event)
    assert activity_doc.set.call_count == 0
