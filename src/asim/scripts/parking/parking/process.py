from .reduction import ReduceRawParkingData
from .imputation import ImputeParkingCosts
from .districts import CreateDistricts
from .estimate_spaces import EstimateStreetParking
from .expected_cost import ExpectedParkingCost

class ParkingProcessing(
    ReduceRawParkingData,
    ImputeParkingCosts,
    CreateDistricts,
    EstimateStreetParking,
    ExpectedParkingCost,
):
    def run_processing(self):
        
        # Runs models listed in settings.yaml
        for model_name in self.settings.get("models"):
            print(f"######## Running {model_name} ########")
            getattr(self, model_name)()