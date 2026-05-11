# 집구해 Design System

## Product Overview

**집구해** (JipGuHae, lit. "Find a House") is a Korean public housing subscription aggregator — a web platform that lets users browse, filter, save, and apply for government-subsidised rental and sale housing announcements (공고).

The product is strongly Airbnb-inspired: white canvas, a singular red brand accent, photography-first cards, pill-based category filtering, and a warm near-black type palette — all re-contextualised for Korean civic housing content.

### Source Materials
- **Codebase:** `SEUIL2/Subscription-TTAIT @ main` — `demo/frontEnd/frontDemo/` (4 HTML prototype screens)
  - `01_main_list.html` — listing grid with category pills + filter tags
  - `02_detail.html` — announcement detail with sticky sidebar card
  - `03_login_signup.html` — auth flow (login + 2-step sign-up)
  - `04_mypage.html` — my page: profile, saved listings, notifications
- **Design Inspiration:** Airbnb design system doc (see system prompt)
- No Figma link provided.

### Core Products / Surfaces
| Surface | Description |
|---------|-------------|
| Main Listing | Category pill bar, filter tag row, 4-column card grid, pagination |
| Detail Page | Hero image, 2-col layout (info + sticky card), timeline, market comparison |
| Auth (Login / Sign-up) | Tab switcher, 2-step sign-up, category preference grid |
| My Page | Sidebar nav, profile form, saved listings, notification centre |

---

## Content Fundamentals

