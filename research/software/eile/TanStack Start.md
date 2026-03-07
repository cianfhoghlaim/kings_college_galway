---
title: "TanStack Start"
source: "https://ui.shadcn.com/docs/installation/tanstack"
author:
  - "[[shadcn]]"
published:
created: 2025-12-29
description: "Install and configure shadcn/ui for TanStack Start."
tags:
  - "clippings"
---
Install and configure shadcn/ui for TanStack Start.

### Create project

Run the following command to create a new TanStack Start project with shadcn/ui:

```bash
bun create @tanstack/start@latest --tailwind --add-ons shadcn
```

### Add Components

You can now start adding components to your project.

```bash
bunx --bun shadcn@latest add button
```

The command above will add the `Button` component to your project. You can then import it like this:

app/routes/index.tsx

```tsx
import { Button } from "@/components/ui/button"

 

function App() {

  return (

    <div>

      <Button>Click me</Button>

    </div>

  )

}
```

If you want to add all `shadcn/ui` components, you can run the following command:

```bash
bunx --bun shadcn@latest add --all
```