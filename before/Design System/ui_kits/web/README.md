# 집구해 Web UI Kit

Interactive click-through prototype of all 4 core screens.

## Screens
| Screen | Description |
|--------|-------------|
| Main Listing | Category pills, filter tags, 4-col card grid |
| Detail | Hero, info grid, timeline, market comparison, sticky sidebar |
| Login / Sign-up | Tab switch, 2-step sign-up, category preference grid |
| My Page | Profile form, saved listings, notification centre |

## Files
| File | Role |
|------|------|
| `index.html` | Entry point — React shell, routing, shared CSS vars |
| `Header.jsx` | Sticky site header (logo, search, bell, avatar) |
| `FilterBar.jsx` | Category pill bar + filter tag row |
| `ListingCard.jsx` | Card component with image, status badge, heart, tags |
| `DetailPage.jsx` | Full detail layout — left column + sticky right card |
| `AuthPage.jsx` | Login / 2-step sign-up / success state |
| `MyPage.jsx` | Sidebar + profile/saved/notifications sections |

## Usage
Open `index.html` in a browser. Navigate between screens using the header
avatar (→ My Page), back buttons, and card clicks.

## Design Notes
- Font: Noto Sans KR 400/500/600/700
- Brand red: #ff385c
- Three-layer card shadow for all elevated surfaces
- Components are cosmetic-only — no real API calls
