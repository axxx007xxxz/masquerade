package projekt.substratum.helper.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import projekt.substratum.helper.util.ReadOverlaysFile;
import projekt.substratum.helper.util.Root;

/**
 * @author Nicholas Chum (nicholaschum)
 */

public class Helper extends BroadcastReceiver {

    private List<String> state5overlays = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SubstratumHelper",
                "BroadcastReceiver has accepted Substratum's commands and is running now...");
        Root.requestRootAccess();

        if (intent.getStringArrayListExtra("pm-uninstall") != null) {
            uninstall_handler(intent);
        }

        if (intent.getStringExtra("om-commands") != null) {
            Log.d("SubstratumHelper", "Running command: \"" +
                    intent.getStringExtra("om-commands") + "\"");
            Root.runCommand(intent.getStringExtra("om-commands"));
        }
    }

    private boolean checkIfPackageActivated(String package_name) {
        return (state5overlays.contains(package_name));
    }

    private void uninstall_handler(Intent intent) {
        String final_commands_disable = "";
        String final_commands_uninstall = "";

        Root.runCommand(
                "pm grant projekt.substratum.helper android.permission.READ_EXTERNAL_STORAGE");
        Root.runCommand(
                "pm grant projekt.substratum.helper android.permission.WRITE_EXTERNAL_STORAGE");

        ArrayList<String> packages_to_uninstall =
                new ArrayList<>(intent.getStringArrayListExtra("pm-uninstall"));
        Root.runCommand("cp /data/system/overlays" +
                ".xml " + Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum/current_overlays.xml");
        String[] state5initial = {Environment.getExternalStorageDirectory().getAbsolutePath() +
                "/.substratum/current_overlays.xml", "5"};
        state5overlays = ReadOverlaysFile.main(state5initial);

        for (int i = 0; i < packages_to_uninstall.size(); i++) {
            String current = packages_to_uninstall.get(i);

            Log.d("SubstratumHelper", "Intent received to purge referendum package file \"" +
                    current + "\"");
            if (checkIfPackageActivated(packages_to_uninstall.get(i))) {
                Log.d("SubstratumHelper", "Package file \"" + current +
                        "\" requires an overlay disable prior to uninstall...");
                if (final_commands_disable.length() == 0) {
                    final_commands_disable = "om disable " + current;
                } else {
                    final_commands_disable = final_commands_disable + " " + current;
                }

                if (final_commands_uninstall.length() == 0) {
                    final_commands_uninstall = "pm uninstall " + current;
                } else {
                    final_commands_uninstall = final_commands_uninstall +
                            " && pm uninstall " + current;
                }
            } else {
                Root.runCommand("pm uninstall " + current);
            }
        }

        if (final_commands_disable.length() > 0) {
            Log.d("SubstratumHelper", "Disable commands: " + final_commands_disable);
            Root.runCommand(final_commands_disable);
        } else {
            if (final_commands_uninstall.length() > 0) {
                Log.d("SubstratumHelper", "Uninstall commands: " + final_commands_uninstall);
                Root.runCommand(final_commands_uninstall);
            }
        }

        // Clear the resource idmapping files generated by OMS
        Log.d("SubstratumHelper", "Cleaning up resource-cache directory...");
        Root.runCommand("rm /data/resource-cache/*");
        // Now clear the persistent overlays database
        Log.d("SubstratumHelper", "Finalizing clean up of persistent overlays database...");
        Root.runCommand("rm -rf /data/system/overlays.xml");
    }
}