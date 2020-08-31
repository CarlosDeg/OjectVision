package appcom.example.firebase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Xfermode;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.common.internal.Constants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Comment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicMarkableReference;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST = 1;
    ImageView imagenView;

    Button elegirImg, subirImg;
    imagenes datos;
    TextView r;
    private static final  int Image_Request_Code = 7;
    StorageReference mStorageRef;
    DatabaseReference storage;
    public int respuesta2 = 1;
    public String NuevaRespuesta;
    public final String res = "objeto";
    private ValueEventListener eventListener;
    Button TomarF;
    Bitmap bitmap;


    public  String key="";

    ProgressDialog progressDialog;
    private Uri imguri;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mStorageRef = FirebaseStorage.getInstance().getReference("Imagenes");//STORAGE
        storage = FirebaseDatabase.getInstance().getReference().child("imagenes");//BASE DE DATOS
        imagenView = (ImageView) findViewById(R.id.ImageView);
        elegirImg = (Button) findViewById(R.id.Elegir);
        subirImg = (Button) findViewById(R.id.Subir);
        TomarF = (Button) findViewById(R.id.TomarFoto);
        r = (TextView) findViewById(R.id.Respuesta);


        progressDialog = new ProgressDialog(MainActivity.this);
        datos = new imagenes();
        elegirImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                elegirimg();

            }
        });
        subirImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SubirImage();


            }
        });


        TomarF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                CapturarFoto();

            }
        });



    }



    private void CapturarFoto() {

        key = UUID.randomUUID().toString();
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        File foto = new File(getExternalFilesDir(null),"test.jpg");
        imguri = FileProvider.getUriForFile(getApplicationContext(),getApplicationContext().getPackageName()+".provider",foto);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT,imguri);
        startActivityForResult(cameraIntent,CAMERA_REQUEST);

        r.setText("");
        imagenView.setImageBitmap(null);


    }


    public void elegirimg() {


        key = UUID.randomUUID().toString();
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), Image_Request_Code);

        r.setText("");
        imagenView.setImageBitmap(null);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Image_Request_Code && resultCode == RESULT_OK && data != null && data.getData() != null) {

            imguri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imguri);
                imagenView.setImageBitmap(bitmap);
                subirImg.setEnabled(true);

            } catch (IOException e) {

                e.printStackTrace();

            }


        }


        else if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {

            Bitmap bitmap = BitmapFactory.decodeFile(getApplicationContext().getExternalFilesDir(null) + "/test.jpg");
            imagenView.setImageBitmap(bitmap);
            subirImg.setEnabled(true);

        }else{
            subirImg.setEnabled(false);
            Toast.makeText(MainActivity.this, "No has seleccionado ninguna imagen", Toast.LENGTH_SHORT).show();

        }
    }



    private String getExtension(Uri uri) {

        ContentResolver cr = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(cr.getType(uri));

    }


    public void SubirImage() {

        if (imguri != null  ) {
            String nameId;
            nameId = System.currentTimeMillis() + "." + getExtension(imguri);
            datos.setImagen(nameId);
            datos.setRespuesta(res);
            datos.setUid(key);
            storage.push().setValue(datos);
            StorageReference Ref = mStorageRef.child(nameId);
            final ProgressDialog progressDialog = new ProgressDialog(this);

            progressDialog.setTitle("Reconociendo imagen...");
            progressDialog.show();
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);


            Ref.putFile(imguri)
                    .addOnSuccessListener(
                            new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                    progressDialog.dismiss();


                                    obtenerdatos();

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(MainActivity.this, "Error Fatal " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            })
                    .addOnProgressListener(
                            new OnProgressListener<UploadTask.TaskSnapshot>() {

                                // Progress Listener for loading
                                // percentage on the dialog box
                                @Override
                                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                    double progress
                                            = (100.0
                                            * taskSnapshot.getBytesTransferred()
                                            / taskSnapshot.getTotalByteCount());
                                    progressDialog.setMessage("  " + (int) progress + "%");
                                }
                            });
        } else {

            Toast.makeText(MainActivity.this, "No has seleccionado ninguna imagen", Toast.LENGTH_SHORT).show();
        }
    }


    public void obtenerdatos() {
        final DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        Query query = db.child("imagenes").orderByChild("respuesta");
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot
                                             dataSnapshot) {

                for (final DataSnapshot snapshot : dataSnapshot.getChildren()) {

                    db.child("imagenes").child(snapshot.getKey()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            imagenes img = snapshot.getValue(imagenes.class);
                            NuevaRespuesta = img.getRespuesta();
                            String Id = img.getUid();
                            if(Id.equals(key)) {
                                if ("objeto".equals(NuevaRespuesta)) {
                                    respuesta2++;
                                    r.setText("Espera un momento....."  );
                                    obtenerdatos();



                                } else {
                                    r.setText("En la imagen hay un " + NuevaRespuesta);
                                    // key = "";

                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                }
            }


            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }



}













