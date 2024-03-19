from parking.process import ParkingProcessing
import sys

if __name__ == "__main__":    

    year = sys.argv[1]

    with open("settings.yaml", "r") as f:
        data = f.read()
        f.close()

    data = data.replace(
        "mgra15_based_input2022.csv",
        "mgra15_based_input{}.csv".format(year)
        )

    with open("settings.yaml", "w") as f:
        f.write(data)
        f.close()

    ParkingProcessing().run_processing()