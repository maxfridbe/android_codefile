# Data safety worksheet — review before submission

This is a code-based inventory, not a pre-submitted Play declaration.

| Data/flow | Local behavior | Off-device behavior |
|---|---|---|
| Opened code/text, formatted previews, annotations | Session memory; cache for exports | Only explicit Android sharing/Save or explicit AI submission |
| Snippets and prompts | Previewed before Send | Sent to the selected API provider for optional analysis; may contain user-provided personal/confidential data |
| API credentials | Endpoint-specific encrypted app-private storage | Sent as authorization to that endpoint |
| API response | Session UI | Provider already handles it; user can share it |
| Diagnostics | Bounded private logs, including device/OS information | Only explicit Support log sharing |
| GitHub file you open by link | Session memory only | The link path and IP address go to GitHub (raw.githubusercontent.com) when the user opens the link |
| Settings | Private preferences | No automatic upload/backup |
| Analytics, ads, account identifiers | Not implemented | No analytics/ad collection |

The developer does not operate a collection backend in this repository. Optional API providers process submissions under their own terms. Determine whether each flow meets Play's user-initiated transfer/collection exceptions; do not infer that all providers collect nothing. Depending on final configuration, Files and docs / other user-generated content / app interactions or diagnostic data may need disclosure. Local-network HTTP is deliberately supported, so an unconditional 'all data encrypted in transit' answer would be inaccurate.

Local data can be cleared using Android app storage controls; credentials can also be removed by saving an empty key. No CodeFile account exists to delete. Requests concerning retained provider data must go to that provider. Supply the public policy URL and support contact before completing the form.
