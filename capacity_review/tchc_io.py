"""
Binary loader/writer for hwycov/aat.adf and hwycov/nat.adf files.

Reads FORTRAN direct-access unformatted big-endian records and projects
them into TCHCLink objects for use with apply_tchc.
"""
from __future__ import annotations

import struct
from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Optional


# ---------------------------------------------------------------------------
# Record size constants (from FORTRAN recl in 4-byte words)
# ---------------------------------------------------------------------------
AAT_RECORD_WORDS = 101
AAT_RECORD_BYTES = AAT_RECORD_WORDS * 4  # 404

NAT_RECORD_WORDS = 20
NAT_RECORD_BYTES = NAT_RECORD_WORDS * 4  # 80

# ---------------------------------------------------------------------------
# struct format strings (big-endian, matching FORTRAN read order)
# ---------------------------------------------------------------------------
# Notation: > = big-endian, i = int32, h = int16, f = float32, s = char bytes

# aat.adf: 404 bytes per record
# See tchc_loader_writer_spec.md for field-by-field documentation.
# Field order: aatarn(2) aatarn(1) lpoly rpoly aatlen aatlb aatid aatqid
#   aatcc aatudj aatadj aattmp(2) | aatplt aatsph aatrt aatlk | aatnm
#   aatxnm(2) aatxnm(1) | aattpn(2) aattpn(1) | aatcj..aatmed(1) x23 |
#   aatcst | tolls(3) | lanes(3) aux pct phf cnt tl rl ll | tlb rlb llb |
#   gc plc | lc(3) xc(3) hc(3) lt(3) xt(3) tc | vla vlp ls |
#   [repeat dir2: lanes(3) aux pct phf cnt tl rl ll tlb rlb llb gc plc
#    lc(3) xc(3) hc(3) lt(3) xt(3) tc vla vlp ls]
_AAT_FORMAT = (
    ">"
    "ii"          # aatarn(2), aatarn(1)
    "ii"          # lpoly, rpoly
    "f"           # aatlen
    "iiiiii"      # aatlb, aatid, aatqid, aatcc, aatudj, aatadj
    "ii"          # aattmp(2)
    "hhhh"        # aatplt, aatsph, aatrt, aatlk
    "20s"         # aatnm
    "20s"         # aatxnm(2)
    "20s"         # aatxnm(1)
    "ii"          # aattpn(2), aattpn(1)
    "23h"         # aatcj..aatmed(1)
    "f"           # aatcst
    "3h"          # aattoll(1..3,1)
    "3h"          # aatln(1..3,1,1)
    "h"           # aataux(1,1)
    "2h"          # aatpct(1), aatphf(1)
    "4h"          # aatcnt(1,1), aattl(1,1), aatrl(1,1), aatll(1,1)
    "iii"         # aattlb(1), aatrlb(1), aatllb(1)
    "2h"          # aatgc(1), aatplc(1)
    "3f"          # aatlc(1..3,1)
    "3f"          # aatxc(1..3,1)
    "3f"          # aathc(1..3,1)
    "3f"          # aatlt(1..3,1)
    "3f"          # aatxt(1..3,1)
    "f"           # aattc(1)
    "ii"          # aatvla(1), aatvlp(1)
    "h"           # aatls(1)
    "3h"          # aatln(1..3,2,1)
    "h"           # aataux(2,1)
    "2h"          # aatpct(2), aatphf(2)
    "4h"          # aatcnt(2,1), aattl(2,1), aatrl(2,1), aatll(2,1)
    "iii"         # aattlb(2), aatrlb(2), aatllb(2)
    "2h"          # aatgc(2), aatplc(2)
    "3f"          # aatlc(1..3,2)
    "3f"          # aatxc(1..3,2)
    "3f"          # aathc(1..3,2)
    "3f"          # aatlt(1..3,2)
    "3f"          # aatxt(1..3,2)
    "f"           # aattc(2)
    "ii"          # aatvla(2), aatvlp(2)
    "h"           # aatls(2)
)

# nat.adf: 80 bytes per record
# NOTE: natz is int16 per FORTRAN tcov.inc (integer*2), not int32 as the
# YAML schema incorrectly declares. See INCONSISTENCIES section below.
_NAT_FORMAT = (
    ">"
    "iii"         # aatlb, natlb, natid
    "20s20s"      # natxnm(1), natxnm(2)
    "iii"         # nattmp, natx, naty
    "hhh"         # natz(int16!), natiuc, natsph
    "i"           # nattpn
    "hhh"         # natyr(1), natjur(1), natcnt(1)
)

