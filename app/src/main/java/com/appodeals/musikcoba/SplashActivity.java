package com.appodeals.musikcoba;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.explorestack.consent.Consent;
import com.explorestack.consent.ConsentForm;
import com.explorestack.consent.ConsentFormListener;
import com.explorestack.consent.ConsentInfoUpdateListener;
import com.explorestack.consent.ConsentManager;
import com.explorestack.consent.exception.ConsentManagerException;


public class SplashActivity extends Activity {

    Handler handler;
    Animation animFin;
//    SharedPreferences appIntro = null;
//    FragmentManager fragmentManager;

    @Nullable
    private ConsentForm consentForm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ImageView splash_image = findViewById(R.id.splash);
        animFin = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_in);
        splash_image.startAnimation(animFin);

        Config.randomNum();
        resolveUserConsent();
        //showPrivacyPolicy();

//        handler=new Handler();
//        handler.postDelayed(() -> {
//
//            //startActivity(intent);
//            //finish();
//        },2000);
    }

//    private void showPrivacyPolicy() {
//        appIntro = getSharedPreferences("hasRunBefore_appIntro", 0);  //load the preferences
//        boolean hasRun = appIntro.getBoolean("hasRun_appIntro", false); //see if it's run before, default no
//        PrivacyPolicy ppFragment = new PrivacyPolicy();
//        //If FirstTime
//        if (!hasRun) {
//            //code for if this is the first time the application is Running
//            //Display dialogfragment
//            ppFragment.setCancelable(true);
//            ppFragment.show(fragmentManager, "Privacy Policy");
//
//        }
//    }

//    public class PrivacyPolicy extends DialogFragment {
//
//        Button btn_accept, btn_decline;
//        Intent intent=new Intent(SplashActivity.this,MainActivity.class);
//
//        @Override
//        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//            View rootView = inflater.inflate(R.layout.privacy_policy, container, false);
//
//
//            WebView webView = rootView.findViewById(R.id.privacy_policy_dialog);
//            btn_accept = rootView.findViewById(R.id.btn_accept);
//            btn_decline = rootView.findViewById(R.id.btn_decline);
//            TextView privacy_title = rootView.findViewById(R.id.title_privacy);
//            TextView gdpr_title = rootView.findViewById(R.id.title_gdpr);
//            TextView gdpr_body = rootView.findViewById(R.id.body_gdpr);
//
//            final Typeface medium = Typeface.createFromAsset(requireActivity().getAssets(), "gotham_medium.ttf");
//            final Typeface book = Typeface.createFromAsset(requireActivity().getAssets(), "gotham_book.ttf");
//
//            privacy_title.setTypeface(medium);
//            gdpr_title.setTypeface(medium);
//            gdpr_body.setTypeface(book);
//
//            webView.setWebViewClient(new WebViewClient());
//            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
//            webView.loadUrl("file:///android_asset/appodeals.html");
//
//            btnAccept();
//            btnDecline();
//
//            return rootView;
//        }
//
//        private void btnAccept() {
//            btn_accept.setOnClickListener(v -> {
//                SharedPreferences settings = requireActivity().getSharedPreferences("hasRunBefore_appIntro", 0);
//                SharedPreferences.Editor edit = settings.edit();
//                edit.putBoolean("hasRun_appIntro", true);
//                edit.apply(); //apply
//                dismiss();
//                startActivity(intent);
//                trans();
//            });
//        }
//
//        private void btnDecline() {
//            btn_decline.setOnClickListener(v -> System.exit(1));
//        }
//    }

    private void resolveUserConsent() {
        // Note: YOU MUST SPECIFY YOUR APPODEAL SDK KET HERE
        String appodealAppKey = getResources().getString(R.string.app_id);
        ConsentManager consentManager = ConsentManager.getInstance(this);
        // Requesting Consent info update
        consentManager.requestConsentInfoUpdate(
                appodealAppKey,
                new ConsentInfoUpdateListener() {
                    @Override
                    public void onConsentInfoUpdated(Consent consent) {
                        Consent.ShouldShow consentShouldShow =
                                consentManager.shouldShowConsentDialog();
                        // If ConsentManager return Consent.ShouldShow.TRUE, than we should show consent form
                        if (consentShouldShow == Consent.ShouldShow.TRUE) {
                            showConsentForm();
                        } else {
                            if (consent.getStatus() == Consent.Status.UNKNOWN) {
                                //Toast.makeText(getApplicationContext(), "Consent.Status.UNKNOWN", Toast.LENGTH_SHORT).show();
                                // Start our main activity with default Consent value
                                showConsentForm();
                            } else {
                                boolean hasConsent = consent.getStatus() == Consent.Status.PERSONALIZED;
                                //Toast.makeText(getApplicationContext(), "Consent.Status.PERSONALIZED", Toast.LENGTH_SHORT).show();
                                // Start our main activity with resolved Consent value
                                startMainActivity(hasConsent);
                            }
                        }
                    }

                    @Override
                    public void onFailedToUpdateConsentInfo(ConsentManagerException e) {
                        // Start our main activity with default Consent value
                        startMainActivity();
                    }
                });
    }

     //Displaying ConsentManger Consent request form
    private void showConsentForm() {
        if (consentForm == null) {
            consentForm = new ConsentForm.Builder(this)
                    .withListener(new ConsentFormListener() {
                        @Override
                        public void onConsentFormLoaded() {
                            // Show ConsentManager Consent request form
                            consentForm.showAsActivity();
                        }

                        @Override
                        public void onConsentFormError(ConsentManagerException error) {
                            Toast.makeText(
                                    SplashActivity.this,
                                    "Consent form error: " + error.getReason(),
                                    Toast.LENGTH_SHORT
                            ).show();
                            // Start our main activity with default Consent value
                            startMainActivity();
                        }

                        @Override
                        public void onConsentFormOpened() {
                            //ignore
                        }

                        @Override
                        public void onConsentFormClosed(Consent consent) {
                            boolean hasConsent = consent.getStatus() == Consent.Status.PERSONALIZED;
                            // Start our main activity with resolved Consent value
                            startMainActivity(hasConsent);
                        }
                    }).build();
        }
        // If Consent request form is already loaded, then we can display it, otherwise, we should load it first
        if (consentForm.isLoaded()) {
            consentForm.showAsActivity();
        } else {
            consentForm.load();
        }
    }

    // Start our main activity with default Consent value
    private void startMainActivity() {
        trans();
        startMainActivity(true);
    }

    // Start our main activity with resolved Consent value
    private void startMainActivity(boolean hasConsent) {
        trans();
        startActivity(MainActivity.getIntent(this, hasConsent));
    }

    private void trans() {
        handler=new Handler();
        handler.postDelayed(() -> {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            getWindow().clearFlags(R.layout.splash_screen);
        },2000);
    }

}
