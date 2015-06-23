package darack.com.player;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

public class SettingActivity extends PreferenceActivity implements
		OnPreferenceClickListener, OnPreferenceChangeListener {

	public static final int MSG_PREFERENCE_CHANGE = 301;
	private static final String TAG = "TEST_DEBUG";

	private Preference about;
	private Preference feedback;

	private EditTextPreference gab;

	private EditTextPreference space;
	private EditTextPreference testCount;
	private EditTextPreference condition;

	private Resources r;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.setting);

		r = getResources();

		about = findPreference("ABOUT");
		feedback = findPreference("FEEDBACK");

		gab = (EditTextPreference) findPreference("GAB");

		space = (EditTextPreference) findPreference("SPACE");
		testCount = (EditTextPreference) findPreference("TESTCOUNT");
		condition = (EditTextPreference) findPreference("CONDITION");

		about.setOnPreferenceClickListener(this);
		feedback.setOnPreferenceClickListener(this);

		gab.setOnPreferenceChangeListener(this);

		space.setOnPreferenceChangeListener(this);
		testCount.setOnPreferenceChangeListener(this);
		condition.setOnPreferenceChangeListener(this);

	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object arg1) {
		// TODO Auto-generated method stub
		int value = Integer.valueOf((String) arg1);

		Log.d("PRE", "value : " + value);
		Log.d("PRE", "preference : [" + preference.getKey() + "]");

		if (preference == space) {
			// 유효성 검사
			if (value > 0 && value < 10000) {
				return true;
			}
		} else if (preference == testCount) {
			// 유효성 검사
			if (value > 0 && value < 1000) {
				return true;
			}
		} else if (preference == condition) {
			// 유효성 검사
			if (value > 0 && value < 128) {
				return true;
			}
		} else if (preference == gab) {
			// 유효성 검사
			if (value > 0 && value < 20000) {
				return true;
			}
		}

		Toast.makeText(this, r.getString(R.string.test_fail),
				Toast.LENGTH_SHORT).show();
		return false;
	}

	@Override
	public boolean onPreferenceClick(Preference arg0) {
		// TODO Auto-generated method stub
		if (arg0.getKey().equals("ABOUT")) {
			String msg;
			msg = r.getString(R.string.set_pro_about_title);
			msg += "\n\n" + r.getString(R.string.set_pro_about_creator);
			msg += "\n" + r.getString(R.string.set_pro_about_creator1);
			msg += "\n" + r.getString(R.string.set_pro_about_creator2);
			msg += "\n" + r.getString(R.string.set_pro_about_creator3);

			String version = "";
			try {
				PackageInfo i = getPackageManager().getPackageInfo(
						getPackageName(), 0);
				version = i.versionName;
			} catch (NameNotFoundException e) {
				Log.e(TAG, e.getMessage());
			}

			msg += "\n\n" + r.getString(R.string.set_pro_about_version) + " "
					+ version;

			new AlertDialog.Builder(this)
					.setIcon(R.drawable.m_icon)
					.setTitle(R.string.set_pro_about)
					.setMessage(msg)
					.setNeutralButton("OK",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int whichButton) {
									// ...할일
								}
							}).show();
			return true;
		} else if (arg0.getKey().equals("FEEDBACK")) {
			// 이메일 발송
			Uri uri = Uri.parse("mailto:cless00@gmail.com");
			Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
			startActivity(intent);
			return true;
		}

		return false;
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		Intent intent = getIntent();
		setResult(RESULT_OK, intent);
		super.onBackPressed();
	}

}
