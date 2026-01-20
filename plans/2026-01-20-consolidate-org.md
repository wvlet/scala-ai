# Plan: Consolidate Maven Organization to org.wvlet.uni

## Objective

Change the Maven organization from `org.wvlet` to `org.wvlet.uni` for all Uni modules to better distinguish them from other wvlet projects (like Airframe). Also update version references to `2026.1.0`.

## Files to Modify

### 1. Build Configuration
- **`build.sbt`** (line 16)
  - Change: `organization := "org.wvlet"` → `organization := "org.wvlet.uni"`

### 2. Documentation Files

| File | Lines | Change |
|------|-------|--------|
| `README.md` | 15 | `"org.wvlet"` → `"org.wvlet.uni"` |
| `docs/index.md` | 40, 44 | Both sbt and Scala CLI examples + version |
| `docs/guide/installation.md` | 14, 17, 20, 29, 32 | All module examples + version |
| `docs/agent/index.md` | 79, 82 | uni-agent and uni-bedrock + version |
| `docs/agent/bedrock.md` | 10 | uni-agent-bedrock + version |
| `docs/uni-walkthrough.md` | 23 | uni example + version |
| `docs/.vitepress/theme/HomeHeroCode.vue` | 14 | Hero code snippet + version |

**Note:** `docs/core/unitest.md` already uses `org.wvlet.uni` - no change needed for org.

## Changes Summary

### Before
```scala
libraryDependencies += "org.wvlet" %% "uni" % "2025.1.0"
//> using dep org.wvlet::uni:2025.1.0
```

### After
```scala
libraryDependencies += "org.wvlet.uni" %% "uni" % "2026.1.0"
//> using dep org.wvlet.uni::uni:2026.1.0
```

## Implementation Steps

1. Update `build.sbt` organization setting
2. Update all documentation files with new organization and version `2026.1.0`
3. Run `./sbt scalafmtAll` to ensure formatting
4. Run `./sbt compile` to verify build works
5. Commit and create PR

## Verification

1. `./sbt compile` - Verify project compiles
2. `./sbt test` - Run tests to ensure nothing is broken
3. `npm run docs:dev` - Verify documentation site builds correctly
4. Review all changed files for consistency

## Impact

- Users will need to update their `build.sbt` dependencies when upgrading
- This is a breaking change for existing users
- The change aligns uni modules under a dedicated organization namespace
