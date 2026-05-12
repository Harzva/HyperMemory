# Security Policy

## Supported Branch

Security fixes target `main` until release branches are introduced.

## Reporting

Do not open public issues for secrets, authentication bypasses, data leaks, or prompt injection vulnerabilities. Use a private maintainer channel first.

## Secret Handling

- Never commit `.env`, model keys, Bot tokens, signing secrets, or uploaded private documents.
- Rotate `OPENAI_API_KEY`, `BOT_SIGNING_SECRET`, and platform tokens before production launch.
- Store production secrets in CI secrets, a secret manager, or Kubernetes Secrets.

## Production Controls

- Keep `BOT_ENABLED=false` until provider-native verification and internal HMAC signing are configured.
- Put public traffic behind HTTPS, rate limiting, and request size limits.
- Treat `tenantId` as the future authorization boundary for documents, conversations, and Bot channels.
