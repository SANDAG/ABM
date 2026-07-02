from dataclasses import dataclass, field
from typing import List, Dict
import math


# ------------------------------------------------------------------
# Data model (direct mapping of tcov.inc aat* fields used by TCHC)
# ------------------------------------------------------------------

@dataclass
class TCHCLink:
    # identifiers
    id: int
    name: str
    length_ft: float
    fc: int              # functional class
    hov: int             # 1=mix, 2=hov2, 3=hov3, 4=toll
    jur: int             # jurisdiction
    med: int             # median type
    way: int             # 1=one-way, 2=two-way
    adt_id: int
    station: int
    project: int

    # nodes
    node_from: int
    node_to: int

    # lanes: [period][dir]
    lanes: List[List[int]] = field(default_factory=lambda: [[0, 0], [0, 0], [0, 0]])
    aux_lanes: List[int] = field(default_factory=lambda: [0, 0])

    # control / turn lanes
    control: List[int] = field(default_factory=lambda: [0, 0])
    tl: List[int] = field(default_factory=lambda: [0, 0])
    rl: List[int] = field(default_factory=lambda: [0, 0])
    ll: List[int] = field(default_factory=lambda: [0, 0])
    gc: List[int] = field(default_factory=lambda: [0, 0])

    # per‑mile tolls → converted in place
    toll: List[int] = field(default_factory=lambda: [0, 0, 0])

    # outputs
    lt: List[List[float]] = field(default_factory=lambda: [[999, 999], [999, 999], [999, 999]])
    xt: List[List[float]] = field(default_factory=lambda: [[0, 0], [0, 0], [0, 0]])
    hc: List[List[float]] = field(default_factory=lambda: [[0, 0], [0, 0], [0, 0]])
    lc: List[List[float]] = field(default_factory=lambda: [[999999, 999999], [999999, 999999], [999999, 999999]])
    xc: List[List[float]] = field(default_factory=lambda: [[999999, 999999], [999999, 999999], [999999, 999999]])
    tc: List[float] = field(default_factory=lambda: [999999, 999999])

    auto_cost: float = 0.0


# ------------------------------------------------------------------
# Context (what FORTRAN pulled from files & COMMON blocks)
# ------------------------------------------------------------------

@dataclass
class TCHCContext:
    aoc: float
    mlcaprate: float
    fwycaprate: float
    year: int

    # node‑based lookups
    approach_count: Dict[int, int]        # xdapp
    ramp_meter_dir: Dict[int, int]        # adtmtr
    station_ppf: List[List[List[float]]]  # [period][dir][station]

    rsa_factor: Dict[int, float]

    gcsig: List[List[List[int]]]           # [iapp][fc][to_fc]
    gcstp4: List[List[int]]
    gcstp2: List[int]

    border_delay: List[List[List[float]]] # [border][period][dir]


# ------------------------------------------------------------------
# Utility helpers
# ------------------------------------------------------------------

def miles(ft: float) -> float:
    return ft / 5280.0


def direction_from_name(name: str) -> int:
    if "SB" in name: return 1
    if "EB" in name: return 2
    if "NB" in name: return 3
    if "WB" in name: return 4
    return 0


# ------------------------------------------------------------------
# ✅ Full TCHC computational procedure
# ------------------------------------------------------------------

def apply_tchc(link: TCHCLink, ctx: TCHCContext, rem_toll=None):
    if rem_toll is None:
        rem_toll = [0.0, 0.0, 0.0]

    dist = miles(link.length_ft)
    tsm = ctx.year > 2015

    # ---- toll conversion (FORTRAN 978–985)
    for p in range(3):
        raw = link.toll[p] * dist + rem_toll[p]
        rounded = int(round(raw))
        rem_toll[p] = raw - rounded
        if link.toll[p] > 0 and rounded == 0:
            rounded = 1
        link.toll[p] = rounded

    # auto operating cost
    link.auto_cost = dist * ctx.aoc

    for d in range(2):
        if link.way == 1 and d == 1:
            continue

        node = link.node_from if d == 0 else link.node_to
        iapp = ctx.approach_count.get(node, 3)

        for p in range(3):
            ln = link.lanes[p][d]
            if ln == 9:
                continue

            speed = 35.0
            time_min = dist * 60.0 / speed
            link.lt[p][d] = time_min

            # peak‑period factor
            if link.fc == 1:
                ppf_dir = 1 if ("NB" in link.name or "WB" in link.name) else 0
            else:
                ppf_dir = d

            station = link.station if link.station > 0 else 1
            ppf = ctx.station_ppf[p][ppf_dir][station]

            # base capacity
            if link.fc == 1:
                fwycap = max(min(2000, 2100), 1900)
                cap = ln * fwycap + link.aux_lanes[d] * 1200
                if link.hov == 1:
                    cap *= ctx.fwycaprate
                if link.hov in (2, 3):
                    cap = ln * 2000
                if link.hov == 3:
                    cap *= ctx.mlcaprate
                if link.project in (613, 614):
                    cap *= ctx.mlcaprate

                if tsm:
                    mdir = ctx.ramp_meter_dir.get(link.adt_id, 0)
                    if mdir == 9 or mdir == direction_from_name(link.name):
                        cap *= 1.10

            elif link.fc == 8:
                cap = ln * 1800
            elif link.fc == 9:
                cap = ln * 1200
            else:
                cap = ln * 1800 - 300
                if link.med < 2:
                    cap -= 200

            link.hc[p][d] = cap
            link.lc[p][d] = cap * ppf

            # ---- intersection / control logic
            ith, irt, ilf = link.tl[d], link.rl[d], link.ll[d]
            ctrl = link.control[d]

            if ctrl == 1:  # signal
                link.xt[p][d] = 0.17
                g = link.gc[d] or ctx.gcsig[min(iapp,4)-1][link.fc-1][6]
                gfac = g / 100.0
                cap = max(1000, ith * 1800 * gfac + (irt + ilf) * 250)
                cap *= ctx.rsa_factor.get(link.jur, 1.0)
                link.xc[p][d] = cap * ppf
                link.hc[p][d] = cap

            elif ctrl == 2:  # 4‑way stop
                link.xt[p][d] = 0.20
                g = link.gc[d] or ctx.gcstp4[link.fc-1][6]
                gfac = g / 100.0
                cap = max(500, ith * 1800 * gfac + (irt + ilf) * 250)
                link.xc[p][d] = cap * ppf
                link.hc[p][d] = cap

            elif ctrl == 3:  # 2‑way stop
                link.xt[p][d] = 0.20
                g = ctx.gcstp2[6] / 100.0
                cap = max(500, (ith + irt + ilf) * 500 * g)
                link.xc[p][d] = cap * ppf
                link.hc[p][d] = cap

            elif ctrl == 6:  # rail crossing
                link.xt[p][d] = 0.02

            elif ctrl == 7:  # toll / border
                ith = max(ith, ln)
                cap = ith * 500
                link.hc[p][d] = cap
                link.xc[p][d] = cap * ppf
                link.xt[p][d] = 1.0

        # generalized cost (FORTRAN 1232)
        peak = 0
        link.tc[d] = (
            link.auto_cost +
            (link.lt[peak][d] + link.xt[peak][d]) * 35 +
            (link.toll[0] + link.toll[1]) / 2
        )
        link.tc[d] = min(link.tc[d], 999999)

    return rem_toll