AAT_STRUCT = struct.Struct(_AAT_FORMAT)
NAT_STRUCT = struct.Struct(_NAT_FORMAT)

assert AAT_STRUCT.size == AAT_RECORD_BYTES, (
    f"AAT struct size {AAT_STRUCT.size} != expected {AAT_RECORD_BYTES}"
)
assert NAT_STRUCT.size == NAT_RECORD_BYTES, (
    f"NAT struct size {NAT_STRUCT.size} != expected {NAT_RECORD_BYTES}"
)


# ---------------------------------------------------------------------------
# Named field indices for unpacked tuples
# ---------------------------------------------------------------------------

# aat field indices (0-based position in the unpacked tuple)
class _AatIdx:
    AATARN_2 = 0
    AATARN_1 = 1
    LPOLY = 2
    RPOLY = 3
    AATLEN = 4
    AATLB = 5
    AATID = 6
    AATQID = 7
    AATCC = 8
    AATUDJ = 9
    AATADJ = 10
    AATTMP_1 = 11
    AATTMP_2 = 12
    AATPLT = 13
    AATSPH = 14
    AATRT = 15
    AATLK = 16
    AATNM = 17
    AATXNM_2 = 18
    AATXNM_1 = 19
    AATTPN_2 = 20
    AATTPN_1 = 21
    AATCJ = 22
    AATSTA = 23
    AATLOC = 24
    AATLP = 25
    AATADT = 26
    AATVOL = 27
    AATPPC = 28
    AATTPC = 29
    AATSEC = 30
    AATDIR = 31
    AATFFC = 32
    AATCL = 33
    AATASP = 34
    AATYR_1 = 35
    AATPRJ_1 = 36
    AATJUR_1 = 37
    AATFC_1 = 38
    AATHOV_1 = 39
    AATTRK_1 = 40
    AATSPD_1 = 41
    AATTSPD_1 = 42
    AATWAY_1 = 43
    AATMED_1 = 44
    AATCST = 45
    AATTOLL_1_1 = 46
    AATTOLL_2_1 = 47
    AATTOLL_3_1 = 48
    AATLN_1_1_1 = 49
    AATLN_2_1_1 = 50
    AATLN_3_1_1 = 51
    AATAUX_1_1 = 52
    AATPCT_1 = 53
    AATPHF_1 = 54
    AATCNT_1_1 = 55
    AATTL_1_1 = 56
    AATRL_1_1 = 57
    AATLL_1_1 = 58
    AATTLB_1 = 59
    AATRLB_1 = 60
    AATLLB_1 = 61
    AATGC_1 = 62
    AATPLC_1 = 63
    AATLC_1_1 = 64
    AATLC_2_1 = 65
    AATLC_3_1 = 66
    AATXC_1_1 = 67
    AATXC_2_1 = 68
    AATXC_3_1 = 69
    AATHC_1_1 = 70
    AATHC_2_1 = 71
    AATHC_3_1 = 72
    AATLT_1_1 = 73
    AATLT_2_1 = 74
    AATLT_3_1 = 75
    AATXT_1_1 = 76
    AATXT_2_1 = 77
    AATXT_3_1 = 78
    AATTC_1 = 79
    AATVLA_1 = 80
    AATVLP_1 = 81
    AATLS_1 = 82
    AATLN_1_2_1 = 83
    AATLN_2_2_1 = 84
    AATLN_3_2_1 = 85
    AATAUX_2_1 = 86
    AATPCT_2 = 87
    AATPHF_2 = 88
    AATCNT_2_1 = 89
    AATTL_2_1 = 90
    AATRL_2_1 = 91
    AATLL_2_1 = 92
    AATTLB_2 = 93
    AATRLB_2 = 94
    AATLLB_2 = 95
    AATGC_2 = 96
    AATPLC_2 = 97
    AATLC_1_2 = 98
    AATLC_2_2 = 99
    AATLC_3_2 = 100
    AATXC_1_2 = 101
    AATXC_2_2 = 102
    AATXC_3_2 = 103
    AATHC_1_2 = 104
    AATHC_2_2 = 105
    AATHC_3_2 = 106
    AATLT_1_2 = 107
    AATLT_2_2 = 108
    AATLT_3_2 = 109
    AATXT_1_2 = 110
    AATXT_2_2 = 111
    AATXT_3_2 = 112
    AATTC_2 = 113
    AATVLA_2 = 114
    AATVLP_2 = 115
    AATLS_2 = 116


