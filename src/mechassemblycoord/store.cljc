(ns mechassemblycoord.store
  "SSoT for the ISCO-08 8211 Mechanical Machinery Assemblers assembly-line
  scheduling/logistics coordination actor (itonami actor pattern,
  ADR-2607121000 / CLAUDE.md Actors section; README's 'Robotics
  premise' — a line scheduling/logistics coordination robot performs
  crew scheduling, production-run/inventory/progress-record logging
  and components/fasteners-stock supply-order coordination for a
  mechanical machinery assembly crew under this advisor/governor pair,
  which never dispatches hardware itself, never performs assembly work
  itself, and never finalizes an assembly-execution decision or a
  line-safety-clearance decision, and never overrides a plant safety
  officer's judgment — those remain the plant safety officer's
  exclusive judgment). Modeled closely on cloud-itonami-isco-8122's
  platingcoord.store.

  Domain:

    assembler — a registered mechanical machinery assembler crew
                member (:assembler-id, :name)
    line      — a registered assembly line/workshop station
                {:line-id :name :max-supply-cost}. `:max-supply-cost`
                is an informational registered ceiling used only to
                decide whether a `:coordinate-supply-order` proposal
                escalates to human sign-off (the governor never blocks
                a within-threshold order outright; it only decides
                commit vs. escalate).
    record    — a committed operating record (a logged production-run/
                inventory/progress entry, a scheduled crew/shift
                operation, a flagged safety concern, or a coordinated
                components/fasteners-stock supply order) — written
                ONLY via commit-record!. This actor coordinates
                assembly-line scheduling/logistics ONLY — a `record`
                is a coordination artifact, never an assembly-
                execution act, never a line-safety-clearance decision,
                and never a plant safety officer's-judgment override.
    ledger    — append-only audit trail, commit or hold.")

(defprotocol Store
  (assembler [s assembler-id])
  (line [s line-id])
  (records-of [s assembler-id])
  (ledger [s])
  (register-assembler! [s assembler])
  (register-line! [s line])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (assembler [_ assembler-id] (get-in @a [:assemblers assembler-id]))
  (line [_ line-id] (get-in @a [:lines line-id]))
  (records-of [_ assembler-id] (filter #(= assembler-id (:assembler-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-assembler! [s asm]
    (swap! a assoc-in [:assemblers (:assembler-id asm)] asm) s)
  (register-line! [s l]
    (swap! a assoc-in [:lines (:line-id l)] l) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:assemblers {} :lines {} :records [] :ledger []}
                                    seed)))))
