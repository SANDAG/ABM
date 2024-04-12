from multiprocessing import shared_memory
import time

# Find and hold on to skims in shared memory so memory is not released

if __name__ == '__main__':
    shared_mem_name = "skim_shared_memory__taz"

    try:
        test_mem = shared_memory.SharedMemory(name=shared_mem_name)
        raise Exception("Skims already exist in shared memory")
    except FileNotFoundError:
        pass

    # Try to get skim from shared memory every 10 minutes
    # Timeout after one hour
    mem = None
    for i in range(6):
        time.sleep(600)
        try:
            mem = shared_memory.SharedMemory(name=shared_mem_name)
            print("Skims found in shared memory")
            break
        except FileNotFoundError:
            continue
    if mem is None:
        print("Timed out, skims not found in shared memory")
    else:
        # Wait for asim models to complete, then release memory
        input()
        print("Releasing skims from shared memory")
        mem.close()
        mem.unlink()