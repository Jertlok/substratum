/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.fragments;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Lunchbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.commands.ElevatedCommands;
import projekt.substratum.common.commands.FileOperations;
import projekt.substratum.common.platform.ThemeManager;
import projekt.substratum.common.tabs.BootAnimationManager;
import projekt.substratum.common.tabs.FontManager;
import projekt.substratum.common.tabs.WallpaperManager;
import projekt.substratum.util.tabs.SoundUtils;
import projekt.substratum.util.views.SheetDialog;

import static android.content.om.OverlayInfo.STATE_APPROVED_ENABLED;
import static android.content.om.OverlayInfo.STATE_NOT_APPROVED_DANGEROUS_OVERLAY;
import static projekt.substratum.common.References.DATA_RESOURCE_DIR;
import static projekt.substratum.common.References.LEGACY_NEXUS_DIR;
import static projekt.substratum.common.References.PIXEL_NEXUS_DIR;
import static projekt.substratum.common.References.VENDOR_DIR;


public class RecoveryFragment extends Fragment {

    private ProgressDialog mProgressDialog;
    private ArrayList<String> final_commands_array;
    private SharedPreferences prefs;
    private SheetDialog sheetDialog;
    private Context mContext;

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sheetDialog != null) sheetDialog.dismiss();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.restore_fragment, container, false);

        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        setHasOptionsMenu(true);

        Button overlaysButton = (Button) root.findViewById(R.id.overlaysButton);
        Button wallpaperButton = (Button) root.findViewById(R.id.wallpaperButton);
        Button bootanimationButton = (Button) root.findViewById(R.id.bootanimationButton);
        Button fontsButton = (Button) root.findViewById(R.id.fontsButton);
        Button soundsButton = (Button) root.findViewById(R.id.soundsButton);

        // Overlays Dialog
        overlaysButton.setOnClickListener(v -> {
            sheetDialog = new SheetDialog(mContext);
            View sheetView = View.inflate(
                    mContext,
                    R.layout.recovery_overlays_sheet_dialog,
                    null);
            LinearLayout disable_all = (LinearLayout) sheetView.findViewById(R.id.disable_all);
            LinearLayout uninstall_all = (LinearLayout) sheetView.findViewById(R.id.uninstall_all);
            if (!References.checkOMS(mContext)) disable_all.setVisibility(View.GONE);
            disable_all.setOnClickListener(view -> {
                if (getView() != null) {
                    Lunchbar.make(getView(),
                            getString(R.string.manage_system_overlay_toast),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
                ThemeManager.disableAll(mContext);
                sheetDialog.hide();
            });
            uninstall_all.setOnClickListener(view -> {
                new AbortFunction().execute("");
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        // Wallpaper Dialog
        wallpaperButton.setOnClickListener(v -> {
            sheetDialog = new SheetDialog(mContext);
            View sheetView = View.inflate(mContext, R.layout.recovery_wallpapers_sheet_dialog,
                    null);
            LinearLayout home = (LinearLayout) sheetView.findViewById(R.id.home);
            LinearLayout lock = (LinearLayout) sheetView.findViewById(R.id.lock);
            LinearLayout both = (LinearLayout) sheetView.findViewById(R.id.both);
            home.setOnClickListener(view2 -> {
                try {
                    WallpaperManager.clearWallpaper(mContext, "home");
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.manage_wallpaper_home_toast),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                } catch (IOException e) {
                    Log.e(References.SUBSTRATUM_LOG,
                            "Failed to restore home screen wallpaper! " + e.getMessage());
                } catch (NullPointerException e) {
                    Log.e(References.SUBSTRATUM_LOG,
                            "Cannot retrieve lock screen wallpaper! " + e.getMessage());
                }
                sheetDialog.hide();
            });
            lock.setOnClickListener(view2 -> {
                try {
                    WallpaperManager.clearWallpaper(mContext, "lock");
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.manage_wallpaper_lock_toast),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                } catch (IOException e) {
                    Log.e(References.SUBSTRATUM_LOG,
                            "Failed to restore lock screen wallpaper!" + e.getMessage());
                }
                sheetDialog.hide();
            });
            both.setOnClickListener(view2 -> {
                try {
                    WallpaperManager.clearWallpaper(mContext, "all");
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.manage_wallpaper_all_toast),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                } catch (IOException e) {
                    Log.e(References.SUBSTRATUM_LOG,
                            "Failed to restore wallpapers! " + e.getMessage());
                }
                sheetDialog.hide();
            });

            home.setVisibility(View.VISIBLE);
            lock.setVisibility(View.VISIBLE);
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        // Boot Animation Dialog
        bootanimationButton.setOnClickListener(v -> {
            sheetDialog = new SheetDialog(mContext);
            View sheetView = View.inflate(mContext,
                    R.layout.recovery_bootanimations_sheet_dialog, null);
            LinearLayout restore = (LinearLayout) sheetView.findViewById(R.id.restore);
            restore.setOnClickListener(view2 -> {
                new BootAnimationClearer().execute("");
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        // Font Dialog
        fontsButton.setOnClickListener(v -> {
            sheetDialog = new SheetDialog(mContext);
            View sheetView = View.inflate(mContext,
                    R.layout.recovery_fonts_sheet_dialog, null);
            LinearLayout restore = (LinearLayout) sheetView.findViewById(R.id.restore);
            restore.setOnClickListener(view2 -> {
                if (References.checkThemeInterfacer(mContext) ||
                        Settings.System.canWrite(mContext)) {
                    new FontsClearer(this).execute("");
                } else {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.fonts_dialog_permissions_grant_toast),
                                Lunchbar.LENGTH_LONG).show();
                    }
                }
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        // Sounds Dialog
        soundsButton.setOnClickListener(v -> {
            sheetDialog = new SheetDialog(mContext);
            View sheetView = View.inflate(mContext,
                    R.layout.recovery_sounds_sheet_dialog, null);
            LinearLayout restore = (LinearLayout) sheetView.findViewById(R.id.restore);
            restore.setOnClickListener(view2 -> {
                if (References.checkThemeInterfacer(mContext) ||
                        Settings.System.canWrite(mContext)) {
                    new SoundsClearer().execute("");
                } else {
                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.sounds_dialog_permissions_grant_toast),
                                Lunchbar.LENGTH_LONG).show();
                    }
                }
                sheetDialog.hide();
            });
            sheetDialog.setContentView(sheetView);
            sheetDialog.show();
        });

        return root;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.restore_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.restore_info) {
            Dialog dialog = new Dialog(mContext, R.style.RestoreInfo);
            dialog.setContentView(R.layout.restore_info);
            dialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static class FontsClearer extends AsyncTask<String, Integer, String> {

        private WeakReference<RecoveryFragment> ref;

        private FontsClearer(RecoveryFragment fragment) {
            ref = new WeakReference<>(fragment);
        }

        @Override
        protected void onPreExecute() {
            RecoveryFragment fragment = ref.get();
            Context context = fragment.mContext;
            if (References.ENABLE_EXTRAS_DIALOG) {
                fragment.mProgressDialog = new ProgressDialog(context, R.style.RestoreDialog);
                fragment.mProgressDialog.setMessage(
                        context.getString(R.string.manage_dialog_performing));
                fragment.mProgressDialog.setIndeterminate(true);
                fragment.mProgressDialog.setCancelable(false);
                fragment.mProgressDialog.show();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            RecoveryFragment fragment = ref.get();
            Context context = fragment.mContext;
            if (References.ENABLE_EXTRAS_DIALOG) {
                fragment.mProgressDialog.dismiss();
            }
            SharedPreferences.Editor editor = fragment.prefs.edit();
            editor.remove("fonts_applied");
            editor.apply();


            if (References.checkOMS(context)) {
                Toast toast = Toast.makeText(
                        context,
                        R.string.manage_fonts_toast,
                        Toast.LENGTH_SHORT);
                toast.show();
            } else {
                Toast toast = Toast.makeText(
                        context,
                        R.string.manage_fonts_toast,
                        Toast.LENGTH_SHORT);
                toast.show();
                final AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(context);
                alertDialogBuilder.setTitle(R.string.legacy_dialog_soft_reboot_title);
                alertDialogBuilder.setMessage(R.string.legacy_dialog_soft_reboot_text);
                alertDialogBuilder.setPositiveButton(android.R.string.ok,
                        (dialog, id) -> ElevatedCommands.reboot());
                alertDialogBuilder.setNegativeButton(R.string.remove_dialog_later,
                        (dialog, id) -> dialog.dismiss());
                alertDialogBuilder.setCancelable(false);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            RecoveryFragment fragment = ref.get();
            Context context = fragment.mContext;
            FontManager.clearFonts(context);
            return null;
        }
    }

    private class AbortFunction extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity(), R.style.RestoreDialog);
            mProgressDialog.setMessage(getString(R.string.manage_dialog_performing));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.dismiss();
            super.onPostExecute(result);
            if (References.checkOMS(mContext)) {
                try {
                    if (getView() != null) {
                        Lunchbar.make(getView(),
                                getString(R.string.manage_system_overlay_uninstall_toast),
                                Lunchbar.LENGTH_LONG)
                                .show();
                    }
                } catch (Exception e) {
                    // At this point the window is refreshed too many times detaching the activity
                    Log.e(References.SUBSTRATUM_LOG, "Profile window refreshed too " +
                            "many times, restarting current activity to preserve app " +
                            "integrity.");
                }
                ThemeManager.uninstallOverlay(mContext, final_commands_array);
            } else {
                if (getView() != null) {
                    Lunchbar.make(getView(),
                            getString(R.string.abort_overlay_toast_success),
                            Lunchbar.LENGTH_LONG)
                            .show();
                }
                AlertDialog.Builder alertDialogBuilder =
                        new AlertDialog.Builder(mContext);
                alertDialogBuilder
                        .setTitle(getString(R.string.legacy_dialog_soft_reboot_title));
                alertDialogBuilder
                        .setMessage(getString(R.string.legacy_dialog_soft_reboot_text));
                alertDialogBuilder
                        .setPositiveButton(android.R.string.ok,
                                (dialog, id) -> ElevatedCommands.reboot());
                alertDialogBuilder.setCancelable(false);
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            if (References.checkOMS(mContext)) {
                List<String> unapproved =
                        ThemeManager.listOverlays(STATE_NOT_APPROVED_DANGEROUS_OVERLAY);
                List<String> disabled =
                        ThemeManager.listOverlays(STATE_APPROVED_ENABLED);
                List<String> enabled =
                        ThemeManager.listOverlays(STATE_APPROVED_ENABLED);

                final_commands_array = new ArrayList<>(unapproved);
                final_commands_array.addAll(disabled);
                final_commands_array.addAll(enabled);
            } else {
                FileOperations.mountRW();
                FileOperations.mountRWData();
                FileOperations.mountRWVendor();
                FileOperations.bruteforceDelete(DATA_RESOURCE_DIR);
                FileOperations.bruteforceDelete(LEGACY_NEXUS_DIR);
                FileOperations.bruteforceDelete(PIXEL_NEXUS_DIR);
                FileOperations.bruteforceDelete(VENDOR_DIR);
                FileOperations.mountROVendor();
                FileOperations.mountROData();
                FileOperations.mountRO();
            }
            return null;
        }
    }

    private class BootAnimationClearer extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity(), R.style.RestoreDialog);
            mProgressDialog.setMessage(getString(R.string.manage_dialog_performing));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.dismiss();

            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("bootanimation_applied").apply();

            if (getView() != null) {
                Lunchbar.make(getView(),
                        getString(R.string.manage_bootanimation_toast),
                        Lunchbar.LENGTH_LONG)
                        .show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            BootAnimationManager.clearBootAnimation(mContext);
            return null;
        }
    }

    private class SoundsClearer extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(getActivity(), R.style.RestoreDialog);
            mProgressDialog.setMessage(getString(R.string.manage_dialog_performing));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.dismiss();

            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("sounds_applied").apply();

            if (getView() != null) {
                Lunchbar.make(getView(),
                        getString(R.string.manage_sounds_toast),
                        Lunchbar.LENGTH_LONG)
                        .show();
            }
        }

        @Override
        protected String doInBackground(String... sUrl) {
            new SoundUtils().SoundsClearer(mContext);
            return null;
        }
    }
}