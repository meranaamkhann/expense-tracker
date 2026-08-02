# Replace your current frontend

This folder is a complete replacement for the repository's `frontend/` directory.

1. Extract the ZIP somewhere temporary.
2. In your repository, rename the current `frontend` directory to `frontend-backup`.
3. Copy this extracted `expense-tracker-frontend` folder into the repository and rename it to `frontend`.
4. In `frontend`, run `npm install` and then `npm run dev`.

The existing Spring Boot backend does not implement authentication, profiles, categories, or persistent data. This frontend therefore works as a polished browser-local demo; all data is held in local storage. It intentionally makes no requests to the incomplete API.
