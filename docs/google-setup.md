# Google Cloud Console Setup Guide

This guide walks you through setting up Google OAuth 2.0 credentials for **Application Tracker**, enabling the application to read job application emails via Gmail API while requesting the minimum necessary readonly scope.

---

## 1. Create a Google Cloud Project

1. Navigate to the [Google Cloud Console](https://console.cloud.google.com/).
2. Click the project dropdown at the top of the page and select **New Project**.
3. Name your project (e.g., `application-tracker`) and click **Create**.
4. Once created, make sure the new project is selected from the top dropdown.

---

## 2. Enable the Gmail API

1. In the left navigation menu, go to **APIs & Services** &rarr; **Library**.
2. In the search box, search for **Gmail API**.
3. Select **Gmail API** from the results and click **Enable**.

---

## 3. Configure the OAuth Consent Screen

1. In the left sidebar, navigate to **APIs & Services** &rarr; **OAuth consent screen**.
2. Select **External** as the User Type (unless you have a Google Workspace organization domain) and click **Create**.
3. Under **App information**:
   - **App name**: `Application Tracker`
   - **User support email**: Select your Google email address.
   - **Developer contact information**: Enter your email address.
4. Click **Save and Continue**.

---

## 4. Configure OAuth Scopes

Under the **Scopes** step, click **Add or Remove Scopes** and select:
- `.../auth/userinfo.email` (`email`)
- `openid`
- `https://www.googleapis.com/auth/gmail.readonly` (Read resources from Gmail)

> [!IMPORTANT]
> **Minimal Privileges Principle**: Strictly request only `gmail.readonly`. Do NOT request full access (`https://mail.google.com/`) or send/compose scopes.

Click **Update** and then **Save and Continue**.

---

## 5. Add Test Users & ⚠️ 7-Day Refresh Token Expiration

Under the **Test users** step:
1. Click **+ Add Users** and enter your personal Google email address (the account you will use to log in and test).
2. Click **Save and Continue**.

> [!WARNING]
> ### ⚠️ Critical Notice: 7-Day Expiry for Testing Mode
> - Google enforces that when an OAuth consent screen is in **"Testing"** publishing status, **OAuth refresh tokens expire automatically after 7 days**!
> - When a refresh token expires or is revoked, any attempt to use or refresh it returns an `invalid_grant` error.
> - Application Tracker handles `invalid_grant` by marking the user as `reauth_required: true` and prompting the user to sign in again.
> - To obtain persistent refresh tokens that do not expire after 7 days, you must move the publishing status to **"In production"** in the OAuth Consent Screen dashboard. (Note: Google may require OAuth App Verification for sensitive scopes like `gmail.readonly` when in production).

---

## 6. Create OAuth 2.0 Web Client Credentials

1. In the left sidebar, go to **APIs & Services** &rarr; **Credentials**.
2. Click **+ Create Credentials** at the top and choose **OAuth client ID**.
3. In the **Application type** dropdown, select **Web application**.
4. Set **Name** to: `Application Tracker Local`.
5. Under **Authorized redirect URIs**, click **+ Add URI** and enter:
   ```text
   http://localhost:8080/login/oauth2/code/google
   ```
6. Click **Create**.
7. A dialog will display your **Client ID** and **Client Secret**. Copy these values securely.

---

## 7. Configure Environment Variables

Copy `.env.example` to `.env` (if not already done) and set your credentials:

```bash
# Google OAuth2 Credentials
GOOGLE_CLIENT_ID=your-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-client-secret
GOOGLE_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google

# AES-256-GCM Token Encryption Key (32-byte hex or base64 or secure passphrase)
APP_ENCRYPTION_KEY=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
```

> [!CAUTION]
> NEVER commit your `.env` file or credentials to Git. `.gitignore` is configured to prevent committing secret and token files.

---

## 8. Offline Access & Refresh Token Behavior

In Spring Security, the application is configured to append:
- `access_type=offline`
- `prompt=consent`

to the Google OAuth authorization URL. This forces Google to provide an **offline refresh token** on successful login, which Application Tracker encrypts with AES-256-GCM before storing in the database.

---

## 9. Handling `invalid_grant` & Expired Tokens

When an access token needs refreshing, `GoogleTokenService` exchanges the stored decrypted refresh token with Google's token endpoint (`https://oauth2.googleapis.com/token`).

If Google responds with `invalid_grant` (due to the 7-day testing limit, token revocation, or password change):
1. The user's database record is automatically updated with `reauth_required = true`.
2. A `ReauthRequiredException` is raised, returning HTTP `401 Unauthorized` with:
   ```json
   {
     "error": "reauth_required",
     "message": "Google authorization has expired or was revoked. Please log in again.",
     "authUrl": "/oauth2/authorization/google"
   }
   ```
3. The frontend displays a re-authentication banner directing the user to `/oauth2/authorization/google` to establish a fresh session and receive a new refresh token.
