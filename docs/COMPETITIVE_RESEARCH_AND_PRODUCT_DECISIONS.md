# Competitive Research & Product Decisions — VORTIX 1.0.0-rc1

Research date: 2026-11-19. This is a feature and positioning review, not a claim that a single app is objectively the global market leader. Store ratings/review counts can vary by country, account and time.

## Competitor capability map

| Product | Public positioning / strengths observed | User gap or trade-off relevant to VORTIX |
|---|---|---|
| AccuBattery | Charge-session wear, charge-counter-based capacity estimation, foreground-app-associated current estimates and long-term battery tracking. | A user review requested a cleaner “new battery” learning baseline without resetting all historical data. VORTIX adds a new-battery marker while retaining older records. |
| Battery Guru | Live current/temperature, health and charging information, widgets/AOD/floating interface and charge-related tools. | A user review requested separate Wi-Fi/mobile-data graphs and forecasts. VORTIX adds separate mobile and Wi-Fi history charts using Android-provided time buckets, with their granularity disclosed. |
| GSam Battery Monitor | Deep battery-drain diagnostics such as CPU, sensors, wakelocks and awake time; some deeper capabilities require additional ADB-level permissions and vary by Android version. | Per-app attribution can be limited by Android restrictions; VORTIX avoids fabricating per-app mAh and makes the evidence boundary visible. |
| 3C Battery Manager | Dense historical plots for %, mA, mW, mV and temperature, remaining-time estimates, custom graphs, profiles and advanced/root-oriented controls. | VORTIX adopts clear power-flow context and keeps dangerous or privileged controls outside the standard app experience. |
| Ampere | Charger/cable comparison using relative battery current; its own Play description explicitly warns that it is not guaranteed to be mA-accurate and should be compared on the same device. | VORTIX exposes signed current and battery-side derived watts with a clear distinction from USB adapter input power. |
| BatteryBot | Lightweight battery indicator, temperature/voltage/status, widgets, alerts and CSV logging. | VORTIX combines the quick-glance concept with deeper evidence-based history and diagnostics rather than focusing only on the status indicator. |
| Android system tools / OEM tools | Native battery saver, app battery restrictions, vendor charging protections and device-specific diagnostics can use privileges unavailable to third-party apps. | VORTIX links to native settings rather than impersonating system control. OEM-specific capabilities must be separately tested per device family. |

## Store ratings and direct user-feedback signals

Google Play pages were checked on 2026-11-19. Public ratings and counts are snapshots, not stable quality rankings: AccuBattery was shown at about 4.7 stars / 611K reviews / 10M+ downloads; Battery Guru around 4.5 / 35.6K / 1M+; GSam around 4.1 / 73.1K / 1M+; Ampere around 4.5 / 333K / 10M+; 3C Battery Manager around 4.0 / 21.6K / 1M+. Counts and country display can vary.

A sample of public review and Reddit feedback, rather than a statistically representative survey, surfaced concrete requests and frustrations:
- A Battery Guru review dated 25 June 2026 asked for separate Wi-Fi and mobile-data history/forecasts; the developer replied that this was a good idea. VORTIX adds separate transport totals and native time-bucket charts.
- A Battery Guru review dated 23 August 2026 complained about a health estimate rising to 105–110%; the developer acknowledged that such a value should not be displayed. VORTIX withholds the health percentage until at least medium evidence confidence and caps it at 100%, while keeping estimated mAh separate.
- A Battery Guru review dated 22 May 2026 described days without background logs; the developer noted that manufacturer background restrictions can stop session/statistics recording. VORTIX surfaces coverage and missing intervals instead of inventing continuity.
- Battery replacement learning markers and history retention are included as a deliberate response to the recurring need to separate a replacement cell's learning period from older battery history; the action is user-confirmed rather than inferred.
- A Reddit request for a dated record of how long each battery charge lasts called out confusing histories and unreliable-looking health percentages; VORTIX exposes individual dated discharge sessions with observed duration, SOC endpoints and screen-on/off coverage, and refuses to invent unobserved time.
- Another Reddit discussion asked for background drain specifically while the screen is off. Replies noted that meaningful per-app background attribution often needs ADB/Shizuku/root and has become less reliable on newer Android versions. VORTIX therefore adds whole-device screen-off drain detection against the phone's own baseline, without falsely naming an app as the cause.
- Recent Reddit suggestions distinguish lightweight system Battery settings from deeper diagnostic tools (BetterBatteryStats, 3C, GSam-like tools); VORTIX keeps a no-root baseline, documents the optional privileged-tool gap, and favours low-overhead scheduled samples rather than a permanently running overlay.

## User pain points translated into product decisions

1. **“How long did my battery actually last?”** — expose a dated endurance diary of the latest discharge sessions, start/end state of charge, observed screen-on/off time and a runtime estimate only when enough samples exist; explain that monitoring may begin mid-cycle and gaps can split a session.
2. **Unexplained screen-off drain** — compare screen-on vs screen-off discharge rate, show sampling gaps, and do not attribute the whole-device loss to a named app without causal evidence.
3. **Wi-Fi vs mobile data context** — separate totals and separate history plots. The network chart uses Android native buckets; it does not interpolate unavailable intervals or relabel traffic bytes as energy.
4. **Battery replacement resets learning** — create a timestamped baseline and keep old history. The baseline is a user-confirmed event, not automatic physical battery detection.
5. **Background work is not reliable on every OEM** — display sampling coverage and longest gap, explain Doze/WorkManager delays, and never imply 24/7 real-time monitoring.
6. **Users want specific, practical advice** — a rule-based Power Advisor prioritizes heat, screen-off drain, low-battery saver, low-quality history and unused-app review. It only fires when evidence thresholds are met.
7. **Users do not want an app to break notifications or background sync** — Vault is advisory and user-directed; no blanket auto-freezing, forced stop or permission revocation.

