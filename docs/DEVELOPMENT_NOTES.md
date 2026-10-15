# Development notes

- JSP pages are stored under `WEB-INF` to prevent direct access.
- Passwords are BCrypt hashes; plaintext passwords are not stored.
- SQL values use prepared statements.
- Role checks occur in the server-side controller, not just the navigation menu.
- Scores are recalculated using the stored answer key on the server.
- Unique database constraints enforce one attempt per student/exam and one answer per question/attempt.

Before production use, add CSRF protection, rate limiting, audit logging, database least-privilege credentials, HTTPS, generic error handling, stronger concurrent submission tests and a secure admin bootstrap process.