- **Language:** Korean (한국어) throughout. All UI copy is in Korean.
- **Tone:** Warm, helpful, approachable. The login greeting is "다시 만나서 반가워요 👋" (Great to see you again). Sign-up success: "가입이 완료됐어요!" — casual, friendly.
- **Voice:** Second-person 해요체 (polite informal). Never formal 합쇼체. Abbreviated and direct: "마감 D-6", "모집중", "신청하러 가기 →".
- **Casing:** Korean doesn't have letter casing. Section labels in the My Page form use `text-transform: uppercase` for Latin/alphanumeric labels only (rare).
- **Numbers:** Korean 만/억 currency format — e.g., "1,119만원", "3억 2,000만원". Deposit + monthly rent shown separately.
- **Dates:** YYYY년 MM월 DD일 long format in official contexts; "D-6", "D-1" urgency format in card/badge contexts. "오늘!" for same-day deadlines.
- **Emoji use:** Sparingly; only in greeting copy and success states (👋, 🎉). Category chips use emoji as icons in lieu of SVG (👤청년, 💑신혼부부, 🏠무주택자, 👴고령자, 💰저소득층, 👨‍👩‍👧‍👦다자녀). Card image placeholders use building emoji (🏢🏗️🏠🏘️🏙️🏛️).
- **Call-to-action copy:** Always ends with arrow → ("신청하러 가기 →", "공고 둘러보기 →", "다음 단계 →").
- **Error/urgency:** Red (#ff385c) "마감임박", "D-1", "D-2", "마감 D-N". Neutral gray for non-urgent deadlines.

---

## Visual Foundations

### Color
See `colors_and_type.css` for full token list.

| Token | Value | Role |
|-------|-------|------|
| `--rausch` | `#ff385c` | Brand red — CTAs, active states, urgency, brand accents |
| `--rausch-dark` | `#e00b41` | Pressed/hover variant of brand red |
| `--near-black` | `#222222` | Primary text and dark button backgrounds |
| `--gray-secondary` | `#6a6a6a` | Secondary text, metadata, placeholders |
| `--gray-border` | `#c1c1c1` | Borders for inputs, dividers |
| `--gray-surface` | `#f2f2f2` | Page backgrounds (My Page), secondary surfaces, icon backgrounds |
| `--white` | `#ffffff` | Card surfaces, main page background, header |

**Semantic colour use:**
- `#fff0f3` / red tint: active pill bg, tag bg, deadline info boxes, selected chips
- `#eff6ff` / blue tint `#1d4ed8`: 청년(youth) tags
- `#fdf4ff` / purple tint `#7e22ce`: 신혼부부(newlywed) tags
- `#f0fdf4` / green tint `#166534`: 저소득(low-income) tags
- `#fff7ed` / orange tint `#c2410c`: 고령자(elderly) tags

### Typography
Font: **Noto Sans KR** (Google Fonts, weights 400/500/600/700). This is a substitution for the Airbnb Cereal VF specified in the inspiration doc — Noto Sans KR has excellent Korean glyph coverage.

Hierarchy mirrors the Airbnb spec closely: weight 700 for headings, 600 for semi-emphasis, 500 for UI/body, 400 for descriptions. Negative letter-spacing on headings (-0.3px to -0.5px).

See `colors_and_type.css` for full type scale.

### Backgrounds
- Pure white (#ffffff) for all main content areas
- Light gray (#f2f2f2) as page background in My Page only
- No gradient backgrounds except the detail hero overlay (linear-gradient to-top, rgba black, for text legibility over image)
- No patterns, textures, or illustrations

### Spacing
Base 8px unit. Key values: 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80px.

### Border Radius Scale
| Name | Value | Usage |
|------|-------|-------|
| Subtle | 4px | Tag borders |
| Standard | 8px | Logo icon, buttons, filter tags |
| Badge | 10–12px | Attachment items, info boxes |
| Card | 14–20px | Listing cards, sticky cards, section cards, sidebar |
| Large | 40px | Search bar (pill) |
| Circle | 50% | Search button, navigation buttons, avatar, bell button |

### Shadows
Three-layer card shadow (all elevated surfaces):
```
rgba(0,0,0,0.02) 0px 0px 0px 1px,
rgba(0,0,0,0.04) 0px 2px 6px,
rgba(0,0,0,0.1) 0px 4px 8px
```
Hover shadow: `rgba(0,0,0,0.08) 0px 4px 12px`

### Animations & Transitions
- All transitions `0.15s–0.2s` ease
- Hover on listing cards: `translateY(-2px)` + card shadow
- Image hover: `scale(1.03)` on card image
- Button active: `scale(0.92)` or `scale(0.98)`
- Toggle switch: `transform: translateX(20px)` on knob
- No bounces, spring, or complex easing — all linear/ease

### Hover & Press States
- Text buttons: background fills with `#f2f2f2`
- Icon buttons: box-shadow lifts to hover shadow
- Cards: translateY(-2px) + shadow
- Primary CTA hover: darkens (`#e00b41`)
- Active/press: slight scale-down (0.92–0.98)

### Cards
- Listing cards: no shadow at rest, shadow+lift on hover; 20px radius; 16:10 image ratio
- Sticky/section cards: always-on three-layer shadow; 20px radius
- No border on cards (shadow provides the boundary)

### Imagery / Color Vibe
- Placeholder emoji in prototype (real images would be building photography)
- No brand illustrations
- Hero image with dark gradient overlay for badge legibility

---

## Iconography

SVG icons are inline throughout — **no icon font, no external icon CDN**. All icons are hand-written SVG path elements in `stroke` style (2px stroke-width, no fill) except:
- Logo icon: filled path (house/pin shape, white fill on red background)
- Heart/wishlist: switchable between filled (saved) and stroked (unsaved)
- File/document attachment: filled

**Icon sizes:** 14px (tags), 16–18px (buttons/nav), 22px (filter pill icons), 24px (category chip emoji).

**No external icon library** is linked. The set uses a Material-style stroke aesthetic. Icons are always embedded inline — no img tags, no sprite sheets.

**Emoji as icons:** Category type chips (청년, 신혼부부, etc.) use emoji characters (👤💑🏠👴💰👨‍👩‍👧‍👦) as visual icons in a 3-column grid layout. This is intentional per the product's approachable tone.

See `assets/icons/` for extracted inline SVG snippets.

---

## File Index

```
README.md                      ← This file
SKILL.md                       ← Agent skill manifest
colors_and_type.css            ← Full CSS variable token sheet + type scale
assets/
  icons/                       ← Key inline SVG icons as .svg files
preview/
  01_colors_brand.html         ← Brand color swatches
  02_colors_semantic.html      ← Semantic / state colors
  03_type_scale.html           ← Typography scale specimen
  04_spacing_radius.html       ← Border radius + spacing tokens
  05_shadows_elevation.html    ← Shadow / elevation system
  06_buttons.html              ← Button components
  07_inputs.html               ← Form inputs + selects
  08_cards_listing.html        ← Listing card variants
  09_tags_badges.html          ← Tags, status badges, category chips
  10_filter_pills.html         ← Category filter pill bar
  11_navigation.html           ← Header + nav components
  12_pagination.html           ← Pagination component
ui_kits/
  web/
    README.md
    index.html                 ← Main listing (interactive prototype)
    Header.jsx
    FilterBar.jsx
    ListingCard.jsx
    DetailPage.jsx
    AuthPage.jsx
    MyPage.jsx
```
