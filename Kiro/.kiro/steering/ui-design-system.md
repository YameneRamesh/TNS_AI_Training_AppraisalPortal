---
inclusion: auto
description: UI design system guidelines for TNS Appraisal Portal including brand colors, typography, spacing, animations, and component patterns
---

# UI Design System — TNS Appraisal Form 

## Brand Color Palette (derived from Think N Solutions)

The palette is anchored in deep navy blues with warm amber/orange accents, reflecting the TnS corporate identity.

### CSS Custom Properties

```css
:root {
  /* Primary — Deep Navy (dominant) */
  --color-primary-900: #0A1628;
  --color-primary-800: #0F2240;
  --color-primary-700: #152E54;
  --color-primary-600: #1B3A68;
  --color-primary-500: #1E4D8C;
  --color-primary-400: #2A6CB8;
  --color-primary-300: #4A8FD4;
  --color-primary-200: #7BB3E8;
  --color-primary-100: #B8D6F5;
  --color-primary-50:  #E8F2FC;

  /* Accent — Warm Amber/Orange (sharp accent) */
  --color-accent-700: #B45309;
  --color-accent-600: #D97706;
  --color-accent-500: #F59E0B;
  --color-accent-400: #FBBF24;
  --color-accent-300: #FCD34D;
  --color-accent-200: #FDE68A;
  --color-accent-100: #FEF3C7;

  /* Neutral — Cool Grays */
  --color-neutral-900: #111827;
  --color-neutral-800: #1F2937;
  --color-neutral-700: #374151;
  --color-neutral-600: #4B5563;
  --color-neutral-500: #6B7280;
  --color-neutral-400: #9CA3AF;
  --color-neutral-300: #D1D5DB;
  --color-neutral-200: #E5E7EB;
  --color-neutral-100: #F3F4F6;
  --color-neutral-50:  #F9FAFB;

  /* Semantic Colors */
  --color-success: #059669;
  --color-success-light: #D1FAE5;
  --color-warning: #D97706;
  --color-warning-light: #FEF3C7;
  --color-danger: #DC2626;
  --color-danger-light: #FEE2E2;
  --color-info: #2563EB;
  --color-info-light: #DBEAFE;

  /* Surface & Background */
  --surface-primary: #FFFFFF;
  --surface-secondary: #F8FAFD;
  --surface-elevated: #FFFFFF;
  --surface-dark: #0A1628;
  --surface-dark-secondary: #0F2240;

  /* Shadows */
  --shadow-sm: 0 1px 2px rgba(10, 22, 40, 0.06);
  --shadow-md: 0 4px 12px rgba(10, 22, 40, 0.08);
  --shadow-lg: 0 12px 32px rgba(10, 22, 40, 0.12);
  --shadow-xl: 0 20px 48px rgba(10, 22, 40, 0.16);
  --shadow-glow-accent: 0 0 24px rgba(245, 158, 11, 0.2);
}
```

## Typography

Use distinctive, characterful fonts — avoid generic choices.

```css
:root {
  /* Display / Headings — bold, architectural feel */
  --font-display: 'Clash Display', 'Satoshi', sans-serif;

  /* Body — refined, highly readable */
  --font-body: 'General Sans', 'Outfit', sans-serif;

  /* Mono — for reference numbers, codes */
  --font-mono: 'JetBrains Mono', 'Fira Code', monospace;

  /* Scale */
  --text-xs: 0.75rem;
  --text-sm: 0.875rem;
  --text-base: 1rem;
  --text-lg: 1.125rem;
  --text-xl: 1.25rem;
  --text-2xl: 1.5rem;
  --text-3xl: 1.875rem;
  --text-4xl: 2.25rem;
  --text-5xl: 3rem;

  /* Weights */
  --font-regular: 400;
  --font-medium: 500;
  --font-semibold: 600;
  --font-bold: 700;

  /* Line heights */
  --leading-tight: 1.2;
  --leading-normal: 1.5;
  --leading-relaxed: 1.75;

  /* Letter spacing */
  --tracking-tight: -0.02em;
  --tracking-normal: 0;
  --tracking-wide: 0.025em;
  --tracking-wider: 0.05em;
}
```

Font sources: Use Google Fonts or Fontshare (free). Load via `@import` or `<link>` in `index.html`.

## Spatial Composition

