import enum


class BusinessTypes(enum.IntEnum):
    wholesale = 1
    gigwork = 2


class RoutePurposes(enum.IntFlag):
    goods = 1
    services = 2
    other = 4


class VehicleTypes(enum.IntEnum):
    car = 1
    light_truck = 2
    med_truck = 3
    heavy_truck = 4
    sidewalk_drone = 5


class CustomerTypes(enum.IntFlag):
    residential = 1
    nonresidential = 2
    mixed = 3


class StopPurposes(enum.IntEnum):
    originate = 1
    terminate = 2
    base = 3
    goods = 4
    service = 5
    other = 6


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
