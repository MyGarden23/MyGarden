# MyGarden

### Pitch
Many people enjoy having plants but forget care routines, leading to unhealthy or dead plants. MyGarden solves this by offering simple reminders, schedules, and photo insights using the phone’s camera to track plant health. The app is designed for every person who wants a low-effort way to keep his room plants alive and healthy.

### Split App Model
The app use a managed authentication service for account creation and login, a document database to store plant data and care schedules, a file storage service for users to keep their plants, and a notification service to send reminders and updates.

### Multi-User Support
Multi-user support is enabled through authentication, giving each user a unique account tied to their personal data. Authenticated users can create and manage their own “garden” with plants and care schedules. Moreover they can add friends to view each other’s gardens and earn points for consistent care and plant growth to maybe appear on the leaderboard.

### Sensor Use
The app uses the camera sensor as a core feature: users take photos of their plants to log growth, document health, and detect issues over time. The photos may be stored to keep track of the history of the plant. The sensor is not just for raw capture but directly drives the app’s main value, helping users track and improve plant care.

### Offline Mode
In offline mode, the user still has full access to their garden, including plant profiles, and care schedules. Notifications continue to alert the user of upcoming or overdue care tasks, ensuring the plant care experience remains functional and reliable even without network access.

### Convention used
#### - Creating a branch
feature/ → for new features or enhancements
Example: feature/login-page

bugfix/ → for fixing bugs linked to an issue
Example: bugfix/123-null-pointer

hotfix/ → for urgent fixes in production
Example: hotfix/payment-crash

ui/ → for user interface or styling changes
Example: ui/header-redesign

docs/ → for documentation updates
Example: docs/api-reference

test/ → for test-related work (unit, integration, e2e)
Example: test/add-user-service-tests

chore/ → for maintenance tasks, refactoring, or dependency updates
Example: chore/update-gradle

release/ → for release preparation branches
Example: release/v2.1.0

experiment/ (or spike/) → for exploratory or proof-of-concept work
Example: experiment/graphql-integration

##### Guidelines:

Use lowercase letters and hyphens for readability.

Keep branch names short and descriptive (max ~4 words).

If linked to a ticket/issue, include its ID (e.g., bugfix/456-crash-on-start).

#### - Commmit messages
Following these guidelines:

https://github.com/swent-epfl/public/blob/main/bootcamp/docs/CommitMessages.md#advanced-understanding-conventional-commits

https://www.conventionalcommits.org/en/v1.0.0/

### Figma
Link: https://www.figma.com/design/3iAjAd0sxYwH84R5g7eaNu/MyGarden?node-id=0-1&p=f&t=4tn0hrWLaffFtwsb-0
