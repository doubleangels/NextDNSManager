# Security Policy

NextDNS Manager gives access to a user's NextDNS account and their DNS query log, so security and privacy reports are taken seriously.

## Supported versions

Only the latest release on [Google Play](https://play.google.com/store/apps/details?id=com.doubleangels.nextdnsmanagement), [GitHub](https://github.com/doubleangels/nextdnsmanager/releases) and F-Droid receives security fixes. Please update before reporting.

## Reporting a vulnerability

**Please do not report security vulnerabilities in a public issue, pull request or discussion.**

Use GitHub's private reporting instead:

1. Open the [**Security** tab](https://github.com/doubleangels/nextdnsmanager/security) of this repository.
2. Choose **Report a vulnerability** and describe the problem.

If that option is not available, open an [issue](https://github.com/doubleangels/nextdnsmanager/issues/new) that only asks for a private way to contact the maintainer. Do not include technical details or exploit steps in it.

Helpful details to include:

- What the problem is and what an attacker could gain
- The NextDNS Manager version, flavor (Google Play, GitHub or F-Droid), Android version and device
- Steps to reproduce, or a small proof of concept
- Whether any real account or query-log data was involved (please use a test NextDNS profile instead)

## What to expect

- An acknowledgement of your report, usually within a few days.
- A fix or a clear explanation of next steps once the issue is confirmed.
- Credit in the release notes if you would like it.

NextDNS Manager is maintained by one person in their spare time, so please be patient.

## What counts as a security or privacy issue

Examples of things worth reporting:

- A way to bypass app lock, or to see the dashboard without unlocking
- Account data, query logs or other personal data leaving the device without the user's consent
- Crash reports containing information that should not have been sent
- A web page or another app being able to call into the app's WebView bridge or trigger actions it should not
- Settings stored without encryption when encrypted storage is available

Ordinary bugs, crashes and feature requests belong in a normal [GitHub issue](https://github.com/doubleangels/nextdnsmanager/issues).
