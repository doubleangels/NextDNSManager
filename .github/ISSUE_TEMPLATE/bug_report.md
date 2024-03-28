name: Report a bug or issue
about: Create a report to help improve the app. Make sure you read the FAQ's first or your issue may be closed.
title: "[Bug/Issue] "
labels: "type: bug"
assignees: "doubleangels"
body:
    - type: dropdown
      id: urgent
      attributes:
            label: Is this bug/issue urgent?
            description: Is this a critical bug, or do you need this fixed urgently?
            options:
                - "No"
                - "Yes"
    - type: input
        id: android-version
        attributes:
            label: Android Version
            description: What is your Android version?
    - type: input
        id: device
        attributes:
            label: Device
            description: What kind of device do you have?
    - type: input
        id: app-version
        attributes:
            label: App Version
            description: What verison of the app are you using?
    - type: textarea
        id: problem
        attributes:
            label: Bug/Issue
            description: Explain what the bug/issue is.
            render: plain text
