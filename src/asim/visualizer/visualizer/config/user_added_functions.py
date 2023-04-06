def get_escort_participant_list(escort_participants):
    if type(escort_participants) == str:
        return [int(participant) for participant in escort_participants.split('_')]
    else:
        return []