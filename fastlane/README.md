# App Store Metadata

Ticklr uses Fastlane only for App Store Connect listing metadata. It is not part
of the build, signing, TestFlight, binary upload, or current CI/CD release flow.

## What Is Managed

Metadata lives under `fastlane/metadata/<locale>/`:

- `name.txt`
- `subtitle.txt`
- `promotional_text.txt`
- `description.txt`
- `keywords.txt`
- `release_notes.txt`
- `privacy_url.txt`
- `support_url.txt`
- `marketing_url.txt`

Screenshots are intentionally not managed here yet.

## Local Setup

Install Fastlane locally:

```sh
bundle install
```

Set App Store Connect API credentials. Keep `.p8` keys out of git.

```sh
export APP_STORE_CONNECT_API_KEY_ID="..."
export APP_STORE_CONNECT_ISSUER_ID="..."
export APP_STORE_CONNECT_API_KEY_CONTENT="$(base64 -i /path/to/AuthKey_XXXX.p8)"
export APP_STORE_CONNECT_API_KEY_IS_KEY_CONTENT_BASE64=true
```

Validate metadata before upload:

```sh
python3 scripts/validate-app-store-metadata.py
```

Upload metadata only:

```sh
bundle exec fastlane ios store_metadata
```

## Locale Notes

The metadata folders use App Store Connect locale tags, not Android resource or
Play Console tags. For example, Android `values-he` / Play `iw-IL` maps to App
Store `he`, and Play `zh-CN` maps to App Store `zh-Hans`.

Ticklr ships an Urdu in-app localization, but this metadata setup does not add a
`ur` App Store listing folder because App Store Connect metadata locale support
does not currently match that app locale. It will fall back to the primary store
listing unless Apple adds support or we choose another store locale strategy.
