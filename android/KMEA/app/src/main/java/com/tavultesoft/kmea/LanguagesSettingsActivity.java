/**
 * Copyright (C) 2019 SIL International. All rights reserved.
 */

package com.tavultesoft.kmea;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.tavultesoft.kmea.data.Dataset;
import com.tavultesoft.kmea.data.Keyboard;
import com.tavultesoft.kmea.data.adapters.AdapterFilter;
import com.tavultesoft.kmea.data.adapters.NestedAdapter;
import com.tavultesoft.kmea.util.FileUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keyman Settings --> Languages Settings
 * Displays a list of installed languages and a count of their associated installed keyboards.
 */
public final class LanguagesSettingsActivity extends AppCompatActivity implements KeyboardEventHandler.OnKeyboardDownloadEventListener {

  private Context context;
  private static Toolbar toolbar = null;
  private static ListView listView = null;
  private static ImageButton addButton = null;

  // ********* ONLY USED BY UPDATE CODE ***********
  private static boolean updateCheckFailed = false;
  private static boolean updateFailed = false;
  private static Calendar lastUpdateCheck = null;
  private static boolean checkingUpdates = false;

  private static int updateCount = 0;
  private static int failedUpdateCount = 0;
  private static ProgressDialog updateProgress;

  private static HashMap<String, String> keyboardVersions = null;
  // ****** END - ONLY USED BY UPDATE CODE ********

  private boolean dismissOnSelect = false;
  protected static boolean canAddNewKeyboard = true;

  private static String TAG = "LanguagesSettingsActivity";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    context = this;
    setContentView(R.layout.languages_settings_list_layout);

