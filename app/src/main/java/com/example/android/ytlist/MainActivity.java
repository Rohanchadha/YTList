package com.example.android.ytlist;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Result;


public class MainActivity extends Activity implements View.OnClickListener {

    //keep track of camera capture intent
    final int CAMERA_CAPTURE = 1;
    //captured picture uri
    private Uri picUri;
    private Uri cropUri;
    //keep track of cropping intent
    final int PIC_CROP = 2;
    //edittext for getting the tags input
    //EditText editTextTags;
    private String ba1;
    public static String URL = "http://10.105.24.161:8080/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        //retrieve a reference to the UI button
        Button captureBtn = (Button)findViewById(R.id.capture_btn);
        //handle button clicks
        captureBtn.setOnClickListener(this);

        Button submitBtn = (Button)findViewById(R.id.submit_btn);
        //handle button clicks
        submitBtn.setOnClickListener(this);


    }

    public void onClick(View v) {
        if (v.getId() == R.id.capture_btn) {
            try {
                //use standard intent to capture an image
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "bookImage");
                picUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, picUri);
                //we will handle the returned data in onActivityResult
                startActivityForResult(captureIntent, CAMERA_CAPTURE);
            } catch (ActivityNotFoundException anfe) {
                //display an error message
                String errorMessage = "Whoops - your device doesn't support capturing images!";
                Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        if (v.getId() == R.id.submit_btn) {
            Intent submitMenuIntent = new Intent(this, submitVideoActivity.class);
            startActivity(submitMenuIntent);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            //user is returning from capturing an image using the camera
            if(requestCode == CAMERA_CAPTURE){
                //get the Uri for the captured image
                //picUri = data.getData();
                //carry out the crop operation
                performCrop();
            } //user is returning from cropping the image
            else if(requestCode == PIC_CROP){
                //get the returned data
                Bundle extras = data.getExtras();
                Uri croppedUri = data.getData();
                Bitmap thePic = decodeUriAsBitmap(croppedUri);
                //get the cropped bitmap
                //Bitmap thePic = extras.getParcelable("data");//retrieve a reference to the ImageView
                ImageView picView = (ImageView)findViewById(R.id.picture);
                //display the returned cropped image
                picView.setImageBitmap(thePic);
                List<String> videoIds = upload(thePic);
                //uploadBitmap(thePic);
//                Intent menuIntent = new Intent(this, VideoActivity.class);
//                startActivity(menuIntent);

            }
        }
    }

    private Bitmap decodeUriAsBitmap(Uri uri){
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    private void performCrop(){
        try {
            //call the standard crop action intent (the user device may not support it)
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            //indicate image type and Uri
            cropIntent.setDataAndType(picUri, "image/*");
            //set crop properties
            cropIntent.putExtra("crop", "true");
            //indicate aspect of desired crop
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            //indicate output X and Y
            cropIntent.putExtra("outputX", 256);
            cropIntent.putExtra("outputY", 256);
            //retrieve data on return
            cropIntent.putExtra("return-data", true);

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "cropped");
            picUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, picUri);
            //start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, PIC_CROP);
        }
        catch(ActivityNotFoundException anfe){
            //display an error message
            String errorMessage = "Whoops - your device doesn't support the crop action!";
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
        }
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (picUri != null) {
            outState.putString("cameraImageUri", picUri.toString());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey("cameraImageUri")) {
            picUri = Uri.parse(savedInstanceState.getString("cameraImageUri"));
        }
    }

    private List<String> upload(Bitmap bitmap) {

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bao);
        byte[] ba = bao.toByteArray();
        ba1 = Base64.encodeToString(ba, Base64.DEFAULT);
        AsyncTask<Void, Void, List<String>> resutls = new uploadToServer(this).execute();
        // Upload image to server
//        ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
//        nameValuePairs.add(new BasicNameValuePair("base64", ba1));
//        nameValuePairs.add(new BasicNameValuePair("ImageName", System.currentTimeMillis() + ".jpg"));
//        try {
//            HttpClient httpclient = new DefaultHttpClient();
//            HttpPost httppost = new HttpPost(URL);
//            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
//            HttpResponse response = httpclient.execute(httppost);
//            String st = EntityUtils.toString(response.getEntity());
//            Log.v("log_tag", "In the try Loop" + st);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.v("log_tag", "Error in http connection " + e.toString());
//        }
        //new uploadToServer().execute();
        return null;

    }

    public class uploadToServer extends AsyncTask<Void, Void, List<String>> {

        private Activity activity;

        public uploadToServer(Activity activity) {
            this.activity = activity;
        }

        private ProgressDialog pd = new ProgressDialog(MainActivity.this);
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setMessage("Wait image uploading!");
            pd.show();
        }

        @Override
        protected List<String> doInBackground(Void... params) {

            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("base64", ba1));
            nameValuePairs.add(new BasicNameValuePair("ImageName", System.currentTimeMillis() + ".jpg"));
            List<String> linesList = new ArrayList<>();
            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(URL);
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse response = httpclient.execute(httppost);
                String responseString = EntityUtils.toString(response.getEntity(), "UTF-8");
                System.out.println("*************************: "+responseString);
                JSONArray lines = new JSONArray(responseString);

                for(int i = 0; i< lines.length(); i++) {
                    linesList.add(lines.getString(0));
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.v("log_tag", "Error in http connection " + e.toString());
            }
            Intent menuIntent = new Intent(activity, VideoActivity.class);
            String [] youLines = linesList.toArray(new String[0]);
            menuIntent.putExtra("youtubeIds", youLines);
            startActivity(menuIntent);
            return linesList;

        }

        protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);
            pd.hide();
            pd.dismiss();
        }
    }

}
