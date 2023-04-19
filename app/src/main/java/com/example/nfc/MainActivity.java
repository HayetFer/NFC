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
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    Button creerpdf;
    int pageHeight = 1120;
    int pagewidth = 792;
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    static StringBuilder[] sb = new StringBuilder[400];
    static int indexSB;
    static int y;
    final static String TAG = "nfc_test";
    DBHelper DB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView myTextView = findViewById(R.id.textView);
        myTextView.setTextSize(50);
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
        ajout(DB, "Thomas Desert", "04 58 1e 92 81 67 80");
        Cursor cursor = DB.getData();
        showData(cursor);
        //-----------------------------------------------------------------------------------------------------------!Partie PDF
        y=260;
        creerpdf = findViewById(R.id.button);
        if (checkPermission()) {
            Toast.makeText(this, "Permission Accordée !", Toast.LENGTH_SHORT).show();
        } else {
            requestPermission();
        }
        Button reset=findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(int i = 0 ;i< sb.length && sb[i].length() > 0; i++){
                    reset(sb[i].toString());
                    sb[i]= new StringBuilder();
                }
            }
        });
        creerpdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Mettre présent à tous ceux qui ont étés scannés
                if(sb[0].length()>0){
                    for(int i = 0 ;i< sb.length && sb[i].length() > 0; i++){
                        Log.v("test " , sb[i].toString());
                    }
                    EditText myEditText = (EditText) findViewById(R.id.promo);
                    String promo = myEditText.getText().toString();
                    EditText myEditText2 = (EditText) findViewById(R.id.exam);
                    String exam = myEditText2.getText().toString();
                    if (!TextUtils.isEmpty(promo.trim()) && !TextUtils.isEmpty(exam.trim()) ) {
                        Cursor cursor2 = DB.getData();
                        generatePDF(cursor2, promo, exam);
                        showData(cursor2);
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(), "Pas assez de monde!", Toast.LENGTH_SHORT).show();
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
    public void reset(String identifiant){
        boolean result = DB.updateUserPresentByIdentifiant2(identifiant);
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
        present(DB, sb[indexSB].toString());
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

    private void generatePDF(Cursor cursor2, String prom, String exam) {
        //-----------------Obtenir la date
        LocalDate currentDate = null;
        currentDate = LocalDate.now();
        // Formater la date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        String formattedDate = currentDate.format(formatter);

        //-------------------Créer le pdf
        PdfDocument pdfDocument = new PdfDocument();

        Paint paint = new Paint();
        Paint title = new Paint();

        int currentPage = 1; // initialize the current page number
        PdfDocument.PageInfo mypageInfo = new PdfDocument.PageInfo.Builder(pagewidth, pageHeight, currentPage).create();
        PdfDocument.Page myPage = pdfDocument.startPage(mypageInfo);
        Canvas canvas = myPage.getCanvas();

        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        title.setTextSize(20);
        title.setColor(ContextCompat.getColor(this, R.color.black));
        // Load the image from the resources
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.universite);

        // Draw the image on the canvas
        canvas.drawBitmap(image, null, new Rect(0, 0, 100, 100), null);
        canvas.drawText("Etudiants passant l'examen de " + exam, 150, 50, title);
        canvas.drawText("Etudiants de : " + prom + " ", 150, 70, title);
        canvas.drawText("Date de l'épreuve : " + formattedDate + " ", 150, 90, title);


        title.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        title.setColor(ContextCompat.getColor(this, R.color.black));
        title.setTextSize(15);
        title.setTextAlign(Paint.Align.CENTER);

        if (cursor2.getCount() == 0) {
            Toast.makeText(getApplicationContext(), "No data found", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor2.moveToNext()) {
                String nom = cursor2.getString(0);
                String identifiant = cursor2.getString(1);
                Boolean present = cursor2.getInt(2) > 0;
                if (present) {
                    if (y > pageHeight) {
                        // créer une nouvelle page
                        pdfDocument.finishPage(myPage);
                        currentPage++;
                        mypageInfo = new PdfDocument.PageInfo.Builder(pagewidth, pageHeight, currentPage).create();
                        myPage = pdfDocument.startPage(mypageInfo);
                        canvas = myPage.getCanvas();
                        y = 0; // reset y
                    }
                    canvas.drawText(nom + " est présent(e)", 396, y, title);
                    y += 50; // mettre à jour y
                }
                Log.d("TAG", "nom: " + nom + ", identifiant: " + identifiant + ", present: " + present);
            }
        }

        pdfDocument.finishPage(myPage);
        File file = new File(Environment.getExternalStorageDirectory(), prom + "-" + exam+".pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(MainActivity.this, "PDF généré !", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

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
