package com.teammimosa.pupalert_android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.teammimosa.pupalert_android.R;
import com.teammimosa.pupalert_android.util.PupAlertFirebase;
import com.teammimosa.pupalert_android.util.Utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * Signs in with google, renders account ui
 * @author Domenic
 */
public class FragmentAccount extends Fragment implements View.OnClickListener
{
    private GoogleSignInClient gsic;
    private FirebaseAuth mAuth;
    private static final int RC_SIGN_IN = 9001;

    private View rootView;

    PupAlertFirebase database;

    public FragmentAccount()
    {
    }

    public static FragmentAccount newInstance()
    {
        FragmentAccount fragment = new FragmentAccount();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Google sign in stuff
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gsic = GoogleSignIn.getClient(getActivity(), gso);

        database = new PupAlertFirebase(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        rootView = inflater.inflate(R.layout.fragment_account, container, false);

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
       // GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getActivity());
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);

        // Inflate the layout for this fragment
        return rootView;
    }

    /**
     * Updates the UI according to the account (including null if the user is not signed in.)
     * @param account
     */
    private void updateUI(final FirebaseUser account)
    {
        if(account != null) //signed in
        {
            //check if user exists in db, if not, add them.
            //TODO possibly move where it checks if the user exists?
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("users").child(account.getUid());

            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    if (dataSnapshot.exists())
                    {
                        // User exists. Do nothing
                    } else
                    {
                        database.storeUser(account.getUid(), account.getDisplayName(), 0);
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {
                }
            });

            rootView.findViewById(R.id.account_signed_out).setVisibility(View.INVISIBLE);
            rootView.findViewById(R.id.account_signed_in).setVisibility(View.VISIBLE);

            TextView acct_name = rootView.findViewById(R.id.account_name);
            acct_name.setText(account.getDisplayName());

            final TextView acct_posts = rootView.findViewById(R.id.account_posts);

            //query user key for post total
            DatabaseReference mRef = FirebaseDatabase.getInstance().getReference().child("users").child(account.getUid());
            mRef.addListenerForSingleValueEvent(new ValueEventListener()
            {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot)
                {
                    if(dataSnapshot.exists())
                    {
                        PupAlertFirebase.User user = dataSnapshot.getValue(PupAlertFirebase.User.class);
                        acct_posts.setText("Total Posts: " + user.gettotal_posts());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError)
                {
                }
            });

            ImageView acct_pic = rootView.findViewById(R.id.account_picture_in);
            Glide.with(this).load(account.getPhotoUrl()).into(acct_pic);

            Button signOutBtn = rootView.findViewById(R.id.signout_button);
            signOutBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    FirebaseAuth.getInstance().signOut();
                    gsic.revokeAccess();
                    updateUI(null);
                }
            });
        }
        else
        {
            rootView.findViewById(R.id.account_signed_out).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.account_signed_in).setVisibility(View.INVISIBLE);

            // Set the dimensions of the sign-in button.
            SignInButton signInButton = rootView.findViewById(R.id.sign_in_button);
            signInButton.setSize(SignInButton.SIZE_WIDE);
            signInButton.setOnClickListener(this);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct)
    {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>()
                {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task)
                    {
                        if (task.isSuccessful())
                        {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        }
                        else
                        {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(getActivity(), "Login Failed!", Toast.LENGTH_SHORT);
                            updateUI(null);
                        }
                    }
                });
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

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN)
        {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try
            {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e)
            {
                // Google Sign In failed, update UI appropriately
                Toast.makeText(getActivity(), "Login Failed!", Toast.LENGTH_SHORT);
                updateUI(null);
            }
        }
    }

    /*
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
*/
}
