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
import android.widget.ImageView;
import android.widget.TextView;
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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by My sister is awesome on 1/19/2019.
 */

public class submitVideoActivity extends Activity implements View.OnClickListener{
    //keep track of camera capture intent
    final int CAMERA_CAPTURE = 1;
    //captured picture uri
    private Uri picUri;
    private Uri cropUri;
    //keep track of cropping intent
    final int PIC_CROP = 2;

    //edittext for getting the tags input
    //EditText editTextTags;
    public static String URL = "http://10.105.24.161:8080/store";
    private String ba1;
    private String youTubeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.submit_video);
        //retrieve a reference to the UI button
        Button submitImage = (Button)findViewById(R.id.submit_image);
        //handle button clicks
        submitImage.setOnClickListener(this);
        Button submitVideo = (Button)findViewById(R.id.button);
        //handle button clicks
        submitVideo.setOnClickListener(this);
    }

    public void onClick(View v) {
        if (v.getId() == R.id.submit_image) {
            try {
                //use standard intent to capture an image
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.TITLE, "submittedImage");
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
        } else if (v.getId() == R.id.button) {
            TextView videoUrl = (TextView)findViewById(R.id.editText);
            youTubeId = videoUrl.getText().toString();
            Bitmap thePic = decodeUriAsBitmap(cropUri);
            //get the cropped bitmap
            //Bitmap thePic = extras.getParcelable("data");//retrieve a reference to the ImageView
            List<String> videoIds = upload(thePic);
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
                cropUri = croppedUri;
                Bitmap thePic = decodeUriAsBitmap(croppedUri);
                //get the cropped bitmap
                //Bitmap thePic = extras.getParcelable("data");//retrieve a reference to the ImageView
                ImageView picView = (ImageView)findViewById(R.id.picture_2);
                //display the returned cropped image
                picView.setImageBitmap(thePic);
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


    public byte[] getFileDataFromDrawable(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    private List<String> upload(Bitmap bitmap) {

        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bao);
        byte[] ba = bao.toByteArray();
        ba1 = Base64.encodeToString(ba, Base64.DEFAULT);
        AsyncTask<Void, Void, List<String>> resutls = new uploadToServer(this).execute();
        return null;

    }

    public class uploadToServer extends AsyncTask<Void, Void, List<String>> {

        private Activity activity;

        public uploadToServer(Activity activity) {
            this.activity = activity;
        }

        private ProgressDialog pd = new ProgressDialog(submitVideoActivity.this);
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setMessage("Wait image uploading!");
            pd.show();
        }

        @Override
        protected List<String> doInBackground(Void... params) {

            ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("base64", ba1));
            nameValuePairs.add(new BasicNameValuePair("url", youTubeId));
            List<String> linesList = new ArrayList<>();
            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost(URL);
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse response = httpclient.execute(httppost);


            } catch (Exception e) {
                e.printStackTrace();
                Log.v("log_tag", "Error in http connection " + e.toString());
            }

            return null;

        }

        protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);
            pd.hide();
            pd.dismiss();
        }
    }

}