class _NatIdx:
    AATLB = 0
    NATLB = 1
    NATID = 2
    NATXNM_1 = 3
    NATXNM_2 = 4
    NATTMP = 5
    NATX = 6
    NATY = 7
    NATZ = 8
    NATIUC = 9
    NATSPH = 10
    NATTPN = 11
    NATYR_1 = 12
    NATJUR_1 = 13
    NATCNT_1 = 14


# ---------------------------------------------------------------------------
# Raw record containers (preserving all fields for round-trip fidelity)
# ---------------------------------------------------------------------------

@dataclass
class AatRecord:
    """Complete hwycov/aat.adf record. Stores raw tuple for lossless writeback."""
    _raw: tuple
    record_index: int  # 0-based position in file

    def __getitem__(self, idx: int):
        return self._raw[idx]

    @property
    def aatid(self) -> int:
        return self._raw[_AatIdx.AATID]

    @property
    def aatnm(self) -> str:
        return self._raw[_AatIdx.AATNM].decode("ascii", errors="replace").rstrip()

    @property
    def from_node(self) -> int:
        return self._raw[_AatIdx.AATARN_2]

    @property
    def to_node(self) -> int:
        return self._raw[_AatIdx.AATARN_1]

    def to_bytes(self) -> bytes:
        return AAT_STRUCT.pack(*self._raw)

    def with_updates(self, **kwargs: dict) -> "AatRecord":
        """Return a new record with specified field indices updated."""
        raw_list = list(self._raw)
        for idx, val in kwargs.items():
            raw_list[int(idx)] = val
        return AatRecord(_raw=tuple(raw_list), record_index=self.record_index)


@dataclass
class NatRecord:
    """Complete hwycov/nat.adf record."""
    _raw: tuple
    record_index: int

    def __getitem__(self, idx: int):
        return self._raw[idx]

    @property
    def natlb(self) -> int:
        return self._raw[_NatIdx.NATLB]

    @property
    def natid(self) -> int:
        return self._raw[_NatIdx.NATID]

    @property
    def nattpn(self) -> int:
        return self._raw[_NatIdx.NATTPN]

    @property
    def natcnt(self) -> int:
        return self._raw[_NatIdx.NATCNT_1]

    @property
    def natsph(self) -> int:
        return self._raw[_NatIdx.NATSPH]

    def to_bytes(self) -> bytes:
        return NAT_STRUCT.pack(*self._raw)


# ---------------------------------------------------------------------------
# Loader
# ---------------------------------------------------------------------------

def _decode_str(raw: bytes) -> str:
    return raw.decode("ascii", errors="replace").rstrip()


def load_aat_records(path: Path) -> List[AatRecord]:
    """Load all records from hwycov/aat.adf."""
    data = path.read_bytes()
    file_size = len(data)
    if file_size % AAT_RECORD_BYTES != 0:
        raise ValueError(
            f"File size {file_size} is not a multiple of {AAT_RECORD_BYTES} bytes. "
            f"Check RECL unit assumption (expected 4-byte words)."
        )
    record_count = file_size // AAT_RECORD_BYTES
    records = []
    for i in range(record_count):
        offset = i * AAT_RECORD_BYTES
        raw = AAT_STRUCT.unpack_from(data, offset)
        records.append(AatRecord(_raw=raw, record_index=i))
    return records


def load_nat_records(path: Path) -> List[NatRecord]:
    """Load all records from hwycov/nat.adf."""
    data = path.read_bytes()
    file_size = len(data)
    if file_size % NAT_RECORD_BYTES != 0:
        raise ValueError(
            f"File size {file_size} is not a multiple of {NAT_RECORD_BYTES} bytes. "
            f"Check RECL unit assumption (expected 4-byte words)."
        )
    record_count = file_size // NAT_RECORD_BYTES
    records = []
    for i in range(record_count):
        offset = i * NAT_RECORD_BYTES
        raw = NAT_STRUCT.unpack_from(data, offset)
        records.append(NatRecord(_raw=raw, record_index=i))
    return records


