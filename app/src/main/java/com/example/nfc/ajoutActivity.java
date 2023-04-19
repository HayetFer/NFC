package com.example.nfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ajoutActivity extends AppCompatActivity {
    
    DBHelper DB2;
    StringBuilder ajt;
    NfcAdapter nfcAdapter2;
    PendingIntent pendingIntent2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ajout);
        //----------------------------------NFC
        ajt=new StringBuilder();
        //----------------------------------Base de données
        //Verifier que le capteur NFC marche
        nfcAdapter2 = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter2 == null){
            Toast.makeText(this,"NO NFC Capabilities",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        pendingIntent2 = PendingIntent.getActivity(this, 0, new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        DB2 = new DBHelper(this);
        
        // retourner en arrière
        Button retour = (Button) findViewById(R.id.retour);
        retour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ajoutActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });
        Button ajout2 = (Button) findViewById(R.id.ajout2);
        ajout2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText myEditText = (EditText) findViewById(R.id.ajouter);
                String ajout2 = myEditText.getText().toString();
                if (!TextUtils.isEmpty(ajout2.trim()) && ajt.length()!=0) {
                    ajout2(DB2,ajout2,ajt.toString());
                }
                else{
                    Toast.makeText(getApplicationContext(), "Pas de nom | identifiant vide", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    public void ajout2(DBHelper DB2,String nom, String identifiant){
        Boolean isUpdated = DB2.insertuserdata(nom, identifiant, false);
        if (isUpdated) {
            Toast.makeText(getApplicationContext(), "Data updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(), "Failed to add data", Toast.LENGTH_SHORT).show();
        }
    }
    
    //--------------------------------------------------------------Partie NFC
    @Override
    protected void onResume() {
        super.onResume();
        assert nfcAdapter2 != null;
        //nfcAdapter2.enableForegroundDispatch(context,pendingIntent2,
        //                                    intentFilterArray,
        //                                    techListsArray)
        nfcAdapter2.enableForegroundDispatch(this,pendingIntent2,null,null);
    }
    protected void onPause() {
        super.onPause();
        //Onpause stop listening
        if (nfcAdapter2 != null) {
            nfcAdapter2.disableForegroundDispatch(this);
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
        for (int i = 0; i < ajt.length(); i++) {
            if (ajt.toString().contains(reversedHex)) {
                isDuplicate = true;
                break;
            }
        }
        if (!isDuplicate) {
            ajt.append(reversedHex);
        }
        return ajt.toString();
    }
    //Avoir l'info en HEX
    private String toReversedHex(byte[] bytes) {
        for (int i = 0; i < bytes.length; ++i) {
            if (i > 0) {
                ajt.append(" ");
            }
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                ajt.append('0');
            ajt.append(Integer.toHexString(b));
        }
        return ajt.toString();
    }
}
