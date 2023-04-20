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
    //Classe qui génère un pdf, avec les élèves présent grâce à un capteur NFC
    private static final int PERMISSION_REQUEST_CODE = 200;
    Button creerpdf;
    int pageHeight = 1120;
    int pagewidth = 792;
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    static StringBuilder[] sb = new StringBuilder[400];
    static int indexSB;
    static int y;
    DBHelper DB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView myTextView = findViewById(R.id.textView);
        myTextView.setTextSize(50);
        //---------------------------------------------------Ajouter manuellement grâce à une autre activité
        Button ajout = (Button) findViewById(R.id.ajouter);
        ajout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ajoutActivity.class);
                startActivity(intent);
            }
        });

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
        //Intent pour le NFC
        pendingIntent = PendingIntent.getActivity(this,0,new Intent(this,this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);
        //-----------------------------------------------------------------------------------------------------------!Partie BDD
        DB = new DBHelper(this);
        //----------------------------------AJOUT DES ELEVES
        ajout(DB, "Hayet Ferahi", "04 34 59 aa 7e 67 80");
        ajout(DB, "Thomas Desert", "04 58 1e 92 81 67 80");
        Cursor cursor = DB.getData();
        //Bien vérifier qu'ils sont présents et obtenir leurs données
        showData(cursor);
        //-----------------------------------------------------------------------------------------------------------!Partie PDF
        y=260;
        creerpdf = findViewById(R.id.button);
        if (checkPermission()) {
            Toast.makeText(this, "Permission Accordée !", Toast.LENGTH_SHORT).show();
        } else {
            requestPermission();
        }
        //Remettre tous les élèves à présent = faux
        Button reset=findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(int i = 0 ;i< sb.length && sb[i].length() > 0; i++){
                    sb[i]= new StringBuilder();
                }
                Cursor cursor3 = DB.getData();
                reset(DB, cursor3);
            }
        });
        creerpdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Mettre présent à tous ceux qui ont étés scannés
                    EditText myEditText = (EditText) findViewById(R.id.promo);
                    String promo = myEditText.getText().toString();
                    EditText myEditText2 = (EditText) findViewById(R.id.exam);
                    String exam = myEditText2.getText().toString();
                    //Creation d'un pdf avec le nom de l'exam, la promo et la bdd
                    if (!TextUtils.isEmpty(promo.trim()) && !TextUtils.isEmpty(exam.trim()) ) {
                        Cursor cursor2 = DB.getData();
                        generatePDF(cursor2, promo, exam);
                        showData(cursor2);
                    }
            }
        });
    }
    //--------------------------------------------Partie BDD
    //Afficher les données pour debugger
    public void showData(Cursor cursor){
        if (cursor.getCount() == 0) {
            Toast.makeText(getApplicationContext(), "No data found", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()) {
                String nom = cursor.getString(0);
                String identifiant = cursor.getString(1);
                Boolean present = cursor.getInt(2) > 0;
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
    //On ne supprime pas les élèves de la bdd, on les mets seulements tous à non présent pour un futur examen
    public void reset(DBHelper DB3, Cursor cursor3) {
        if (cursor3.getCount() == 0) {
            Toast.makeText(getApplicationContext(), "No data found", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor3.moveToNext()) {
                String identifiant = cursor3.getString(1);
                String nom = cursor3.getString(0);
                DB.updateUserPresentByIdentifiant2(identifiant);
                Toast.makeText(getApplicationContext(), "Remis " + nom + "à non présent", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //Ajouter des élèves à la bdd
    public void ajout(DBHelper DB,String nom, String identifiant){
        //Eviter d'ajouter les élèves à chaque retour à MainActivity
        if(!DB.checkIfExists(identifiant)){
            Boolean isUpdated = DB.insertuserdata(nom, identifiant, false);
            if (isUpdated) {
                Toast.makeText(getApplicationContext(), "Data updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Failed to add data", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //----------------------------------------------------NFC Lecture
    @Override
    protected void onResume() {
        super.onResume();
        assert nfcAdapter != null;
        nfcAdapter.enableForegroundDispatch(this,pendingIntent,null,null);
    }
    //En pause : On arrête le NFC, en arrière plan : il reste activé
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
    //Detecter le NFC
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
        Log.v("test " , sb[indexSB].toString());
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

        //Textes
        Paint prst = new Paint();
        Paint abs = new Paint();
        Paint enTete = new Paint();

        int currentPage = 1; // initialiser la page courrante
        PdfDocument.PageInfo mypageInfo = new PdfDocument.PageInfo.Builder(pagewidth, pageHeight, currentPage).create();
        PdfDocument.Page myPage = pdfDocument.startPage(mypageInfo);
        Canvas canvas = myPage.getCanvas();

        // Image
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.universite);
        canvas.drawBitmap(image, null, new Rect(0, 0, 100, 100), null);
        canvas.drawText("Etudiants passant l'examen de " + exam, 150, 50, enTete);
        canvas.drawText("Etudiants de : " + prom + " ", 150, 70, enTete);
        canvas.drawText("Date de l'épreuve : " + formattedDate + " ", 150, 90, enTete);

        //Textes différents : présents, absents et titre
        prst.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        prst.setColor(ContextCompat.getColor(this, R.color.green));
        prst.setTextSize(15);
        prst.setTextAlign(Paint.Align.CENTER);

        abs.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        abs.setColor(ContextCompat.getColor(this, R.color.red));
        abs.setTextSize(15);
        abs.setTextAlign(Paint.Align.CENTER);

        enTete.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        enTete.setColor(ContextCompat.getColor(this, R.color.black));
        enTete.setTextSize(15);
        enTete.setTextAlign(Paint.Align.CENTER);

        //générer le pdf
        if (cursor2.getCount() == 0) {
            Toast.makeText(getApplicationContext(), "No data found", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor2.moveToNext()) {
                //Parcourir la base de données avec le curseur
                String nom = cursor2.getString(0);
                String identifiant = cursor2.getString(1);
                Boolean present = cursor2.getInt(2) > 0;
                    if (y > pageHeight) {
                        // créer une nouvelle page quad y > la taille de la page
                        pdfDocument.finishPage(myPage);
                        currentPage++;
                        mypageInfo = new PdfDocument.PageInfo.Builder(pagewidth, pageHeight, currentPage).create();
                        myPage = pdfDocument.startPage(mypageInfo);
                        canvas = myPage.getCanvas();
                        y = 0; // reset y
                    }
                    //rouge si absent, vert sinon
                if (present) {
                    canvas.drawText("\u2713 " + nom , 376, y, prst);
                    y += 50; // mettre à jour y
                }
                else{
                    canvas.drawText("\u274C " + nom , 376, y, abs);
                    y += 50; // mettre à jour y
                }
                Log.d("TAG", "nom: " + nom + ", identifiant: " + identifiant + ", present: " + present);
            }
        }
        //créer le pdf
        pdfDocument.finishPage(myPage);
        File file = new File(Environment.getExternalStorageDirectory(), prom + "-" + exam+".pdf");
        y=250;
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(MainActivity.this, "PDF généré !", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //Permissions pour le pdf
    private boolean checkPermission() {
        int permission1 = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int permission2 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);
        return permission1 == PackageManager.PERMISSION_GRANTED && permission2 == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {

                boolean writeStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean readStorage = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if (writeStorage && readStorage) {
                    Toast.makeText(this, "Permission Obtenue", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Non obtenue", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }
}
