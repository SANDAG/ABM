# ActivitySim
# See full license in LICENSE.txt.
import os
import logging
import win32com.client as win32

from activitysim.core import inject

logger = logging.getLogger("activitysim")

@inject.step()
def check_disk_usage():
    output_dir = inject.get_injectable("output_dir")
    path = os.path.abspath(os.path.join(output_dir, "..", ".."))
    disk_usage = win32.Dispatch('Scripting.FileSystemObject').GetFolder(path).Size
    logger.info("Disk space usage: %f GB" % (disk_usage / (1024 ** 3)))