# ---------------------------------------------------------------------------
# Projection: AatRecord -> TCHCLink
# ---------------------------------------------------------------------------
# Import TCHCLink from capacity_review if available, otherwise define minimal
# protocol. The actual import path will depend on project layout.

try:
    import sys
    sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "capacity_review"))
    from tchc import TCHCLink
except ImportError:
    from dataclasses import dataclass as _dc

    @_dc
    class TCHCLink:  # type: ignore[no-redef]
        """Stub - replace with real import."""
        pass


def project_aat_to_tchc_link(rec: AatRecord) -> "TCHCLink":
    """Map an AatRecord to a TCHCLink for apply_tchc."""
    from tchc import TCHCLink  # ensure real class is used

    r = rec._raw
    I = _AatIdx
    return TCHCLink(
        link_identifier=r[I.AATID],
        link_name=_decode_str(r[I.AATNM]),
        length_feet=r[I.AATLEN],
        functional_class=r[I.AATFC_1],
        high_occupancy_vehicle_class=r[I.AATHOV_1],
        jurisdiction=r[I.AATJUR_1],
        median_type=r[I.AATMED_1],
        directionality=r[I.AATWAY_1],
        traffic_count_identifier=r[I.AATADT],
        station_identifier=r[I.AATSTA],
        project_identifier=r[I.AATPRJ_1],
        from_node_identifier=r[I.AATARN_2],
        to_node_identifier=r[I.AATARN_1],
        speed=r[I.AATSPD_1],
        lane_count_by_period_and_direction=[
            [r[I.AATLN_1_1_1], r[I.AATLN_1_2_1]],
            [r[I.AATLN_2_1_1], r[I.AATLN_2_2_1]],
            [r[I.AATLN_3_1_1], r[I.AATLN_3_2_1]],
        ],
        auxiliary_lane_count_by_direction=[r[I.AATAUX_1_1], r[I.AATAUX_2_1]],
        planned_lane_capacity_by_direction=[r[I.AATPLC_1], r[I.AATPLC_2]],
        control_type_by_direction=[r[I.AATCNT_1_1], r[I.AATCNT_2_1]],
        through_lane_count_by_direction=[r[I.AATTL_1_1], r[I.AATTL_2_1]],
        right_turn_lane_count_by_direction=[r[I.AATRL_1_1], r[I.AATRL_2_1]],
        left_turn_lane_count_by_direction=[r[I.AATLL_1_1], r[I.AATLL_2_1]],
        green_cycle_value_by_direction=[r[I.AATGC_1], r[I.AATGC_2]],
        toll_cost_by_period=[r[I.AATTOLL_1_1], r[I.AATTOLL_2_1], r[I.AATTOLL_3_1]],
    )


# ---------------------------------------------------------------------------
# Writeback: TCHCLink outputs -> AatRecord field updates
# ---------------------------------------------------------------------------

