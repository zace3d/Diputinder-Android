package mx.com.factico.diputinder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import java.util.List;
import java.util.Locale;

import mx.com.factico.diputinder.beans.Candidate;
import mx.com.factico.diputinder.beans.CandidateInfo;
import mx.com.factico.diputinder.beans.Indicator;
import mx.com.factico.diputinder.beans.Party;
import mx.com.factico.diputinder.dialogues.Dialogues;
import mx.com.factico.diputinder.httpconnection.HttpConnection;
import mx.com.factico.diputinder.utils.CacheUtils;
import mx.com.factico.diputinder.utils.ScreenUtils;
import mx.com.factico.diputinder.views.CustomTextView;

/**
 * Created by zace3d on 4/30/15.
 */
public class CandidateActivity extends AppCompatActivity {
    public static final String TAG_CLASS = CandidateActivity.class.getSimpleName();

    public static final String TAG_CANDIDATE = "candidate";
    private CandidateInfo candidateInfo;
    private DisplayImageOptions options;

    // View name of the header image. Used for activity scene transitions
    public static final String VIEW_NAME_HEADER_IMAGE = "detail:header:image";

    // View name of the header title. Used for activity scene transitions
    public static final String VIEW_NAME_HEADER_TITLE = "detail:header:title";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_candidate);

        setSupportActionBar();
        initUI();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            candidateInfo = (CandidateInfo) bundle.getSerializable(TAG_CANDIDATE);

            if (candidateInfo != null) {
                fillCandidateInfo();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        CacheUtils.unbindDrawables(findViewById(R.id.container));
        Runtime.getRuntime().gc();
    }

    @Override
    protected void onPause() {
        super.onPause();

        CacheUtils.clearMemoryCache();
    }

    protected void setSupportActionBar() {
        Toolbar mToolbar = (Toolbar) findViewById(R.id.actionbar);
        mToolbar.setTitle("");
        mToolbar.getBackground().setAlpha(255);
        TextView actionbarTitle = (TextView) mToolbar.findViewById(R.id.actionbar_title);
        actionbarTitle.setText(getResources().getString(R.string.app_name));

        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    protected void initUI() {
        options = new DisplayImageOptions.Builder()
                //.showImageOnLoading(null)
                .showImageForEmptyUri(R.drawable.drawable_bgr_gray)
                .showImageOnFail(R.drawable.drawable_bgr_gray)
                .resetViewBeforeLoading(true)
                //.cacheInMemory(false)
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .considerExifParams(true)
                //.displayer(new FadeInBitmapDisplayer(300))
                .build();
    }

    protected void fillCandidateInfo() {

        Candidate candidate = candidateInfo.getCandidate().getCandidate();
        if (candidate != null) {
            String nombres =  candidate.getNombres() != null ? candidate.getNombres() : "";
            String apellidoPaterno = candidate.getApellidoPaterno() != null ? candidate.getApellidoPaterno() : "";
            String apellidoMaterno = candidate.getApellidoMaterno() != null ? candidate.getApellidoMaterno() : "";

            CustomTextView tvName = (CustomTextView) findViewById(R.id.candidate_tv_name);
            tvName.setText(String.format(Locale.getDefault(), "%s %s %s", nombres, apellidoPaterno, apellidoMaterno));

            Point point = ScreenUtils.getScreenSize(getBaseContext());
            int width = (int) (point.x / 2.6);
            int height = point.y / 5;

            /*RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(width / 2, 0, 0, 0);
            View vgInfo = findViewById(R.id.candidate_vg_info);
            vgInfo.setMinimumHeight(width);
            vgInfo.setPadding(width / 2, 0, 0, 0);
            vgInfo.setLayoutParams(params);*/

            int sizeIcons = point.x / 5;

            LinearLayout.LayoutParams profileImageParams = new LinearLayout.LayoutParams(width, width);
            profileImageParams.gravity = Gravity.CENTER_HORIZONTAL;

            RelativeLayout vgProfile = (RelativeLayout) findViewById(R.id.candidate_vg_profile);

            ImageView ivProfile = (ImageView) findViewById(R.id.candidate_iv_profile);
            //ivProfile.setLayoutParams(profileImageParams);
            vgProfile.setLayoutParams(profileImageParams);

            // BEGIN_INCLUDE(detail_set_view_name)
            /**
             * Set the name of the view's which will be transition to, using the static values above.
             * This could be done in the layout XML, but exposing it via static variables allows easy
             * querying from other Activities
             */
            //ViewCompat.setTransitionName(ivProfile, VIEW_NAME_HEADER_IMAGE);
            ViewCompat.setTransitionName(tvName, VIEW_NAME_HEADER_TITLE);
            // END_INCLUDE(detail_set_view_name)

            if (candidate.getTwitter() != null && !candidate.getTwitter().equals("") && !candidate.getTwitter().equals("no se identificó")) {
                String twitter = candidate.getTwitter().replaceAll("\\s+", "");
                ImageLoader.getInstance().displayImage(String.format(Locale.getDefault(), HttpConnection.TWITTER_IMAGE_URL, twitter), ivProfile, options);
            } else {
                ivProfile.setImageResource(R.drawable.drawable_bgr_gray);
            }

            // Cargo
            CustomTextView tvCargo = (CustomTextView) findViewById(R.id.candidate_tv_cargo);
            tvCargo.setText(candidateInfo.getPosition());

            // Entidad
            CustomTextView tvEntidad = (CustomTextView) findViewById(R.id.candidate_tv_entidad);
            tvEntidad.setText(candidateInfo.getTerritoryName());

            if (candidateInfo.getCandidate() != null) {
                // Party Name
                CustomTextView tvParty = (CustomTextView) findViewById(R.id.candidate_tv_party);

                // Partido
                ImageView ivIcon = (ImageView) findViewById(R.id.candidate_iv_partido);
                RelativeLayout.LayoutParams paramsPartido = new RelativeLayout.LayoutParams(width / 3, width / 3);
                paramsPartido.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                paramsPartido.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                ivIcon.setLayoutParams(paramsPartido);

                List<Party> parties = candidateInfo.getCandidate().getParty();
                if (parties != null && parties.size() > 0) {
                    tvParty.setText(parties.get(0).getName());

                    ImageLoader.getInstance().displayImage(parties.get(0).getImage(), ivIcon, options);
                }

                List<Indicator> indicators = candidateInfo.getCandidate().getIndicators();
                if (indicators != null && indicators.size() > 0) {
                    LinearLayout vgIndicators = (LinearLayout) findViewById(R.id.candidate_vg_indicators);

                    for (Indicator indicator : indicators) {
                        View indicatorView = createIndicatorView(indicator, sizeIcons);

                        if (indicatorView != null)
                            vgIndicators.addView(indicatorView);

                    }
                }
            }
        }
    }

    protected View createIndicatorView(Indicator indicator, int sizeIcons) {
        RelativeLayout view = (RelativeLayout) getLayoutInflater().inflate(R.layout.item_indicator, null, false);
        view.setOnClickListener(WebViewOnClickListener);

        RelativeLayout.LayoutParams paramsIconsStatus = new RelativeLayout.LayoutParams(sizeIcons / 2, sizeIcons / 2);
        paramsIconsStatus.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        paramsIconsStatus.addRule(RelativeLayout.CENTER_VERTICAL);

        RelativeLayout.LayoutParams paramsIcons = new RelativeLayout.LayoutParams((int) (sizeIcons / 1.3), (int) (sizeIcons / 1.3));
        paramsIcons.setMargins(5, 10, 5, 10);
        paramsIcons.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

        CustomTextView tvName = (CustomTextView) view.findViewById(R.id.indicator_tv_name);
        tvName.setText(indicator.getName());

        ImageView ivIndicator = (ImageView) view.findViewById(R.id.indicator_iv_icon);
        ivIndicator.setLayoutParams(paramsIcons);

        ImageView ivIndicatorStatus = (ImageView) view.findViewById(R.id.indicator_iv_status);
        ivIndicatorStatus.setLayoutParams(paramsIconsStatus);

        if (indicator.getDocument() != null && !indicator.getDocument().equals("")) {
            view.setTag(indicator.getDocument());

            ivIndicatorStatus.setImageResource(R.drawable.ic_btn_declaro);
        }

        return view;
    }

    private View.OnClickListener WebViewOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getTag() != null) {
                String url = v.getTag().toString();

                if (url != null && !url.equals(""))
                    startWebViewIntent(url);
                else
                    Dialogues.Toast(getBaseContext(), "No ha presentado documento.", Toast.LENGTH_SHORT);
            } else {
                Dialogues.Toast(getBaseContext(), "No ha presentado documento.", Toast.LENGTH_SHORT);
            }
        }
    };

    private void startWebViewIntent(String url) {
        Intent intent = new Intent(getBaseContext(), WebViewActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("actionbarTitle", getString(R.string.lbl_document));
        startActivity(intent);
    }

    protected View createImageParty(Party party, int width) {
        ImageView ivIcon = new ImageView(getBaseContext());
        ivIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams paramsParty = new LinearLayout.LayoutParams(width / 4, width / 4);
        paramsParty.setMargins(5, 0, 5, 0);
        ivIcon.setLayoutParams(paramsParty);
        ImageLoader.getInstance().displayImage(party.getImage(), ivIcon, options);

        return ivIcon;
    }
}