# HTTPS and production checklist

PWAs served over HTTPS cannot call an `http://` API (mixed content), and Play Store builds should not allow cleartext traffic.

1. Get a domain (e.g. `sacco.example.co.ke`) pointing at the server and put a TLS proxy in front of the backend (Caddy example):

   ```
   sacco.example.co.ke {
       handle /api/* { reverse_proxy 127.0.0.1:9090 }
       handle /ws/*  { reverse_proxy 127.0.0.1:9090 }
       handle        { root * C:/minetsacco-deploy/frontend/dist
                       try_files {path} /index.html
                       file_server }
   }
   ```
   Caddy obtains and renews the certificate automatically.
2. In `minetsacco-main/.env.production` set
   `VITE_API_URL=https://sacco.example.co.ke` and `VITE_NATIVE_BACKEND_URL=https://sacco.example.co.ke`, then rebuild (`npm run build && npx cap sync android`).
3. In `android/app/src/main/AndroidManifest.xml` set `android:usesCleartextTraffic="false"`, and in `capacitor.config.ts` set `allowMixedContent: false` and restrict `allowNavigation` to your domain.
4. Backend environment variables (override the defaults in `application-prod.properties`):
   `DB_PASSWORD`, `JWT_SECRET` (32+ random chars), `JASYPT_PASSWORD`, `MPESA_CONSUMER_KEY`, `MPESA_CONSUMER_SECRET`, `MPESA_PASSKEY`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`.
   Also update `app.frontend.url`, `mpesa.callback-url` and `mpesa.timeout-url` to the https domain, and set `mpesa.environment=production` only with live credentials.
5. Android signing: copy `android/app/keystore.properties.example` to `keystore.properties` (git-ignored) and fill in the real values, or set `KEYSTORE_PASSWORD` / `KEY_PASSWORD` environment variables.
