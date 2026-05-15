import os
import json

PUSHER_APP_ID = os.getenv("PUSHER_APP_ID", "")
PUSHER_KEY = os.getenv("PUSHER_KEY", "")
PUSHER_SECRET = os.getenv("PUSHER_SECRET", "")
PUSHER_CLUSTER = os.getenv("PUSHER_CLUSTER", "us2")


def send_push_notification(user_id: int, title: str, body: str):
    if not all([PUSHER_APP_ID, PUSHER_KEY, PUSHER_SECRET]):
        print(f"[Push] User {user_id}: {title} - {body}")
        return

    try:
        import pusher as pusher_client
        p = pusher_client.Pusher(
            app_id=PUSHER_APP_ID,
            key=PUSHER_KEY,
            secret=PUSHER_SECRET,
            cluster=PUSHER_CLUSTER,
        )
        p.trigger(f"user-{user_id}", "notification", {
            "title": title,
            "body": body,
        })
    except Exception as e:
        print(f"Failed to send push notification to user {user_id}: {e}")
