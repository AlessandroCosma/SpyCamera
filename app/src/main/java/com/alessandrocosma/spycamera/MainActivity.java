package com.alessandrocosma.spycamera;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.things.contrib.driver.button.Button;
import com.google.android.things.contrib.driver.button.ButtonInputDriver;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.tasks.Task;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;



public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private FirebaseDatabase mDatabase;
    private FirebaseStorage mStorage;
    private FirebaseAuth mAuth;
    private MyCamera mCamera;

    private DatabaseReference newImageLogReference, newRequestReference;
    private ChildEventListener mChildEventListener;

    /**Driver for buttonA*/
    //private ButtonInputDriver mButtonInputDriver;

    /**Handler for running Camera tasks in the background.*/
    private Handler mCameraHandler;

    /**Additional thread for running Camera tasks that shouldn't block the UI.*/
    private HandlerThread mCameraThread;

    /**Handler for running Cloud tasks in the background.*/
    private Handler mCloudHandler;

    /**Additional thread for running Cloud tasks that shouldn't block the UI.*/
    private HandlerThread mCloudThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Main Activity created.");


        /**Turn off the led lights, opened by default*/
        try {
            BoardDefaults.turnOffLedR();
            BoardDefaults.turnOffLedG();
            BoardDefaults.turnOffLedB();
        }
        catch (IOException e){
            Log.e(TAG, "Unable to turn off the leds");
        }


        // Check for the permissions to access the camera
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No permission");
            return;
        }

        mDatabase = FirebaseDatabase.getInstance();
        mStorage = FirebaseStorage.getInstance();



        /**AUTENTICAZIONE*/
        mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithEmailAndPassword("lamiaemail@gmail.com","lamiapasswordsegreta").addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success");
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.getException());
                }

            }
        });

        /** REFERENZE FIREBASE DATABASE*/
        //ottengo una referenza al nodo /request del FirebaseDB
        newRequestReference = mDatabase.getReference().child("request");

        /** CHILDEVENTISTENER */
        //aggiungo un listener per segnalare l'avvenuta aggiunta di un figlio al nodo
        mChildEventListener = newRequestReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                Log.d(TAG, "Nuovo figlio inserito");

                //estraggo il valore appena inserito che mi rappresenta lo Uid dell utente che esegue la richiesta
                Uid uid = dataSnapshot.getValue(Uid.class);
                String uidRequest = uid.getUid();
                Log.e(TAG, "uid della richiesta= "+uidRequest);

                //scatto la foto perchè ho una nuova richiesta
                mCamera.takePicture();

                //rimuovo la richiesta effettuata
                removeRequest(uidRequest);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });



        // Creates new handlers and associated threads for camera and networking operations.
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        mCloudThread = new HandlerThread("CloudThread");
        mCloudThread.start();
        mCloudHandler = new Handler(mCloudThread.getLooper());

        // Camera code is complicated, so we've shoved it all in this closet class for you.
        mCamera = MyCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);
    }


    //metodo che rimuove la richiesta di scatto foto appena presa in carico
    private void removeRequest(String uidRequest){

        Query deleteQuery = newRequestReference.orderByChild("uid").equalTo(uidRequest);

        deleteQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot uidSnapshot: dataSnapshot.getChildren()) {
                    uidSnapshot.getRef().removeValue();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "onCancelled", databaseError.toException());
            }
        });
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.shutDown();

        mCameraThread.quitSafely();
        mCloudThread.quitSafely();

        //rimuovo il listener
        newRequestReference.removeEventListener(mChildEventListener);
        //logout dal FirebaseDB
        mAuth.signOut();
    }



    /**
     * Listener for new camera images.
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    // get image bytes
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    onPictureTaken(imageBytes);
                }
            };

    /**
     * Upload image data to Firebase.
     */
    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {
            newImageLogReference = mDatabase.getReference("imagesLog/newImage").push();
            final StorageReference imageRef = mStorage.getReference().child(newImageLogReference.getKey());

            // upload image to storage
            UploadTask task = imageRef.putBytes(imageBytes);
            task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!urlTask.isSuccessful());
                        Uri downloadUrl = urlTask.getResult();

                    // mark image in the database
                    Log.i(TAG, "Image upload successful");



                    /** Carico nel database le info sulle foto: timestamp e downloadUrl */

                    //String photoId = newImageLogReference.child("uploaded").push().getKey();
                    newImageLogReference.setValue(new FirebaseImage(downloadUrl.toString(), System.currentTimeMillis()));

                    /**
                     * Write 'IMAGE UPLOAD in the HT16K33 segment display
                     * */
                    try {
                        BoardDefaults.writeText("IMAGE UPLOAD");
                    }
                    catch (IOException | InterruptedException e){
                        Log.e(TAG, "Unable to write on the screen");
                    }

                    }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // clean up this entry
                    Log.w(TAG, "Unable to upload image to Firebase");
                    newImageLogReference.removeValue();
                }
            });
        }
    }

}

