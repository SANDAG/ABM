from activitysim.core.configuration.top import Settings


class CVMSettings(Settings):
    establishments_sample_size: int = 0
    """
    Number of establishments to sample and simulate.

    If omitted or set to 0, ActivitySim will simulate all establishments.
    """

    trace_establishment_id: int | None = None
