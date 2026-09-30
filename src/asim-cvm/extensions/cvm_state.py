from activitysim.core.workflow import State as workflow_State
from activitysim.core.workflow.state import StateAttr
from activitysim.abm.models.util import canonical_ids as cid

from .cvm_settings import CVMSettings


class State(workflow_State):
    """
    Subclass of State for commercial vehicle model.
    """

    settings: CVMSettings = StateAttr(CVMSettings)

    # cvm tables that need RNG channel / traceable-table / canonical
    # index registration on every process start (single or multiprocess),
    # since checkpoint reloads only tables listed here.
    _CVM_TABLES = [
        ("establishments", "establishment_id"),
        ("establishments_all", "establishment_id_all"),
        ("routes", "route_id"),
    ]

    @classmethod
    def _register_cvm_tables(cls):
        for table_name, index_name in cls._CVM_TABLES:
            if table_name not in cid.RANDOM_CHANNELS:
                cid.RANDOM_CHANNELS.append(table_name)
            if table_name not in cid.TRACEABLE_TABLES:
                cid.TRACEABLE_TABLES.append(table_name)
            cid.CANONICAL_TABLE_INDEX_NAMES.setdefault(table_name, index_name)


State._register_cvm_tables()