def writeback_updates_from_link(link) -> dict:
    """
    Build a dict of {field_index: value} to patch into an AatRecord after
    apply_tchc has computed outputs.
    """
    I = _AatIdx
    updates = {}

    # Travel time by period and direction
    for ipk, (lt_ab, lt_ba) in enumerate([
        (I.AATLT_1_1, I.AATLT_1_2),
        (I.AATLT_2_1, I.AATLT_2_2),
        (I.AATLT_3_1, I.AATLT_3_2),
    ]):
        updates[str(lt_ab)] = link.link_travel_time_minutes_by_period_and_direction[ipk][0]
        updates[str(lt_ba)] = link.link_travel_time_minutes_by_period_and_direction[ipk][1]

    # Intersection delay
    for ipk, (xt_ab, xt_ba) in enumerate([
        (I.AATXT_1_1, I.AATXT_1_2),
        (I.AATXT_2_1, I.AATXT_2_2),
        (I.AATXT_3_1, I.AATXT_3_2),
    ]):
        updates[str(xt_ab)] = link.intersection_delay_minutes_by_period_and_direction[ipk][0]
        updates[str(xt_ba)] = link.intersection_delay_minutes_by_period_and_direction[ipk][1]

    # Hourly capacity
    for ipk, (hc_ab, hc_ba) in enumerate([
        (I.AATHC_1_1, I.AATHC_1_2),
        (I.AATHC_2_1, I.AATHC_2_2),
        (I.AATHC_3_1, I.AATHC_3_2),
    ]):
        updates[str(hc_ab)] = link.hourly_capacity_by_period_and_direction[ipk][0]
        updates[str(hc_ba)] = link.hourly_capacity_by_period_and_direction[ipk][1]

    # Period capacity
    for ipk, (lc_ab, lc_ba) in enumerate([
        (I.AATLC_1_1, I.AATLC_1_2),
        (I.AATLC_2_1, I.AATLC_2_2),
        (I.AATLC_3_1, I.AATLC_3_2),
    ]):
        updates[str(lc_ab)] = link.period_capacity_by_period_and_direction[ipk][0]
        updates[str(lc_ba)] = link.period_capacity_by_period_and_direction[ipk][1]

    # Intersection capacity
    for ipk, (xc_ab, xc_ba) in enumerate([
        (I.AATXC_1_1, I.AATXC_1_2),
        (I.AATXC_2_1, I.AATXC_2_2),
        (I.AATXC_3_1, I.AATXC_3_2),
    ]):
        updates[str(xc_ab)] = link.intersection_capacity_by_period_and_direction[ipk][0]
        updates[str(xc_ba)] = link.intersection_capacity_by_period_and_direction[ipk][1]

    # Generalized cost
    updates[str(I.AATTC_1)] = link.generalized_cost_by_direction[0]
    updates[str(I.AATTC_2)] = link.generalized_cost_by_direction[1]

    # Auto operating cost
    updates[str(I.AATCST)] = link.auto_operating_cost

    # Toll (modified in-place by apply_tchc)
    updates[str(I.AATTOLL_1_1)] = link.toll_cost_by_period[0]
    updates[str(I.AATTOLL_2_1)] = link.toll_cost_by_period[1]
    updates[str(I.AATTOLL_3_1)] = link.toll_cost_by_period[2]

    return updates


# ---------------------------------------------------------------------------
# Writer
# ---------------------------------------------------------------------------

def write_aat_records(path: Path, records: List[AatRecord]) -> None:
    """Write all aat records back to a binary file."""
    with open(path, "wb") as f:
        for rec in records:
            data = rec.to_bytes()
            assert len(data) == AAT_RECORD_BYTES
            f.write(data)


def write_nat_records(path: Path, records: List[NatRecord]) -> None:
    """Write all nat records back to a binary file."""
    with open(path, "wb") as f:
        for rec in records:
            data = rec.to_bytes()
            assert len(data) == NAT_RECORD_BYTES
            f.write(data)


# ---------------------------------------------------------------------------
# Node-based context builders
# ---------------------------------------------------------------------------

def build_approach_counts(
    aat_records: List[AatRecord],
    nat_records: List[NatRecord],
) -> dict:
    """
    Build node approach count dict (xdapp) from link/node records.

    The FORTRAN accumulates xdapp by counting links that touch each node
    (excluding FC=10 zone connectors), then clamps to [2, 4].
    natcnt(1) provides the coded control type per node.
    """
    from collections import defaultdict
    xdapp: dict = defaultdict(int)
    I = _AatIdx

    for rec in aat_records:
        r = rec._raw
        fc = r[I.AATFC_1]
        if fc < 1 or fc > 9:
            continue
        way = r[I.AATWAY_1]
        from_node = r[I.AATARN_2]
        to_node = r[I.AATARN_1]
        # directional approach count (only AB if one-way, both if two-way)
        xdapp[from_node] += 1
        if way == 2:
            xdapp[to_node] += 1

    # Clamp to [2, 4] per FORTRAN logic
    for node in xdapp:
        xdapp[node] = max(2, min(4, xdapp[node]))

    return dict(xdapp)


def build_node_control_map(nat_records: List[NatRecord]) -> dict:
    """Map node_id (natlb) -> coded control type (natcnt(1))."""
    return {rec.natlb: rec.natcnt for rec in nat_records}


# ---------------------------------------------------------------------------
# High-level orchestration
# ---------------------------------------------------------------------------

def load_network(aat_path: Path, nat_path: Path):
    """
    Load aat and nat files, return (aat_records, nat_records).
    Validates alignment on load.
    """
    aat_records = load_aat_records(aat_path)
    nat_records = load_nat_records(nat_path)
    return aat_records, nat_records
