package th.ac.tu.siit.its333.lab8exercise1;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MessageActivity extends ActionBarActivity implements Runnable {

    int timestamp = 0;
    ArrayList<Map<String, String>> data;
    SimpleAdapter adapter;
    String user;
    long lastUpdate = 0;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        data = new ArrayList<Map<String, String>>();
        adapter = new SimpleAdapter(this,
                data,
                android.R.layout.simple_list_item_2,
                new String[] {"user", "message"},
                new int[] {android.R.id.text1, android.R.id.text2});
        ListView l = (ListView)findViewById(R.id.listView);
        l.setAdapter(adapter);

        Intent i = this.getIntent();
        user = i.getStringExtra("user");
        LoadMessageTask task = new LoadMessageTask();
        task.execute();
        handler = new Handler();
        handler.postDelayed(this, 30000);


    }

    @Override
    public void run() {

        Toast t = Toast.makeText(this.getApplicationContext(),
                "Called by handler", Toast.LENGTH_SHORT);
        t.show();


        LoadMessageTask task = new LoadMessageTask();
        task.execute();

        handler.postDelayed(this, 30000); //execute again after another 30 seconds


    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(this);
    }

    public void buttonClicked(View v) {
        EditText etMessage = (EditText)findViewById(R.id.etMessage);
        String message = etMessage.getText().toString().trim();
        if (message.length() > 0) {
            PostMessageTask p = new PostMessageTask();
            p.execute(user, message);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            LoadMessageTask task = new LoadMessageTask();
            task.execute();
            handler.removeCallbacks(this);
            handler.postDelayed(this, 30000);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class LoadMessageTask extends AsyncTask<String, Void, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            BufferedReader reader;
            StringBuilder buffer = new StringBuilder();
            String line;

            try {

                Log.e("LoadMessageTask", ""+ timestamp);
                URL u = new URL("http://ict.siit.tu.ac.th/~cholwich/microblog/fetch.php?time="
                        + timestamp);
                HttpURLConnection h = (HttpURLConnection)u.openConnection();
                h.setRequestMethod("GET");
                h.setDoInput(true);
                h.connect();

                int response = h.getResponseCode();
                if (response == 200) {
                    reader = new BufferedReader(new InputStreamReader(h.getInputStream()));
                    while((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }

                    Log.e("LoadMessageTask", buffer.toString());
                    //Parsing JSON and displaying messages
                    JSONObject jMessage = new JSONObject(buffer.toString());
                    JSONArray jArray = jMessage.getJSONArray("msg");
                    int msgNum = jArray.length();

                    //To append a new message:
                    Map<String, String> item = new HashMap<String, String>();
                    for (int i = 0; i<msgNum; i++){
                        item = new HashMap<String, String>();
                        item.put("user", jArray.getJSONObject(i).getString("user"));
                        item.put("message", jArray.getJSONObject(i).getString("message"));
                        data.add(0, item);

                        if(i==msgNum-1){
                            timestamp = jArray.getJSONObject(i).getInt("time");
                        }
                    }







                    //JSONObject json = new JSONObject(buffer.toString());

                }
            } catch (MalformedURLException e) {
                Log.e("LoadMessageTask", "Invalid URL");
            } catch (IOException e) {
                Log.e("LoadMessageTask", "I/O Exception");
            } catch (JSONException e) {
                Log.e("LoadMessageTask", "Invalid JSON");
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                adapter.notifyDataSetChanged();
                lastUpdate = System.currentTimeMillis();
                Toast t = Toast.makeText(MessageActivity.this.getApplicationContext(),
                        "Updated the timeline",
                        Toast.LENGTH_SHORT);
                t.show();
            }
        }
    }

    class PostMessageTask extends AsyncTask<String, Void, Boolean> {
        String line,res="";
        StringBuilder buffer = new StringBuilder();

        @Override
        protected Boolean doInBackground(String... params) {
            String user = params[0];
            String message = params[1];
            HttpClient h = new DefaultHttpClient();
            HttpPost p = new HttpPost("http://ict.siit.tu.ac.th/~cholwich/microblog/post.php");

            List<NameValuePair> values = new ArrayList<NameValuePair>();
            values.add(new BasicNameValuePair("user", user));
            values.add(new BasicNameValuePair("message", message));
            try {
                p.setEntity(new UrlEncodedFormEntity(values));
                HttpResponse response = h.execute(p);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent()));
                while((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                JSONObject json = new JSONObject(buffer.toString());
                res = json.getString("response");



            } catch (UnsupportedEncodingException e) {
                Log.e("Error", "Invalid encoding");
            } catch (ClientProtocolException e) {
                Log.e("Error", "Error in posting a message");
            } catch (IOException e) {
                Log.e("Error", "I/O Exception");
            }catch (JSONException e) {
                Log.e("LoadMessageTask", "Invalid JSON");
            }


            if(res.equals("true")){
                return true;
            }
            else{
                return false;
            }

        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                Toast t = Toast.makeText(MessageActivity.this.getApplicationContext(),
                        "Successfully post your status",
                        Toast.LENGTH_SHORT);
                t.show();
            }
            else {
                Toast t = Toast.makeText(MessageActivity.this.getApplicationContext(),
                        "Unable to post your status",
                        Toast.LENGTH_SHORT);
                t.show();
            }
        }
    }
}