## VORTIX differentiation

- **Charge-session evidence:** when the device exposes a fuel-gauge charge counter at both endpoints, the diary records the net mAh counter increase alongside current, duration and peak temperature; it does not claim this is a per-session wear percentage or USB input power.
- **Evidence-first capacity:** robust median/outlier handling; capacity estimate requires substantial percent and charge-counter delta; confidence is tied to sample count, time span and spread; health percentage is withheld without a design-capacity reference and at least medium-confidence measurement evidence; the health score is capped at 100% while estimated mAh remains visible.
- **Measurement coverage as a first-class metric:** sample count, observation span, coverage and longest gap prevent attractive graphs from hiding poor sampling.
- **Screen-state-aware drain:** interval-based screen-on/off rate rather than just a single daily battery percentage.
- **Dated endurance diary:** the latest 14 observed discharge sessions are retained in analytics and up to seven are shown in the Health tab with start/end level and observed screen-on/off duration, responding to requests for an understandable per-day battery-duration record.
- **Personal anomaly baseline:** recent 24-hour screen-off drain is compared with the preceding 7-day median; an alert requires at least 5 baseline intervals, 3 recent intervals, at least +1 percentage point/hour and at least 1.75× the baseline. This is designed to detect a meaningful change from the same device's own norm rather than applying one universal threshold to every phone.
- **Thermal exposure with honest sampling:** only qualifying observed intervals count toward exposure; Android device thermal status is not confused with battery temperature.
- **Before/after energy experiments:** timestamped user notes make a settings or habit change comparable over real observations.
- **Mobile/Wi-Fi traffic context:** system-wide totals plus separate profile-history charts; not represented as per-app energy.
- **Transparent Power Advisor:** prioritised and testable rules; it never diagnoses from network bytes or foreground duration alone.
- **Local-first and non-destructive:** the manifest requests no `INTERNET` permission and disables Android app backup; battery data remains local, CSV export is user-triggered, and optional Usage access is only used for the displayed diagnostics. App hibernation and charging cutoff are not misrepresented as powers the app does not possess.

## Deliberately not copied / not claimed

- **No fake per-app mAh ranking:** UsageStats and NetworkStats are not direct per-app power meters. Android power profiles/UID power data availability is OEM-dependent; stronger attribution would require an explicitly installed and secured ADB/Shizuku/root bridge and separate security review.
- **No automated charge cut-off:** third-party Android apps do not receive a generic cross-OEM hardware charging-stop API. VORTIX provides alerts and system-settings handoff only.
- **No silent hibernation:** Android hibernation/package suspension is system-controlled. Vault guides the user to system App Info and battery settings.
- **No minute-level network timeline claims:** NetworkStats history buckets are hour-scale and do not interpolate partial buckets. The UI preserves those bucket timestamps.
- **No always-on promise:** WorkManager periodic work has a 15-minute lower bound and is deferrable. OEM battery management may further delay or suppress execution.

## Public sources consulted

- [AccuBattery — Google Play](https://play.google.com/store/apps/details?id=com.digibites.accubattery)
- [Battery Guru — Google Play](https://play.google.com/store/apps/details?id=com.paget96.batteryguru)
- [GSam Battery Monitor — Google Play](https://play.google.com/store/apps/details?id=com.gsamlabs.bbm)
- [3C Battery Manager — Google Play](https://play.google.com/store/apps/details?id=ccc71.bmw)
- [Ampere — Google Play](https://play.google.com/store/apps/details?id=com.gombosdev.ampere)
- [BatteryBot Pro — Google Play](https://play.google.com/store/apps/details?id=com.darshancomputing.BatteryIndicatorPro)
- [Reddit — What's the best battery app? (November 2026)](https://www.reddit.com/r/androidapps/comments/1xopacc/whats_the_best_battery_app/)
- [Reddit — Background battery drain while screen off](https://www.reddit.com/r/androidapps/comments/1eeib6n/)
- [Reddit — Battery app / daily duration after last charge](https://www.reddit.com/r/androidapps/comments/1txfroh/battery_app_charged_battery_vs_time_used_after/)
- [Reddit — Charging stats notification suggestions (November 2026)](https://www.reddit.com/r/androidapps/comments/1xz068p/looking_for_an_app_that_will_send_a_live/)
- [Android BatteryManager API](https://developer.android.com/reference/android/os/BatteryManager)
- [Android NetworkStatsManager API](https://developer.android.com/reference/android/app/usage/NetworkStatsManager)
- [Android NetworkStats API](https://developer.android.com/reference/android/app/usage/NetworkStats)
- [Android AppOpsManager API](https://developer.android.com/reference/android/app/AppOpsManager)
- [Android UsageStatsManager API](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [Android PowerManager API](https://developer.android.com/reference/android/os/PowerManager)
- [WorkManager periodic work guide](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)
- [AOSP App Hibernation](https://source.android.com/docs/core/permissions/app-hibernation)

Store ratings, public review excerpts and public 2026 comparison roundups were used as qualitative product signals, not statistically representative surveys or a claim of objective market ranking. Findings and trade-offs are detailed in the source review for this release.
