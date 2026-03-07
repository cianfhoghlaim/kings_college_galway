---
title: "Release Komodo v2.0.0-dev-102 · moghtech/komodo"
source: "https://github.com/moghtech/komodo/releases/tag/v2.0.0-dev-102"
author:
  - "[[GitHub]]"
published:
created: 2025-12-20
description: "🦎 a tool to build and deploy software on many servers 🦎 - Release Komodo v2.0.0-dev-102 · moghtech/komodo"
tags:
  - "clippings"
---
[Skip to content](https://github.com/moghtech/komodo/releases/tag/#start-of-content)

## Komodo v2.0.0-dev-102

Pre-release

Pre-release

[mbecker20](https://github.com/mbecker20) released this

· [4 commits](https://github.com/moghtech/komodo/compare/v2.0.0-dev-102...2.0.0) to 2.0.0 since this release

[v2.0.0-dev-102](https://github.com/moghtech/komodo/tree/v2.0.0-dev-102)

[`c638e38`](https://github.com/moghtech/komodo/commit/c638e3876e785e6fda76a3410afa9baae782cb70)

Core image: `ghcr.io/moghtech/komodo-core:2-dev`  
Periphery image: `ghcr.io/moghtech/komodo-periphery:2-dev`  
Km image: `ghcr.io/moghtech/komodo-cli:2-dev`

- Docker Swarm **config** and **secret** management
- Support linking **multiple primary login methods** (Local / OIDC / Google / Github) for the same user.
- 🚨 There is a small change to a database schema that is made when you upgrade from v1 to v2. This is done automatically, but if you need to go back to v1, **before taking down the v2 Core container run**:
```
docker compose -p komodo exec core bash -c "km database v1-downgrade -y"
```

PR: [#889](https://github.com/moghtech/komodo/pull/889)

[![Screenshot 2025-12-18 at 3 24 16 PM](https://private-user-images.githubusercontent.com/49575486/528370299-f309bb9c-8ad1-4165-a976-8961cbdaa54e.png?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3NjYxODkzMjEsIm5iZiI6MTc2NjE4OTAyMSwicGF0aCI6Ii80OTU3NTQ4Ni81MjgzNzAyOTktZjMwOWJiOWMtOGFkMS00MTY1LWE5NzYtODk2MWNiZGFhNTRlLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNTEyMjAlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjUxMjIwVDAwMDM0MVomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWVmOTJjMDFmZDIyMjhkYjhmYTA5MTcyYmU1MDM4N2U4ZGZlYjdkMDY1MWY1ZjhhM2YxNTZhMTQ2Y2Y0ODlmM2MmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0In0.kR0SS8MCTgwZ6uAaXlu7MtNP5RsW02ASP-2m3rnSW6g)](https://private-user-images.githubusercontent.com/49575486/528370299-f309bb9c-8ad1-4165-a976-8961cbdaa54e.png?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3NjYxODkzMjEsIm5iZiI6MTc2NjE4OTAyMSwicGF0aCI6Ii80OTU3NTQ4Ni81MjgzNzAyOTktZjMwOWJiOWMtOGFkMS00MTY1LWE5NzYtODk2MWNiZGFhNTRlLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNTEyMjAlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjUxMjIwVDAwMDM0MVomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWVmOTJjMDFmZDIyMjhkYjhmYTA5MTcyYmU1MDM4N2U4ZGZlYjdkMDY1MWY1ZjhhM2YxNTZhMTQ2Y2Y0ODlmM2MmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0In0.kR0SS8MCTgwZ6uAaXlu7MtNP5RsW02ASP-2m3rnSW6g) [![Screenshot 2025-12-18 at 3 08 35 PM](https://private-user-images.githubusercontent.com/49575486/528370318-adac3e98-f459-485b-be22-6b119d9744bb.png?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3NjYxODkzMjEsIm5iZiI6MTc2NjE4OTAyMSwicGF0aCI6Ii80OTU3NTQ4Ni81MjgzNzAzMTgtYWRhYzNlOTgtZjQ1OS00ODViLWJlMjItNmIxMTlkOTc0NGJiLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNTEyMjAlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjUxMjIwVDAwMDM0MVomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWRlZTMzODM1ZmUzZDgwOGQ3ZDg4NTg2OTgzOTRiZTY5MDdlMGQ4NWY0NjE4NDBiMjg0OTFlYTNhOTEwZTMzZWQmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0In0.X7l0jaDbsbY1jfjHDfnsEIMPDEglTkPaLZZYO0PoH1w)](https://private-user-images.githubusercontent.com/49575486/528370318-adac3e98-f459-485b-be22-6b119d9744bb.png?jwt=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJnaXRodWIuY29tIiwiYXVkIjoicmF3LmdpdGh1YnVzZXJjb250ZW50LmNvbSIsImtleSI6ImtleTUiLCJleHAiOjE3NjYxODkzMjEsIm5iZiI6MTc2NjE4OTAyMSwicGF0aCI6Ii80OTU3NTQ4Ni81MjgzNzAzMTgtYWRhYzNlOTgtZjQ1OS00ODViLWJlMjItNmIxMTlkOTc0NGJiLnBuZz9YLUFtei1BbGdvcml0aG09QVdTNC1ITUFDLVNIQTI1NiZYLUFtei1DcmVkZW50aWFsPUFLSUFWQ09EWUxTQTUzUFFLNFpBJTJGMjAyNTEyMjAlMkZ1cy1lYXN0LTElMkZzMyUyRmF3czRfcmVxdWVzdCZYLUFtei1EYXRlPTIwMjUxMjIwVDAwMDM0MVomWC1BbXotRXhwaXJlcz0zMDAmWC1BbXotU2lnbmF0dXJlPWRlZTMzODM1ZmUzZDgwOGQ3ZDg4NTg2OTgzOTRiZTY5MDdlMGQ4NWY0NjE4NDBiMjg0OTFlYTNhOTEwZTMzZWQmWC1BbXotU2lnbmVkSGVhZGVycz1ob3N0In0.X7l0jaDbsbY1jfjHDfnsEIMPDEglTkPaLZZYO0PoH1w)