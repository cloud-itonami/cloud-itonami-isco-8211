(ns mechassemblycoord.governor
  "MechAssemblyCoordGovernor — the independent safety/scope layer
  gating every assembly-line scheduling/logistics proposal an advisor
  may make for a mechanical machinery assembler crew. The governor
  never dispatches hardware itself, never performs assembly work
  itself, and never finalizes an assembly-execution decision (e.g.
  deciding to proceed with a specific assembly run) or a
  line-safety-clearance decision (e.g. declaring an assembly line
  safety cleared), and never overrides a plant safety officer's
  judgment — those are permanently out of this actor's scope and
  remain a plant safety officer's exclusive judgment (README's
  'Robotics premise': this actor coordinates LINE SCHEDULING/
  LOGISTICS ONLY — it never performs assembly work itself). Modeled
  closely on cloud-itonami-isco-8122's platingcoord.governor.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. assembler provenance   — the crew member must be independently
                                verified/registered before any action.
    2. line provenance       — the assembly line/workshop station
                                must be independently verified/
                                registered before any action.
    3. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never performs assembly work itself;
                                it only gates what the advisor may
                                coordinate).
    4. closed op-allowlist    — only :log-work-record,
                                :schedule-crew-operation,
                                :flag-safety-concern and
                                :coordinate-supply-order may ever be
                                proposed; anything else is refused.
    5. scope-excluded action  — any proposal to directly finalize an
                                assembly-execution decision (e.g.
                                deciding to proceed with a specific
                                assembly run), or a line-safety-
                                clearance decision (e.g. declaring an
                                assembly line safety cleared), or to
                                override a plant safety officer's
                                judgment, is a hard, permanent block
                                (checked both against the proposed :op
                                and, defense-in-depth, against the
                                proposal's :rationale text — matched as
                                full finalization/execution ACTION
                                phrases such as \"finalize the assembly
                                operation\" / \"declare the line safety
                                cleared\" / \"override the plant safety
                                officer's judgment\", never as bare
                                nouns like \"assembly\", \"fixture\" or
                                \"component\", so the check can never
                                self-trip on the advisor's own routine
                                rationale text, e.g. \"logged work
                                record for assembler …\" or \"scheduled
                                crew operation for assembly line task
                                …\" or \"…routed for plant safety
                                officer review\" — all three
                                legitimately contain bare domain nouns
                                but none is a finalization action, and
                                all are exercised by
                                `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    6. :op :flag-safety-concern (a pinch-point-hazard / tool-hazard /
                                equipment-condition concern always
                                escalates to a human, never
                                auto-commits).
    7. :op :coordinate-supply-order above `supply-cost-threshold`.
    8. low confidence (< `confidence-floor`).

  This actor coordinates assembly-line scheduling/logistics ONLY — it
  never performs assembly work itself, and it never makes a line-
  safety-clearance decision itself; those decisions always route to a
  human plant safety officer, either via a hard permanent block on the
  op-allowlist (rules 4/5 above) or via a mandatory escalation (rule 6
  above)."
  (:require [kotoba.lang.text :as str]
            [mechassemblycoord.store :as store]))

(def confidence-floor 0.6)
(def supply-cost-threshold 2000)

(def allowed-ops
  #{:log-work-record :schedule-crew-operation
    :flag-safety-concern :coordinate-supply-order})

;; Defense-in-depth: none of these ops are ever in `allowed-ops`
;; above, so they are already refused by the closed-allowlist check
;; below; they are named again here — as explicit finalization/
;; execution ACTIONS, never bare nouns — so a future allowlist edit
;; cannot silently re-open this specific out-of-scope path without
;; also touching this list.
(def ^:private scope-excluded-ops
  #{:finalize-assembly-decision :finalize-assembly-operation
    :authorize-assembly-run
    :proceed-with-assembly-run
    :finalize-line-safety-clearance
    :declare-line-safety-cleared
    :declare-assembly-line-safety-cleared
    :declare-line-cleared-for-operation
    :clear-line-for-operation
    :override-plant-safety-officer-judgment
    :override-safety-officer-judgment})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; ("assembly", "fixture", "component", "pinch-point", "crush hazard",
;; "line", "plant", "safety", "officer") — so this can never match
;; inside the mock advisor's own default rationale text (which
;; legitimately contains those bare nouns, e.g. "assembly line task" /
;; "plant safety officer review"). See
;; `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
(def ^:private scope-excluded-phrases
  ["proceed with the assembly run" "proceed with the assembly operation"
   "authorize the assembly run" "authorize the assembly operation"
   "finalize the assembly decision" "finalize the assembly operation"
   "declare the line safety cleared" "declare the assembly line safety cleared"
   "declare the line cleared for operation" "clear the line for operation"
   "finalize the line safety clearance" "finalize the line-safety clearance"
   "override the plant safety officer's judgment"
   "override the safety officer's judgment"
   "override plant safety officer judgment"])

(defn- contains-excluded-phrase? [s]
  (let [s (str/lower (or s ""))]
    (boolean (some #(str/includes? s %) scope-excluded-phrases))))

(defn- hard-violations [proposal assembler-record line-record]
  (let [{:keys [op rationale]} proposal]
    (cond-> []
      (nil? assembler-record)
      (conj {:rule :no-assembler
             :detail "未登録 assembler への提案は不可（assembler record は独立して検証・登録済みでなければならない）"})

      (nil? line-record)
      (conj {:rule :no-line
             :detail "未登録 line への提案は不可（line record は独立して検証・登録済みでなければならない）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は組立作業を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op
             :detail (str op " は closed op-allowlist に無い — 提案不可")})

      (or (contains? scope-excluded-ops op) (contains-excluded-phrase? rationale))
      (conj {:rule :scope-excluded-action
             :detail "組立作業実行判断・ライン安全(line-safety)クリアランス判断の確定、および plant safety officer の判断の上書きは、この actor の権限外 — 常に永続ブロック"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `mechassemblycoord.store/Store`. Pure — never
  mutates the store, never dispatches an assembly-line operation."
  [request _context proposal store]
  (let [assembler-record (store/assembler store (:assembler-id request))
        line-record (some->> (:line-id proposal) (store/line store))
        hard (hard-violations proposal assembler-record line-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        supply-order-over-threshold?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-threshold))
        always-risky? (or (= :flag-safety-concern (:op proposal))
                           supply-order-over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
