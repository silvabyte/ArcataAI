# Migration to silvabyte/ArcataAI Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Migrate the current codebase (from `jobify` branch) to a new GitHub repository `silvabyte/ArcataAI` under the `main` branch.

**Architecture:** Create new remote repo, clone to target directory, copy files (excluding `.git`), and push to new remote.

**Tech Stack:** GitHub CLI, git, bash.

### Task 1: Create silvabyte/ArcataAI repo

**Files:**
- None (Remote operation)

**Step 1: Check if repo exists**
Run: `gh repo view silvabyte/ArcataAI`
Expected: Error (not found)

**Step 2: Create repo**
Run: `gh repo create silvabyte/ArcataAI --public --description "Arcata AI Platform" --confirm`
(Use `--private` if preferred, assuming public/private based on current or user preference. Defaulting to private for safety if not specified, or public if open source. I'll use `--private` to be safe unless instructed otherwise, or maybe `--public` since the user said "opencode" might imply open source? The current repo `arcata-saas` visibility isn't explicitly known but I can check. I'll assume private for safety.)
Actually, I will use `gh repo create silvabyte/ArcataAI --private` to be safe.

**Step 3: Verify creation**
Run: `gh repo view silvabyte/ArcataAI`
Expected: Success

### Task 2: Clone ArcataAI repo

**Files:**
- Directory: `/home/matsilva/code/silvabyte/ArcataAI`

**Step 1: Ensure parent directory exists**
Run: `mkdir -p /home/matsilva/code/silvabyte`

**Step 2: Clone repo**
Run: `gh repo clone silvabyte/ArcataAI /home/matsilva/code/silvabyte/ArcataAI`

**Step 3: Verify clone**
Run: `ls -la /home/matsilva/code/silvabyte/ArcataAI/.git`
Expected: Exists

### Task 3: Port code from jobify branch

**Files:**
- Source: `/home/matsilva/code/silvabyte/arcata-saas` (current)
- Dest: `/home/matsilva/code/silvabyte/ArcataAI`

**Step 1: Copy files**
Run: `rsync -av --progress --exclude '.git' --exclude 'node_modules' --exclude '.mill-bsp' --exclude '.bloop' --exclude 'out' /home/matsilva/code/silvabyte/arcata-saas/ /home/matsilva/code/silvabyte/ArcataAI/`
(Using rsync to exclude git and build artifacts)

**Step 2: Verify copy**
Run: `ls -la /home/matsilva/code/silvabyte/ArcataAI`

### Task 4: Initial commit and push to main

**Files:**
- Repo: `/home/matsilva/code/silvabyte/ArcataAI`

**Step 1: Stage files**
Run: `cd /home/matsilva/code/silvabyte/ArcataAI && git add .`

**Step 2: Commit**
Run: `cd /home/matsilva/code/silvabyte/ArcataAI && git commit -m "feat: initial migration from arcata-saas"`

**Step 3: Push**
Run: `cd /home/matsilva/code/silvabyte/ArcataAI && git push -u origin main`
