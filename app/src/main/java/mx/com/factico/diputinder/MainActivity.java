package mx.com.factico.diputinder;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;

import java.util.ArrayList;
import java.util.List;

import mx.com.factico.diputinder.adapters.MyArrayAdapter;
import mx.com.factico.diputinder.beans.Address;
import mx.com.factico.diputinder.beans.Candidatos;
import mx.com.factico.diputinder.beans.Diputado;
import mx.com.factico.diputinder.dialogues.Dialogues;
import mx.com.factico.diputinder.httpconnection.HttpConnection;
import mx.com.factico.diputinder.location.LocationClientListener;
import mx.com.factico.diputinder.location.LocationUtils;
import mx.com.factico.diputinder.parser.GsonParser;
import mx.com.factico.diputinder.utils.ScreenUtils;

public class MainActivity extends ActionBarActivity implements View.OnClickListener {
    public static final String TAG_CLASS = MainActivity.class.getSimpleName();

    private List<Diputado> diputados = new ArrayList<>();
    private List<Diputado> auxDiputados = new ArrayList<>();
    private MyArrayAdapter arrayAdapter;
    private int i;

    private SwipeFlingAdapterView flingContainer;

    private LocationClientListener clientListener;
    private LatLng userLocation;
    private Address address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar();
        initLocationClientListener();
        initUI();
    }

    protected void setSupportActionBar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.actionbar);
        mToolbar.setTitle("");
        mToolbar.getBackground().setAlpha(255);
        TextView actionbarTitle = (TextView) mToolbar.findViewById(R.id.actionbar_title);
        actionbarTitle.setText(getResources().getString(R.string.app_name));

        setSupportActionBar(mToolbar);
    }

    protected void initLocationClientListener() {
        clientListener = new LocationClientListener(MainActivity.this);
        clientListener.setOnLocationClientListener(new LocationClientListener.OnLocationClientListener() {
            @Override
            public void onLocationChanged(Location location) {
                userLocation = LocationUtils.getLatLngFromLocation(location);
                //Dialogues.Toast(getBaseContext(), "Find location: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_LONG);

                address = LocationUtils.getAdressFromLatLong(getBaseContext(), location.getLatitude(), location.getLongitude());

                if (address != null) {
                    /*Dialogues.Log(TAG_CLASS, "Address: " + address.getAddress(), Log.ERROR);
                    Dialogues.Log(TAG_CLASS, "City: " + address.getCity(), Log.ERROR);
                    Dialogues.Log(TAG_CLASS, "State: " + address.getState(), Log.ERROR);
                    Dialogues.Log(TAG_CLASS, "Country: " + address.getCountry(), Log.ERROR);
                    Dialogues.Log(TAG_CLASS, "PostalCode: " + address.getPostalCode(), Log.ERROR);
                    Dialogues.Log(TAG_CLASS, "KnownName: " + address.getKnownName(), Log.ERROR);*/

                    loadDiputados();

                    clientListener.stopLocationUpdates();
                }
            }
        });
    }

    public void startLocationListener() {
        if (clientListener != null)
            clientListener.connect();
    }

    public void stopLocationListener() {
        if (clientListener != null)
            clientListener.disconnect();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (clientListener != null && userLocation == null) {
            startLocationListener();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (clientListener != null) {
            clientListener.stopLocationUpdates();
        }
    }

    protected void loadDiputados() {
        GetDiputadosPublicationsAsyncTask task = new GetDiputadosPublicationsAsyncTask();
        task.execute();
    }

    protected void initUI() {
        flingContainer = (SwipeFlingAdapterView) findViewById(R.id.main_swipe_tinder);

        // arrayAdapter = new ArrayAdapter<>(this, R.layout.item, R.id.helloText, diputados);
        arrayAdapter = new MyArrayAdapter(this, auxDiputados);

        flingContainer.setAdapter(arrayAdapter);
        arrayAdapter.notifyDataSetChanged();

        flingContainer.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                // this is the simplest way to delete an object from the Adapter (/AdapterView)
                //Log.d("LIST", "removed object!");
                auxDiputados.remove(0);
                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeftCardExit(Object dataObject) {
                //Do something on the left!
                //You also have access to the original object.
                //If you want to use it just cast it (String) dataObject
                //Dialogues.Toast(getBaseContext(), "Left!", Toast.LENGTH_SHORT);
            }

            @Override
            public void onRightCardExit(Object dataObject) {
                //Dialogues.Toast(getBaseContext(), "Right!", Toast.LENGTH_SHORT);

                startShareIntent();
            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
                // Ask for more data here
                //diputados.add(new Diputado("XML ".concat(String.valueOf(i))));
                //arrayAdapter.notifyDataSetChanged();
                //Log.d("LIST", "notified");
                //i++;
            }

            @Override
            public void onScroll(float scrollProgressPercent) {
                View view = flingContainer.getSelectedView();
                view.findViewById(R.id.item_swipe_right_indicator).setAlpha(scrollProgressPercent < 0 ? -scrollProgressPercent : 0);
                view.findViewById(R.id.item_swipe_left_indicator).setAlpha(scrollProgressPercent > 0 ? scrollProgressPercent : 0);
            }
        });

        // Optionally add an OnItemClickListener
        flingContainer.setOnItemClickListener(new SwipeFlingAdapterView.OnItemClickListener() {
            @Override
            public void onItemClicked(int itemPosition, Object dataObject) {
                //Dialogues.Toast(getBaseContext(), "Clicked!", Toast.LENGTH_SHORT);
                startIntentDiputado((Diputado) dataObject);
            }
        });

        Point point = ScreenUtils.getScreenSize(getBaseContext());
        int width = point.x / 4;

        View btnSwipeLeft = findViewById(R.id.main_btn_swipe_left);
        LinearLayout.LayoutParams paramsLeft = new LinearLayout.LayoutParams(width, width);
        paramsLeft.setMargins(0, 0, width / 3, 0);
        btnSwipeLeft.setLayoutParams(paramsLeft);
        btnSwipeLeft.setOnClickListener(this);

        View btnSwipeRight = findViewById(R.id.main_btn_swipe_right);
        LinearLayout.LayoutParams paramsRight = new LinearLayout.LayoutParams(width, width);
        paramsRight.setMargins(width / 3, 0, 0, 0);
        btnSwipeRight.setLayoutParams(paramsRight);
        btnSwipeRight.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_btn_swipe_left:
                swipeLeft();
                break;
            case R.id.main_btn_swipe_right:
                swipeRight();
                break;
        }
    }

    private void startIntentDiputado(Diputado diputado) {
        Intent intent = new Intent(getBaseContext(), DiputadoActivity.class);
        intent.putExtra(DiputadoActivity.TAG_DIPUTADO, diputado);
        startActivity(intent);
    }

    private void swipeLeft() {
        flingContainer.getTopCardListener().selectLeft();
    }

    private void swipeRight() {
        /**
         * Trigger the right event manually.
         */
        flingContainer.getTopCardListener().selectRight();
    }

    private void startShareIntent() {
        Intent shareIntent = findTwitterClient();
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Este es el texto que se debe compartir en Twitter");
        startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    public Intent findTwitterClient() {
        final String[] twitterApps = {
                // package // name - nb installs (thousands)
                "com.twitter.android", // official - 10 000
                "com.twidroid", // twidroid - 5 000
                "com.handmark.tweetcaster", // Tweecaster - 5 000
                "com.thedeck.android"}; // TweetDeck - 5 000 };
        Intent tweetIntent = new Intent();
        tweetIntent.setType("text/plain");
        final PackageManager packageManager = getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(tweetIntent, PackageManager.MATCH_DEFAULT_ONLY);

        for (int i = 0; i < twitterApps.length; i++) {
            for (ResolveInfo resolveInfo : list) {
                String p = resolveInfo.activityInfo.packageName;
                if (p != null && p.startsWith(twitterApps[i])) {
                    tweetIntent.setPackage(p);
                    return tweetIntent;
                }
            }
        }

        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class GetDiputadosPublicationsAsyncTask extends AsyncTask<String, String, String> {
        private ProgressDialog dialog;
        private String json_PDF;

        public GetDiputadosPublicationsAsyncTask() {}

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Obteniendo json de diputados");
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            json_PDF = HttpConnection.GET(HttpConnection.URL + HttpConnection.PDFS);
            return HttpConnection.GET(HttpConnection.URL + HttpConnection.DIPUTADOS);
        }

        @Override
        protected void onPostExecute(String result) {
            //Dialogues.Log(TAG_CLASS, "Result: " + result, Log.INFO);
            // Dialogues.Toast(getBaseContext(), "Result: " + result, Toast.LENGTH_LONG);

            if (result != null) {
                if (json_PDF != null && !json_PDF.equals("")) {
                    try {
                        diputados = GsonParser.getListDiputadosFromJSON(result);
                        Candidatos candidatos = GsonParser.getCandidatosFromJSON(json_PDF);

                        if (candidatos == null || diputados == null) {
                            return;
                        }

                        if (diputados.size() > 0) {
                            List<Diputado> diputadosUnorder = getListDiputadosFromState(diputados, address.getState());

                            auxDiputados = getOrderedListDiputados(diputadosUnorder);

                            if (auxDiputados != null && auxDiputados.size() > 0) {
                                List<Diputado> candidatosPDF = candidatos.getCandidatos();
                                if (candidatosPDF != null && candidatosPDF.size() > 0) {
                                    for (Diputado diputado : candidatosPDF) {
                                        if (auxDiputados.contains(diputado)) {
                                            int indexOf = auxDiputados.indexOf(diputado);
                                            if (indexOf != -1) {
                                                auxDiputados.get(indexOf).setPatrimonialPDF(diputado.getPatrimonialPDF());
                                                auxDiputados.get(indexOf).setFiscalPDF(diputado.getFiscalPDF());
                                                auxDiputados.get(indexOf).setInteresesPDF(diputado.getInteresesPDF());
                                            }
                                            //Dialogues.Log(TAG_CLASS, "Lo contiene: " + diputado.getNombres() + diputado.getApellidoPaterno(), Log.ERROR);
                                        } else {
                                            //Dialogues.Log(TAG_CLASS, "NO lo contiene: " + diputado.getNombres() + diputado.getApellidoPaterno(), Log.ERROR);
                                        }
                                    }
                                }

                                initUI();
                            } else {
                                Dialogues.Toast(getBaseContext(), "No se encontraron coincidencias en tu Entidad Federativa. " + address.getState(), Toast.LENGTH_SHORT);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }

            if (!this.isCancelled())
                this.cancel(true);
        }
    }

    protected List<Diputado> getOrderedListDiputados(List<Diputado> listDiputados) {
        List<Diputado> auxTwitter = new ArrayList<>();
        List<Diputado> auxNoTwitter = new ArrayList<>();

        for (Diputado diputado : listDiputados) {
            if (diputado.getTwitter() != null && !diputado.getTwitter().equals(""))
                auxTwitter.add(diputado);
            else
                auxNoTwitter.add(diputado);
        }

        if (auxNoTwitter.size() > 0)
            auxTwitter.addAll(auxNoTwitter);

        return auxTwitter;
    }

    protected List<Diputado> getListDiputadosFromState(List<Diputado> listDiputados, String state) {
        List<Diputado> auxListDiputados = new ArrayList<>();

        for (Diputado diputado : listDiputados) {
            if (diputado.getEntidadFederativa().equalsIgnoreCase(state))
                auxListDiputados.add(diputado);
        }

        return auxListDiputados;
    }
}
