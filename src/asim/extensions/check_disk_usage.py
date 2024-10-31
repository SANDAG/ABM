# ActivitySim
# See full license in LICENSE.txt.
import os
import logging
import win32com.client as win32

from activitysim.core import workflow

logger = logging.getLogger("activitysim")

@workflow.step()
def check_disk_usage(state):
    output_dir = state.get_injectable("output_dir")
    path = os.path.abspath(os.path.join(output_dir, "..", ".."))
    disk_usage = win32.Dispatch('Scripting.FileSystemObject').GetFolder(path).Size
    logger.info("Disk space usage: %f GB" % (disk_usage / (1024 ** 3)))