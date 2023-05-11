
from activitysim.core.workflow import State as workflow_State
from activitysim.core.workflow.state import StateAttr
from .cvm_settings import CVMSettings

class State(workflow_State):
    """
    Subclass of State for commercial vehicle model.
    """

    settings: CVMSettings = StateAttr(CVMSettings)

