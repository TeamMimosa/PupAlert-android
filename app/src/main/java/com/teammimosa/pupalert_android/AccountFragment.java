package com.teammimosa.pupalert_android;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

/**
 * Signs in with google, renders account ui
 * @author Domenic
 */
public class AccountFragment extends Fragment implements View.OnClickListener
{
    private GoogleSignInClient gsic;
    private static final int RC_SIGN_IN = 9001;

    public AccountFragment()
    {
    }

    public static AccountFragment newInstance()
    {
        AccountFragment fragment = new AccountFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Google sign in stuff
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        gsic = GoogleSignIn.getClient(getActivity(), gso);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.fragment_account, container, false);

        // Set the dimensions of the sign-in button.
        SignInButton signInButton = rootView.findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setOnClickListener(this);

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity());
        updateUI(account);

        // Inflate the layout for this fragment
        return rootView;
    }

    /**
     * Updates the UI according to the account (including null if the user is not signed in.)
     * @param account
     */
    private void updateUI(GoogleSignInAccount account)
    {
        //TODO :
        //https://stackoverflow.com/questions/5953502/how-do-i-change-the-view-inside-a-fragment
        if(account != null)
        {
            //Signed in ui
        }
        else
        {
            //not signed in ui
        }
    }

    private void createSignInDialog()
    {
        Intent signInIntent = gsic.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /*
    * When the sign in button is clicked.
     */
    @Override
    public void onClick(View v)
    {
        createSignInDialog();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN)
        {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask)
    {
        try
        {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            updateUI(account);
        } catch (ApiException e)
        {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            updateUI(null);
        }
    }

}
