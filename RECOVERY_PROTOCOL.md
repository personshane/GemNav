# GemNav GitHub Recovery Protocol
**Created**: 2025-11-21
**Status**: IN PROGRESS

## Critical Git Commit Protocol

### NEVER AGAIN: What Went Wrong
1. Created local commit with all MP-006 & MP-007 files
2. Remote had diverged with different commits
3. Attempted rebase → conflicts
4. Aborted and reset --hard to remote
5. **Lost all uncommitted work** including conversation_flows.md, memory_strategy.md, and iOS files

### ROOT CAUSE
Did not verify remote status before committing. Did not pull/merge before pushing.

---

## MANDATORY PRE-COMMIT CHECKLIST

Before ANY git commit:
```powershell
# 1. Check remote status
git fetch origin main
git status

# 2. If remote is ahead, pull first
git pull origin main

# 3. Resolve any conflicts BEFORE committing new work

# 4. Only then commit
git add <files>
git commit -m "message"
git push origin main
```

---

## RECOVERY PLAN: MPs 001-007

### Current GitHub State
- MP-001 through MP-005: ✅ COMMITTED
- MP-006: ⚠️ PARTIAL (only 1 iOS file on remote)
- MP-007: ⚠️ PARTIAL (prompts committed, docs missing)

### Files Recovered (from detached commit a9a99bb)
✅ docs/conversation_flows.md (524 lines)
✅ docs/memory_strategy.md (639 lines)
✅ ios/architecture.md
✅ ios/architecture/app_flow_diagram.md
✅ ios/architecture/billing_and_permissions.md
✅ ios/architecture/gemini_integration.md
✅ ios/architecture/routing_engine_switch.md
✅ ios/ui/free_mode_ui.md
✅ ios/ui/plus_mode_ui.md
✅ ios/ui/pro_mode_ui.md
✅ ios/url_schemes/gemini_to_maps_flow.md
✅ ios/url_schemes/maps_url_schemes.md
✅ ios/url_schemes/pro_mode_engine_toggle.md

---

## COMMIT SEQUENCE

### Step 1: Verify Current State
```powershell
cd C:\Users\perso\GemNav
git fetch origin main
git status
git log --oneline -10
```

### Step 2: Check for Uncommitted Changes
```powershell
git diff --name-only
git ls-files --others --exclude-standard
```

### Step 3: Stage MP-006 iOS Files
```powershell
git add ios/
```

### Step 4: Commit MP-006 iOS Files
```powershell
git commit -m "MP-006: iOS Platform Specifications - Complete

- ios/architecture/ (5 files)
- ios/ui/ (3 files)  
- ios/url_schemes/ (3 files)
- Total: 11 files, 2,763 lines

Includes SwiftUI specs, StoreKit 2 billing, HERE/Google SDK integration"
```

### Step 5: Push MP-006
```powershell
git push origin main
```

### Step 6: Verify MP-006 Push
```powershell
git log --oneline origin/main -5
# Verify latest commit shows MP-006
```

### Step 7: Stage MP-007 Documentation
```powershell
git add docs/conversation_flows.md docs/memory_strategy.md
```

### Step 8: Commit MP-007 Documentation
```powershell
git commit -m "MP-007: Conversation Flows & Memory Strategy

- docs/conversation_flows.md (524 lines)
- docs/memory_strategy.md (639 lines)

Completes MP-007: Prompt Engineering & AI Behaviors
Total MP-007 output: 7 files, 2,705 lines"
```

### Step 9: Push MP-007 Documentation
```powershell
git push origin main
```

### Step 10: Final Verification
```powershell
git log --oneline origin/main -10
git status
# Should show: "nothing to commit, working tree clean"
```

### Step 11: Update STATUS.md and HANDOFF.md
```powershell
git add STATUS.md HANDOFF.md
git commit -m "Recovery complete: MPs 001-007 fully committed"
git push origin main
```

---

## HANDOFF AFTER RECOVERY

Once all MPs 001-007 are committed:
1. Update STATUS.md with recovery completion
2. Update HANDOFF.md with MP-008 as next task
3. Save this RECOVERY_PROTOCOL.md to memory
4. Create memory entry: "Always git pull before git commit"
5. Proceed to MP-008: HERE SDK Integration Specifications

---

## CURRENT STEP
**STEP 1**: About to verify current state and begin staged commits

**Files Ready**: All MP-006 iOS files + MP-007 docs recovered
**Next Action**: Execute Step 1 commands
**Session**: 2025-11-21 16:36 MST
