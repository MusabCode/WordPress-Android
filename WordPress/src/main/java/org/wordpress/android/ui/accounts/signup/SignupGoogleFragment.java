package org.wordpress.android.ui.accounts.signup;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.ui.accounts.GoogleFragment;
import org.wordpress.android.util.AppLog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

public class SignupGoogleFragment extends GoogleFragment {
    private static final int REQUEST_SIGNUP = 1002;

    public static final String TAG = "signup_google_fragment_tag";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);

        switch (request) {
            case REQUEST_SIGNUP:
                if (result == RESULT_OK) {
                    GoogleSignInResult signInResult = Auth.GoogleSignInApi.getSignInResultFromIntent(data);

                    if (signInResult.isSuccess()) {
                        try {
                            GoogleSignInAccount account = signInResult.getSignInAccount();
                            mDisplayName = account.getDisplayName();
                            mGoogleEmail = account.getEmail();
                            mIdToken = account.getIdToken();
                            mPhotoUrl = removeScaleFromGooglePhotoUrl(account.getPhotoUrl().toString());
                            // TODO: Call social signup endpoint.
                        } catch (NullPointerException exception) {
                            disconnectGoogleClient();
                            AppLog.e(AppLog.T.NUX, "Cannot get ID token from Google signup account.", exception);
                            showErrorDialog(getString(R.string.login_error_generic));
                        }
                    } else {
                        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_BUTTON_FAILURE);
                        switch (signInResult.getStatus().getStatusCode()) {
                            // Internal error.
                            case GoogleSignInStatusCodes.INTERNAL_ERROR:
                                AppLog.e(AppLog.T.NUX, "Google Signup Failed: internal error.");
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                            // Attempted to connect with an invalid account name specified.
                            case GoogleSignInStatusCodes.INVALID_ACCOUNT:
                                AppLog.e(AppLog.T.NUX, "Google Signup Failed: invalid account name.");
                                showErrorDialog(getString(R.string.login_error_generic)
                                        + getString(R.string.login_error_suffix));
                                break;
                            // Network error.
                            case GoogleSignInStatusCodes.NETWORK_ERROR:
                                AppLog.e(AppLog.T.NUX, "Google Signup Failed: network error.");
                                showErrorDialog(getString(R.string.error_generic_network));
                                break;
                            // Cancelled by the user.
                            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                                AppLog.e(AppLog.T.NUX, "Google Signup Failed: cancelled by user.");
                                break;
                            // Attempt didn't succeed with the current account.
                            case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                                AppLog.e(AppLog.T.NUX, "Google Signup Failed: current account failed.");
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                            // Attempted to connect, but the user is not signed in.
                            case GoogleSignInStatusCodes.SIGN_IN_REQUIRED:
                                AppLog.e(AppLog.T.NUX, "Google Signup Failed: user is not signed in.");
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                            // Unknown error.
                            default:
                                AppLog.e(AppLog.T.NUX, "Google Signup Failed: unknown error.");
                                showErrorDialog(getString(R.string.login_error_generic));
                                break;
                        }
                    }
                } else if (result == RESULT_CANCELED) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_BUTTON_FAILURE);
                    AppLog.e(AppLog.T.NUX, "Google Signup Failed: result was CANCELED.");
                } else {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_BUTTON_FAILURE);
                    AppLog.e(AppLog.T.NUX, "Google Signup Failed: result was not OK or CANCELED.");
                    showErrorDialog(getString(R.string.login_error_generic));
                }

                break;
        }
    }

    @Override
    protected void showAccountDialog() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, REQUEST_SIGNUP);
    }

    // Remove scale from photo URL path string.  Current URL matches /s96-c, which returns a 96 x 96
    // pixel image.  Removing /s96-c from the string returns a 512 x 512 pixel image.  Using regular
    // expressions may help if the photo URL scale value in the returned path changes.
    private String removeScaleFromGooglePhotoUrl(String photoUrl) {
        Pattern pattern = Pattern.compile("(/s[0-9]+-c)");
        Matcher matcher = pattern.matcher(photoUrl);
        return matcher.find() ? photoUrl.replace(matcher.group(1), "") : photoUrl;
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(AccountStore.OnAuthenticationChanged event) {
        mGoogleListener.onGoogleSignupFinished(mDisplayName, mGoogleEmail, mPhotoUrl);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocialChanged(AccountStore.OnSocialChanged event) {
        mGoogleListener.onGoogleSignupFinished(mDisplayName, mGoogleEmail, mPhotoUrl);
    }
}
