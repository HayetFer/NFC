package com.example.nfc;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    Button creerpdf;
    int pageHeight = 1120;
    int pagewidth = 792;
    //Bitmap bmp, scaledbmp;
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    static StringBuilder[] sb = new StringBuilder[400];
    static int indexSB;
    final static String TAG = "nfc_test";
    DBHelper DB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //---------------------------------------------------Partie NFC
        //Initialiser les nfc
        indexSB=0;
        for (int i = 0; i < sb.length; i++) {
            sb[i] = new StringBuilder();
        }
        //Verifier que le capteur NFC marche
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null){
            Toast.makeText(this,"NO NFC Capabilities",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        pendingIntent = PendingIntent.getActivity(this,0,new Intent(this,this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);
        //-----------------------------------------------------------------------------------------------------------!Partie BDD
        DB = new DBHelper(this);
        DB.deleteAllData();
        //----------------------------------AJOUT DES ELEVES
        ajout(DB, "Hayet Ferahi", "04 34 59 aa 7e 67 80");
        Cursor cursor = DB.getData();
        showData(cursor);
        //-----------------------------------------------------------------------------------------------------------!Partie PDF
        creerpdf = findViewById(R.id.button);
        if (checkPermission()) {
            Toast.makeText(this, "Permission Accordée !", Toast.LENGTH_SHORT).show();
        } else {
            requestPermission();
        }

        creerpdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Mettre présent à tous ceux qui ont étés scannés
                if(sb.length!=0){
                    for(int i = 0 ;i< sb.length && sb[i].length() > 0; i++){
                        present(DB, sb[i].toString());
                    }
                    Cursor cursor2 = DB.getData();
                    generatePDF(cursor2);
                    showData(cursor2);
                }
            }
        });
    }
    //--------------------------------------------Partie BDD
    public void showData(Cursor cursor){
        if (cursor.getCount() == 0) {
            Toast.makeText(getApplicationContext(), "No data found", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()) {
                String nom = cursor.getString(0);
                String identifiant = cursor.getString(1);
                Boolean present = cursor.getInt(2) > 0;
                // print the data to the console or logcat
                Log.d("TAG", "nom: " + nom + ", identifiant: " + identifiant + ", present: " + present);
            }
        }
    }
    //Mettre présent à vrai avec l'identifiant
    public void present(DBHelper DB, String identifiant){
        boolean result = DB.updateUserPresentByIdentifiant(identifiant);
        if (result) {
            Toast.makeText(this, "Data updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to update data", Toast.LENGTH_SHORT).show();
        }
    }
    //Ajouter des élèves à la bdd
    public void ajout(DBHelper DB,String nom, String identifiant){
        Boolean isUpdated = DB.insertuserdata(nom, identifiant, false);
        if (isUpdated) {
            Toast.makeText(getApplicationContext(), "Data updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Failed to add data", Toast.LENGTH_SHORT).show();
        }
    }
    //----------------------------------------------------NFC Lecture
    @Override
    protected void onResume() {
        super.onResume();
        assert nfcAdapter != null;
        //nfcAdapter.enableForegroundDispatch(context,pendingIntent,
        //                                    intentFilterArray,
        //                                    techListsArray)
        nfcAdapter.enableForegroundDispatch(this,pendingIntent,null,null);
    }
    protected void onPause() {
        super.onPause();
        //Onpause stop listening
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        resolveIntent(intent);
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            assert tag != null;
            //byte[] payload = detectTagData(tag).getBytes();
            detectTagData(tag);
        }
    }
    //Ajouter dans un tableau les identifiants présents
    private String detectTagData(Tag tag) {
        byte[] id = tag.getId();
        String reversedHex = toReversedHex(id);
        boolean isDuplicate = false;
        for (int i = 0; i < sb.length; i++) {
            if (sb[i].toString().contains(reversedHex)) {
                isDuplicate = true;
                break;
            }
        }
        if (!isDuplicate) {
                sb[indexSB].append(reversedHex);
        }
        indexSB++;
        return sb.toString();
    }
    //Avoir l'info en HEX
    private String toReversedHex(byte[] bytes) {
        StringBuilder sb2 = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            if (i > 0) {
                sb2.append(" ");
            }
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb2.append('0');
            sb2.append(Integer.toHexString(b));
        }
        return sb2.toString();
    }

    //-----------------------------------------------------------Partie PDF

    private void generatePDF(Cursor cursor2) {

        PdfDocument pdfDocument = new PdfDocument();


        Paint paint = new Paint();
        Paint title = new Paint();


        PdfDocument.PageInfo mypageInfo = new PdfDocument.PageInfo.Builder(pagewidth, pageHeight, 1).create();


        PdfDocument.Page myPage = pdfDocument.startPage(mypageInfo);


        Canvas canvas = myPage.getCanvas();


        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        title.setTextSize(15);


        title.setColor(ContextCompat.getColor(this, R.color.purple_200));


        canvas.drawText("Etudiants passant l'examen", 300, 300, title);


        title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        title.setColor(ContextCompat.getColor(this, R.color.purple_200));
        title.setTextSize(15);


        title.setTextAlign(Paint.Align.CENTER);
        if (cursor2.getCount() == 0) {
            Toast.makeText(getApplicationContext(), "No data found", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor2.moveToNext()) {
                String nom = cursor2.getString(0);
                String identifiant = cursor2.getString(1);
                Boolean present = cursor2.getInt(2) > 0;
                if(present==true){
                    canvas.drawText(nom + " est présent(e)",396, 560, title);
                }
                Log.d("TAG", "nom: " + nom + ", identifiant: " + identifiant + ", present: " + present);
            }
        }
        pdfDocument.finishPage(myPage);
        File file = new File(Environment.getExternalStorageDirectory(), "Etudiants.pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(MainActivity.this, "PDF généré !", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
        pdfDocument.close();
    }

    private boolean checkPermission() {
        // checking of permissions.
        int permission1 = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int permission2 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
        return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        // requesting permissions if not provided.
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {

                // after requesting permissions we are showing
                // users a toast message of permission granted.
                boolean writeStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean readStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if (writeStorage && readStorage) {
                    Toast.makeText(this, "Permission Granted..", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Denied.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }
}
