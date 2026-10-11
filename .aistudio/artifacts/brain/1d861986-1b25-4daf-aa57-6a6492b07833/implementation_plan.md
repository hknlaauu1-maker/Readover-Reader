# Screen Size and Resolution Responsive Adaptation

Comprehensive UI and layout adaptations to ensure the application automatically scales and responds gracefully across all Android screen sizes, densities, orientations (portrait/landscape), tablets, and foldables.

## User Decisions & Confirmed Scope
<is_multi_select>true</is_multi_select>
- **Screen Adaptation Focus**:
  - Farklı ekran yoğunlukları için dinamik yazı tipi ve boşluk ayarları (Dynamic font and spacing scaling for screen density and size)
  - Yatay ve dikey mod geçişlerinde esnek yerleşim (Flexible layouts for portrait and landscape orientation switching)
  - Tablet ve katlanabilir ekran uyumluluğu (Tablet and foldable screen multi-window / multi-pane support)
  - Görsellerin ve kartların ekrana göre ölçeklenmesi (Responsive card and image scaling based on window size classes)

---

## 1. Overview & Core Concept
- **What It Does**: Enhances the entire application layout using Jetpack Compose WindowSizeClass, BoxWithConstraints, and responsive modifiers so that typography, spacing, padding, cards, and navigation adjust smoothly from compact phones to large tablets and foldables in both portrait and landscape modes.
- **Key Value**: Delivers an optimal, polished native Android reading and library management experience on every device factor without clipping, text overflow, or rigid sizing constraints.

---

## 2. User Experience & Responsive Design
- **Key User Flows & Layout Adaptations**:
  - **Compact Phones (Portrait)**: Single-column vertical layout, bottom navigation bar, edge-to-edge content with status bar padding clearance.
  - **Phones (Landscape)**: Side-by-side or horizontally optimized content panels, compact bottom bar or floating navigation, preventing vertical scrolling exhaustion.
  - **Tablets & Foldables (Medium / Expanded)**: Multi-pane layout in Library and Reader screens, adaptive grid columns for book lists (`LazyVerticalGrid` with adaptive span), and expanded margins.
- **Dynamic Sizing & Typography**:
  - Use `BoxWithConstraints` and window width size classes to scale padding (e.g., 16dp on compact, 24dp on medium, 32dp on expanded).
  - Use responsive text styles that scale with system font sizes and density.

---

## 3. Technical Architecture & Implementation Strategy

```
┌────────────────────────────────────────────────────────┐
│               MainActivity / NavHost                   │
└───────────────────────────┬────────────────────────────┘
                            │ WindowSizeClass / BoxWithConstraints
                            ▼
┌────────────────────────────────────────────────────────┐
│             Responsive Scaffolds & Containers          │
├───────────────────┬───────────────────┬────────────────┤
│  LibraryScreen    │  ReaderScreen     │ AudiobookScreen│
│  (Adaptive Grid)  │  (Multi-Pane/     │ (Scaled Cards) │
│                   │   Two-Page/Wide)  │                │
└───────────────────┴───────────────────┴────────────────┘
```

- **Responsive Components**:
  - `LibraryScreen`: Adapt book grid columns (`Adaptive(minSize = 160.dp)`).
  - `ReaderScreen`: Dynamic margin and max text width constraints (`Modifier.widthIn(max = 720.dp).align(Alignment.CenterHorizontally)`) on tablets to maintain comfortable reading line lengths.
  - `AudiobookPlayerScreen`: Scaled artwork, flexible button spacing for landscape and portrait modes.
  - `SettingsScreen`: Centered card layout with maximum width bounds on wider screens.
