<div align="center">

<img src=".github/assets/banner.svg" alt="devxed_test_task - QA test assignment" width="100%">

<br>

[![CI](https://github.com/DmitryDerkach/devxed_test_task/actions/workflows/ci.yml/badge.svg)](https://github.com/DmitryDerkach/devxed_test_task/actions/workflows/ci.yml)
![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![JUnit 5](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5&logoColor=white)
![REST Assured](https://img.shields.io/badge/REST%20Assured-5-4B9CD3)
![Postman](https://img.shields.io/badge/Postman-collection-FF6C37?logo=postman&logoColor=white)
![Newman](https://img.shields.io/badge/Newman-CLI-FF6C37)
![Maven Wrapper](https://img.shields.io/badge/Maven-wrapper-C71A36?logo=apachemaven&logoColor=white)
![Process: AI-DLC](https://img.shields.io/badge/process-AI--DLC-8B5CF6)

**Solution of a two-part QA test assignment: checklists first, then everything that proves they work.**

[Task 1 · Packaging](Task%201/README.md) &nbsp;•&nbsp; [Task 2 · Promo-code API](Task%202/README.md) &nbsp;•&nbsp; [Quick start](#-quick-start) &nbsp;•&nbsp; [How it was built (AI-DLC)](#-how-it-was-built-ai-dlc) &nbsp;•&nbsp; [Original assignment (PDF)](Задание.pdf)

</div>

---

## 📌 The assignment in one minute

The original task is in [`Задание.pdf`](Задание.pdf) (English text). It has two parts, and for each one **the required deliverable is a checklist**:

| | Task | What the checklist covers |
|:-:|---|---|
| 📦 | **[Task 1 · Packaging selection](Task%201/README.md)** | An algorithm that suggests to a warehouse employee the **cheapest available packaging** (box or bag) a shipment fits into. Boxes are rigid (dimensions compared position by position), bags stretch (`Max + Min ≤ length`, `Mid + Min ≤ width`). |
| 🏷️ | **[Task 2 · `POST /api/apply-promo-code`](Task%202/README.md)** | Business logic of a promo-code endpoint: gift cards are excluded from the discount, shipping is **$4.99** or **free from $35.00**, digital-only carts are not shipped, the discount is applied before shipping. |

> [!NOTE]
> The task says: *“If the description lacks some details, make your own assumptions and state them in your answer.”*
> Every gap that was found is closed by an explicit, numbered **assumption** (10 for Task 1, 13 for Task 2), and every check that depends on one says so.

---

## 🗂️ What is in the repository

```text
devxed_test_task/
├── README.md                    ← you are here
├── Задание.pdf                  ← the original assignment
├── run-all.cmd / .ps1 / .sh     ← ONE command that runs every test
├── .github/workflows/ci.yml     ← the same run on GitHub Actions (Linux + Windows)
│
├── Task 1/                      📦 packaging selection
│   ├── README.md                ← detailed write-up: rules, diagrams, techniques, assumptions
│   ├── Checklist_EN.xlsx        ★ MAIN DELIVERABLE (31 checks)
│   ├── Checklist_EN.md            the same checklist, readable right here on GitHub
│   ├── images/                    diagrams used by the README
│   └── Additional/
│       ├── Checklist_RU.xlsx      Russian version of the checklist
│       ├── Test cases/            44 test cases (EN + RU), Azure DevOps import layout
│       └── Automation/            Java 17 · JUnit 5 · AssertJ · mutation check (79 tests)
│
└── Task 2/                      🏷️ POST /api/apply-promo-code
    ├── README.md                ← detailed write-up
    ├── Checklist_EN.xlsx        ★ MAIN DELIVERABLE (34 checks)
    ├── Checklist_EN.md
    └── Additional/
        ├── Checklist_RU.xlsx
        ├── Test cases/            52 test cases (EN + RU), Azure DevOps import layout
        ├── Automation/            reference service + REST Assured tests + mutation check (74 tests)
        └── Postman/               collection (48 requests, 267 assertions) + environment
```

| | Task 1 📦 | Task 2 🏷️ |
|---|:-:|:-:|
| ★ Checklist (EN, main deliverable) | **31 checks** | **34 checks** |
| Assumptions | 10 | 13 |
| Test cases (Azure DevOps layout, EN + RU) | 44 | 52 |
| Automated Java tests | 79 | 74 |
| Deliberately broken versions (mutants) the tests must catch | 16 / 16 caught | 17 / 17 caught |
| Postman / Newman | – | 48 requests · 267 assertions |

Each sheet of `Checklist_EN.xlsx`: **Checklist** (check · expected result · why it matters · technique · priority · linked test cases), **Assumptions**, **Techniques** (where each test-design technique was used, and where it deliberately was not).

---

## 🚀 Quick start

You need **JDK 17+** (Maven is downloaded automatically by the wrapper) and **Node.js 18+** (only for the Postman/Newman part, it is skipped with a warning if Node is missing).

```bash
git clone https://github.com/DmitryDerkach/devxed_test_task.git
cd devxed_test_task
```

| OS | Run everything |
|---|---|
| 🪟 Windows | double-click **`run-all.cmd`** &nbsp;or&nbsp; `powershell -ExecutionPolicy Bypass -File .\run-all.ps1` |
| 🍎 macOS / 🐧 Linux / Git Bash | `./run-all.sh` |

What happens:

```mermaid
flowchart LR
    A([run-all]) --> B{JDK 17+?}
    B -- no --> X([tell what to install])
    B -- yes --> C["Task 1<br/>79 JUnit tests"]
    C --> D["Task 2<br/>74 REST Assured tests"]
    D --> E["start the reference<br/>service on :8080"]
    E --> F["Newman runs the<br/>Postman collection"]
    F --> G["stop the service"]
    G --> H([summary ✔ / ✘])

    classDef ok fill:#d9f2e6,stroke:#2f9e73,color:#1a1a1a
    classDef step fill:#eef3f9,stroke:#5b7fa6,color:#1a1a1a
    classDef dec fill:#fff6d6,stroke:#c9a227,color:#1a1a1a
    class A,H ok
    class C,D,E,F,G step
    class B dec
```

Expected output (a real run):

```text
▶ Task 1 · Packaging selector (JUnit 5 + AssertJ + mutation check)
  ✔ Task 1 · Java tests: 79 tests, 0 failed  (5 s)
▶ Task 2 · Promo-code API (REST Assured + mutation check)
  ✔ Task 2 · Java tests: 74 tests, 0 failed  (11 s)
▶ Task 2 · Postman collection (Newman against the reference service)
  ✔ service is up
  ✔ Newman: 48 requests, 267 assertions, 0 failed

  Summary
  ──────────────────────────────────────────────────────────
  ✔  Task 1 · Java tests         79 tests, 0 failed
  ✔  Task 2 · Java tests         74 tests, 0 failed
  ✔  Task 2 · Postman / Newman   48 requests, 267 assertions, 0 failed
  ──────────────────────────────────────────────────────────
  All green.
```

The first run downloads Maven and the dependencies (~1–2 min); later runs take ~30 s. Logs land in `.run-logs/`, JUnit reports in `**/target/surefire-reports/`. Port busy? `PORT=8090 ./run-all.sh` or `.\run-all.ps1 -Port 8090`.

Prefer to run one part by hand, open the Postman collection in the Postman app, or import the test cases into Azure DevOps? Each task README has a step-by-step section.

---

## 🤖 How it was built (AI-DLC)

Instead of the classic STLC (*requirements → planning → design → execution → closure*, one long hand-made pass), the work followed the **AI-Driven Development Life Cycle (AI-DLC)** idea: an AI agent does the heavy lifting in short iterations, and a human owns every decision at explicit checkpoints. The agent was Claude Code, the human was the author of this repository.

```mermaid
flowchart LR
    subgraph I["① Inception"]
        direction TB
        I1["AI reads the spec,<br/>lists every gap"] --> I2{{"Human:<br/>accept / change<br/>assumptions"}}
    end
    subgraph C["② Construction"]
        direction TB
        C1["AI drafts checklist<br/>(EP · BVA · DT · EG)"] --> C2["AI derives test cases<br/>+ automation + Postman"]
        C2 --> C3{{"Human review:<br/>wording, scope,<br/>expected results"}}
        C3 -- "rework" --> C1
    end
    subgraph O["③ Operations"]
        direction TB
        O1["mutation check:<br/>do the tests catch bugs?"] --> O2["one-command run<br/>+ CI on every push"]
    end
    I --> C --> O

    classDef ai fill:#eef3f9,stroke:#5b7fa6,color:#1a1a1a
    classDef human fill:#fff6d6,stroke:#c9a227,color:#1a1a1a
    classDef ops fill:#d9f2e6,stroke:#2f9e73,color:#1a1a1a
    class I1,C1,C2 ai
    class I2,C3 human
    class O1,O2 ops
```

| AI-DLC phase | What happened here | Where to see it |
|---|---|---|
| **① Inception**: intent → questions → decisions | The spec was read line by line; every place where two testers could expect different results became an open question with a proposed answer. The human accepted or changed each one. | *Assumptions* sheet of each checklist |
| **② Construction**: short build/review loops (“bolts”) | Checklist first (the real deliverable), then test cases and automation **derived from the same scenarios**, so they cannot disagree. Human review drove several rework loops, e.g. neutral packaging names, a precondition step instead of “request is prepared”, expected results written out with full arithmetic, and no invented response texts. | `Checklist_EN.*`, `Additional/Test cases/` |
| **③ Operations**: prove it, keep it green | 33 deliberately broken versions of the code (`>` instead of `≥`, gift cards discounted, threshold after discount …) must each be caught by the suite, otherwise the checklist is not sharp enough. One-command run + CI on Linux and Windows. | `Additional/Automation/`, `run-all.*`, Actions tab |

**Why it matters for QA:** the AI made breadth cheap (every boundary, every combination, two languages, executable proof), while the human kept the parts that need judgement: what the spec *means*, which risks matter, and what a good test case reads like.

---

## ⚖️ Honest notes

* **There is no real system behind the assignment.** To make the checklists executable, each task has a small **reference implementation** of the spec *plus the stated assumptions*. Green tests mean *“the checklist is executable and catches real defects”* (see the mutants), not *“someone else’s backend is correct”*. Point the Postman environment at a real server to test one.
* Response **texts** (`message`) are not defined by the assignment. The test cases therefore do not prescribe them; the reference service uses its own texts, and only the automation checks them.
* The checklist is the main deliverable; everything under `Additional/` is supporting material.

<div align="center">
<br>

**[📦 Task 1 README →](Task%201/README.md)** &nbsp;&nbsp;&nbsp; **[🏷️ Task 2 README →](Task%202/README.md)**

</div>
