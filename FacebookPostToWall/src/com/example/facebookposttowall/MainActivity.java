package com.example.facebookposttowall;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

public class MainActivity extends Activity {

	private Button mLogin;
	private Button mLogout;
	private Button mPost;

	private EditText mPostText;

	private LinearLayout mPostLayout;

	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");

	private static final String TAG = "MainFragment";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mLogin = (Button) findViewById(R.id.login);
		mLogout = (Button) findViewById(R.id.logout);
		mPost = (Button) findViewById(R.id.post_button);

		mPostText = (EditText) findViewById(R.id.post_input);

		mPostLayout = (LinearLayout) findViewById(R.id.post_layout);

		mLogin.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				handleFacebookLogin();
			}

		});
		
		mLogout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				logout();
				updatePostVisibility();
			}
			
		});
		
		mPost.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String message = mPostText.getText().toString();
				publishStory(message);
				mPostText.setText("");
			}
			
		});
		
		updatePostVisibility();
	}
	
	private boolean isLoggedIn() {
		Session session = Session.getActiveSession();
		return session != null && session.isOpened();
	}
	
	private void logout() {
	    Session session = Session.getActiveSession();
	    session.closeAndClearTokenInformation();
	}
	
	private void updatePostVisibility() {
		if (isLoggedIn()) {
			mLogin.setVisibility(View.GONE);
			mPostLayout.setVisibility(View.VISIBLE);
		} else {
			mLogin.setVisibility(View.VISIBLE);
			mPostLayout.setVisibility(View.GONE);
		}
	}

	private void handleFacebookLogin() {
		Session.openActiveSession(this, true, new Session.StatusCallback() {

			@Override
			public void call(Session session, SessionState state, Exception exception) {
				if (session.isOpened()) {
					List<String> permissions = session.getPermissions();
					if (!isSubsetOf(PERMISSIONS, permissions)) {
						Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(MainActivity.this, PERMISSIONS);
						session.requestNewPublishPermissions(newPermissionsRequest);
						return;
					}
					Request.newMeRequest(session, new Request.GraphUserCallback() {

						@Override
						public void onCompleted(GraphUser user, Response response) {
							updatePostVisibility();
						}
					}).executeAsync();
				} else {
					updatePostVisibility();
				}
			}
		});
	}

	private void publishStory(String message) {
		Session session = Session.getActiveSession();

		if (session != null) {
			Bundle postParams = new Bundle();
			postParams.putString("message", message);

			Request.Callback callback = new Request.Callback() {
				public void onCompleted(Response response) {
					JSONObject graphResponse = response.getGraphObject().getInnerJSONObject();
					String postId = null;
					try {
						postId = graphResponse.getString("id");
					} catch (JSONException e) {
						Log.i(TAG, "JSON error " + e.getMessage());
					}
					FacebookRequestError error = response.getError();
					if (error != null) {
						Toast.makeText(MainActivity.this, error.getErrorMessage(), Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(MainActivity.this, postId, Toast.LENGTH_LONG).show();
					}
				}
			};

			Request request = new Request(session, "me/feed", postParams, HttpMethod.POST, callback);

			RequestAsyncTask task = new RequestAsyncTask(request);
			task.execute();
		}

	}

	private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
		for (String string : subset) {
			if (!superset.contains(string)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}
}
