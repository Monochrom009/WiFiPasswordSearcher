package com.example.WiFiPasswordSearcher;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.json.*;


public class WPSActivity extends Activity
{
	ArrayList<ItemWps> data = new ArrayList<ItemWps>();

	ProgressDialog pd=null;
	private Settings mSettings;
    public static String SERVER_URI = "";
    public static String API_READ_KEY = "";

	ArrayList<String> wpsPin = new ArrayList<String>();
	ArrayList<String> wpsMet = new ArrayList<String>();
	ArrayList<String> wpsScore = new ArrayList<String>();
	ArrayList<String> wpsDb = new ArrayList<String>();

	@Override
    public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        setContentView(R.layout.wps);

		if (android.os.Build.VERSION.SDK_INT > 9)
		{
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        mSettings = new Settings(getApplicationContext());

        SERVER_URI = mSettings.AppSettings.getString(Settings.APP_SERVER_URI, "http://3wifi.stascorp.com");
        API_READ_KEY = mSettings.AppSettings.getString(Settings.API_READ_KEY, "");

		ActionBar actionBar = getActionBar(); actionBar.hide();

		TextView ESSDWpsText = (TextView)findViewById(R.id.ESSDWpsTextView);
		String ESSDWps = getIntent().getExtras().getString("variable");
		ESSDWpsText.setText(ESSDWps); // ESSID
		TextView BSSDWpsText = (TextView)findViewById(R.id.BSSDWpsTextView);
		final String BSSDWps = getIntent().getExtras().getString("variable1");
		BSSDWpsText.setText(BSSDWps); // BSSID

		new GetPinsFromBase().execute(BSSDWps);

    }
	private class GetPinsFromBase extends AsyncTask <String, Void, String>
	{
		@Override
		protected void onPreExecute()
		{
			super.onPreExecute();
			pd = ProgressDialog.show(WPSActivity.this, "Please wait...", "Getting pins...");
		}


		protected String doInBackground(String[] BSSDWps)
		{
			String BSSID = BSSDWps[0];
			String response = "";
			String response2 = "";
			data.clear();
			wpsScore.clear();
			wpsDb.clear();
			wpsPin.clear();
			wpsMet.clear();
			DefaultHttpClient hc = new DefaultHttpClient();
			DefaultHttpClient hc2 = new DefaultHttpClient();
			ResponseHandler<String> res = new BasicResponseHandler();
			ResponseHandler<String> res2 = new BasicResponseHandler();

			HttpPost http2 = new HttpPost("http://wpsfinder.com/ethernet-wifi-brand-lookup/MAC:" + BSSID);
			try
			{
				response2 = hc2.execute(http2, res2);
				response2 = response2.substring(response2.indexOf("muted'><center>") + 15, response2.indexOf("<center></h4><h6"));
			}
			catch (Exception e)
			{}

			HttpGet http = new HttpGet("http://3wifi.stascorp.com/api/apiwps?key=" + API_READ_KEY + "&bssid=" + BSSID);
			try
			{
				response = hc.execute(http, res);
			}
			catch (Exception e)
			{
			    e.printStackTrace();
			    return null;
			}
			try
			{
				JSONObject jObject = new JSONObject(response);
				jObject = jObject.getJSONObject("data");
				jObject = jObject.getJSONObject(BSSID);

				JSONArray array =jObject.optJSONArray("scores");
				for (int i = 0; i < array.length(); i++)
				{
					jObject = array.getJSONObject(i);
					wpsPin.add(jObject.getString("value"));
					wpsMet.add(jObject.getString("name"));
					wpsScore.add(jObject.getString("score"));
					if (jObject.getBoolean("fromdb"))
					{
						wpsDb.add("✔");
					}
					else
					{wpsDb.add("");}
					Integer score = Math.round(Float.parseFloat(wpsScore.get(i)) * 100);
					wpsScore.set(i, Integer.toString(score) + "%");

					data.add(new ItemWps(wpsPin.get(i), wpsMet.get(i), wpsScore.get(i), wpsDb.get(i)));
				}
			}
			catch (JSONException e)
			{
			    e.printStackTrace();
			}

			return response2;
		}

		@Override
		protected void onPostExecute(String response2)
		{

			pd.dismiss();
			ListView wpslist = (ListView)findViewById(R.id.WPSlist);
			if (data.isEmpty())
			{
				data.add(new ItemWps(null, "   not found", null, null));
				wpslist.setEnabled(false);
			}

			wpslist.setAdapter(new MyAdapterWps(WPSActivity.this, data));
			TextView VendorWpsText = (TextView)findViewById(R.id.VendorWpsTextView);
			if (response2.length() > 50)
			{response2 = "unknown vendor";}
			VendorWpsText.setText(response2);

			wpslist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View itemClicked, int position,
											long id)
					{
						Toast.makeText(getApplicationContext(), "Pin " + wpsPin.get(position) + " copied",
									   Toast.LENGTH_SHORT).show();
						ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
						ClipData dataClip = ClipData.newPlainText("text", wpsPin.get(position));
						clipboard.setPrimaryClip(dataClip);
					}
				});
		}
    }
	public void btnwpsbaseclick(View view)
	{ //пины из базы
		String BSSDWps = getIntent().getExtras().getString("variable1");
		//getWpsFromBase(BSSDWps);
		new GetPinsFromBase().execute(BSSDWps);
	}

	public void btnGenerate(View view)
	{ //генераторpppppp
		ListView wpslist = (ListView)findViewById(R.id.WPSlist);
		wpslist.setAdapter(null);
	}


	//Toast
	public void toastMessage(String text)
	{
		Toast toast = Toast.makeText(getApplicationContext(),
									 text, Toast.LENGTH_LONG);
		toast.show();
	}
}