def get_escort_participant_list(escort_participants):
    if type(escort_participants) == str:
        return [int(participant) for participant in escort_participants.split('_')[1:]]
    else:
        return []

def _get_vehicle_info(vehicle_attributes, index):
    if type(vehicle_attributes) != str or vehicle_attributes == "non_hh_veh":
        return vehicle_attributes
    return vehicle_attributes.split("_")[index]

def get_vehicle_body_type(vehicle_attributes):
    return _get_vehicle_info(vehicle_attributes, 0)

def get_vehicle_age(vehicle_attributes):
    return _get_vehicle_info(vehicle_attributes, 1)

def get_vehicle_fuel_type(vehicle_attributes):
    return _get_vehicle_info(vehicle_attributes, 2)