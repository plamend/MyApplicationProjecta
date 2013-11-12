package net.wtfitio.qrticket;

import net.wtfitio.qrticket.CameraPreview;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.Button;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;

import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.ImageFormat;
import android.widget.Toast;

/* Import ZBar Class files */
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import net.sourceforge.zbar.Config;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class QRticket extends Activity
{
    private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    TextView scanText;
    Button scanButton;
    String qr;
    View layoutt,layoutf;

    Toast toastt,toastf;
    ImageScanner scanner;

    private boolean barcodeScanned = false;
    private boolean previewing = true;




    static {
        System.loadLibrary("iconv");
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy); }

        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
        preview.addView(mPreview);

        scanText = (TextView)findViewById(R.id.scanText);
        scanButton = (Button)findViewById(R.id.ScanButton);

        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (barcodeScanned) {
                    barcodeScanned = false;
                    scanText.setText("Scanning...");
                    mCamera.setPreviewCallback(previewCb);
                    mCamera.startPreview();
                    previewing = true;
                    mCamera.autoFocus(autoFocusCB);
                    scanText.setBackgroundColor(-3355444);
                }
            }
        });
    }

    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (previewing)
                mCamera.autoFocus(autoFocusCB);
        }
    };

    PreviewCallback previewCb = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);

            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();

                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {
                    scanText.setText("barcode result " + sym.getData());
                    //postData(sym.getData());
                    qr=sym.getData();
                    barcodeScanned = true;
                    PostData task = new PostData();
                    task.execute(qr);

                }
            }
        }
    };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 1000);
        }
    };

    public void postData(String qrcode) {

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://www.tarambuka.eu/ticket/index.php");

        try {
// Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("qr", qrcode));
            nameValuePairs.add(new BasicNameValuePair("stringdata", "AndDev is Cool!"));

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response =  httpclient.execute(httppost);
            HttpEntity responseEntity = response.getEntity();
            String reponceent = EntityUtils.toString(responseEntity);
            System.out.println("Response Code : "
                    + response.getStatusLine().getStatusCode());
            System.out.println("Response Code : "
                    +  reponceent);
               Toast.makeText(getBaseContext(),reponceent,Toast.LENGTH_LONG).show();
            if (reponceent.equals(qrcode)) {
                scanText.setBackgroundColor(-16711936);

            }

        } catch (ClientProtocolException e) {
// TODO Auto-generated catch block
        } catch (IOException e) {
// TODO Auto-generated catch block
        }
    }

private class PostData extends android.os.AsyncTask<String,String,String>{

    private String resp;
    @Override
    protected String doInBackground(String... params) {

        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://test.wtfitio.net/server.php");

        try {
// Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("qr", params[0]));
            nameValuePairs.add(new BasicNameValuePair("stringdata", "AndDev is Cool!"));

            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse response =  httpclient.execute(httppost);
            HttpEntity responseEntity = response.getEntity();
            String reponceent = EntityUtils.toString(responseEntity);
            System.out.println("Response Code : "
                    + response.getStatusLine().getStatusCode());
            System.out.println("Response Code : "
                    +  reponceent);

resp = reponceent;
        } catch (ClientProtocolException e) {
// TODO Auto-generated catch block
        } catch (IOException e) {
// TODO Auto-generated catch block
        }
        return resp;
    }
    @Override
    protected void onPostExecute(String result) {
        Toast.makeText(getBaseContext(),result,Toast.LENGTH_LONG).show();
String test = sha1Hash( qr );

        if (result.equals(sha1Hash( qr ))) {

            LayoutInflater inflater1 = getLayoutInflater();
            View layoutt = inflater1.inflate(R.layout.toast,(ViewGroup) findViewById(R.id.toast_layout_root));
            assert layoutt != null;
            ImageView imaget = (ImageView) layoutt.findViewById(R.id.image);
            TextView textt = (TextView) layoutt.findViewById(R.id.text);
            imaget.setImageResource(R.drawable.t);
            //textt.setBackgroundColor( -16711936);
            //textt.setText("Hello! This is a custom toast!");
            //scanText.setBackgroundColor(-16711936);
            Toast toastt = new Toast(getApplicationContext());
            toastt.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toastt.setDuration(Toast.LENGTH_LONG);
            toastt.setView(layoutt);
            toastt.show();
        }
        else{

            LayoutInflater inflater2 = getLayoutInflater();
            View layoutf =inflater2.inflate(R.layout.toast,(ViewGroup) findViewById(R.id.toast_layout_root));
            assert layoutf != null;
            ImageView imagef = (ImageView) layoutf.findViewById(R.id.image);
            TextView textf = (TextView) layoutf.findViewById(R.id.text);
            imagef.setImageResource(R.drawable.f);
            //textt.setBackgroundColor( -16711936);
            //textt.setText("Hello! This is a custom toast!");
            Toast toastf = new Toast(getApplicationContext());
            toastf.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            toastf.setDuration(Toast.LENGTH_LONG);
            toastf.setView(layoutf);
            toastf.show();
        }
    }

    @Override
    protected void onPreExecute() {
        // Things to be done before execution of long running operation. For
        // example showing ProgessDialog
    }

    protected void onProgressUpdate(String... text) {
         // Things to be done while execution of long running operation is in
        // progress. For example updating ProgessDialog
    }
}

    String sha1Hash( String toHash )
    {
        String hash = null;
        try
        {
            MessageDigest digest = MessageDigest.getInstance( "SHA-1" );
            byte[] bytes = toHash.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for( byte b : bytes )
            {
                sb.append( String.format("%02X", b) );
            }
            hash = sb.toString();
        }
        catch( NoSuchAlgorithmException e )
        {
            e.printStackTrace();
        }
        catch( UnsupportedEncodingException e )
        {
            e.printStackTrace();
        }
        return hash;
    }

}