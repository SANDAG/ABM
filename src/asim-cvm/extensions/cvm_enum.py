import enum


class BusinessTypes(enum.IntEnum):
    AGM = 1
    MFG = 2
    IUT = 3
    RET = 4
    WHL = 5
    CON = 6
    TRN = 7
    IFR = 8
    EPO = 9
    MHS = 10
    LAF = 11


class RoutePurposes(enum.IntEnum):
    goods = 1
    service = 2
    maintenance = 3


class VehicleTypes(enum.IntEnum):
    LCV = 1
    MUT = 2
    SUT = 3


class CustomerTypes(enum.IntEnum):
    residential = 1
    nonresidential = 2
    mixed = 3
    na = 0


class StopPurposes(enum.IntEnum):
    originate = 1
    terminate = 2
    base = 3
    goods_pickup = 4
    goods_delivery = 5
    service = 6
    maintenance = 7
    home = 8


class LocationTypes(enum.IntEnum):
    base = 1
    warehouse = 2
    intermodal = 3
    residential = 4
    commercial = 5
    terminal = 6


def decipher(enum_cls, value):
    """
    Get an enum value by name, or return the value unchanged.

    Parameters
    ----------
    enum_cls
    value

    Returns
    -------

    """
    if value in enum_cls.__members__:
        return enum_cls[value]
    else:
        return value