    toolbar = (Toolbar) findViewById(R.id.list_toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setDisplayShowHomeEnabled(true);
    getSupportActionBar().setDisplayShowTitleEnabled(false);
    TextView textView = (TextView) findViewById(R.id.bar_title);
    textView.setText(getString(R.string.title_languages_settings));

    listView = (ListView) findViewById(R.id.listView);
    listView.setFastScrollEnabled(true);

    Dataset storage = KeyboardPickerActivity.getInstalledDataset(this);
    LanguagesAdapter listAdapter = new LanguagesAdapter(this, storage);

    listView.setAdapter(listAdapter);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LanguagesAdapter adapter = ((LanguagesAdapter) listView.getAdapter());

        Dataset.LanguageDataset languageData = adapter.getItem(position);
        String langId = languageData.code;
        String langName = languageData.name;

        HashMap<String, String> associatedLexicalModel = KMManager.getAssociatedLexicalModel(langId);

        Bundle args = new Bundle();
        args.putString(KMManager.KMKey_LanguageID, langId);
        args.putString(KMManager.KMKey_LanguageName, langName);

        if(associatedLexicalModel != null) {
          args.putString(KMManager.KMKey_LexicalModelName, associatedLexicalModel.get(KMManager.KMKey_LexicalModelName));
        }

        Intent intent = new Intent(context, LanguageSettingsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.putExtras(args);
        startActivity(intent);

        if (dismissOnSelect)
          finish();
      }
    });

    addButton = (ImageButton) findViewById(R.id.add_button);
    addButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        // Check that available keyboard information can be obtained via:
        // 1. connection to cloud catalog
        // 2. cached file
        // 3. local kmp.json files in packages/
        if (KMManager.hasConnection(context) || LanguageListActivity.getCacheFile(context).exists() ||
          KeyboardPickerActivity.hasKeyboardFromPackage()){
          dismissOnSelect = false;
          Intent i = new Intent(context, LanguageListActivity.class);
          i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
          context.startActivity(i);
        } else {
          AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
          dialogBuilder.setTitle(getString(R.string.title_add_keyboard));
          dialogBuilder.setMessage(String.format("\n%s\n", getString(R.string.cannot_connect)));
          dialogBuilder.setPositiveButton(getString(R.string.label_ok), null);
          AlertDialog dialog = dialogBuilder.create();
          dialog.show();
        }
      }
    });
    if (!canAddNewKeyboard) {
      addButton.setVisibility(View.GONE);
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    // From here on is update-specific stuff.
    KMKeyboardDownloaderActivity.addKeyboardDownloadEventListener(this);

    final Context context = this;
    Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        boolean shouldCheckUpdate = false;
        if (lastUpdateCheck == null) {
          SharedPreferences prefs = context.getSharedPreferences(getString(R.string.kma_prefs_name), Context.MODE_PRIVATE);
          Long lastUpdateCheckTime = prefs.getLong("lastUpdateCheck", 0);
          if (lastUpdateCheckTime > 0) {
            lastUpdateCheck = Calendar.getInstance();
            lastUpdateCheck.setTime(new Date(lastUpdateCheckTime));
          }
        }

        if (lastUpdateCheck != null) {
          Calendar lastChecked = Calendar.getInstance();
          lastChecked.setTime(lastUpdateCheck.getTime());
          if (updateCheckFailed || updateFailed) {
            lastChecked.add(Calendar.HOUR_OF_DAY, 1);
          } else {
            lastChecked.add(Calendar.HOUR_OF_DAY, 24);
          }

          Calendar now = Calendar.getInstance();
          if (now.compareTo(lastChecked) > 0) {
            shouldCheckUpdate = true;
          }
        } else {
          shouldCheckUpdate = true;
        }

        if (shouldCheckUpdate) {
          updateCheckFailed = false;
          updateFailed = false;
          if (!checkingUpdates) {
            checkKeyboardUpdates(context);
          }
        }
      }
    }, 1000);
  }

  @Override
  public void onPause() {
    super.onPause();

    // Intentionally not removing KeyboardDownloadEventListener to
    // ensure onKeyboardDownloadFinished() gets called
    // (Transplanted from KeyboardPickerActivity.)
  }

  @Override
  public boolean onSupportNavigateUp() {
    onBackPressed();
    return true;
  }

  @Override
  public void onBackPressed() {
    finish();
  }

  // Fully details the building of this Activity's list view items.
  static private class LanguagesAdapter extends NestedAdapter<Dataset.LanguageDataset, Dataset, Void> {
    static final int RESOURCE = R.layout.list_row_layout2;

    private static class ViewHolder {
      ImageView img;
      TextView textLang;
      TextView textCount;
    }

    public LanguagesAdapter(@NonNull Context context, final Dataset storage) {
      super(context, RESOURCE, storage, new AdapterFilter<Dataset.LanguageDataset, Dataset, Void>() {
        public List<Dataset.LanguageDataset> selectFrom(Dataset dataset, Void dummy) {
          // Filter out any languages without installed keyboards.  This can occur with ad-hoc
          // lexical model installations.
          ArrayList<Dataset.LanguageDataset> languages = new ArrayList<>();

          for(Dataset.LanguageDataset language: dataset.asList()) {
            if(language.keyboards.size() > 0) {
              languages.add(language);
            }
          }

          return languages;
        }
      }, null);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      Dataset.LanguageDataset data = this.getItem(position);
      ViewHolder holder;

      // If we're being told to reuse an existing view, do that.  It's automatic optimization.
      if (convertView == null) {
        convertView = LayoutInflater.from(getContext()).inflate(RESOURCE, parent, false);
        holder = new ViewHolder();

        holder.img = convertView.findViewById(R.id.image1);
        holder.textLang = convertView.findViewById(R.id.text1);
        holder.textCount = convertView.findViewById(R.id.text2);
        convertView.setTag(holder);
      } else {
        holder = (ViewHolder) convertView.getTag();
      }

      holder.textLang.setText(data.name);
      holder.img.setImageResource(R.drawable.ic_arrow_forward);

      if(data.keyboards.size() == 1) {
        holder.textCount.setText("(1 keyboard)");
      } else {
        holder.textCount.setText("(" + data.keyboards.size() + " keyboards)");
      }

      return convertView;
    }
  }

  // TODO:  Handle within the new CloudRepository class (or similar other class)
  private static void checkKeyboardUpdates(final Context context) {
    new AsyncTask<Void, Integer, Integer>() {
      private final boolean hasConnection = KMManager.hasConnection(context);
      private ProgressDialog progressDialog;
      JSONParser jsonParser = new JSONParser();
      private final Context _context = context;

      @Override
      protected void onPreExecute() {
        super.onPreExecute();
        checkingUpdates = true;
        if (hasConnection) {
          progressDialog = new ProgressDialog(context);
          progressDialog.setMessage(context.getString(R.string.checking_keyboard_updates));
          progressDialog.setCancelable(false);
          if (!((AppCompatActivity) context).isFinishing()) {
            progressDialog.show();
          } else {
            cancel(true);
            progressDialog = null;
          }
        }
      }

      @Override
      protected Integer doInBackground(Void... voids) {
        int ret = 0;
        if (hasConnection && !isCancelled()) {
          try {
            String deviceType = context.getString(R.string.device_type);
            if (deviceType.equals("AndroidTablet")) {
              deviceType = "androidtablet";
            } else {
              deviceType = "androidphone";
            }

            List<Keyboard> keyboardsList = KeyboardPickerActivity.getInstalledDataset(_context).keyboards.asList();

            keyboardVersions = new HashMap<>();
            int len = keyboardsList.size();
            for (int i = 0; i < len; i++) {
              String languageID = keyboardsList.get(i).map.get(KMManager.KMKey_LanguageID);
              String keyboardID = keyboardsList.get(i).map.get(KMManager.KMKey_KeyboardID);
              String kbVersion = keyboardsList.get(i).map.get(KMManager.KMKey_KeyboardVersion);
              String url = String.format("%s/%s/%s?version=%s&device=%s&languageidtype=bcp47",
                  KMKeyboardDownloaderActivity.kKeymanApiBaseURL, languageID, keyboardID,  BuildConfig.VERSION_NAME, deviceType);
              JSONObject kbData = jsonParser.getJSONObjectFromUrl(url);
              JSONObject language = kbData.optJSONObject(KMKeyboardDownloaderActivity.KMKey_Language);
              JSONArray keyboards = language.getJSONArray(KMKeyboardDownloaderActivity.KMKey_LanguageKeyboards);
              JSONObject keyboard = keyboards.getJSONObject(0);
              String newKbVersion = keyboard.optString(KMManager.KMKey_KeyboardVersion, "1.0");
              String kbKey = String.format("%s_%s", languageID, keyboardID);
              if (keyboardVersions.get(kbKey) == null) {
                keyboardVersions.put(kbKey, newKbVersion);
              }

              if (FileUtils.compareVersions(newKbVersion, kbVersion) == FileUtils.VERSION_GREATER) {
                ret++;
              }
            }
          } catch (Exception e) {
            Log.e(TAG, "Failed to compare keyboard version. Error: " + e);
            keyboardVersions = null;
            ret = -1;
          }
        }

        return ret;
      }

      @Override
      protected void onProgressUpdate(Integer... progress) {
        // Do nothing
      }

      @Override
      protected void onPostExecute(Integer result) {
        if (progressDialog != null && progressDialog.isShowing()) {
          try {
            progressDialog.dismiss();
            progressDialog = null;
          } catch (Exception e) {
            progressDialog = null;
          }
        }

        if (result > 0) {
          failedUpdateCount = 0;
          updateCount = result;
          AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
          dialogBuilder.setTitle(context.getString(R.string.keyboard_updates_available));
          dialogBuilder.setMessage(context.getString(R.string.confirm_update));
          dialogBuilder.setPositiveButton(context.getString(R.string.label_update), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              // Update keyboards
              if (KMManager.hasConnection(context)) {
                List<Keyboard> keyboardsList = KeyboardPickerActivity.getInstalledDataset(_context).keyboards.asList();

                int len = keyboardsList.size();
                for (int i = 0; i < len; i++) {
                  String pkgID = keyboardsList.get(i).map.get(KMManager.KMKey_PackageID);
                  String kbID = keyboardsList.get(i).map.get(KMManager.KMKey_KeyboardID);
                  String langID = keyboardsList.get(i).map.get(KMManager.KMKey_LanguageID);
                  String kbKey = String.format("%s_%s", langID, kbID);
                  String langName = keyboardsList.get(i).map.get(KMManager.KMKey_LanguageName);
                  String kbName = keyboardsList.get(i).map.get(KMManager.KMKey_KeyboardName);
                  String kbVersion = keyboardsList.get(i).map.get(KMManager.KMKey_KeyboardVersion);
                  String newKbVersion = keyboardVersions.get(kbKey);
                  if (newKbVersion != null) {
                    keyboardVersions.put(kbKey, newKbVersion);
                    if (FileUtils.compareVersions(newKbVersion, kbVersion) == FileUtils.VERSION_GREATER) {
                      if (updateProgress == null || !updateProgress.isShowing()) {
                        updateProgress = new ProgressDialog(context);
                        updateProgress.setMessage(context.getString(R.string.updating_keyboards));
                        updateProgress.setCancelable(false);
                        updateProgress.show();
                      }

                      Bundle bundle = new Bundle();
                      bundle.putString(KMKeyboardDownloaderActivity.ARG_PKG_ID, pkgID);
                      bundle.putString(KMKeyboardDownloaderActivity.ARG_KB_ID, kbID);
                      bundle.putString(KMKeyboardDownloaderActivity.ARG_LANG_ID, langID);
                      bundle.putString(KMKeyboardDownloaderActivity.ARG_KB_NAME, kbName);
                      bundle.putString(KMKeyboardDownloaderActivity.ARG_LANG_NAME, langName);
                      bundle.putBoolean(KMKeyboardDownloaderActivity.ARG_IS_CUSTOM, false);
                      Intent intent = new Intent(context, KMKeyboardDownloaderActivity.class);
                      intent.putExtras(bundle);
                      context.startActivity(intent);
                    }
                  }
                }
              } else {
                Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
                checkingUpdates = false;
              }
            }
          });

          dialogBuilder.setNegativeButton(context.getString(R.string.label_later), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              lastUpdateCheck = Calendar.getInstance();
              checkingUpdates = false;
            }
          });

          AlertDialog dialog = dialogBuilder.create();
          if (!((AppCompatActivity) context).isFinishing()) {
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
              @Override
              public void onCancel(DialogInterface dialog) {
                lastUpdateCheck = Calendar.getInstance();
                checkingUpdates = false;
              }
            });
            dialog.show();
          } else {
            checkingUpdates = false;
          }
        } else if (result == 0) {
          Toast.makeText(context, "All keyboards are up to date!", Toast.LENGTH_SHORT).show();
          lastUpdateCheck = Calendar.getInstance();
          SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.kma_prefs_name), Context.MODE_PRIVATE);
          SharedPreferences.Editor editor = prefs.edit();
          editor.putLong("lastUpdateCheck", lastUpdateCheck.getTime().getTime());
          editor.commit();
          checkingUpdates = false;
        } else {
          Toast.makeText(context, "Failed to access Keyman server!", Toast.LENGTH_SHORT).show();
          lastUpdateCheck = Calendar.getInstance();
          updateCheckFailed = true;
          checkingUpdates = false;
        }
      }
    }.execute();
  }

  @Override
  public void onKeyboardDownloadStarted(HashMap<String, String> keyboardInfo) {
    // Do nothing
  }

  @Override
  public void onKeyboardDownloadFinished(HashMap<String, String> keyboardInfo, int result) {
    if (result > 0) {
      KeyboardPickerActivity.handleDownloadedKeyboard(this, keyboardInfo);
    } else if (result < 0) {
      failedUpdateCount++;
    }

    if (updateCount > 0) {
      updateCount--;
    }

    if (updateCount == 0 && updateProgress != null && updateProgress.isShowing()) {
      if (updateProgress != null && updateProgress.isShowing()) {
        try {
          updateProgress.dismiss();
          updateProgress = null;
        } catch (Exception e) {
          updateProgress = null;
        }
      }

      if (failedUpdateCount > 0) {
        Toast.makeText(this, "One or more keyboards failed to update!", Toast.LENGTH_SHORT).show();
        keyboardVersions = null;
        lastUpdateCheck = Calendar.getInstance();
        updateFailed = true;
        checkingUpdates = false;
      } else {
        Toast.makeText(this, "Keyboards successfully updated!", Toast.LENGTH_SHORT).show();
        lastUpdateCheck = Calendar.getInstance();
        SharedPreferences prefs = getSharedPreferences(getString(R.string.kma_prefs_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("lastUpdateCheck", lastUpdateCheck.getTime().getTime());
        editor.commit();
        checkingUpdates = false;
      }
    }
    if (updateProgress != null && updateProgress.isShowing()) {
      updateProgress.dismiss();
    }
  }

  @Override
  public void onPackageInstalled(List<Map<String, String>> keyboardsInstalled) {
    // Do nothing
  }

  @Override
  public void onLexicalModelInstalled(List<Map<String, String>> lexicalModelsInstalled) {
    // Do nothing
  }

}