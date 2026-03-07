# Educational Game Development

Comprehensive guide to building scientifically accurate educational simulations using game engines, Manim overlays, and AI-driven asset generation for Leaving Certificate Physics and Chemistry.

---

## Table of Contents

1. [Design Philosophy](#1-design-philosophy)
2. [Curriculum Analysis](#2-curriculum-analysis)
3. [Game Engine Limitations](#3-game-engine-limitations)
4. [Pipeline Architecture](#4-pipeline-architecture)
5. [Physics Simulation](#5-physics-simulation)
6. [Chemistry Visualization](#6-chemistry-visualization)
7. [Manim Integration](#7-manim-integration)
8. [Backend Architecture](#8-backend-architecture)
9. [Engine Comparison](#9-engine-comparison)
10. [Implementation Guide](#10-implementation-guide)

---

## 1. Design Philosophy

### 1.1 The Pedagogical Uncanny Valley

Game engines are designed for **believability** and **performance**, not scientific accuracy. This creates a gap where simulations look hyper-realistic but behave according to simplified approximations.

**Core Principle:** Decouple visual representation from physical calculation.

```
Visual Layer (Game Engine)  ←→  Physics Layer (Custom Integrators)
        ↓                                    ↓
   Rendering                            Calculation
   (What you see)                    (What is true)
```

### 1.2 Semantic Digital Twins

The goal is creating **Semantic Digital Twins**: simulations where the visualization is driven by structured, scientifically accurate data. When a student changes a variable, both the 3D scene and mathematical overlay update because they share the same underlying parameter.

### 1.3 Key Architectural Decisions

| Decision | Rationale |
|----------|-----------|
| **3D for asset generation** | Not runtime display |
| **Database for simulation** | Not just storage |
| **Custom physics** | Override engine defaults |
| **Manim for math** | High-quality LaTeX overlays |

---

## 2. Curriculum Analysis

### 2.1 Leaving Certificate Physics: Mandatory Experiments

#### 2.1.1 Strand 1: Forces and Motion (Mechanics)

| Experiment | Challenge | Simulation Strategy |
|------------|-----------|---------------------|
| **Measurement of g (Pendulum)** | Period drift, energy loss | Custom RK4 integrator |
| **Velocity/Acceleration** | Ticker-tape simulation | Explicit position function |
| **Conservation of Momentum** | Collision energy loss | Analytical velocity calculation |
| **Boyle's Law** | Invisible gas pressure | Particle system + live plotting |

**Mathematical Foundation:**
- Pendulum: `T = 2π√(L/g)`
- Motion: `s = ut + ½at²`
- Momentum: `p_before = p_after`
- Boyle's Law: `PV = k`

#### 2.1.2 Strand 2: Wave Motion

| Experiment | Challenge | Simulation Strategy |
|------------|-----------|---------------------|
| **Speed of Sound** | Visualizing longitudinal waves | Vertex displacement shaders |
| **Fundamental Frequency** | Standing waves on string | Sine wave vertex animation |

**Mathematical Foundation:**
- Mersenne's Laws: `f ∝ 1/l`, `f ∝ √T`, `f ∝ 1/√μ`

#### 2.1.3 Strand 3: Light and Optics

| Experiment | Challenge | Simulation Strategy |
|------------|-----------|---------------------|
| **Focal Length (Lens)** | Ray precision, caustics | Line renderers + ray tracing |
| **Snell's Law** | Angle measurement | Manim protractor overlay |

**Mathematical Foundation:**
- Lens equation: `1/f = 1/u + 1/v`
- Snell's Law: `n₁sinθ₁ = n₂sinθ₂`

#### 2.1.4 Strand 4: Electricity and Magnetism

| Experiment | Challenge | Simulation Strategy |
|------------|-----------|---------------------|
| **Joule's Law** | Circuit state visualization | State machine + visual effects |
| **Wheatstone Bridge** | Resistance calculation | Analytical circuit solver |

### 2.2 Leaving Certificate Chemistry: Mandatory Experiments

#### 2.2.1 Structure of Matter

| Experiment | Challenge | Simulation Strategy |
|------------|-----------|---------------------|
| **Flame Tests** | Specific spectral colors | Niagara particles + blackbody override |
| **Redox Reactions** | Color change over time | Material shader interpolation |

**Flame Test Colors:**

| Element | Color | RGB Approximation |
|---------|-------|-------------------|
| Lithium | Crimson Red | (220, 20, 60) |
| Sodium | Amber Yellow | (255, 191, 0) |
| Potassium | Lilac | (200, 162, 200) |
| Copper | Blue-Green | (0, 255, 127) |

#### 2.2.2 Volumetric Analysis

| Experiment | Challenge | Simulation Strategy |
|------------|-----------|---------------------|
| **Acid-Base Titration** | Endpoint color change | Shader lerp on volume variable |
| **Standardisation** | Precise stoichiometry | State machine with threshold |

---

## 3. Game Engine Limitations

### 3.1 The Euler Integration Problem

Standard game physics engines use semi-implicit Euler integration:

```
y_{n+1} = y_n + f(t_n, y_n) · dt
```

**Problems:**
- **Error Accumulation:** Energy dissipates or explodes over time
- **Phase Shift:** Simulated period drifts from analytical solution
- **Chaos Sensitivity:** Double pendulum diverges from MATLAB solution

### 3.2 Stokes Workshop Findings

Research on Unity physics accuracy concluded:
- **Unity for scientific calculations:** NO
- **Unity for visualization of scientific results:** YES

The solution is "Shadow Physics": inject custom scripts that calculate position using high-order integrators, degrading the game engine to a high-quality renderer.

### 3.3 Corrective Architectures

| Integrator | Order | Error | Use Case |
|------------|-------|-------|----------|
| **Euler** | 1st | O(dt²) | Never for education |
| **Runge-Kutta 4** | 4th | O(dt⁵) | Pendulum, springs |
| **Velocity Verlet** | 2nd (symplectic) | Energy-conserving | Orbits, molecular |

---

## 4. Pipeline Architecture

### 4.1 DIAGE: Deep Insight & Asset Generation Engine

A multi-agent system for generating scientifically accurate simulations:

```
Syllabus PDF → [PEDAGOGICAL ARCHITECT] → Instruction Manifest
                        ↓
            [SIMULATION ENGINEER] → Custom Physics Scripts
                        ↓
           [VISUALIZATION DIRECTOR] → Manim Overlays
                        ↓
               [ASSET MANAGER] → Scene Assembly
```

### 4.2 Agent Roles

| Agent | Responsibility | Output |
|-------|----------------|--------|
| **Pedagogical Architect** | Analyzes learning outcomes | BAML-compliant JSON |
| **Simulation Engineer** | Generates physics code | C#/C++/Rust scripts |
| **Visualization Director** | Creates math overlays | Manim Python scripts |
| **Asset Manager** | Assembles scene | Engine-specific project |

### 4.3 BAML Schema for Experiments

```typescript
class ExperimentStep {
  step_number: int;
  action: string;  // "Add", "Measure", "Heat"
  equipment: string;
  duration_seconds: float?;
  expected_observation: string?;
}

class ScienceExperiment {
  title: string;
  syllabus_ref: string;  // "Leaving Cert Physics Strand 1"
  required_chemicals: string[]?;
  steps: ExperimentStep[];
  safety_warnings: string[];
}
```

### 4.4 Experiment JSON Schema

```json
{
  "experiment_type": "MECHANICS_PENDULUM",
  "engine_target": "UNITY",
  "simulation_parameters": {
    "integrator": "RK4",
    "gravity": 9.81,
    "length_range": [0.5, 1.0],
    "initial_angle_deg": 5,
    "damping": 0.0
  },
  "visualization_overlays": [
    "force_vectors",
    "period_timer",
    "graph_period_sq_vs_length"
  ]
}
```

---

## 5. Physics Simulation

### 5.1 Runge-Kutta 4 Implementation (Unity C#)

```csharp
// Custom RK4 Integrator for Pendulum
using UnityEngine;

public class PendulumRK4 : MonoBehaviour {
    public float length = 1.0f;
    public float gravity = 9.81f;
    public float theta = 0.087f;  // 5 degrees in radians
    public float omega = 0.0f;    // Angular velocity

    struct State {
        public float theta;
        public float omega;
    }

    State Evaluate(State initial, float dt, State derivative) {
        State state;
        state.theta = initial.theta + derivative.theta * dt;
        state.omega = initial.omega + derivative.omega * dt;

        State output;
        output.theta = state.omega;
        output.omega = -(gravity / length) * Mathf.Sin(state.theta);
        return output;
    }

    void FixedUpdate() {
        float dt = Time.fixedDeltaTime;
        State state = new State { theta = theta, omega = omega };

        // RK4: Sample 4 derivatives
        State a = Evaluate(state, 0, new State());
        State b = Evaluate(state, dt * 0.5f, a);
        State c = Evaluate(state, dt * 0.5f, b);
        State d = Evaluate(state, dt, c);

        // Weighted average
        theta += (a.theta + 2*b.theta + 2*c.theta + d.theta) * (dt / 6.0f);
        omega += (a.omega + 2*b.omega + 2*c.omega + d.omega) * (dt / 6.0f);

        // Update Transform (Shadow Physics)
        float x = length * Mathf.Sin(theta);
        float y = -length * Mathf.Cos(theta);
        transform.localPosition = new Vector3(x, y, 0);
    }
}
```

### 5.2 Velocity Verlet for Orbital Mechanics

```csharp
// For planet/satellite simulations
void FixedUpdate() {
    // Velocity Verlet preserves energy (symplectic)
    Vector3 acceleration = CalculateGravity(position);

    // Half-step velocity
    velocity += 0.5f * acceleration * dt;

    // Full-step position
    position += velocity * dt;

    // Recalculate acceleration at new position
    Vector3 newAcceleration = CalculateGravity(position);

    // Half-step velocity with new acceleration
    velocity += 0.5f * newAcceleration * dt;
}
```

### 5.3 Collision Resolution (Momentum Conservation)

```csharp
// Analytical collision for trolleys (avoids engine physics)
public static void ResolveElasticCollision(
    ref float v1, ref float v2,
    float m1, float m2
) {
    float v1_new = ((m1 - m2) * v1 + 2 * m2 * v2) / (m1 + m2);
    float v2_new = ((m2 - m1) * v2 + 2 * m1 * v1) / (m1 + m2);

    v1 = v1_new;
    v2 = v2_new;
}
```

---

## 6. Chemistry Visualization

### 6.1 Flame Test Implementation (Unreal Engine 5)

Using Niagara particles with color override:

```python
# Python script for Unreal Editor automation
import unreal

def set_flame_color(element_name: str, rgb_color: unreal.Vector):
    # Load Material Parameter Collection
    mpc = unreal.load_asset("/Game/Chemistry/Materials/MPC_FlameParams")

    # Set vector parameter
    unreal.MaterialEditingLibrary.set_material_parameter_collection_vector_parameter_value(
        mpc, "FlameTint", rgb_color
    )

# Copper: Blue-Green
set_flame_color("Copper", unreal.Vector(0.0, 1.0, 0.5))
```

### 6.2 Titration Color Change (Shader Logic)

```cpp
// Blueprint/C++ logic
if (VolumeAdded < EndPoint) {
    LiquidColor = Color_Yellow;  // Methyl Orange before endpoint
} else {
    LiquidColor = Color_Pink;    // After endpoint
}
```

**Material Shader:**
```
Lerp(Yellow, Pink, saturate((VolumeAdded - EndPoint) / TransitionWidth))
```

### 6.3 Redox Reaction Visualization

```cpp
// Zinc in Copper Sulfate solution
// Blue → Colorless, Copper precipitates
float ReactionProgress = TimeSinceStart / ReactionDuration;

// Solution color
SolutionColor = Lerp(CuSO4_Blue, Water_Clear, ReactionProgress);

// Precipitate opacity
CopperPrecipitate.SetOpacity(ReactionProgress);
```

---

## 7. Manim Integration

### 7.1 Graph Overlay Generation

```python
from manim import *
import numpy as np

class PendulumGraph(Scene):
    def construct(self):
        # Transparent background for overlay
        self.camera.background_color = "#00000000"

        # Setup axes
        axes = Axes(
            x_range=[0, 10, 1],
            y_range=[-1, 1, 0.5],
            axis_config={"include_tip": False, "color": WHITE}
        ).add_coordinates()

        labels = axes.get_axis_labels(x_label="t (s)", y_label="\\theta (rad)")

        # SHM function matching Unity simulation
        g = 9.81
        L = 1.0
        func = axes.plot(
            lambda t: 0.087 * np.cos(np.sqrt(g/L) * t),
            color=YELLOW
        )

        self.add(axes, labels)
        self.play(Create(func), run_time=10, rate_func=linear)
```

### 7.2 Rendering with Transparency

```bash
# Export with alpha channel
manim -pql --transparent -o output.mov script.py PendulumGraph
```

### 7.3 Unity Integration

```csharp
// Import Manim video as texture
public class ManimOverlay : MonoBehaviour {
    public VideoPlayer videoPlayer;
    public RawImage displayImage;

    void Start() {
        videoPlayer.prepareCompleted += OnVideoPrepared;
        videoPlayer.Prepare();
    }

    void OnVideoPrepared(VideoPlayer vp) {
        displayImage.texture = vp.texture;
    }

    public void PlaySynchronized() {
        // Trigger when simulation starts
        videoPlayer.Play();
    }
}
```

### 7.4 Unreal Engine Integration

Using Media Framework:
1. Import .mov with alpha channel
2. Create Media Texture
3. Apply to UMG Widget or world-space plane
4. Synchronize with simulation start via Blueprint

---

## 8. Backend Architecture

### 8.1 SpacetimeDB for Multiplayer Simulations

SpacetimeDB unifies game server and database:

```rust
// Server module in Rust
#[spacetimedb(table)]
pub struct Player {
    #[primarykey]
    pub id: u64,
    pub position: Vector3,
    pub experiment_state: ExperimentState,
}

#[spacetimedb(reducer)]
pub fn update_pendulum_length(ctx: ReducerContext, player_id: u64, new_length: f32) {
    if let Some(mut player) = Player::filter_by_id(player_id) {
        player.experiment_state.pendulum_length = new_length;
        // Automatic state sync to all subscribers
    }
}
```

### 8.2 Unity Integration

```csharp
// SpacetimeDB Unity SDK
using SpacetimeDB;

void Start() {
    SpacetimeDBClient.instance.Connect("ws://localhost:3000");
    Player.OnInsert += OnPlayerJoined;
    Player.OnUpdate += OnPlayerStateChanged;
}

void OnPlayerStateChanged(Player oldState, Player newState) {
    // Update local simulation parameters
    pendulum.length = newState.experiment_state.pendulum_length;
}
```

### 8.3 Godot + Rust GDExtension

For maximum performance:

```rust
// Rust GDExtension with SpacetimeDB
use godot::prelude::*;
use spacetimedb_sdk::*;

#[derive(GodotClass)]
#[class(base=Node)]
pub struct NetworkManager {
    client: SpacetimeDBClient,
    #[signal]
    on_experiment_update: Signal<(i64, f32, f32)>,
}

#[godot_api]
impl NetworkManager {
    #[func]
    fn connect_to_server(&mut self, url: GString) {
        self.client.connect(url.to_string());
    }
}
```

---

## 9. Engine Comparison

### 9.1 Feature Matrix

| Feature | Unreal Engine 5 | Unity 6 | Godot 4 |
|---------|-----------------|---------|---------|
| **Render Quality** | Superior (Lumen) | Good (HDRP) | Efficient |
| **Python API** | Native | C# scripting | GDScript/@tool |
| **Normal Map Baking** | Native buffer export | Replacement shader | Viewport override |
| **Resource Usage** | High | Medium | Low |
| **Iteration Speed** | Slow | Medium | Fast |
| **SpacetimeDB** | C++ (complex) | Native SDK | Rust GDExtension |

### 9.2 Recommended Hybrid Pipeline

For indie teams:

1. **Asset Generation:** Unreal Engine 5 (render farm)
2. **Runtime Engine:** Godot 4 (2D sprite handling)
3. **Backend:** SpacetimeDB + Rust GDExtension

### 9.3 Pre-Rendered Sprite Pipeline (Hades-Style)

For performance on mid-range hardware:

```
UE5 (3D Model + Animation)
        ↓
Movie Render Queue (Lumen lighting)
        ↓
Output: Color + Normal + Emissive PNGs
        ↓
TexturePacker → Sprite Sheets
        ↓
Godot 4 (CanvasTexture with normal map)
```

**Benefits:**
- 50,000-poly characters become single quads
- Consistent visual style
- Dynamic lighting via normal maps
- Massive on-screen entity counts

---

## 10. Implementation Guide

### 10.1 Phase 1: Environment Setup

```bash
# Install tools
brew install unreal-engine  # or download from Epic
brew install godot
curl https://install.spacetimedb.com | sh

# Rust toolchain for GDExtension
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
cargo install gdext-cli
```

### 10.2 Phase 2: UE5 Baking Pipeline

```python
# render_sprites.py - Unreal Python automation
import unreal

def create_render_job(animation_path: str) -> unreal.MoviePipelineExecutorJob:
    subsystem = unreal.get_editor_subsystem(unreal.MoviePipelineQueueSubsystem)
    queue = subsystem.get_queue()

    job = queue.allocate_new_job()
    job.set_configuration(load_preset("SpriteBakePreset"))
    job.set_sequence(animation_path)

    return job

# Configure output: Multilayer EXR (Color + Normal)
preset = unreal.MoviePipelineMasterConfig()
preset.output_format = "EXR"
preset.disable_tone_curve = True  # Linear color for compositing
```

### 10.3 Phase 3: Godot Integration

```gdscript
# import_sprites.gd - EditorPlugin
@tool
extends EditorPlugin

func _on_file_system_changed():
    var raw_dir = "res://assets/sprites/raw/"
    for file in DirAccess.get_files_at(raw_dir):
        if file.ends_with("_color.png"):
            var base_name = file.trim_suffix("_color.png")
            create_canvas_texture(base_name)

func create_canvas_texture(base_name: String):
    var canvas_tex = CanvasTexture.new()
    canvas_tex.diffuse_texture = load(f"res://assets/sprites/raw/{base_name}_color.png")
    canvas_tex.normal_texture = load(f"res://assets/sprites/raw/{base_name}_normal.png")
    ResourceSaver.save(canvas_tex, f"res://assets/sprites/{base_name}.tres")
```

### 10.4 Phase 4: Backend Integration

```rust
// server/src/lib.rs - SpacetimeDB module
use spacetimedb::{spacetimedb, ReducerContext};

#[spacetimedb(table)]
pub struct ExperimentSession {
    #[primarykey]
    pub session_id: u64,
    pub experiment_type: String,
    pub parameters: String,  // JSON
    pub created_at: u64,
}

#[spacetimedb(reducer)]
pub fn create_session(ctx: ReducerContext, experiment_type: String, parameters: String) {
    ExperimentSession::insert(ExperimentSession {
        session_id: ctx.random(),
        experiment_type,
        parameters,
        created_at: ctx.timestamp.as_secs(),
    });
}
```

### 10.5 Docker Compose Deployment

```yaml
version: "3.8"

services:
  spacetimedb:
    image: clockworklabs/spacetimedb:latest
    ports:
      - "3000:3000"
    volumes:
      - stdb_data:/var/lib/spacetimedb

  manim_renderer:
    build: ./manim
    volumes:
      - ./output:/app/output

  content_server:
    image: nginx:alpine
    ports:
      - "8080:80"
    volumes:
      - ./godot_export:/usr/share/nginx/html

volumes:
  stdb_data:
```

---

## References

- Stokes Workshop: "Video game physics: All smoke and mirrors?"
- Supergiant Games GDC Talk: "The Art of Hades"
- SpacetimeDB: https://spacetimedb.com/docs
- Godot GDExtension: https://github.com/godot-rust/gdext
- Manim: https://docs.manim.community/
- Unreal MRQ: https://dev.epicgames.com/documentation/en-us/unreal-engine/movie-render-queue
