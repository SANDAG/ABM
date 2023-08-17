from hwyShapeExport import export_highway_shape
from skimAppender import SkimAppender
from abmScenario import ScenarioData, LandUse, SyntheticPopulation, TourLists, TripLists
import os
import sys


def export_data(fp):
    # set file path to completed ABM run scenario folder
    # set report folder path
    scenarioPath = fp
    reportPath = os.path.join(scenarioPath, "report")


    # initialize base ABM scenario data class
    print("Initializing Scenario Data")
    scenario_data = ScenarioData(scenarioPath)


    # initialize land use class
    # write out MGRA-based input file
    print("Initializing Land Use Output")
    land_use = LandUse(scenarioPath)
    print("Writing: MGRA-Based Input File")
    land_use.mgra_input.to_csv(os.path.join(reportPath, "mgraBasedInput.csv"), index=False)


    # initialize synthetic population class
    # write out households and persons files
    print("Initializing Synthetic Population Output")
    population = SyntheticPopulation(scenarioPath)

    print("Writing: Households File")
    population.households.to_csv(os.path.join(reportPath, "households.csv"), index=False)

    print("Writing: Persons File")
    population.persons.to_csv(os.path.join(reportPath, "persons.csv"), index=False)


    # initialize tour list class
    # write out tour list files
    print("Initializing Tour List Output")
    tours = TourLists(scenarioPath)

    print("Writing: Commercial Vehicle Tours")
    tours.cvm.to_csv(os.path.join(reportPath, "commercialVehicleTours.csv"), index=False)

    print("Writing: Cross Border Tours")
    tours.cross_border.to_csv(os.path.join(reportPath, "crossBorderTours.csv"), index=False)

    print("Writing: Individual Tours")
    tours.individual.to_csv(os.path.join(reportPath, "individualTours.csv"), index=False)

    print("Writing: Internal-External Tours")
    tours.ie.to_csv(os.path.join(reportPath, "internalExternalTours.csv"), index=False)

    print("Writing: Joint Tours")
    tours.joint.to_csv(os.path.join(reportPath, "jointTours.csv"), index=False)

    print("Writing: Visitor Tours")
    tours.visitor.to_csv(os.path.join(reportPath, "visitorTours.csv"), index=False)


    print("Initializing Trip List Output")

    # initialize trip list class
    trips = TripLists(scenarioPath)

    # initialize skim appender class
    skims = SkimAppender(scenarioPath)

    # write out trip list files
    print("Writing: Airport-SAN Trips")
    model_trip = trips.airport_san
    # skims.append_skims(model_trip,
    #                    auto_only=False,
    #                    terminal_skims=False).to_csv(
    #     os.path.join(reportPath, "airportSANTrips.csv"),
    #     index=False)

    print("Writing: Airport-CBX Trips")
    model_trip = trips.airport_cbx
    # skims.append_skims(trips.airport_cbx,
    #                    auto_only=False,
    #                    terminal_skims=False).to_csv(
    #     os.path.join(reportPath, "airportCBXTrips.csv"),
    #     index=False)

    print("Writing: Commercial Vehicle Trips")
    skims.append_skims(trips.cvm,
                       auto_only=True,
                       terminal_skims=False).to_csv(
        os.path.join(reportPath, "commercialVehicleTrips.csv"),
        index=False)

    print("Writing: Cross-Border Trips")
    # skims.append_skims(trips.cross_border,
    #                    auto_only=False,
    #                    terminal_skims=False).to_csv(
    #     os.path.join(reportPath, "crossBorderTrips.csv"),
    #     index=False)

    print("Writing: External-External Trips")
    trips.ee.to_csv(
        os.path.join(reportPath, "externalExternalTrips.csv"),
        index=False)

    print("Writing: External-Internal Trips")
    trips.ei.to_csv(
        os.path.join(reportPath, "externalInternalTrips.csv"),
        index=False)

    print("Writing: Individual Trips")
    # skims.append_skims(trips.individual,
    #                    auto_only=False,
    #                    terminal_skims=True).to_csv(
    #     os.path.join(reportPath, "individualTrips.csv"),
    #     index=False)

    print("Writing: Internal-External Trips")
    skims.append_skims(trips.ie,
                       auto_only=False,
                       terminal_skims=False).to_csv(
        os.path.join(reportPath, "internalExternalTrips.csv"),
        index=False)

    print("Writing: Joint Trips")
    skims.append_skims(trips.joint,
                       auto_only=False,
                       terminal_skims=True).to_csv(
        os.path.join(reportPath, "jointTrips.csv"),
        index=False)

    print("Writing: Truck Trips")
    trips.truck.to_csv(
        os.path.join(reportPath, "truckTrips.csv"),
        index=False)

    print("Writing: Visitor Trips")
    skims.append_skims(trips.visitor,
                       auto_only=False,
                       terminal_skims=False).to_csv(
        os.path.join(reportPath, "visitorTrips.csv"),
        index=False)

    print("Writing: Zombie AV Trips")
    skims.append_skims(trips.zombie_av,
                       auto_only=True,
                       terminal_skims=False).to_csv(
        os.path.join(reportPath, "zombieAVTrips.csv"),
        index=False)

    print("Writing: Zombie TNC Trips")
    skims.append_skims(trips.zombie_tnc,
                       auto_only=True,
                       terminal_skims=False).to_csv(
        os.path.join(reportPath, "zombieTNCTrips.csv"),
        index=False)

    print("Writing: Highway Load Shape File")
    export_highway_shape(scenarioPath).to_file(
        os.path.join(reportPath, "hwyLoad.shp"))
        
if __name__ == '__main__':
    targets = sys.argv[1:]
    export_data(targets[0])
