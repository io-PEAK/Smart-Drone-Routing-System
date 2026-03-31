<p align="center">
  <img src="file:///Users/peakmac/.gemini/antigravity/brain/a3ee818d-304c-4c58-a704-b6cddeff1aa5/smart_drone_logo_1774847343749.png" width="300" alt="Smart Drone Routing & Geofencing System Logo" />
</p>

# 🚀 Smart Drone Routing & Geofencing System

<p align="center">
  <img src="https://img.shields.io/badge/Language-Java-orange?style=for-the-badge&logo=java" alt="Java Badge"/>
  <img src="https://img.shields.io/badge/UI_Framework-Swing-blue?style=for-the-badge" alt="Swing Badge"/>
  <img src="https://img.shields.io/badge/Algorithms-DAA-brightgreen?style=for-the-badge&logo=apache" alt="DAA Badge"/>
</p>

The **Smart Drone Routing & Geofencing System** is an interactive, visual application built in pure Java that brings core Design and Analysis of Algorithms (DAA) concepts to life. 

Wrapped in a beautiful **Neumorphic (Soft UI)** design system, this tool acts as a powerful "Mission Control" surface, allowing users to map out drone supply hubs and strategically solve routing and containment challenges in real-time.

---

## 🛰 Core Capabilities

### 📐 1. Geofence Construction (Graham Scan)
Click and place multiple targets on the interactive map. When activated, the system dynamically plots a seamless geographical boundary (Convex Hull) perfectly wrapping your targets.
- **Algorithm Used:** Graham Scan Convex Hull
- **Time Complexity:** $O(n \log n)$ 
- **Use Case:** Preventing drones from exiting designated flight authorization zones and ensuring containment.

### 🛸 2. Optimal Route Calculation (TSP Backtracking)
Computes the absolute shortest Hamiltonian cycle that visits every plotted drone delivery target exactly once and safely returns to the Base Depot Hub.
- **Algorithm Used:** Traveling Salesman Problem via Exhaustive Backtracking with Distance Pruning
- **Time Complexity:** $O(n!)$
- **Use Case:** Maximizing drone battery life while minimizing delivery turnaround times.

### 🔗 3. Hub Connection Network (Kruskal's MST)
Generates the cheapest possible structural network to link all plotted drone charging hubs without creating any redundant loops. 
- **Algorithm Used:** Kruskal’s Minimum Spanning Tree utilizing Union-Find mapping
- **Time Complexity:** $O(E \log E)$
- **Use Case:** Extending persistent communication grid infrastructure or laying power lines between base stations with minimal cabling overhead.

---

## 🎨 Neumorphic Design Interface

We bypassed conventional Java `Look-And-Feel` constraints by engineering a **custom 2D Graphics Engine** utilizing raw `Graphics2D`. The application sports a premium **Neumorphic / Soft UI** aesthetic right out of the box:

- **Molded Physical Nodes:** Hub nodes dynamically cast multi-directional shadow layers (`#FFFFFF` top-left, `#A3B1C6` bottom-right) into the cool clay `#E0E5EC` background.
- **Extruded Buttons:** The navigation buttons actively calculate "Inset" vs "Extruded" depths based on click registration, rendering a highly satisfying tactile pressure mechanism.
- **Auto-Anchoring Context Tooltips:** Contextual algorithmic teachings are smoothly rendered in bottom-right bound panels, instantly wrapping character boundaries based on exact pixel metrics to prevent messy HTML clipping.

---

## ⚙️ Getting Started

### Prerequisites
- Operating System: Windows / macOS / Linux
- IDE: IntelliJ IDEA, Eclipse, VSCode, or Terminal
- Java Development Kit (JDK 11 or higher recommended)

### Build & Run
If using a Bash/Zsh terminal:

```bash
# 1. Provide execution rights
chmod +x run.sh

# 2. Compile and Launch
./run.sh
```

Alternatively, if running directly in an IDE, simply compile the `src/` directory and execute the `Main.java` class as the entry point.

---