- Use generous whitespace — let content breathe
- Sidebar navigation: fixed left, dark navy (`--color-primary-900`)
- Main content area: light surface with subtle warm tint
- Cards: white with `--shadow-md`, 12px border-radius, subtle left-border accent in amber
- Grid-breaking hero sections on dashboard with diagonal clip-paths or overlapping elements
- Status badges: pill-shaped with semantic colors, slight backdrop blur

### Spacing Scale
```css
:root {
  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 0.75rem;
  --space-4: 1rem;
  --space-5: 1.25rem;
  --space-6: 1.5rem;
  --space-8: 2rem;
  --space-10: 2.5rem;
  --space-12: 3rem;
  --space-16: 4rem;
  --space-20: 5rem;
}
```

## Motion & Animations

- Page transitions: staggered fade-up reveals with `animation-delay` increments of 60ms
- Card hover: subtle lift (`translateY(-2px)`) + shadow expansion + amber glow
- Button interactions: scale(0.97) on press, smooth color transitions (200ms ease)
- Sidebar nav items: slide-in indicator bar from left on active state
- Status transitions: smooth color morph with 300ms ease-in-out
- Loading states: skeleton shimmer with navy-to-light gradient sweep
- Use Angular's `@angular/animations` module for route transitions and component enter/leave

```css
/* Base transition */
--transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1);
--transition-base: 200ms cubic-bezier(0.4, 0, 0.2, 1);
--transition-slow: 300ms cubic-bezier(0.4, 0, 0.2, 1);
--transition-spring: 500ms cubic-bezier(0.34, 1.56, 0.64, 1);
```

## Backgrounds & Visual Details

- Dashboard background: subtle dot-grid pattern over `--surface-secondary`
- Sidebar: deep navy gradient from `--color-primary-900` to `--color-primary-800`
- Cards: frosted glass effect on hover (backdrop-filter: blur(8px))
- Section dividers: thin amber accent lines or gradient fades
- Resume viewer panel: dark inset background (`--color-neutral-800`) for contrast
- Screening/Call screens: split layout with details on left, action panel on right
- Noise texture overlay at 3-5% opacity on dark surfaces for depth
- Status pipeline visualization: horizontal stepper with connected dots and amber active indicator

## Component Patterns

### Buttons
- Primary: solid `--color-accent-500` background, dark text, bold weight
- Secondary: outlined with `--color-primary-500` border, transparent background
- Danger (Reject): solid `--color-danger` background, white text
- Success (Select/Accept): solid `--color-success` background, white text
- All buttons: 8px border-radius, 12px 24px padding, uppercase tracking-wide text

### Cards
- White background, 12px border-radius
- 3px left border in `--color-accent-500` for active/highlighted cards
- Hover: lift + shadow-lg + subtle amber glow

### Tables
- Striped rows with alternating `--surface-primary` and `--surface-secondary`
- Header row: `--color-primary-800` background, white text, uppercase tracking-wider
- Row hover: light amber tint (`--color-accent-100`)
- Sticky header on scroll

### Forms
- Input fields: 8px border-radius, 1px `--color-neutral-300` border
- Focus state: 2px `--color-accent-500` border with `--shadow-glow-accent`
- Labels: `--font-body` semibold, `--color-neutral-700`
- Error states: `--color-danger` border + light red background

### Status Badges
```
SUBMITTED       → --color-neutral-500 bg, white text
UNDER_REVIEW    → --color-info bg, white text
SCREENING_PASSED → --color-accent-500 bg, dark text
CALL_COMPLETED  → --color-primary-500 bg, white text
ACCEPTED        → --color-success bg, white text
REJECTED        → --color-danger bg, white text
```

## Layout Structure

```
┌─────────────────────────────────────────────────┐
│  Top Bar (logo, user avatar, notifications)     │
├──────────┬──────────────────────────────────────┤
│          │                                      │
│  Sidebar │  Main Content Area                   │
│  (dark   │  ┌──────────────────────────────┐   │
│   navy)  │  │  Page Header + Breadcrumbs   │   │
│          │  ├──────────────────────────────┤   │
│  • List  │  │                              │   │
│  • Screen│  │  Content (tables, forms,     │   │
│  • Call  │  │  split views, cards)         │   │
│  • Verify│  │                              │   │
│          │  └──────────────────────────────┘   │
│          │                                      │
└──────────┴──────────────────────────────────────┘
```

## Do NOT Use
- Generic fonts: Arial, Inter, Roboto, system-ui
- Purple gradients on white backgrounds
- Cookie-cutter Material Design defaults without customization
- Flat, lifeless layouts with no depth or texture
- Evenly distributed color palettes — commit to navy dominance with amber accents
